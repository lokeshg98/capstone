import { api } from '@/lib/api';

export interface FaqDocumentResponse {
  id:          string;
  workspaceId: string;
  title:       string;
  chunkCount:  number;
  ingestedAt:  string;
}

/** Submits an already-uploaded attachment for FAQ ingestion (chunking + embedding). */
export async function ingestDocument(
  workspaceId: string,
  attachmentId: string,
): Promise<FaqDocumentResponse> {
  const res = await api.post<{ ok: boolean; data: FaqDocumentResponse }>(
    `/workspaces/${workspaceId}/documents`,
    { attachmentId },
  );
  return res.data.data;
}

export async function fetchDocuments(workspaceId: string): Promise<FaqDocumentResponse[]> {
  const res = await api.get<{ ok: boolean; data: FaqDocumentResponse[] }>(
    `/workspaces/${workspaceId}/documents`,
  );
  return res.data.data;
}

export interface AskResponse {
  answer:       string;
  sourceChunks: number;
}

export async function askBot(workspaceId: string, question: string): Promise<AskResponse> {
  const res = await api.post<{ ok: boolean; data: AskResponse }>(
    `/workspaces/${workspaceId}/ask`,
    { question },
  );
  return res.data.data;
}
