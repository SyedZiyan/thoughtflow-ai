import React, { useState, useEffect } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Film,
  Play,
  Download,
  Upload,
  RefreshCw,
  Palette,
  Sparkles,
  CheckCircle2,
  Sliders,
  Clock,
  Layers,
  Type,
  AlignVerticalJustifyCenter,
  Volume2,
  Mic,
  Music,
  Eye,
  Check,
  AlertCircle
} from 'lucide-react';
import { api, Thought, VideoAsset } from '../api/client';

const VISUAL_THEMES = [
  { id: 'gradient_purple', name: 'Cosmic Purple', preview: 'from-purple-950 via-indigo-950 to-slate-950', accent: '#a878ff' },
  { id: 'geometric_dark', name: 'Emerald Obsidian', preview: 'from-emerald-950 via-slate-900 to-black', accent: '#34d399' },
  { id: 'nebula', name: 'Midnight Cyan', preview: 'from-blue-950 via-slate-900 to-slate-950', accent: '#38bdf8' },
  { id: 'gold_luxury', name: 'Bronze Luxury', preview: 'from-amber-950 via-neutral-900 to-black', accent: '#fbbf24' },
  { id: 'minimal_slate', name: 'Graphite Minimal', preview: 'from-slate-900 via-zinc-900 to-neutral-950', accent: '#94a3b8' },
  { id: 'sunset_crimson', name: 'Sunset Crimson', preview: 'from-rose-950 via-stone-900 to-black', accent: '#f43f5e' }
];

const ANIMATION_STYLES = [
  { id: 'smooth_zoom', name: 'Cinematic Zoom', desc: 'Slow continuous punch-in' },
  { id: 'kinetic_pan', name: 'Kinetic Vertical Pan', desc: 'Subtle downward framed tracking' },
  { id: 'subtle_pulse', name: 'Breathing Pulse', desc: 'Gentle oscillating zoom rhythm' },
  { id: 'gentle_drift', name: 'Diagonal Drift', desc: 'Slow diagonal panning motion' }
];

const TYPOGRAPHY_OPTIONS = [
  { id: 'modern_sans', name: 'Modern Sans', preview: 'Segoe UI / Arial', desc: 'Crisp, bold, punchy' },
  { id: 'editorial_serif', name: 'Editorial Serif', preview: 'Georgia / Times', desc: 'Literary, reflective elegance' },
  { id: 'clean_mono', name: 'Clean Monospace', preview: 'Consolas / Mono', desc: 'Minimalist tech aesthetic' }
];

const TEXT_PLACEMENTS = [
  { id: 'center', name: 'Center' },
  { id: 'upper_third', name: 'Upper Third' },
  { id: 'lower_third', name: 'Lower Third' }
];

const TTS_VOICES = [
  { id: 'en-US-ChristopherNeural', name: 'Christopher (Motivating Male)' },
  { id: 'en-US-JennyNeural', name: 'Jenny (Warm Female)' },
  { id: 'en-US-GuyNeural', name: 'Guy (Conversational Male)' },
  { id: 'en-GB-SoniaNeural', name: 'Sonia (Reflective British Female)' }
];

