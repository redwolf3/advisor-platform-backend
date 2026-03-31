# Code Review Fixes — Design Spec

**Date:** 2026-03-31
**Scope:** Address 5 bugs identified in peer code review. No new features beyond the visitor follow-up endpoint needed to close the addMessage dead-code gap.

---

## Issues Being Fixed

| # | Issue | File(s) |
|---|-------|---------|
| 1 | `addMessage` has no HTTP endpoint; visitor cannot follow up on a thread | `MessageController`, `MessageService` |
| 2 | `@PreUpdate` never fires in `addMessage` — `updatedAt` not bumped after new message | `MessageService` |
| 3 | `@Transactional` on `streamChat` is a no-op; misleads about durability | `PlannerAiService` |
| 4 | `IllegalArgumentException` from services surfaces as HTTP 500 (no handler) | new `GlobalExceptionHandler` |
| 5 | `@NotNull` missing on `visitorId` in two request records — null bypasses validation | `MessageController`, `ChatController` |

---

## Fix 1: Visitor Follow-Up Endpoint

**What:** Add `POST /api/thread/{threadId}/messages` to `MessageController`.

**Endpoint:**
```
POST /api/thread/{threadId}/messages
Body: { "content": "..." }
Response 200: { "messageId": "<uuid>" }
Response 400: blank content
```

**Implementation:**
- Add `@PostMapping("/thread/{threadId}/messages")` handler calling `messageService.addMessage(threadId, req.content())`
- Add inline records:
  - `record AddMessageRequest(@NotBlank String content) {}`
  - `record AddMessageResponse(UUID messageId) {}`
- `addMessage` already hardcodes `senderRole = "visitor"` — correct, no change needed there
- Annotate the `@PathVariable UUID threadId` parameter (no validation annotation needed — Spring parses UUID from path; malformed UUIDs already return 400)

**No changes to `MessageService.addMessage` signature.**

---

## Fix 2: `updatedAt` Not Bumped in `addMessage`

**Problem:** `threadRepo.save(thread)` is called on an entity with no mutated fields. Hibernate dirty-checking skips the SQL UPDATE, so `@PreUpdate` never fires and `updatedAt` stays at thread-creation time. `getThreads` orders by `updatedAt DESC`, so new messages don't reorder threads.

**Fix:** Explicitly set `updatedAt` before saving, making the entity dirty:

```java
thread.setUpdatedAt(Instant.now());
threadRepo.save(thread);
```

Remove the misleading comment `// Bump updatedAt via @PreUpdate` — replace with `// Dirty the entity so Hibernate issues UPDATE`.

`MessageThread` has `@Setter` from Lombok so `setUpdatedAt` exists.

---

## Fix 3: Remove `@Transactional` from `streamChat`

**Problem:** `@Transactional` on a method returning `Flux<String>` commits the transaction when the method returns — before the reactive stream executes. The `doOnComplete` persistence runs outside the original transaction on a reactor thread. The annotation is a no-op at best, and misleading about durability at worst.

**Fix:** Remove `@Transactional` from `PlannerAiService.streamChat`. Spring Data repositories provide their own per-operation transactions, so the two `messageRepository.save` calls (user message in method body, assistant message in `doOnComplete`) each get their own transaction automatically.

**Javadoc update:** Change "Persistence of the full response happens when the stream completes" to "User message is persisted immediately; assistant message is persisted in a separate transaction when the stream completes."

No change to `chat()` — it is fully synchronous and `@Transactional` is correct there.

---

## Fix 4: Global Exception Handler

**Problem:** All three services throw `IllegalArgumentException` for not-found inputs. No `@RestControllerAdvice` exists, so Spring Boot maps these to HTTP 500.

**Fix:** New file `src/main/java/com/advisorplatform/api/GlobalExceptionHandler.java`:

```java
package com.advisorplatform.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<String> handleBadArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}
```

- Package: `api/` — consistent with project package conventions
- Returns 400 Bad Request with the exception message as plain-text body
- Covers all existing services (`VisitorService`, `MessageService`, `PlannerAiService`)
- No Spring Security involved

---

## Fix 5: `@NotNull` on `visitorId`

**Problem:** `UUID visitorId` in two request records has no `@NotNull`, so `"visitorId": null` passes `@Valid` and causes a 500.

**Fix:** Add `@NotNull` to both records:

- `MessageController.CreateThreadRequest`: `@NotNull UUID visitorId`
- `ChatController.CreateSessionRequest`: `@NotNull UUID visitorId`

Jakarta Validation is already on the classpath (used for `@NotBlank`). `@Valid` is already present on both method parameters. No additional imports needed beyond `jakarta.validation.constraints.NotNull`.

---

## Testing

Each fix gets a targeted test addition or update:

| Fix | Test change |
|-----|-------------|
| 1 (endpoint) | New `@WebMvcTest` case in `MessageControllerTest`: valid content → 200 with messageId; blank content → 400 |
| 2 (`updatedAt`) | New assertion in `MessageServiceTest.addMessage_knownThread`: verify `thread.getUpdatedAt()` is after a pinned pre-call value (same pattern as `VisitorServiceTest.findOrCreate_existingVisitor`) |
| 3 (`@Transactional`) | No test change — the annotation removal is structural; existing `PlannerAiServiceTest.streamChat_knownSession` already verifies persistence |
| 4 (handler) | Two new `@WebMvcTest` cases in `ChatControllerTest` or `MessageControllerTest`: mock service to throw `IllegalArgumentException`, verify 400 response |
| 5 (`@NotNull`) | New `@WebMvcTest` cases: `POST /api/thread/{threadId}/messages` with null content already covered; add null-visitorId cases for createThread and createSession → 400 |

---

## What Is NOT Changing

- No advisor reply endpoint (`POST /api/thread/{threadId}/reply`) — planned for next session
- No thread status management (`PATCH /api/thread/{threadId}/status`) — planned for next session
- No Spring Security
- No Flyway migrations (no schema changes)
- No changes to `addMessage` signature or senderRole behavior
