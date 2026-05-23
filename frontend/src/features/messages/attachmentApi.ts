import { api } from '@/lib/api';

export type AttachmentKind = 'PDF' | 'DOCX' | 'JPEG' | 'TXT' | 'MD';

export interface AttachmentResponse {
  id:         string;
  filename:   string;
  mimeType:   string;
  kind:       AttachmentKind;
  sizeBytes:  number;
  scanStatus: 'PENDING' | 'CLEAN' | 'INFECTED' | 'ERROR';
}

export interface AttachmentLimits {
  maxSizeBytes:      number;
  allowedExtensions: string[];
}

/** Default 10 MB — overridden when /attachments/limits loads. */
export const DEFAULT_MAX_ATTACHMENT_BYTES = 10 * 1024 * 1024;

export const ACCEPTED_FILE_TYPES =
  '.jpg,.jpeg,.pdf,.docx,.md,.txt,image/jpeg,application/pdf,' +
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document,text/plain,text/markdown';

export async function fetchAttachmentLimits(): Promise<AttachmentLimits> {
  const res = await api.get<{ ok: boolean; data: AttachmentLimits }>('/attachments/limits');
  return res.data.data;
}

export async function uploadAttachment(file: File): Promise<AttachmentResponse> {
  const form = new FormData();
  form.append('file', file);
  const res = await api.post<{ ok: boolean; data: AttachmentResponse }>(
    '/attachments',
    form,
    { headers: { 'Content-Type': 'multipart/form-data' } },
  );
  return res.data.data;
}

/**
 * Returns the backend proxy URL that streams the file content.
 * Images and PDFs are served inline; other types download by default.
 */
export function attachmentContentUrl(attachmentId: string): string {
  return `/api/attachments/${attachmentId}/content`;
}

export function formatAttachmentBytes(bytes: number): string {
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
}

export function validateAttachmentFile(
  file: File,
  maxBytes: number = DEFAULT_MAX_ATTACHMENT_BYTES,
): string | null {
  if (file.size > maxBytes) {
    return `File exceeds the ${formatAttachmentBytes(maxBytes)} limit`;
  }
  const lower = file.name.toLowerCase();
  const allowed = ['.jpg', '.jpeg', '.pdf', '.docx', '.md', '.txt'];
  if (!allowed.some((ext) => lower.endsWith(ext))) {
    return 'Only JPG, PDF, DOCX, .md, and .txt files are allowed';
  }
  return null;
}
