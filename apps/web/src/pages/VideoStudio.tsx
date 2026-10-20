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
  Layers
} from 'lucide-react';
import { api, Thought, VideoAsset } from '../api/client';

const VISUAL_THEMES = [
  { id: 'gradient_purple', name: 'Cosmic Purple', preview: 'from-purple-950 to-indigo-950' },
  { id: 'geometric_dark', name: 'Emerald Obsidian', preview: 'from-emerald-950 to-slate-950' },
  { id: 'nebula', name: 'Midnight Cyan', preview: 'from-blue-950 to-slate-900' },
  { id: 'gold_luxury', name: 'Bronze Luxury', preview: 'from-amber-950 to-neutral-950' },
  { id: 'minimal_slate', name: 'Graphite Minimal', preview: 'from-slate-900 to-zinc-900' },
];

const ANIMATION_STYLES = [
  { id: 'smooth_zoom', name: 'Slow Cinematic Zoom', desc: 'Ken Burns slow continuous punch-in' },
  { id: 'kinetic_pan', name: 'Kinetic Vertical Pan', desc: 'Subtle downward framed tracking' }
];

export const VideoStudio: React.FC = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [selectedThoughtId, setSelectedThoughtId] = useState<string>('');
  const [visualTheme, setVisualTheme] = useState('gradient_purple');
  const [animationStyle, setAnimationStyle] = useState('smooth_zoom');
  const [duration, setDuration] = useState(15);
  const [narrationEnabled, setNarrationEnabled] = useState(false);
  const [renderedVideo, setRenderedVideo] = useState<VideoAsset | null>(null);

  // Fetch all thoughts to select from
  const { data: thoughts, isLoading: thoughtsLoading } = useQuery<Thought[]>({
    queryKey: ['thoughts'],
    queryFn: () => api.getThoughts(),
  });

  // Set initial selected thought from location state or first item
  useEffect(() => {
    if (location.state?.thoughtId) {
      setSelectedThoughtId(location.state.thoughtId);
    } else if (thoughts && thoughts.length > 0 && !selectedThoughtId) {
      setSelectedThoughtId(thoughts[0].id);
    }
  }, [thoughts, location.state]);

  const selectedThought = thoughts?.find((t) => t.id === selectedThoughtId);

  const renderMutation = useMutation({
    mutationFn: () => {
      if (!selectedThoughtId) throw new Error('Please select a thought first');
      return api.renderVideo({
        thought_id: selectedThoughtId,
        visual_theme: visualTheme,
        animation_style: animationStyle,
        duration_seconds: duration,
        narration_enabled: narrationEnabled,
      });
    },
    onSuccess: (video) => {
      setRenderedVideo(video);
      queryClient.invalidateQueries({ queryKey: ['videos'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      alert('1080x1920 Animated Vertical MP4 rendered successfully!');
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
        <h1 className="text-2xl sm:text-3xl font-extrabold text-white tracking-tight">Video Studio</h1>
        <p className="text-sm text-slate-400 mt-1">
          Compose, style, and render animated vertical 9:16 MP4s with Pillow aesthetics and FFmpeg motion filters.
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

            {/* Animation Style */}
            <div>
              <label className="block text-xs font-semibold uppercase text-slate-400 mb-2 flex items-center gap-1.5">
                <Layers className="w-3.5 h-3.5 text-brand-400" />
                <span>3. Animation Effect</span>
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

            {/* Duration Selector */}
            <div>
              <label className="block text-xs font-semibold uppercase text-slate-400 mb-2 flex items-center gap-1.5">
                <Clock className="w-3.5 h-3.5 text-brand-400" />
                <span>4. Video Duration</span>
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

            {/* Render Button */}
            <button
              onClick={() => renderMutation.mutate()}
              disabled={renderMutation.isPending || !selectedThoughtId}
              className="w-full py-3.5 rounded-2xl bg-gradient-to-r from-brand-600 to-indigo-600 hover:from-brand-500 hover:to-indigo-500 text-white text-sm font-bold flex items-center justify-center gap-2 shadow-lg shadow-purple-600/30 transition-all disabled:opacity-50"
            >
              {renderMutation.isPending ? (
                <>
                  <RefreshCw className="w-5 h-5 animate-spin" />
                  <span>Rendering 1080x1920 MP4 Video...</span>
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

        {/* Video Player & Inspection Column */}
        <div className="lg:col-span-5 space-y-6">
          <div className="glass-panel rounded-3xl p-6 border border-white/5 flex flex-col items-center">
            <h3 className="text-sm font-bold text-white mb-4 w-full text-left flex items-center justify-between">
              <span>Vertical 9:16 Player Preview</span>
              {renderedVideo && (
                <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-emerald-500/20 text-emerald-300">
                  Ready ({renderedVideo.duration_seconds}s)
                </span>
              )}
            </h3>

            <div className="w-full max-w-[280px] aspect-[9/16] rounded-2xl overflow-hidden bg-black border border-white/10 shadow-2xl relative flex items-center justify-center">
              {renderedVideo?.video_url ? (
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
              )}
            </div>

            {renderedVideo && (
              <div className="w-full mt-6 space-y-3">
                <div className="glass-card rounded-xl p-3 text-xs text-slate-300 space-y-1">
                  <div><strong>Storage Key:</strong> {renderedVideo.storage_key}</div>
                  <div><strong>Resolution:</strong> {renderedVideo.width} × {renderedVideo.height}</div>
                  <div><strong>Status:</strong> {renderedVideo.render_status}</div>
                </div>

                <div className="flex gap-2">
                  <a
                    href={renderedVideo.video_url}
                    download
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
