package com.advisorplatform.domain.repository;

import com.advisorplatform.domain.entity.AiMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface AiMessageRepository extends JpaRepository<AiMessage, UUID> {
    List<AiMessage> findBySessionIdOrderByCreatedAtAsc(UUID sessionId);
}
