package com.advisorplatform.domain.repository;

import com.advisorplatform.domain.entity.ThreadMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ThreadMessageRepository extends JpaRepository<ThreadMessage, UUID> {
    List<ThreadMessage> findByThreadIdOrderByCreatedAtAsc(UUID threadId);
}
