package com.disneyplanner.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "visitor")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Visitor {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "browser_token", nullable = false, unique = true)
    private String browserToken;

    @Column(unique = true)
    private String email;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    @Column(name = "tos_accepted_at")
    private Instant tosAcceptedAt;

    @Column(name = "tos_version")
    private String tosVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    @PrePersist
    void prePersist() {
        createdAt = lastSeenAt = Instant.now();
    }
}
