import { api } from '@/lib/api';

export interface EmojiSearchResult {
  unicode:   string;
  name:      string;
  shortname: string;
  category:  string;
  imageUrl:  string;
}

const EMOJI_JSON_URL = 'https://cdn.jsdelivr.net/npm/emoji-toolkit@9.0.0/emoji.json';
const CDN_BASE = 'https://cdn.jsdelivr.net/joypixels/assets/9.0/png/unicode/64';

interface CatalogEntry {
  name:      string;
  shortname: string;
  keywords:  string[];
  category:  string;
  fq:        string;
  unicode:   string;
}

let catalogPromise: Promise<CatalogEntry[]> | null = null;

function codePointsToUnicode(fullyQualified: string): string {
  let out = '';
  for (const part of fullyQualified.split('-')) {
    out += String.fromCodePoint(parseInt(part, 16));
  }
  return out;
}

function unicodeToFullyQualified(unicode: string): string {
  const parts: string[] = [];
  for (let i = 0; i < unicode.length; ) {
    const cp = unicode.codePointAt(i)!;
    parts.push(cp.toString(16));
    i += cp > 0xffff ? 2 : 1;
  }
  return parts.join('-');
}

function pngUrl(fullyQualified: string): string {
  return `${CDN_BASE}/${fullyQualified}.png`;
}

/** PNG URL for a stored reaction/message emoji character sequence. */
export function emojiImageUrl(unicode: string): string {
  return pngUrl(unicodeToFullyQualified(unicode));
}

function withFixedImageUrl(result: EmojiSearchResult): EmojiSearchResult {
  return { ...result, imageUrl: emojiImageUrl(result.unicode) };
}

async function loadCatalog(): Promise<CatalogEntry[]> {
  if (!catalogPromise) {
    catalogPromise = fetch(EMOJI_JSON_URL)
      .then((r) => r.json())
      .then((root: Record<string, {
        name?: string;
        shortname?: string;
        keywords?: string[];
        category?: string;
        display?: number;
        code_points?: { fully_qualified?: string };
      }>) => {
        const entries: CatalogEntry[] = [];
        for (const [key, node] of Object.entries(root)) {
          if (node.display !== 1) continue;
          const fq = node.code_points?.fully_qualified ?? key;
          entries.push({
            name:      node.name ?? '',
            shortname: node.shortname ?? '',
            keywords:  node.keywords ?? [],
            category:  node.category ?? '',
            fq,
            unicode:   codePointsToUnicode(fq),
          });
        }
        return entries;
      })
      .catch(() => []);
  }
  return catalogPromise;
}

function scoreEntry(entry: CatalogEntry, q: string): number {
  let s = 0;
  if (entry.name.includes(q)) s += 10;
  if (entry.shortname.toLowerCase().includes(q)) s += 8;
  for (const kw of entry.keywords) {
    if (kw.includes(q)) s += 5;
  }
  if (entry.name.startsWith(q)) s += 4;
  return s;
}

function toResult(entry: CatalogEntry): EmojiSearchResult {
  return {
    unicode:   entry.unicode,
    name:      entry.name,
    shortname: entry.shortname,
    category:  entry.category,
    imageUrl:  pngUrl(entry.fq),
  };
}

async function clientSearch(query: string, limit: number): Promise<EmojiSearchResult[]> {
  const catalog = await loadCatalog();
  if (catalog.length === 0) return [];

  const q = query.trim().toLowerCase();
  if (!q) {
    const picks = [':thumbsup:', ':heart:', ':fire:', ':joy:', ':smile:', ':tada:'];
    return picks
      .map((s) => catalog.find((e) => e.shortname === s))
      .filter((e): e is CatalogEntry => e != null)
      .map(toResult);
  }

  if (q.startsWith(':') && q.endsWith(':')) {
    const exact = catalog.find((e) => e.shortname.toLowerCase() === q);
    if (exact) return [toResult(exact)];
  }

  return catalog
    .map((e) => ({ entry: e, score: scoreEntry(e, q) }))
    .filter((s) => s.score > 0)
    .sort((a, b) => b.score - a.score)
    .slice(0, limit)
    .map((s) => toResult(s.entry));
}

export async function searchEmojis(query: string, limit = 24): Promise<EmojiSearchResult[]> {
  const client = await clientSearch(query, limit);
  if (client.length > 0) {
    return client.map(withFixedImageUrl);
  }

  try {
    const res = await api.get<{ ok: boolean; data: EmojiSearchResult[] }>(
      '/emoji/search',
      { params: { q: query, limit } },
    );
    return res.data.data.map(withFixedImageUrl);
  } catch {
    return [];
  }
}
