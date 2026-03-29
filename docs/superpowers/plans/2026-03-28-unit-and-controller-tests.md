# Unit & Controller Tests Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add complete unit and `@WebMvcTest` slice tests for all five service/AI/controller classes, covering positive and negative paths.

**Architecture:** Pure Mockito unit tests for `VisitorService`, `MessageService`, and `PlannerAiService` (no Spring context). `@WebMvcTest` controller slice tests for `ChatController` and `MessageController` with `@MockBean` service dependencies. No database or real Anthropic API required.

**Tech Stack:** JUnit 5, Mockito (both already provided by `spring-boot-starter-test`), Spring MockMvc, AssertJ, Spring `ReflectionTestUtils`.

---

## File Map

| File (create) | Responsibility |
|---|---|
| `src/test/resources/application.yml` | Provides `spring.ai.anthropic.api-key=test` so Spring AI auto-config doesn't fail in the `@WebMvcTest` slice |
| `src/test/java/com/advisorplatform/service/VisitorServiceTest.java` | Unit tests for `VisitorService` — 5 cases |
| `src/test/java/com/advisorplatform/service/MessageServiceTest.java` | Unit tests for `MessageService` — 8 cases |
| `src/test/java/com/advisorplatform/ai/PlannerAiServiceTest.java` | Unit tests for `PlannerAiService` — 5 cases |
| `src/test/java/com/advisorplatform/api/ChatControllerTest.java` | `@WebMvcTest` slice for `ChatController` — 7 cases |
| `src/test/java/com/advisorplatform/api/MessageControllerTest.java` | `@WebMvcTest` slice for `MessageController` — 4 cases |

---

## Task 1: Test Infrastructure

**Files:**
- Create: `src/test/resources/application.yml`

- [ ] **Step 1: Create the test application.yml**

```yaml
# src/test/resources/application.yml
spring:
  ai:
    anthropic:
      api-key: test-key
  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
  flyway:
    enabled: false
  jpa:
    hibernate:
      ddl-auto: none
```

- [ ] **Step 2: Verify compile still passes**

Run: `mvn compile -q`
Expected: no output (clean build)

- [ ] **Step 3: Commit**

```bash
git add src/test/resources/application.yml
git commit -m "test: add test application.yml to satisfy Spring AI config in test slice"
```

---

## Task 2: VisitorServiceTest

**Files:**
- Create: `src/test/java/com/advisorplatform/service/VisitorServiceTest.java`

- [ ] **Step 1: Write the test file**

```java
package com.advisorplatform.service;

import com.advisorplatform.domain.entity.AiSession;
import com.advisorplatform.domain.entity.Visitor;
import com.advisorplatform.domain.repository.AiSessionRepository;
import com.advisorplatform.domain.repository.VisitorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VisitorServiceTest {

    @Mock VisitorRepository visitorRepository;
    @Mock AiSessionRepository aiSessionRepository;
    @InjectMocks VisitorService visitorService;

    // ── findOrCreate ─────────────────────────────────────────────────────────

    @Test
    void findOrCreate_existingVisitor_updatesLastSeenAtAndReturns() {
        Visitor existing = new Visitor();
        existing.setBrowserToken("token-abc");
        when(visitorRepository.findByBrowserToken("token-abc")).thenReturn(Optional.of(existing));
        when(visitorRepository.save(existing)).thenReturn(existing);

        Visitor result = visitorService.findOrCreate("token-abc");

        assertThat(result).isSameAs(existing);
        verify(visitorRepository).save(existing);
    }

    @Test
    void findOrCreate_newVisitor_createsWithBrowserTokenAndReturns() {
        when(visitorRepository.findByBrowserToken("token-new")).thenReturn(Optional.empty());
        Visitor saved = new Visitor();
        when(visitorRepository.save(any(Visitor.class))).thenReturn(saved);

        Visitor result = visitorService.findOrCreate("token-new");

        assertThat(result).isSameAs(saved);
        ArgumentCaptor<Visitor> captor = ArgumentCaptor.forClass(Visitor.class);
        verify(visitorRepository).save(captor.capture());
        assertThat(captor.getValue().getBrowserToken()).isEqualTo("token-new");
    }

    // ── createSession ────────────────────────────────────────────────────────

    @Test
    void createSession_knownVisitor_savesAndReturnsSession() {
        UUID visitorId = UUID.randomUUID();
        Visitor visitor = new Visitor();
        when(visitorRepository.findById(visitorId)).thenReturn(Optional.of(visitor));
        AiSession session = new AiSession();
        when(aiSessionRepository.save(any(AiSession.class))).thenReturn(session);

        AiSession result = visitorService.createSession(visitorId);

        assertThat(result).isSameAs(session);
        ArgumentCaptor<AiSession> captor = ArgumentCaptor.forClass(AiSession.class);
        verify(aiSessionRepository).save(captor.capture());
        assertThat(captor.getValue().getVisitor()).isSameAs(visitor);
    }

    @Test
    void createSession_unknownVisitor_throwsIllegalArgument() {
        UUID unknown = UUID.randomUUID();
        when(visitorRepository.findById(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> visitorService.createSession(unknown))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Visitor not found");
    }

    // ── getSessions ──────────────────────────────────────────────────────────

    @Test
    void getSessions_delegatesAndReturnsSortedList() {
        UUID visitorId = UUID.randomUUID();
        List<AiSession> sessions = List.of(new AiSession(), new AiSession());
        when(aiSessionRepository.findByVisitorIdOrderByCreatedAtDesc(visitorId)).thenReturn(sessions);

        assertThat(visitorService.getSessions(visitorId)).isSameAs(sessions);
    }
}
```

