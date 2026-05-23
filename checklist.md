# Community Bot — Feature Checklist

Compared against [requirements.md](requirements.md).  
**Legend:** ✅ Implemented · 🟡 Partial · ❌ Missing

Last reviewed: 2026-05-23

---

## 1. Community Platform

| Feature | Status | Notes |
|--------|--------|-------|
| Channels (create, list, join) | ✅ | `ChannelController`, workspace sidebar in `WorkspacePage.tsx` |
| Public / private channel types | 🟡 | `ChannelType` in schema; basic join UX only |
| Threads (reply in thread) | 🟡 | Backend: `thread_root_id`, `findThreadReplies()`; **no thread UI**, frontend always sends `threadRootId: null` |
| Thread reply API (list replies) | ❌ | Repository method exists; no REST endpoint exposed |
| User profiles | 🟡 | OAuth sync → `GET /api/auth/me` (name, email, avatar); no profile page, bio, or interests |
| Real-time messaging (WebSocket) | ✅ | STOMP + Redis fan-out; `WebSocketContext.tsx`, `ChatWebSocketController` |
| Message history (paginated) | ✅ | `GET /api/channels/{id}/messages`; fixed page size, no “load more” in UI |
| Typing indicators | 🟡 | Backend `/typing` exists; no frontend component |
| OAuth authentication (Google, GitHub) | ✅ | `SecurityConfig`, `LoginPage.tsx`, JWT refresh |
| Role system (admin, moderator, member) | 🟡 | `WorkspaceRole` enforced server-side; **no API/UI to assign roles** |
| Role-gated UI | 🟡 | Moderation/settings links shown to all users; backend returns 403 for non-mods |
| Message delete by moderator | ❌ | `MessageService.delete()` has TODO for mod override |
| Reactions | 🟡 | API + display in `MessageItem.tsx`; **no UI to add reactions** |
| File attachments | ✅ | Upload, MinIO, ClamAV scan, display in chat |
| Org / workspace hierarchy | ✅ | Beyond minimum; org create/join on dashboard |

---

## 2. Bot Capabilities

| Feature | Status | Notes |
|--------|--------|-------|
| Answer questions via RAG (FAQ / guidelines) | ✅ | pgvector + `RagService`, `searchFaqDocuments` tool, document ingest API, `AskBotPanel.tsx` |
| In-channel `@bot` mentions | ✅ | `BotMentionListener` replies in thread under parent message |
| Detect inappropriate content | ✅ | `ContentClassifierService` (LLM JSON: toxic, hate, harassment, spam, threat) |
| Flag messages for review | ✅ | `MessageFlagListener` → `moderation_flags`; `ModerationDashboard.tsx` |
| Auto-hide flagged messages | ❌ | Flag sets `FLAGGED` but feed still shows them; hide only after moderator “Remove” |
| Moderator approve / remove flow | ✅ | `ModerationController` approve → keep; remove → `HIDDEN` |
| Summarize long threads (10+ messages) | 🟡 | Agent tool `summarizeThread()` in Ask Bot only; **no auto-trigger at 10+ replies** |
| Welcome new members | 🟡 | `WelcomeMessageListener` posts template to `#general`; `{name}` substitution only |
| Personalized welcome (profile / interests) | ❌ | No interest fields; not LLM-personalized |
| Default welcome message on join | ❌ | Template must be configured manually in workspace settings |
| Scheduled automated posts | ✅ | Cron via `ScheduledPostService`, CRUD in `WorkspaceSettings.tsx` |
| Agent-proposed schedules (human confirm) | ✅ | `proposeScheduledPost` + `AgentController` confirm flow |
| Example “Weekly Digest Friday 5pm” | ❌ | No bundled schedule template |

---

## 3. Intelligence Layer

| Feature | Status | Notes |
|--------|--------|-------|
| FAQ knowledge base (20+ Q&A pairs) | 🟡 | [FAQ.md](FAQ.md) has **34 Q&A pairs** as user docs; **not auto-ingested** into vector store |
| Pre-seeded RAG documents at deploy | ❌ | Ingestion is upload-only; no seed migration or sample PDF in repo |
| Content classification for moderation | ✅ | `ContentClassifierService` with configurable confidence threshold |
| Profanity / keyword fallback | ❌ | LLM-only; fails open on API errors |
| Context-aware responses (thread history) | 🟡 | `fetchThreadTranscript`, channel search, Redis chat memory; agent must call tools |
| Long-term memory across sessions | ✅ | `LongTermMemoryService` + embeddings |
| Multi-agent orchestration | 🟡 | Single assistant with tools (`MultiAgentService`), not separate specialist agents |

