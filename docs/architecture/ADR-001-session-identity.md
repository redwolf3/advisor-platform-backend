# ADR-001: Session Identity — Browser Token + Find-or-Create

**Status:** Accepted
**Date:** 2026-04-01

---

## Context

The platform needs to identify visitors across page loads without requiring account creation. Phase 1 targets first-time visitors who want to plan a trip immediately, without friction. Requiring email/password login upfront would reduce conversion.

We also need to link AI chat sessions and message threads to a persistent visitor identity, so returning visitors can see their history.

## Decision

Use a randomly-generated browser token stored in `localStorage` as the visitor identity for Phase 1.

On every page load, the frontend calls `POST /api/v1/visitor/identify` with the stored token. The backend does a find-or-create on the `visitor` table using `browser_token` as a unique key, updating `last_seen_at` on each visit.

No password, no email, no session cookie — just a UUID in localStorage.

## Consequences

**Positive:**
- Zero friction for new visitors — no sign-up required
- Simple to implement and reason about
- History persists across sessions on the same device/browser

**Negative:**
- Identity is lost if the user clears localStorage or switches browsers
- No way to merge identities (e.g., same user on mobile and desktop)
- No authentication — any client with a valid `visitorId` can access that visitor's data

**Phase 2 plan:** Replace with Supabase-based auth (JWT verification in `infra/`). The `visitor` table already has `email` and `email_verified` columns for this transition. The `visitorId` in all requests will map to the authenticated user once auth is in place.