- [ ] **Step 2: Run tests**

Run: `mvn test -pl . -Dtest=VisitorServiceTest -q`
Expected: `BUILD SUCCESS`, 5 tests passing

- [ ] **Step 3: Commit**

```bash
git add src/test/java/com/advisorplatform/service/VisitorServiceTest.java
git commit -m "test: add VisitorService unit tests (5 cases)"
```

---

## Task 3: MessageServiceTest

**Files:**
- Create: `src/test/java/com/advisorplatform/service/MessageServiceTest.java`

- [ ] **Step 1: Write the test file**

```java
package com.advisorplatform.service;

import com.advisorplatform.domain.entity.AiSession;
import com.advisorplatform.domain.entity.MessageThread;
import com.advisorplatform.domain.entity.ThreadMessage;
import com.advisorplatform.domain.entity.Visitor;
import com.advisorplatform.domain.repository.AiSessionRepository;
import com.advisorplatform.domain.repository.MessageThreadRepository;
import com.advisorplatform.domain.repository.ThreadMessageRepository;
import com.advisorplatform.domain.repository.VisitorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock MessageThreadRepository threadRepo;
    @Mock ThreadMessageRepository messageRepo;
    @Mock VisitorRepository visitorRepo;
    @Mock AiSessionRepository sessionRepo;
    @InjectMocks MessageService messageService;

    // ── createThread ─────────────────────────────────────────────────────────

    @Test
    void createThread_noSession_savesThreadAndFirstVisitorMessage() {
        UUID visitorId = UUID.randomUUID();
        Visitor visitor = new Visitor();
        when(visitorRepo.findById(visitorId)).thenReturn(Optional.of(visitor));
        MessageThread savedThread = MessageThread.builder().visitor(visitor).subject("Help").build();
        when(threadRepo.save(any(MessageThread.class))).thenReturn(savedThread);
        when(messageRepo.save(any(ThreadMessage.class))).thenAnswer(inv -> inv.getArgument(0));

        MessageThread result = messageService.createThread(visitorId, null, "Help", "Hello there");

        assertThat(result).isSameAs(savedThread);
        verify(messageRepo).save(argThat(m ->
                "visitor".equals(m.getSenderRole()) && "Hello there".equals(m.getContent())));
    }

    @Test
    void createThread_withSession_linksAiSessionToThread() {
        UUID visitorId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        Visitor visitor = new Visitor();
        AiSession aiSession = new AiSession();
        when(visitorRepo.findById(visitorId)).thenReturn(Optional.of(visitor));
        when(sessionRepo.findById(sessionId)).thenReturn(Optional.of(aiSession));
        MessageThread savedThread = MessageThread.builder().visitor(visitor).aiSession(aiSession).build();
        when(threadRepo.save(any(MessageThread.class))).thenReturn(savedThread);
        when(messageRepo.save(any(ThreadMessage.class))).thenAnswer(inv -> inv.getArgument(0));

        messageService.createThread(visitorId, sessionId, null, "With session");

        verify(threadRepo).save(argThat(t -> aiSession.equals(t.getAiSession())));
    }

    @Test
    void createThread_unknownVisitor_throwsIllegalArgument() {
        UUID unknown = UUID.randomUUID();
        when(visitorRepo.findById(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> messageService.createThread(unknown, null, "S", "C"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Visitor not found");
    }

    @Test
    void createThread_unknownSession_throwsIllegalArgument() {
        UUID visitorId = UUID.randomUUID();
        UUID badSession = UUID.randomUUID();
        when(visitorRepo.findById(visitorId)).thenReturn(Optional.of(new Visitor()));
        when(sessionRepo.findById(badSession)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> messageService.createThread(visitorId, badSession, "S", "C"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Session not found");
    }

    // ── addMessage ───────────────────────────────────────────────────────────

    @Test
    void addMessage_knownThread_savesVisitorMessageAndBumpsThread() {
        UUID threadId = UUID.randomUUID();
        MessageThread thread = MessageThread.builder().build();
        when(threadRepo.findById(threadId)).thenReturn(Optional.of(thread));
        ThreadMessage saved = ThreadMessage.builder()
                .thread(thread).senderRole("visitor").content("More info").build();
        when(messageRepo.save(any(ThreadMessage.class))).thenReturn(saved);
        when(threadRepo.save(thread)).thenReturn(thread);

        ThreadMessage result = messageService.addMessage(threadId, "More info");

        assertThat(result).isSameAs(saved);
        verify(messageRepo).save(argThat(m ->
                "visitor".equals(m.getSenderRole()) && "More info".equals(m.getContent())));
        verify(threadRepo).save(thread); // updatedAt bump
    }

    @Test
    void addMessage_unknownThread_throwsIllegalArgument() {
        UUID unknown = UUID.randomUUID();
        when(threadRepo.findById(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> messageService.addMessage(unknown, "content"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Thread not found");
    }

    // ── getThreads / getMessages ──────────────────────────────────────────────

    @Test
    void getThreads_delegatesAndReturnsRepoResult() {
        UUID visitorId = UUID.randomUUID();
        List<MessageThread> threads = List.of(MessageThread.builder().build());
        when(threadRepo.findByVisitorIdOrderByUpdatedAtDesc(visitorId)).thenReturn(threads);

        assertThat(messageService.getThreads(visitorId)).isSameAs(threads);
    }

    @Test
    void getMessages_delegatesAndReturnsRepoResult() {
        UUID threadId = UUID.randomUUID();
        MessageThread thread = MessageThread.builder().build();
        List<ThreadMessage> messages = List.of(
                ThreadMessage.builder().thread(thread).senderRole("visitor").content("Hi").build());
        when(messageRepo.findByThreadIdOrderByCreatedAtAsc(threadId)).thenReturn(messages);

        assertThat(messageService.getMessages(threadId)).isSameAs(messages);
    }
}
```

