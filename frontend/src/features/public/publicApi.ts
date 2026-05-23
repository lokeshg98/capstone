import { api } from '@/lib/api';

export interface PlatformInfo {
  title:      string;
  tagline:    string;
  summary:    string;
  features:   { title: string; description: string; icon: string }[];
}

export interface PublicFaqEntry {
  question: string;
  answer:   string;
}

export interface PublicAskResponse {
  answer: string;
  steps:  string[];
}

export async function fetchPlatformInfo(): Promise<PlatformInfo> {
  const res = await api.get<{ ok: boolean; data: PlatformInfo }>('/public/platform');
  return res.data.data;
}

export async function fetchPublicFaq(limit = 8): Promise<PublicFaqEntry[]> {
  const res = await api.get<{ ok: boolean; data: PublicFaqEntry[] }>(`/public/faq?limit=${limit}`);
  return res.data.data;
}

export async function subscribeNewsletter(email: string): Promise<void> {
  await api.post('/public/newsletter', { email });
}

export async function askPublicBot(question: string): Promise<PublicAskResponse> {
  const res = await api.post<{ ok: boolean; data: PublicAskResponse }>('/public/agent/ask', { question });
  return res.data.data;
}
