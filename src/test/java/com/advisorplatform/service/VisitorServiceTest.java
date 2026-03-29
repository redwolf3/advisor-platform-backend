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
        ArgumentCaptor<Visitor> captor = ArgumentCaptor.forClass(Visitor.class);
        verify(visitorRepository).save(captor.capture());
        assertThat(captor.getValue().getLastSeenAt()).isNotNull();
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
    void getSessions_delegatesToRepositoryAndReturnsResult() {
        UUID visitorId = UUID.randomUUID();
        List<AiSession> sessions = List.of(new AiSession(), new AiSession());
        when(aiSessionRepository.findByVisitorIdOrderByCreatedAtDesc(visitorId)).thenReturn(sessions);

        assertThat(visitorService.getSessions(visitorId)).isSameAs(sessions);
    }
}
