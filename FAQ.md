# Community Bot — FAQ

Answers to the most common questions about using the platform.

---

## Getting Started

**Q: How do I sign in?**
Go to `http://localhost:3000`. You will be redirected to the login page. You can sign in with email and password, or click **Sign in with Google** or **Sign in with GitHub**. You are returned to the dashboard automatically.

For local development, these demo accounts are available:

- `admin@communitybot.local` / `Admin123!`
- `mod@communitybot.local` / `Mod123!`
- `user@communitybot.local` / `User123!`

**Q: I just signed in and the dashboard is empty. What do I do?**
Click **+ New Organisation** in the top-right corner and give your organisation a name (at least 2 characters). Once created, click on it — the app will automatically provision a default **General** workspace and take you straight to the chat.

**Q: What is the difference between an Organisation and a Workspace?**
An **Organisation** is the top-level container (e.g. your company or community). A **Workspace** lives inside an organisation and holds channels, members, and settings. You can have multiple workspaces per organisation for different teams or topics.

---

## Channels & Messaging

**Q: How do I create a channel?**
Inside a workspace, look at the left sidebar under **Channels**. Click the **+** icon next to the heading, type a channel name, and press `Enter` to create it. The channel appears in the list immediately.

**Q: How do I send a message?**
Click a channel in the sidebar, type in the message box at the bottom, and press `Enter` (or click the send button). Hold `Shift + Enter` to add a line break without sending.

**Q: Can I attach files to messages?**
Yes. Click the paperclip / attachment icon in the message input bar to upload a file. Supported formats include images, PDFs, and DOCX files. Files are stored securely in the backend object store (MinIO).

**Q: Are messages delivered in real time?**
Yes. The platform uses WebSockets. Any message sent by one member appears instantly in the channel for all connected members — no page refresh needed.

**Q: Can I react to messages?**
Yes. Hover over a message to reveal the emoji reaction option. Reactions are counted and displayed inline below the message. Your own reactions are highlighted.

**Q: I see "This message was deleted." — what happened?**
The message was removed by a moderator or admin due to a content policy violation. The placeholder is intentional so that conversation threads remain readable.

---

## Community Bot (AI Assistant)

**Q: What can the bot do?**
The bot supports a **multi-agent** assistant in **Ask Bot** (sidebar): it can search **FAQ documents**, recall **long-term memories** from your past Ask Bot Q&A in this workspace, search **channel messages** (keyword and semantic), **summarize threads**, run **web search** when enabled (Tavily; typically **admins only**), inspect **moderation** stats and queues (moderators), and **propose scheduled posts** that only apply after you **confirm** them in the UI. **In-channel** replies via `@bot` are intentionally shorter and only use FAQ-style tools.

**Q: How do I ask the bot a question?**
Click **Ask Bot** at the bottom of the left sidebar. A full-page chat opens. Type your question and press `Enter` (or click send). Replies **stream** in real time; you will see **tool steps** (collapsed by default), **source excerpts** from documents when RAG is used, and a **confirmation card** if the bot proposes scheduling a post.

**Q: How do I teach the bot new information?**
In the **Ask Bot** panel, scroll to the **FAQ Documents** section at the bottom (click to expand). Click **Upload FAQ document** and select a PDF or DOCX file. The platform uploads the file, chunks it, and embeds it automatically. Once ingested, the bot can answer questions from it immediately.

**Q: The bot offered to schedule a post. What should I do?**
If the assistant proposes a **scheduled post**, a yellow **Confirm / Decline** card appears under the reply. Nothing is written to the schedule until you click **Confirm**. **Decline** discards the proposal.

**Q: How do I enable web search in Ask Bot?**
Set `TAVILY_API_KEY` in `infra/.env` (see [RUNBOOK.md](./RUNBOOK.md)). By default, only **workspace admins** can use web search tools. Without the key, web search is disabled and the bot explains that when relevant.

**Q: What file types does the bot accept for its knowledge base?**
PDF (`.pdf`) and Word documents (`.docx`) only.

**Q: How do I trigger the bot from a channel?**
Type `@bot` anywhere in your message, e.g.:

```
@bot what is our refund policy?
```

The bot reads the question, searches the FAQ knowledge base (and limited tools in quick-reply mode), and posts its answer directly in the channel.

**Q: The bot answered "I don't know" or gave a vague response. Why?**
FAQ answers depend on **uploaded documents**. In the full **Ask Bot** panel the assistant can also search **channels** and other sources when it chooses the right tools; if nothing matches, it may still say it does not know. Upload the relevant PDF or DOCX to improve document coverage.

