import { FileText, File, ExternalLink, Download } from 'lucide-react';
import {
  attachmentContentUrl,
  formatAttachmentBytes,
  type AttachmentResponse,
} from './attachmentApi';

interface Props {
  attachment: Pick<AttachmentResponse, 'id' | 'filename' | 'kind' | 'sizeBytes'>;
}

/**
 * Renders an attachment inside a message bubble.
 * JPEG images preview inline; PDFs/text open in a new tab; DOCX downloads.
 */
export default function AttachmentDisplay({ attachment }: Props) {
  const url = attachmentContentUrl(attachment.id);

  if (attachment.kind === 'JPEG') {
    return (
      <div className="mt-1.5 max-w-sm">
        <a href={url} target="_blank" rel="noreferrer" className="block group">
          <img
            src={url}
            alt={attachment.filename}
            className="rounded-lg border border-gray-200 max-h-64 object-contain bg-gray-50"
            loading="lazy"
          />
          <p className="mt-1 text-xs text-gray-500 group-hover:text-brand-600 truncate">
            {attachment.filename} · {formatAttachmentBytes(attachment.sizeBytes)}
          </p>
        </a>
      </div>
    );
  }

  const isInline = attachment.kind === 'PDF' || attachment.kind === 'TXT' || attachment.kind === 'MD';

  const icon = (() => {
    switch (attachment.kind) {
      case 'PDF': return <FileText className="h-4 w-4 shrink-0 text-red-500" />;
      case 'TXT':
      case 'MD':  return <FileText className="h-4 w-4 shrink-0 text-gray-500" />;
      default:    return <File className="h-4 w-4 shrink-0 text-blue-500" />;
    }
  })();

  return (
    <div className="mt-1.5 inline-flex items-center gap-2.5 rounded-lg border border-gray-200 bg-gray-50 px-3 py-2 text-sm max-w-xs">
      {icon}

      <div className="flex-1 min-w-0">
        <p className="truncate font-medium text-gray-800 leading-tight">{attachment.filename}</p>
        <p className="text-xs text-gray-400">{formatAttachmentBytes(attachment.sizeBytes)}</p>
      </div>

      {isInline ? (
        <a
          href={url}
          target="_blank"
          rel="noreferrer"
          className="shrink-0 text-brand-600 hover:text-brand-700 transition-colors"
          title="Open file"
        >
          <ExternalLink className="h-4 w-4" />
        </a>
      ) : (
        <a
          href={url}
          download={attachment.filename}
          className="shrink-0 text-brand-600 hover:text-brand-700 transition-colors"
          title="Download"
        >
          <Download className="h-4 w-4" />
        </a>
      )}
    </div>
  );
}
