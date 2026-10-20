import React from 'react';
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
  Info
} from 'lucide-react';
import { api, YouTubeStatus } from '../api/client';

export const YouTubeChannel: React.FC = () => {
  const queryClient = useQueryClient();

  const { data: status, isLoading, refetch } = useQuery<YouTubeStatus>({
    queryKey: ['youtube-status'],
    queryFn: api.getYouTubeStatus,
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

  if (isLoading) {
    return <div className="p-12 text-center text-slate-400">Loading YouTube integration status...</div>;
  }

  return (
    <div className="max-w-5xl mx-auto space-y-8">
      <div>
        <h1 className="text-2xl sm:text-3xl font-extrabold text-white tracking-tight">YouTube Channel Integration</h1>
        <p className="text-sm text-slate-400 mt-1">
          Manage your channel connection, verify OAuth 2.0 authorization, and inspect quota status.
        </p>
      </div>

      {/* Main Connection Status Card */}
      <div className="glass-panel rounded-3xl p-6 sm:p-8 border border-white/5 relative overflow-hidden">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-6 pb-6 border-b border-white/5">
          <div className="flex items-center gap-4">
            <div className="w-14 h-14 rounded-2xl bg-red-600/20 border border-red-500/30 flex items-center justify-center text-red-400">
              <Youtube className="w-8 h-8" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h3 className="text-lg font-bold text-white">
                  {status?.channel_title || 'YouTube Shorts Channel'}
                </h3>
                <span className={`px-2.5 py-0.5 rounded-full text-xs font-semibold ${
                  status?.connected
                    ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30'
                    : 'bg-amber-500/20 text-amber-400 border border-amber-500/30'
                }`}>
                  {status?.connected ? 'Connected' : 'Dry-Run Mode Active'}
                </span>
              </div>
              <p className="text-xs text-slate-400 mt-1">
                {status?.connected
                  ? `Authorized on ${new Date(status.connected_at || '').toLocaleDateString()}`
                  : 'Videos are safely validated and rendered locally without publishing to live YouTube.'}
              </p>
            </div>
          </div>

          <div>
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
                  <span>{status?.connected ? 'Reconnect Channel' : 'Connect via Google OAuth'}</span>
                </>
              )}
            </button>
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
