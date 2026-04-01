# OpenAPI Spec-First Codegen & Controller Migration — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Wire `openapi-generator-maven-plugin` for three domain specs, migrate Phase 1 hand-written controllers to the generated delegate pattern, and expose Swagger UI grouped by domain.

**Architecture:** Each OpenAPI YAML spec in `src/main/resources/api/` drives code generation into `target/generated-sources/openapi`. Generated controllers delegate to hand-written `*ApiDelegate` implementations in `com.advisorplatform.api`. Existing controllers are deleted once delegates are verified.

**Tech Stack:** `openapi-generator-maven-plugin` 7.10.0, `springdoc-openapi-starter-webmvc-ui` 2.8.5, Spring Boot 3.4.3, JaCoCo (managed by Spring Boot parent).

---

## File Map

**Create:**
- `src/main/resources/api/visitor-session-api.yaml`
- `src/main/resources/api/ai-chat-api.yaml`
- `src/main/resources/api/messaging-api.yaml`
- `src/main/resources/api/advisor-api.yaml` (placeholder)
- `src/main/java/com/advisorplatform/api/VisitorSessionDelegate.java`
- `src/main/java/com/advisorplatform/api/AiChatDelegate.java`
- `src/main/java/com/advisorplatform/api/MessagingDelegate.java`
- `src/main/java/com/advisorplatform/config/SwaggerConfig.java`
- `src/test/java/com/advisorplatform/api/VisitorSessionControllerTest.java`
- `src/test/java/com/advisorplatform/api/AiChatControllerTest.java`
- `src/test/java/com/advisorplatform/api/MessagingControllerTest.java`
- `docs/api/visitor-session-api.yaml`
- `docs/api/ai-chat-api.yaml`
- `docs/api/messaging-api.yaml`

**Modify:**
- `pom.xml` — add plugin, springdoc dep, jacoco config

**Delete:**
- `src/main/java/com/advisorplatform/api/ChatController.java`
- `src/main/java/com/advisorplatform/api/MessageController.java`
- `src/test/java/com/advisorplatform/api/ChatControllerTest.java`
- `src/test/java/com/advisorplatform/api/MessageControllerTest.java`

**Never edit:**
- `target/generated-sources/openapi/**` — regenerated on every `mvn compile`

---

## Task 1: Bootstrap — Add Codegen Plugin and Springdoc to pom.xml

**Files:**
- Modify: `pom.xml`

The plugin must be configured before specs are written. All three spec executions are added here so the build structure is stable. Specs are created in the next task.

Note on package layout: each spec gets its own sub-package under `com.advisorplatform.generated.*` to prevent class-name collisions between specs (e.g. both might want a model named `ErrorResponse`). All output goes to `target/generated-sources/openapi`, which the plugin auto-adds as a compile source root.

- [ ] **Step 1: Add springdoc and openapi-generator-maven-plugin to pom.xml**

In `pom.xml`, add the springdoc dependency inside `<dependencies>`:

```xml
<!-- Swagger UI -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.8.5</version>
</dependency>
```

Add the codegen plugin and jacoco plugin inside `<build><plugins>`:

