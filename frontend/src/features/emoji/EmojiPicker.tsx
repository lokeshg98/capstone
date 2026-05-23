import { useEffect, useRef, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Search } from 'lucide-react';
import { searchEmojis, type EmojiSearchResult } from './emojiApi';
import { cn } from '@/lib/utils';

interface Props {
  onSelect: (emoji: EmojiSearchResult) => void;
  onClose:  () => void;
  className?: string;
}

export default function EmojiPicker({ onSelect, onClose, className }: Props) {
  const [query, setQuery] = useState('');
  const [debounced, setDebounced] = useState('');
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const t = setTimeout(() => setDebounced(query), 200);
    return () => clearTimeout(t);
  }, [query]);

  const { data: results = [], isFetching } = useQuery({
    queryKey: ['emoji-search', debounced],
    queryFn:  () => searchEmojis(debounced),
    staleTime: 60_000,
  });

  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) {
        onClose();
      }
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, [onClose]);

  return (
    <div
      ref={ref}
      className={cn(
        'absolute bottom-full mb-2 left-0 z-50 w-80 rounded-xl border border-gray-200',
        'bg-white shadow-xl flex flex-col max-h-72',
        className,
      )}
    >
      <div className="p-2 border-b border-gray-100">
        <div className="flex items-center gap-2 rounded-lg border border-gray-200 px-2 py-1.5">
          <Search className="h-4 w-4 text-gray-400 shrink-0" />
          <input
            type="text"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Search JoyPixels emoji…"
            className="flex-1 text-sm outline-none placeholder-gray-400"
            autoFocus
          />
        </div>
        <p className="text-[10px] text-gray-400 mt-1 px-1">
          Powered by JoyPixels · try “celebration”, “thumbs up”, or :heart:
        </p>
      </div>

      <div className="flex-1 overflow-y-auto p-2">
        {isFetching && results.length === 0 && (
          <p className="text-xs text-gray-400 text-center py-4">Searching…</p>
        )}
        {!isFetching && results.length === 0 && (
          <p className="text-xs text-gray-400 text-center py-4">
            {debounced.trim() ? 'No emoji match that search.' : 'Type to search emoji.'}
          </p>
        )}
        <div className="grid grid-cols-8 gap-1">
          {results.map((emoji) => (
            <button
              key={emoji.unicode + emoji.shortname}
              type="button"
              title={emoji.name}
              onClick={() => {
                onSelect(emoji);
                onClose();
              }}
              className="h-9 w-9 rounded-lg hover:bg-gray-100 flex items-center justify-center"
            >
              <img
                src={emoji.imageUrl}
                alt={emoji.name}
                className="h-6 w-6"
                loading="lazy"
                onError={(e) => {
                  const img = e.currentTarget;
                  img.style.display = 'none';
                  const fallback = img.nextElementSibling;
                  if (fallback instanceof HTMLElement) fallback.style.display = 'block';
                }}
              />
              <span className="text-lg leading-none hidden" aria-hidden>{emoji.unicode}</span>
            </button>
          ))}
        </div>
      </div>
    </div>
  );
}
