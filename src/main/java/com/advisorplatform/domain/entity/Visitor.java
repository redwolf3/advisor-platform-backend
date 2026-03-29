package com.advisorplatform.domain.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "visitor")
public class Visitor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "browser_token", nullable = false, unique = true)
    private String browserToken;

    @Column(name = "email", unique = true)
    private String email;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    @Column(name = "tos_accepted_at")
    private Instant tosAcceptedAt;

    @Column(name = "tos_version")
    private String tosVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt = Instant.now();

    // Getters / setters — replace with Lombok @Data if you add it later
    public UUID getId() { return id; }
    public String getBrowserToken() { return browserToken; }
    public void setBrowserToken(String browserToken) { this.browserToken = browserToken; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }
    public Instant getTosAcceptedAt() { return tosAcceptedAt; }
    public void setTosAcceptedAt(Instant tosAcceptedAt) { this.tosAcceptedAt = tosAcceptedAt; }
    public String getTosVersion() { return tosVersion; }
    public void setTosVersion(String tosVersion) { this.tosVersion = tosVersion; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(Instant lastSeenAt) { this.lastSeenAt = lastSeenAt; }
}
