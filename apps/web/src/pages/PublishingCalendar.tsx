import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  CalendarDays,
  Clock,
  CheckCircle2,
  Calendar as CalendarIcon,
  ChevronLeft,
  ChevronRight,
  Sparkles,
  Youtube
} from 'lucide-react';
import { api, DashboardMetrics } from '../api/client';

export const PublishingCalendar: React.FC = () => {
  const { data: dashboard, isLoading } = useQuery<DashboardMetrics>({
    queryKey: ['dashboard'],
    queryFn: api.getDashboard,
  });

  const [currentDate] = useState(new Date());

  // Generate 14 days calendar preview
  const days = Array.from({ length: 14 }).map((_, i) => {
    const d = new Date();
    d.setDate(d.getDate() - 2 + i);
    const dateStr = d.toISOString().split('T')[0];
    const isToday = i === 2;
    const isPast = i < 2;

    const themes = ['discipline', 'motivation', 'consistency', 'resilience', 'success', 'mindfulness'];
    const themePick = themes[i % themes.length];

    let status = isPast ? 'published' : (isToday ? 'in_progress' : 'scheduled');

    return {
      date: dateStr,
      dayName: d.toLocaleDateString('en-US', { weekday: 'short' }),
      dayNumber: d.getDate(),
      month: d.toLocaleDateString('en-US', { month: 'short' }),
      theme: themePick,
      status,
      time: '09:00 UTC',
      isToday,
      isPast
    };
  });

  return (
    <div className="max-w-7xl mx-auto space-y-8">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-white tracking-tight">Publishing Calendar</h1>
          <p className="text-sm text-slate-400 mt-1">
            Visual publishing schedule for daily YouTube Shorts automation. All times rendered in UTC.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <span className="px-3 py-1.5 rounded-xl bg-white/5 border border-white/10 text-xs text-slate-300 font-mono">
            Timezone: UTC (Auto-converted)
          </span>
        </div>
      </div>

      {/* Calendar Grid */}
      <div className="glass-panel rounded-3xl p-6 sm:p-8 border border-white/5 space-y-6">
        <div className="flex items-center justify-between">
          <h3 className="text-base font-bold text-white flex items-center gap-2">
            <CalendarDays className="w-5 h-5 text-brand-400" />
            <span>14-Day Automation Horizon</span>
          </h3>
          <div className="flex items-center gap-3 text-xs text-slate-400">
            <span className="flex items-center gap-1.5">
              <span className="w-2.5 h-2.5 rounded-full bg-emerald-500"></span>
              Published
            </span>
            <span className="flex items-center gap-1.5">
              <span className="w-2.5 h-2.5 rounded-full bg-brand-500"></span>
              Scheduled
            </span>
          </div>
        </div>

        <div className="grid grid-cols-2 sm:grid-cols-4 lg:grid-cols-7 gap-3 sm:gap-4">
          {days.map((d, index) => (
            <div
              key={index}
              className={`rounded-2xl p-4 border transition-all flex flex-col justify-between min-h-[140px] ${
                d.isToday
                  ? 'bg-brand-600/20 border-brand-500 shadow-md shadow-brand-500/20'
                  : 'glass-card border-white/5'
              }`}
            >
              <div>
                <div className="flex items-center justify-between mb-2">
                  <span className="text-[11px] font-bold text-slate-400 uppercase">{d.dayName}</span>
                  {d.isToday && (
                    <span className="px-1.5 py-0.5 rounded text-[9px] font-bold bg-brand-500 text-white uppercase">
                      Today
                    </span>
                  )}
                </div>
                <div className="text-xl font-extrabold text-white">
                  {d.dayNumber} <span className="text-xs font-normal text-slate-400">{d.month}</span>
                </div>
              </div>

              <div className="space-y-1.5 mt-3 pt-3 border-t border-white/5">
                <div className="text-[10px] font-bold uppercase tracking-wider text-brand-300 capitalize truncate">
                  {d.theme}
                </div>
                <div className="flex items-center justify-between text-[10px] text-slate-400">
                  <span>{d.time}</span>
                  <span className={`px-1.5 py-0.5 rounded font-semibold capitalize ${
                    d.status === 'published'
                      ? 'bg-emerald-500/20 text-emerald-300'
                      : 'bg-brand-500/20 text-brand-300'
                  }`}>
                    {d.status}
                  </span>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};
