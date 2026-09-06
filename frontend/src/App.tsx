import { useCallback, useEffect, useRef, useState } from 'react';
import {
  getFeed,
  getPreferences,
  login,
  logout,
  register,
  triggerIngest,
  type FeedItem,
  type Preferences,
} from './api';
import { FeedCard } from './FeedCard';
import { Logo, LogoMark } from './Logo';
import { MoonIcon, SunIcon, useTheme } from './theme';

export default function App() {
  const { theme, toggle } = useTheme();
  const [authed, setAuthed] = useState(() => !!localStorage.getItem('feedrank_token'));
  const [username, setUsername] = useState(() => localStorage.getItem('feedrank_user') ?? '');
  const [formU, setFormU] = useState('');
  const [formP, setFormP] = useState('');
  const [authErr, setAuthErr] = useState('');
  const [authBusy, setAuthBusy] = useState(false);
  const [feed, setFeed] = useState<FeedItem[]>([]);
  const [mode, setMode] = useState('tag');
  const [prefs, setPrefs] = useState<Preferences | null>(null);
  const [showDebug, setShowDebug] = useState(false);
  const [status, setStatus] = useState('');
  const [loading, setLoading] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);

  // Close the profile menu on outside click / Escape. (A plain overlay div
  // doesn't work here: the frosted nav's backdrop-blur creates a containing
  // block that traps fixed-position descendants inside the nav.)
  useEffect(() => {
    if (!menuOpen) return;
    const onDown = (e: PointerEvent) => {
      if (menuRef.current && !menuRef.current.contains(e.target as Node)) setMenuOpen(false);
    };
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setMenuOpen(false);
    };
    document.addEventListener('pointerdown', onDown);
    document.addEventListener('keydown', onKey);
    return () => {
      document.removeEventListener('pointerdown', onDown);
      document.removeEventListener('keydown', onKey);
    };
  }, [menuOpen]);

  const refresh = useCallback(async () => {
    setLoading(true);
    try {
      const [f, p] = await Promise.all([getFeed(mode), getPreferences()]);
      setFeed(f);
      setPrefs(p);
      setStatus('');
    } catch (e: any) {
      setStatus(e?.message ?? 'Couldn’t load your feed. Is the backend running?');
    } finally {
      setLoading(false);
    }
  }, [mode]);

  useEffect(() => {
    if (authed) refresh();
  }, [authed, refresh]);

  const doAuth = async (kind: 'login' | 'register') => {
    setAuthErr('');
    setAuthBusy(true);
    try {
      const data = kind === 'login' ? await login(formU, formP) : await register(formU, formP);
      setUsername(data.username);
      setAuthed(true);
    } catch (e: any) {
      setAuthErr(e?.message ?? 'Something went wrong');
    } finally {
      setAuthBusy(false);
    }
  };

  /* ---------- Auth screen ---------- */
  if (!authed) {
    return (
      <div className="flex min-h-screen items-center justify-center p-6">
        <div className="rise w-full max-w-[400px] text-center">
          <LogoMark size={60} className="mx-auto drop-shadow-md" />
          <h1 className="mt-6 text-[34px] font-semibold leading-tight tracking-tight">FeedRank</h1>
          <p className="mt-2 text-[17px] leading-snug text-[#6e6e73] dark:text-white/55">
            A Hacker News feed that learns what you love. Upvote what interests you — it gets smarter.
          </p>
          <div className="apple-card mt-8 space-y-3 p-4 text-left">
            <input
              value={formU}
              onChange={(e) => setFormU(e.target.value)}
              placeholder="Username"
              autoComplete="username"
              className="apple-input"
            />
            <input
              value={formP}
              onChange={(e) => setFormP(e.target.value)}
              placeholder="Password"
              type="password"
              autoComplete={formU ? 'current-password' : 'new-password'}
              onKeyDown={(e) => e.key === 'Enter' && doAuth('login')}
              className="apple-input"
            />
            {authErr && <p className="px-1 text-[13px] text-red-500">{authErr}</p>}
            <button onClick={() => doAuth('login')} disabled={authBusy} className="apple-btn-primary w-full !py-2.5 disabled:opacity-50">
              {authBusy ? 'Signing in…' : 'Sign in'}
            </button>
            <button onClick={() => doAuth('register')} disabled={authBusy} className="w-full py-1 text-[15px] font-medium text-[#0071e3] hover:underline disabled:opacity-50 dark:text-[#0a84ff]">
              Create account
            </button>
          </div>
          <p className="mt-6 text-[12px] text-[#86868b]">Private by design · Your votes never leave your feed</p>
        </div>
      </div>
    );
  }

  /* ---------- Main app ---------- */
  const exploreCount = feed.filter((f) => f.exploration).length;

  return (
    <div className="min-h-screen">
      <nav className="apple-nav">
        <div className="mx-auto flex h-[52px] max-w-[720px] items-center justify-between gap-1.5 px-3 sm:gap-2 sm:px-4">
          <Logo />
          <div className="flex items-center gap-1.5 sm:gap-2">
            <div className="apple-segment" role="tablist" aria-label="Ranking mode">
              <button data-active={mode === 'tag'} onClick={() => setMode('tag')} title="Tag-preference ranking (Phase 3)">
                Tags
              </button>
              <button data-active={mode === 'embedding'} onClick={() => setMode('embedding')} title="Embedding-similarity ranking (Phase 4)">
                Vector
              </button>
            </div>
            <button
              onClick={toggle}
              title={theme === 'dark' ? 'Switch to light mode' : 'Switch to dark mode'}
              aria-label="Toggle light/dark mode"
              className="grid h-8 w-8 place-items-center rounded-full text-[#515154] transition hover:bg-black/[0.06] dark:text-white/70 dark:hover:bg-white/[0.12]"
            >
              {theme === 'dark' ? <SunIcon /> : <MoonIcon />}
            </button>
            <div className="relative" ref={menuRef}>
              <button
                onClick={() => setMenuOpen((o) => !o)}
                aria-haspopup="menu"
                aria-expanded={menuOpen}
                title={`Signed in as ${username}`}
                className="grid h-8 w-8 place-items-center rounded-full bg-gradient-to-b from-[#0071e3] to-[#0058b0] text-[13px] font-semibold text-white transition active:scale-95"
              >
                {username.slice(0, 1).toUpperCase()}
              </button>
              {menuOpen && (
                <div
                  role="menu"
                  className="apple-card absolute right-0 z-40 mt-2 w-60 overflow-hidden !rounded-2xl p-1.5"
                >
                    <div className="px-3 pb-2 pt-2.5">
                      <p className="text-[13px] text-[#6e6e73] dark:text-white/50">Signed in as</p>
                      <p className="truncate text-[15px] font-semibold tracking-tight">{username}</p>
                    </div>
                    <div className="mx-1 border-t border-black/[0.06] dark:border-white/[0.08]" />
                    <button
                      role="menuitem"
                      onClick={() => {
                        setMenuOpen(false);
                        logout();
                        setAuthed(false);
                        setFeed([]);
                        setPrefs(null);
                      }}
                      className="mt-1 flex w-full items-center gap-2 rounded-xl px-3 py-2 text-left text-[15px] font-medium text-red-500 transition hover:bg-red-500/10 active:scale-[0.99]"
                    >
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={1.8} className="h-[17px] w-[17px]">
                        <path strokeLinecap="round" strokeLinejoin="round" d="M15 12H4m0 0 3.5-3.5M4 12l3.5 3.5M10 5H6a2 2 0 0 0-2 2v10a2 2 0 0 0 2 2h4" />
                        <path strokeLinecap="round" d="M15 5h3a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2h-3" />
                      </svg>
                      Sign out
                    </button>
                  </div>
              )}
            </div>
          </div>
        </div>
      </nav>

      <main className="mx-auto max-w-[720px] px-4 pb-24">
        {/* Hero */}
        <header className="rise pb-2 pt-10 text-center sm:pt-14">
          <p className="text-[13px] font-semibold uppercase tracking-[0.08em] text-[#0071e3] dark:text-[#0a84ff]">
            Personalized Hacker News
          </p>
          <h1 className="mx-auto mt-2 max-w-[520px] text-balance text-[36px] font-semibold leading-[1.05] tracking-tight sm:text-[52px]">
            Your feed, tuned to you.
          </h1>
          <p className="mx-auto mt-3 max-w-[460px] text-[17px] leading-snug text-[#6e6e73] dark:text-white/55">
            Upvote what interests you, skip what doesn’t. FeedRank re-ranks around your taste.
          </p>
          {prefs && (
            <div className="mt-5 flex flex-wrap items-center justify-center gap-2 text-[13px] text-[#6e6e73] dark:text-white/50">
              <span className="apple-pill !bg-black/[0.04] dark:!bg-white/[0.08]">
                {prefs.totalInteractions} {prefs.totalInteractions === 1 ? 'signal' : 'signals'}
              </span>
              <span className="apple-pill !bg-black/[0.04] dark:!bg-white/[0.08]">
                {prefs.weights.length} topics learned
              </span>
              {exploreCount > 0 && (
                <span className="rounded-full bg-[#0071e3]/10 px-2.5 py-[3px] text-[12px] font-semibold text-[#0071e3] dark:bg-[#0a84ff]/15 dark:text-[#0a84ff]">
                  ✦ {exploreCount} exploring
                </span>
              )}
            </div>
          )}
        </header>

        {/* Toolbar */}
        <div className="rise mt-6 flex flex-wrap items-center justify-between gap-2">
          <h2 className="text-[21px] font-semibold tracking-tight">Top stories</h2>
          <div className="flex items-center gap-2">
            <button onClick={() => setShowDebug((s) => !s)} className="apple-btn-quiet" aria-expanded={showDebug}>
              {showDebug ? 'Hide why' : 'Why this?'}
            </button>
            <button onClick={refresh} className="apple-btn-quiet" title="Re-fetch and re-rank">
              ↻ Refresh
            </button>
          </div>
        </div>

        {/* Debug / why panel */}
        {showDebug && prefs && (
          <section className="apple-card rise mt-3 p-5" aria-label="Why am I seeing this">
            <h3 className="text-[15px] font-semibold tracking-tight">Why am I seeing this?</h3>
            <p className="mt-1 text-[13px] leading-relaxed text-[#6e6e73] dark:text-white/55">
              Every upvote raises the weight of a story’s topics; skips lower them. Weights decay daily so your
              feed follows your current interests. Embeddings: {prefs.embeddingProvider}
              {prefs.hasPreferenceVector ? ` · vector dim ${prefs.vectorDim}` : ' · no preference vector yet'}.
            </p>
            <div className="mt-3 flex flex-wrap gap-1.5">
              {prefs.weights.slice(0, 24).map((w) => (
                <span
                  key={w.tag}
                  title={`weight ${w.weight}`}
                  className={
                    w.weight >= 0
                      ? 'rounded-full bg-[#0071e3]/10 px-2.5 py-[3px] text-[12px] font-semibold text-[#0071e3] dark:bg-[#0a84ff]/15 dark:text-[#0a84ff]'
                      : 'apple-pill'
                  }
                >
                  {w.tag} · {w.weight}
                </span>
              ))}
              {prefs.weights.length === 0 && (
                <span className="text-[13px] text-[#86868b]">Vote on a few stories and your topics will appear here.</span>
              )}
            </div>
            <button
              onClick={async () => {
                const r = await triggerIngest();
                setStatus(`Ingested +${r.inserted} stories (fetched ${r.fetched})`);
                refresh();
              }}
              className="apple-btn-quiet mt-4"
            >
              ↻ Ingest Hacker News now
            </button>
          </section>
        )}

        {status && <p className="mt-3 text-center text-[13px] text-[#6e6e73] dark:text-white/50">{status}</p>}

        {/* Feed */}
        {loading && feed.length === 0 ? (
          <div className="mt-6 space-y-3" aria-label="Loading">
            {[0, 1, 2].map((i) => (
              <div key={i} className="apple-card animate-pulse p-6">
                <div className="h-4 w-3/4 rounded bg-black/10 dark:bg-white/10" />
                <div className="mt-2 h-4 w-1/2 rounded bg-black/10 dark:bg-white/10" />
              </div>
            ))}
          </div>
        ) : feed.length === 0 ? (
          <div className="apple-card rise mt-6 p-10 text-center">
            <p className="text-[21px] font-semibold tracking-tight">Nothing here yet.</p>
            <p className="mx-auto mt-2 max-w-[380px] text-[15px] text-[#6e6e73] dark:text-white/55">
              The backend ingests Hacker News on startup and every 15 minutes. Open “Why this?” and tap “Ingest
              Hacker News now”, then refresh.
            </p>
            <button onClick={refresh} className="apple-btn-primary mt-5">
              Refresh
            </button>
          </div>
        ) : (
          <div className="rise-stagger mt-4 space-y-3">
            {feed.map((item) => (
              <FeedCard
                key={item.id}
                item={item}
                onVoted={() => {
                  getPreferences().then(setPrefs).catch(() => {});
                }}
              />
            ))}
          </div>
        )}

        {feed.length > 0 && (
          <div className="mt-10 text-center">
            <button onClick={refresh} className="apple-btn-primary !px-8 !py-3 !text-[17px]">
              Re-rank my feed
            </button>
            <p className="mt-3 text-[13px] text-[#86868b]">Vote on a few stories first — then feel the order adapt.</p>
          </div>
        )}
      </main>

      <footer className="border-t border-black/[0.08] py-6 text-center text-[12px] text-[#86868b] dark:border-white/[0.1]">
        FeedRank · Tags → Vectors → Exploration ·{' '}
        <span className="font-medium">{mode === 'tag' ? 'Tag ranking' : 'Vector ranking'}</span>
      </footer>
    </div>
  );
}