```xml
<!-- OpenAPI codegen -->
<plugin>
    <groupId>org.openapitools</groupId>
    <artifactId>openapi-generator-maven-plugin</artifactId>
    <version>7.10.0</version>
    <executions>
        <execution>
            <id>visitor-session</id>
            <goals><goal>generate</goal></goals>
            <configuration>
                <inputSpec>${project.basedir}/src/main/resources/api/visitor-session-api.yaml</inputSpec>
                <generatorName>spring</generatorName>
                <output>${project.build.directory}/generated-sources/openapi</output>
                <apiPackage>com.advisorplatform.generated.visitorsession.api</apiPackage>
                <modelPackage>com.advisorplatform.generated.visitorsession.model</modelPackage>
                <generateApiTests>false</generateApiTests>
                <generateModelTests>false</generateModelTests>
                <generateApiDocumentation>false</generateApiDocumentation>
                <generateModelDocumentation>false</generateModelDocumentation>
                <configOptions>
                    <delegatePattern>true</delegatePattern>
                    <useSpringBoot3>true</useSpringBoot3>
                    <useLombokAnnotations>true</useLombokAnnotations>
                    <useTags>true</useTags>
                    <openApiNullable>false</openApiNullable>
                </configOptions>
            </configuration>
        </execution>
        <execution>
            <id>ai-chat</id>
            <goals><goal>generate</goal></goals>
            <configuration>
                <inputSpec>${project.basedir}/src/main/resources/api/ai-chat-api.yaml</inputSpec>
                <generatorName>spring</generatorName>
                <output>${project.build.directory}/generated-sources/openapi</output>
                <apiPackage>com.advisorplatform.generated.aichat.api</apiPackage>
                <modelPackage>com.advisorplatform.generated.aichat.model</modelPackage>
                <generateApiTests>false</generateApiTests>
                <generateModelTests>false</generateModelTests>
                <generateApiDocumentation>false</generateApiDocumentation>
                <generateModelDocumentation>false</generateModelDocumentation>
                <configOptions>
                    <delegatePattern>true</delegatePattern>
                    <useSpringBoot3>true</useSpringBoot3>
                    <useLombokAnnotations>true</useLombokAnnotations>
                    <useTags>true</useTags>
                    <openApiNullable>false</openApiNullable>
                </configOptions>
            </configuration>
        </execution>
        <execution>
            <id>messaging</id>
            <goals><goal>generate</goal></goals>
            <configuration>
                <inputSpec>${project.basedir}/src/main/resources/api/messaging-api.yaml</inputSpec>
                <generatorName>spring</generatorName>
                <output>${project.build.directory}/generated-sources/openapi</output>
                <apiPackage>com.advisorplatform.generated.messaging.api</apiPackage>
                <modelPackage>com.advisorplatform.generated.messaging.model</modelPackage>
                <generateApiTests>false</generateApiTests>
                <generateModelTests>false</generateModelTests>
                <generateApiDocumentation>false</generateApiDocumentation>
                <generateModelDocumentation>false</generateModelDocumentation>
                <configOptions>
                    <delegatePattern>true</delegatePattern>
                    <useSpringBoot3>true</useSpringBoot3>
                    <useLombokAnnotations>true</useLombokAnnotations>
                    <useTags>true</useTags>
                    <openApiNullable>false</openApiNullable>
                </configOptions>
            </configuration>
        </execution>
        <execution>
            <id>advisor</id>
            <goals><goal>generate</goal></goals>
            <configuration>
                <inputSpec>${project.basedir}/src/main/resources/api/advisor-api.yaml</inputSpec>
                <generatorName>spring</generatorName>
                <output>${project.build.directory}/generated-sources/openapi</output>
                <apiPackage>com.advisorplatform.generated.advisor.api</apiPackage>
                <modelPackage>com.advisorplatform.generated.advisor.model</modelPackage>
                <generateApiTests>false</generateApiTests>
                <generateModelTests>false</generateModelTests>
                <generateApiDocumentation>false</generateApiDocumentation>
                <generateModelDocumentation>false</generateModelDocumentation>
                <configOptions>
                    <delegatePattern>true</delegatePattern>
                    <useSpringBoot3>true</useSpringBoot3>
                    <useLombokAnnotations>true</useLombokAnnotations>
                    <useTags>true</useTags>
                    <openApiNullable>false</openApiNullable>
                </configOptions>
            </configuration>
        </execution>
    </executions>
</plugin>

<!-- Code coverage — exclude generated sources -->
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <executions>
        <execution>
            <goals><goal>prepare-agent</goal></goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals><goal>report</goal></goals>
        </execution>
    </executions>
    <configuration>
        <excludes>
            <exclude>com/advisorplatform/generated/**</exclude>
        </excludes>
    </configuration>
</plugin>
```

The build will fail until the 4 spec YAML files exist. That happens in Task 2.

- [ ] **Step 2: Commit the pom.xml changes**

```bash
git add pom.xml
git commit -m "build: add openapi-generator-maven-plugin, springdoc, and jacoco config"
```

---

## Task 2: Write All Four OpenAPI Specs

**Files:**
- Create: `src/main/resources/api/visitor-session-api.yaml`
- Create: `src/main/resources/api/ai-chat-api.yaml`
- Create: `src/main/resources/api/messaging-api.yaml`
- Create: `src/main/resources/api/advisor-api.yaml`

These specs reverse-engineer the existing endpoints exactly — same paths, same request/response shapes, same field names. Do not redesign the API while writing specs.

