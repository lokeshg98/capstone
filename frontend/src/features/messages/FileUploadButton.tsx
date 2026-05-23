import { useRef, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Paperclip, X, FileText, File, ImageIcon } from 'lucide-react';
import {
  uploadAttachment,
  fetchAttachmentLimits,
  validateAttachmentFile,
  formatAttachmentBytes,
  ACCEPTED_FILE_TYPES,
  DEFAULT_MAX_ATTACHMENT_BYTES,
  type AttachmentResponse,
} from './attachmentApi';
import { cn } from '@/lib/utils';

interface Props {
  pending:    AttachmentResponse | null;
  onAttached: (attachment: AttachmentResponse) => void;
  onClear:    () => void;
}

function kindIcon(kind: AttachmentResponse['kind']) {
  switch (kind) {
    case 'PDF':  return <FileText className="h-3.5 w-3.5 shrink-0" />;
    case 'JPEG': return <ImageIcon className="h-3.5 w-3.5 shrink-0" />;
    default:     return <File className="h-3.5 w-3.5 shrink-0" />;
  }
}

/**
 * Paperclip upload control. Validates type/size client-side, uploads to the backend,
 * and runs a virus scan on the stored object before the attachment can be sent.
 */
export default function FileUploadButton({ pending, onAttached, onClear }: Props) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const { data: limits } = useQuery({
    queryKey: ['attachment-limits'],
    queryFn:  fetchAttachmentLimits,
    staleTime: 60_000,
  });

  const maxBytes = limits?.maxSizeBytes ?? DEFAULT_MAX_ATTACHMENT_BYTES;

  const handleChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    e.target.value = '';
    setError(null);

    const validationError = validateAttachmentFile(file, maxBytes);
    if (validationError) {
      setError(validationError);
      return;
    }

    setUploading(true);
    try {
      const result = await uploadAttachment(file);
      if (result.scanStatus !== 'CLEAN') {
        setError('File could not be verified by the virus scanner');
        return;
      }
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
        {kindIcon(pending.kind)}
        <span className="max-w-[140px] truncate font-medium">{pending.filename}</span>
        <span className="text-blue-400">({formatAttachmentBytes(pending.sizeBytes)})</span>
        {pending.scanStatus === 'CLEAN' && (
          <span className="text-green-600" title="Virus scan passed">✓</span>
        )}
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

  const maxLabel = formatAttachmentBytes(maxBytes);

  return (
    <>
      <input
        ref={inputRef}
        type="file"
        accept={ACCEPTED_FILE_TYPES}
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
          title={`Attach JPG, PDF, DOCX, .md, or .txt (max ${maxLabel})`}
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
        {uploading && (
          <p className="mt-0.5 px-1 text-[10px] text-gray-500">Uploading &amp; scanning…</p>
        )}
        {error && (
          <p className="mt-0.5 px-1 text-xs text-red-600 max-w-[220px]">{error}</p>
        )}
      </div>
    </>
  );
}
