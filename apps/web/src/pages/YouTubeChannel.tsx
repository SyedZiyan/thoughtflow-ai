import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Youtube,
  CheckCircle2,
  AlertTriangle,
  ExternalLink,
  ShieldCheck,
  RefreshCw,
  Users,
  Eye,
  Video,
  Info,
  LogOut,
  BarChart3,
  Check,
  AlertCircle
} from 'lucide-react';
import { api, YouTubeStatus, VideoAsset } from '../api/client';

export const YouTubeChannel: React.FC = () => {
  const queryClient = useQueryClient();
  const [syncMessage, setSyncMessage] = useState<string | null>(null);

  const { data: status, isLoading } = useQuery<YouTubeStatus>({
    queryKey: ['youtube-status'],
    queryFn: api.getYouTubeStatus,
  });

  const { data: videos } = useQuery<VideoAsset[]>({
    queryKey: ['videos'],
    queryFn: () => api.getVideos(),
  });

  const connectMutation = useMutation({
    mutationFn: () => api.connectYouTube(),
    onSuccess: (data) => {
      if (data.configured && data.auth_url) {
        window.location.href = data.auth_url;
      } else {
        alert(data.message || 'Please configure GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET in .env');
      }
    },
    onError: (err: any) => {
      alert(`Connect error: ${err.message}`);
    }
  });

  const disconnectMutation = useMutation({
    mutationFn: () => api.disconnectYouTube(),
    onSuccess: (res) => {
      alert(res.message);
      queryClient.invalidateQueries({ queryKey: ['youtube-status'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
    },
    onError: (err: any) => {
      alert(`Disconnect error: ${err.message}`);
    }
  });

  const syncMutation = useMutation({
    mutationFn: () => api.syncYouTubeMetrics(),
    onSuccess: (res) => {
      setSyncMessage(res.message || `Synced metrics for ${res.synced_count} videos`);
      queryClient.invalidateQueries({ queryKey: ['youtube-status'] });
      queryClient.invalidateQueries({ queryKey: ['analytics'] });
      setTimeout(() => setSyncMessage(null), 5000);
    },
    onError: (err: any) => {
      alert(`Sync error: ${err.message}`);
    }
  });

  if (isLoading) {
    return <div className="p-12 text-center text-slate-400">Loading YouTube integration status...</div>;
  }

  const publishedVideos = videos?.filter((v) => v.upload_status === 'published' || v.youtube_video_id) || [];

  return (
    <div className="max-w-5xl mx-auto space-y-8">
      <div>
        <h1 className="text-2xl sm:text-3xl font-extrabold text-white tracking-tight flex items-center gap-3">
          <span>YouTube Channel Integration</span>
          <span className="text-xs px-2.5 py-1 rounded-full bg-red-500/20 text-red-300 font-semibold border border-red-500/30">
            Phase 4 Publishing
          </span>
        </h1>
        <p className="text-sm text-slate-400 mt-1">
          OAuth 2.0 channel authorization, resumable chunked video uploads, quota telemetry, and metrics syncing.
        </p>
      </div>

      {syncMessage && (
        <div className="p-4 rounded-2xl bg-emerald-500/20 border border-emerald-500/30 text-emerald-300 text-xs font-semibold flex items-center gap-2">
          <CheckCircle2 className="w-4 h-4" />
          <span>{syncMessage}</span>
        </div>
      )}

      {/* Main Connection Status Card */}
      <div className="glass-panel rounded-3xl p-6 sm:p-8 border border-white/5 relative overflow-hidden">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-6 pb-6 border-b border-white/5">
          <div className="flex items-center gap-4">
            {status?.channel_thumbnail ? (
              <img
                src={status.channel_thumbnail}
                alt="Channel Avatar"
                className="w-16 h-16 rounded-2xl object-cover border border-white/10"
              />
            ) : (
              <div className="w-16 h-16 rounded-2xl bg-red-600/20 border border-red-500/30 flex items-center justify-center text-red-400">
                <Youtube className="w-8 h-8" />
              </div>
            )}
            <div>
              <div className="flex items-center gap-2">
                <h3 className="text-lg font-bold text-white">
                  {status?.channel_title || 'YouTube Channel'}
                </h3>
                <span className={`px-2.5 py-0.5 rounded-full text-xs font-semibold ${
                  status?.connected
                    ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30'
                    : 'bg-amber-500/20 text-amber-400 border border-amber-500/30'
                }`}>
                  {status?.connected ? 'Channel Connected' : 'Dry-Run Mode Active'}
                </span>
              </div>
              <p className="text-xs text-slate-400 mt-1">
                {status?.connected
                  ? `Channel ID: ${status.channel_id} • Authorized on ${new Date(status.connected_at || '').toLocaleDateString()}`
                  : 'Videos are safely validated and rendered locally without publishing to live YouTube.'}
              </p>
            </div>
          </div>

          <div className="flex items-center gap-3">
            {status?.connected ? (
              <>
                <button
                  onClick={() => syncMutation.mutate()}
                  disabled={syncMutation.isPending}
                  className="px-4 py-2.5 rounded-xl bg-white/10 hover:bg-white/15 text-white text-xs font-bold flex items-center gap-1.5 transition-all"
                  title="Sync latest live view and like counts from YouTube"
                >
                  <RefreshCw className={`w-3.5 h-3.5 ${syncMutation.isPending ? 'animate-spin' : ''}`} />
                  <span>Sync Metrics</span>
                </button>
                <button
                  onClick={() => {
                    if (confirm('Disconnect YouTube channel? Stored credentials will be safely revoked.')) {
                      disconnectMutation.mutate();
                    }
                  }}
                  disabled={disconnectMutation.isPending}
                  className="px-4 py-2.5 rounded-xl bg-red-600/20 hover:bg-red-600/30 text-red-400 border border-red-500/30 text-xs font-bold flex items-center gap-1.5 transition-all"
                >
                  <LogOut className="w-3.5 h-3.5" />
                  <span>Disconnect</span>
                </button>
              </>
            ) : (
              <button
                onClick={() => connectMutation.mutate()}
                disabled={connectMutation.isPending}
                className="px-5 py-2.5 rounded-xl bg-red-600 hover:bg-red-500 text-white text-xs font-bold flex items-center gap-2 transition-all shadow-lg shadow-red-600/30 disabled:opacity-50"
              >
                {connectMutation.isPending ? (
                  <>
                    <RefreshCw className="w-4 h-4 animate-spin" />
                    <span>Redirecting to Google...</span>
                  </>
                ) : (
                  <>
                    <Youtube className="w-4 h-4" />
                    <span>Connect via Google OAuth</span>
                  </>
                )}
              </button>
            )}
          </div>
        </div>

        {/* Channel Stats if Connected */}
        {status?.connected && (
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 pt-6">
            <div className="glass-card rounded-2xl p-4 flex items-center gap-3">
              <Users className="w-5 h-5 text-brand-400" />
              <div>
                <div className="text-xs text-slate-400">Subscribers</div>
                <div className="text-lg font-bold text-white">{status.subscriber_count?.toLocaleString() || 0}</div>
              </div>
            </div>

            <div className="glass-card rounded-2xl p-4 flex items-center gap-3">
              <Video className="w-5 h-5 text-indigo-400" />
              <div>
                <div className="text-xs text-slate-400">Total Videos</div>
                <div className="text-lg font-bold text-white">{status.video_count?.toLocaleString() || 0}</div>
              </div>
            </div>

            <div className="glass-card rounded-2xl p-4 flex items-center gap-3">
              <Eye className="w-5 h-5 text-emerald-400" />
              <div>
                <div className="text-xs text-slate-400">Lifetime Views</div>
                <div className="text-lg font-bold text-white">{status.view_count?.toLocaleString() || 0}</div>
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Published YouTube Shorts Catalog */}
      <div className="glass-panel rounded-3xl p-6 sm:p-8 border border-white/5 space-y-4">
        <h3 className="text-base font-bold text-white flex items-center justify-between">
          <span className="flex items-center gap-2">
            <Video className="w-5 h-5 text-red-500" />
            <span>Published Shorts History ({publishedVideos.length})</span>
          </span>
        </h3>

        {publishedVideos.length === 0 ? (
          <div className="p-8 text-center text-xs text-slate-500 border border-dashed border-white/10 rounded-2xl">
            No published YouTube Shorts yet. Render a video in Video Studio and click "Publish YouTube" to upload.
          </div>
        ) : (
          <div className="space-y-3">
            {publishedVideos.slice(0, 5).map((vid) => (
              <div
                key={vid.id}
                className="glass-card rounded-2xl p-4 border border-white/5 flex items-center justify-between gap-4"
              >
                <div className="space-y-1">
                  <div className="text-xs font-bold text-white flex items-center gap-2">
                    <span>{vid.title}</span>
                    <span className="text-[10px] px-2 py-0.5 rounded-full bg-emerald-500/20 text-emerald-400 font-mono">
                      {vid.upload_status}
                    </span>
                  </div>
                  <div className="text-[11px] text-slate-400">
                    Duration: {vid.duration_seconds}s • Storage: {vid.storage_key}
                  </div>
                </div>

                {vid.youtube_url && (
                  <a
                    href={vid.youtube_url}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="px-3 py-1.5 rounded-xl bg-white/10 hover:bg-white/15 text-white text-xs font-semibold flex items-center gap-1.5"
                  >
                    <span>Watch</span>
                    <ExternalLink className="w-3.5 h-3.5" />
                  </a>
                )}
              </div>
            ))}
          </div>
        )}
      </div>

      {/* API Compliance & Guidelines Card */}
      <div className="glass-panel rounded-3xl p-6 sm:p-8 border border-white/5 space-y-4">
        <div className="flex items-center gap-2 text-white font-bold text-base">
          <Info className="w-5 h-5 text-brand-400" />
          <span>YouTube API v3 Quotas, Verification & Production Readiness</span>
        </div>
        <div className="space-y-3 text-xs text-slate-400 leading-relaxed">
          <p>
            • <strong>Daily Upload Quota:</strong> YouTube Data API v3 grants default quota of 10,000 units/day. A video upload costs 1,600 units, allowing up to 6 video uploads daily per Google Cloud project.
          </p>
          <p>
            • <strong>OAuth Verification:</strong> New Google Cloud projects in 'Testing' mode allow uploads only to authorized test user accounts. To upload publicly to any channel, submit your project for OAuth Verification in the Google Cloud Console.
          </p>
          <p>
            • <strong>Dry-Run Mode:</strong> When credentials are unconfigured or <code>YOUTUBE_UPLOAD_ENABLED=False</code>, ThoughtFlow AI executes all thought generation, typography composition, and 9:16 MP4 video rendering in dry-run mode. This guarantees 100% zero-cost operation during local testing.
          </p>
        </div>
      </div>
    </div>
  );
};
