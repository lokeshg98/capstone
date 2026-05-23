import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Calendar, MessageSquarePlus, Trash2, Clock, RefreshCw, Shield, Plus, X, Sparkles } from 'lucide-react';
import { fetchChannels } from '@/features/channels/channelApi';
import {
  fetchScheduledPosts,
  createScheduledPost,
  cancelScheduledPost,
  type ScheduledPostResponse,
  type ScheduleType,
} from '@/features/scheduling/schedulingApi';
import {
  fetchDigestPreferences,
  updateDigestPreferences,
} from '@/features/scheduling/digestApi';
import { fetchWelcomeTemplate, updateWelcomeTemplate, fetchCommunityGuidelines, updateCommunityGuidelines } from './workspaceApi';
import {
  fetchRoles,
  createRole,
  deleteRole,
  fetchMembersWithRoles,
  assignRoleToMember,
  removeRoleFromMember,
  type WorkspaceRoleResponse,
  type MemberRolesResponse,
} from './roleApi';

type Tab = 'scheduled' | 'digest' | 'guidelines' | 'welcome' | 'roles';

const STATUS_BADGE: Record<string, string> = {
  PENDING:   'bg-yellow-100 text-yellow-700',
  SENT:      'bg-green-100 text-green-700',
  CANCELLED: 'bg-gray-100 text-gray-500',
  ERROR:     'bg-red-100 text-red-700',
};

export default function WorkspaceSettings({ workspaceId }: { workspaceId: string }) {
  const [tab, setTab] = useState<Tab>('scheduled');

  return (
    <div className="flex flex-col h-full bg-white">
      {/* Header */}
      <div className="h-14 border-b border-gray-200 px-6 flex items-center gap-4">
        <span className="font-semibold text-gray-800">Workspace Settings</span>
        <div className="flex gap-1 ml-auto">
          {(['scheduled', 'digest', 'guidelines', 'welcome', 'roles'] as Tab[]).map((t) => (
            <button
              key={t}
              onClick={() => setTab(t)}
              className={`px-3 py-1.5 rounded text-sm font-medium transition-colors ${
                tab === t
                  ? 'bg-brand-600 text-white'
                  : 'text-gray-600 hover:bg-gray-100'
              }`}
            >
              {t === 'scheduled' ? 'Scheduled Posts'
                : t === 'digest' ? 'Weekly Digest (n8n)'
                : t === 'guidelines' ? 'Moderation Guidelines'
                : t === 'welcome' ? 'Welcome Message'
                : 'Roles'}
            </button>
          ))}
        </div>
      </div>

      <div className="flex-1 overflow-y-auto p-6">
        {tab === 'scheduled' ? (
          <ScheduledPostsTab workspaceId={workspaceId} />
        ) : tab === 'digest' ? (
          <WeeklyDigestTab workspaceId={workspaceId} />
        ) : tab === 'guidelines' ? (
          <GuidelinesTab workspaceId={workspaceId} />
        ) : tab === 'welcome' ? (
          <WelcomeMessageTab workspaceId={workspaceId} />
        ) : (
          <RolesTab workspaceId={workspaceId} />
        )}
      </div>
    </div>
  );
}

// ─── Scheduled Posts ─────────────────────────────────────────────────────────