- [ ] **Step 2: Run tests**

Run: `mvn test -pl . -Dtest=MessageServiceTest -q`
Expected: `BUILD SUCCESS`, 8 tests passing

- [ ] **Step 3: Commit**

```bash
git add src/test/java/com/advisorplatform/service/MessageServiceTest.java
git commit -m "test: add MessageService unit tests (8 cases)"
```

---

## Task 4: PlannerAiServiceTest

**Files:**
- Create: `src/test/java/com/advisorplatform/ai/PlannerAiServiceTest.java`

- [ ] **Step 1: Write the test file**

```java
package com.advisorplatform.ai;

import com.advisorplatform.domain.entity.AiMessage;
import com.advisorplatform.domain.entity.AiSession;
import com.advisorplatform.domain.repository.AiMessageRepository;
import com.advisorplatform.domain.repository.AiSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.io.Resource;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlannerAiServiceTest {

    @Mock ChatModel chatModel;
    @Mock AiSessionRepository sessionRepository;
    @Mock AiMessageRepository messageRepository;

    PlannerAiService service;

    @BeforeEach
    void setUp() throws IOException {
        Resource resource = mock(Resource.class);
        when(resource.getContentAsString(StandardCharsets.UTF_8)).thenReturn("You are a helpful advisor.");
        service = new PlannerAiService(chatModel, sessionRepository, messageRepository, resource, 3);
    }

    // ── chat ─────────────────────────────────────────────────────────────────

    @Test
    void chat_unknownSession_throwsIllegalArgument() {
        UUID sessionId = UUID.randomUUID();
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.chat(sessionId, "hello"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Session not found");
    }

    @Test
    void chat_knownSession_persistsUserAndAssistantMessagesAndReturnsAssistant() {
        UUID sessionId = UUID.randomUUID();
        AiSession session = new AiSession();
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId)).thenReturn(List.of());
        when(messageRepository.save(any(AiMessage.class))).thenAnswer(inv -> inv.getArgument(0));

        ChatResponse response = mock(ChatResponse.class, RETURNS_DEEP_STUBS);
        when(response.getResult().getOutput().getText()).thenReturn("AI answer");
        when(response.getMetadata().getUsage()).thenReturn(null);
        when(chatModel.call(any(Prompt.class))).thenReturn(response);
        when(chatModel.getDefaultOptions()).thenReturn(mock(ChatOptions.class));

        AiMessage result = service.chat(sessionId, "User question");

        assertThat(result.getContent()).isEqualTo("AI answer");
        assertThat(result.getRole()).isEqualTo("assistant");
        verify(messageRepository, times(2)).save(any(AiMessage.class)); // user + assistant
    }

    @Test
    void chat_historyExceedsWindow_onlyRecentTurnsIncludedInPrompt() {
        UUID sessionId = UUID.randomUUID();
        AiSession session = new AiSession();
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        // 4 full turns = 8 messages; maxHistoryTurns=3 so only last 6 should be sent
        List<AiMessage> history = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            history.add(AiMessage.userMessage(session, "user " + i));
            history.add(AiMessage.assistantMessage(session, "ai " + i, "model", 0, 0));
        }
        when(messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId)).thenReturn(history);
        when(messageRepository.save(any(AiMessage.class))).thenAnswer(inv -> inv.getArgument(0));

        ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);
        ChatResponse response = mock(ChatResponse.class, RETURNS_DEEP_STUBS);
        when(response.getResult().getOutput().getText()).thenReturn("ok");
        when(response.getMetadata().getUsage()).thenReturn(null);
        when(chatModel.call(promptCaptor.capture())).thenReturn(response);
        when(chatModel.getDefaultOptions()).thenReturn(mock(ChatOptions.class));

        service.chat(sessionId, "new message");

        // 1 system + 6 history + 1 new user = 8 instructions
        List<?> instructions = promptCaptor.getValue().getInstructions();
        assertThat(instructions).hasSize(8);
        assertThat(instructions.get(0)).isInstanceOf(SystemMessage.class);
        assertThat(instructions.get(7)).isInstanceOf(UserMessage.class);
    }

    // ── streamChat ───────────────────────────────────────────────────────────

    @Test
    void streamChat_unknownSession_throwsIllegalArgument() {
        UUID sessionId = UUID.randomUUID();
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.streamChat(sessionId, "hello"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Session not found");
    }

    @Test
    void streamChat_knownSession_emitsChunksAndPersistsOnComplete() {
        UUID sessionId = UUID.randomUUID();
        AiSession session = new AiSession();
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId)).thenReturn(List.of());
        when(messageRepository.save(any(AiMessage.class))).thenAnswer(inv -> inv.getArgument(0));

        ChatResponse chunk = mock(ChatResponse.class, RETURNS_DEEP_STUBS);
        when(chunk.getResult().getOutput().getText()).thenReturn("Hello");
        when(chatModel.stream(any(Prompt.class))).thenReturn(Flux.just(chunk));
        when(chatModel.getDefaultOptions()).thenReturn(mock(ChatOptions.class));

        Flux<String> flux = service.streamChat(sessionId, "Hi");
        List<String> chunks = flux.collectList().block();

        assertThat(chunks).containsExactly("Hello");
        // user message persisted before stream + assistant message persisted in doOnComplete
        verify(messageRepository, times(2)).save(any(AiMessage.class));
    }
}
```

