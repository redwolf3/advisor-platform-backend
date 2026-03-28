package com.disneyplanner.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "thread_message")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ThreadMessage {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thread_id", nullable = false)
    private MessageThread thread;

    @Column(name = "sender_role", nullable = false)
    private String senderRole; // "visitor" | "advisor"

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "sent_via")
    private String sentVia; // "app" | "email"

    @Column(name = "email_notified", nullable = false)
    private boolean emailNotified;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() { createdAt = Instant.now(); }
}
