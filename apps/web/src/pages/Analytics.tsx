import React from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  BarChart3,
  TrendingUp,
  Eye,
  ThumbsUp,
  MessageSquare,
  CheckCircle2,
  ExternalLink,
  Award,
  Video
} from 'lucide-react';
import { api, AnalyticsData } from '../api/client';

export const Analytics: React.FC = () => {
  const { data: analytics, isLoading } = useQuery<AnalyticsData>({
    queryKey: ['analytics'],
    queryFn: api.getAnalytics,
  });

  if (isLoading) {
    return <div className="p-12 text-center text-slate-400">Loading channel analytics...</div>;
  }

  if (!analytics) {
    return <div className="p-12 text-center text-slate-400">No analytics data available yet.</div>;
  }

  return (
    <div className="max-w-7xl mx-auto space-y-8">
      <div>
        <h1 className="text-2xl sm:text-3xl font-extrabold text-white tracking-tight">Performance Analytics</h1>
        <p className="text-sm text-slate-400 mt-1">
          Monitor your automated YouTube Shorts audience reach, view counts, and engagement by theme.
        </p>
      </div>

      {/* Metric Counters */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 sm:gap-6">
        <div className="glass-card rounded-2xl p-5 border border-white/5">
          <div className="flex items-center justify-between text-slate-400 mb-2">
            <span className="text-xs font-semibold uppercase">Total Views</span>
            <Eye className="w-4 h-4 text-brand-400" />
          </div>
          <div className="text-2xl font-bold text-white">{analytics.total_views.toLocaleString()}</div>
          <p className="text-[11px] text-slate-400 mt-1">Shorts feed impressions</p>
        </div>

        <div className="glass-card rounded-2xl p-5 border border-white/5">
          <div className="flex items-center justify-between text-slate-400 mb-2">
            <span className="text-xs font-semibold uppercase">Total Likes</span>
            <ThumbsUp className="w-4 h-4 text-emerald-400" />
          </div>
          <div className="text-2xl font-bold text-white">{analytics.total_likes.toLocaleString()}</div>
          <p className="text-[11px] text-slate-400 mt-1">Viewer appreciation</p>
        </div>

        <div className="glass-card rounded-2xl p-5 border border-white/5">
          <div className="flex items-center justify-between text-slate-400 mb-2">
            <span className="text-xs font-semibold uppercase">Published Shorts</span>
            <Video className="w-4 h-4 text-indigo-400" />
          </div>
          <div className="text-2xl font-bold text-white">{analytics.total_published_videos}</div>
          <p className="text-[11px] text-slate-400 mt-1">Active live videos</p>
        </div>

        <div className="glass-card rounded-2xl p-5 border border-white/5">
          <div className="flex items-center justify-between text-slate-400 mb-2">
            <span className="text-xs font-semibold uppercase">Pipeline Success</span>
            <CheckCircle2 className="w-4 h-4 text-emerald-400" />
          </div>
          <div className="text-2xl font-bold text-white">{analytics.success_rate}%</div>
          <p className="text-[11px] text-slate-400 mt-1">Render & upload reliability</p>
        </div>
      </div>

      {/* Performance by Theme Breakdown */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
        <div className="lg:col-span-7 glass-panel rounded-3xl p-6 sm:p-8 border border-white/5 space-y-6">
          <h3 className="text-base font-bold text-white flex items-center gap-2">
            <Award className="w-5 h-5 text-brand-400" />
            <span>Performance Breakdown by Category Theme</span>
          </h3>

          <div className="space-y-4">
            {analytics.performance_by_theme.map((pt) => {
              const maxCount = Math.max(...analytics.performance_by_theme.map((x) => x.count), 1);
              const percentage = Math.round((pt.count / maxCount) * 100);
              return (
                <div key={pt.theme} className="space-y-1.5">
                  <div className="flex items-center justify-between text-xs">
                    <span className="font-bold text-white capitalize">{pt.theme}</span>
                    <span className="text-slate-400">{pt.count} generated • {pt.views} views</span>
                  </div>
                  <div className="h-2.5 rounded-full bg-white/5 overflow-hidden">
                    <div
                      className="h-full rounded-full bg-gradient-to-r from-brand-600 to-indigo-500 transition-all duration-500"
                      style={{ width: `${Math.max(percentage, 8)}%` }}
                    />
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {/* Recent Uploads Performance */}
        <div className="lg:col-span-5 glass-panel rounded-3xl p-6 sm:p-8 border border-white/5 space-y-4">
          <h3 className="text-base font-bold text-white">Recent Shorts Performance</h3>

          <div className="space-y-3">
            {analytics.recent_uploads.map((ru) => (
              <div key={ru.id} className="glass-card rounded-2xl p-3.5 space-y-2">
                <div className="flex items-center justify-between">
                  <h4 className="text-xs font-bold text-white line-clamp-1">{ru.title}</h4>
                  {ru.youtube_url && (
                    <a href={ru.youtube_url} target="_blank" rel="noreferrer" className="text-red-400 hover:text-red-300">
                      <ExternalLink className="w-3.5 h-3.5" />
                    </a>
                  )}
                </div>
                <div className="flex items-center justify-between text-[11px] text-slate-400">
                  <span>{ru.views} views • {ru.likes} likes</span>
                  <span>{new Date(ru.published_at).toLocaleDateString()}</span>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};