export const VideoStudio: React.FC = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [selectedThoughtId, setSelectedThoughtId] = useState<string>('');
  const [visualTheme, setVisualTheme] = useState('gradient_purple');
  const [animationStyle, setAnimationStyle] = useState('smooth_zoom');
  const [typography, setTypography] = useState('modern_sans');
  const [textPlacement, setTextPlacement] = useState('center');
  const [duration, setDuration] = useState(15);
  const [narrationEnabled, setNarrationEnabled] = useState(false);
  const [ambientEnabled, setAmbientEnabled] = useState(true);
  const [narrationVoice, setNarrationVoice] = useState('en-US-ChristopherNeural');
  const [renderedVideo, setRenderedVideo] = useState<VideoAsset | null>(null);
  const [previewImageUrl, setPreviewImageUrl] = useState<string | null>(null);
  const [previewLoading, setPreviewLoading] = useState(false);
  const [activeTab, setActiveTab] = useState<'preview' | 'video'>('preview');

  // Fetch thoughts catalog
  const { data: thoughts, isLoading: thoughtsLoading } = useQuery<Thought[]>({
    queryKey: ['thoughts'],
    queryFn: () => api.getThoughts(),
  });

  useEffect(() => {
    if (location.state?.thoughtId) {
      setSelectedThoughtId(location.state.thoughtId);
    } else if (thoughts && thoughts.length > 0 && !selectedThoughtId) {
      setSelectedThoughtId(thoughts[0].id);
    }
  }, [thoughts, location.state]);

  const selectedThought = thoughts?.find((t) => t.id === selectedThoughtId);

  // Live slide preview generator
  useEffect(() => {
    if (!selectedThought?.text) return;

    let isMounted = true;
    setPreviewLoading(true);

    const timeout = setTimeout(async () => {
      try {
        const url = await api.previewFrame({
          thought_text: selectedThought.text,
          theme: selectedThought.theme,
          visual_theme: visualTheme,
          typography: typography,
          text_placement: textPlacement
        });
        if (isMounted) {
          setPreviewImageUrl(url);
          setPreviewLoading(false);
        }
      } catch (err) {
        console.error('Preview error:', err);
        if (isMounted) setPreviewLoading(false);
      }
    }, 250);

    return () => {
      isMounted = false;
      clearTimeout(timeout);
    };
  }, [selectedThought?.text, visualTheme, typography, textPlacement]);

  const renderMutation = useMutation({
    mutationFn: () => {
      if (!selectedThoughtId) throw new Error('Please select a thought first');
      return api.renderVideo({
        thought_id: selectedThoughtId,
        visual_theme: visualTheme,
        animation_style: animationStyle,
        typography: typography,
        text_placement: textPlacement,
        duration_seconds: duration,
        narration_enabled: narrationEnabled,
        ambient_enabled: ambientEnabled,
        narration_voice: narrationVoice,
      });
    },
    onSuccess: (video) => {
      setRenderedVideo(video);
      setActiveTab('video');
      queryClient.invalidateQueries({ queryKey: ['videos'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
    },
    onError: (err: any) => {
      alert(`Render failed: ${err.message}`);
    }
  });

  const uploadMutation = useMutation({
    mutationFn: (videoId: string) => api.uploadVideoToYouTube(videoId),
    onSuccess: (res) => {
      alert(res.dry_run ? 'Dry-run upload passed! Ready for live YouTube.' : 'Successfully published on YouTube!');
      queryClient.invalidateQueries({ queryKey: ['videos'] });
    },
    onError: (err: any) => {
      alert(`Upload failed: ${err.message}`);
    }
  });

  return (
    <div className="max-w-7xl mx-auto space-y-8">
      <div>
        <h1 className="text-2xl sm:text-3xl font-extrabold text-white tracking-tight flex items-center gap-3">
          <span>Video Studio</span>
          <span className="text-xs px-2.5 py-1 rounded-full bg-brand-500/20 text-brand-300 font-semibold border border-brand-500/30">
            Phase 3 Engine
          </span>
        </h1>
        <p className="text-sm text-slate-400 mt-1">
          Compose typography, select luxury palettes, mix neural narration & ambient chords, and render 1080×1920 MP4s.
        </p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
        {/* Studio Controls */}
        <div className="lg:col-span-7 space-y-6">
          <div className="glass-panel rounded-3xl p-6 sm:p-7 border border-white/5 space-y-6">
            {/* Thought Selector */}
            <div>
              <label className="block text-xs font-semibold uppercase text-slate-400 mb-2">
                1. Select Thought to Animate
              </label>
              {thoughtsLoading ? (
                <div className="p-3 text-xs text-slate-400">Loading library thoughts...</div>
              ) : thoughts && thoughts.length > 0 ? (
                <select
                  value={selectedThoughtId}
                  onChange={(e) => setSelectedThoughtId(e.target.value)}
                  className="w-full px-4 py-3 rounded-2xl bg-white/5 border border-white/10 text-white text-sm focus:outline-none focus:border-brand-500 font-medium"
                >
                  {thoughts.map((t) => (
                    <option key={t.id} value={t.id} className="bg-dark-800 text-white">
                      [{t.theme.toUpperCase()}] {t.text.slice(0, 75)}...
                    </option>
                  ))}
                </select>
              ) : (
                <div className="p-4 rounded-xl bg-white/5 text-xs text-slate-400">
                  No thoughts found. <button onClick={() => navigate('/generate')} className="text-brand-400 underline">Generate one now.</button>
                </div>
              )}
            </div>

            {/* Selected Thought Display Card */}
            {selectedThought && (
              <div className="glass-card rounded-2xl p-4 border border-brand-500/20">
                <div className="text-[10px] font-semibold uppercase tracking-wider text-brand-400 mb-1">
                  Selected Content
                </div>
                <div className="text-sm text-white font-medium leading-relaxed italic">
                  "{selectedThought.text}"
                </div>
                <div className="mt-2 text-xs text-slate-400 font-semibold">
                  Title: {selectedThought.title}
                </div>
              </div>
            )}

            {/* Visual Themes */}
            <div>
              <label className="block text-xs font-semibold uppercase text-slate-400 mb-2 flex items-center gap-1.5">
                <Palette className="w-3.5 h-3.5 text-brand-400" />
                <span>2. Visual Theme & Palette</span>
              </label>
              <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
                {VISUAL_THEMES.map((vt) => (
                  <button
                    key={vt.id}
                    type="button"
                    onClick={() => setVisualTheme(vt.id)}
                    className={`p-3 rounded-xl border text-left transition-all ${
                      visualTheme === vt.id
                        ? 'border-brand-500 bg-brand-600/20 shadow-sm shadow-brand-500/20'
                        : 'border-white/5 bg-white/5 hover:border-white/10'
                    }`}
                  >
                    <div className={`h-8 rounded-lg bg-gradient-to-br ${vt.preview} border border-white/10 mb-2`} />
                    <div className="text-xs font-semibold text-white">{vt.name}</div>
                  </button>
                ))}
              </div>
            </div>

            {/* Typography & Placement */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-semibold uppercase text-slate-400 mb-2 flex items-center gap-1.5">
                  <Type className="w-3.5 h-3.5 text-brand-400" />
                  <span>3. Typography</span>
                </label>
                <div className="space-y-2">
                  {TYPOGRAPHY_OPTIONS.map((ty) => (
                    <button
                      key={ty.id}
                      type="button"
                      onClick={() => setTypography(ty.id)}
                      className={`w-full p-2.5 rounded-xl border text-left transition-all text-xs ${
                        typography === ty.id
                          ? 'border-brand-500 bg-brand-600/20 text-white font-bold'
                          : 'border-white/5 bg-white/5 text-slate-400 hover:text-white'
                      }`}
                    >
                      <div>{ty.name}</div>
                      <div className="text-[10px] text-slate-500">{ty.desc}</div>
                    </button>
                  ))}
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold uppercase text-slate-400 mb-2 flex items-center gap-1.5">
                  <AlignVerticalJustifyCenter className="w-3.5 h-3.5 text-brand-400" />
                  <span>4. Text Placement</span>
                </label>
                <div className="space-y-2">
                  {TEXT_PLACEMENTS.map((tp) => (
                    <button
                      key={tp.id}
                      type="button"
                      onClick={() => setTextPlacement(tp.id)}
                      className={`w-full p-2.5 rounded-xl border text-left transition-all text-xs ${
                        textPlacement === tp.id
                          ? 'border-brand-500 bg-brand-600/20 text-white font-bold'
                          : 'border-white/5 bg-white/5 text-slate-400 hover:text-white'
                      }`}
                    >
                      {tp.name}
                    </button>
                  ))}
                </div>
              </div>
            </div>

            {/* Animation Style */}
            <div>
              <label className="block text-xs font-semibold uppercase text-slate-400 mb-2 flex items-center gap-1.5">
                <Layers className="w-3.5 h-3.5 text-brand-400" />
                <span>5. Animation Effect</span>
              </label>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                {ANIMATION_STYLES.map((st) => (
                  <button
                    key={st.id}
                    type="button"
                    onClick={() => setAnimationStyle(st.id)}
                    className={`p-3 rounded-xl border text-left transition-all ${
                      animationStyle === st.id
                        ? 'border-brand-500 bg-brand-600/20 shadow-sm shadow-brand-500/20'
                        : 'border-white/5 bg-white/5 hover:border-white/10'
                    }`}
                  >
                    <div className="text-xs font-bold text-white">{st.name}</div>
                    <div className="text-[11px] text-slate-400 mt-0.5">{st.desc}</div>
                  </button>
                ))}
              </div>
            </div>

            {/* Audio Options: Narration & Ambient */}
            <div className="glass-card rounded-2xl p-4 border border-white/5 space-y-4">
              <div className="text-xs font-semibold uppercase text-slate-400 flex items-center gap-1.5">
                <Volume2 className="w-3.5 h-3.5 text-brand-400" />
                <span>6. Audio & Narration Engine</span>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                {/* Narration Toggle */}
                <div className="p-3 rounded-xl bg-white/5 border border-white/5 space-y-2">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-semibold text-white flex items-center gap-1.5">
                      <Mic className="w-3.5 h-3.5 text-brand-400" />
                      <span>AI Voice Narration</span>
                    </span>
                    <input
                      type="checkbox"
                      checked={narrationEnabled}
                      onChange={(e) => setNarrationEnabled(e.target.checked)}
                      className="rounded accent-brand-500 cursor-pointer"
                    />
                  </div>
                  {narrationEnabled && (
                    <select
                      value={narrationVoice}
                      onChange={(e) => setNarrationVoice(e.target.value)}
                      className="w-full text-xs p-2 rounded-lg bg-dark-800 border border-white/10 text-slate-200"
                    >
                      {TTS_VOICES.map((v) => (
                        <option key={v.id} value={v.id}>{v.name}</option>
                      ))}
                    </select>
                  )}
                </div>

                {/* Ambient Chords Toggle */}
                <div className="p-3 rounded-xl bg-white/5 border border-white/5 space-y-2">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-semibold text-white flex items-center gap-1.5">
                      <Music className="w-3.5 h-3.5 text-brand-400" />
                      <span>Ambient Chords</span>
                    </span>
                    <input
                      type="checkbox"
                      checked={ambientEnabled}
                      onChange={(e) => setAmbientEnabled(e.target.checked)}
                      className="rounded accent-brand-500 cursor-pointer"
                    />
                  </div>
                  <p className="text-[11px] text-slate-400">
                    Generates soothing ambient drone chords with volume ducking.
                  </p>
                </div>
              </div>
            </div>

            {/* Duration Selector */}
            <div>
              <label className="block text-xs font-semibold uppercase text-slate-400 mb-2 flex items-center gap-1.5">
                <Clock className="w-3.5 h-3.5 text-brand-400" />
                <span>7. Video Duration</span>
              </label>
              <div className="flex gap-3">
                {[10, 15, 20, 30].map((sec) => (
                  <button
                    key={sec}
                    type="button"
                    onClick={() => setDuration(sec)}
                    className={`px-4 py-2 rounded-xl text-xs font-bold border transition-all ${
                      duration === sec
                        ? 'bg-brand-600 text-white border-brand-500'
                        : 'bg-white/5 text-slate-400 border-white/5 hover:bg-white/10'
                    }`}
                  >
                    {sec} Seconds
                  </button>
                ))}
              </div>
            </div>

            {/* Render Action Button */}
            <button
              onClick={() => renderMutation.mutate()}
              disabled={renderMutation.isPending || !selectedThoughtId}
              className="w-full py-4 rounded-2xl bg-gradient-to-r from-brand-600 to-indigo-600 hover:from-brand-500 hover:to-indigo-500 text-white text-sm font-bold flex items-center justify-center gap-2 shadow-lg shadow-purple-600/30 transition-all disabled:opacity-50"
            >
              {renderMutation.isPending ? (
                <>
                  <RefreshCw className="w-5 h-5 animate-spin" />
                  <span>Compiling 1080×1920 MP4 Video...</span>
                </>
              ) : (
                <>
                  <Film className="w-5 h-5" />
                  <span>Render Animated Video</span>
                </>
              )}
            </button>
          </div>
        </div>

        {/* Video Player & Real-Time Slide Preview Column */}
        <div className="lg:col-span-5 space-y-6">
          <div className="glass-panel rounded-3xl p-6 border border-white/5 flex flex-col items-center">
            {/* View Mode Switcher */}
            <div className="flex w-full mb-4 p-1 rounded-2xl bg-white/5 border border-white/5">
              <button
                onClick={() => setActiveTab('preview')}
                className={`flex-1 py-1.5 rounded-xl text-xs font-semibold flex items-center justify-center gap-1.5 transition-all ${
                  activeTab === 'preview'
                    ? 'bg-brand-600 text-white shadow-sm'
                    : 'text-slate-400 hover:text-white'
                }`}
              >
                <Eye className="w-3.5 h-3.5" />
                <span>Live Slide Preview</span>
              </button>
              <button
                onClick={() => setActiveTab('video')}
                className={`flex-1 py-1.5 rounded-xl text-xs font-semibold flex items-center justify-center gap-1.5 transition-all ${
                  activeTab === 'video'
                    ? 'bg-brand-600 text-white shadow-sm'
                    : 'text-slate-400 hover:text-white'
                }`}
              >
                <Play className="w-3.5 h-3.5" />
                <span>Rendered MP4</span>
              </button>
            </div>

            {/* 9:16 Vertical Screen Container */}
            <div className="w-full max-w-[280px] aspect-[9/16] rounded-2xl overflow-hidden bg-black border border-white/10 shadow-2xl relative flex items-center justify-center">
              {activeTab === 'preview' ? (
                previewLoading ? (
                  <div className="text-center p-6 space-y-3">
                    <RefreshCw className="w-8 h-8 text-brand-400 animate-spin mx-auto" />
                    <div className="text-xs text-slate-300">Composing typography...</div>
                  </div>
                ) : previewImageUrl ? (
                  <img
                    src={previewImageUrl}
                    alt="Slide Preview"
                    className="w-full h-full object-cover"
                  />
                ) : (
                  <div className="text-center p-6 space-y-3">
                    <Eye className="w-10 h-10 text-slate-600 mx-auto" />
                    <div className="text-xs text-slate-400">Select a thought to preview</div>
                  </div>
                )
              ) : (
                renderedVideo?.video_url ? (
                  <video
                    src={renderedVideo.video_url}
                    controls
                    autoPlay
                    loop
                    className="w-full h-full object-cover"
                  />
                ) : (
                  <div className="text-center p-6 space-y-3">
                    <Film className="w-12 h-12 text-slate-600 mx-auto" />
                    <div className="text-xs font-semibold text-slate-300">No Rendered Output</div>
                    <p className="text-[11px] text-slate-500">
                      Click "Render Animated Video" on the left to generate the real MP4 file.
                    </p>
                  </div>
                )
              )}
            </div>

            {/* Rendered Video Details & Action Buttons */}
            {renderedVideo && (
              <div className="w-full mt-6 space-y-3">
                <div className="glass-card rounded-xl p-3 text-xs text-slate-300 space-y-1">
                  <div><strong>Storage Key:</strong> {renderedVideo.storage_key}</div>
                  <div><strong>Resolution:</strong> {renderedVideo.width} × {renderedVideo.height} (9:16 Vertical)</div>
                  <div><strong>Duration:</strong> {renderedVideo.duration_seconds} seconds</div>
                  <div className="flex items-center gap-1.5 text-emerald-400">
                    <CheckCircle2 className="w-3.5 h-3.5" />
                    <span>H.264 & AAC Validated</span>
                  </div>
                </div>

                <div className="flex gap-2">
                  <a
                    href={renderedVideo.video_url}
                    download={`thoughtflow_${renderedVideo.storage_key}`}
                    className="flex-1 py-2.5 rounded-xl bg-white/10 hover:bg-white/15 text-white text-xs font-semibold flex items-center justify-center gap-1.5"
                  >
                    <Download className="w-4 h-4" />
                    <span>Download MP4</span>
                  </a>
                  <button
                    onClick={() => uploadMutation.mutate(renderedVideo.id)}
                    disabled={uploadMutation.isPending}
                    className="flex-1 py-2.5 rounded-xl bg-red-600 hover:bg-red-500 text-white text-xs font-semibold flex items-center justify-center gap-1.5"
                  >
                    <Upload className="w-4 h-4" />
                    <span>Publish YouTube</span>
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
