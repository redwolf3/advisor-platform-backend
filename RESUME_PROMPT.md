# Advisor Platform Backend — Resume Prompt

## What Has Been Done

1. **Renamed project** from `disneyplanner` → `advisorplatform` (packages, pom.xml, config, docker-compose)
2. **Cleaned up scaffolding** — deleted duplicate `domain/model/` package, duplicate migration, leftover `Repositories.java`
3. **Created entities** `MessageThread` and `ThreadMessage` in `domain/entity/`
4. **Added docker-compose.yml** — PostgreSQL 16, all credentials `advisorplatform`
5. **Added Spring Boot Actuator** — health check at `GET /actuator/health`
6. **Built messaging layer** — `MessageThreadRepository`, `ThreadMessageRepository`, `MessageService`, `MessageController`
7. **Refactored `PlannerAiService`** — uses `ChatModel` interface; fixed prompt-duplication bug (history loaded before user message persisted)
8. **Added `.gitignore`** — Maven, Gradle, Java, Spring Boot, IntelliJ, VS Code, Claude Code, OS, .env
9. **Written and executed test plan** — 29 tests passing across 5 test classes (VisitorServiceTest, MessageServiceTest, PlannerAiServiceTest, ChatControllerTest, MessageControllerTest)
10. **Written spec** for 5 code-review bug fixes at `docs/superpowers/specs/2026-03-31-code-review-fixes-design.md` (committed)

## Current Project State

```
src/main/java/com/advisorplatform/
├── AdvisorPlatformApplication.java
├── ai/
│   └── PlannerAiService.java
├── api/
│   ├── ChatController.java
│   └── MessageController.java
├── config/
│   └── CorsConfig.java
├── domain/
│   ├── entity/
│   │   ├── Visitor.java
│   │   ├── AiSession.java
│   │   ├── AiMessage.java
│   │   ├── MessageThread.java
│   │   └── ThreadMessage.java
│   └── repository/
│       ├── VisitorRepository.java
│       ├── AiSessionRepository.java
│       ├── AiMessageRepository.java
│       ├── MessageThreadRepository.java
│       └── ThreadMessageRepository.java
└── service/
    ├── VisitorService.java
    └── MessageService.java
```

## Immediate Next Task — Write Implementation Plan then Execute

**Spec file:** `docs/superpowers/specs/2026-03-31-code-review-fixes-design.md`

Use `superpowers:writing-plans` to write the implementation plan for the 5 fixes below, then execute with `superpowers:subagent-driven-development`.

**NOTE:** Do NOT commit spec/plan files until the user has reviewed them. Leave them as uncommitted working tree changes and show the content in the conversation for review.

### The 5 Fixes (from peer code review)

**Fix 1 — Visitor follow-up endpoint**
- Add `POST /api/thread/{threadId}/messages` to `MessageController`
- Calls existing `addMessage(threadId, content)` — senderRole stays "visitor"
- New inline records: `AddMessageRequest(@NotBlank String content)`, `AddMessageResponse(UUID messageId)`
- Tests: new `@WebMvcTest` cases in `MessageControllerTest` (valid → 200 with messageId; blank → 400)

**Fix 2 — `updatedAt` not bumped in `addMessage`**
- In `MessageService.addMessage`, add `thread.setUpdatedAt(Instant.now())` before `threadRepo.save(thread)`
- `MessageThread` has `@Setter` from Lombok so `setUpdatedAt` exists
- Remove misleading comment; replace with accurate one
- Tests: add assertion to `MessageServiceTest.addMessage_knownThread` that `updatedAt` is after a pinned pre-call value

**Fix 3 — Remove `@Transactional` from `streamChat`**
- Remove annotation from `PlannerAiService.streamChat` (it commits before the Flux executes; assistant message save is already auto-transactional via Spring Data)
- Update Javadoc: "User message is persisted immediately; assistant message is persisted in a separate transaction when the stream completes."
- No test change needed — existing `streamChat_knownSession_emitsChunksAndPersistsOnComplete` already verifies persistence

**Fix 4 — Global exception handler**
- New file: `src/main/java/com/advisorplatform/api/GlobalExceptionHandler.java`
- `@RestControllerAdvice` with `@ExceptionHandler(IllegalArgumentException.class)` → 400 + message body
- Tests: new `@WebMvcTest` cases mocking service to throw `IllegalArgumentException`, verifying 400

**Fix 5 — `@NotNull` on `visitorId`**
- `MessageController.CreateThreadRequest`: add `@NotNull` to `UUID visitorId`
- `ChatController.CreateSessionRequest`: add `@NotNull` to `UUID visitorId`
- Tests: new `@WebMvcTest` cases sending `"visitorId": null` → 400

## After These Fixes — Next Feature

**Advisor reply + thread status management** (deferred from this session):
1. `POST /api/thread/{threadId}/reply` — advisor posts a message (`senderRole="advisor"`), thread status → `pending_reply`
2. `PATCH /api/thread/{threadId}/status` — advisor closes/resolves a thread (`status` = `resolved` | `closed`)

This requires:
- New method `advisorReply(UUID threadId, String content)` in `MessageService`
- New method `updateStatus(UUID threadId, String status)` in `MessageService`
- Two new endpoints in `MessageController`

Auth remains Phase 2 — do not add Spring Security yet.

## Live API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET  | `/actuator/health` | Health check |
| POST | `/api/visitor/identify` | Find-or-create visitor by browser token |
| POST | `/api/session` | Create a new AI planning session |
| GET  | `/api/visitor/{visitorId}/sessions` | List sessions for a visitor |
| POST | `/api/chat/{sessionId}` | Non-streaming chat |
| POST | `/api/chat/{sessionId}/stream` | Streaming SSE chat |
| POST | `/api/message` | Create a message thread |
| GET  | `/api/visitor/{visitorId}/threads` | List threads for a visitor |
| GET  | `/api/thread/{threadId}/messages` | List messages in a thread |

## Running the Project

```bash
docker-compose up -d
mvn spring-boot:run
```

Required env vars (copy `.env.example` to `.env`):
- `ANTHROPIC_API_KEY`
- `DB_URL`, `DB_USER`, `DB_PASS`
- `CORS_ORIGINS`

## Conventions (from CLAUDE.md)

- Constructor injection only — no `@Autowired`
- `@Transactional` on service write methods, not controllers
- Records for DTOs (inline in controller)
- `Instant` for all timestamps (UTC)
- Lombok (`@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder`) on JPA entities only
- No Spring Security yet — auth is Phase 2
- Java 21 — use records, sealed interfaces, pattern matching where appropriate
