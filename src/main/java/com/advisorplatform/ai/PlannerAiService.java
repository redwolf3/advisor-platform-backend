package com.advisorplatform.ai;

import com.advisorplatform.domain.entity.AiMessage;
import com.advisorplatform.domain.entity.AiSession;
import com.advisorplatform.domain.repository.AiMessageRepository;
import com.advisorplatform.domain.repository.AiSessionRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class PlannerAiService {

    private final ChatModel chatModel;
    private final AiSessionRepository sessionRepository;
    private final AiMessageRepository messageRepository;
    private final String systemPrompt;
    private final int maxHistoryTurns;

    public PlannerAiService(
            ChatModel chatModel,
            AiSessionRepository sessionRepository,
            AiMessageRepository messageRepository,
            @Value("classpath:${app.ai.system-prompt-path}") Resource systemPromptResource,
            @Value("${app.ai.max-history-turns}") int maxHistoryTurns) throws IOException {
        this.chatModel = chatModel;
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.systemPrompt = systemPromptResource.getContentAsString(StandardCharsets.UTF_8);
        this.maxHistoryTurns = maxHistoryTurns;
    }

    /**
     * Send a user message, persist both turns, return the assistant response.
     * Non-streaming version — use streamChat for SSE.
     */
    @Transactional
    public AiMessage chat(UUID sessionId, String userContent) {
        AiSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));

        // Load history BEFORE persisting the user message to avoid the new message
        // appearing in the reloaded history and then again as the final UserMessage.
        List<AiMessage> history = messageRepository.findBySessionIdOrderByCreatedAtAsc(session.getId());

        // Persist user message
        AiMessage userMsg = AiMessage.userMessage(session, userContent);
        messageRepository.save(userMsg);

        // Build prompt with pre-loaded history window
        List<Message> messages = buildMessages(history, userContent);
        Prompt prompt = new Prompt(messages);

        long start = System.currentTimeMillis();
        ChatResponse response = chatModel.call(prompt);
        int latency = (int) (System.currentTimeMillis() - start);

        String assistantContent = response.getResult().getOutput().getText();
        int tokenCount = response.getMetadata().getUsage() != null
                ? (int) response.getMetadata().getUsage().getTotalTokens() : 0;
        String model = chatModel.getDefaultOptions().getModel();

        // Persist assistant message
        AiMessage assistantMsg = AiMessage.assistantMessage(
                session, assistantContent, model, tokenCount, latency);
        messageRepository.save(assistantMsg);

        return assistantMsg;
    }

    /**
     * Streaming version — returns a Flux of text chunks for SSE.
     * User message is persisted immediately; assistant message is persisted in a
     * separate transaction when the stream completes.
     */
    public Flux<String> streamChat(UUID sessionId, String userContent) {
        AiSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));

        // Load history BEFORE persisting the user message (same fix as chat()).
        List<AiMessage> history = messageRepository.findBySessionIdOrderByCreatedAtAsc(session.getId());

        AiMessage userMsg = AiMessage.userMessage(session, userContent);
        messageRepository.save(userMsg);

        List<Message> messages = buildMessages(history, userContent);
        Prompt prompt = new Prompt(messages);

        long start = System.currentTimeMillis();
        StringBuilder fullResponse = new StringBuilder();

        return chatModel.stream(prompt)
                .filter(chunk -> chunk.getResult() != null)
                .map(chunk -> {
                    String text = chunk.getResult().getOutput().getText();
                    if (text != null) fullResponse.append(text);
                    return text != null ? text : "";
                })
                .doOnComplete(() -> {
                    int latency = (int) (System.currentTimeMillis() - start);
                    AiMessage assistantMsg = AiMessage.assistantMessage(
                            session, fullResponse.toString(),
                            chatModel.getDefaultOptions().getModel(),
                            0, // token count not available mid-stream; update if metadata available
                            latency);
                    messageRepository.save(assistantMsg);
                });
    }

    private List<Message> buildMessages(List<AiMessage> history, String newUserContent) {
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemPrompt));

        // Apply window cap to the pre-loaded history (maxHistoryTurns pairs)
        int startIndex = Math.max(0, history.size() - (maxHistoryTurns * 2));
        for (int i = startIndex; i < history.size(); i++) {
            AiMessage m = history.get(i);
            if ("user".equals(m.getRole())) {
                messages.add(new UserMessage(m.getContent()));
            } else {
                messages.add(new AssistantMessage(m.getContent()));
            }
        }

        // Add the new user message
        messages.add(new UserMessage(newUserContent));
        return messages;
    }
}
