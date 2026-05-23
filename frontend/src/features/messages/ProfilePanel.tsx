import { useState, useEffect } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { X, Pencil, Shield } from 'lucide-react';
import { fetchUserProfile } from '@/features/auth/profileApi';
import { updateMyProfile } from '@/features/profile/profileApi';
import { assignRoleToMember, removeRoleFromMember, fetchRoles } from '@/features/workspace/roleApi';

interface Role {
  id: string;
  name: string;
  isSystem: boolean;
}

interface Props {
  userId:       string;
  currentUserId: string;
  workspaceId:  string;
  canEditRoles: boolean;
  memberId?:    string;
  memberRoles?: string[];
  onClose:      () => void;
}

export default function ProfilePanel({
  userId,
  currentUserId,
  workspaceId,
  canEditRoles,
  memberId,
  memberRoles = [],
  onClose,
}: Props) {
  const isSelf = userId === currentUserId;
  const [editing, setEditing] = useState(false);
  const [form, setForm] = useState({ statusMessage: '', aboutMe: '', interests: '' });
  const qc = useQueryClient();

  const { data: profile, isLoading } = useQuery({
    queryKey: ['profile', userId],
    queryFn:  () => fetchUserProfile(userId),
  });

  const { data: roles = [] } = useQuery({
    queryKey: ['roles', workspaceId],
    queryFn:  () => fetchRoles(workspaceId),
  });

  useEffect(() => {
    if (profile && editing) {
      setForm({
        statusMessage: profile.statusMessage ?? '',
        aboutMe:       profile.aboutMe ?? '',
        interests:     profile.interests.join(', '),
      });
    }
  }, [profile, editing]);

  const handleSave = async () => {
    await updateMyProfile({
      statusMessage: form.statusMessage || undefined,
      aboutMe:       form.aboutMe || undefined,
      interests:     form.interests.split(',').map((s) => s.trim()).filter(Boolean),
    });
    qc.invalidateQueries({ queryKey: ['profile', userId] });
    setEditing(false);
  };

  const handleAssignRole = async (roleId: string) => {
    if (!memberId) return;
    await assignRoleToMember(workspaceId, memberId, roleId);
    qc.invalidateQueries({ queryKey: ['profile', userId] });
    qc.invalidateQueries({ queryKey: ['roles', workspaceId] });
  };

  const handleRemoveRole = async (roleId: string) => {
    if (!memberId) return;
    await removeRoleFromMember(workspaceId, memberId, roleId);
    qc.invalidateQueries({ queryKey: ['profile', userId] });
    qc.invalidateQueries({ queryKey: ['roles', workspaceId] });
  };

  const hasRole = (name: string) => memberRoles.includes(name);

  return (
    <div className="w-72 shrink-0 bg-white border-l border-gray-200 overflow-y-auto flex flex-col">
      <div className="h-14 px-4 flex items-center justify-between border-b border-gray-200 shrink-0">
        <span className="font-semibold text-gray-800 text-sm">Profile</span>
        <button onClick={onClose} className="text-gray-400 hover:text-gray-600">
          <X className="h-4 w-4" />
        </button>
      </div>

      {isLoading ? (
        <p className="text-sm text-gray-400 p-4">Loading…</p>
      ) : profile ? (
        <div className="flex-1 overflow-y-auto">
          {/* Avatar + name */}
          <div className="p-4 flex flex-col items-center text-center border-b border-gray-100">
            <div className="h-16 w-16 rounded-full bg-gradient-to-br from-brand-500 to-purple-600 flex items-center justify-center text-white text-2xl font-bold mb-2">
              {(profile.displayName ?? profile.email).charAt(0).toUpperCase()}
            </div>
            <h3 className="font-semibold text-gray-900">{profile.displayName ?? 'Unknown'}</h3>
            <p className="text-xs text-gray-400 mt-0.5">{profile.email}</p>

            {/* Status message */}
            {editing ? (
              <input
                autoFocus
                value={form.statusMessage}
                onChange={(e) => setForm({ ...form, statusMessage: e.target.value })}
                placeholder="Set a status message…"
                maxLength={200}
                className="mt-3 w-full border border-gray-200 rounded px-2 py-1 text-xs text-center focus:outline-none focus:ring-1 focus:ring-brand-500"
              />
            ) : (
              profile.statusMessage ? (
                <p className="mt-2 text-xs text-gray-500 italic">{profile.statusMessage}</p>
              ) : isSelf ? (
                <button onClick={() => setEditing(true)} className="mt-2 text-xs text-brand-600 hover:text-brand-700">
                  + Add a status
                </button>
              ) : null
            )}
          </div>

          <div className="p-4 space-y-4 text-sm">
            {/* About me */}
            <Section label="About Me">
              {editing ? (
                <textarea
                  value={form.aboutMe}
                  onChange={(e) => setForm({ ...form, aboutMe: e.target.value })}
                  placeholder="Write a short bio…"
                  maxLength={500}
                  rows={3}
                  className="w-full border border-gray-200 rounded px-2 py-1 text-xs resize-none focus:outline-none focus:ring-1 focus:ring-brand-500"
                />
              ) : (
                profile.aboutMe ? (
                  <p className="text-gray-600 text-xs leading-relaxed">{profile.aboutMe}</p>
                ) : isSelf ? (
                  <p className="text-gray-400 text-xs italic">Tell others about yourself.</p>
                ) : null
              )}
            </Section>

            {/* Interests */}
            <Section label="Interests">
              {editing ? (
                <input
                  value={form.interests}
                  onChange={(e) => setForm({ ...form, interests: e.target.value })}
                  placeholder="coding, reading, music"
                  maxLength={500}
                  className="w-full border border-gray-200 rounded px-2 py-1 text-xs focus:outline-none focus:ring-1 focus:ring-brand-500"
                />
              ) : profile.interests.length > 0 ? (
                <div className="flex flex-wrap gap-1">
                  {profile.interests.map((tag) => (
                    <span key={tag} className="px-2 py-0.5 bg-brand-50 text-brand-700 rounded-full text-xs">
                      {tag}
                    </span>
                  ))}
                </div>
              ) : isSelf ? (
                <p className="text-gray-400 text-xs italic">Add your interests.</p>
              ) : null}
            </Section>

            {/* Roles (visible to everyone) */}
            {memberRoles.length > 0 && (
              <Section label="Roles">
                <div className="flex flex-wrap gap-1">
                  {memberRoles.map((role) => (
                    <span
                      key={role}
                      className={`px-2 py-0.5 rounded-full text-xs font-medium ${
                        role === 'Admin' || role === 'Moderator'
                          ? 'bg-blue-50 text-blue-700'
                          : 'bg-gray-100 text-gray-700'
                      }`}
                    >
                      {role}
                    </span>
                  ))}
                </div>
              </Section>
            )}

          </div>

          {/* Role editing for admins */}
          {canEditRoles && !isSelf && memberId && (
            <div className="p-4 border-t border-gray-100">
              <h4 className="text-xs font-semibold text-gray-700 flex items-center gap-1 mb-2">
                <Shield className="h-3 w-3" /> Role Management
              </h4>
              <div className="space-y-1">
                {roles.map((role: Role) => (
                  <label key={role.id} className="flex items-center gap-2 text-xs text-gray-600 py-0.5">
                    <input
                      type="checkbox"
                      checked={hasRole(role.name)}
                      onChange={(e) => {
                        if (e.target.checked) handleAssignRole(role.id);
                        else handleRemoveRole(role.id);
                      }}
                      className="rounded border-gray-300 text-brand-600 focus:ring-brand-500"
                    />
                    {role.name}
                    {role.isSystem && <span className="text-gray-400 text-[10px]">(system)</span>}
                  </label>
                ))}
              </div>
            </div>
          )}
        </div>
      ) : (
        <p className="text-sm text-gray-400 p-4">User not found.</p>
      )}

      {/* Edit / Save buttons */}
      {isSelf && (
        <div className="shrink-0 p-3 border-t border-gray-100">
          {editing ? (
            <div className="flex gap-2">
              <button onClick={handleSave} className="flex-1 py-1.5 bg-brand-600 text-white text-xs font-medium rounded hover:bg-brand-700">
                Save
              </button>
              <button onClick={() => setEditing(false)} className="flex-1 py-1.5 text-gray-600 text-xs font-medium rounded hover:bg-gray-100">
                Cancel
              </button>
            </div>
          ) : (
            <button onClick={() => setEditing(true)} className="w-full py-1.5 text-brand-600 text-xs font-medium rounded hover:bg-brand-50 flex items-center justify-center gap-1">
              <Pencil className="h-3 w-3" /> Edit Profile
            </button>
          )}
        </div>
      )}
    </div>
  );
}

function Section({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div>
      <h4 className="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-1.5">{label}</h4>
      {children}
    </div>
  );
}
