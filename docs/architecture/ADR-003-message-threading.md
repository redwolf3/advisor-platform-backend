# ADR-003: Message Threading — Separate from AI Sessions

**Status:** Accepted
**Date:** 2026-04-01

---

## Context

The platform has two distinct communication patterns:

1. **AI Chat** — real-time, streaming, ephemeral. A visitor talks directly to Claude. Stored in `ai_session` / `ai_message`.
2. **Advisor Messaging** — asynchronous, human-reviewed. A visitor sends a message to a human advisor and waits for a reply.

These could have been modeled as a single "conversation" abstraction, or the advisor could have been added as a participant in the AI chat thread.

## Decision

Model advisor messaging as a completely separate entity hierarchy: `MessageThread` and `ThreadMessage`, independent of `AiSession` and `AiMessage`.

A `MessageThread` has:
- `visitorId` — the visitor who created it
- `aiSessionId` (nullable) — an optional link to the AI session that prompted the inquiry
- `subject` — a short description
- `status` — state machine: `open` → `pending_reply` → `resolved` / `closed`

A `ThreadMessage` has:
- `senderRole` — either `"visitor"` or `"advisor"`
- `content` — the message text

## Consequences

**Positive:**
- Clean separation of concerns — AI chat and human messaging evolve independently
- No risk of AI responses appearing in human advisor threads or vice versa
- Status field enables advisor workflow (filter by `open`, sort by `updated_at`)
- The nullable `aiSessionId` link allows context-passing ("I was talking to the AI about X, here is my follow-up question") without coupling the models

**Negative:**
- Two separate data models to maintain instead of one unified conversation model
- Visitors cannot (currently) reference specific AI messages in their advisor thread — only the session as a whole

**Alternatives considered:**
- Unified conversation model: rejected because it would complicate the AI context window (advisor messages should not be sent to Claude) and the advisor inbox (AI messages should not appear there)
- Adding advisor as an AI participant: rejected because it conflates human and AI roles in the same message stream