- [ ] **Step 1: Create `src/main/resources/api/visitor-session-api.yaml`**

```yaml
openapi: 3.0.3
info:
  title: Visitor Session API
  description: Visitor identity and AI planning session lifecycle
  version: 1.0.0
tags:
  - name: VisitorSession
    description: Visitor identification and session management
paths:
  /api/visitor/identify:
    post:
      tags: [VisitorSession]
      summary: Find or create a visitor by browser token
      operationId: identify
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/IdentifyRequest'
      responses:
        '200':
          description: Visitor identified
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/VisitorResponse'
        '400':
          description: Validation error
  /api/session:
    post:
      tags: [VisitorSession]
      summary: Create a new AI planning session
      operationId: createSession
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CreateSessionRequest'
      responses:
        '200':
          description: Session created
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/SessionResponse'
        '400':
          description: Validation error or visitor not found
  /api/visitor/{visitorId}/sessions:
    get:
      tags: [VisitorSession]
      summary: List sessions for a visitor
      operationId: getSessions
      parameters:
        - name: visitorId
          in: path
          required: true
          schema:
            type: string
            format: uuid
      responses:
        '200':
          description: Session list
          content:
            application/json:
              schema:
                type: array
                items:
                  $ref: '#/components/schemas/SessionResponse'
components:
  schemas:
    IdentifyRequest:
      type: object
      required: [browserToken]
      properties:
        browserToken:
          type: string
          minLength: 1
    VisitorResponse:
      type: object
      properties:
        visitorId:
          type: string
          format: uuid
        browserToken:
          type: string
    CreateSessionRequest:
      type: object
      required: [visitorId]
      properties:
        visitorId:
          type: string
          format: uuid
    SessionResponse:
      type: object
      properties:
        sessionId:
          type: string
          format: uuid
        createdAt:
          type: string
```

- [ ] **Step 2: Create `src/main/resources/api/ai-chat-api.yaml`**

```yaml
openapi: 3.0.3
info:
  title: AI Chat API
  description: AI-powered planning chat endpoints (streaming and non-streaming)
  version: 1.0.0
tags:
  - name: AiChat
    description: AI chat with Claude
paths:
  /api/chat/{sessionId}:
    post:
      tags: [AiChat]
      summary: Non-streaming chat (blocking)
      operationId: chat
      parameters:
        - name: sessionId
          in: path
          required: true
          schema:
            type: string
            format: uuid
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/ChatRequest'
      responses:
        '200':
          description: Assistant response
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ChatResponse'
        '400':
          description: Validation error or session not found
  /api/chat/{sessionId}/stream:
    post:
      tags: [AiChat]
      summary: Streaming SSE chat
      operationId: streamChat
      parameters:
        - name: sessionId
          in: path
          required: true
          schema:
            type: string
            format: uuid
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/ChatRequest'
      responses:
        '200':
          description: Server-sent event stream of text tokens
          content:
            text/event-stream:
              schema:
                type: string
components:
  schemas:
    ChatRequest:
      type: object
      required: [message]
      properties:
        message:
          type: string
          minLength: 1
    ChatResponse:
      type: object
      properties:
        messageId:
          type: string
          format: uuid
        content:
          type: string
        model:
          type: string
        createdAt:
          type: string
```

- [ ] **Step 3: Create `src/main/resources/api/messaging-api.yaml`**

