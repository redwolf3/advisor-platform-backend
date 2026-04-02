package com.advisorplatform.api;

import com.advisorplatform.ai.PlannerAiService;
import com.advisorplatform.domain.entity.AiMessage;
import com.advisorplatform.domain.entity.AiSession;
import com.advisorplatform.generated.aichat.api.AiChatApiController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {AiChatApiController.class, AiChatStreamController.class})
@Import(AiChatDelegate.class)
@TestPropertySource(properties = "spring.ai.anthropic.api-key=test-key")
// AiChatApiController owns POST /api/chat/{sessionId}
// AiChatStreamController owns POST /api/chat/{sessionId}/stream (SSE — not code-generated)
class AiChatControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean PlannerAiService plannerAiService;

    @Test
    void chat_validRequest_returns200WithAssistantMessage() throws Exception {
        UUID sessionId = UUID.randomUUID();
        AiMessage response = AiMessage.assistantMessage(new AiSession(), "AI reply", "claude-3", 100, 250);
        ReflectionTestUtils.setField(response, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(response, "createdAt", Instant.parse("2026-03-28T10:00:00Z"));
        when(plannerAiService.chat(eq(sessionId), eq("Hello"))).thenReturn(response);

        mockMvc.perform(post("/api/chat/{sessionId}", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Hello\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("AI reply"))
                .andExpect(jsonPath("$.model").value("claude-3"));
    }

    @Test
    void chat_blankMessage_returns400() throws Exception {
        mockMvc.perform(post("/api/chat/{sessionId}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void streamChat_validRequest_returnsEventStreamStatus200() throws Exception {
        UUID sessionId = UUID.randomUUID();
        when(plannerAiService.streamChat(eq(sessionId), eq("Hi"))).thenReturn(Flux.just("chunk1", "chunk2"));

        mockMvc.perform(post("/api/chat/{sessionId}/stream", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Hi\"}"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM));
    }
}
