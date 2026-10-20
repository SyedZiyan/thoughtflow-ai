import React, { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Bot,
  Clock,
  Globe,
  Sliders,
  CheckCircle2,
  RefreshCw,
  Save,
  Volume2,
  ShieldCheck,
  AlertCircle
} from 'lucide-react';
import { api, UserSettings } from '../api/client';

const ALL_THEMES = [
  'motivation',
  'discipline',
  'consistency',
  'self-improvement',
  'life lessons',
  'success',
  'confidence',
  'relationships',
  'resilience',
  'personal growth',
  'mindfulness',
  'productivity'
];

export const AutomationSettings: React.FC = () => {
  const queryClient = useQueryClient();

  const { data: settings, isLoading } = useQuery<UserSettings>({
    queryKey: ['settings'],
    queryFn: api.getSettings,
  });

  const [form, setForm] = useState<Partial<UserSettings>>({
    automation_enabled: false,
    generation_time: '07:00',
    publishing_time: '09:00',
    timezone: 'UTC',
    selected_themes: ['motivation', 'discipline', 'consistency'],
    video_duration: 15,
    narration_enabled: false,
    auto_publish_enabled: false,
    daily_limit: 5,
    visual_theme: 'gradient_purple',
    animation_style: 'smooth_zoom'
  });

  useEffect(() => {
    if (settings) {
      setForm(settings);
    }
  }, [settings]);

  const updateMutation = useMutation({
    mutationFn: (updated: Partial<UserSettings>) => api.updateSettings(updated),
    onSuccess: (data) => {
      setForm(data);
      queryClient.invalidateQueries({ queryKey: ['settings'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      alert('Automation settings updated and applied to worker!');
    },
    onError: (err: any) => {
      alert(`Update failed: ${err.message}`);
    }
  });

  const handleToggleTheme = (t: string) => {
    const current = form.selected_themes || [];
    const updated = current.includes(t)
      ? current.filter((x) => x !== t)
      : [...current, t];
    setForm({ ...form, selected_themes: updated });
  };

  if (isLoading) {
    return <div className="p-12 text-center text-slate-400">Loading settings...</div>;
  }

  return (
    <div className="max-w-4xl mx-auto space-y-8">
      <div>
        <h1 className="text-2xl sm:text-3xl font-extrabold text-white tracking-tight">Automation Settings</h1>
        <p className="text-sm text-slate-400 mt-1">
          Configure daily autonomous generation, render parameters, time slots, and publishing limits.
        </p>
      </div>

      <div className="glass-panel rounded-3xl p-6 sm:p-8 border border-white/5 space-y-8">
        {/* Master Automation Toggle */}
        <div className="flex items-center justify-between pb-6 border-b border-white/5">
          <div>
            <h3 className="text-base font-bold text-white flex items-center gap-2">
              <Bot className="w-5 h-5 text-brand-400" />
              <span>Daily Automated Execution</span>
            </h3>
            <p className="text-xs text-slate-400 mt-1">
              When enabled, the standalone Python worker generates and renders your daily Short automatically.
            </p>
          </div>
          <label className="relative inline-flex items-center cursor-pointer">
            <input
              type="checkbox"
              checked={form.automation_enabled || false}
              onChange={(e) => setForm({ ...form, automation_enabled: e.target.checked })}
              className="sr-only peer"
            />
            <div className="w-12 h-6 bg-white/10 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-brand-600"></div>
          </label>
        </div>

        {/* Schedule Times */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-6">
          <div>
            <label className="block text-xs font-semibold uppercase text-slate-400 mb-2">Generation Time</label>
            <input
              type="time"
              value={form.generation_time || '07:00'}
              onChange={(e) => setForm({ ...form, generation_time: e.target.value })}
              className="w-full px-4 py-2.5 rounded-xl bg-white/5 border border-white/10 text-white text-sm focus:outline-none focus:border-brand-500 font-mono"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold uppercase text-slate-400 mb-2">Publishing Time</label>
            <input
              type="time"
              value={form.publishing_time || '09:00'}
              onChange={(e) => setForm({ ...form, publishing_time: e.target.value })}
              className="w-full px-4 py-2.5 rounded-xl bg-white/5 border border-white/10 text-white text-sm focus:outline-none focus:border-brand-500 font-mono"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold uppercase text-slate-400 mb-2">Timezone</label>
            <select
              value={form.timezone || 'UTC'}
              onChange={(e) => setForm({ ...form, timezone: e.target.value })}
              className="w-full px-4 py-2.5 rounded-xl bg-white/5 border border-white/10 text-white text-sm focus:outline-none focus:border-brand-500"
            >
              <option value="UTC" className="bg-dark-800">UTC (Universal)</option>
              <option value="Asia/Kolkata" className="bg-dark-800">Asia/Kolkata (IST)</option>
              <option value="America/New_York" className="bg-dark-800">America/New_York (EST)</option>
              <option value="Europe/London" className="bg-dark-800">Europe/London (GMT)</option>
            </select>
          </div>
        </div>

        {/* Allowed Themes Rotation */}
        <div>
          <label className="block text-xs font-semibold uppercase text-slate-400 mb-2">
            Themes Included in Daily Rotation ({form.selected_themes?.length || 0} selected)
          </label>
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-2.5">
            {ALL_THEMES.map((t) => {
              const isSelected = form.selected_themes?.includes(t);
              return (
                <button
                  key={t}
                  type="button"
                  onClick={() => handleToggleTheme(t)}
                  className={`px-3 py-2 rounded-xl text-xs font-semibold capitalize text-left transition-all border ${
                    isSelected
                      ? 'bg-brand-600/30 text-brand-300 border-brand-500/50'
                      : 'bg-white/5 text-slate-400 border-white/5 hover:border-white/10'
                  }`}
                >
                  {t}
                </button>
              );
            })}
          </div>
        </div>

        {/* Video Parameters */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-6 pt-4 border-t border-white/5">
          <div>
            <label className="block text-xs font-semibold uppercase text-slate-400 mb-2">Default Video Duration</label>
            <div className="flex gap-2">
              {[10, 15, 20, 30].map((dur) => (
                <button
                  key={dur}
                  type="button"
                  onClick={() => setForm({ ...form, video_duration: dur })}
                  className={`px-4 py-2 rounded-xl text-xs font-bold border transition-all ${
                    form.video_duration === dur
                      ? 'bg-brand-600 text-white border-brand-500'
                      : 'bg-white/5 text-slate-400 border-white/5'
                  }`}
                >
                  {dur}s
                </button>
              ))}
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold uppercase text-slate-400 mb-2">Daily Generation Limit</label>
            <input
              type="number"
              min={1}
              max={20}
              value={form.daily_limit || 5}
              onChange={(e) => setForm({ ...form, daily_limit: parseInt(e.target.value) || 5 })}
              className="w-full px-4 py-2.5 rounded-xl bg-white/5 border border-white/10 text-white text-sm focus:outline-none focus:border-brand-500 font-mono"
            />
          </div>
        </div>

        {/* Auto Publish Toggle */}
        <div className="flex items-center justify-between pt-4 border-t border-white/5">
          <div>
            <div className="text-sm font-bold text-white">Direct Auto-Publishing to YouTube</div>
            <p className="text-xs text-slate-400">
              When disabled, videos are rendered in dry-run mode and saved to the Content Library for your review.
            </p>
          </div>
          <label className="relative inline-flex items-center cursor-pointer">
            <input
              type="checkbox"
              checked={form.auto_publish_enabled || false}
              onChange={(e) => setForm({ ...form, auto_publish_enabled: e.target.checked })}
              className="sr-only peer"
            />
            <div className="w-12 h-6 bg-white/10 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-emerald-600"></div>
          </label>
        </div>

        {/* Save button */}
        <div className="pt-6 border-t border-white/5 flex justify-end">
          <button
            onClick={() => updateMutation.mutate(form)}
            disabled={updateMutation.isPending}
            className="flex items-center gap-2 px-6 py-3 rounded-2xl bg-gradient-to-r from-brand-600 to-indigo-600 hover:from-brand-500 hover:to-indigo-500 text-white text-sm font-bold shadow-lg shadow-purple-600/30 transition-all disabled:opacity-50"
          >
            {updateMutation.isPending ? (
              <>
                <RefreshCw className="w-4 h-4 animate-spin" />
                <span>Saving Settings...</span>
              </>
            ) : (
              <>
                <Save className="w-4 h-4" />
                <span>Save Automation Settings</span>
              </>
            )}
          </button>
        </div>
      </div>
    </div>
  );
};
