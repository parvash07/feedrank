import { useState } from 'react';
import type { FeedItem } from './api';
import { logInteraction } from './api';
import { useDwell } from './useDwellFix';

function timeAgo(iso: string): string {
  const then = new Date(iso).getTime();
  if (Number.isNaN(then)) return '';
  const mins = Math.max(0, Math.floor((Date.now() - then) / 60000));
  if (mins < 1) return 'just now';
  if (mins < 60) return `${mins}m ago`;
  const hours = Math.floor(mins / 60);
  if (hours < 24) return `${hours}h ago`;
  const days = Math.floor(hours / 24);
  if (days < 30) return `${days}d ago`;
  return new Date(iso).toLocaleDateString();
}

export function FeedCard({ item, onVoted }: { item: FeedItem; onVoted: (id: number, kind: string) => void }) {
  const [vote, setVote] = useState<'upvote' | 'skip' | null>(null);
  const ref = useDwell(item.id, true);

  const act = async (kind: 'click' | 'upvote' | 'skip') => {
    try {
      await logInteraction(item.id, kind);
    } catch {
      /* offline-safe: vote UI still updates */
    }
    if (kind !== 'click') setVote(kind);
    onVoted(item.id, kind);
  };

  return (
    <article ref={ref} className="apple-card apple-card-hover rise p-5 sm:p-6">
      <div className="flex items-start justify-between gap-4">
        <div className="min-w-0 flex-1">
          <a
            href={item.url ?? '#'}
            target="_blank"
            rel="noreferrer"
            onClick={() => act('click')}
            className="block break-words text-balance text-[17px] font-semibold leading-snug tracking-tight text-[#1d1d1f] hover:text-[#0071e3] dark:text-white dark:hover:text-[#0a84ff]"
          >
            {item.title}
          </a>
          <p className="mt-1.5 text-[13px] text-[#6e6e73] dark:text-white/50">
            {item.author ? <span className="font-medium">{item.author}</span> : <span>Hacker News</span>}
            <span aria-hidden> · </span>
            <span>▲ {item.score}</span>
            <span aria-hidden> · </span>
            <span>{timeAgo(item.createdAt)}</span>
          </p>
        </div>
        {item.exploration && (
          <span
            title="Injected by epsilon-greedy exploration to keep your feed from collapsing into an echo chamber"
            className="mt-0.5 shrink-0 rounded-full bg-[#0071e3]/10 px-2.5 py-1 text-[11px] font-semibold text-[#0071e3] dark:bg-[#0a84ff]/15 dark:text-[#0a84ff]"
          >
            ✦ For you to explore
          </span>
        )}
      </div>

      {item.tags.length > 0 && (
        <div className="mt-3 flex flex-wrap gap-1.5">
          {item.tags.map((t) => (
            <span key={t} className="apple-pill">
              {t}
            </span>
          ))}
        </div>
      )}

      <div className="mt-4 flex flex-wrap items-center gap-2 border-t border-black/[0.06] pt-4 dark:border-white/[0.08]">
        <button
          onClick={() => act('upvote')}
          aria-pressed={vote === 'upvote'}
          className={
            vote === 'upvote'
              ? 'rounded-full bg-[#0071e3] px-4 py-1.5 text-[14px] font-medium text-white transition hover:bg-[#0077ed] active:scale-[0.98]'
              : 'apple-btn-quiet'
          }
        >
          {vote === 'upvote' ? '▲ Upvoted' : '▲ Upvote'}
        </button>
        <button
          onClick={() => act('skip')}
          aria-pressed={vote === 'skip'}
          className={
            vote === 'skip'
              ? 'rounded-full bg-black/[0.12] px-4 py-1.5 text-[14px] font-medium transition active:scale-[0.98] dark:bg-white/[0.2] dark:text-white'
              : 'apple-btn-quiet !text-[#6e6e73] dark:!text-white/50'
          }
        >
          {vote === 'skip' ? 'Skipped' : 'Not interested'}
        </button>
        <div className="ml-auto hidden items-center gap-1 text-[12px] tabular-nums text-[#86868b] sm:flex dark:text-white/35">
          <span className="inline-block h-1.5 w-1.5 rounded-full bg-emerald-500" aria-hidden />
          {item.rankScore.toFixed(2)}
        </div>
      </div>
    </article>
  );
}
