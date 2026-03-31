package com.advisorplatform.service;

import com.advisorplatform.domain.entity.AiSession;
import com.advisorplatform.domain.entity.MessageThread;
import com.advisorplatform.domain.entity.ThreadMessage;
import com.advisorplatform.domain.entity.Visitor;
import com.advisorplatform.domain.repository.AiSessionRepository;
import com.advisorplatform.domain.repository.MessageThreadRepository;
import com.advisorplatform.domain.repository.ThreadMessageRepository;
import com.advisorplatform.domain.repository.VisitorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class MessageService {

    private final MessageThreadRepository threadRepo;
    private final ThreadMessageRepository messageRepo;
    private final VisitorRepository visitorRepo;
    private final AiSessionRepository sessionRepo;

    public MessageService(MessageThreadRepository threadRepo,
                          ThreadMessageRepository messageRepo,
                          VisitorRepository visitorRepo,
                          AiSessionRepository sessionRepo) {
        this.threadRepo = threadRepo;
        this.messageRepo = messageRepo;
        this.visitorRepo = visitorRepo;
        this.sessionRepo = sessionRepo;
    }

    @Transactional
    public MessageThread createThread(UUID visitorId, UUID aiSessionId, String subject, String content) {
        Visitor visitor = visitorRepo.findById(visitorId)
                .orElseThrow(() -> new IllegalArgumentException("Visitor not found: " + visitorId));

        AiSession aiSession = null;
        if (aiSessionId != null) {
            aiSession = sessionRepo.findById(aiSessionId)
                    .orElseThrow(() -> new IllegalArgumentException("Session not found: " + aiSessionId));
        }

        MessageThread thread = MessageThread.builder()
                .visitor(visitor)
                .aiSession(aiSession)
                .subject(subject)
                .build();
        thread = threadRepo.save(thread);

        ThreadMessage firstMessage = ThreadMessage.builder()
                .thread(thread)
                .senderRole("visitor")
                .content(content)
                .build();
        messageRepo.save(firstMessage);

        return thread;
    }

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

    public List<MessageThread> getThreads(UUID visitorId) {
        return threadRepo.findByVisitorIdOrderByUpdatedAtDesc(visitorId);
    }

    public List<ThreadMessage> getMessages(UUID threadId) {
        return messageRepo.findByThreadIdOrderByCreatedAtAsc(threadId);
    }
}
