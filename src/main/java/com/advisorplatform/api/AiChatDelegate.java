package com.advisorplatform.api;

import com.advisorplatform.ai.PlannerAiService;
import com.advisorplatform.domain.entity.AiMessage;
import com.advisorplatform.generated.aichat.api.AiChatApiDelegate;
import com.advisorplatform.generated.aichat.model.ChatRequest;
import com.advisorplatform.generated.aichat.model.ChatResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AiChatDelegate implements AiChatApiDelegate {

    private final PlannerAiService plannerAiService;

    public AiChatDelegate(PlannerAiService plannerAiService) {
        this.plannerAiService = plannerAiService;
    }

    @Override
    public ResponseEntity<ChatResponse> chat(UUID sessionId, ChatRequest request) {
        AiMessage message = plannerAiService.chat(sessionId, request.getMessage());
        var response = new ChatResponse();
        response.setMessageId(message.getId());
        response.setContent(message.getContent());
        response.setModel(message.getModelVersion());
        response.setCreatedAt(message.getCreatedAt().toString());
        return ResponseEntity.ok(response);
    }
}
