package com.disneyplanner.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_message")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AiMessage {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private AiSession session;

    @Column(nullable = false)
    private String role; // "user" | "assistant"

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "token_count")
    private Integer tokenCount;

    @Column(name = "model_version")
    private String modelVersion;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Column(name = "quality_tag")
    private String qualityTag;

    @Column(name = "reviewed_by")
    private String reviewedBy;

    @Column(name = "review_note")
    private String reviewNote;

    @Column(name = "promoted_to_kb", nullable = false)
    private boolean promotedToKb;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() { createdAt = Instant.now(); }
}
