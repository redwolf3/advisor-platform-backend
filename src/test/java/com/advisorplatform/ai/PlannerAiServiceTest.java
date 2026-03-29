package com.advisorplatform.ai;

import com.advisorplatform.domain.entity.AiMessage;
import com.advisorplatform.domain.entity.AiSession;
import com.advisorplatform.domain.repository.AiMessageRepository;
import com.advisorplatform.domain.repository.AiSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.io.Resource;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlannerAiServiceTest {

    @Mock ChatModel chatModel;
    @Mock AiSessionRepository sessionRepository;
    @Mock AiMessageRepository messageRepository;

    PlannerAiService service;

    @BeforeEach
    void setUp() throws IOException {
        Resource resource = mock(Resource.class);
        when(resource.getContentAsString(StandardCharsets.UTF_8)).thenReturn("You are a helpful advisor.");
        service = new PlannerAiService(chatModel, sessionRepository, messageRepository, resource, 3);
    }

    // ── chat ─────────────────────────────────────────────────────────────────

    @Test
    void chat_unknownSession_throwsIllegalArgument() {
        UUID sessionId = UUID.randomUUID();
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.chat(sessionId, "hello"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Session not found");
    }

    @Test
    void chat_knownSession_persistsUserAndAssistantMessagesAndReturnsAssistant() {
        UUID sessionId = UUID.randomUUID();
        AiSession session = new AiSession();
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(messageRepository.findBySessionIdOrderByCreatedAtAsc(any())).thenReturn(List.of());
        when(messageRepository.save(any(AiMessage.class))).thenAnswer(inv -> inv.getArgument(0));

        ChatOptions chatOptions = mock(ChatOptions.class);
        when(chatOptions.getModel()).thenReturn("claude-3-haiku");

        ChatResponse response = mock(ChatResponse.class, RETURNS_DEEP_STUBS);
        when(response.getResult().getOutput().getText()).thenReturn("AI answer");
        when(response.getMetadata().getUsage()).thenReturn(null);
        when(chatModel.call(any(Prompt.class))).thenReturn(response);
        when(chatModel.getDefaultOptions()).thenReturn(chatOptions);

        AiMessage result = service.chat(sessionId, "User question");

        assertThat(result.getContent()).isEqualTo("AI answer");
        assertThat(result.getRole()).isEqualTo("assistant");
        verify(messageRepository, times(2)).save(any(AiMessage.class)); // user + assistant
    }

    @Test
    void chat_historyExceedsWindow_onlyRecentTurnsIncludedInPrompt() {
        UUID sessionId = UUID.randomUUID();
        AiSession session = new AiSession();
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        // 4 full turns = 8 messages; maxHistoryTurns=3 so only last 6 should be sent
        List<AiMessage> history = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            history.add(AiMessage.userMessage(session, "user " + i));
            history.add(AiMessage.assistantMessage(session, "ai " + i, "model", 0, 0));
        }
        when(messageRepository.findBySessionIdOrderByCreatedAtAsc(any())).thenReturn(history);
        when(messageRepository.save(any(AiMessage.class))).thenAnswer(inv -> inv.getArgument(0));

        ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);

        ChatOptions chatOptions = mock(ChatOptions.class);
        when(chatOptions.getModel()).thenReturn("claude-3-haiku");

        ChatResponse response = mock(ChatResponse.class, RETURNS_DEEP_STUBS);
        when(response.getResult().getOutput().getText()).thenReturn("ok");
        when(response.getMetadata().getUsage()).thenReturn(null);
        when(chatModel.call(promptCaptor.capture())).thenReturn(response);
        when(chatModel.getDefaultOptions()).thenReturn(chatOptions);

        service.chat(sessionId, "new message");

        // 1 system + 6 history (last 3 turns = 6 messages) + 1 new user = 8 instructions
        List<?> instructions = promptCaptor.getValue().getInstructions();
        assertThat(instructions).hasSize(8);
        assertThat(instructions.get(0)).isInstanceOf(SystemMessage.class);
        assertThat(instructions.get(7)).isInstanceOf(UserMessage.class);
    }

    // ── streamChat ───────────────────────────────────────────────────────────

    @Test
    void streamChat_unknownSession_throwsIllegalArgument() {
        UUID sessionId = UUID.randomUUID();
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.streamChat(sessionId, "hello"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Session not found");
    }

    @Test
    void streamChat_knownSession_emitsChunksAndPersistsOnComplete() {
        UUID sessionId = UUID.randomUUID();
        AiSession session = new AiSession();
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(messageRepository.findBySessionIdOrderByCreatedAtAsc(any())).thenReturn(List.of());
        when(messageRepository.save(any(AiMessage.class))).thenAnswer(inv -> inv.getArgument(0));

        ChatOptions chatOptions = mock(ChatOptions.class);
        when(chatOptions.getModel()).thenReturn("claude-3-haiku");

        ChatResponse chunk = mock(ChatResponse.class, RETURNS_DEEP_STUBS);
        when(chunk.getResult().getOutput().getText()).thenReturn("Hello");
        when(chatModel.stream(any(Prompt.class))).thenReturn(Flux.just(chunk));
        when(chatModel.getDefaultOptions()).thenReturn(chatOptions);

        Flux<String> flux = service.streamChat(sessionId, "Hi");
        List<String> chunks = flux.collectList().block();

        assertThat(chunks).containsExactly("Hello");
        // user message persisted before stream + assistant message persisted in doOnComplete
        verify(messageRepository, times(2)).save(any(AiMessage.class));
    }
}
