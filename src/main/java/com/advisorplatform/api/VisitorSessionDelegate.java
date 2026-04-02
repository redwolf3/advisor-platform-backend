package com.advisorplatform.api;

import com.advisorplatform.generated.visitorsession.api.VisitorSessionApiDelegate;
import com.advisorplatform.generated.visitorsession.model.CreateSessionRequest;
import com.advisorplatform.generated.visitorsession.model.IdentifyRequest;
import com.advisorplatform.generated.visitorsession.model.SessionResponse;
import com.advisorplatform.generated.visitorsession.model.VisitorResponse;
import com.advisorplatform.service.VisitorService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class VisitorSessionDelegate implements VisitorSessionApiDelegate {

    private final VisitorService visitorService;

    public VisitorSessionDelegate(VisitorService visitorService) {
        this.visitorService = visitorService;
    }

    @Override
    public ResponseEntity<VisitorResponse> identify(IdentifyRequest request) {
        var visitor = visitorService.findOrCreate(request.getBrowserToken());
        var response = new VisitorResponse();
        response.setVisitorId(visitor.getId());
        response.setBrowserToken(visitor.getBrowserToken());
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<SessionResponse> createSession(CreateSessionRequest request) {
        var session = visitorService.createSession(request.getVisitorId());
        var response = new SessionResponse();
        response.setSessionId(session.getId());
        response.setCreatedAt(session.getCreatedAt().toString());
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<List<SessionResponse>> getSessions(UUID visitorId) {
        var sessions = visitorService.getSessions(visitorId).stream()
                .map(s -> {
                    var r = new SessionResponse();
                    r.setSessionId(s.getId());
                    r.setCreatedAt(s.getCreatedAt().toString());
                    return r;
                })
                .toList();
        return ResponseEntity.ok(sessions);
    }
}
