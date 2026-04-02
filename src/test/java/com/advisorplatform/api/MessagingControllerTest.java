package com.advisorplatform.api;

import com.advisorplatform.domain.entity.MessageThread;
import com.advisorplatform.domain.entity.ThreadMessage;
import com.advisorplatform.domain.entity.Visitor;
import com.advisorplatform.generated.messaging.api.MessagingApiController;
import com.advisorplatform.service.MessageService;
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
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = MessagingApiController.class)
@Import(MessagingDelegate.class)
@TestPropertySource(properties = "spring.ai.anthropic.api-key=test-key")
class MessagingControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean MessageService messageService;

    @Test
    void createThread_validRequest_returns200WithThreadId() throws Exception {
        UUID visitorId = UUID.randomUUID();
        UUID threadId = UUID.randomUUID();
        Visitor visitor = new Visitor();
        MessageThread thread = MessageThread.builder().visitor(visitor).subject("Help me").build();
        ReflectionTestUtils.setField(thread, "id", threadId);
        when(messageService.createThread(eq(visitorId), isNull(), eq("Help me"), eq("First message")))
                .thenReturn(thread);

        mockMvc.perform(post("/api/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visitorId\":\"" + visitorId + "\","
                                + "\"subject\":\"Help me\","
                                + "\"content\":\"First message\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.threadId").value(threadId.toString()));
    }

    @Test
    void createThread_blankContent_returns400() throws Exception {
        mockMvc.perform(post("/api/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visitorId\":\"" + UUID.randomUUID() + "\","
                                + "\"content\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createThread_serviceThrowsIllegalArgument_returns400() throws Exception {
        when(messageService.createThread(any(), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Visitor not found: some-id"));

        mockMvc.perform(post("/api/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visitorId\":\"" + UUID.randomUUID() + "\","
                                + "\"content\":\"Hello\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createThread_nullVisitorId_returns400() throws Exception {
        mockMvc.perform(post("/api/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visitorId\":null,\"content\":\"Hello\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getThreads_validVisitorId_returns200WithList() throws Exception {
        UUID visitorId = UUID.randomUUID();
        UUID threadId = UUID.randomUUID();
        Visitor visitor = new Visitor();
        MessageThread thread = MessageThread.builder().visitor(visitor).subject("My question").build();
        ReflectionTestUtils.setField(thread, "id", threadId);
        ReflectionTestUtils.setField(thread, "updatedAt", Instant.parse("2026-03-28T10:00:00Z"));
        when(messageService.getThreads(visitorId)).thenReturn(List.of(thread));

        mockMvc.perform(get("/api/visitor/{visitorId}/threads", visitorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].threadId").value(threadId.toString()))
                .andExpect(jsonPath("$[0].subject").value("My question"))
                .andExpect(jsonPath("$[0].status").value("open"));
    }

    @Test
    void getMessages_validThreadId_returns200WithList() throws Exception {
        UUID threadId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        Visitor visitor = new Visitor();
        MessageThread thread = MessageThread.builder().visitor(visitor).build();
        ThreadMessage message = ThreadMessage.builder()
                .thread(thread).senderRole("visitor").content("Hello").build();
        ReflectionTestUtils.setField(message, "id", messageId);
        ReflectionTestUtils.setField(message, "createdAt", Instant.parse("2026-03-28T10:00:00Z"));
        when(messageService.getMessages(threadId)).thenReturn(List.of(message));

        mockMvc.perform(get("/api/thread/{threadId}/messages", threadId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].messageId").value(messageId.toString()))
                .andExpect(jsonPath("$[0].senderRole").value("visitor"))
                .andExpect(jsonPath("$[0].content").value("Hello"));
    }

    @Test
    void addMessage_validContent_returns200WithMessageId() throws Exception {
        UUID threadId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        ThreadMessage message = ThreadMessage.builder()
                .thread(MessageThread.builder().visitor(new Visitor()).build())
                .senderRole("visitor")
                .content("Follow up question")
                .build();
        ReflectionTestUtils.setField(message, "id", messageId);
        when(messageService.addMessage(eq(threadId), eq("Follow up question"))).thenReturn(message);

        mockMvc.perform(post("/api/thread/{threadId}/messages", threadId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Follow up question\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messageId").value(messageId.toString()));
    }

    @Test
    void addMessage_blankContent_returns400() throws Exception {
        mockMvc.perform(post("/api/thread/{threadId}/messages", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"\"}"))
                .andExpect(status().isBadRequest());
    }
}
