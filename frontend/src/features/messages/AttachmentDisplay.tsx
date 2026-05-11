import { FileText, File, ExternalLink, Download } from 'lucide-react';
import { attachmentContentUrl, type AttachmentResponse } from './attachmentApi';

interface Props {
  attachment: AttachmentResponse;
}

function formatBytes(bytes: number): string {
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
}

/**
 * Compact card shown inside a message bubble when the message has an attachment.
 * PDFs open in a new browser tab (served inline by the backend).
 * DOCX files trigger a download.
 */
export default function AttachmentDisplay({ attachment }: Props) {
  const url   = attachmentContentUrl(attachment.id);
  const isPdf = attachment.kind === 'PDF';

  return (
    <div className="mt-1.5 inline-flex items-center gap-2.5 rounded-lg border border-gray-200 bg-gray-50 px-3 py-2 text-sm max-w-xs">
      {isPdf
        ? <FileText className="h-4 w-4 shrink-0 text-red-500" />
        : <File     className="h-4 w-4 shrink-0 text-blue-500" />
      }

      <div className="flex-1 min-w-0">
        <p className="truncate font-medium text-gray-800 leading-tight">{attachment.filename}</p>
        <p className="text-xs text-gray-400">{formatBytes(attachment.sizeBytes)}</p>
      </div>

      {isPdf ? (
        <a
          href={url}
          target="_blank"
          rel="noreferrer"
          className="shrink-0 text-brand-600 hover:text-brand-700 transition-colors"
          title="Open PDF"
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