- [ ] **Step 2: Run tests**

Run: `mvn test -pl . -Dtest=PlannerAiServiceTest -q`
Expected: `BUILD SUCCESS`, 5 tests passing

- [ ] **Step 3: Commit**

```bash
git add src/test/java/com/advisorplatform/ai/PlannerAiServiceTest.java
git commit -m "test: add PlannerAiService unit tests (5 cases)"
```

---

## Task 5: ChatControllerTest

**Files:**
- Create: `src/test/java/com/advisorplatform/api/ChatControllerTest.java`

- [ ] **Step 1: Write the test file**

```java
package com.advisorplatform.api;

import com.advisorplatform.ai.PlannerAiService;
import com.advisorplatform.domain.entity.AiMessage;
import com.advisorplatform.domain.entity.AiSession;
import com.advisorplatform.domain.entity.Visitor;
import com.advisorplatform.service.VisitorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ChatController.class)
@TestPropertySource(properties = "spring.ai.anthropic.api-key=test-key")
class ChatControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean VisitorService visitorService;
    @MockBean PlannerAiService plannerAiService;

    // ── POST /api/visitor/identify ────────────────────────────────────────────

    @Test
    void identify_validToken_returns200WithVisitorIdAndToken() throws Exception {
        UUID visitorId = UUID.randomUUID();
        Visitor visitor = new Visitor();
        ReflectionTestUtils.setField(visitor, "id", visitorId);
        visitor.setBrowserToken("token-abc");
        when(visitorService.findOrCreate("token-abc")).thenReturn(visitor);

        mockMvc.perform(post("/api/visitor/identify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"browserToken\":\"token-abc\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visitorId").value(visitorId.toString()))
                .andExpect(jsonPath("$.browserToken").value("token-abc"));
    }

    @Test
    void identify_blankToken_returns400() throws Exception {
        mockMvc.perform(post("/api/visitor/identify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"browserToken\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    // ── POST /api/session ────────────────────────────────────────────────────

    @Test
    void createSession_validVisitorId_returns200WithSessionId() throws Exception {
        UUID sessionId = UUID.randomUUID();
        AiSession session = new AiSession();
        ReflectionTestUtils.setField(session, "id", sessionId);
        ReflectionTestUtils.setField(session, "createdAt", Instant.parse("2026-03-28T10:00:00Z"));
        when(visitorService.createSession(any(UUID.class))).thenReturn(session);

        mockMvc.perform(post("/api/session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visitorId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(sessionId.toString()));
    }

    // ── GET /api/visitor/{visitorId}/sessions ─────────────────────────────────

    @Test
    void getSessions_returns200WithList() throws Exception {
        UUID visitorId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        AiSession session = new AiSession();
        ReflectionTestUtils.setField(session, "id", sessionId);
        ReflectionTestUtils.setField(session, "createdAt", Instant.parse("2026-03-28T10:00:00Z"));
        when(visitorService.getSessions(visitorId)).thenReturn(List.of(session));

        mockMvc.perform(get("/api/visitor/{visitorId}/sessions", visitorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sessionId").value(sessionId.toString()));
    }

    // ── POST /api/chat/{sessionId} ────────────────────────────────────────────

    @Test
    void chat_validRequest_returns200WithAssistantMessage() throws Exception {
        UUID sessionId = UUID.randomUUID();
        AiMessage response = AiMessage.assistantMessage(new AiSession(), "AI reply", "claude-3", 100, 250);
        ReflectionTestUtils.setField(response, "id", UUID.randomUUID());
        when(plannerAiService.chat(eq(sessionId), eq("Hello"))).thenReturn(response);

        mockMvc.perform(post("/api/chat/{sessionId}", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Hello\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("AI reply"))
                .andExpect(jsonPath("$.model").value("claude-3"));
    }

    @Test
    void chat_blankMessage_returns400() throws Exception {
        mockMvc.perform(post("/api/chat/{sessionId}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    // ── POST /api/chat/{sessionId}/stream ─────────────────────────────────────

    @Test
    void streamChat_validRequest_returnsEventStreamStatus200() throws Exception {
        UUID sessionId = UUID.randomUUID();
        when(plannerAiService.streamChat(eq(sessionId), eq("Hi"))).thenReturn(Flux.just("chunk1", "chunk2"));

        mockMvc.perform(post("/api/chat/{sessionId}/stream", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Hi\"}"))
                .andExpect(status().isOk());
    }
}
```

