# Community Bot — Multi-Tool Coordination & Intelligent Routing

Use this document as a **NotebookLM source**. In Studio, choose **Infographic**, **Slide Deck**, or **Briefing Doc** and prompt:

> Create a clear architecture diagram showing how Community Bot routes requests to the correct agent flow and coordinates multiple tools. Include the four entry points, four agent flows, tool library, and shared state.

---

## System purpose

Community Bot is a Slack-like community platform with an AI assistant. When a user asks a question, the system:

1. **Routes** the request to the correct agent persona (public visitor, channel @bot, member, or moderator).
2. **Coordinates** multiple tools in sequence (FAQ search, memory, message search, moderation, scheduling, etc.).
3. **Streams** tool progress and citations back to the UI.

---

## Layer 1 — Entry points (where requests start)

| Entry point | API / trigger | Context mode |
|-------------|---------------|--------------|
| Ask Bot panel | `POST /api/workspaces/{id}/agent/ask/stream` (SSE) | Streaming workspace context |
| @bot in channel | `MessageSentEvent` → `BotMentionListener` | Constrained channel mode |
| Public marketing FAQ | Public site controller | Public mode (no login) |
| Sync answer (internal) | `MultiAgentService.answerSync()` | Sync workspace or channel |

---

## Layer 2 — Intelligent routing (which agent runs?)

**Decision engine:** `AgentFlowType.fromContext(AgentContext, WorkspaceMemberRepository)`

**Routing rules (in order):**

1. No workspace / public → **PUBLIC** flow
2. `constrainedChannelMode == true` (@bot in channel) → **CHANNEL** flow
3. User has Admin or Moderator role → **MODERATOR** flow
4. Otherwise → **MEMBER** flow

**LangGraph4j orchestrator:** `CommunityAgentGraphService`

- Compiles four subgraphs: `public_agent`, `channel_agent`, `member_agent`, `moderator_agent`
- Conditional edge from START picks one subgraph based on context
- Each subgraph is a LangChain4j `AgentExecutor` with its own system prompt and tools

**Key Java classes:**

- `AgentContext` — per-request thread-local context (workspace, user, channel mode, SSE sink)
- `AgentContextHolder` — holds context during tool execution
- `AgentFlowType` — enum + routing logic
- `CommunityAgentGraphService` — LangGraph4j graph compile + `runSync()`

---

## Layer 3 — Multi-tool coordination (what runs inside the agent?)

**Tool registry:**

- **Public:** `PublicAgentTools.searchPublicFaq`
- **Workspace:** `CommunityAgentTools` (~12 tools)

**Workspace tools:**

| Tool | Function |
|------|----------|
| searchFaqDocuments | pgvector RAG over uploaded FAQ/PDF/DOCX |
| recallPastConversations | Long-term Ask Bot memory (pgvector) |
| searchChannelMessagesByKeyword | Keyword search in one channel |
| searchWorkspaceMessagesSemantic | Semantic search over message embeddings |
| fetchThreadTranscript | Load thread messages |
| summarizeThread | Generate thread digest |
| searchWeb | Tavily web search (optional, admin-gated) |
| listModerationFlags | List flags for moderators |
| listWorkspaceBans | List active bans |
| moderationStats | Flag counts over N days |
| proposeScheduledPost | Create human-confirmed schedule proposal |

**How tools are coordinated:**

1. LangChain4j `AiServices` builds an assistant with `.tools(tools)`.
2. The LLM chooses which tools to call and in what order (ReAct loop).
3. `maxSequentialToolsInvocations`: **8** for Ask Bot, **4** for @bot channel mode.
4. `beforeToolExecution` / `afterToolExecution` emit SSE events (`tool_start`, `tool_result`).
5. Before the loop, long-term memory may be **pre-injected** into the prompt from pgvector.

**Second-layer gating:** Individual `@Tool` methods check `AgentContext.constrainedChannelMode()` and refuse advanced tools in quick @bot replies.

---

## Layer 4 — Shared run state (merge results from all tools)

**`AgentRunStateHolder` / `AgentRunState` collects:**

- Citations (FAQ chunks for UI “Sources” panel)
- Steps (audit log: faq_search, mod_flags, etc.)
- Proposals (pending scheduled post for Confirm/Decline in UI)

Final SSE event `type: final` bundles answer + citations + steps + proposedAction.

---

## Layer 5 — User experience (frontend)

**Ask Bot:** `AskBotPanel.tsx` + `AgentTimeline.tsx`

- Shows thinking → tool calls (expandable args/results) → streaming tokens → citations → schedule confirm card

**@bot:** Reply posted in thread under the mention; no full timeline UI.

---

## Architecture diagram (text for NotebookLM to visualize)

