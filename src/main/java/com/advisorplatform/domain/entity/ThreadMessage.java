package com.advisorplatform.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "thread_message")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ThreadMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "thread_id", nullable = false)
    private MessageThread thread;

    @Column(name = "sender_role", nullable = false)
    private String senderRole; // "visitor" | "advisor"

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "email_notified", nullable = false)
    @Builder.Default
    private boolean emailNotified = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() { createdAt = Instant.now(); }
}
