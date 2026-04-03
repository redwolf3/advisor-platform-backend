# Advisor Platform — Architecture

This document is the entry point for understanding the system. It contains the high-level context diagram, links to detailed sequence and flow diagrams, and links to Architecture Decision Records (ADRs).

---

## System Context

```mermaid
C4Context
    title System Context — Advisor Platform

    Person(visitor, "Visitor", "Website visitor seeking travel planning advice")
    Person(advisor, "Advisor", "Travel advisor reviewing visitor inquiries")

    System_Boundary(platform, "Advisor Platform") {
        System(api, "Backend API", "Spring Boot 3.4 — handles AI chat, visitor identity, and advisor messaging")
    }

    System_Ext(anthropic, "Anthropic API", "Claude AI model — generates travel advice responses")
    SystemDb_Ext(postgres, "PostgreSQL 16", "Stores visitors, AI sessions, messages, and threads")

    Rel(visitor, api, "Identifies, chats with AI, sends advisor messages", "HTTPS/JSON + SSE")
    Rel(advisor, api, "Reviews threads, replies to visitors", "HTTPS/JSON")
    Rel(api, anthropic, "Streams AI completions", "HTTPS")
    Rel(api, postgres, "Reads and writes data", "JDBC/Flyway")
```

---

## Visitor Identity + Session Flow

```mermaid
sequenceDiagram
    participant Browser
    participant API as Advisor Platform API
    participant DB as PostgreSQL

    Note over Browser,DB: Page load — visitor identification

    Browser->>API: POST /api/v1/visitor/identify {browserToken}
    API->>DB: SELECT * FROM visitor WHERE browser_token = ?
    alt Visitor exists
        DB-->>API: Visitor row
        API->>DB: UPDATE visitor SET last_seen_at = now()
    else New visitor
        API->>DB: INSERT INTO visitor (browser_token)
        DB-->>API: New Visitor row
    end
    API-->>Browser: {visitorId, browserToken}

    Note over Browser,DB: Session creation

    Browser->>API: POST /api/v1/session {visitorId}
    API->>DB: SELECT * FROM visitor WHERE id = ?
    DB-->>API: Visitor row
    API->>DB: INSERT INTO ai_session (visitor_id)
    DB-->>API: AiSession row
    API-->>Browser: {sessionId, createdAt}

    Note over Browser,DB: Non-streaming chat

    Browser->>API: POST /api/v1/chat/{sessionId} {message}
    API->>DB: SELECT * FROM ai_message WHERE session_id = ? ORDER BY created_at
    DB-->>API: Message history
    API->>DB: INSERT INTO ai_message (session_id, role='user', content)
    API->>+Anthropic: Messages API call (history + new message)
    Anthropic-->>-API: Assistant response
    API->>DB: INSERT INTO ai_message (session_id, role='assistant', content, model, tokens)
    API-->>Browser: {messageId, content, model, createdAt}
```

---

## AI Streaming Pipeline

```mermaid
sequenceDiagram
    participant Browser
    participant API as Advisor Platform API
    participant SpringAI as Spring AI (ChatModel)
    participant Anthropic as Anthropic API
    participant DB as PostgreSQL

    Browser->>API: POST /api/v1/chat/{sessionId}/stream {message}
    Note over API: SSE connection established (text/event-stream)

    API->>DB: SELECT * FROM ai_message WHERE session_id = ? ORDER BY created_at
    DB-->>API: Message history (up to maxHistoryTurns window)

    API->>DB: INSERT INTO ai_message (role='user', content)
    DB-->>API: Saved

    API->>SpringAI: ChatModel.stream(systemPrompt + history + userMessage)
    SpringAI->>Anthropic: POST /v1/messages (streaming: true)

    loop Token stream
        Anthropic-->>SpringAI: data: {delta: {text: "chunk"}}
        SpringAI-->>API: Flux<ChatResponse> chunk
        API-->>Browser: data: chunk\n\n
    end

    Anthropic-->>SpringAI: data: [DONE]
    SpringAI-->>API: Flux complete (doOnComplete triggered)

    API->>DB: INSERT INTO ai_message (role='assistant', full content, model, latency_ms)
    Note over Browser: SSE connection closes
```

