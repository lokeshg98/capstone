import { useQuery } from '@tanstack/react-query';
import { fetchChannelMembers, type ChannelMemberResponse } from '@/features/messages/memberApi';
import { usePresence } from '@/hooks/usePresence';
import { cn } from '@/lib/utils';

interface Props {
  workspaceId: string;
  channelId:  string;
  onSelectUser: (userId: string) => void;
}

export default function MemberList({ workspaceId, channelId, onSelectUser }: Props) {
  const { isOnline } = usePresence(workspaceId);

  const { data: members = [], isLoading } = useQuery({
    queryKey: ['members', channelId],
    queryFn:  () => fetchChannelMembers(workspaceId, channelId),
    enabled:  !!channelId && !!workspaceId,
  });

  if (isLoading) {
    return (
      <div className="w-56 shrink-0 bg-gray-50 border-l border-gray-200 overflow-y-auto p-3">
        <p className="text-xs text-gray-400">Loading…</p>
      </div>
    );
  }

  const admins = members.filter((m) => m.roles.includes('Admin'));
  const mods = members.filter((m) => !m.roles.includes('Admin') && m.roles.includes('Moderator'));
  const users = members.filter((m) => !m.roles.includes('Admin') && !m.roles.includes('Moderator'));

  const onlineAdmins = admins.filter((m) => isOnline(m.userId));
  const offlineAdmins = admins.filter((m) => !isOnline(m.userId));
  const onlineMods = mods.filter((m) => isOnline(m.userId));
  const offlineMods = mods.filter((m) => !isOnline(m.userId));
  const onlineUsers = users.filter((m) => isOnline(m.userId));
  const offlineUsers = users.filter((m) => !isOnline(m.userId));

  return (
    <div className="w-56 shrink-0 bg-gray-50 border-l border-gray-200 overflow-y-auto">
      {onlineAdmins.length > 0 && <MemberGroup label="Admins" members={onlineAdmins} onSelectUser={onSelectUser} isOnline />}
      {offlineAdmins.length > 0 && <MemberGroup label="Admins" members={offlineAdmins} onSelectUser={onSelectUser} isOnline={false} />}
      {onlineMods.length > 0 && <MemberGroup label="Moderators" members={onlineMods} onSelectUser={onSelectUser} isOnline />}
      {offlineMods.length > 0 && <MemberGroup label="Moderators" members={offlineMods} onSelectUser={onSelectUser} isOnline={false} />}
      {onlineUsers.length > 0 && <MemberGroup label="Online" members={onlineUsers} onSelectUser={onSelectUser} isOnline />}
      {offlineUsers.length > 0 && <MemberGroup label="Offline" members={offlineUsers} onSelectUser={onSelectUser} isOnline={false} />}
      {members.length === 0 && (
        <p className="text-xs text-gray-400 p-3">No members yet.</p>
      )}
    </div>
  );
}

function MemberGroup({
  label,
  members,
  isOnline,
  onSelectUser,
}: {
  label: string;
  members: ChannelMemberResponse[];
  isOnline: boolean | undefined;
  onSelectUser: (userId: string) => void;
}) {
  return (
    <div>
      <div className="px-3 py-2">
        <span className="text-xs font-semibold uppercase tracking-wider text-gray-400">{label} — {members.length}</span>
      </div>
      {members.map((m) => (
        <button
          key={m.userId}
          onClick={() => onSelectUser(m.userId)}
          className="w-full flex items-center gap-2 px-3 py-1.5 hover:bg-gray-100 transition-colors text-left"
        >
          <div className="relative shrink-0">
            <div className={cn(
              'h-7 w-7 rounded-full flex items-center justify-center text-xs font-bold text-white',
              avatarColor(m.displayName ?? m.userId),
            )}>
              {(m.displayName ?? '?').charAt(0).toUpperCase()}
            </div>
            {isOnline !== undefined && (
              <span
                className={cn(
                  'absolute -bottom-0.5 -right-0.5 h-3 w-3 rounded-full border-2 border-gray-50',
                  isOnline ? 'bg-green-500' : 'bg-gray-300',
                )}
              />
            )}
          </div>
          <span className={cn('text-sm truncate', isOnline === false ? 'text-gray-400' : 'text-gray-700')}>
            {m.displayName ?? 'Unknown'}
          </span>
        </button>
      ))}
    </div>
  );
}

function avatarColor(seed: string): string {
  const colors = [
    'bg-blue-500', 'bg-green-500', 'bg-purple-500', 'bg-pink-500',
    'bg-indigo-500', 'bg-teal-500', 'bg-orange-500', 'bg-cyan-500',
  ];
  let hash = 0;
  for (let i = 0; i < seed.length; i++) {
    hash = seed.charCodeAt(i) + ((hash << 5) - hash);
  }
  return colors[Math.abs(hash) % colors.length];
}
