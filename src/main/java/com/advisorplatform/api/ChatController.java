package com.advisorplatform.api;

import com.advisorplatform.ai.PlannerAiService;
import com.advisorplatform.domain.entity.AiMessage;
import com.advisorplatform.domain.entity.AiSession;
import com.advisorplatform.service.VisitorService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class ChatController {

    private final VisitorService visitorService;
    private final PlannerAiService plannerAiService;

    public ChatController(VisitorService visitorService, PlannerAiService plannerAiService) {
        this.visitorService = visitorService;
        this.plannerAiService = plannerAiService;
    }

    // ── Visitor / Session lifecycle ──────────────────────────────────────────

    /** Called on page load with the browser token from localStorage. */
    @PostMapping("/visitor/identify")
    public ResponseEntity<VisitorResponse> identify(@Valid @RequestBody IdentifyRequest req) {
        var visitor = visitorService.findOrCreate(req.browserToken());
        return ResponseEntity.ok(new VisitorResponse(visitor.getId(), visitor.getBrowserToken()));
    }

    /** Create a new planning session. */
    @PostMapping("/session")
    public ResponseEntity<SessionResponse> createSession(@Valid @RequestBody CreateSessionRequest req) {
        AiSession session = visitorService.createSession(req.visitorId());
        return ResponseEntity.ok(new SessionResponse(session.getId(), session.getCreatedAt().toString()));
    }

    /** List sessions for a visitor (for history panel). */
    @GetMapping("/visitor/{visitorId}/sessions")
    public ResponseEntity<List<SessionResponse>> getSessions(@PathVariable UUID visitorId) {
        List<SessionResponse> sessions = visitorService.getSessions(visitorId).stream()
                .map(s -> new SessionResponse(s.getId(), s.getCreatedAt().toString()))
                .toList();
        return ResponseEntity.ok(sessions);
    }

    // ── Chat ─────────────────────────────────────────────────────────────────

    /**
     * Streaming SSE chat endpoint.
     * Frontend connects with EventSource or fetch+ReadableStream.
     */
    @PostMapping(value = "/chat/{sessionId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChat(
            @PathVariable UUID sessionId,
            @Valid @RequestBody ChatRequest req) {
        return plannerAiService.streamChat(sessionId, req.message());
    }

    /** Non-streaming fallback — useful for testing. */
    @PostMapping("/chat/{sessionId}")
    public ResponseEntity<ChatResponse> chat(
            @PathVariable UUID sessionId,
            @Valid @RequestBody ChatRequest req) {
        AiMessage response = plannerAiService.chat(sessionId, req.message());
        return ResponseEntity.ok(new ChatResponse(
                response.getId(),
                response.getContent(),
                response.getModelVersion(),
                response.getCreatedAt().toString()
        ));
    }

    // ── Records (inline DTOs) ─────────────────────────────────────────────────

    record IdentifyRequest(@NotBlank String browserToken) {}
    record VisitorResponse(UUID visitorId, String browserToken) {}
    record CreateSessionRequest(UUID visitorId) {}
    record SessionResponse(UUID sessionId, String createdAt) {}
    record ChatRequest(@NotBlank String message) {}
    record ChatResponse(UUID messageId, String content, String model, String createdAt) {}
}
