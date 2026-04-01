# Code Review Fixes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix 5 bugs identified in peer code review: missing HTTP endpoint for addMessage, updatedAt not bumped on new messages, @Transactional no-op on streamChat, missing global exception handler for IllegalArgumentException, and missing @NotNull on visitorId fields.

**Architecture:** Fixes are self-contained: one service mutation (updatedAt), one annotation removal (streamChat), one new file (GlobalExceptionHandler), two annotation additions (@NotNull), and one new controller endpoint (addMessage). All follow existing patterns — no new beans, no schema changes.

**Tech Stack:** Java 21, Spring Boot, Spring MVC, Jakarta Validation, Mockito/JUnit 5, MockMvc (@WebMvcTest)

---

## File Map

| Fix | Files Modified | Files Created |
|-----|----------------|---------------|
| 2 | `service/MessageService.java`, `test/.../MessageServiceTest.java` | — |
| 3 | `ai/PlannerAiService.java` | — |
| 4 | `test/.../MessageControllerTest.java`, `test/.../ChatControllerTest.java` | `api/GlobalExceptionHandler.java` |
| 5 | `api/MessageController.java`, `api/ChatController.java`, `test/.../MessageControllerTest.java`, `test/.../ChatControllerTest.java` | — |
| 1 | `api/MessageController.java`, `test/.../MessageControllerTest.java` | — |

---

### Task 1: Fix updatedAt Not Bumped in addMessage

**Files:**
- Modify: `src/test/java/com/advisorplatform/service/MessageServiceTest.java`
- Modify: `src/main/java/com/advisorplatform/service/MessageService.java`

