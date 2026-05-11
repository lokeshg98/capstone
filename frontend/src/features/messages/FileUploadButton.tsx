import { useRef, useState } from 'react';
import { Paperclip, X, FileText, File } from 'lucide-react';
import { uploadAttachment, type AttachmentResponse } from './attachmentApi';
import { cn } from '@/lib/utils';

interface Props {
  pending:    AttachmentResponse | null;
  onAttached: (attachment: AttachmentResponse) => void;
  onClear:    () => void;
}

function formatBytes(bytes: number): string {
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
}

/**
 * Renders a paperclip button. When a file is selected it uploads immediately
 * and calls `onAttached` with the result, showing a dismissible chip.
 */
export default function FileUploadButton({ pending, onAttached, onClear }: Props) {
  const inputRef            = useRef<HTMLInputElement>(null);
  const [uploading, setUploading] = useState(false);
  const [error,     setError]     = useState<string | null>(null);

  const handleChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    // Reset so re-selecting the same file triggers onChange
    e.target.value = '';
    setError(null);
    setUploading(true);
    try {
      const result = await uploadAttachment(file);
      onAttached(result);
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { error?: string } } })?.response?.data?.error ??
        'Upload failed';
      setError(msg);
    } finally {
      setUploading(false);
    }
  };

  if (pending) {
    return (
      <div className="flex items-center gap-1.5 rounded-lg border border-blue-200 bg-blue-50 px-2.5 py-1 text-xs text-blue-700">
        {pending.kind === 'PDF'
          ? <FileText className="h-3.5 w-3.5 shrink-0" />
          : <File     className="h-3.5 w-3.5 shrink-0" />
        }
        <span className="max-w-[140px] truncate font-medium">{pending.filename}</span>
        <span className="text-blue-400">({formatBytes(pending.sizeBytes)})</span>
        <button
          type="button"
          onClick={onClear}
          className="ml-1 hover:text-red-600 transition-colors"
          title="Remove attachment"
        >
          <X className="h-3.5 w-3.5" />
        </button>
      </div>
    );
  }

  return (
    <>
      <input
        ref={inputRef}
        type="file"
        accept=".pdf,.docx,application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        className="hidden"
        onChange={handleChange}
      />
      <div className="flex flex-col items-start">
        <button
          type="button"
          onClick={() => inputRef.current?.click()}
          disabled={uploading}
          className={cn(
            'h-8 w-8 rounded-lg flex items-center justify-center transition-colors',
            uploading
              ? 'text-gray-300 cursor-not-allowed'
              : 'text-gray-400 hover:text-gray-600 hover:bg-gray-100',
          )}
          title="Attach PDF or DOCX (max 10 MB)"
        >
          {uploading ? (
            <svg className="h-4 w-4 animate-spin" viewBox="0 0 24 24" fill="none">
              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
              <path className="opacity-75" fill="currentColor"
                d="M4 12a8 8 0 018-8v4l3-3-3-3v4a8 8 0 00-8 8h4z" />
            </svg>
          ) : (
            <Paperclip className="h-4 w-4" />
          )}
        </button>
        {error && (
          <p className="mt-0.5 px-1 text-xs text-red-600 max-w-[200px] truncate">{error}</p>
        )}
      </div>
    </>
  );
}
