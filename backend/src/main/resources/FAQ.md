# Community Bot — FAQ

Answers to the most common questions about using the platform.

---

## Getting Started

**Q: How do I sign in?**
Go to `http://localhost:3000`. You will be redirected to the login page. Click **Sign in with Google** or **Sign in with GitHub** and complete the OAuth flow. You are returned to the dashboard automatically.

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
Yes. Click the attachment icon in the message input bar to upload a file. Supported formats include images, PDFs, Word documents, plain text, and Markdown. Files are scanned and stored securely.

**Q: Are messages delivered in real time?**
Yes. Messages appear instantly in the channel for all connected members — no page refresh needed.

**Q: Can I react to messages?**
Yes. Hover over a message and click the smile icon to open the emoji picker. Search by keyword (e.g. "happy") and add a reaction. Reactions are counted and shown below the message.

**Q: Does the app support Giphy or animated GIF stickers?**
No. Giphy and third-party GIF libraries are not integrated. Use the built-in emoji picker for Unicode emoji and searchable emoji images when composing messages or adding reactions.

**Q: Does the app support vacation mode or away status?**
No. Vacation mode, out-of-office messages, and custom away status are not available. You can see who is currently online in the member list, but there is no setting to pause notifications or mark yourself away while on leave.

**Q: I see "This message was deleted." — what happened?**
The message was removed by a moderator or admin due to a content policy violation. The placeholder is intentional so that conversation threads remain readable.

---

## Community Bot (AI Assistant)

**Q: What can the bot do?**
The bot supports a **multi-agent** assistant in **Ask Bot** (sidebar): it can search **FAQ documents**, recall **past conversations** from earlier Ask Bot chats in this workspace, search **channel messages** (keyword and semantic), **summarize threads**, run **web search** when enabled (typically **admins only**), inspect **moderation** stats and queues (moderators), and **propose scheduled posts** that only apply after you **confirm** them in the UI. **In-channel** replies via `@bot` are intentionally shorter and only use FAQ-style tools.

**Q: How do I ask the bot a question?**
Click **Ask Bot** at the bottom of the left sidebar. A full-page chat opens. Type your question and press `Enter` (or click send). Replies **stream** in real time; you will see **tool steps** (collapsed by default), **source excerpts** from documents when RAG is used, and a **confirmation card** if the bot proposes scheduling a post.

**Q: How do I teach the bot new information?**
In the **Ask Bot** panel, scroll to the **FAQ Documents** section at the bottom (click to expand). Click **Upload FAQ document** and select a PDF or DOCX file. The platform uploads the file, chunks it, and embeds it automatically. Once ingested, the bot can answer questions from it immediately.

**Q: The bot offered to schedule a post. What should I do?**
If the assistant proposes a **scheduled post**, a yellow **Confirm / Decline** card appears under the reply. Nothing is written to the schedule until you click **Confirm**. **Decline** discards the proposal.

**Q: How do I enable web search in Ask Bot?**
Web search is available to **workspace admins** when your organisation has enabled it. If you need live web results in Ask Bot, ask a workspace admin.

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
Try again in a moment. If the problem continues, contact your workspace admin — the assistant may be temporarily unavailable.

---

## Moderation

**Q: How does automatic moderation work?**
When moderation is enabled, every message is analysed by the AI moderation pipeline before delivery. Messages classified as inappropriate (profanity, spam, harassment) are automatically flagged and hidden from the channel. Moderators and admins review the queue and decide to approve or permanently remove flagged messages.

**Q: I don't see any flagged messages. Is moderation on?**
Moderation is controlled by your workspace administrators. If you expect automatic flagging but don't see it, ask an admin to confirm moderation is enabled for your workspace.

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
The app auto-creates a **General** workspace on first click. If nothing happens, the service may be temporarily unavailable. Check your browser console (F12 → Network) for errors.

**Q: I get redirected to `/login` every time.**
Your session has expired or sign-in did not complete. Sign in again from the home page.

**Q: The frontend shows a blank white screen.**
Make sure you are opening the app at `http://localhost:3000` and that the server is running. Check your browser console (F12 → Network) for failed requests.

**Q: Messages I send don't appear for other users without a refresh.**
Real-time delivery may be interrupted. Refresh the page and try again. If the problem persists, contact your workspace admin.

**Q: File uploads fail.**
Check that the file is under the size limit and is a supported type (image, PDF, Word, text, or Markdown). If it still fails, contact your workspace admin.

---

*For infrastructure setup, port map, and step-by-step startup instructions see [RUNBOOK.md](./RUNBOOK.md).*
