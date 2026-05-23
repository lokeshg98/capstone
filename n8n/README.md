# n8n — Weekly Digest automation

This folder contains an [n8n](https://n8n.io/) workflow that triggers **personalized weekly digests** every **Friday at 5pm**.

## What it does

1. n8n Cron fires at `0 17 * * 5` (5:00 PM every Friday).
2. HTTP POST to the Community Bot webhook:
   - `POST /api/internal/n8n/weekly-digest`
   - Header: `X-N8n-Api-Key: <N8N_API_KEY>`
3. Backend generates an LLM digest for each opted-in workspace member, summarizing activity across **all channels they are subscribed to** (`channel_members`).
4. Digests are posted to `#weekly-digest` (auto-created per workspace).

## Quick start

1. Set the same secret on backend and n8n:
   ```bash
   export N8N_API_KEY=your-secret-key
   ```

2. Start n8n (Docker):
   ```bash
   cd infra && docker compose up -d n8n
   ```
   Open http://localhost:5678 (default login `admin` / `admin`).

3. In n8n: **Workflows → Import from File** → select `weekly-digest-workflow.json`.

4. Set environment variable `N8N_API_KEY` in n8n (Settings → Variables) to match the backend.

5. Activate the workflow.

## Webhook body (optional)

```json
{
  "workspaceId": "uuid-to-limit-one-workspace",
  "periodDays": 7,
  "dryRun": false
}
```

## User preferences

Members can opt in/out under **Workspace Settings → Weekly Digest (n8n)** in the app.
