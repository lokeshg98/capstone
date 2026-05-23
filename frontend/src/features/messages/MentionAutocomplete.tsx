import { Bot } from 'lucide-react';
import { type MentionSuggestion } from './memberApi';
import { cn } from '@/lib/utils';

interface Props {
  suggestions:   MentionSuggestion[];
  selectedIndex: number;
  onSelect:      (s: MentionSuggestion) => void;
}

export default function MentionAutocomplete({ suggestions, selectedIndex, onSelect }: Props) {
  return (
    <div className="absolute bottom-full left-0 mb-1 w-64 max-h-48 overflow-y-auto rounded-lg border border-gray-200 bg-white shadow-lg z-50">
      {suggestions.map((s, i) => (
        <button
          key={s.userId}
          type="button"
          onMouseDown={(e) => {
            e.preventDefault();
            onSelect(s);
          }}
          className={cn(
            'w-full flex items-center gap-2 px-3 py-2 text-left text-sm transition-colors',
            i === selectedIndex
              ? 'bg-brand-50 text-brand-900'
              : 'text-gray-700 hover:bg-gray-50',
          )}
        >
          <div className="h-6 w-6 rounded-full bg-brand-600 flex items-center justify-center shrink-0">
            {s.avatarUrl ? (
              <img src={s.avatarUrl} alt="" className="h-6 w-6 rounded-full object-cover" />
            ) : (
              <span className="text-white text-[10px] font-semibold">
                {(s.displayName ?? '?').slice(0, 2).toUpperCase()}
              </span>
            )}
          </div>
          <span className="flex-1 truncate">{s.displayName}</span>
          {s.isBot && (
            <span className="inline-flex items-center gap-0.5 text-[10px] text-violet-600 bg-violet-50 px-1.5 py-0.5 rounded-full">
              <Bot className="h-2.5 w-2.5" />
              bot
            </span>
          )}
        </button>
      ))}
    </div>
  );
}
