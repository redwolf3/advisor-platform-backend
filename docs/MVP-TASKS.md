# Advisor Platform Backend — MVP Task Tracker

Status key: ✅ Done · 🔄 In Progress · ⬜ Pending · 🚫 Deferred

---

## Phase 1 — Core Infrastructure

| # | Task | Status |
|---|------|--------|
| 1.1 | Project setup: rename, packages, pom.xml, docker-compose | ✅ Done |
| 1.2 | PostgreSQL via Docker Compose (credentials: `advisorplatform`) | ✅ Done |
| 1.3 | Flyway migrations — `visitor`, `ai_session`, `ai_message`, `message_thread`, `thread_message` tables | ✅ Done |
| 1.4 | JPA entities: `Visitor`, `AiSession`, `AiMessage`, `MessageThread`, `ThreadMessage` | ✅ Done |
| 1.5 | Spring Boot Actuator health check (`GET /actuator/health`) | ✅ Done |
| 1.6 | CORS config (`CorsConfig.java`) | ✅ Done |

---

## Phase 1 — Visitor & AI Chat

| # | Task | Status |
|---|------|--------|
| 2.1 | `VisitorService.findOrCreate` — find-or-create visitor by browser token | ✅ Done |
| 2.2 | `POST /api/visitor/identify` endpoint | ✅ Done |
| 2.3 | `VisitorService.createSession` / `getSessions` | ✅ Done |
| 2.4 | `POST /api/session`, `GET /api/visitor/{id}/sessions` endpoints | ✅ Done |
| 2.5 | `PlannerAiService.chat` — non-streaming, persists both turns | ✅ Done |
| 2.6 | `PlannerAiService.streamChat` — SSE Flux, persists assistant turn on complete | ✅ Done |
| 2.7 | `POST /api/chat/{sessionId}` and `POST /api/chat/{sessionId}/stream` endpoints | ✅ Done |
| 2.8 | System prompt loaded from classpath resource | ✅ Done |

---

## Phase 1 — Visitor Messaging (Thread + Follow-up)

| # | Task | Status |
|---|------|--------|
| 3.1 | `MessageService.createThread` — creates thread + first visitor message | ✅ Done |
| 3.2 | `MessageService.addMessage` — adds follow-up visitor message, bumps `updatedAt` | ✅ Done |
| 3.3 | `MessageService.getThreads` / `getMessages` | ✅ Done |
| 3.4 | `POST /api/message` — create thread endpoint | ✅ Done |
| 3.5 | `GET /api/visitor/{id}/threads`, `GET /api/thread/{id}/messages` endpoints | ✅ Done |
| 3.6 | `POST /api/thread/{threadId}/messages` — visitor follow-up endpoint | ✅ Done |

---

## Phase 1 — Code Quality & Tests

| # | Task | Status |
|---|------|--------|
| 4.1 | Unit tests: `VisitorServiceTest` (5), `MessageServiceTest` (8), `PlannerAiServiceTest` (5) | ✅ Done |
| 4.2 | Controller slice tests: `ChatControllerTest` (9), `MessageControllerTest` (8) | ✅ Done |
| 4.3 | `GlobalExceptionHandler` — `IllegalArgumentException` → 400 | ✅ Done |
| 4.4 | `@NotNull` on `visitorId` in `CreateThreadRequest` and `CreateSessionRequest` | ✅ Done |

---

## Phase 1 — Advisor Reply & Thread Status

| # | Task | Status |
|---|------|--------|
| 5.1 | `MessageService.advisorReply(UUID threadId, String content)` — saves `senderRole="advisor"`, sets status → `"pending_reply"` | ⬜ Pending |
| 5.2 | `MessageService.updateStatus(UUID threadId, String status)` — validates `resolved`/`closed`, updates thread | ⬜ Pending |
| 5.3 | `POST /api/thread/{threadId}/reply` endpoint | ⬜ Pending |
| 5.4 | `PATCH /api/thread/{threadId}/status` endpoint | ⬜ Pending |
| 5.5 | Tests for advisor reply and status endpoints | ⬜ Pending |

---

## Phase 2 — Auth (Deferred)

| # | Task | Status |
|---|------|--------|
| 6.1 | Spring Security — visitor vs advisor roles | 🚫 Deferred |
| 6.2 | JWT or session-based auth | 🚫 Deferred |
| 6.3 | Secure advisor-only endpoints (`/reply`, `/status`) | 🚫 Deferred |

---

## Phase 2 — Notifications (TBD)

| # | Task | Status |
|---|------|--------|
| 7.1 | Email notification to advisor on new thread | 🚫 Deferred |
| 7.2 | Email notification to visitor on advisor reply | 🚫 Deferred |

---

*Last updated: 2026-04-01. Next up: Phase 1 §5 — Advisor Reply & Thread Status.*