- [ ] **Step 1: Add failing assertion to the existing addMessage test**

  Open `src/test/java/com/advisorplatform/service/MessageServiceTest.java`.

  Replace the existing `addMessage_knownThread_savesVisitorMessageAndBumpsThread` test with this version that also pins `updatedAt` and asserts it was bumped:

  ```java
  @Test
  void addMessage_knownThread_savesVisitorMessageAndBumpsThread() {
      UUID threadId = UUID.randomUUID();
      MessageThread thread = MessageThread.builder().build();
      Instant oldTime = Instant.parse("2020-01-01T00:00:00Z");
      thread.setUpdatedAt(oldTime); // pin to a known past value so we can detect the bump
      when(threadRepo.findById(threadId)).thenReturn(Optional.of(thread));
      ThreadMessage saved = ThreadMessage.builder()
              .thread(thread).senderRole("visitor").content("More info").build();
      when(messageRepo.save(any(ThreadMessage.class))).thenReturn(saved);
      when(threadRepo.save(thread)).thenReturn(thread);

      ThreadMessage result = messageService.addMessage(threadId, "More info");

      assertThat(result).isSameAs(saved);
      verify(messageRepo).save(argThat(m ->
              "visitor".equals(m.getSenderRole()) && "More info".equals(m.getContent())));
      // updatedAt must have been explicitly set before threadRepo.save
      assertThat(thread.getUpdatedAt()).isAfter(oldTime);
      verify(threadRepo).save(thread);
  }
  ```

  Add `import java.time.Instant;` if not present (it's already in the test file via `MessageThread` usage — but add it explicitly at the top if the compiler complains).

- [ ] **Step 2: Run to confirm the new assertion fails**

  ```bash
  mvn test -pl . -Dtest=MessageServiceTest#addMessage_knownThread_savesVisitorMessageAndBumpsThread -q
  ```

  Expected: FAIL — assertion error on `thread.getUpdatedAt()` because the service does not yet set it.

- [ ] **Step 3: Fix MessageService.addMessage — explicitly set updatedAt before save**

  Open `src/main/java/com/advisorplatform/service/MessageService.java`.

  Replace the `addMessage` method body:

  ```java
  @Transactional
  public ThreadMessage addMessage(UUID threadId, String content) {
      MessageThread thread = threadRepo.findById(threadId)
              .orElseThrow(() -> new IllegalArgumentException("Thread not found: " + threadId));

      ThreadMessage message = ThreadMessage.builder()
              .thread(thread)
              .senderRole("visitor")
              .content(content)
              .build();
      message = messageRepo.save(message);

      // Dirty the entity so Hibernate issues UPDATE and updatedAt is refreshed
      thread.setUpdatedAt(Instant.now());
      threadRepo.save(thread);

      return message;
  }
  ```

  Add `import java.time.Instant;` at the top of the file (it may already be present — check the imports).

- [ ] **Step 4: Run all MessageService tests to confirm they pass**

  ```bash
  mvn test -pl . -Dtest=MessageServiceTest -q
  ```

  Expected: all 8 tests pass (BUILD SUCCESS).

- [ ] **Step 5: Commit**

  ```bash
  git add src/main/java/com/advisorplatform/service/MessageService.java \
          src/test/java/com/advisorplatform/service/MessageServiceTest.java
  git commit -m "fix: explicitly set updatedAt in addMessage so Hibernate issues UPDATE"
  ```

---

### Task 2: Remove @Transactional from streamChat

**Files:**
- Modify: `src/main/java/com/advisorplatform/ai/PlannerAiService.java`

No test changes — the existing `PlannerAiServiceTest.streamChat_knownSession_emitsChunksAndPersistsOnComplete` already verifies persistence.

- [ ] **Step 1: Remove @Transactional and update Javadoc in streamChat**

  Open `src/main/java/com/advisorplatform/ai/PlannerAiService.java`.

  Replace the `streamChat` method's annotation and Javadoc block:

  ```java
  /**
   * Streaming version — returns a Flux of text chunks for SSE.
   * User message is persisted immediately; assistant message is persisted in a
   * separate transaction when the stream completes.
   */
  public Flux<String> streamChat(UUID sessionId, String userContent) {
  ```

  (Remove the `@Transactional` annotation line entirely; keep everything else in the method body unchanged.)

  Also remove the `import org.springframework.transaction.annotation.Transactional;` line **only if** it is no longer used — `chat()` still has `@Transactional`, so the import remains needed.

- [ ] **Step 2: Run PlannerAiService tests to confirm nothing regressed**

  ```bash
  mvn test -pl . -Dtest=PlannerAiServiceTest -q
  ```

  Expected: all tests pass (BUILD SUCCESS).

- [ ] **Step 3: Commit**

  ```bash
  git add src/main/java/com/advisorplatform/ai/PlannerAiService.java
  git commit -m "fix: remove @Transactional from streamChat — annotation is a no-op on Flux return type"
  ```

---

### Task 3: Global Exception Handler for IllegalArgumentException

**Files:**
- Create: `src/main/java/com/advisorplatform/api/GlobalExceptionHandler.java`
- Modify: `src/test/java/com/advisorplatform/api/MessageControllerTest.java`
- Modify: `src/test/java/com/advisorplatform/api/ChatControllerTest.java`

- [ ] **Step 1: Write failing test in MessageControllerTest**

  Open `src/test/java/com/advisorplatform/api/MessageControllerTest.java`.

  Add this test method after the existing `createThread_blankContent_returns400` test:

  ```java
  @Test
  void createThread_serviceThrowsIllegalArgument_returns400() throws Exception {
      when(messageService.createThread(any(), any(), any(), any()))
              .thenThrow(new IllegalArgumentException("Visitor not found: some-id"));

      mockMvc.perform(post("/api/message")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content("{\"visitorId\":\"" + UUID.randomUUID() + "\","
                              + "\"content\":\"Hello\"}"))
              .andExpect(status().isBadRequest());
  }
  ```

- [ ] **Step 2: Write failing test in ChatControllerTest**

  Open `src/test/java/com/advisorplatform/api/ChatControllerTest.java`.

  Add this test method after the existing `createSession_validVisitorId_returns200WithSessionId` test:

  ```java
  @Test
  void createSession_serviceThrowsIllegalArgument_returns400() throws Exception {
      when(visitorService.createSession(any()))
              .thenThrow(new IllegalArgumentException("Visitor not found: some-id"));

      mockMvc.perform(post("/api/session")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content("{\"visitorId\":\"" + UUID.randomUUID() + "\"}"))
              .andExpect(status().isBadRequest());
  }
  ```

- [ ] **Step 3: Run the two new tests to confirm they fail**

  ```bash
  mvn test -pl . -Dtest="MessageControllerTest#createThread_serviceThrowsIllegalArgument_returns400+ChatControllerTest#createSession_serviceThrowsIllegalArgument_returns400" -q
  ```

  Expected: FAIL — both tests get 500 (no handler yet).

- [ ] **Step 4: Create GlobalExceptionHandler**

  Create `src/main/java/com/advisorplatform/api/GlobalExceptionHandler.java`:

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

- [ ] **Step 5: Run both controller test classes to confirm all tests pass**

  ```bash
  mvn test -pl . -Dtest="MessageControllerTest,ChatControllerTest" -q
  ```

  Expected: all tests pass (BUILD SUCCESS).

- [ ] **Step 6: Commit**

  ```bash
  git add src/main/java/com/advisorplatform/api/GlobalExceptionHandler.java \
          src/test/java/com/advisorplatform/api/MessageControllerTest.java \
          src/test/java/com/advisorplatform/api/ChatControllerTest.java
  git commit -m "fix: add GlobalExceptionHandler — map IllegalArgumentException to 400"
  ```

---

### Task 4: @NotNull on visitorId in Request Records

**Files:**
- Modify: `src/main/java/com/advisorplatform/api/MessageController.java`
- Modify: `src/main/java/com/advisorplatform/api/ChatController.java`
- Modify: `src/test/java/com/advisorplatform/api/MessageControllerTest.java`
- Modify: `src/test/java/com/advisorplatform/api/ChatControllerTest.java`

- [ ] **Step 1: Write failing test for null visitorId in MessageControllerTest**

  Open `src/test/java/com/advisorplatform/api/MessageControllerTest.java`.

  Add this test after `createThread_serviceThrowsIllegalArgument_returns400`:

  ```java
  @Test
  void createThread_nullVisitorId_returns400() throws Exception {
      mockMvc.perform(post("/api/message")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content("{\"visitorId\":null,\"content\":\"Hello\"}"))
              .andExpect(status().isBadRequest());
  }
  ```

- [ ] **Step 2: Write failing test for null visitorId in ChatControllerTest**

  Open `src/test/java/com/advisorplatform/api/ChatControllerTest.java`.

  Add this test after `createSession_serviceThrowsIllegalArgument_returns400`:

  ```java
  @Test
  void createSession_nullVisitorId_returns400() throws Exception {
      mockMvc.perform(post("/api/session")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content("{\"visitorId\":null}"))
              .andExpect(status().isBadRequest());
  }
  ```

- [ ] **Step 3: Run the two new tests to confirm they fail**

  ```bash
  mvn test -pl . -Dtest="MessageControllerTest#createThread_nullVisitorId_returns400+ChatControllerTest#createSession_nullVisitorId_returns400" -q
  ```

  Expected: FAIL — both tests get 200 (null passes @Valid without @NotNull).

- [ ] **Step 4: Add @NotNull to CreateThreadRequest in MessageController**

  Open `src/main/java/com/advisorplatform/api/MessageController.java`.

  Replace the `CreateThreadRequest` record:

  ```java
  record CreateThreadRequest(@NotNull UUID visitorId, UUID aiSessionId, String subject, @NotBlank String content) {}
  ```

  Add `import jakarta.validation.constraints.NotNull;` to the imports.

- [ ] **Step 5: Add @NotNull to CreateSessionRequest in ChatController**

  Open `src/main/java/com/advisorplatform/api/ChatController.java`.

  Replace the `CreateSessionRequest` record:

  ```java
  record CreateSessionRequest(@NotNull UUID visitorId) {}
  ```

  Add `import jakarta.validation.constraints.NotNull;` to the imports.

- [ ] **Step 6: Run both controller test classes to confirm all tests pass**

  ```bash
  mvn test -pl . -Dtest="MessageControllerTest,ChatControllerTest" -q
  ```

  Expected: all tests pass (BUILD SUCCESS).

- [ ] **Step 7: Commit**

  ```bash
  git add src/main/java/com/advisorplatform/api/MessageController.java \
          src/main/java/com/advisorplatform/api/ChatController.java \
          src/test/java/com/advisorplatform/api/MessageControllerTest.java \
          src/test/java/com/advisorplatform/api/ChatControllerTest.java
  git commit -m "fix: add @NotNull to visitorId in CreateThreadRequest and CreateSessionRequest"
  ```

---

### Task 5: Visitor Follow-Up Endpoint POST /api/thread/{threadId}/messages

**Files:**
- Modify: `src/main/java/com/advisorplatform/api/MessageController.java`
- Modify: `src/test/java/com/advisorplatform/api/MessageControllerTest.java`

- [ ] **Step 1: Write failing tests for the new endpoint**

  Open `src/test/java/com/advisorplatform/api/MessageControllerTest.java`.

  Add these two test methods after `createThread_nullVisitorId_returns400`:

  ```java
  // ── POST /api/thread/{threadId}/messages ─────────────────────────────────

  @Test
  void addMessage_validContent_returns200WithMessageId() throws Exception {
      UUID threadId = UUID.randomUUID();
      UUID messageId = UUID.randomUUID();
      ThreadMessage message = ThreadMessage.builder()
              .thread(MessageThread.builder().visitor(new Visitor()).build())
              .senderRole("visitor")
              .content("Follow up question")
              .build();
      ReflectionTestUtils.setField(message, "id", messageId);
      when(messageService.addMessage(eq(threadId), eq("Follow up question"))).thenReturn(message);

      mockMvc.perform(post("/api/thread/{threadId}/messages", threadId)
                      .contentType(MediaType.APPLICATION_JSON)
                      .content("{\"content\":\"Follow up question\"}"))
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.messageId").value(messageId.toString()));
  }

  @Test
  void addMessage_blankContent_returns400() throws Exception {
      mockMvc.perform(post("/api/thread/{threadId}/messages", UUID.randomUUID())
                      .contentType(MediaType.APPLICATION_JSON)
                      .content("{\"content\":\"\"}"))
              .andExpect(status().isBadRequest());
  }
  ```

- [ ] **Step 2: Run new tests to confirm they fail**

  ```bash
  mvn test -pl . -Dtest="MessageControllerTest#addMessage_validContent_returns200WithMessageId+MessageControllerTest#addMessage_blankContent_returns400" -q
  ```

  Expected: FAIL — 404 (no route exists yet).

- [ ] **Step 3: Add the endpoint and new records to MessageController**

  Open `src/main/java/com/advisorplatform/api/MessageController.java`.

  Add this handler method after the `getMessages` method (before the records section):

  ```java
  /** Add a follow-up visitor message to an existing thread. */
  @PostMapping("/thread/{threadId}/messages")
  public ResponseEntity<AddMessageResponse> addMessage(
          @PathVariable UUID threadId,
          @Valid @RequestBody AddMessageRequest req) {
      ThreadMessage message = messageService.addMessage(threadId, req.content());
      return ResponseEntity.ok(new AddMessageResponse(message.getId()));
  }
  ```

  Add these two records to the records section at the bottom of the class:

  ```java
  record AddMessageRequest(@NotBlank String content) {}
  record AddMessageResponse(UUID messageId) {}
  ```

- [ ] **Step 4: Run all MessageController tests**

  ```bash
  mvn test -pl . -Dtest=MessageControllerTest -q
  ```

  Expected: all tests pass (BUILD SUCCESS).

- [ ] **Step 5: Run the full test suite to confirm no regressions**

  ```bash
  mvn test -q
  ```

  Expected: all tests pass (BUILD SUCCESS). The suite currently has 29 tests; it should now have at minimum 37 (6 new: 1 in MessageServiceTest, 1 in MessageControllerTest×2 for Fix 4, 2 in ChatControllerTest×2 for Fix 4, 2 new for Fix 5, 2 new for Fix 1 — 8 total new tests = 37).

- [ ] **Step 6: Commit**

  ```bash
  git add src/main/java/com/advisorplatform/api/MessageController.java \
          src/test/java/com/advisorplatform/api/MessageControllerTest.java
  git commit -m "feat: add POST /api/thread/{threadId}/messages endpoint for visitor follow-up"
  ```

---

## Self-Review

**Spec coverage check:**
- Fix 1 (follow-up endpoint): Task 5 — endpoint, records, 2 new tests ✓
- Fix 2 (updatedAt bump): Task 1 — service fix + extended assertion ✓
- Fix 3 (@Transactional removal): Task 2 — annotation removed, Javadoc updated ✓
- Fix 4 (global handler): Task 3 — new file + 2 cross-controller tests ✓
- Fix 5 (@NotNull): Task 4 — two record changes + 2 new tests ✓

**Placeholder scan:** No TBDs. All code blocks are complete and self-contained.

**Type consistency:** `messageService.addMessage(UUID, String)` used in Task 5 matches existing signature in `MessageService`. `ThreadMessage.getId()` exists via `@Getter`. `AddMessageResponse.messageId` matches the record field. All consistent.
