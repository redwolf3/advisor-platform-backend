package com.disneyplanner.service;

import com.disneyplanner.domain.entity.AiSession;
import com.disneyplanner.domain.entity.Visitor;
import com.disneyplanner.domain.repository.AiSessionRepository;
import com.disneyplanner.domain.repository.VisitorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class VisitorService {

    private final VisitorRepository visitorRepository;
    private final AiSessionRepository aiSessionRepository;

    public VisitorService(VisitorRepository visitorRepository,
                          AiSessionRepository aiSessionRepository) {
        this.visitorRepository = visitorRepository;
        this.aiSessionRepository = aiSessionRepository;
    }

    /** Find or create a visitor by browser token. Called on every page load. */
    @Transactional
    public Visitor findOrCreate(String browserToken) {
        return visitorRepository.findByBrowserToken(browserToken)
                .map(v -> {
                    v.setLastSeenAt(java.time.Instant.now());
                    return visitorRepository.save(v);
                })
                .orElseGet(() -> {
                    Visitor v = new Visitor();
                    v.setBrowserToken(browserToken);
                    return visitorRepository.save(v);
                });
    }

    /** Create a new AI planning session for a visitor. */
    @Transactional
    public AiSession createSession(UUID visitorId) {
        Visitor visitor = visitorRepository.findById(visitorId)
                .orElseThrow(() -> new IllegalArgumentException("Visitor not found"));
        AiSession session = new AiSession();
        session.setVisitor(visitor);
        return aiSessionRepository.save(session);
    }

    /** Get all sessions for a visitor, newest first. */
    public List<AiSession> getSessions(UUID visitorId) {
        return aiSessionRepository.findByVisitorIdOrderByCreatedAtDesc(visitorId);
    }
}