```
                    ┌─────────────────────────────────────────┐
                    │           ENTRY POINTS                   │
                    ├─────────────┬─────────────┬─────────────┤
                    │  Ask Bot    │  @bot in    │  Public FAQ │
                    │  (SSE)      │  channel    │  (no login) │
                    └──────┬──────┴──────┬──────┴──────┬──────┘
                           │             │             │
                           ▼             ▼             ▼
                    ┌─────────────────────────────────────────┐
                    │     AgentContext (thread-local)          │
                    │  workspace · user · channelMode · sink   │
                    └────────────────────┬────────────────────┘
                                         │
                                         ▼
                    ┌─────────────────────────────────────────┐
                    │   AgentFlowType.fromContext() ROUTER     │
                    └────────────────────┬────────────────────┘
                                         │
           ┌──────────────┬──────────────┼──────────────┬──────────────┐
           ▼              ▼              ▼              ▼              │
    ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌────────────┐       │
    │  PUBLIC    │ │  CHANNEL   │ │  MEMBER    │ │ MODERATOR  │       │
    │  agent     │ │  agent     │ │  agent     │ │  agent     │       │
    │ (FAQ only) │ │ (limited)  │ │ (standard) │ │ (full)     │       │
    └─────┬──────┘ └─────┬──────┘ └─────┬──────┘ └─────┬──────┘       │
          │              │              │              │              │
          └──────────────┴──────────────┴──────────────┘              │
                                         │                             │
                                         ▼                             │
                    ┌─────────────────────────────────────────┐       │
                    │   LangChain4j tool loop (ReAct)          │       │
                    │   LLM picks tools · max 4 or 8 steps     │       │
                    └────────────────────┬────────────────────┘       │
                                         │                             │
           ┌─────────────────────────────┼─────────────────────────────┘
           ▼                             ▼
    ┌──────────────┐              ┌──────────────┐
    │ FAQ / Memory │              │ Mod / Schedule│
    │ Search / Web │              │ Thread tools  │
    └──────┬───────┘              └──────┬───────┘
           │                             │
           └──────────────┬──────────────┘
                          ▼
                    ┌─────────────────────────────────────────┐
                    │        AgentRunState (shared)            │
                    │  citations · steps · proposals           │
                    └────────────────────┬────────────────────┘
                                         │
                                         ▼
                    ┌─────────────────────────────────────────┐
                    │   SSE stream → AskBotPanel / Timeline    │
                    └─────────────────────────────────────────┘
```

---

## Mermaid source (optional — paste into mermaid.live or NotebookLM if supported)

```mermaid
flowchart TB
  subgraph entry [Entry Points]
    A[Ask Bot SSE]
    B[@bot Channel Mention]
    C[Public FAQ]
  end

  subgraph routing [Intelligent Routing]
    D[AgentContext]
    E[AgentFlowType.fromContext]
    F{Select flow}
    G[PUBLIC - FAQ only]
    H[CHANNEL - limited tools]
    I[MEMBER - standard tools]
    J[MODERATOR - full tools]
  end

  subgraph tools [Multi-Tool Coordination]
    K[LangChain4j ReAct loop]
    L[CommunityAgentTools]
    M[AgentRunState]
  end

  subgraph ux [User Experience]
    N[SSE tool_start / tool_result / final]
    O[AgentTimeline UI]
  end

  A --> D
  B --> D
  C --> D
  D --> E --> F
  F --> G & H & I & J
  G & H & I & J --> K --> L --> M
  K --> N --> O
```

---

## Example user journey (for slide narration)

**Moderator in Ask Bot:** “Summarize onboarding discussions and propose a Friday reminder.”

1. Route → MODERATOR agent (role check)
2. Pre-inject pgvector memory into prompt
3. Tools: `recallPastConversations` → `searchFaqDocuments` → `proposeScheduledPost`
4. State: citations + pending proposal
5. UI: answer + sources + Confirm/Decline buttons

**Member types @bot in #general:** “What’s our spam policy?”

1. Route → CHANNEL agent (constrained mode)
2. Max 4 tool steps; advanced tools blocked at method level
3. Tool: `searchFaqDocuments` only
4. Reply posted as thread under mention

---

## Key backend files (for reference)

- `CommunityAgentGraphService.java` — LangGraph routing
- `AgentFlowType.java` — routing rules
- `AgentContext.java` — request context
- `MultiAgentService.java` — streaming/sync execution, tool hooks
- `CommunityAgentTools.java` — workspace tools
- `PublicAgentTools.java` — public FAQ tool
- `BotMentionListener.java` — @bot entry
- `AskBotPanel.tsx` / `AgentTimeline.tsx` — UI

---

## What to exclude from the diagram

- Content moderation pipeline (separate from agent graph; runs on `MessageSentEvent`)
- Redis chat memory and pgvector storage internals (optional secondary box)
