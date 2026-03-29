package com.advisorplatform.domain.repository;

import com.advisorplatform.domain.entity.MessageThread;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MessageThreadRepository extends JpaRepository<MessageThread, UUID> {
    List<MessageThread> findByVisitorIdOrderByUpdatedAtDesc(UUID visitorId);
}