- [ ] **Step 2: Run tests**

Run: `mvn test -pl . -Dtest=ChatControllerTest -q`
Expected: `BUILD SUCCESS`, 7 tests passing

- [ ] **Step 3: Commit**

```bash
git add src/test/java/com/advisorplatform/api/ChatControllerTest.java
git commit -m "test: add ChatController WebMvcTest slice (7 cases)"
```

---

## Task 6: MessageControllerTest

**Files:**
- Create: `src/test/java/com/advisorplatform/api/MessageControllerTest.java`

- [ ] **Step 1: Write the test file**

```java
package com.advisorplatform.api;

import com.advisorplatform.domain.entity.MessageThread;
import com.advisorplatform.domain.entity.ThreadMessage;
import com.advisorplatform.domain.entity.Visitor;
import com.advisorplatform.service.MessageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = MessageController.class)
@TestPropertySource(properties = "spring.ai.anthropic.api-key=test-key")
class MessageControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean MessageService messageService;

    // ── POST /api/message ────────────────────────────────────────────────────

    @Test
    void createThread_validRequest_returns200WithThreadId() throws Exception {
        UUID visitorId = UUID.randomUUID();
        UUID threadId = UUID.randomUUID();
        Visitor visitor = new Visitor();
        MessageThread thread = MessageThread.builder().visitor(visitor).subject("Help").build();
        ReflectionTestUtils.setField(thread, "id", threadId);
        when(messageService.createThread(eq(visitorId), isNull(), eq("Help me"), eq("First message")))
                .thenReturn(thread);

        mockMvc.perform(post("/api/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visitorId\":\"" + visitorId + "\","
                                + "\"subject\":\"Help me\","
                                + "\"content\":\"First message\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.threadId").value(threadId.toString()));
    }

    @Test
    void createThread_blankContent_returns400() throws Exception {
        mockMvc.perform(post("/api/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visitorId\":\"" + UUID.randomUUID() + "\","
                                + "\"content\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    // ── GET /api/visitor/{visitorId}/threads ──────────────────────────────────

    @Test
    void getThreads_validVisitorId_returns200WithList() throws Exception {
        UUID visitorId = UUID.randomUUID();
        UUID threadId = UUID.randomUUID();
        Visitor visitor = new Visitor();
        MessageThread thread = MessageThread.builder().visitor(visitor).subject("My question").build();
        ReflectionTestUtils.setField(thread, "id", threadId);
        ReflectionTestUtils.setField(thread, "status", "open");
        ReflectionTestUtils.setField(thread, "updatedAt", Instant.parse("2026-03-28T10:00:00Z"));
        when(messageService.getThreads(visitorId)).thenReturn(List.of(thread));

        mockMvc.perform(get("/api/visitor/{visitorId}/threads", visitorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].threadId").value(threadId.toString()))
                .andExpect(jsonPath("$[0].subject").value("My question"))
                .andExpect(jsonPath("$[0].status").value("open"));
    }

    // ── GET /api/thread/{threadId}/messages ───────────────────────────────────

    @Test
    void getMessages_validThreadId_returns200WithList() throws Exception {
        UUID threadId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        Visitor visitor = new Visitor();
        MessageThread thread = MessageThread.builder().visitor(visitor).build();
        ThreadMessage message = ThreadMessage.builder()
                .thread(thread).senderRole("visitor").content("Hello").build();
        ReflectionTestUtils.setField(message, "id", messageId);
        ReflectionTestUtils.setField(message, "createdAt", Instant.parse("2026-03-28T10:00:00Z"));
        when(messageService.getMessages(threadId)).thenReturn(List.of(message));

        mockMvc.perform(get("/api/thread/{threadId}/messages", threadId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].messageId").value(messageId.toString()))
                .andExpect(jsonPath("$[0].senderRole").value("visitor"))
                .andExpect(jsonPath("$[0].content").value("Hello"));
    }
}
```