---

## 4. Cross-Cutting (beyond requirements.md)

| Feature | Status | Notes |
|--------|--------|-------|
| AI usage / token tracking | ✅ | `LlmUsageService`, `V10__llm_usage_events.sql`, chat + embedding + classifier |
| Usage UI (per user + project totals) | ✅ | `AiUsageSummary.tsx` on dashboard and workspace sidebar |
| Dedicated per-user usage profile page | ❌ | Shown on dashboard/sidebar only; no category breakdown in UI |
| Streaming agent responses | ✅ | `POST .../agent/ask/stream`, `AgentTimeline.tsx` |
| Rate limiting on agent | ✅ | `AgentRateLimiter` |
| Ban members | ✅ | Workspace ban API (mod/admin) |
| Deployment / global access (20 users) | 🟡 | Stack supports it (Redis, Postgres, MinIO); no deployment docs in repo |

---

## 5. Presentation & Documentation (requirements.md asks for)

| Deliverable | Status | Notes |
|-------------|--------|-------|
| Architecture diagrams | ❌ | Not in repo |
| Component layers & technology choices (pros/cons) | ❌ | Not documented in repo |
| Scalability / Slack-parity gap analysis | ❌ | Not documented in repo |
| Deployment guide (small global user base) | 🟡 | `FAQ.md` covers local dev; no production deploy doc |
| Demo script (bot Q&A, moderation, summarize) | 🟡 | Features exist; no written demo checklist |

---

## Summary

| Area | ✅ | 🟡 | ❌ |
|------|---:|---:|---:|
| Community platform | 6 | 7 | 3 |
| Bot capabilities | 5 | 5 | 4 |
| Intelligence layer | 2 | 3 | 3 |
| Cross-cutting | 4 | 1 | 1 |
| Presentation / docs | 0 | 2 | 3 |

---

## Highest-Priority Gaps (for grading / demo)

1. **Threads UI** — backend ready; users cannot open, reply to, or view threads in the frontend.
2. **Auto thread summarization at 10+ messages** — requirement calls this out explicitly; only manual Ask Bot summarization exists.
3. **Pre-seeded FAQ in RAG (20+ pairs)** — upload pipeline works; nothing ships pre-ingested (FAQ.md is docs only).
4. **Personalized welcome by profile/interests** — template + `{name}` only.
5. **Role management UI/API** — promote/demote moderators and admins.
6. **Flagged message visibility** — flagged content stays in the channel feed until a moderator removes it.
7. **Reactions picker** — API exists; users cannot add reactions from the UI.
8. **Architecture & presentation materials** — diagrams, stack rationale, scalability notes for the 20-minute presentation.

---

## Stack Implemented (for reference)

| Layer | Technology |
|-------|------------|
| Backend | Java 21, Spring Boot 3, Spring Security OAuth2/JWT |
| Real-time | STOMP WebSocket, Redis pub/sub |
| Database | PostgreSQL + pgvector (Flyway migrations) |
| Object storage | MinIO |
| AI | LangChain4j, OpenAI (chat, embeddings, classifier) |
| Frontend | React, TypeScript, Vite, TanStack Query, Tailwind |
| Auth | Google + GitHub OAuth2 |

FIXES

1) Moderation needs to be fixed using langgraph
2) ask bot to be fixed. after sending a message, the bot keeps thinking and never responds.
3) messages latency needs to be fixed
4) code needs to synced and merged from 2 braches into main branch

FEATUTES
1) when user joins an organization, it doesn't access to all channels automatically.
2) delete organization
the modertor assigns channel to the user

3) each user should have a profile information with following features
    about me - user can put information about him/her
    contact information - email address/phone. user should have option to display/hide contact information
    organization
    user can set notification settings 
        show all messages immediately
        show only followed threads
        show only mentions
    user should have interests
    
4) application should have the threads. users can respond to a message in the channel or they can choose to response in a thread. 

5) application chat box should allow tagging and mentioning
6) the user can have multiple roles at same time..
admin is application admin
moderator, member is for channels

7) Summarize long threads (10+ messages) into digestible overviews

8) Welcome new members with personalized onboarding messages based on their profile/interests
9) Schedule automated posts (e.g., "Weekly Digest every Friday 5pm")

10) Build FAQ knowledge base (minimum 20 Q&A pairs covering community rules, technical help, resources)

11) implement content classification for moderation

12) user should be allowed to search for other members by namme and able to send direct messages

13) users should be able to send reactions/emojis

14) users should be able to share images and files in the chat