**Q: The bot returned an error. What should I do?**
Ensure the backend is running and that `OPENAI_API_KEY` is set in `infra/.env` and exported before starting the backend. Without a valid OpenAI key, RAG queries will fail with a 500 error.

---

## Moderation

**Q: How does automatic moderation work?**
When `MODERATION_ENABLED=true`, every message is analysed by the AI moderation pipeline before delivery. Messages classified as inappropriate (profanity, spam, harassment) are automatically flagged and hidden from the channel. Moderators and admins review the queue and decide to approve or permanently remove flagged messages.

**Q: Moderation is disabled in my environment. Is that normal?**
Yes. The dev profile disables AI moderation by default to avoid unnecessary OpenAI API calls during local development. To enable it, set `MODERATION_ENABLED=true` before starting the backend:

```bash
export MODERATION_ENABLED=true
./gradlew :backend:bootRun --args='--spring.profiles.active=dev'
```

**Q: How do I review flagged messages?**
Click **Moderation** at the bottom of the left sidebar (requires Moderator or Admin role). You will see a queue of flagged messages. For each one:
- **Approve** — restores the message (marks it as a false positive).
- **Remove** — hides the message permanently (confirms the violation).

**Q: How do I ban a member?**
In the Moderation panel, navigate to the **Bans** section. Enter the user ID, a reason, and optionally an expiry date/time. Leave the expiry blank for a permanent ban. Admins can also lift bans from the same screen.

**Q: What member roles exist?**
| Role | Permissions |
|------|-------------|
| **Member** | Read and send messages in channels they belong to |
| **Moderator** | All Member permissions + review flags, ban/unban members |
| **Admin** | All Moderator permissions + manage workspace settings, channels, and scheduled posts |

---

## Scheduled Posts

**Q: Can the bot post messages automatically on a schedule?**
Yes. Go to **Settings** in the sidebar, then select the **Scheduled Posts** tab.

**Q: What schedule options are available?**
Two modes:
- **One-shot** — posts once at a specific date and time you choose.
- **Recurring (cron)** — posts on a repeating schedule defined by a Spring 6-field cron expression (seconds, minutes, hours, day-of-month, month, day-of-week). Example for every weekday at 9 am: `0 0 9 * * MON-FRI`.

**Q: How do I create a scheduled post?**
1. Click **Settings** in the sidebar.
2. Click **New Scheduled Post**.
3. Select the target channel, write your message, choose the schedule type, and set the time or cron expression.
4. Click **Schedule Post**. The post appears in the list with status `PENDING`.

**Q: How do I cancel a scheduled post?**
In the **Scheduled Posts** list, click the trash icon on any `PENDING` post to cancel it.

---

## Welcome Messages

**Q: Can the bot welcome new members automatically?**
Yes. Go to **Settings → Welcome Message**. Write a template message and use `{name}` as a placeholder for the new member's display name. The bot posts this message to `#general` whenever someone new joins the workspace.

**Q: Example welcome message?**
```
👋 Welcome to the workspace, {name}! We're glad to have you here.
Feel free to introduce yourself and ask any questions in this channel.
```

---

## Troubleshooting

**Q: I click an organisation but nothing happens.**
The app auto-creates a **General** workspace on first click. If nothing happens, the backend may be down or returning an error. Check that:
- Docker services are running: `docker compose ps` (all should show `healthy`)
- The Spring Boot backend is running on port 8080
- Your browser console shows any network errors (F12 → Network tab)

**Q: I get redirected to `/login` every time.**
Your session has expired or you were not fully authenticated. Sign in again. Ensure the backend is running — if it is down, the token refresh call will fail and the frontend will clear your session.

**Q: The frontend shows a blank white screen.**
A CORS mismatch is the most common cause. Confirm `FRONTEND_URL=http://localhost:3000` is in `infra/.env` and was exported before starting the backend.

**Q: Messages I send don't appear for other users without a refresh.**
The WebSocket connection to `ws://localhost:8080/ws` may have failed. Check the browser console for WebSocket errors. Ensure the backend is running and that no firewall or proxy is blocking the WebSocket upgrade.

**Q: File uploads fail.**
Ensure MinIO is running and the `community-bot` bucket exists. See Step 3 of the RUNBOOK for bucket creation instructions.

---

*For infrastructure setup, port map, and step-by-step startup instructions see [RUNBOOK.md](./RUNBOOK.md).*
