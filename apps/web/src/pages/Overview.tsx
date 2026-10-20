import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import {
  Sparkles,
  PlayCircle,
  Video,
  CheckCircle2,
  Clock,
  AlertTriangle,
  Youtube,
  Calendar,
  ArrowRight,
  TrendingUp,
  RefreshCw
} from 'lucide-react';
import { api, DashboardMetrics } from '../api/client';

export const Overview: React.FC = () => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [selectedVideoUrl, setSelectedVideoUrl] = useState<string | null>(null);

  const { data: dashboard, isLoading, error, refetch } = useQuery<DashboardMetrics>({
    queryKey: ['dashboard'],
    queryFn: api.getDashboard,
    refetchInterval: 15000,
  });

  const runPipelineMutation = useMutation({
    mutationFn: () => api.runPipeline(),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      alert('Pipeline triggered successfully! Video rendering initiated.');
    },
    onError: (err: any) => {
      alert(`Pipeline error: ${err.message}`);
    }
  });

  if (isLoading) {
    return (
      <div className="flex h-64 items-center justify-center">
        <div className="flex items-center gap-3 text-brand-400">
          <RefreshCw className="w-6 h-6 animate-spin" />
          <span className="text-sm font-medium text-slate-300">Loading ThoughtFlow operational state...</span>
        </div>
      </div>
    );
  }

  if (error || !dashboard) {
    return (
      <div className="glass-panel p-6 rounded-2xl border border-red-500/20 text-red-400">
        <div className="flex items-center gap-3">
          <AlertTriangle className="w-6 h-6" />
          <h3 className="font-semibold">Unable to load dashboard data</h3>
        </div>
        <p className="text-sm text-slate-400 mt-2">Ensure the FastAPI backend server is running on http://localhost:8000.</p>
        <button onClick={() => refetch()} className="mt-4 px-4 py-2 bg-red-500/20 rounded-lg text-sm font-medium hover:bg-red-500/30">
          Retry Connection
        </button>
      </div>
    );
  }

  return (
    <div className="space-y-8 max-w-7xl mx-auto">
      {/* Hero Action Header */}
      <div className="glass-panel rounded-3xl p-6 sm:p-8 relative overflow-hidden brand-glow border border-brand-500/20">
        <div className="relative z-10 flex flex-col md:flex-row md:items-center justify-between gap-6">
          <div>
            <div className="flex items-center gap-2 mb-2">
              <span className="px-3 py-1 rounded-full text-xs font-semibold bg-brand-500/20 text-brand-300 border border-brand-500/30">
                Daily Automation Engine
              </span>
              <span className="text-xs text-slate-400">• Personal Creator Edition</span>
            </div>
            <h1 className="text-2xl sm:text-3xl font-extrabold text-white tracking-tight">
              One Thought Every Day. <span className="bg-gradient-to-r from-brand-400 to-indigo-300 bg-clip-text text-transparent">Fully Automated.</span>
            </h1>
            <p className="text-sm text-slate-400 max-w-2xl mt-2">
              Next scheduled execution: <strong className="text-slate-200">{dashboard.next_scheduled_execution || 'Daily at 07:00 UTC'}</strong>.
              Your pipeline generates, animates vertical 9:16 MP4s, and manages YouTube publishing.
            </p>
          </div>

          <div className="flex flex-wrap items-center gap-3">
            <button
              onClick={() => navigate('/generate')}
              className="flex items-center gap-2 px-4 py-2.5 rounded-xl bg-white/10 hover:bg-white/15 text-white text-sm font-semibold transition-all border border-white/10 shadow-sm"
            >
              <Sparkles className="w-4 h-4 text-brand-400" />
              <span>Generate Thought</span>
            </button>
            <button
              onClick={() => runPipelineMutation.mutate()}
              disabled={runPipelineMutation.isPending}
              className="flex items-center gap-2 px-5 py-2.5 rounded-xl bg-gradient-to-r from-brand-600 to-indigo-600 hover:from-brand-500 hover:to-indigo-500 text-white text-sm font-semibold transition-all shadow-lg shadow-purple-600/30 disabled:opacity-50"
            >
              {runPipelineMutation.isPending ? (
                <>
                  <RefreshCw className="w-4 h-4 animate-spin" />
                  <span>Processing Pipeline...</span>
                </>
              ) : (
                <>
                  <PlayCircle className="w-4 h-4" />
                  <span>Run Pipeline Now</span>
                </>
              )}
            </button>
          </div>
        </div>
      </div>

      {/* Operational Metrics Cards */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 sm:gap-6">
        <div className="glass-card rounded-2xl p-5 border border-white/5">
          <div className="flex items-center justify-between text-slate-400 mb-3">
            <span className="text-xs font-semibold uppercase tracking-wider">Today's Generation</span>
            <Sparkles className="w-4 h-4 text-brand-400" />
          </div>
          <div className="flex items-baseline gap-2">
            <span className="text-2xl font-bold text-white capitalize">{dashboard.todays_generation_status}</span>
          </div>
          <p className="text-xs text-slate-400 mt-2">AI structured thought status</p>
        </div>

        <div className="glass-card rounded-2xl p-5 border border-white/5">
          <div className="flex items-center justify-between text-slate-400 mb-3">
            <span className="text-xs font-semibold uppercase tracking-wider">Today's Publishing</span>
            <Youtube className="w-4 h-4 text-red-400" />
          </div>
          <div className="flex items-baseline gap-2">
            <span className="text-2xl font-bold text-white capitalize">{dashboard.todays_publishing_status}</span>
          </div>
          <p className="text-xs text-slate-400 mt-2">YouTube Shorts publication</p>
        </div>

        <div className="glass-card rounded-2xl p-5 border border-white/5">
          <div className="flex items-center justify-between text-slate-400 mb-3">
            <span className="text-xs font-semibold uppercase tracking-wider">Total Rendered</span>
            <Video className="w-4 h-4 text-indigo-400" />
          </div>
          <div className="flex items-baseline gap-2">
            <span className="text-2xl font-bold text-white">{dashboard.total_videos}</span>
            <span className="text-xs text-slate-400">videos</span>
          </div>
          <p className="text-xs text-slate-400 mt-2">1080x1920 animated MP4s</p>
        </div>

        <div className="glass-card rounded-2xl p-5 border border-white/5">
          <div className="flex items-center justify-between text-slate-400 mb-3">
            <span className="text-xs font-semibold uppercase tracking-wider">Channel Status</span>
            <CheckCircle2 className={`w-4 h-4 ${dashboard.youtube_connected ? 'text-emerald-400' : 'text-amber-400'}`} />
          </div>
          <div className="flex items-baseline gap-2">
            <span className="text-2xl font-bold text-white">
              {dashboard.youtube_connected ? 'Connected' : 'Dry Run Ready'}
            </span>
          </div>
          <p className="text-xs text-slate-400 mt-2">
            {dashboard.youtube_connected ? 'OAuth tokens active' : 'Safe local validation active'}
          </p>
        </div>
      </div>

      {/* Main Grid: Latest Videos & Upcoming Schedule */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Latest Rendered Videos */}
        <div className="lg:col-span-2 glass-panel rounded-3xl p-6 border border-white/5">
          <div className="flex items-center justify-between mb-6">
            <div>
              <h2 className="text-lg font-bold text-white">Latest Rendered Videos</h2>
              <p className="text-xs text-slate-400">Animated vertical 9:16 MP4 assets ready for preview or upload</p>
            </div>
            <button
              onClick={() => navigate('/studio')}
              className="flex items-center gap-1.5 text-xs font-medium text-brand-400 hover:text-brand-300"
            >
              <span>Video Studio</span>
              <ArrowRight className="w-3.5 h-3.5" />
            </button>
          </div>

          {dashboard.latest_videos.length === 0 ? (
            <div className="text-center py-12 glass-card rounded-2xl p-8 border border-dashed border-white/10">
              <Video className="w-10 h-10 text-slate-500 mx-auto mb-3" />
              <h4 className="text-sm font-semibold text-slate-200">No videos rendered yet</h4>
              <p className="text-xs text-slate-400 max-w-sm mx-auto mt-1 mb-4">
                Click "Run Pipeline Now" or navigate to Video Studio to generate your first animated video.
              </p>
              <button
                onClick={() => navigate('/studio')}
                className="px-4 py-2 rounded-xl bg-brand-600 hover:bg-brand-500 text-white text-xs font-semibold"
              >
                Open Studio
              </button>
            </div>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              {dashboard.latest_videos.map((vid) => (
                <div key={vid.id} className="glass-card rounded-2xl p-4 border border-white/5 flex flex-col justify-between">
                  <div>
                    <div className="flex items-center justify-between text-xs text-slate-400 mb-2">
                      <span className="px-2 py-0.5 rounded bg-white/5 font-mono text-[10px]">{vid.duration_seconds}s • 9:16</span>
                      <span className={`px-2 py-0.5 rounded-full text-[10px] font-semibold ${
                        vid.upload_status === 'published' ? 'bg-emerald-500/20 text-emerald-400' : 'bg-brand-500/20 text-brand-300'
                      }`}>
                        {vid.upload_status}
                      </span>
                    </div>
                    <h3 className="text-sm font-bold text-white line-clamp-1">{vid.title}</h3>
                    <p className="text-xs text-slate-400 line-clamp-2 mt-1">{vid.description}</p>
                  </div>

                  <div className="mt-4 pt-3 border-t border-white/5 flex items-center justify-between">
                    <button
                      onClick={() => setSelectedVideoUrl(vid.video_url || null)}
                      className="flex items-center gap-1.5 text-xs font-semibold text-brand-400 hover:text-brand-300"
                    >
                      <PlayCircle className="w-4 h-4" />
                      <span>Preview Video</span>
                    </button>
                    {vid.youtube_url && (
                      <a
                        href={vid.youtube_url}
                        target="_blank"
                        rel="noreferrer"
                        className="text-xs text-red-400 hover:text-red-300 font-medium"
                      >
                        YouTube Link
                      </a>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Upcoming Publishing Calendar & Activity */}
        <div className="space-y-6">
          {/* Upcoming Schedule Card */}
          <div className="glass-panel rounded-3xl p-6 border border-white/5">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-base font-bold text-white flex items-center gap-2">
                <Calendar className="w-4 h-4 text-brand-400" />
                <span>Upcoming Publishing</span>
              </h2>
              <button
                onClick={() => navigate('/calendar')}
                className="text-xs font-medium text-brand-400 hover:text-brand-300"
              >
                View Full
              </button>
            </div>
            <div className="space-y-3">
              {dashboard.upcoming_schedule.map((item, idx) => (
                <div key={idx} className="glass-card rounded-xl p-3 flex items-center justify-between">
                  <div>
                    <div className="text-xs font-bold text-white">{item.date}</div>
                    <div className="text-[11px] text-slate-400 capitalize">{item.theme} • {item.time} UTC</div>
                  </div>
                  <span className="px-2 py-0.5 rounded text-[10px] font-semibold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                    Scheduled
                  </span>
                </div>
              ))}
            </div>
          </div>

          {/* Activity Log Mini Stream */}
          <div className="glass-panel rounded-3xl p-6 border border-white/5">
            <h2 className="text-base font-bold text-white mb-4">Recent Activity</h2>
            <div className="space-y-3">
              {dashboard.recent_activity.slice(0, 4).map((act) => (
                <div key={act.id} className="text-xs border-l-2 border-brand-500/40 pl-3 py-1">
                  <div className="text-slate-200 font-medium">{act.message}</div>
                  <div className="text-[10px] text-slate-500 mt-0.5">
                    {new Date(act.created_at).toLocaleTimeString()} • {act.event_type}
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>

      {/* Video Modal Player */}
      {selectedVideoUrl && (
        <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-md flex items-center justify-center p-4">
          <div className="glass-panel rounded-3xl p-6 max-w-sm w-full border border-white/10 relative">
            <button
              onClick={() => setSelectedVideoUrl(null)}
              className="absolute top-4 right-4 p-1.5 rounded-full bg-white/10 text-slate-300 hover:text-white"
            >
              ✕
            </button>
            <h3 className="text-sm font-bold text-white mb-3">Vertical 9:16 Animated MP4 Preview</h3>
            <div className="rounded-2xl overflow-hidden bg-black aspect-[9/16] w-full flex items-center justify-center border border-white/10">
              <video
                src={selectedVideoUrl}
                controls
                autoPlay
                loop
                className="w-full h-full object-cover"
              />
            </div>
            <div className="mt-4 flex justify-end">
              <a
                href={selectedVideoUrl}
                download
                className="px-4 py-2 rounded-xl bg-brand-600 hover:bg-brand-500 text-white text-xs font-semibold"
              >
                Download MP4
              </a>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