---

## Message Threading Workflow

> Dashed nodes indicate Phase 2 endpoints not yet implemented.

```mermaid
flowchart TD
    A([Visitor submits initial message]) --> B

    B["POST /api/v1/message\n─────────────────\nCreates MessageThread\nstatus = 'open'\nCreates ThreadMessage\nsenderRole = 'visitor'"]

    B --> C{Advisor action}

    C -->|View thread| D["GET /api/v1/thread/:id/messages\nReturns all messages"]
    D --> C

    C -->|Reply ➜ Phase 2| E["POST /api/v1/thread/:id/reply\nAdds ThreadMessage\nsenderRole = 'advisor'\nstatus → 'pending_reply'"]
    E --> F{Visitor follow-up?}

    F -->|Yes| G["POST /api/v1/thread/:id/messages\nAdds ThreadMessage\nsenderRole = 'visitor'\nstatus → 'open'"]
    G --> C

    F -->|No| C

    C -->|Resolve ➜ Phase 2| H["PATCH /api/v1/thread/:id/status\nstatus = 'resolved'"]
    C -->|Close ➜ Phase 2| I["PATCH /api/v1/thread/:id/status\nstatus = 'closed'"]

    H --> J([Thread archived])
    I --> J

    style E stroke-dasharray: 5 5
    style H stroke-dasharray: 5 5
    style I stroke-dasharray: 5 5
```

---

## Entity Relationships

```mermaid
erDiagram
    VISITOR {
        uuid id PK
        varchar browser_token UK
        varchar email UK
        boolean email_verified
        timestamptz tos_accepted_at
        varchar tos_version
        timestamptz created_at
        timestamptz last_seen_at
    }

    AI_SESSION {
        uuid id PK
        uuid visitor_id FK
        timestamptz created_at
    }

    AI_MESSAGE {
        uuid id PK
        uuid session_id FK
        varchar role
        text content
        varchar model_version
        int token_count
        int latency_ms
        timestamptz created_at
    }

    MESSAGE_THREAD {
        uuid id PK
        uuid visitor_id FK
        uuid ai_session_id FK
        varchar subject
        varchar status
        timestamptz created_at
        timestamptz updated_at
    }

    THREAD_MESSAGE {
        uuid id PK
        uuid thread_id FK
        varchar sender_role
        text content
        timestamptz created_at
    }

    VISITOR ||--o{ AI_SESSION : "has"
    AI_SESSION ||--o{ AI_MESSAGE : "contains"
    VISITOR ||--o{ MESSAGE_THREAD : "owns"
    AI_SESSION |o--o{ MESSAGE_THREAD : "optionally linked to"
    MESSAGE_THREAD ||--o{ THREAD_MESSAGE : "contains"
```

---

## Architecture Decision Records

| ADR | Decision |
|---|---|
| [ADR-001](architecture/ADR-001-session-identity.md) | Session identity: browser token + find-or-create instead of authenticated users |
| [ADR-002](architecture/ADR-002-ai-streaming.md) | AI streaming: SSE with post-stream persistence |
| [ADR-003](architecture/ADR-003-message-threading.md) | Message threading: separate from AI sessions, visitor/advisor role model |

---

## Package Structure

```
src/main/java/com/advisorplatform/
├── api/           ApiDelegate implementations (HTTP layer)
├── service/       Business logic and orchestration
├── domain/
│   ├── entity/    JPA entities
│   └── repository/ Spring Data repositories
├── ai/            Spring AI + Anthropic integration
└── config/        Spring configuration beans

src/main/resources/api/   OpenAPI 3 spec files (one per domain, versioned)
target/generated-sources/ Generated Spring interfaces + POJOs — do not edit
```

---

## Maintenance

When a PR changes the API surface or database schema, update the relevant diagram and/or ADR as part of the same PR.
