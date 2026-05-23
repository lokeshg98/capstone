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

1. Set the same secret on backend and n8n (in `infra/.env`):
   ```bash
   N8N_API_KEY=your-secret-key
   ```

2. Start n8n (Docker). The compose file sets `N8N_BLOCK_ENV_ACCESS_IN_NODE=false` so the workflow can use `{{$env.N8N_API_KEY}}`:
   ```bash
   cd infra && docker compose up -d n8n
   ```
   Or from repo root: `./dev.sh --with-n8n`

   **If you changed docker-compose, recreate the container** (env vars are read at start):
   ```bash
   docker compose -f infra/docker-compose.yml up -d --force-recreate n8n
   ```

3. In n8n: **Workflows → Import from File** → select `weekly-digest-workflow.json`.

4. Ensure `N8N_API_KEY` in `infra/.env` matches the backend. The workflow reads it via `$env.N8N_API_KEY` (no separate n8n Variables step needed when using Docker).

5. Activate the workflow.

## Troubleshooting

### `access to env vars denied` on “Trigger Weekly Digest”

n8n **2.x** blocks `$env` in node expressions by default. Fix:

1. Confirm `infra/docker-compose.yml` includes:
   ```yaml
   N8N_BLOCK_ENV_ACCESS_IN_NODE: "false"
   ```
2. Recreate the n8n container:
   ```bash
   set -a && source infra/.env && set +a
   docker compose -f infra/docker-compose.yml up -d --force-recreate n8n
   ```
3. Re-run the workflow.

**Alternative (no `$env`):** In the HTTP Request node, replace the header value with your literal key (same as `N8N_API_KEY` in `infra/.env`) instead of `={{$env.N8N_API_KEY}}`.

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