```yaml
openapi: 3.0.3
info:
  title: Messaging API
  description: Visitor-to-advisor message threading
  version: 1.0.0
tags:
  - name: Messaging
    description: Message thread management
paths:
  /api/message:
    post:
      tags: [Messaging]
      summary: Create a new message thread with initial visitor message
      operationId: createThread
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CreateThreadRequest'
      responses:
        '200':
          description: Thread created
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/CreateThreadResponse'
        '400':
          description: Validation error or visitor not found
  /api/visitor/{visitorId}/threads:
    get:
      tags: [Messaging]
      summary: List all threads for a visitor
      operationId: getThreads
      parameters:
        - name: visitorId
          in: path
          required: true
          schema:
            type: string
            format: uuid
      responses:
        '200':
          description: Thread list
          content:
            application/json:
              schema:
                type: array
                items:
                  $ref: '#/components/schemas/ThreadSummary'
  /api/thread/{threadId}/messages:
    get:
      tags: [Messaging]
      summary: List all messages in a thread
      operationId: getMessages
      parameters:
        - name: threadId
          in: path
          required: true
          schema:
            type: string
            format: uuid
      responses:
        '200':
          description: Message list
          content:
            application/json:
              schema:
                type: array
                items:
                  $ref: '#/components/schemas/MessageSummary'
    post:
      tags: [Messaging]
      summary: Add a visitor follow-up message to a thread
      operationId: addMessage
      parameters:
        - name: threadId
          in: path
          required: true
          schema:
            type: string
            format: uuid
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/AddMessageRequest'
      responses:
        '200':
          description: Message added
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/AddMessageResponse'
        '400':
          description: Validation error or thread not found
components:
  schemas:
    CreateThreadRequest:
      type: object
      required: [visitorId, content]
      properties:
        visitorId:
          type: string
          format: uuid
        aiSessionId:
          type: string
          format: uuid
          nullable: true
        subject:
          type: string
        content:
          type: string
          minLength: 1
    CreateThreadResponse:
      type: object
      properties:
        threadId:
          type: string
          format: uuid
    ThreadSummary:
      type: object
      properties:
        threadId:
          type: string
          format: uuid
        subject:
          type: string
        status:
          type: string
        updatedAt:
          type: string
    MessageSummary:
      type: object
      properties:
        messageId:
          type: string
          format: uuid
        senderRole:
          type: string
        content:
          type: string
        createdAt:
          type: string
    AddMessageRequest:
      type: object
      required: [content]
      properties:
        content:
          type: string
          minLength: 1
    AddMessageResponse:
      type: object
      properties:
        messageId:
          type: string
          format: uuid
```

- [ ] **Step 4: Create `src/main/resources/api/advisor-api.yaml` (placeholder)**

```yaml
openapi: 3.0.3
info:
  title: Advisor API
  description: Advisor-facing endpoints — Phase 2
  version: 0.1.0
tags:
  - name: Advisor
    description: Advisor operations (not yet implemented)
paths: {}
```

- [ ] **Step 5: Verify the build compiles and generates sources**

```bash
mvn compile -q
```

Expected: BUILD SUCCESS with no errors. Then verify:

```bash
ls target/generated-sources/openapi/com/advisorplatform/generated/
```

Expected output (4 subdirectories):
```
aichat/  advisor/  messaging/  visitorsession/
```

Each contains `api/` and `model/` packages with generated classes.

- [ ] **Step 6: Check generated delegate interface names** (used in Tasks 3 and 4)

```bash
find target/generated-sources/openapi -name "*ApiDelegate.java" | sort
```

Expected:
```
.../aichat/api/AiChatApiDelegate.java
.../advisor/api/AdvisorApiDelegate.java
.../messaging/api/MessagingApiDelegate.java
.../visitorsession/api/VisitorSessionApiDelegate.java
```

If names differ from expected, update delegate class names in Tasks 3 and 4 accordingly.

- [ ] **Step 7: Commit specs**

```bash
git add src/main/resources/api/
git commit -m "feat: add OpenAPI specs for visitor-session, ai-chat, messaging, and advisor domains"
```

---

## Task 3: Implement VisitorSessionDelegate + AiChatDelegate, Delete ChatController

**Files:**
- Create: `src/main/java/com/advisorplatform/api/VisitorSessionDelegate.java`
- Create: `src/main/java/com/advisorplatform/api/AiChatDelegate.java`
- Create: `src/test/java/com/advisorplatform/api/VisitorSessionControllerTest.java`
- Create: `src/test/java/com/advisorplatform/api/AiChatControllerTest.java`
- Delete: `src/main/java/com/advisorplatform/api/ChatController.java`
- Delete: `src/test/java/com/advisorplatform/api/ChatControllerTest.java`

**Important:** `ChatController` handles both visitor-session AND chat endpoints. Both delegates must be created before the controller is deleted, or the build will break.

The generated controller for `VisitorSession` tag is `VisitorSessionApiController` in `com.advisorplatform.generated.visitorsession.api`. The `@WebMvcTest` tests import it and also `@Import` the delegate so Spring can wire it.

- [ ] **Step 1: Write `VisitorSessionDelegate.java`**

