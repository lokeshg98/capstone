# Community Bot — FAQ & Community Rules

Answers to common questions about the platform and our community guidelines.

---

## Community Rules

**Q: What are the community rules?**
Be respectful, stay on topic, no spam, no harassment, no NSFW content, and follow moderator instructions. Use common sense — if you wouldn't say it in a professional meeting, don't say it here.

**Q: What is the rule about being respectful?**
Treat others with kindness and professionalism. No personal attacks, insults, name-calling, trolling, or aggressive behavior. Disagreements are fine, but keep them constructive and focused on ideas, not people.

**Q: Is harassment allowed?**
No. Harassment of any kind is strictly prohibited. This includes but is not limited to bullying, intimidation, stalking, repeated unwanted contact, sexual harassment, and making threats. Violations result in immediate moderation action.

**Q: What is the rule about spam and self-promotion?**
No spamming or excessive self-promotion. Do not post the same message across multiple channels, send unsolicited DMs, or promote your products, services, or social media without admin approval. Occasional sharing of relevant resources in #resources is fine.

**Q: What content is not allowed?**
NSFW (not safe for work) content is strictly prohibited. This includes pornography, gore, hate speech, violent extremism, and any content that violates the platform's terms of service or applicable laws. Keep all content appropriate for a professional environment.

**Q: What is the rule about illegal content?**
Any content that violates laws or regulations is forbidden. This includes sharing copyrighted material without permission, doxxing, sharing personal information without consent, discussing illegal activities, or distributing malware. Violations result in permanent bans.

**Q: What is doxxing and is it allowed?**
Doxxing is sharing someone's private personal information (real name, address, phone number, workplace, etc.) without their consent. Doxxing is strictly prohibited and results in an immediate permanent ban.

**Q: Should I use the right channels for topics?**
Yes. Post in the most relevant channel for your topic. If you're unsure which channel to use, ask in #general. If a conversation drifts off-topic, move it to a thread or a more appropriate channel. Moderators may move or remove off-topic messages.

**Q: When should I use threads?**
Use threads for extended replies, off-topic tangents, or detailed discussions that would clutter the main channel. Click "Reply in thread" on a message to start one. Threads keep the main channel clean while letting conversations go deep.

**Q: What is the rule about spoilers and sensitive content?**
Use spoiler warnings for content that might spoil movies, games, books, or TV shows. If sharing potentially sensitive or triggering content, give a content warning first and put the content behind a thread or spoiler tag.

**Q: What language should I use?**
The primary language of this server is English. While occasional phrases in other languages are fine, please use English for main discussions so that moderators and most members can understand and participate.

**Q: What happens if I break a rule?**
Moderators may issue a warning, delete the offending message, temporarily mute you, or ban you from the server depending on the severity and frequency of the violation. First-time minor violations typically get a warning. Repeated or severe violations lead to bans.

**Q: How do I report a rule violation?**
If you see someone breaking the rules, you can report it to a moderator or admin via direct message. Alternatively, use the "Report" button (flag icon) if your workspace has moderation features enabled. Do not engage or escalate the situation yourself.

**Q: Do moderators have final say?**
Yes. Moderators and admins have the final say on rule enforcement and content decisions. If you disagree with a moderation action, you can appeal it privately with an admin. Public arguments about moderation are not allowed.

**Q: What is the rule about privacy?**
Respect other members' privacy. Do not share screenshots of private conversations without consent. Do not collect or store other members' personal information. What happens in private channels stays in private channels.

**Q: Can I DM other members without permission?**
Avoid sending unsolicited direct messages. If you need to reach someone privately, ask in a public channel first if it's okay to DM them. Repeated unwanted DMs may be considered harassment.

**Q: What about bots and automation?**
Do not run unauthorized bots, scripts, or automation tools that post messages, scrape content, or interact with the platform in ways that disrupt normal use. Ask an admin before connecting any third-party bot or integration.

**Q: Can I share links and external content?**
Yes, but ensure the content is safe and relevant. Do not share phishing links, malware, pirated content, or referral/affiliate links. If a link requires a content warning (e.g., flashing lights, loud audio), add a note before the link.

**Q: Is there a minimum age requirement?**
You must be at least 13 years old to use this platform, in compliance with the Children's Online Privacy Protection Act (COPPA) and the platform's terms of service.

**Q: Can I have multiple accounts?**
Using multiple accounts to evade bans, impersonate others, or manipulate discussions is not allowed. One person should use one account. If you need a second account for a legitimate reason, ask an admin first.

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
In the **Ask Bot** panel, scroll to the **FAQ Documents** section at the bottom (click to expand). Click **Upload FAQ document** and select a PDF, DOCX, TXT, or MD file. The platform uploads the file, chunks it, and embeds it automatically. Once ingested, the bot can answer questions from it immediately.

**Q: The bot offered to schedule a post. What should I do?**
If the assistant proposes a **scheduled post**, a yellow **Confirm / Decline** card appears under the reply. Nothing is written to the schedule until you click **Confirm**. **Decline** discards the proposal.

**Q: How do I enable web search in Ask Bot?**
Web search is available to **workspace admins** when your organisation has enabled it. If you need live web results in Ask Bot, ask a workspace admin.

**Q: What file types does the bot accept for its knowledge base?**
PDF (`.pdf`), Word documents (`.docx`), plain text (`.txt`), and Markdown (`.md`).

**Q: How do I trigger the bot from a channel?**
Type `@bot` or `@Community Bot` anywhere in your message, for example:

```
@bot what is the rule about spam?
```

The bot reads the question, searches the FAQ knowledge base (and limited tools in quick-reply mode), and posts its answer directly in the channel as a thread reply.

**Q: The bot answered "I don't know" or gave a vague response. Why?**
FAQ answers depend on **uploaded documents**. In the full **Ask Bot** panel the assistant can also search **channels** and other sources when it chooses the right tools; if nothing matches, it may still say it does not know. Upload the relevant PDF, DOCX, TXT, or MD file to improve document coverage.

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
