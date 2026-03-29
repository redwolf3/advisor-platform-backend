package com.advisorplatform.domain.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "ai_session")
public class AiSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "visitor_id", nullable = false)
    private Visitor visitor;

    @Column(name = "title")
    private String title;

    // Freeform intake data: party size, dates, ages, interests
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "trip_context", columnDefinition = "jsonb")
    private Map<String, Object> tripContext;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("createdAt ASC")
    private List<AiMessage> messages = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void preUpdate() { this.updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public Visitor getVisitor() { return visitor; }
    public void setVisitor(Visitor visitor) { this.visitor = visitor; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Map<String, Object> getTripContext() { return tripContext; }
    public void setTripContext(Map<String, Object> tripContext) { this.tripContext = tripContext; }
    public List<AiMessage> getMessages() { return messages; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
