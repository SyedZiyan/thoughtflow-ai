import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  ListOrdered,
  Activity,
  AlertCircle,
  CheckCircle2,
  Clock,
  Filter,
  RefreshCw
} from 'lucide-react';
import { api, ActivityLog } from '../api/client';

export const ExecutionLogs: React.FC = () => {
  const [filterType, setFilterType] = useState('');

  const { data: logs, isLoading, refetch } = useQuery<ActivityLog[]>({
    queryKey: ['logs'],
    queryFn: () => api.getLogs(),
    refetchInterval: 10000,
  });

  const filtered = (logs || []).filter((l) => {
    if (!filterType) return true;
    return l.event_type === filterType;
  });

  return (
    <div className="max-w-6xl mx-auto space-y-8">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-white tracking-tight">Execution & Activity Logs</h1>
          <p className="text-sm text-slate-400 mt-1">
            Real-time audit trail of background worker runs, rendering jobs, and YouTube publishing events.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <select
            value={filterType}
            onChange={(e) => setFilterType(e.target.value)}
            className="px-3 py-2 rounded-xl bg-white/5 border border-white/10 text-white text-xs focus:outline-none focus:border-brand-500"
          >
            <option value="" className="bg-dark-800">All Events</option>
            <option value="video_rendered" className="bg-dark-800">Video Rendered</option>
            <option value="upload_success" className="bg-dark-800">Upload Success</option>
            <option value="upload_dry_run" className="bg-dark-800">Upload Dry Run</option>
            <option value="pipeline_error" className="bg-dark-800">Pipeline Errors</option>
            <option value="settings_updated" className="bg-dark-800">Settings Updated</option>
          </select>

          <button
            onClick={() => refetch()}
            className="p-2 rounded-xl bg-white/5 hover:bg-white/10 text-slate-300"
            title="Refresh logs"
          >
            <RefreshCw className="w-4 h-4" />
          </button>
        </div>
      </div>

      <div className="glass-panel rounded-3xl p-6 border border-white/5">
        {isLoading ? (
          <div className="p-12 text-center text-slate-400 text-xs">Loading execution logs...</div>
        ) : filtered.length === 0 ? (
          <div className="p-12 text-center text-slate-500 text-xs">No activity logs recorded yet.</div>
        ) : (
          <div className="space-y-3">
            {filtered.map((log) => {
              const isError = log.event_type.includes('error') || log.event_type.includes('fail');
              return (
                <div
                  key={log.id}
                  className={`glass-card rounded-2xl p-4 border flex flex-col sm:flex-row sm:items-center justify-between gap-3 ${
                    isError ? 'border-red-500/20 bg-red-950/10' : 'border-white/5'
                  }`}
                >
                  <div className="flex items-start gap-3">
                    <div className={`p-2 rounded-xl mt-0.5 ${
                      isError ? 'bg-red-500/20 text-red-400' : 'bg-brand-500/20 text-brand-400'
                    }`}>
                      {isError ? <AlertCircle className="w-4 h-4" /> : <Activity className="w-4 h-4" />}
                    </div>
                    <div>
                      <div className="text-xs font-bold text-white">{log.message}</div>
                      <div className="flex items-center gap-2 mt-1 text-[10px] text-slate-400 font-mono">
                        <span className="uppercase px-1.5 py-0.5 rounded bg-white/5">{log.event_type}</span>
                        {log.job_id && <span>Job ID: {log.job_id.slice(0, 8)}...</span>}
                      </div>
                    </div>
                  </div>

                  <div className="text-[11px] text-slate-500 font-mono self-end sm:self-center">
                    {new Date(log.created_at).toLocaleString()}
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
};
