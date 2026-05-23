import { api } from '@/lib/api';

export interface EmojiSearchResult {
  unicode:   string;
  name:      string;
  shortname: string;
  category:  string;
  imageUrl:  string;
}

export async function searchEmojis(query: string, limit = 24): Promise<EmojiSearchResult[]> {
  const res = await api.get<{ ok: boolean; data: EmojiSearchResult[] }>(
    '/emoji/search',
    { params: { q: query, limit } },
  );
  return res.data.data;
}
