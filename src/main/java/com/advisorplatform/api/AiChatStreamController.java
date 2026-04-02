package com.advisorplatform.api;

import com.advisorplatform.ai.PlannerAiService;
import com.advisorplatform.generated.aichat.model.ChatRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
public class AiChatStreamController {

    private final PlannerAiService plannerAiService;

    public AiChatStreamController(PlannerAiService plannerAiService) {
        this.plannerAiService = plannerAiService;
    }

    @PostMapping(value = "/{sessionId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChat(
            @PathVariable UUID sessionId,
            @Valid @RequestBody ChatRequest request) {
        return plannerAiService.streamChat(sessionId, request.getMessage());
    }
}
