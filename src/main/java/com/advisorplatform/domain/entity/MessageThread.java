package com.advisorplatform.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "message_thread")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MessageThread {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "visitor_id", nullable = false)
    private Visitor visitor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ai_session_id")
    private AiSession aiSession;

    @Column
    private String subject;

    @Column(nullable = false)
    @Builder.Default
    private String status = "open"; // open | pending_reply | resolved | closed

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() { createdAt = updatedAt = Instant.now(); }

    @PreUpdate
    void preUpdate() { updatedAt = Instant.now(); }
}
