import { api } from '@/lib/api';

export interface AttachmentResponse {
  id:        string;
  filename:  string;
  mimeType:  string;
  kind:      'PDF' | 'DOCX';
  sizeBytes: number;
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
 * For PDFs this serves Content-Disposition: inline (browser renders).
 * For DOCX this serves Content-Disposition: attachment (browser downloads).
 */
export function attachmentContentUrl(attachmentId: string): string {
  return `/api/attachments/${attachmentId}/content`;
}