function ScheduledPostsTab({ workspaceId }: { workspaceId: string }) {
  const qc = useQueryClient();

  const { data: posts = [], isLoading } = useQuery({
    queryKey: ['scheduled-posts', workspaceId],
    queryFn:  () => fetchScheduledPosts(workspaceId),
  });

  const { data: channels = [] } = useQuery({
    queryKey: ['channels', workspaceId],
    queryFn:  () => fetchChannels(workspaceId),
  });

  const cancelMut = useMutation({
    mutationFn: (postId: string) => cancelScheduledPost(workspaceId, postId),
    onSuccess:  () => qc.invalidateQueries({ queryKey: ['scheduled-posts', workspaceId] }),
  });

  const createMut = useMutation({
    mutationFn: (data: Parameters<typeof createScheduledPost>[1]) =>
      createScheduledPost(workspaceId, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['scheduled-posts', workspaceId] });
      setForm(defaultForm);
    },
  });

  const defaultForm = {
    channelId:      '',
    body:           '',
    scheduleType:   'ONE_SHOT' as ScheduleType,
    fireAt:         '',
    cronExpression: '',
  };
  const [form, setForm] = useState(defaultForm);
  const [showForm, setShowForm] = useState(false);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.channelId || !form.body) return;
    createMut.mutate({
      channelId:      form.channelId,
      body:           form.body,
      scheduleType:   form.scheduleType,
      fireAt:         form.scheduleType === 'ONE_SHOT' ? new Date(form.fireAt).toISOString() : undefined,
      cronExpression: form.scheduleType === 'CRON'     ? form.cronExpression                 : undefined,
    });
  };

  return (
    <div className="space-y-6">
      {/* New post form */}
      <div className="border border-gray-200 rounded-lg p-4">
        <button
          onClick={() => setShowForm((v) => !v)}
          className="flex items-center gap-2 text-sm font-medium text-brand-600 hover:text-brand-700"
        >
          <MessageSquarePlus className="h-4 w-4" />
          {showForm ? 'Collapse' : 'New Scheduled Post'}
        </button>

        {showForm && (
          <form onSubmit={handleSubmit} className="mt-4 space-y-3">
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block text-xs font-medium text-gray-600 mb-1">Channel</label>
                <select
                  required
                  value={form.channelId}
                  onChange={(e) => setForm({ ...form, channelId: e.target.value })}
                  className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
                >
                  <option value="">Select channel…</option>
                  {channels.map((c) => (
                    <option key={c.id} value={c.id}>#{c.name}</option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-medium text-gray-600 mb-1">Schedule Type</label>
                <select
                  value={form.scheduleType}
                  onChange={(e) => setForm({ ...form, scheduleType: e.target.value as ScheduleType })}
                  className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
                >
                  <option value="ONE_SHOT">One-shot (specific date/time)</option>
                  <option value="CRON">Recurring (cron)</option>
                </select>
              </div>
            </div>

            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">Message</label>
              <textarea
                required
                rows={3}
                value={form.body}
                onChange={(e) => setForm({ ...form, body: e.target.value })}
                placeholder="Type your scheduled message…"
                className="w-full border border-gray-300 rounded px-3 py-2 text-sm resize-none focus:outline-none focus:ring-2 focus:ring-brand-500"
              />
            </div>

            {form.scheduleType === 'ONE_SHOT' ? (
              <div>
                <label className="block text-xs font-medium text-gray-600 mb-1">Send at</label>
                <input
                  type="datetime-local"
                  required
                  value={form.fireAt}
                  onChange={(e) => setForm({ ...form, fireAt: e.target.value })}
                  className="border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
                />
              </div>
            ) : (
              <div>
                <label className="block text-xs font-medium text-gray-600 mb-1">
                  Cron expression{' '}
                  <span className="text-gray-400 font-normal">
                    (Spring 6-field: sec min hr dom month dow — e.g. <code>0 9 * * * MON-FRI</code>)
                  </span>
                </label>
                <input
                  required
                  value={form.cronExpression}
                  onChange={(e) => setForm({ ...form, cronExpression: e.target.value })}
                  placeholder="0 9 * * * MON-FRI"
                  className="w-full border border-gray-300 rounded px-3 py-2 text-sm font-mono focus:outline-none focus:ring-2 focus:ring-brand-500"
                />
              </div>
            )}

            <div className="flex gap-2 pt-1">
              <button
                type="submit"
                disabled={createMut.isPending}
                className="px-4 py-2 bg-brand-600 text-white text-sm font-medium rounded hover:bg-brand-700 disabled:opacity-50"
              >
                {createMut.isPending ? 'Scheduling…' : 'Schedule Post'}
              </button>
              <button
                type="button"
                onClick={() => { setShowForm(false); setForm(defaultForm); }}
                className="px-4 py-2 text-gray-600 text-sm font-medium rounded hover:bg-gray-100"
              >
                Cancel
              </button>
            </div>
            {createMut.isError && (
              <p className="text-red-600 text-xs">Failed to schedule post. Check the form and try again.</p>
            )}
          </form>
        )}
      </div>

      {/* Post list */}
      {isLoading ? (
        <p className="text-sm text-gray-500">Loading…</p>
      ) : posts.length === 0 ? (
        <div className="text-center text-gray-400 py-10">
          <Calendar className="mx-auto h-8 w-8 mb-2 opacity-30" />
          <p className="text-sm">No scheduled posts yet</p>
        </div>
      ) : (
        <ul className="space-y-2">
          {posts.map((p) => (
            <ScheduledPostRow
              key={p.id}
              post={p}
              onCancel={() => cancelMut.mutate(p.id)}
            />
          ))}
        </ul>
      )}
    </div>
  );
}

function ScheduledPostRow({
  post,
  onCancel,
}: {
  post: ScheduledPostResponse;
  onCancel: () => void;
}) {
  return (
    <li className="border border-gray-200 rounded-lg p-4 flex gap-3">
      <div className="mt-0.5">
        {post.scheduleType === 'ONE_SHOT' ? (
          <Clock className="h-4 w-4 text-gray-400" />
        ) : (
          <RefreshCw className="h-4 w-4 text-gray-400" />
        )}
      </div>
      <div className="flex-1 min-w-0">
        <div className="flex items-center gap-2 mb-1">
          <span className="text-sm font-medium text-gray-700">#{post.channelName}</span>
          <span
            className={`inline-block px-2 py-0.5 rounded-full text-xs font-medium ${STATUS_BADGE[post.status] ?? ''}`}
          >
            {post.status}
          </span>
          {post.scheduleType === 'CRON' && (
            <code className="text-xs bg-gray-100 text-gray-600 px-1.5 py-0.5 rounded">
              {post.cronExpression}
            </code>
          )}
        </div>
        <p className="text-sm text-gray-600 line-clamp-2">{post.body}</p>
        <p className="text-xs text-gray-400 mt-1">
          {post.status === 'PENDING'
            ? `Next: ${new Date(post.nextFireAt).toLocaleString()}`
            : post.lastSentAt
            ? `Sent: ${new Date(post.lastSentAt).toLocaleString()}`
            : ''}
        </p>
      </div>
      {post.status === 'PENDING' && (
        <button
          onClick={onCancel}
          className="shrink-0 text-gray-400 hover:text-red-500 transition-colors"
          title="Cancel"
        >
          <Trash2 className="h-4 w-4" />
        </button>
      )}
    </li>
  );
}

// ─── Weekly Digest (n8n) ─────────────────────────────────────────────────────

function WeeklyDigestTab({ workspaceId: _workspaceId }: { workspaceId: string }) {
  const qc = useQueryClient();

  const { data: prefs, isLoading: prefsLoading } = useQuery({
    queryKey: ['digest-preferences'],
    queryFn:  fetchDigestPreferences,
  });

  const updateMut = useMutation({
    mutationFn: (enabled: boolean) => updateDigestPreferences(enabled),
    onSuccess:  () => qc.invalidateQueries({ queryKey: ['digest-preferences'] }),
  });

  if (prefsLoading) {
    return <p className="text-sm text-gray-500">Loading…</p>;
  }

  return (
    <div className="max-w-2xl space-y-6">
      <div className="rounded-lg border border-violet-200 bg-violet-50 p-4">
        <div className="flex items-center gap-2 text-violet-900 font-semibold text-sm mb-2">
          <Sparkles className="h-4 w-4" />
          Weekly Digest every Friday 5pm
        </div>
        <p className="text-sm text-violet-800 leading-relaxed">
          Each Friday at 5pm you can receive a personalized summary of activity in{' '}
          <strong>channels you belong to</strong>. Summaries are posted to{' '}
          <code className="bg-white/70 px-1 rounded">#weekly-digest</code>.
        </p>
      </div>

      <label className="flex items-start gap-3 cursor-pointer">
        <input
          type="checkbox"
          checked={prefs?.weeklyDigestEnabled ?? true}
          onChange={(e) => updateMut.mutate(e.target.checked)}
          disabled={updateMut.isPending}
          className="mt-1 h-4 w-4 rounded border-gray-300 text-brand-600"
        />
        <span>
          <span className="text-sm font-medium text-gray-800 block">Receive weekly digest</span>
          <span className="text-xs text-gray-500">
            Opt out if you do not want a Friday summary across your channels.
          </span>
        </span>
      </label>
    </div>
  );
}

// ─── Moderation Guidelines ───────────────────────────────────────────────────

function GuidelinesTab({ workspaceId }: { workspaceId: string }) {
  const qc = useQueryClient();

  const { data: guidelines, isLoading } = useQuery({
    queryKey: ['community-guidelines', workspaceId],
    queryFn:  () => fetchCommunityGuidelines(workspaceId),
  });

  const [draft, setDraft] = useState<string | null>(null);
  const current = draft ?? guidelines ?? '';

  const saveMut = useMutation({
    mutationFn: (text: string) => updateCommunityGuidelines(workspaceId, text),
    onSuccess:  () => {
      qc.invalidateQueries({ queryKey: ['community-guidelines', workspaceId] });
      setDraft(null);
    },
  });

  if (isLoading) return <p className="text-sm text-gray-500">Loading…</p>;

  return (
    <div className="max-w-2xl space-y-4">
      <div>
        <h3 className="text-sm font-semibold text-gray-800 mb-1">Community guidelines</h3>
        <p className="text-xs text-gray-500 mb-3">
          Used with the <strong>OpenAI Moderation API</strong> for context-aware checks.
          Objectionable posts are auto-hidden and flagged. Leave blank to use the default rules shipped with the app.
        </p>
        <textarea
          rows={14}
          value={current}
          onChange={(e) => setDraft(e.target.value)}
          placeholder="Describe your community rules (respect, no spam, no harassment…)"
          className="w-full border border-gray-300 rounded px-3 py-2 text-sm font-mono resize-y focus:outline-none focus:ring-2 focus:ring-brand-500"
        />
        <p className="text-xs text-gray-400 mt-1">{current.length} / 10000</p>
      </div>
      <div className="flex gap-2">
        <button
          disabled={saveMut.isPending || draft === null}
          onClick={() => saveMut.mutate(current)}
          className="px-4 py-2 bg-brand-600 text-white text-sm font-medium rounded hover:bg-brand-700 disabled:opacity-50"
        >
          {saveMut.isPending ? 'Saving…' : 'Save guidelines'}
        </button>
        {draft !== null && (
          <button
            onClick={() => setDraft(null)}
            className="px-4 py-2 text-gray-600 text-sm font-medium rounded hover:bg-gray-100"
          >
            Discard
          </button>
        )}
      </div>
    </div>
  );
}

// ─── Welcome Message ──────────────────────────────────────────────────────────

function WelcomeMessageTab({ workspaceId }: { workspaceId: string }) {
  const qc = useQueryClient();

  const { data: template, isLoading } = useQuery({
    queryKey: ['welcome-template', workspaceId],
    queryFn:  () => fetchWelcomeTemplate(workspaceId),
  });

  const [draft, setDraft] = useState<string | null>(null);
  const current = draft ?? template ?? '';

  const saveMut = useMutation({
    mutationFn: (t: string) => updateWelcomeTemplate(workspaceId, t),
    onSuccess:  () => {
      qc.invalidateQueries({ queryKey: ['welcome-template', workspaceId] });
      setDraft(null);
    },
  });

  if (isLoading) return <p className="text-sm text-gray-500">Loading…</p>;

  return (
    <div className="max-w-lg space-y-4">
      <div>
        <h3 className="text-sm font-semibold text-gray-800 mb-1">Welcome message</h3>
        <p className="text-xs text-gray-500 mb-3">
          Sent by the bot to <strong>#general</strong> when someone joins the workspace.
          Placeholders: <code className="bg-gray-100 px-1 rounded">{'{name}'}</code>,{' '}
          <code className="bg-gray-100 px-1 rounded">{'{interests}'}</code>,{' '}
          <code className="bg-gray-100 px-1 rounded">{'{about}'}</code>.
          If the member filled in their profile, the bot generates a <strong>personalized</strong> LLM welcome
          using their about me and interests.
        </p>
        <textarea
          rows={5}
          value={current}
          onChange={(e) => setDraft(e.target.value)}
          placeholder={`👋 Welcome to the workspace, {name}! Feel free to introduce yourself in this channel.`}
          className="w-full border border-gray-300 rounded px-3 py-2 text-sm resize-none focus:outline-none focus:ring-2 focus:ring-brand-500"
        />
        <p className="text-xs text-gray-400 mt-1">{current.length} / 2000</p>
      </div>

      <div className="flex gap-2">
        <button
          disabled={saveMut.isPending || draft === null}
          onClick={() => saveMut.mutate(current)}
          className="px-4 py-2 bg-brand-600 text-white text-sm font-medium rounded hover:bg-brand-700 disabled:opacity-50"
        >
          {saveMut.isPending ? 'Saving…' : 'Save'}
        </button>
        {draft !== null && (
          <button
            onClick={() => setDraft(null)}
            className="px-4 py-2 text-gray-600 text-sm font-medium rounded hover:bg-gray-100"
          >
            Discard
          </button>
        )}
      </div>

      {saveMut.isSuccess && (
        <p className="text-green-600 text-xs">Welcome message saved.</p>
      )}
      {saveMut.isError && (
        <p className="text-red-600 text-xs">Failed to save. Please try again.</p>
      )}
    </div>
  );
}

// ─── Roles ────────────────────────────────────────────────────────────────────

export function RolesTab({ workspaceId }: { workspaceId: string }) {
  const qc = useQueryClient();

  const { data: roles = [], isLoading: rolesLoading } = useQuery({
    queryKey: ['roles', workspaceId],
    queryFn:  () => fetchRoles(workspaceId),
  });

  const { data: members = [], isLoading: membersLoading } = useQuery({
    queryKey: ['members-roles', workspaceId],
    queryFn:  () => fetchMembersWithRoles(workspaceId),
  });

  const [newRoleName, setNewRoleName] = useState('');

  const createMut = useMutation({
    mutationFn: (name: string) => createRole(workspaceId, name),
    onSuccess:  () => {
      qc.invalidateQueries({ queryKey: ['roles', workspaceId] });
      setNewRoleName('');
    },
  });

  const deleteMut = useMutation({
    mutationFn: (roleId: string) => deleteRole(workspaceId, roleId),
    onSuccess:  () => qc.invalidateQueries({ queryKey: ['roles', workspaceId] }),
  });

  const assignMut = useMutation({
    mutationFn: ({ memberId, roleId }: { memberId: string; roleId: string }) =>
      assignRoleToMember(workspaceId, memberId, roleId),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['members-roles', workspaceId] }),
  });

  const removeMut = useMutation({
    mutationFn: ({ memberId, roleId }: { memberId: string; roleId: string }) =>
      removeRoleFromMember(workspaceId, memberId, roleId),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['members-roles', workspaceId] }),
  });

  return (
    <div className="space-y-6">
      {/* Role management */}
      <div className="border border-gray-200 rounded-lg p-4">
        <h3 className="text-sm font-semibold text-gray-800 mb-3 flex items-center gap-2">
          <Shield className="h-4 w-4 text-gray-400" />
          Workspace Roles
        </h3>

        {/* Create new role */}
        <form
          onSubmit={(e) => {
            e.preventDefault();
            if (newRoleName.trim()) {
              createMut.mutate(newRoleName.trim());
            }
          }}
          className="flex gap-2 mb-4"
        >
          <input
            value={newRoleName}
            onChange={(e) => setNewRoleName(e.target.value)}
            placeholder="New role name…"
            maxLength={50}
            className="flex-1 border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
          />
          <button
            type="submit"
            disabled={!newRoleName.trim() || createMut.isPending}
            className="px-3 py-2 bg-brand-600 text-white text-sm font-medium rounded hover:bg-brand-700 disabled:opacity-50 flex items-center gap-1"
          >
            <Plus className="h-3.5 w-3.5" />
            Add
          </button>
        </form>

        {createMut.isError && (
          <p className="text-red-600 text-xs mb-3">Failed to create role. Name may already exist.</p>
        )}

        {rolesLoading ? (
          <p className="text-sm text-gray-500">Loading…</p>
        ) : (
          <div className="flex flex-wrap gap-2">
            {roles.map((role) => (
              <span
                key={role.id}
                className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-medium ${
                  role.isSystem
                    ? 'bg-blue-50 text-blue-700'
                    : 'bg-gray-100 text-gray-700'
                }`}
              >
                {role.name}
                {role.isSystem && (
                  <span className="text-blue-400" title="System role">(default)</span>
                )}
                {!role.isSystem && (
                  <button
                    onClick={() => deleteMut.mutate(role.id)}
                    className="text-gray-400 hover:text-red-500"
                    title="Delete role"
                  >
                    <X className="h-3 w-3" />
                  </button>
                )}
              </span>
            ))}
          </div>
        )}
      </div>

      {/* Member role assignment */}
      <div className="border border-gray-200 rounded-lg p-4">
        <h3 className="text-sm font-semibold text-gray-800 mb-3">Member Roles</h3>

        {membersLoading ? (
          <p className="text-sm text-gray-500">Loading…</p>
        ) : members.length === 0 ? (
          <p className="text-sm text-gray-400">No members found.</p>
        ) : (
          <ul className="space-y-2">
            {members.map((member) => (
              <MemberRoleRow
                key={member.memberId}
                member={member}
                roles={roles}
                onAssign={(roleId) => assignMut.mutate({ memberId: member.memberId, roleId })}
                onRemove={(roleId) => removeMut.mutate({ memberId: member.memberId, roleId })}
              />
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}

function MemberRoleRow({
  member,
  roles,
  onAssign,
  onRemove,
}: {
  member: MemberRolesResponse;
  roles: WorkspaceRoleResponse[];
  onAssign: (roleId: string) => void;
  onRemove: (roleId: string) => void;
}) {
  const [adding, setAdding] = useState(false);
  const [selected, setSelected] = useState('');

  const unassignedRoles = roles.filter(
    (r) => !member.roles.includes(r.name),
  );

  return (
    <li className="border border-gray-100 rounded-lg p-3">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-sm font-medium text-gray-800">
            {member.userDisplayName || member.userEmail}
          </p>
          <p className="text-xs text-gray-400">{member.userEmail}</p>
        </div>
        <div className="flex items-center gap-2">
          <div className="flex items-center gap-1">
            {member.roles.length === 0 && (
              <span className="text-xs text-gray-400 italic">No roles</span>
            )}
            {member.roles.map((r) => (
              <span
                key={r}
                className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium ${
                  r === 'Admin' || r === 'Moderator'
                    ? 'bg-blue-50 text-blue-700'
                    : 'bg-gray-100 text-gray-700'
                }`}
              >
                {r}
                <button
                  onClick={() => {
                    const role = roles.find((x) => x.name === r);
                    if (role) onRemove(role.id);
                  }}
                  className="text-gray-400 hover:text-red-500"
                  title="Remove role"
                >
                  <X className="h-3 w-3" />
                </button>
              </span>
            ))}
          </div>

          {adding ? (
            <div className="flex items-center gap-1">
              <select
                value={selected}
                onChange={(e) => setSelected(e.target.value)}
                className="border border-gray-300 rounded px-2 py-1 text-xs focus:outline-none focus:ring-1 focus:ring-brand-500"
              >
                <option value="">Select…</option>
                {unassignedRoles.map((r) => (
                  <option key={r.id} value={r.id}>{r.name}</option>
                ))}
              </select>
              <button
                disabled={!selected}
                onClick={() => { onAssign(selected); setAdding(false); setSelected(''); }}
                className="px-2 py-1 bg-brand-600 text-white text-xs rounded hover:bg-brand-700 disabled:opacity-50"
              >
                Add
              </button>
              <button
                onClick={() => setAdding(false)}
                className="text-gray-400 hover:text-gray-600 text-xs"
              >
                Cancel
              </button>
            </div>
          ) : (
            unassignedRoles.length > 0 && (
              <button
                onClick={() => setAdding(true)}
                className="text-xs text-brand-600 hover:text-brand-700 font-medium"
              >
                + Add role
              </button>
            )
          )}
        </div>
      </div>
    </li>
  );
}
