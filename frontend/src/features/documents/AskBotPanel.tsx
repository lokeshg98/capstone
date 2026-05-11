import { useState, useRef } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Bot, Send, FileText, Upload, ChevronDown, ChevronUp } from 'lucide-react';
import { askBot, fetchDocuments, ingestDocument, type AskResponse } from './documentApi';
import { uploadAttachment } from '@/features/messages/attachmentApi';
import { cn } from '@/lib/utils';

interface Props {
  workspaceId: string;
}

interface QaPair {
  question:    string;
  answer:      string;
  sourceChunks: number;
}

/**
 * Full-page Ask Bot experience shown when the user clicks "Ask Bot" in the sidebar.
 * Features:
 * - Chat-style Q&A with the RAG bot
 * - FAQ document list with upload-and-ingest capability
 */
export default function AskBotPanel({ workspaceId }: Props) {
  const [question, setQuestion] = useState('');
  const [history,  setHistory]  = useState<QaPair[]>([]);
  const [loading,  setLoading]  = useState(false);
  const [showDocs, setShowDocs] = useState(false);
  const inputRef = useRef<HTMLTextAreaElement>(null);

  const { data: docs = [], refetch: refetchDocs } = useQuery({
    queryKey: ['documents', workspaceId],
    queryFn:  () => fetchDocuments(workspaceId),
  });

  const handleAsk = async () => {
    const q = question.trim();
    if (!q || loading) return;
    setQuestion('');
    setLoading(true);
    try {
      const res: AskResponse = await askBot(workspaceId, q);
      setHistory((h) => [...h, { question: q, answer: res.answer, sourceChunks: res.sourceChunks }]);
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { error?: string } } })?.response?.data?.error
        ?? 'Bot is unavailable. Please try again.';
      setHistory((h) => [...h, { question: q, answer: `⚠️ ${msg}`, sourceChunks: 0 }]);
    } finally {
      setLoading(false);
      inputRef.current?.focus();
    }
  };

  return (
    <div className="flex-1 flex flex-col bg-white overflow-hidden">
      {/* Header */}
      <div className="shrink-0 h-14 px-5 flex items-center gap-3 border-b border-gray-200">
        <div className="h-8 w-8 rounded-lg bg-brand-600 flex items-center justify-center">
          <Bot className="h-4 w-4 text-white" />
        </div>
        <div>
          <p className="font-semibold text-gray-900 text-sm leading-tight">Community Bot</p>
          <p className="text-xs text-gray-400">Answers from your FAQ documents</p>
        </div>
      </div>

      {/* Q&A history */}
      <div className="flex-1 overflow-y-auto px-5 py-4 space-y-5">
        {history.length === 0 && (
          <div className="text-center text-gray-400 pt-12">
            <Bot className="mx-auto h-12 w-12 mb-3 opacity-20" />
            <p className="text-sm font-medium">Ask anything about your FAQ documents</p>
            <p className="text-xs mt-1">Type a question below. The bot uses only your uploaded PDFs and DOCX files.</p>
            <p className="text-xs mt-3 text-brand-500">
              Tip: mention <code className="bg-gray-100 px-1 rounded">@bot</code> in any channel message to get a reply there too.
            </p>
          </div>
        )}

        {history.map((pair, i) => (
          <div key={i} className="space-y-2">
            {/* Question bubble */}
            <div className="flex justify-end">
              <div className="max-w-[75%] rounded-2xl rounded-tr-sm bg-brand-600 text-white px-4 py-2.5 text-sm">
                {pair.question}
              </div>
            </div>
            {/* Answer bubble */}
            <div className="flex items-start gap-2.5">
              <div className="h-7 w-7 rounded-full bg-brand-100 flex items-center justify-center shrink-0 mt-0.5">
                <Bot className="h-4 w-4 text-brand-600" />
              </div>
              <div className="max-w-[80%] rounded-2xl rounded-tl-sm bg-gray-100 px-4 py-2.5 text-sm text-gray-800 whitespace-pre-wrap">
                {pair.answer}
                {pair.sourceChunks > 0 && (
                  <p className="mt-1.5 text-xs text-gray-400">
                    Based on {pair.sourceChunks} document excerpt{pair.sourceChunks !== 1 ? 's' : ''}
                  </p>
                )}
              </div>
            </div>
          </div>
        ))}

        {loading && (
          <div className="flex items-start gap-2.5">
            <div className="h-7 w-7 rounded-full bg-brand-100 flex items-center justify-center shrink-0">
              <Bot className="h-4 w-4 text-brand-600" />
            </div>
            <div className="rounded-2xl rounded-tl-sm bg-gray-100 px-4 py-3">
              <div className="flex gap-1 items-center">
                <span className="h-1.5 w-1.5 rounded-full bg-gray-400 animate-bounce [animation-delay:0ms]" />
                <span className="h-1.5 w-1.5 rounded-full bg-gray-400 animate-bounce [animation-delay:150ms]" />
                <span className="h-1.5 w-1.5 rounded-full bg-gray-400 animate-bounce [animation-delay:300ms]" />
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Input */}
      <div className="shrink-0 px-4 pb-4 pt-2">
        <div className={cn(
          'flex items-end gap-2 rounded-xl border bg-white px-3 py-2 transition-shadow',
          'focus-within:ring-2 focus-within:ring-brand-500 focus-within:border-transparent border-gray-300',
        )}>
          <textarea
            ref={inputRef}
            rows={1}
            value={question}
            onChange={(e) => {
              setQuestion(e.target.value);
              const el = e.target;
              el.style.height = 'auto';
              el.style.height = Math.min(el.scrollHeight, 120) + 'px';
            }}
            onKeyDown={(e) => {
              if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); handleAsk(); }
            }}
            placeholder="Ask a question about your FAQ docs…"
            className="flex-1 resize-none outline-none text-sm text-gray-900 placeholder-gray-400 bg-transparent max-h-28"
            disabled={loading}
          />
          <button
            onClick={handleAsk}
            disabled={!question.trim() || loading}
            className={cn(
              'h-8 w-8 rounded-lg flex items-center justify-center shrink-0 transition-colors',
              question.trim() && !loading
                ? 'bg-brand-600 text-white hover:bg-brand-700'
                : 'bg-gray-100 text-gray-400 cursor-not-allowed',
            )}
          >
            <Send className="h-4 w-4" />
          </button>
        </div>
      </div>

      {/* FAQ Documents section */}
      <div className="shrink-0 border-t border-gray-200">
        <button
          onClick={() => setShowDocs((s) => !s)}
          className="w-full flex items-center justify-between px-5 py-3 text-sm text-gray-600 hover:bg-gray-50 transition-colors"
        >
          <span className="flex items-center gap-2 font-medium">
            <FileText className="h-4 w-4" />
            FAQ Documents ({docs.length})
          </span>
          {showDocs ? <ChevronDown className="h-4 w-4" /> : <ChevronUp className="h-4 w-4" />}
        </button>

        {showDocs && (
          <div className="px-5 pb-4 space-y-2 max-h-64 overflow-y-auto">
            <UploadAndIngest workspaceId={workspaceId} onIngested={refetchDocs} />
            {docs.length === 0 && (
              <p className="text-xs text-gray-400 py-2">No documents yet. Upload a PDF or DOCX to get started.</p>
            )}
            {docs.map((doc) => (
              <div key={doc.id} className="flex items-center gap-2 py-1.5 text-sm text-gray-700">
                <FileText className="h-3.5 w-3.5 text-gray-400 shrink-0" />
                <span className="flex-1 truncate">{doc.title}</span>
                <span className="text-xs text-gray-400 shrink-0">{doc.chunkCount} chunks</span>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

// ── Upload & Ingest sub-component ──────────────────────────────────────────────

function UploadAndIngest({
  workspaceId,
  onIngested,
}: {
  workspaceId: string;
  onIngested: () => void;
}) {
  const [status, setStatus] = useState<'idle' | 'uploading' | 'ingesting' | 'done' | 'error'>('idle');
  const [msg,    setMsg]    = useState('');
  const inputRef = useRef<HTMLInputElement>(null);

  const handle = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    e.target.value = '';
    setStatus('uploading');
    setMsg('');
    try {
      const att = await uploadAttachment(file);
      setStatus('ingesting');
      await ingestDocument(workspaceId, att.id);
      setStatus('done');
      setMsg(`"${file.name}" ingested successfully.`);
      onIngested();
    } catch (err: unknown) {
      setStatus('error');
      setMsg(
        (err as { response?: { data?: { error?: string } } })?.response?.data?.error
          ?? 'Ingestion failed.',
      );
    }
  };

  const label =
    status === 'uploading'  ? 'Uploading…' :
    status === 'ingesting'  ? 'Embedding…' :
    'Upload FAQ document';

  return (
    <div>
      <input ref={inputRef} type="file"
        accept=".pdf,.docx,application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        className="hidden" onChange={handle} />
      <button
        onClick={() => { setStatus('idle'); setMsg(''); inputRef.current?.click(); }}
        disabled={status === 'uploading' || status === 'ingesting'}
        className={cn(
          'flex items-center gap-1.5 text-xs px-3 py-1.5 rounded-lg border transition-colors',
          status === 'uploading' || status === 'ingesting'
            ? 'border-gray-200 text-gray-400 cursor-not-allowed'
            : 'border-brand-300 text-brand-600 hover:bg-brand-50',
        )}
      >
        <Upload className="h-3 w-3" />
        {label}
      </button>
      {msg && (
        <p className={cn('mt-1 text-xs', status === 'error' ? 'text-red-500' : 'text-green-600')}>
          {msg}
        </p>
      )}
    </div>
  );
}
