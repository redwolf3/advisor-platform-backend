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
    void getThreads_delegatesToRepositoryAndReturnsResult() {
        UUID visitorId = UUID.randomUUID();
        List<MessageThread> threads = List.of(MessageThread.builder().build());
        when(threadRepo.findByVisitorIdOrderByUpdatedAtDesc(visitorId)).thenReturn(threads);

        assertThat(messageService.getThreads(visitorId)).isSameAs(threads);
    }

    @Test
    void getMessages_delegatesToRepositoryAndReturnsResult() {
        UUID threadId = UUID.randomUUID();
        MessageThread thread = MessageThread.builder().build();
        List<ThreadMessage> messages = List.of(
                ThreadMessage.builder().thread(thread).senderRole("visitor").content("Hi").build());
        when(messageRepo.findByThreadIdOrderByCreatedAtAsc(threadId)).thenReturn(messages);

        assertThat(messageService.getMessages(threadId)).isSameAs(messages);
    }
}
