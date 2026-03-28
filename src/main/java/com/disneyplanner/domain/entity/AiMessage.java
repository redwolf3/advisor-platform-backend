package com.disneyplanner.domain.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_message")
public class AiMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private AiSession session;

    @Column(name = "role", nullable = false)
    private String role; // "user" | "assistant"

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "token_count")
    private Integer tokenCount;

    @Column(name = "model_version")
    private String modelVersion;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    // Reinforcement / admin review fields
    @Column(name = "quality_tag")
    private String qualityTag; // "good" | "bad" | "needs_edit"

    @Column(name = "review_note", columnDefinition = "TEXT")
    private String reviewNote;

    @Column(name = "promoted_to_kb", nullable = false)
    private boolean promotedToKb = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    // Static factory helpers
    public static AiMessage userMessage(AiSession session, String content) {
        AiMessage m = new AiMessage();
        m.session = session;
        m.role = "user";
        m.content = content;
        return m;
    }

    public static AiMessage assistantMessage(AiSession session, String content,
                                              String model, int tokenCount, int latencyMs) {
        AiMessage m = new AiMessage();
        m.session = session;
        m.role = "assistant";
        m.content = content;
        m.modelVersion = model;
        m.tokenCount = tokenCount;
        m.latencyMs = latencyMs;
        return m;
    }

    public UUID getId() { return id; }
    public AiSession getSession() { return session; }
    public String getRole() { return role; }
    public String getContent() { return content; }
    public Integer getTokenCount() { return tokenCount; }
    public String getModelVersion() { return modelVersion; }
    public Integer getLatencyMs() { return latencyMs; }
    public String getQualityTag() { return qualityTag; }
    public void setQualityTag(String qualityTag) { this.qualityTag = qualityTag; }
    public String getReviewNote() { return reviewNote; }
    public void setReviewNote(String reviewNote) { this.reviewNote = reviewNote; }
    public boolean isPromotedToKb() { return promotedToKb; }
    public void setPromotedToKb(boolean promotedToKb) { this.promotedToKb = promotedToKb; }
    public Instant getCreatedAt() { return createdAt; }
}
