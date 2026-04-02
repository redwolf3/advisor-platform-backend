package com.advisorplatform.api;

import com.advisorplatform.domain.entity.AiSession;
import com.advisorplatform.domain.entity.Visitor;
import com.advisorplatform.generated.visitorsession.api.VisitorSessionApiController;
import com.advisorplatform.service.VisitorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = VisitorSessionApiController.class)
@Import(VisitorSessionDelegate.class)
@TestPropertySource(properties = "spring.ai.anthropic.api-key=test-key")
class VisitorSessionControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean VisitorService visitorService;

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

    @Test
    void createSession_nullVisitorId_returns400() throws Exception {
        mockMvc.perform(post("/api/session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visitorId\":null}"))
                .andExpect(status().isBadRequest());
    }

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
}
