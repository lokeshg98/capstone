import { useState, useRef, type KeyboardEvent } from 'react';
import { Send } from 'lucide-react';
import { useWebSocket } from '@/context/WebSocketContext';
import { cn } from '@/lib/utils';
import FileUploadButton from './FileUploadButton';
import { type AttachmentResponse } from './attachmentApi';
import type { MessageResponse } from './messageApi';

interface Props {
  channelId:   string;
  channelName: string;
  addOptimisticMessage: (msg: MessageResponse) => void;
  commitMessage: (tempId: string, msg: MessageResponse) => void;
  removeOptimisticMessage: (id: string) => void;
  currentUser?: { id: string; displayName: string | null; avatarUrl: string | null } | null;
}

/**
 * Sends messages via WebSocket (primary) with automatic fallback to HTTP.
 * Supports optional file attachment: user picks a PDF/DOCX which is uploaded
 * immediately, then the returned attachment ID is bundled with the message send.
 */
export default function MessageInput({
  channelId,
  channelName,
  addOptimisticMessage,
  commitMessage,
  removeOptimisticMessage,
  currentUser,
}: Props) {
  const [body,              setBody]              = useState('');
  const [sending,           setSending]           = useState(false);
  const [pendingAttachment, setPendingAttachment] = useState<AttachmentResponse | null>(null);
  const { connected, publish } = useWebSocket();
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  const send = async () => {
    const trimmed = body.trim();
    // Require at least a body OR an attachment
    if ((!trimmed && !pendingAttachment) || sending) return;

    setSending(true);
    const optimisticId = makeTempId();
    try {
      const draftText = trimmed || (pendingAttachment?.filename ?? '');
      const now = new Date().toISOString();
      const actor = currentUser ?? {
        id: 'me',
        displayName: 'You',
        avatarUrl: null,
      };

      setBody('');
      setPendingAttachment(null);
      if (textareaRef.current) textareaRef.current.style.height = 'auto';

      try {
        addOptimisticMessage({
          id: optimisticId,
          channelId,
          workspaceId: '',
          author: {
            id: actor.id,
            displayName: actor.displayName,
            avatarUrl: actor.avatarUrl,
          },
          body: draftText,
          threadRootId: null,
          status: 'ACTIVE',
          edited: false,
          editedAt: null,
          createdAt: now,
          reactions: [],
          attachment: pendingAttachment
            ? {
                id: pendingAttachment.id,
                filename: pendingAttachment.filename,
                mimeType: pendingAttachment.mimeType,
                kind: pendingAttachment.kind,
                sizeBytes: pendingAttachment.sizeBytes,
              }
            : null,
          pending: true,
        });
      } catch (optimisticError) {
        console.error('Failed to add optimistic message', optimisticError);
      }

      const payload = {
        body:         draftText,
        threadRootId: null,
        attachmentId: pendingAttachment?.id ?? null,
      };

      const { api } = await import('@/lib/api');
      const { data } = await api.post<{ ok: boolean; data: MessageResponse }>(`/channels/${channelId}/messages`, payload);
      if (!connected) {
        commitMessage(optimisticId, { ...data.data, pending: false });
      }

    } catch (error) {
      removeOptimisticMessage(optimisticId);
      console.error('Failed to send message', error);
    } finally {
      setSending(false);
      textareaRef.current?.focus();
    }
  };

  const handleKeyDown = (e: KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      send();
    }
  };

  const handleChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    setBody(e.target.value);
    const el = e.target;
    el.style.height = 'auto';
    el.style.height = Math.min(el.scrollHeight, 144) + 'px';
  };

  const sendTyping = () => {
    if (connected) publish(`/app/channels/${channelId}/typing`, {});
  };

  const canSend = (body.trim().length > 0 || pendingAttachment !== null) && !sending;

  return (
    <form
      className="shrink-0 px-4 pb-4 pt-2"
      onSubmit={(e) => {
        e.preventDefault();
        void send();
      }}
    >
      <div className={cn(
        'rounded-xl border bg-white transition-shadow',
        'focus-within:ring-2 focus-within:ring-brand-500 focus-within:border-transparent',
        'border-gray-300',
      )}>
        {/* Attachment chip sits above the text row */}
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
          {/* Paperclip — only shown when no attachment is pending */}
          {!pendingAttachment && (
            <FileUploadButton
              pending={null}
              onAttached={setPendingAttachment}
              onClear={() => setPendingAttachment(null)}
            />
          )}

          <textarea
            ref={textareaRef}
            rows={1}
            value={body}
            onChange={handleChange}
            onKeyDown={handleKeyDown}
            onInput={sendTyping}
            placeholder={`Message #${channelName}`}
            className="flex-1 resize-none outline-none text-sm text-gray-900 placeholder-gray-400 bg-transparent max-h-36"
            disabled={sending}
          />

          <button
            type="submit"
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

      {!connected && (
        <p className="mt-1 text-xs text-amber-600">
          Reconnecting to real-time server… messages will be sent via HTTP.
        </p>
      )}
    </form>
  );
}

function makeTempId() {
  return `temp-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
}
