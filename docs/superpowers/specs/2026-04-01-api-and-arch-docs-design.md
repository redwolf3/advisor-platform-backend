# Design Spec: API Documentation + Architecture Documentation
**Date:** 2026-04-01
**Status:** Approved — pending implementation plan

---

## Problem Statement

The codebase has no machine-readable API contracts and no architectural documentation. This creates friction for:
- Frontend developers consuming the API (no types, no schema)
- New contributors onboarding to the system
- Design review and decision traceability

---

## Task A — API Documentation (OpenAPI Spec-First)

### Approach

Introduce `openapi-generator-maven-plugin` to generate Spring controller interfaces and POJOs from OpenAPI 3 YAML specs stored on the classpath. Implement the generated delegate interfaces by hand.

### Spec Storage

All YAML specs live at `src/main/resources/api/`, one file per domain. They are on the classpath and served by springdoc at runtime.

| Domain | Spec File |
|---|---|
| Visitor + Session | `visitor-session-api.yaml` |
| AI Chat | `ai-chat-api.yaml` |
| Messaging (threads) | `messaging-api.yaml` |
| Advisor (Phase 2) | `advisor-api.yaml` |

### Codegen Configuration

Plugin: `openapi-generator-maven-plugin`
Generator: `spring`
Key options:
- `delegatePattern: true`
- `interfaceOnly: true`
- `useLombokAnnotations: true`
- `useSpringBoot3: true`
- Output to `target/generated-sources/openapi`

Generated sources are never edited directly. The Maven build regenerates them on each compile.

### Generated Source Exclusions

**Git:** `target/` is already excluded by `.gitignore` (standard Maven convention). Verify `target/generated-sources/openapi` is covered; add an explicit entry if needed.

**Code coverage:** Configure the JaCoCo Maven plugin to exclude the generated package from coverage calculations. Add exclusion patterns to the `jacoco-maven-plugin` configuration in `pom.xml`:
```xml
<exclude>com/advisorplatform/generated/**</exclude>
```
The exact package path depends on the `apiPackage` and `modelPackage` codegen options — confirm after first generation and update the exclusion pattern accordingly.

### Delegate Pattern

For each spec, the plugin generates:
- `{Domain}Api` — Spring MVC interface with `@RequestMapping` annotations
- `{Domain}ApiDelegate` — delegate interface with default no-op implementations
- POJOs for all request/response models (Lombok-annotated, not records)

Hand-written delegate implementations live in `src/main/java/com/advisorplatform/api/` and implement `{Domain}ApiDelegate`. The generated controller wires the delegate automatically via constructor injection.

### Migration Path for Phase 1 Controllers

Phase 1 controllers (`ChatController`, `MessageController`, `VisitorController`) were written code-first before this convention existed. The migration approach:

1. Write specs that match the existing endpoints exactly (reverse-engineer, don't redesign)
2. Generate interfaces from the specs
3. Extract logic from existing controllers into new `*ApiDelegate` implementations
4. Delete the hand-written controllers once delegate implementations are verified

This is a one-time migration. All new domains (Phase 2+) start spec-first.

### Swagger UI

Add `springdoc-openapi-starter-webmvc-ui` dependency. Configure `GroupedOpenApi` beans — one per domain — so Swagger UI shows separate tabs at `/swagger-ui.html`.

### Exported Snapshots

After codegen is wired up, export each group's resolved YAML to `docs/api/` for:
- Offline review
- TypeScript client codegen (future)

Files: `docs/api/visitor-session-api.yaml`, `docs/api/ai-chat-api.yaml`, `docs/api/messaging-api.yaml`

---

## Task B — Workflow & Architecture Documentation

### Approach

Mermaid diagrams in Markdown files, plus lightweight ADRs. Serves three audiences: onboarding, design review, and living reference.

### Folder Structure

```
docs/
├── ARCHITECTURE.md              # Entry point: C4 context + links to all diagrams/ADRs
├── architecture/
│   ├── diagrams/
│   │   ├── c4-context.mmd
│   │   ├── visitor-session-flow.mmd
│   │   ├── ai-streaming-pipeline.mmd
│   │   ├── message-threading-workflow.mmd
│   │   └── entity-relationships.mmd
│   ├── ADR-001-session-identity.md
│   ├── ADR-002-ai-streaming.md
│   └── ADR-003-message-threading.md
└── api/                         # Exported OpenAPI YAML snapshots (from Task A)
```

### Diagrams

| File | Diagram Type | Content |
|---|---|---|
| `c4-context.mmd` | C4Context | System boundary: Visitor browser, Advisor browser, Claude API, Postgres |
| `visitor-session-flow.mmd` | Sequence | `POST /identify` → `POST /session` → `POST /chat/{id}` call chain with persistence |
| `ai-streaming-pipeline.mmd` | Sequence | SSE streaming: request → Spring AI → Anthropic API → token stream → save AiMessages |
| `message-threading-workflow.mmd` | Flowchart | Thread state machine: `open` → `pending_reply` → `resolved` / `closed` |
| `entity-relationships.mmd` | ER | Visitor, AiSession, AiMessage, MessageThread, ThreadMessage and FK relationships |

All `.mmd` files are embedded inline in `ARCHITECTURE.md` using fenced code blocks so they render on GitHub without a separate tool.

### ADRs

Each ADR follows the format: **Status / Context / Decision / Consequences** (~1 page).

| File | Decision Captured |
|---|---|
| `ADR-001-session-identity.md` | Why browser token + find-or-create instead of authenticated identity for Phase 1 |
| `ADR-002-ai-streaming.md` | Why SSE for AI chat; how message persistence is timed relative to the stream |
| `ADR-003-message-threading.md` | Why message threads are separate from AI sessions; the role + status model |

### Maintenance

Add to PR checklist (in `CLAUDE.md` or a `CONTRIBUTING.md`): "If the API surface or database schema changed, update the relevant diagram and/or ADR."

---

## Out of Scope

- No interactive API explorer beyond Swagger UI
- No automated diagram rendering pipeline (Mermaid in GitHub Markdown is sufficient)
- No TypeScript client generation in this task (snapshots in `docs/api/` enable it later)
- `advisor-api.yaml` spec is created as a placeholder only — no implementation in Phase 1

---

## Success Criteria

- `mvn compile` succeeds with generated sources; no hand-written controllers reference removed classes
- `/swagger-ui.html` loads with 3 domain tabs (visitor-session, ai-chat, messaging)
- `docs/ARCHITECTURE.md` renders all 5 diagrams correctly on GitHub
- 3 ADRs written and linked from `ARCHITECTURE.md`
- Existing 35 tests continue to pass
