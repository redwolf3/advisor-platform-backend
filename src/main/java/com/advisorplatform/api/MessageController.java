package com.advisorplatform.api;

import com.advisorplatform.domain.entity.MessageThread;
import com.advisorplatform.domain.entity.ThreadMessage;
import com.advisorplatform.service.MessageService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    /** Create a new message thread with an initial visitor message. */
    @PostMapping("/message")
    public ResponseEntity<CreateThreadResponse> createThread(@Valid @RequestBody CreateThreadRequest req) {
        MessageThread thread = messageService.createThread(
                req.visitorId(), req.aiSessionId(), req.subject(), req.content());
        return ResponseEntity.ok(new CreateThreadResponse(thread.getId()));
    }

    /** List all threads for a visitor. */
    @GetMapping("/visitor/{visitorId}/threads")
    public ResponseEntity<List<ThreadSummary>> getThreads(@PathVariable UUID visitorId) {
        List<ThreadSummary> summaries = messageService.getThreads(visitorId).stream()
                .map(t -> new ThreadSummary(t.getId(), t.getSubject(), t.getStatus(), t.getUpdatedAt().toString()))
                .toList();
        return ResponseEntity.ok(summaries);
    }

    /** List all messages in a thread. */
    @GetMapping("/thread/{threadId}/messages")
    public ResponseEntity<List<MessageSummary>> getMessages(@PathVariable UUID threadId) {
        List<MessageSummary> messages = messageService.getMessages(threadId).stream()
                .map(m -> new MessageSummary(m.getId(), m.getSenderRole(), m.getContent(), m.getCreatedAt().toString()))
                .toList();
        return ResponseEntity.ok(messages);
    }

    /** Add a follow-up visitor message to an existing thread. */
    @PostMapping("/thread/{threadId}/messages")
    public ResponseEntity<AddMessageResponse> addMessage(
            @PathVariable UUID threadId,
            @Valid @RequestBody AddMessageRequest req) {
        ThreadMessage message = messageService.addMessage(threadId, req.content());
        return ResponseEntity.ok(new AddMessageResponse(message.getId()));
    }

    // ── Records (inline DTOs) ─────────────────────────────────────────────────

    record CreateThreadRequest(@NotNull UUID visitorId, UUID aiSessionId, String subject, @NotBlank String content) {}
    record CreateThreadResponse(UUID threadId) {}
    record ThreadSummary(UUID threadId, String subject, String status, String updatedAt) {}
    record MessageSummary(UUID messageId, String senderRole, String content, String createdAt) {}
    record AddMessageRequest(@NotBlank String content) {}
    record AddMessageResponse(UUID messageId) {}
}