- [ ] **Step 2: Run tests**

Run: `mvn test -pl . -Dtest=MessageControllerTest -q`
Expected: `BUILD SUCCESS`, 4 tests passing

- [ ] **Step 3: Run full test suite**

Run: `mvn test -q`
Expected: `BUILD SUCCESS`, all 29 tests passing

- [ ] **Step 4: Commit**

```bash
git add src/test/java/com/advisorplatform/api/MessageControllerTest.java
git commit -m "test: add MessageController WebMvcTest slice (4 cases)"
```

---

## Self-Review Checklist

- [x] **Spec coverage:** All 5 classes covered: `VisitorService` (5), `MessageService` (8), `PlannerAiService` (5), `ChatController` (7), `MessageController` (4) = 29 total tests.
- [x] **Positive cases:** Every service method and endpoint has a happy-path test.
- [x] **Negative cases:** Every `IllegalArgumentException` path tested in services; `@NotBlank` validation tested in controllers.
- [x] **No placeholders:** All test bodies contain real assertions and stubs.
- [x] **Type consistency:** `ReflectionTestUtils.setField` used consistently for setting private `id`/`createdAt`/`updatedAt` fields that have no public setters.
- [x] **Note on AnthropicChatOptions:** Step 2 of Task 4 includes a verification step for the exact import path, which may vary by Spring AI milestone version.
