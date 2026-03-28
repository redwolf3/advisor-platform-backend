package com.disneyplanner.domain.repository;

import com.disneyplanner.domain.entity.AiSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface AiSessionRepository extends JpaRepository<AiSession, UUID> {
    List<AiSession> findByVisitorIdOrderByCreatedAtDesc(UUID visitorId);
}
