-- Enable UUID generation
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Visitor identity (anonymous until email collected)
CREATE TABLE visitor (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    browser_token   TEXT NOT NULL UNIQUE,
    email           TEXT UNIQUE,
    email_verified  BOOLEAN NOT NULL DEFAULT FALSE,
    tos_accepted_at TIMESTAMPTZ,
    tos_version     TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_seen_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_visitor_browser_token ON visitor(browser_token);
CREATE INDEX idx_visitor_email ON visitor(email) WHERE email IS NOT NULL;

-- Durable sessions
CREATE TABLE visitor_session (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    visitor_id    UUID NOT NULL REFERENCES visitor(id),
    session_token TEXT NOT NULL UNIQUE,
    issued_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at    TIMESTAMPTZ NOT NULL,
    revoked       BOOLEAN NOT NULL DEFAULT FALSE,
    user_agent    TEXT,
    last_used_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_visitor_session_token ON visitor_session(session_token);
CREATE INDEX idx_visitor_session_visitor ON visitor_session(visitor_id);

-- OTP (magic link + return login)
CREATE TABLE auth_otp (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    visitor_id    UUID NOT NULL REFERENCES visitor(id),
    otp_hash      TEXT NOT NULL,
    purpose       TEXT NOT NULL CHECK (purpose IN ('magic_link', 'return_otp')),
    expires_at    TIMESTAMPTZ NOT NULL,
    used          BOOLEAN NOT NULL DEFAULT FALSE,
    attempt_count INT NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- OTP rate limiting
CREATE TABLE otp_request_log (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email        TEXT NOT NULL,
    requested_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ip_address   TEXT
);

CREATE INDEX idx_otp_request_email_time ON otp_request_log(email, requested_at);

-- AI chat sessions
CREATE TABLE ai_session (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    visitor_id   UUID NOT NULL REFERENCES visitor(id),
    title        TEXT,
    trip_context JSONB,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_ai_session_visitor ON ai_session(visitor_id);

-- AI messages within a session
CREATE TABLE ai_message (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id    UUID NOT NULL REFERENCES ai_session(id),
    role          TEXT NOT NULL CHECK (role IN ('user', 'assistant')),
    content       TEXT NOT NULL,
    token_count   INT,
    model_version TEXT,
    latency_ms    INT,
    quality_tag   TEXT CHECK (quality_tag IN ('good', 'bad', 'needs_edit')),
    reviewed_by   TEXT,
    review_note   TEXT,
    promoted_to_kb BOOLEAN NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_ai_message_session ON ai_message(session_id);
CREATE INDEX idx_ai_message_quality ON ai_message(quality_tag) WHERE quality_tag IS NOT NULL;

-- Human message threads
CREATE TABLE message_thread (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    visitor_id     UUID NOT NULL REFERENCES visitor(id),
    ai_session_id  UUID REFERENCES ai_session(id),
    subject        TEXT,
    status         TEXT NOT NULL DEFAULT 'open'
                   CHECK (status IN ('open', 'pending_reply', 'resolved', 'closed')),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_message_thread_visitor ON message_thread(visitor_id);
CREATE INDEX idx_message_thread_status ON message_thread(status);

-- Messages within a thread
CREATE TABLE thread_message (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    thread_id      UUID NOT NULL REFERENCES message_thread(id),
    sender_role    TEXT NOT NULL CHECK (sender_role IN ('visitor', 'advisor')),
    content        TEXT NOT NULL,
    sent_via       TEXT CHECK (sent_via IN ('app', 'email')),
    email_notified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_thread_message_thread ON thread_message(thread_id);

-- Knowledge base (feeds AI context)
CREATE TABLE knowledge_base_entry (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category       TEXT NOT NULL,
    title          TEXT NOT NULL,
    content        TEXT NOT NULL,
    source         TEXT CHECK (source IN ('manual', 'promoted_ai', 'promoted_thread')),
    promoted_from  UUID,
    active         BOOLEAN NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_kb_category_active ON knowledge_base_entry(category) WHERE active = TRUE;
