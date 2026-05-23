import { useState, useRef, useEffect, type KeyboardEvent } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { Send, Smile } from 'lucide-react';
import { api } from '@/lib/api';
import { cn } from '@/lib/utils';
import FileUploadButton from './FileUploadButton';
import { type AttachmentResponse } from './attachmentApi';
import EmojiPicker from '@/features/emoji/EmojiPicker';
import { type EmojiSearchResult } from '@/features/emoji/emojiApi';
import type { MessageResponse, PageResponse } from './messageApi';

interface Props {
  channelId:    string;
  channelName:  string;
  threadRootId?: string | null;
  addOptimisticMessage?: (msg: MessageResponse) => void;
  commitMessage?: (tempId: string, msg: MessageResponse) => void;
  removeOptimisticMessage?: (id: string) => void;
  currentUser?: { id: string; displayName: string | null; avatarUrl: string | null } | null;
}

export default function MessageInput({
  channelId,
  channelName,
  threadRootId = null,
  addOptimisticMessage,
  commitMessage,
  removeOptimisticMessage,
  currentUser,
}: Props) {
  const [body,              setBody]              = useState('');
  const [sending,           setSending]           = useState(false);
  const [showEmoji,         setShowEmoji]         = useState(false);
  const [pendingAttachment, setPendingAttachment] = useState<AttachmentResponse | null>(null);
  const qc = useQueryClient();
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  const appendToCache = (newMsg: MessageResponse) => {
    if (newMsg.threadRootId) {
      qc.setQueryData(['thread', channelId, newMsg.threadRootId], (prev: unknown) => {
        if (!prev || typeof prev !== 'object' || !('replies' in prev)) return prev;
        const thread = prev as { root: MessageResponse; replies: MessageResponse[]; summary: unknown };
        if (thread.replies.some((m) => m.id === newMsg.id)) return prev;
        return { ...thread, replies: [...thread.replies, newMsg] };
      });
      qc.setQueryData<PageResponse<MessageResponse>>(['messages', channelId], (prev) => {
        if (!prev) return prev;
        return {
          ...prev,
          content: prev.content.map((m) =>
            m.id === newMsg.threadRootId
              ? { ...m, replyCount: (m.replyCount ?? 0) + 1 }
              : m,
          ),
        };
      });
      return;
    }
    qc.setQueryData<PageResponse<MessageResponse>>(['messages', channelId], (prev) => {
      if (!prev) return prev;
      if (prev.content.some((m) => m.id === newMsg.id)) return prev;
      return {
        ...prev,
        content: [...prev.content, { ...newMsg, replyCount: newMsg.replyCount ?? 0 }],
      };
    });
  };

  const insertEmoji = (emoji: EmojiSearchResult) => {
    setBody((prev) => prev + emoji.unicode);
    textareaRef.current?.focus();
  };

  const send = async () => {
    const trimmed = body.trim();
    if ((!trimmed && !pendingAttachment) || sending) return;

    const draftText = trimmed || (pendingAttachment?.filename ?? '');
    const payload = {
      body:         draftText,
      threadRootId: threadRootId ?? null,
      attachmentId: pendingAttachment?.id ?? null,
    };

    const attachmentSnapshot = pendingAttachment;
    const useOptimistic = !threadRootId && addOptimisticMessage && commitMessage && removeOptimisticMessage;
    const optimisticId = useOptimistic ? makeTempId() : null;

    setSending(true);
    setBody('');
    setPendingAttachment(null);
    if (textareaRef.current) textareaRef.current.style.height = 'auto';

    if (useOptimistic && optimisticId) {
      const now = new Date().toISOString();
      const actor = currentUser ?? { id: 'me', displayName: 'You', avatarUrl: null };
      try {
        addOptimisticMessage({
          id: optimisticId,
          channelId,
          workspaceId: '',
          author: { id: actor.id, displayName: actor.displayName, avatarUrl: actor.avatarUrl },
          body: draftText,
          threadRootId: null,
          status: 'ACTIVE',
          edited: false,
          editedAt: null,
          createdAt: now,
          reactions: [],
          replyCount: 0,
          attachment: attachmentSnapshot
            ? {
                id: attachmentSnapshot.id,
                filename: attachmentSnapshot.filename,
                mimeType: attachmentSnapshot.mimeType,
                kind: attachmentSnapshot.kind,
                sizeBytes: attachmentSnapshot.sizeBytes,
              }
            : null,
          pending: true,
        });
      } catch {
        // ignore optimistic UI errors
      }
    }

    try {
      const res = await api.post<{ ok: boolean; data: MessageResponse }>(
        `/channels/${channelId}/messages`,
        payload,
      );
      if (useOptimistic && optimisticId && commitMessage) {
        commitMessage(optimisticId, { ...res.data.data, pending: false });
      } else {
        appendToCache(res.data.data);
      }
    } catch {
      if (useOptimistic && optimisticId && removeOptimisticMessage) {
        removeOptimisticMessage(optimisticId);
      }
      setBody(trimmed);
      if (attachmentSnapshot) setPendingAttachment(attachmentSnapshot);
    } finally {
      setSending(false);
      textareaRef.current?.focus();
    }
  };

  useEffect(() => {
    if (!sending) textareaRef.current?.focus();
  }, [sending]);

  const handleKeyDown = (e: KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      void send();
    }
  };

  const handleChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    setBody(e.target.value);
    const el = e.target;
    el.style.height = 'auto';
    el.style.height = Math.min(el.scrollHeight, 144) + 'px';
  };

  const canSend = (body.trim().length > 0 || pendingAttachment !== null) && !sending;

  return (
    <div className="shrink-0 px-4 pb-4 pt-2 relative">
      {showEmoji && (
        <EmojiPicker onSelect={insertEmoji} onClose={() => setShowEmoji(false)} />
      )}

      <div className={cn(
        'rounded-xl border bg-white transition-shadow',
        'focus-within:ring-2 focus-within:ring-brand-500 focus-within:border-transparent',
        'border-gray-300',
      )}
      >
        {pendingAttachment && (
          <div className="px-3 pt-2">
            <FileUploadButton
              pending={pendingAttachment}
              onAttached={setPendingAttachment}
              onClear={() => setPendingAttachment(null)}
            />
          </div>
        )}

        <div className="flex items-end gap-2 px-3 py-2">
          {!pendingAttachment && (
            <FileUploadButton
              pending={null}
              onAttached={setPendingAttachment}
              onClear={() => setPendingAttachment(null)}
            />
          )}

          <button
            type="button"
            onClick={() => setShowEmoji((v) => !v)}
            className="p-1.5 rounded-lg text-gray-400 hover:text-brand-600 hover:bg-gray-100 shrink-0"
            title="Insert emoji"
          >
            <Smile className="h-5 w-5" />
          </button>

          <textarea
            ref={textareaRef}
            rows={1}
            value={body}
            onChange={handleChange}
            onKeyDown={handleKeyDown}
            placeholder={threadRootId ? 'Reply in thread…' : `Message #${channelName}`}
            className="flex-1 resize-none outline-none text-sm text-gray-900 placeholder-gray-400 bg-transparent max-h-36"
            disabled={sending}
          />

          <button
            type="button"
            onClick={() => void send()}
            disabled={!canSend}
            className={cn(
              'h-8 w-8 rounded-lg flex items-center justify-center shrink-0 transition-colors',
              canSend
                ? 'bg-brand-600 text-white hover:bg-brand-700'
                : 'bg-gray-100 text-gray-400 cursor-not-allowed',
            )}
            title="Send (Enter)"
          >
            <Send className="h-4 w-4" />
          </button>
        </div>
      </div>
    </div>
  );
}

function makeTempId() {
  return `temp-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
}
