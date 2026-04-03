# ADR-002: AI Streaming — SSE with Post-Stream Persistence

**Status:** Accepted
**Date:** 2026-04-01

---

## Context

The AI chat feature calls the Anthropic Claude API, which can take 5-30 seconds to generate a full response. Waiting for the full response before returning to the browser creates a poor user experience — the UI appears frozen.

We need a streaming strategy that:
1. Shows tokens to the user as they arrive
2. Persists the full conversation turn to the database
3. Maintains the conversation history for multi-turn context

## Decision

Use Server-Sent Events (SSE) for streaming. The `/api/v1/chat/{sessionId}/stream` endpoint returns `Content-Type: text/event-stream` and yields text tokens via a `Flux<String>` backed by Spring AI's `ChatModel.stream()`.

**Persistence timing:**
- The user message is persisted **before** the stream begins (so history is correct on retry)
- The assistant message is persisted **after** the stream completes, using `Flux.doOnComplete()`
- The full assistant content is accumulated in a `StringBuilder` during streaming

**History loading:**
- Message history is loaded **before** the user message is persisted to avoid the user's new message appearing twice in the context window (once in history, once as the final UserMessage)

## Consequences

**Positive:**
- Perceived responsiveness is greatly improved — users see text immediately
- Spring AI + Project Reactor (`Flux`) handle backpressure and cancellation natively
- Simple implementation — no WebSocket, no custom SSE infrastructure

**Negative:**
- If the client disconnects mid-stream, `doOnComplete()` may not fire — the assistant message may not be persisted
- Token count is unavailable during streaming (the Anthropic stream metadata arrives at the end); currently stored as 0 for stream responses
- Non-streaming (`POST /api/v1/chat/{sessionId}`) is maintained as a fallback for testing and clients that don't support SSE

**Future improvement:** Use `doOnNext()` + a final `doOnComplete()` callback with the accumulated tokens metadata if the Anthropic SDK exposes it in the stream completion event.
