/** Shared brand mark: a minimal "ranked list" glyph — three bars in a rounded squircle. */

export function LogoMark({ size = 32, className = '' }: { size?: number; className?: string }) {
  const radius = Math.round(size * 0.28);
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 32 32"
      className={className}
      role="img"
      aria-label="FeedRank logo"
    >
      <defs>
        <linearGradient id="fr-mark" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0%" stopColor="#0a84ff" />
          <stop offset="100%" stopColor="#0060d0" />
        </linearGradient>
      </defs>
      <rect width="32" height="32" rx="8" fill="url(#fr-mark)" />
      {/* three ranked bars, staggered like a leaderboard */}
      <rect x="8" y="8" width="16" height="3" rx="1.5" fill="white" opacity="0.95" />
      <rect x="8" y="14.5" width="11" height="3" rx="1.5" fill="white" opacity="0.75" />
      <rect x="8" y="21" width="13.5" height="3" rx="1.5" fill="white" opacity="0.85" />
    </svg>
  );
}

export function Logo() {
  return (
    <div className="flex min-w-0 items-center gap-2.5">
      <LogoMark size={30} className="shrink-0" />
      <span className="hidden truncate text-[17px] font-semibold tracking-tight min-[400px]:inline">
        FeedRank
      </span>
    </div>
  );
}
