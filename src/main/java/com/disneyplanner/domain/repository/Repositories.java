package com.disneyplanner.domain.repository;

import com.disneyplanner.domain.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VisitorRepository extends JpaRepository<Visitor, UUID> {
    Optional<Visitor> findByBrowserToken(String browserToken);
    Optional<Visitor> findByEmail(String email);
}

@Repository
interface AiSessionRepository extends JpaRepository<AiSession, UUID> {
    List<AiSession> findByVisitorIdOrderByCreatedAtDesc(UUID visitorId);
}

@Repository
interface AiMessageRepository extends JpaRepository<AiMessage, UUID> {
    List<AiMessage> findBySessionIdOrderByCreatedAt(UUID sessionId);
}

@Repository
interface MessageThreadRepository extends JpaRepository<MessageThread, UUID> {
    List<MessageThread> findByVisitorIdOrderByUpdatedAtDesc(UUID visitorId);
}

@Repository
interface ThreadMessageRepository extends JpaRepository<ThreadMessage, UUID> {
    List<ThreadMessage> findByThreadIdOrderByCreatedAt(UUID threadId);
}
