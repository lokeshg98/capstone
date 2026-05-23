import { Users } from 'lucide-react';
import { useQuery } from '@tanstack/react-query';
import { fetchChannelMembers, type ChannelMemberResponse } from '@/features/channels/channelApi';
import { cn } from '@/lib/utils';

interface Props {
  workspaceId: string;
  channelId:   string;
}

export default function ChannelMembersPanel({ workspaceId, channelId }: Props) {
  const { data: members = [], isLoading } = useQuery({
    queryKey: ['channel-members', workspaceId, channelId],
    queryFn:  () => fetchChannelMembers(workspaceId, channelId),
  });

  return (
    <aside className="w-56 shrink-0 border-l border-gray-200 bg-white flex flex-col h-full">
      <header className="h-14 shrink-0 px-4 flex items-center gap-2 border-b border-gray-200">
        <Users className="h-4 w-4 text-gray-400" />
        <div>
          <p className="text-sm font-semibold text-gray-900">Members</p>
          <p className="text-xs text-gray-500">{members.length} in channel</p>
        </div>
      </header>

      <div className="flex-1 overflow-y-auto py-2">
        {isLoading && (
          <p className="px-4 text-xs text-gray-400">Loading…</p>
        )}
        {!isLoading && members.length === 0 && (
          <p className="px-4 text-xs text-gray-400">No members yet</p>
        )}
        {members.map((member) => (
          <MemberRow key={member.userId} member={member} />
        ))}
      </div>
    </aside>
  );
}

function MemberRow({ member }: { member: ChannelMemberResponse }) {
  const initials = (member.displayName ?? member.email)
    .slice(0, 2)
    .toUpperCase();

  return (
    <div className="flex items-center gap-2.5 px-3 py-2 hover:bg-gray-50">
      <div className="h-8 w-8 rounded-full bg-brand-600 flex items-center justify-center shrink-0 overflow-hidden">
        {member.avatarUrl ? (
          <img src={member.avatarUrl} alt="" className="h-8 w-8 object-cover" />
        ) : (
          <span className="text-white text-xs font-semibold">{initials}</span>
        )}
      </div>
      <div className="min-w-0">
        <p className={cn('text-sm font-medium text-gray-900 truncate')}>
          {member.displayName ?? 'Member'}
        </p>
        <p className="text-xs text-gray-400 truncate">{member.email}</p>
      </div>
    </div>
  );
}