```java
package com.advisorplatform.api;

import com.advisorplatform.generated.visitorsession.api.VisitorSessionApiDelegate;
import com.advisorplatform.generated.visitorsession.model.CreateSessionRequest;
import com.advisorplatform.generated.visitorsession.model.IdentifyRequest;
import com.advisorplatform.generated.visitorsession.model.SessionResponse;
import com.advisorplatform.generated.visitorsession.model.VisitorResponse;
import com.advisorplatform.service.VisitorService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class VisitorSessionDelegate implements VisitorSessionApiDelegate {

    private final VisitorService visitorService;

    public VisitorSessionDelegate(VisitorService visitorService) {
        this.visitorService = visitorService;
    }

    @Override
    public ResponseEntity<VisitorResponse> identify(IdentifyRequest request) {
        var visitor = visitorService.findOrCreate(request.getBrowserToken());
        var response = new VisitorResponse();
        response.setVisitorId(visitor.getId());
        response.setBrowserToken(visitor.getBrowserToken());
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<SessionResponse> createSession(CreateSessionRequest request) {
        var session = visitorService.createSession(request.getVisitorId());
        var response = new SessionResponse();
        response.setSessionId(session.getId());
        response.setCreatedAt(session.getCreatedAt().toString());
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<List<SessionResponse>> getSessions(UUID visitorId) {
        var sessions = visitorService.getSessions(visitorId).stream()
                .map(s -> {
                    var r = new SessionResponse();
                    r.setSessionId(s.getId());
                    r.setCreatedAt(s.getCreatedAt().toString());
                    return r;
                })
                .toList();
        return ResponseEntity.ok(sessions);
    }
}
```

- [ ] **Step 2: Write `AiChatDelegate.java`**

The `streamChat` endpoint returns `Flux<String>` with SSE content type. The generated delegate interface returns `ResponseEntity<Void>` for streaming by default — override the controller method directly instead of using the delegate for the stream endpoint. Check the generated `AiChatApiDelegate` interface:

```bash
cat target/generated-sources/openapi/com/advisorplatform/generated/aichat/api/AiChatApiDelegate.java
```

Write the delegate matching the generated method signatures:

```java
package com.advisorplatform.api;

import com.advisorplatform.ai.PlannerAiService;
import com.advisorplatform.domain.entity.AiMessage;
import com.advisorplatform.generated.aichat.api.AiChatApiDelegate;
import com.advisorplatform.generated.aichat.model.ChatRequest;
import com.advisorplatform.generated.aichat.model.ChatResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Component
public class AiChatDelegate implements AiChatApiDelegate {

    private final PlannerAiService plannerAiService;

    public AiChatDelegate(PlannerAiService plannerAiService) {
        this.plannerAiService = plannerAiService;
    }

    @Override
    public ResponseEntity<ChatResponse> chat(UUID sessionId, ChatRequest request) {
        AiMessage message = plannerAiService.chat(sessionId, request.getMessage());
        var response = new ChatResponse();
        response.setMessageId(message.getId());
        response.setContent(message.getContent());
        response.setModel(message.getModelVersion());
        response.setCreatedAt(message.getCreatedAt().toString());
        return ResponseEntity.ok(response);
    }
}
```

**Note on `streamChat`:** The openapi-generator spring delegate pattern does not handle reactive `Flux` returns well for SSE. After writing the above, check the generated `AiChatApiDelegate` interface to see what `streamChat` signature it generates. If it returns `ResponseEntity<Void>` or similar, you will need a custom `@RestController` for the stream endpoint instead of using the delegate for it. Add a minimal `AiChatStreamController.java`:

```java
package com.advisorplatform.api;

import com.advisorplatform.ai.PlannerAiService;
import com.advisorplatform.generated.aichat.model.ChatRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
public class AiChatStreamController {

    private final PlannerAiService plannerAiService;

    public AiChatStreamController(PlannerAiService plannerAiService) {
        this.plannerAiService = plannerAiService;
    }

    @PostMapping(value = "/{sessionId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChat(
            @PathVariable UUID sessionId,
            @Valid @RequestBody ChatRequest request) {
        return plannerAiService.streamChat(sessionId, request.getMessage());
    }
}
```

If the generated delegate interface already handles Flux properly, skip `AiChatStreamController` and implement the stream method in `AiChatDelegate` instead.

- [ ] **Step 3: Verify it compiles**

```bash
mvn compile -q
```

