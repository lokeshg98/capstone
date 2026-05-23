import { api } from '@/lib/api';

export interface LlmUsageSummary {
  userInputTokens:   number;
  userOutputTokens:  number;
  projectInputTokens:  number;
  projectOutputTokens: number;
  userTotalCostUsd:    number;
  projectTotalCostUsd: number;
}

export async function fetchMyLlmUsage(): Promise<LlmUsageSummary> {
  const res = await api.get<{ ok: boolean; data: LlmUsageSummary }>('/me/llm-usage');
  return res.data.data;
}
