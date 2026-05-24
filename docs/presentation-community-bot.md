---
marp: true
theme: default
paginate: true
header: 'Community Bot — Capstone'
footer: 'Community Bot | 20-minute presentation'
style: |
  section { font-size: 28px; }
  section.lead h1 { font-size: 2.4em; }
  section.lead p { font-size: 1.05em; color: #444; }
  h2 { color: #1e40af; margin-bottom: 0.4em; }
  table { font-size: 0.72em; width: 100%; }
  th { background: #eff6ff; }
  ul { margin: 0.3em 0; }
  li { margin: 0.15em 0; }
  .cols { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 0.8rem; font-size: 0.78em; }
  .col { background: #f8fafc; border-radius: 8px; padding: 0.6rem 0.8rem; }
  .col h3 { font-size: 1em; margin: 0 0 0.4em; color: #1e3a8a; }
  .ai-box { background: #fef3c7; border: 2px solid #d97706; border-radius: 6px; padding: 0.15em 0.45em; font-weight: 600; color: #92400e; }
  .note { font-size: 0.65em; color: #64748b; margin-top: 0.8em; }
  blockquote { font-size: 0.85em; border-left: 4px solid #2563eb; background: #f0f9ff; padding: 0.5em 0.8em; margin: 0.5em 0; }
  section.arch h2 { margin-bottom: 0.25em; font-size: 1.1em; }
  .arch { font-size: 0.52em; line-height: 1.25; margin-top: 0.2em; }
  .arch-layer { border-radius: 8px; padding: 0.35rem 0.5rem; text-align: center; margin: 0 auto; }
  .arch-title { font-weight: 700; font-size: 1.05em; margin-bottom: 0.15em; }
  .arch-sub { font-size: 0.92em; color: #334155; }
  .arch-client { background: #eff6ff; border: 2px solid #2563eb; width: 72%; }
  .arch-api { background: #f8fafc; border: 2px solid #64748b; width: 88%; }
  .arch-ai { background: #fef3c7; border: 3px solid #d97706; width: 92%; }
  .arch-data { background: #f1f5f9; border: 2px solid #64748b; width: 68%; }
  .arch-ext { background: #fce7f3; border: 3px solid #db2777; width: 26%; }
  .arch-pills { display: flex; justify-content: center; gap: 0.45rem; flex-wrap: wrap; margin-top: 0.25em; }
  .arch-pill { background: #fff; border: 1px solid #94a3b8; border-radius: 6px; padding: 0.2em 0.55em; font-size: 0.9em; }
  .arch-pill-ai { background: #fff7ed; border-color: #d97706; }
  .arch-arrow { text-align: center; color: #64748b; font-size: 1.3em; line-height: 1; margin: 0.1em 0; }
  .arch-mid { display: flex; justify-content: center; align-items: stretch; gap: 0.6rem; margin: 0.15em auto; width: 96%; }
  .arch-legend { display: flex; gap: 1.2rem; justify-content: center; font-size: 0.88em; margin-top: 0.35em; color: #475569; }
  .arch-leg-box { display: inline-block; width: 14px; height: 14px; border-radius: 3px; vertical-align: middle; margin-right: 4px; }
  section.sysdiag h2 { margin-bottom: 0.2em; font-size: 1.05em; }
  .sys { font-size: 0.48em; line-height: 1.2; margin-top: 0.1em; }
  .sys-node { border-radius: 8px; padding: 0.35rem 0.5rem; text-align: center; }
  .sys-title { font-weight: 700; font-size: 1.05em; }
  .sys-sub { font-size: 0.88em; color: #334155; margin-top: 0.1em; }
  .sys-browser { background: #eff6ff; border: 2px solid #2563eb; width: 38%; margin: 0 auto; }
  .sys-backend { background: #f8fafc; border: 2px solid #475569; flex: 1; min-width: 0; }
  .sys-ai-inner { background: #fef3c7; border: 2px solid #d97706; border-radius: 6px; padding: 0.25em 0.4em; margin-top: 0.3em; }
  .sys-ext { background: #fce7f3; border: 2px solid #db2777; width: 100%; }
  .sys-auto { background: #ede9fe; border: 2px solid #7c3aed; width: 100%; }
  .sys-store { background: #f1f5f9; border: 2px solid #64748b; flex: 1; }
  .sys-links { display: flex; justify-content: center; gap: 0.55rem; margin: 0.2em 0; flex-wrap: wrap; }
  .sys-link { background: #fff; border: 1px dashed #64748b; border-radius: 999px; padding: 0.15em 0.55em; font-size: 0.88em; color: #475569; }
  .sys-link-ai { border-color: #d97706; color: #92400e; background: #fffbeb; }
  .sys-link-rt { border-color: #2563eb; color: #1e40af; background: #eff6ff; }
  .sys-link-auto { border-color: #7c3aed; color: #5b21b6; background: #f5f3ff; }
  .sys-row { display: flex; justify-content: center; align-items: stretch; gap: 0.55rem; margin: 0.15em auto; width: 98%; }
  .sys-side { display: flex; flex-direction: column; gap: 0.35rem; width: 22%; }
  .sys-arrow { text-align: center; color: #94a3b8; font-size: 1.1em; margin: 0.05em 0; }
  .sys-hline { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 0.15em; min-width: 52px; }
  .sys-hline span { font-size: 0.82em; color: #64748b; }
  .sys-stores { display: flex; gap: 0.4rem; width: 72%; margin: 0.15em auto 0; }
  .sys-legend { display: flex; gap: 1rem; justify-content: center; font-size: 0.85em; margin-top: 0.3em; color: #475569; flex-wrap: wrap; }
---

<!-- _class: lead -->
<!-- _paginate: false -->

# Community Bot

### AI-powered community platform for teams

Real-time chat · intelligent assistant · smart moderation · thread intelligence

*Capstone demo · ~20 minutes*

---

## Application features, AI capabilities & roles

<div class="cols">

<div class="col">

### Platform
- Orgs, workspaces & channels
- Real-time messaging & presence
- Threads, reactions & emoji picker
- File attachments (scanned uploads)
- OAuth + local demo accounts

</div>

<div class="col">

### AI features
- **Ask Bot** — RAG over FAQ/docs + channel search + memory
- **@bot** — quick in-channel FAQ replies
- **Moderation** — layered AI classification + mod queue
- **Thread summaries** — Otter-style digests (overview, decisions, actions)
- **Welcome messages** & **scheduled posts** (agent proposals need human confirm)
- **Weekly digest** via n8n workflow (optional)

</div>

<div class="col">

### Roles
| Role | Capabilities |
|------|----------------|
| **Member** | Chat, Ask Bot, upload docs |
| **Moderator** | + moderation queue, bans, moderation stats in Ask Bot |
| **Admin** | + workspace settings, AI usage, schedules, web search (when enabled) |

Agent routing adapts tools automatically by role.

</div>

</div>

<p class="note">Demo focus: Ask Bot Q&A → post flagged content → summarize a thread in Ask Bot or moderation dashboard.</p>

---

<!-- _class: arch -->

## System architecture

<div class="arch">

<div class="arch-layer arch-client">
  <div class="arch-title">React SPA (Vite + TypeScript)</div>
  <div class="arch-sub">Workspace · Ask Bot · Moderation · Settings</div>
</div>

<div class="arch-arrow">↓</div>

<div class="arch-layer arch-api">
  <div class="arch-title">Spring Boot API</div>
  <div class="arch-pills">
    <span class="arch-pill">REST + JWT</span>
    <span class="arch-pill">WebSocket (STOMP)</span>
    <span class="arch-pill">Ask Bot SSE</span>
  </div>
</div>

<div class="arch-arrow">↓</div>

<div class="arch-layer arch-ai">
  <div class="arch-title">AI layer — LangGraph4j + LangChain4j</div>
  <div class="arch-pills">
    <span class="arch-pill arch-pill-ai">Role routing (Member / Mod / Channel / Public)</span>
    <span class="arch-pill arch-pill-ai">→</span>
    <span class="arch-pill arch-pill-ai">Tool agent: FAQ · memory · search · summarize · mod</span>
  </div>
</div>

<div class="arch-arrow">↓</div>

<div class="arch-mid">
  <div class="arch-layer arch-data">
    <div class="arch-title">Data &amp; infrastructure</div>
    <div class="arch-pills">
      <span class="arch-pill">PostgreSQL + vectors</span>
      <span class="arch-pill">Redis pub/sub</span>
      <span class="arch-pill">Object store</span>
    </div>
  </div>
  <div class="arch-layer arch-ext">
    <div class="arch-title">OpenAI</div>
    <div class="arch-sub">chat · embeddings · moderation</div>
  </div>
</div>

<div class="arch-legend">
  <span><span class="arch-leg-box" style="background:#fef3c7;border:2px solid #d97706"></span>AI orchestration</span>
  <span><span class="arch-leg-box" style="background:#fce7f3;border:2px solid #db2777"></span>External models</span>
</div>

</div>

<p class="note">Flow: question → SSE → role routing → tool loop → citations in UI &nbsp;|&nbsp; Chat: WebSocket → Redis → all clients</p>

---

<!-- _class: sysdiag -->

## System component diagram

<div class="sys">

<div class="sys-node sys-browser">
  <div class="sys-title">Browser — React SPA</div>
  <div class="sys-sub">Chat · Ask Bot · Moderation · Settings · Agent timeline UI</div>
</div>

<div class="sys-links">
  <span class="sys-link sys-link-rt">REST / JWT — login, channels, uploads, moderation</span>
  <span class="sys-link sys-link-rt">WebSocket (STOMP) — live messages &amp; presence</span>
  <span class="sys-link sys-link-ai">SSE — Ask Bot token stream + tool events</span>
</div>

<div class="sys-arrow">↓ &nbsp;&nbsp;&nbsp; ↑</div>

<div class="sys-row">
  <div class="sys-node sys-backend">
    <div class="sys-title">Spring Boot backend</div>
    <div class="sys-sub">REST controllers · WebSocket broker · SSE agent endpoint · OAuth/JWT</div>
    <div class="sys-ai-inner">
      <div class="sys-title" style="font-size:0.95em">LangGraph4j + LangChain4j</div>
      <div class="sys-sub">Role routing · tool-calling agent · RAG · moderation · summaries</div>
    </div>
  </div>

  <div class="sys-hline"><span>→</span><span class="sys-link sys-link-ai">API calls</span></div>

  <div class="sys-side">
    <div class="sys-node sys-ext">
      <div class="sys-title">OpenAI</div>
      <div class="sys-sub">chat · embeddings · moderation</div>
    </div>
    <div class="sys-node sys-auto">
      <div class="sys-title">n8n (optional)</div>
      <div class="sys-sub">Friday cron → weekly digest webhook</div>
    </div>
  </div>
</div>

<div class="sys-arrow">↓</div>

<div class="sys-stores">
  <div class="sys-node sys-store">
    <div class="sys-title">PostgreSQL</div>
    <div class="sys-sub">users · messages · vectors (RAG)</div>
  </div>
  <div class="sys-node sys-store">
    <div class="sys-title">Redis</div>
    <div class="sys-sub">chat memory · pub/sub fan-out</div>
  </div>
  <div class="sys-node sys-store">
    <div class="sys-title">Object store</div>
    <div class="sys-sub">file attachments</div>
  </div>
</div>

<div class="sys-links" style="margin-top:0.25em">
  <span class="sys-link sys-link-auto">n8n → POST /api/internal/n8n/weekly-digest</span>
  <span class="sys-link">ClamAV (optional) — attachment scan</span>
</div>

<div class="sys-legend">
  <span><span class="arch-leg-box" style="background:#eff6ff;border:2px solid #2563eb"></span>Client</span>
  <span><span class="arch-leg-box" style="background:#fef3c7;border:2px solid #d97706"></span>AI frameworks</span>
  <span><span class="arch-leg-box" style="background:#fce7f3;border:2px solid #db2777"></span>External AI</span>
  <span><span class="arch-leg-box" style="background:#ede9fe;border:2px solid #7c3aed"></span>Automation</span>
</div>

</div>

<p class="note">Three transport paths from browser: REST for CRUD · WebSocket for real-time chat · SSE for streaming Ask Bot responses</p>

---

## Technology stack — why these choices?

| Layer | Choice | Why |
|-------|--------|-----|
| **Frontend** | React + TypeScript + Vite | Component model fits rich chat UI; type safety; fast dev proxy to API |
| **Real-time** | STOMP over WebSocket + Redis pub/sub | Instant message fan-out; Redis lets us scale to multiple API instances |
| **Backend** | Java 25 + Spring Boot 3 | Mature security (OAuth2/JWT), REST, scheduling, enterprise patterns |
| **Database** | PostgreSQL + pgvector | One store for relational data **and** semantic search (RAG, memory, message search) |
| **Object storage** | S3-compatible (MinIO locally) | Cheap attachment storage; swap to AWS S3 in production |
| **AI orchestration** | LangChain4j + LangGraph4j | Tool-calling ReAct loop + role-based subgraph routing in one codebase |
| **Models** | OpenAI (gpt-4o-mini, embeddings, moderation API) | Strong cost/quality balance; moderation API reduces custom classifier work |
| **Automation** | n8n (optional) | Non-devs can own digest workflows without redeploying the app |

**Not React+Node:** we wanted typed enterprise backend, built-in security, and JVM ecosystem for long-running services — React handles UX; Spring handles domain logic and AI integration.

---

## Moderation — balancing false positives

**Design goal:** catch harmful content early **without** silencing legitimate discussion.

| Layer | What it does | False-positive control |
|-------|----------------|------------------------|
| **OpenAI Moderation API** | Fast baseline for toxic / hate / harassment / spam | High-precision categories; flags go to queue, not instant permanent removal |
| **Guidelines LLM judge** | Scores against workspace-specific rules | Configurable **confidence threshold** (default 0.7) — only high-confidence flags auto-hide |
| **Human review queue** | Moderators approve or remove | **Approve** = false positive recovered; message stays visible |
| **Fail-open on API errors** | If classifier unavailable, message still delivers | Avoids blocking all chat when AI is down |

**Challenges addressed**
- **Over-moderation** → threshold + human-in-the-loop before lasting impact
- **Edge cases / sarcasm** → explanation stored on flag; mod sees context
- **Context across threads** → classifier sees message body; mods review full thread in dashboard

> Messages are flagged and hidden pending review — moderators are the final authority.

---

## Extensibility, scale & commercial roadmap

<div class="cols">

<div class="col">

### Scale today
- Stateless Spring Boot behind load balancer
- Redis pub/sub for multi-instance WebSocket fan-out
- Postgres connection pooling; vector index for RAG at workspace scope
- Rate limits on AI usage per workspace

</div>

<div class="col">

### Extensibility
- **Tool-based agent** — add capabilities without new UI (new `@Tool` methods)
- **Role-based flows** — Member / Moderator / Channel / Public agents
- **Document ingest** — PDF, DOCX, FAQ upload → automatic chunking & embedding
- **n8n hooks** — external automations (digests, alerts)

</div>

<div class="col">

### Commercial path
- Multi-tenant **org → workspace** model already in place
- Swap MinIO → cloud object storage; managed Postgres/Redis
- SSO / billing / usage metering via existing `LlmUsageService`
- Roadmap: mobile apps, away/vacation mode, specialist sub-agents, enterprise audit logs

</div>

</div>

---

<!-- _class: lead -->
<!-- _paginate: false -->

# Questions?

**Try it:** http://localhost:3000

| Demo account | Password |
|--------------|----------|
| `user@communitybot.local` | `User123!` |
| `mod@communitybot.local` | `Mod123!` |

Thank you

---

<!--
PRESENTER GUIDE (~20 min) — not exported to slides

0:00–2:00  Title + features slide — problem/solution verbally
2:00–6:00  Architecture slides — layered stack + component diagram (REST vs WebSocket vs SSE)
5:00–8:00  Moderation slide — false positives, threshold, human queue
8:00–17:00 LIVE DEMO (3 beats):
  1. Ask Bot: "How does moderation work?" or workspace FAQ question
  2. Post borderline message as member → show flag in mod dashboard → approve as false positive
  3. Ask Bot: "Summarize thread …" on an active thread OR show existing thread summary
17:00–19:00 Extensibility / roadmap — scale story, commercial deployment
19:00–20:00 Q&A

Talking points for objectives not on slides:
- Summarization: Otter-style structured sections; transcript-only, no invented facts; on-demand via Ask Bot tool
- Context: Redis short-term memory + vector long-term Ask Bot memory + channel/thread search tools
- Edge cases: FAQ fallback when OpenAI unavailable; sanitizer hides infra from user-facing bot answers

Export: npx @marp-team/marp-cli --no-stdin docs/presentation-community-bot.md -o docs/presentation-community-bot.pptx
-->