Expected: BUILD SUCCESS. Fix any import or signature mismatch errors before continuing.

- [ ] **Step 4: Write `VisitorSessionControllerTest.java`**

This test replaces the visitor-session portion of `ChatControllerTest`. It targets the generated controller class and imports the hand-written delegate.

```java
package com.advisorplatform.api;

import com.advisorplatform.domain.entity.AiSession;
import com.advisorplatform.domain.entity.Visitor;
import com.advisorplatform.generated.visitorsession.api.VisitorSessionApiController;
import com.advisorplatform.service.VisitorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = VisitorSessionApiController.class)
@Import(VisitorSessionDelegate.class)
@TestPropertySource(properties = "spring.ai.anthropic.api-key=test-key")
class VisitorSessionControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean VisitorService visitorService;

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

    @Test
    void createSession_validVisitorId_returns200WithSessionId() throws Exception {
        UUID visitorId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        AiSession session = new AiSession();
        ReflectionTestUtils.setField(session, "id", sessionId);
        ReflectionTestUtils.setField(session, "createdAt", Instant.parse("2026-03-28T10:00:00Z"));
        when(visitorService.createSession(eq(visitorId))).thenReturn(session);

        mockMvc.perform(post("/api/session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visitorId\":\"" + visitorId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(sessionId.toString()));
    }

    @Test
    void createSession_serviceThrowsIllegalArgument_returns400() throws Exception {
        when(visitorService.createSession(any()))
                .thenThrow(new IllegalArgumentException("Visitor not found: some-id"));

        mockMvc.perform(post("/api/session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visitorId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createSession_nullVisitorId_returns400() throws Exception {
        mockMvc.perform(post("/api/session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visitorId\":null}"))
                .andExpect(status().isBadRequest());
    }

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
}
```

- [ ] **Step 5: Write `AiChatControllerTest.java`**

```java
package com.advisorplatform.api;

import com.advisorplatform.ai.PlannerAiService;
import com.advisorplatform.domain.entity.AiMessage;
import com.advisorplatform.domain.entity.AiSession;
import com.advisorplatform.generated.aichat.api.AiChatApiController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {AiChatApiController.class, AiChatStreamController.class})
@Import(AiChatDelegate.class)
@TestPropertySource(properties = "spring.ai.anthropic.api-key=test-key")
class AiChatControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean PlannerAiService plannerAiService;

    @Test
    void chat_validRequest_returns200WithAssistantMessage() throws Exception {
        UUID sessionId = UUID.randomUUID();
        AiMessage response = AiMessage.assistantMessage(new AiSession(), "AI reply", "claude-3", 100, 250);
        ReflectionTestUtils.setField(response, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(response, "createdAt", Instant.parse("2026-03-28T10:00:00Z"));
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

    @Test
    void streamChat_validRequest_returnsEventStreamStatus200() throws Exception {
        UUID sessionId = UUID.randomUUID();
        when(plannerAiService.streamChat(eq(sessionId), eq("Hi"))).thenReturn(Flux.just("chunk1", "chunk2"));

        mockMvc.perform(post("/api/chat/{sessionId}/stream", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Hi\"}"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM));
    }
}
```

- [ ] **Step 6: Run the new tests to verify they pass before deleting the old controller**

```bash
mvn test -pl . -Dtest="VisitorSessionControllerTest,AiChatControllerTest" -q
```

Expected: Both test classes pass. If any test fails, fix before proceeding.

- [ ] **Step 7: Delete `ChatController.java` and `ChatControllerTest.java`**

```bash
rm src/main/java/com/advisorplatform/api/ChatController.java
rm src/test/java/com/advisorplatform/api/ChatControllerTest.java
```

- [ ] **Step 8: Run full test suite**

```bash
mvn test -q
```

Expected: All tests pass (the count will be the same as before since old tests were replaced 1:1). If tests fail, check for leftover imports referencing `ChatController`.

- [ ] **Step 9: Commit**

```bash
git add src/main/java/com/advisorplatform/api/ src/test/java/com/advisorplatform/api/
git commit -m "feat: migrate ChatController to VisitorSessionDelegate and AiChatDelegate (#<issue-number>)"
```

---

## Task 4: Implement MessagingDelegate, Delete MessageController

