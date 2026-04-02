package com.advisorplatform.api;

import com.advisorplatform.generated.messaging.api.MessagingApiDelegate;
import com.advisorplatform.generated.messaging.model.*;
import com.advisorplatform.service.MessageService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class MessagingDelegate implements MessagingApiDelegate {

    private final MessageService messageService;

    public MessagingDelegate(MessageService messageService) {
        this.messageService = messageService;
    }

    @Override
    public ResponseEntity<CreateThreadResponse> createThread(CreateThreadRequest request) {
        var thread = messageService.createThread(
                request.getVisitorId(),
                request.getAiSessionId(),
                request.getSubject(),
                request.getContent());
        var response = new CreateThreadResponse();
        response.setThreadId(thread.getId());
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<List<ThreadSummary>> getThreads(UUID visitorId) {
        var summaries = messageService.getThreads(visitorId).stream()
                .map(t -> {
                    var s = new ThreadSummary();
                    s.setThreadId(t.getId());
                    s.setSubject(t.getSubject());
                    s.setStatus(t.getStatus());
                    s.setUpdatedAt(t.getUpdatedAt().toString());
                    return s;
                })
                .toList();
        return ResponseEntity.ok(summaries);
    }

    @Override
    public ResponseEntity<List<MessageSummary>> getMessages(UUID threadId) {
        var summaries = messageService.getMessages(threadId).stream()
                .map(m -> {
                    var s = new MessageSummary();
                    s.setMessageId(m.getId());
                    s.setSenderRole(m.getSenderRole());
                    s.setContent(m.getContent());
                    s.setCreatedAt(m.getCreatedAt().toString());
                    return s;
                })
                .toList();
        return ResponseEntity.ok(summaries);
    }

    @Override
    public ResponseEntity<AddMessageResponse> addMessage(UUID threadId, AddMessageRequest request) {
        var message = messageService.addMessage(threadId, request.getContent());
        var response = new AddMessageResponse();
        response.setMessageId(message.getId());
        return ResponseEntity.ok(response);
    }
}
