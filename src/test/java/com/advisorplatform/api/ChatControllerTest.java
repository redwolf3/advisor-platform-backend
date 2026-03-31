package com.advisorplatform.api;

import com.advisorplatform.ai.PlannerAiService;
import com.advisorplatform.domain.entity.AiMessage;
import com.advisorplatform.domain.entity.AiSession;
import com.advisorplatform.domain.entity.Visitor;
import com.advisorplatform.service.VisitorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ChatController.class)
@TestPropertySource(properties = "spring.ai.anthropic.api-key=test-key")
class ChatControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean VisitorService visitorService;
    @MockBean PlannerAiService plannerAiService;

    // ── POST /api/visitor/identify ────────────────────────────────────────────

    @Test
    void identify_validToken_returns200WithVisitorIdAndToken() throws Exception {
        UUID visitorId = UUID.randomUUID();
        Visitor visitor = new Visitor();
        ReflectionTestUtils.setField(visitor, "id", visitorId);
        visitor.setBrowserToken("token-abc");
        when(visitorService.findOrCreate("token-abc")).thenReturn(visitor);

        mockMvc.perform(post("/api/visitor/identify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"browserToken\":\"token-abc\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visitorId").value(visitorId.toString()))
                .andExpect(jsonPath("$.browserToken").value("token-abc"));
    }

    @Test
    void identify_blankToken_returns400() throws Exception {
        mockMvc.perform(post("/api/visitor/identify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"browserToken\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    // ── POST /api/session ────────────────────────────────────────────────────

    @Test
    void createSession_validVisitorId_returns200WithSessionId() throws Exception {
        UUID visitorId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        AiSession session = new AiSession();
        ReflectionTestUtils.setField(session, "id", sessionId);
        ReflectionTestUtils.setField(session, "createdAt", Instant.parse("2026-03-28T10:00:00Z"));
        when(visitorService.createSession(eq(visitorId))).thenReturn(session);

        mockMvc.perform(post("/api/session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visitorId\":\"" + visitorId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(sessionId.toString()));
    }

    @Test
    void createSession_serviceThrowsIllegalArgument_returns400() throws Exception {
        when(visitorService.createSession(any()))
                .thenThrow(new IllegalArgumentException("Visitor not found: some-id"));

        mockMvc.perform(post("/api/session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visitorId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isBadRequest());
    }

    // ── GET /api/visitor/{visitorId}/sessions ─────────────────────────────────

    @Test
    void getSessions_returns200WithList() throws Exception {
        UUID visitorId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        AiSession session = new AiSession();
        ReflectionTestUtils.setField(session, "id", sessionId);
        ReflectionTestUtils.setField(session, "createdAt", Instant.parse("2026-03-28T10:00:00Z"));
        when(visitorService.getSessions(visitorId)).thenReturn(List.of(session));

        mockMvc.perform(get("/api/visitor/{visitorId}/sessions", visitorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sessionId").value(sessionId.toString()));
    }

    // ── POST /api/chat/{sessionId} ────────────────────────────────────────────

    @Test
    void chat_validRequest_returns200WithAssistantMessage() throws Exception {
        UUID sessionId = UUID.randomUUID();
        // assistantMessage(session, content, model, tokenCount, latencyMs)
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

    // ── POST /api/chat/{sessionId}/stream ─────────────────────────────────────

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