**Files:**
- Create: `src/main/java/com/advisorplatform/api/MessagingDelegate.java`
- Create: `src/test/java/com/advisorplatform/api/MessagingControllerTest.java`
- Delete: `src/main/java/com/advisorplatform/api/MessageController.java`
- Delete: `src/test/java/com/advisorplatform/api/MessageControllerTest.java`

- [ ] **Step 1: Write `MessagingDelegate.java`**

```java
package com.advisorplatform.api;

import com.advisorplatform.generated.messaging.api.MessagingApiDelegate;
import com.advisorplatform.generated.messaging.model.*;
import com.advisorplatform.service.MessageService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class MessagingDelegate implements MessagingApiDelegate {

    private final MessageService messageService;

    public MessagingDelegate(MessageService messageService) {
        this.messageService = messageService;
    }

    @Override
    public ResponseEntity<CreateThreadResponse> createThread(CreateThreadRequest request) {
        var thread = messageService.createThread(
                request.getVisitorId(),
                request.getAiSessionId(),
                request.getSubject(),
                request.getContent());
        var response = new CreateThreadResponse();
        response.setThreadId(thread.getId());
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<List<ThreadSummary>> getThreads(UUID visitorId) {
        var summaries = messageService.getThreads(visitorId).stream()
                .map(t -> {
                    var s = new ThreadSummary();
                    s.setThreadId(t.getId());
                    s.setSubject(t.getSubject());
                    s.setStatus(t.getStatus());
                    s.setUpdatedAt(t.getUpdatedAt().toString());
                    return s;
                })
                .toList();
        return ResponseEntity.ok(summaries);
    }

    @Override
    public ResponseEntity<List<MessageSummary>> getMessages(UUID threadId) {
        var summaries = messageService.getMessages(threadId).stream()
                .map(m -> {
                    var s = new MessageSummary();
                    s.setMessageId(m.getId());
                    s.setSenderRole(m.getSenderRole());
                    s.setContent(m.getContent());
                    s.setCreatedAt(m.getCreatedAt().toString());
                    return s;
                })
                .toList();
        return ResponseEntity.ok(summaries);
    }

    @Override
    public ResponseEntity<AddMessageResponse> addMessage(UUID threadId, AddMessageRequest request) {
        var message = messageService.addMessage(threadId, request.getContent());
        var response = new AddMessageResponse();
        response.setMessageId(message.getId());
        return ResponseEntity.ok(response);
    }
}
```

- [ ] **Step 2: Write `MessagingControllerTest.java`**

```java
package com.advisorplatform.api;

import com.advisorplatform.domain.entity.MessageThread;
import com.advisorplatform.domain.entity.ThreadMessage;
import com.advisorplatform.domain.entity.Visitor;
import com.advisorplatform.generated.messaging.api.MessagingApiController;
import com.advisorplatform.service.MessageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
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

@WebMvcTest(controllers = MessagingApiController.class)
@Import(MessagingDelegate.class)
@TestPropertySource(properties = "spring.ai.anthropic.api-key=test-key")
class MessagingControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean MessageService messageService;

    @Test
    void createThread_validRequest_returns200WithThreadId() throws Exception {
        UUID visitorId = UUID.randomUUID();
        UUID threadId = UUID.randomUUID();
        Visitor visitor = new Visitor();
        MessageThread thread = MessageThread.builder().visitor(visitor).subject("Help me").build();
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

    @Test
    void createThread_nullVisitorId_returns400() throws Exception {
        mockMvc.perform(post("/api/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visitorId\":null,\"content\":\"Hello\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getThreads_validVisitorId_returns200WithList() throws Exception {
        UUID visitorId = UUID.randomUUID();
        UUID threadId = UUID.randomUUID();
        Visitor visitor = new Visitor();
        MessageThread thread = MessageThread.builder().visitor(visitor).subject("My question").build();
        ReflectionTestUtils.setField(thread, "id", threadId);
        ReflectionTestUtils.setField(thread, "updatedAt", Instant.parse("2026-03-28T10:00:00Z"));
        when(messageService.getThreads(visitorId)).thenReturn(List.of(thread));

        mockMvc.perform(get("/api/visitor/{visitorId}/threads", visitorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].threadId").value(threadId.toString()))
                .andExpect(jsonPath("$[0].subject").value("My question"))
                .andExpect(jsonPath("$[0].status").value("open"));
    }

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
}
```

