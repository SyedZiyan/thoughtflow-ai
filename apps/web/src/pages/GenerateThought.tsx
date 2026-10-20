import React, { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import {
  Sparkles,
  RefreshCw,
  Film,
  Copy,
  Check,
  Tag,
  AlignLeft,
  Settings2,
  FileText
} from 'lucide-react';
import { api, Thought } from '../api/client';

const THEMES = [
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

const TONES = ['inspiring', 'stoic', 'hard-hitting', 'philosophical', 'energizing'];

export const GenerateThought: React.FC = () => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [theme, setTheme] = useState('discipline');
  const [tone, setTone] = useState('inspiring');
  const [language, setLanguage] = useState('en');
  const [customPrompt, setCustomPrompt] = useState('');
  const [autoRender, setAutoRender] = useState(false);
  const [copied, setCopied] = useState(false);

  const [activeThought, setActiveThought] = useState<Thought | null>(null);

  const generateMutation = useMutation({
    mutationFn: () =>
      api.generateThought({
        theme,
        tone,
        language,
        custom_prompt: customPrompt,
        auto_render: autoRender
      }),
    onSuccess: (data) => {
      setActiveThought(data);
      queryClient.invalidateQueries({ queryKey: ['thoughts'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      if (autoRender) {
        alert('Thought generated and video rendering queued in studio!');
      }
    },
    onError: (err: any) => {
      alert(`Generation failed: ${err.message}`);
    }
  });

  const updateMutation = useMutation({
    mutationFn: (updated: Partial<Thought>) => {
      if (!activeThought) throw new Error('No active thought');
      return api.updateThought(activeThought.id, updated);
    },
    onSuccess: (data) => {
      setActiveThought(data);
      alert('Thought text and metadata updated successfully!');
    }
  });

  const handleCopy = () => {
    if (activeThought) {
      navigator.clipboard.writeText(activeThought.text);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  return (
    <div className="max-w-6xl mx-auto space-y-8">
      <div>
        <h1 className="text-2xl sm:text-3xl font-extrabold text-white tracking-tight">AI Thought Generator</h1>
        <p className="text-sm text-slate-400 mt-1">
          Generate original, high-impact motivational thoughts curated for viral vertical video.
        </p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
        {/* Controls Column */}
        <div className="lg:col-span-5 space-y-6">
          <div className="glass-panel rounded-3xl p-6 border border-white/5 space-y-5">
            <h3 className="text-base font-bold text-white flex items-center gap-2">
              <Settings2 className="w-4 h-4 text-brand-400" />
              <span>Generation Settings</span>
            </h3>

            {/* Theme Picker */}
            <div>
              <label className="block text-xs font-semibold uppercase text-slate-400 mb-2">Category Theme</label>
              <div className="grid grid-cols-2 gap-2 max-h-48 overflow-y-auto pr-1">
                {THEMES.map((t) => (
                  <button
                    key={t}
                    type="button"
                    onClick={() => setTheme(t)}
                    className={`px-3 py-2 rounded-xl text-xs font-semibold capitalize text-left transition-all border ${
                      theme === t
                        ? 'bg-brand-600/30 text-brand-300 border-brand-500/50'
                        : 'bg-white/5 text-slate-400 border-white/5 hover:border-white/10'
                    }`}
                  >
                    {t}
                  </button>
                ))}
              </div>
            </div>

            {/* Tone Selector */}
            <div>
              <label className="block text-xs font-semibold uppercase text-slate-400 mb-2">Tone of Voice</label>
              <div className="flex flex-wrap gap-2">
                {TONES.map((to) => (
                  <button
                    key={to}
                    type="button"
                    onClick={() => setTone(to)}
                    className={`px-3 py-1.5 rounded-lg text-xs font-medium capitalize transition-all border ${
                      tone === to
                        ? 'bg-brand-500/20 text-brand-300 border-brand-500/40'
                        : 'bg-white/5 text-slate-400 border-white/5 hover:bg-white/10'
                    }`}
                  >
                    {to}
                  </button>
                ))}
              </div>
            </div>

            {/* Custom Prompt Context */}
            <div>
              <label className="block text-xs font-semibold uppercase text-slate-400 mb-2">
                Custom Focus (Optional)
              </label>
              <input
                type="text"
                value={customPrompt}
                onChange={(e) => setCustomPrompt(e.target.value)}
                placeholder="e.g. Focus on morning habits, overcoming burnout..."
                className="w-full px-3.5 py-2.5 rounded-xl bg-white/5 border border-white/10 text-white placeholder-slate-500 text-xs focus:outline-none focus:border-brand-500"
              />
            </div>

            {/* Auto Render Checkbox */}
            <div className="flex items-center gap-3 pt-2">
              <input
                type="checkbox"
                id="autoRender"
                checked={autoRender}
                onChange={(e) => setAutoRender(e.target.checked)}
                className="w-4 h-4 rounded border-white/20 bg-white/5 text-brand-600 focus:ring-brand-500"
              />
              <label htmlFor="autoRender" className="text-xs text-slate-300 cursor-pointer">
                Automatically render 9:16 animated MP4 after generating
              </label>
            </div>

            {/* Generate Trigger */}
            <button
              onClick={() => generateMutation.mutate()}
              disabled={generateMutation.isPending}
              className="w-full py-3 rounded-2xl bg-gradient-to-r from-brand-600 to-indigo-600 hover:from-brand-500 hover:to-indigo-500 text-white text-sm font-bold flex items-center justify-center gap-2 shadow-lg shadow-purple-600/30 transition-all disabled:opacity-50"
            >
              {generateMutation.isPending ? (
                <>
                  <RefreshCw className="w-4 h-4 animate-spin" />
                  <span>Generating Structured Thought...</span>
                </>
              ) : (
                <>
                  <Sparkles className="w-4 h-4" />
                  <span>Generate Thought Now</span>
                </>
              )}
            </button>
          </div>
        </div>

        {/* Output & Editor Column */}
        <div className="lg:col-span-7 space-y-6">
          {activeThought ? (
            <div className="glass-panel rounded-3xl p-6 sm:p-8 border border-white/5 space-y-6">
              <div className="flex items-center justify-between pb-4 border-b border-white/5">
                <div className="flex items-center gap-2">
                  <span className="px-3 py-1 rounded-full text-xs font-semibold bg-brand-500/20 text-brand-300 border border-brand-500/30 capitalize">
                    {activeThought.theme}
                  </span>
                  <span className="text-xs text-slate-400">Hash: {activeThought.content_hash.slice(0, 10)}...</span>
                </div>
                <div className="flex items-center gap-2">
                  <button
                    onClick={handleCopy}
                    className="p-2 rounded-xl bg-white/5 text-slate-300 hover:text-white hover:bg-white/10"
                    title="Copy text"
                  >
                    {copied ? <Check className="w-4 h-4 text-emerald-400" /> : <Copy className="w-4 h-4" />}
                  </button>
                </div>
              </div>

              {/* Editable Thought Text */}
              <div>
                <label className="block text-xs font-semibold uppercase text-slate-400 mb-2">Generated Thought</label>
                <textarea
                  rows={3}
                  value={activeThought.text}
                  onChange={(e) => setActiveThought({ ...activeThought, text: e.target.value })}
                  className="w-full p-4 rounded-2xl bg-white/5 border border-white/10 text-white text-base sm:text-lg font-medium leading-relaxed focus:outline-none focus:border-brand-500"
                />
              </div>

              {/* YouTube Title */}
              <div>
                <label className="block text-xs font-semibold uppercase text-slate-400 mb-2">Video Title (#Shorts)</label>
                <input
                  type="text"
                  value={activeThought.title}
                  onChange={(e) => setActiveThought({ ...activeThought, title: e.target.value })}
                  className="w-full px-4 py-2.5 rounded-xl bg-white/5 border border-white/10 text-white text-sm focus:outline-none focus:border-brand-500 font-semibold"
                />
              </div>

              {/* Description & Hashtags */}
              <div>
                <label className="block text-xs font-semibold uppercase text-slate-400 mb-2">Description</label>
                <textarea
                  rows={3}
                  value={activeThought.description}
                  onChange={(e) => setActiveThought({ ...activeThought, description: e.target.value })}
                  className="w-full p-3 rounded-xl bg-white/5 border border-white/10 text-slate-300 text-xs focus:outline-none focus:border-brand-500"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold uppercase text-slate-400 mb-2">Tags</label>
                <div className="flex flex-wrap gap-1.5">
                  {activeThought.hashtags.map((tag, i) => (
                    <span key={i} className="px-2.5 py-1 rounded-lg bg-white/5 text-slate-300 text-xs font-mono">
                      #{tag}
                    </span>
                  ))}
                </div>
              </div>

              {/* Action Buttons */}
              <div className="pt-4 border-t border-white/5 flex flex-wrap items-center justify-between gap-4">
                <button
                  onClick={() => updateMutation.mutate({
                    text: activeThought.text,
                    title: activeThought.title,
                    description: activeThought.description
                  })}
                  className="px-4 py-2.5 rounded-xl bg-white/10 hover:bg-white/15 text-white text-xs font-semibold"
                >
                  Save Edits
                </button>

                <button
                  onClick={() => navigate('/studio', { state: { thoughtId: activeThought.id } })}
                  className="flex items-center gap-2 px-5 py-2.5 rounded-xl bg-gradient-to-r from-brand-600 to-indigo-600 hover:from-brand-500 hover:to-indigo-500 text-white text-xs font-bold shadow-lg shadow-purple-600/30"
                >
                  <Film className="w-4 h-4" />
                  <span>Send to Video Studio</span>
                </button>
              </div>
            </div>
          ) : (
            <div className="h-full min-h-[380px] glass-panel rounded-3xl p-8 border border-dashed border-white/10 flex flex-col items-center justify-center text-center">
              <Sparkles className="w-12 h-12 text-slate-600 mb-4" />
              <h3 className="text-base font-bold text-slate-200">No Thought Generated Yet</h3>
              <p className="text-xs text-slate-400 max-w-sm mt-1">
                Choose a category theme on the left and click "Generate Thought Now" to create original content.
              </p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
