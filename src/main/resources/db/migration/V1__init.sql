-- V1__init.sql
-- Prototype schema: visitor identity, AI sessions, messages, contact threads

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Anonymous visitor identity
CREATE TABLE visitor (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    browser_token TEXT        NOT NULL UNIQUE,
    email         TEXT        UNIQUE,
    email_verified BOOLEAN    NOT NULL DEFAULT FALSE,
    tos_accepted_at TIMESTAMPTZ,
    tos_version   TEXT,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_seen_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- OTP for magic link / return login
CREATE TABLE auth_otp (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    visitor_id  UUID        NOT NULL REFERENCES visitor(id),
    otp_hash    TEXT        NOT NULL,
    purpose     TEXT        NOT NULL CHECK (purpose IN ('magic_link', 'return_otp')),
    expires_at  TIMESTAMPTZ NOT NULL,
    used        BOOLEAN     NOT NULL DEFAULT FALSE,
    attempt_count INT       NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Durable visitor sessions (issued post-OTP)
CREATE TABLE visitor_session (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    visitor_id    UUID        NOT NULL REFERENCES visitor(id),
    session_token TEXT        NOT NULL UNIQUE,
    expires_at    TIMESTAMPTZ NOT NULL,
    revoked       BOOLEAN     NOT NULL DEFAULT FALSE,
    last_used_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- AI planning conversation session
CREATE TABLE ai_session (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    visitor_id   UUID        NOT NULL REFERENCES visitor(id),
    title        TEXT,
    trip_context JSONB,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Individual AI turns (user + assistant messages)
CREATE TABLE ai_message (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id    UUID        NOT NULL REFERENCES ai_session(id),
    role          TEXT        NOT NULL CHECK (role IN ('user', 'assistant')),
    content       TEXT        NOT NULL,
    token_count   INT,
    model_version TEXT,
    latency_ms    INT,
    -- reinforcement fields (populated via admin review)
    quality_tag   TEXT CHECK (quality_tag IN ('good', 'bad', 'needs_edit')),
    review_note   TEXT,
    promoted_to_kb BOOLEAN    NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Human contact thread (linked to AI session that triggered it)
CREATE TABLE message_thread (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    visitor_id    UUID        NOT NULL REFERENCES visitor(id),
    ai_session_id UUID        REFERENCES ai_session(id),
    subject       TEXT,
    status        TEXT        NOT NULL DEFAULT 'open'
                      CHECK (status IN ('open', 'pending_reply', 'resolved', 'closed')),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Messages within a contact thread
CREATE TABLE thread_message (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    thread_id      UUID        NOT NULL REFERENCES message_thread(id),
    sender_role    TEXT        NOT NULL CHECK (sender_role IN ('visitor', 'advisor')),
    content        TEXT        NOT NULL,
    email_notified BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Indexes
CREATE INDEX idx_visitor_browser_token  ON visitor(browser_token);
CREATE INDEX idx_visitor_email          ON visitor(email) WHERE email IS NOT NULL;
CREATE INDEX idx_auth_otp_visitor       ON auth_otp(visitor_id);
CREATE INDEX idx_visitor_session_token  ON visitor_session(session_token);
CREATE INDEX idx_ai_session_visitor     ON ai_session(visitor_id);
CREATE INDEX idx_ai_message_session     ON ai_message(session_id);
CREATE INDEX idx_ai_message_quality     ON ai_message(quality_tag) WHERE quality_tag IS NOT NULL;
CREATE INDEX idx_message_thread_visitor ON message_thread(visitor_id);
CREATE INDEX idx_thread_message_thread  ON thread_message(thread_id);