- [ ] **Step 3: Run new tests before deleting old controller**

```bash
mvn test -pl . -Dtest="MessagingControllerTest" -q
```

Expected: All 8 tests pass.

- [ ] **Step 4: Delete `MessageController.java` and `MessageControllerTest.java`**

```bash
rm src/main/java/com/advisorplatform/api/MessageController.java
rm src/test/java/com/advisorplatform/api/MessageControllerTest.java
```

- [ ] **Step 5: Run full test suite**

```bash
mvn test -q
```

Expected: All tests pass.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/advisorplatform/api/ src/test/java/com/advisorplatform/api/
git commit -m "feat: migrate MessageController to MessagingDelegate (#<issue-number>)"
```

---

## Task 5: Add Swagger UI Configuration

**Files:**
- Create: `src/main/java/com/advisorplatform/config/SwaggerConfig.java`

- [ ] **Step 1: Write `SwaggerConfig.java`**

```java
package com.advisorplatform.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public GroupedOpenApi visitorSessionApi() {
        return GroupedOpenApi.builder()
                .group("visitor-session")
                .pathsToMatch("/api/visitor/**", "/api/session/**")
                .build();
    }

    @Bean
    public GroupedOpenApi aiChatApi() {
        return GroupedOpenApi.builder()
                .group("ai-chat")
                .pathsToMatch("/api/chat/**")
                .build();
    }

    @Bean
    public GroupedOpenApi messagingApi() {
        return GroupedOpenApi.builder()
                .group("messaging")
                .pathsToMatch("/api/message/**", "/api/thread/**")
                .build();
    }
}
```

- [ ] **Step 2: Run full test suite**

```bash
mvn test -q
```

Expected: All tests pass.

- [ ] **Step 3: Smoke test Swagger UI (requires running app)**

```bash
mvn spring-boot:run &
sleep 8
curl -s http://localhost:8080/swagger-ui.html | grep -c "swagger"
curl -s "http://localhost:8080/v3/api-docs/groups" | grep -o '"group":"[^"]*"' | sort
kill %1
```

Expected second command output (order may vary):
```
"group":"ai-chat"
"group":"messaging"
"group":"visitor-session"
```

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/advisorplatform/config/SwaggerConfig.java
git commit -m "feat: add Swagger UI with grouped API tabs (#<issue-number>)"
```

---

## Task 6: Export YAML Snapshots to docs/api/

**Files:**
- Create: `docs/api/visitor-session-api.yaml`
- Create: `docs/api/ai-chat-api.yaml`
- Create: `docs/api/messaging-api.yaml`

These are static copies of the spec files for offline review and future TypeScript codegen. They are identical to the source specs in `src/main/resources/api/` — copy them now and keep them in sync as specs evolve.

- [ ] **Step 1: Create the docs/api/ directory and copy specs**

```bash
mkdir -p docs/api
cp src/main/resources/api/visitor-session-api.yaml docs/api/
cp src/main/resources/api/ai-chat-api.yaml docs/api/
cp src/main/resources/api/messaging-api.yaml docs/api/
```

- [ ] **Step 2: Commit**

```bash
git add docs/api/
git commit -m "docs: export OpenAPI YAML snapshots to docs/api/ (#<issue-number>)"
```

---

## Self-Review Checklist

**Spec coverage:**
- [x] openapi-generator-maven-plugin configured with 4 executions
- [x] springdoc dependency added
- [x] JaCoCo configured with `com/advisorplatform/generated/**` exclusion
- [x] `target/` already covered by existing `.gitignore` — no change needed
- [x] All 3 Phase 1 specs written matching existing endpoints exactly
- [x] advisor-api.yaml placeholder created
- [x] VisitorSessionDelegate, AiChatDelegate, MessagingDelegate implemented
- [x] All old controllers deleted
- [x] All tests migrated and passing
- [x] SwaggerConfig with 3 groups
- [x] docs/api/ snapshots exported

**Known adaptation point:** The `streamChat` endpoint returns `Flux<String>` with SSE content type, which openapi-generator's delegate pattern does not handle natively. Task 3 Step 2 includes conditional logic: check the generated `AiChatApiDelegate` signature and use `AiChatStreamController` if needed. The test in Task 3 Step 5 accounts for both the generated controller and the stream controller in `@WebMvcTest`.
