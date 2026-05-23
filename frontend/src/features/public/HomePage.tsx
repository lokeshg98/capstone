import { useState } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import {
  Bot, Calendar, MessageSquare, Shield, Sparkles, Users, Mail, Send,
} from 'lucide-react';
import { cn } from '@/lib/utils';
import {
  askPublicBot,
  fetchPlatformInfo,
  fetchPublicFaq,
  subscribeNewsletter,
} from './publicApi';

const BACKEND = import.meta.env.VITE_BACKEND_URL ?? 'http://localhost:8080';

const ICONS: Record<string, React.ReactNode> = {
  messages: <MessageSquare className="h-5 w-5" />,
  bot:      <Bot className="h-5 w-5" />,
  shield:   <Shield className="h-5 w-5" />,
  summary:  <Sparkles className="h-5 w-5" />,
  calendar: <Calendar className="h-5 w-5" />,
  roles:    <Users className="h-5 w-5" />,
};

export default function HomePage() {
  const { data: platform } = useQuery({ queryKey: ['platform'], queryFn: fetchPlatformInfo });
  const { data: faq = [] } = useQuery({ queryKey: ['public-faq'], queryFn: () => fetchPublicFaq(6) });

  const [newsletterEmail, setNewsletterEmail] = useState('');
  const [newsletterMsg, setNewsletterMsg]       = useState<string | null>(null);
  const [botQuestion, setBotQuestion]           = useState('');
  const [botAnswer, setBotAnswer]               = useState<string | null>(null);

  const newsletter = useMutation({
    mutationFn: subscribeNewsletter,
    onSuccess:  () => { setNewsletterMsg('Thanks — you are subscribed!'); setNewsletterEmail(''); },
    onError: (err: unknown) => {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setNewsletterMsg(msg || 'Subscription failed');
    },
  });

  const publicBot = useMutation({
    mutationFn: askPublicBot,
    onSuccess:  (res) => setBotAnswer(res.answer),
    onError:    () => setBotAnswer('Sorry, the demo bot is unavailable. Is the backend running?'),
  });

  const title = platform?.title ?? 'Community Bot';

  return (
    <div className="min-h-screen bg-gradient-to-b from-slate-50 to-white text-gray-900">
      {/* Nav */}
      <header className="border-b border-gray-200/80 bg-white/80 backdrop-blur sticky top-0 z-20">
        <div className="mx-auto max-w-5xl px-6 h-14 flex items-center justify-between">
          <div className="flex items-center gap-2 font-semibold">
            <div className="h-8 w-8 rounded-lg bg-brand-600 flex items-center justify-center text-white text-sm font-bold">CB</div>
            {title}
          </div>
          <div className="flex items-center gap-3">
            <a href="#features" className="text-sm text-gray-600 hover:text-gray-900 hidden sm:inline">Features</a>
            <a href="#faq" className="text-sm text-gray-600 hover:text-gray-900 hidden sm:inline">FAQ</a>
            <Link to="/login" className="text-sm font-medium text-brand-700 hover:text-brand-800">Sign in</Link>
          </div>
        </div>
      </header>

      {/* Hero */}
      <section className="mx-auto max-w-5xl px-6 pt-16 pb-12 text-center">
        <p className="text-sm font-medium text-brand-600 mb-3">{platform?.tagline ?? 'AI-powered community platform'}</p>
        <h1 className="text-4xl sm:text-5xl font-bold tracking-tight text-gray-900 mb-4">
          {title}
        </h1>
        <p className="mx-auto max-w-2xl text-lg text-gray-600 leading-relaxed">
          {platform?.summary ?? 'Slack-style chat with an AI moderator, RAG FAQ assistant, thread summaries, and scheduled posts.'}
        </p>
        <div id="login" className="mt-8 flex flex-col sm:flex-row items-center justify-center gap-3">
          <OAuthButton href={`${BACKEND}/oauth2/authorization/google`} label="Continue with Google" variant="google" />
          <OAuthButton href={`${BACKEND}/oauth2/authorization/github`} label="Continue with GitHub" variant="github" />
        </div>
        <p className="mt-4 text-xs text-gray-400">Sign in to create organisations, join workspaces, and chat in real time.</p>
      </section>

      {/* Features */}
      <section id="features" className="mx-auto max-w-5xl px-6 py-12">
        <h2 className="text-2xl font-bold mb-6">Platform features</h2>
        <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {(platform?.features ?? []).map((f) => (
            <div key={f.title} className="rounded-xl border border-gray-200 bg-white p-5 shadow-sm">
              <div className="h-10 w-10 rounded-lg bg-brand-50 text-brand-700 flex items-center justify-center mb-3">
                {ICONS[f.icon] ?? <Sparkles className="h-5 w-5" />}
              </div>
              <h3 className="font-semibold text-gray-900 mb-1">{f.title}</h3>
              <p className="text-sm text-gray-600 leading-relaxed">{f.description}</p>
            </div>
          ))}
        </div>
      </section>

      {/* Public demo bot */}
      <section className="mx-auto max-w-5xl px-6 py-12">
        <div className="rounded-2xl border border-brand-100 bg-brand-50/50 p-6 sm:p-8">
          <div className="flex items-center gap-2 mb-2">
            <Bot className="h-5 w-5 text-brand-700" />
            <h2 className="text-xl font-bold">Try the bot — no login required</h2>
          </div>
          <p className="text-sm text-gray-600 mb-4">
            Ask about Community Bot features. Answers are grounded in the platform FAQ.
          </p>
          <div className="flex gap-2">
            <input
              value={botQuestion}
              onChange={(e) => setBotQuestion(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && botQuestion.trim() && publicBot.mutate(botQuestion.trim())}
              placeholder="e.g. How does moderation work?"
              className="flex-1 rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
            />
            <button
              type="button"
              disabled={!botQuestion.trim() || publicBot.isPending}
              onClick={() => publicBot.mutate(botQuestion.trim())}
              className="inline-flex items-center gap-1 rounded-lg bg-brand-600 px-4 py-2 text-sm font-medium text-white hover:bg-brand-700 disabled:opacity-50"
            >
              <Send className="h-4 w-4" /> Ask
            </button>
          </div>
          {botAnswer && (
            <div className="mt-4 rounded-lg bg-white border border-gray-200 p-4 text-sm text-gray-800 whitespace-pre-wrap">
              {botAnswer}
            </div>
          )}
        </div>
      </section>

      {/* FAQ preview */}
      <section id="faq" className="mx-auto max-w-5xl px-6 py-12">
        <h2 className="text-2xl font-bold mb-6">Frequently asked questions</h2>
        <div className="space-y-4">
          {faq.map((entry) => (
            <details key={entry.question} className="rounded-xl border border-gray-200 bg-white p-4 group">
              <summary className="font-medium cursor-pointer list-none flex justify-between items-center">
                {entry.question}
                <span className="text-gray-400 group-open:rotate-180 transition-transform">▾</span>
              </summary>
              <p className="mt-3 text-sm text-gray-600 leading-relaxed">{entry.answer}</p>
            </details>
          ))}
        </div>
      </section>

      {/* Newsletter */}
      <section className="mx-auto max-w-5xl px-6 py-12">
        <div className="rounded-2xl bg-gray-900 text-white p-8 sm:p-10">
          <div className="flex items-center gap-2 mb-2">
            <Mail className="h-5 w-5" />
            <h2 className="text-xl font-bold">Stay in the loop</h2>
          </div>
          <p className="text-gray-300 text-sm mb-4">Product updates and community tips. No spam.</p>
          <form
            className="flex flex-col sm:flex-row gap-2 max-w-md"
            onSubmit={(e) => {
              e.preventDefault();
              setNewsletterMsg(null);
              newsletter.mutate(newsletterEmail.trim());
            }}
          >
            <input
              type="email"
              required
              value={newsletterEmail}
              onChange={(e) => setNewsletterEmail(e.target.value)}
              placeholder="you@example.com"
              className="flex-1 rounded-lg border-0 px-3 py-2 text-sm text-gray-900"
            />
            <button
              type="submit"
              disabled={newsletter.isPending}
              className="rounded-lg bg-white px-4 py-2 text-sm font-medium text-gray-900 hover:bg-gray-100 disabled:opacity-50"
            >
              Subscribe
            </button>
          </form>
          {newsletterMsg && (
            <p className={cn('mt-3 text-sm', newsletterMsg.includes('Thanks') ? 'text-green-400' : 'text-amber-300')}>
              {newsletterMsg}
            </p>
          )}
        </div>
      </section>

      <footer className="border-t border-gray-200 py-8 text-center text-xs text-gray-400">
        Community Bot · Capstone project
      </footer>
    </div>
  );
}

function OAuthButton({ href, label, variant }: { href: string; label: string; variant: 'google' | 'github' }) {
  return (
    <a
      href={href}
      className={cn(
        'inline-flex w-full sm:w-auto min-w-[220px] items-center justify-center gap-2 rounded-lg border px-5 py-3 text-sm font-medium transition-colors',
        variant === 'google'
          ? 'bg-white text-gray-700 border-gray-300 hover:bg-gray-50'
          : 'bg-gray-900 text-white border-gray-900 hover:bg-gray-800',
      )}
    >
      {label}
    </a>
  );
}
