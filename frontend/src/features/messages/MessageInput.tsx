import { useState, useRef, type KeyboardEvent } from 'react';
import { Send } from 'lucide-react';
import { useWebSocket } from '@/context/WebSocketContext';
import { cn } from '@/lib/utils';
import FileUploadButton from './FileUploadButton';
import { type AttachmentResponse } from './attachmentApi';

interface Props {
  channelId:   string;
  channelName: string;
}

/**
 * Sends messages via WebSocket (primary) with automatic fallback to HTTP.
 * Supports optional file attachment: user picks a PDF/DOCX which is uploaded
 * immediately, then the returned attachment ID is bundled with the message send.
 */
export default function MessageInput({ channelId, channelName }: Props) {
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
    try {
      const payload = {
        body:         trimmed || (pendingAttachment?.filename ?? ''),
        threadRootId: null,
        attachmentId: pendingAttachment?.id ?? null,
      };

      if (connected) {
        publish(`/app/channels/${channelId}/send`, payload);
      } else {
        const { api } = await import('@/lib/api');
        await api.post(`/channels/${channelId}/messages`, payload);
      }

      setBody('');
      setPendingAttachment(null);
      if (textareaRef.current) textareaRef.current.style.height = 'auto';
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
    <div className="shrink-0 px-4 pb-4 pt-2">
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
            onClick={send}
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
    </div>
  );
}
