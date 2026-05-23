import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { ArrowLeft, User } from 'lucide-react';
import {
  fetchMyProfile,
  updateMyProfile,
  type NotificationMode,
} from './profileApi';

const NOTIFICATION_OPTIONS: { value: NotificationMode; label: string; hint: string }[] = [
  { value: 'ALL', label: 'All messages', hint: 'Show every new message immediately' },
  { value: 'MENTIONS', label: 'Mentions only', hint: 'Only when someone @mentions you' },
  { value: 'FOLLOWED_THREADS', label: 'Followed threads', hint: 'Only threads you follow' },
];

export default function ProfilePage() {
  const qc = useQueryClient();
  const { data: profile, isLoading } = useQuery({
    queryKey: ['my-profile'],
    queryFn:  fetchMyProfile,
  });

  const [aboutMe, setAboutMe] = useState<string | null>(null);
  const [phone, setPhone] = useState<string | null>(null);
  const [showEmail, setShowEmail] = useState<boolean | null>(null);
  const [showPhone, setShowPhone] = useState<boolean | null>(null);
  const [interestsText, setInterestsText] = useState<string | null>(null);
  const [notificationMode, setNotificationMode] = useState<NotificationMode | null>(null);

  const saveMut = useMutation({
    mutationFn: updateMyProfile,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['my-profile'] });
      setAboutMe(null);
      setPhone(null);
      setShowEmail(null);
      setShowPhone(null);
      setInterestsText(null);
      setNotificationMode(null);
    },
  });

  if (isLoading || !profile) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center text-gray-500">
        Loading profile…
      </div>
    );
  }

  const currentAbout = aboutMe ?? profile.aboutMe ?? '';
  const currentPhone = phone ?? profile.phone ?? '';
  const currentShowEmail = showEmail ?? profile.showEmail;
  const currentShowPhone = showPhone ?? profile.showPhone;
  const currentInterests = interestsText ?? profile.interests.join(', ');
  const currentNotify = notificationMode ?? profile.notificationMode;

  const dirty =
    aboutMe !== null ||
    phone !== null ||
    showEmail !== null ||
    showPhone !== null ||
    interestsText !== null ||
    notificationMode !== null;

  const handleSave = () => {
    saveMut.mutate({
      aboutMe:          currentAbout,
      phone:            currentPhone,
      showEmail:        currentShowEmail,
      showPhone:        currentShowPhone,
      interests:        currentInterests.split(',').map((s) => s.trim()).filter(Boolean),
      notificationMode: currentNotify,
    });
  };

  return (
    <div className="min-h-screen bg-gray-50">
      <header className="bg-white border-b border-gray-200">
        <div className="mx-auto max-w-2xl px-6 h-14 flex items-center gap-3">
          <Link to="/dashboard" className="text-gray-400 hover:text-gray-600">
            <ArrowLeft className="h-5 w-5" />
          </Link>
          <User className="h-5 w-5 text-brand-600" />
          <h1 className="font-semibold text-gray-900">Your profile</h1>
        </div>
      </header>

      <main className="mx-auto max-w-2xl px-6 py-8 space-y-8">
        <div className="rounded-lg border border-brand-200 bg-brand-50 px-4 py-3 text-sm text-brand-900">
          Fill in <strong>About me</strong> and <strong>Interests</strong> before joining a workspace —
          the bot uses them to send a personalized welcome message in <code className="bg-white/60 px-1 rounded">#general</code>.
        </div>

        <section className="space-y-4">
          <h2 className="text-sm font-semibold text-gray-800">Basic info</h2>
          <p className="text-sm text-gray-600">
            {profile.displayName ?? 'Member'} · {profile.email}
          </p>

          <label className="block">
            <span className="text-xs font-medium text-gray-600">About me</span>
            <textarea
              rows={4}
              value={currentAbout}
              onChange={(e) => setAboutMe(e.target.value)}
              placeholder="Tell the community a bit about yourself…"
              className="mt-1 w-full border border-gray-300 rounded-lg px-3 py-2 text-sm resize-y focus:outline-none focus:ring-2 focus:ring-brand-500"
            />
          </label>

          <label className="block">
            <span className="text-xs font-medium text-gray-600">Interests</span>
            <input
              type="text"
              value={currentInterests}
              onChange={(e) => setInterestsText(e.target.value)}
              placeholder="Java, open source, hiking (comma-separated)"
              className="mt-1 w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
            />
          </label>
        </section>

        <section className="space-y-3">
          <h2 className="text-sm font-semibold text-gray-800">Contact (optional)</h2>
          <label className="block">
            <span className="text-xs font-medium text-gray-600">Phone</span>
            <input
              type="tel"
              value={currentPhone}
              onChange={(e) => setPhone(e.target.value)}
              className="mt-1 w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
            />
          </label>
          <label className="flex items-center gap-2 text-sm text-gray-700">
            <input
              type="checkbox"
              checked={currentShowEmail}
              onChange={(e) => setShowEmail(e.target.checked)}
            />
            Show email on my profile
          </label>
          <label className="flex items-center gap-2 text-sm text-gray-700">
            <input
              type="checkbox"
              checked={currentShowPhone}
              onChange={(e) => setShowPhone(e.target.checked)}
            />
            Show phone on my profile
          </label>
        </section>

        <section className="space-y-3">
          <h2 className="text-sm font-semibold text-gray-800">Notifications</h2>
          {NOTIFICATION_OPTIONS.map((opt) => (
            <label key={opt.value} className="flex items-start gap-2 cursor-pointer">
              <input
                type="radio"
                name="notify"
                checked={currentNotify === opt.value}
                onChange={() => setNotificationMode(opt.value)}
                className="mt-1"
              />
              <span>
                <span className="text-sm font-medium text-gray-800 block">{opt.label}</span>
                <span className="text-xs text-gray-500">{opt.hint}</span>
              </span>
            </label>
          ))}
        </section>

        <div className="flex gap-2">
          <button
            type="button"
            disabled={!dirty || saveMut.isPending}
            onClick={handleSave}
            className="px-4 py-2 bg-brand-600 text-white text-sm font-medium rounded-lg hover:bg-brand-700 disabled:opacity-50"
          >
            {saveMut.isPending ? 'Saving…' : 'Save profile'}
          </button>
          {dirty && (
            <button
              type="button"
              onClick={() => {
                setAboutMe(null);
                setPhone(null);
                setShowEmail(null);
                setShowPhone(null);
                setInterestsText(null);
                setNotificationMode(null);
              }}
              className="px-4 py-2 text-gray-600 text-sm font-medium rounded-lg hover:bg-gray-100"
            >
              Discard
            </button>
          )}
        </div>

        {saveMut.isSuccess && (
          <p className="text-green-600 text-sm">Profile saved.</p>
        )}
      </main>
    </div>
  );
}
