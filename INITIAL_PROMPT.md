# Disney Planner Backend — Claude Code Handoff

## Project Overview

A Spring Boot backend for a Disneyland vacation planning site. Phase 1 goal:
anonymous visitor identity, AI chat with session history, human contact messaging.
This is a learning/portfolio project — full stack with React frontend in a separate repo.

## What Has Been Designed and Partially Scaffolded

### Tech Stack
- Java 21, Spring Boot 3.4, Maven
- Spring AI (`spring-ai-anthropic-spring-boot-starter`) for Claude API integration
- PostgreSQL + Flyway migrations
- Lombok
- jjwt for future auth token verification

### Package Structure
```
com.disneyplanner
├── DisneyPlannerApplication.java
├── ai/
│   └── PlannerAiService.java        ← Spring AI streaming + history + persistence
├── api/
│   └── ChatController.java          ← REST endpoints (visitor, session, chat)
├── config/
│   └── CorsConfig.java              ← CORS from app.cors.allowed-origins
├── domain/
│   ├── entity/                      ← JPA entities (use these, not domain/model/)
│   │   ├── Visitor.java
│   │   ├── AiSession.java
│   │   └── AiMessage.java
│   └── repository/
│       ├── VisitorRepository.java
│       ├── AiSessionRepository.java
│       └── AiMessageRepository.java
└── service/
    └── VisitorService.java          ← visitor find-or-create, session management
```

### Known Issues To Fix First
1. **Duplicate entity packages** — there are both `domain/entity/` and `domain/model/`
   directories with overlapping classes. The `domain/entity/` versions are correct
   (they're used by the service and controller). DELETE `domain/model/` entirely.

2. **Duplicate migrations** — both `V1__init.sql` and `V1__initial_schema.sql` exist.
   Keep `V1__init.sql` (it is the correct, complete schema). Delete `V1__initial_schema.sql`.

3. **Repositories.java** — delete `domain/repository/Repositories.java` (it's a leftover
   multi-interface file). The individual repository files are correct.

4. **Missing entities** — `MessageThread` and `ThreadMessage` entities exist in
   `domain/model/` but NOT in `domain/entity/`. Create them in `domain/entity/` to
   match the schema in `V1__init.sql`.

5. **Missing `app.ai.*` config** — `PlannerAiService` references
   `${app.ai.system-prompt-path}` and `${app.ai.max-history-turns}`.
   Add these to `application.yml`:
   ```yaml
   app:
     ai:
       system-prompt-path: prompts/system.md
       max-history-turns: 10
   ```

6. **AiMessage factory methods** — `PlannerAiService` calls `AiMessage.userMessage()`
   and `AiMessage.assistantMessage()` static factory methods. These must be added
   to the `AiMessage` entity if not already present.

## What Still Needs To Be Built

### Backend (complete these in order)

1. **Fix the issues above** before adding anything new.

2. **`MessageThread` and `ThreadMessage` entities** in `domain/entity/` matching
   the schema — status enum: open/pending_reply/resolved/closed,
   sender_role: visitor/advisor.

3. **`MessageThreadRepository` and `ThreadMessageRepository`** in `domain/repository/`.

4. **`MessageService`** in `service/` — create thread linked to an ai_session,
   add visitor message, list threads for a visitor, list messages in a thread.

5. **`MessageController`** in `api/` — endpoints:
   - `POST /api/message` — visitor sends a message (creates thread if needed)
   - `GET /api/visitor/{visitorId}/threads` — list visitor's threads
   - `GET /api/thread/{threadId}/messages` — get messages in a thread

6. **`docker-compose.yml`** at project root — PostgreSQL only, port 5432,
   database name `disneyplanner`, user `disneyplanner`, password `disneyplanner`.

7. **`.env.example`** — document required environment variables:
   - `ANTHROPIC_API_KEY`
   - `DB_URL`, `DB_USER`, `DB_PASS`
   - `CORS_ORIGINS`

8. **`README.md`** — local dev setup: docker-compose, mvn spring-boot:run, env vars.

### What Is Intentionally Deferred (Phase 2)
- Auth (OTP, magic link, session tokens) — auth_otp and visitor_session tables
  exist in schema but no backend logic yet
- Email sending
- Admin review UI for AI message quality tagging
- Knowledge base RAG (pgvector)
- Stripe / payments

## Data Model Reference

See `src/main/resources/db/migration/V1__init.sql` for the authoritative schema.

Key relationships:
- `visitor` → has many `ai_session`
- `ai_session` → has many `ai_message` (role: user|assistant)
- `visitor` → has many `message_thread`
- `message_thread` → optionally linked to `ai_session` (the conversation that triggered it)
- `message_thread` → has many `thread_message` (sender_role: visitor|advisor)

## AI Integration Notes

`PlannerAiService` uses Spring AI's `AnthropicChatModel`:
- Loads system prompt from `src/main/resources/prompts/system.md`
- Builds conversation history from `ai_message` table (capped at `max-history-turns` pairs)
- Streaming via `chatModel.stream()` → `Flux<String>` → SSE endpoint
- Non-streaming `chatModel.call()` for testing
- Persists both user and assistant messages with latency and model version

The system prompt is at `src/main/resources/prompts/system.md` — it contains
opinionated Disneyland planning advice and defines the assistant's personality.
Do not modify it without instruction.

## API Contract (for frontend alignment)

```
POST /api/visitor/identify       { browserToken: string }
                                 → { visitorId, browserToken }

POST /api/session                { visitorId: UUID }
                                 → { sessionId, createdAt }

GET  /api/visitor/:id/sessions   → [{ sessionId, createdAt }]

POST /api/chat/:sessionId        { message: string }
                                 → { messageId, content, model, createdAt }

POST /api/chat/:sessionId/stream { message: string }
                                 → text/event-stream (SSE chunks)

POST /api/message                { visitorId, aiSessionId?, subject?, content }
                                 → { threadId }

GET  /api/visitor/:id/threads    → [{ threadId, subject, status, updatedAt }]

GET  /api/thread/:id/messages    → [{ messageId, senderRole, content, createdAt }]
```

## Conventions

- No `@Autowired` — constructor injection only
- Records for DTOs (inline in controller for now, extract if they grow)
- `@Transactional` on service methods that write
- All timestamps are `Instant` (UTC), mapped to `TIMESTAMPTZ`
- UUIDs as primary keys, generated by Postgres (`gen_random_uuid()`)
- Flyway manages all schema changes — never modify existing migration files
