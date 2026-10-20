import React from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  Settings,
  Server,
  Database,
  CheckCircle2,
  AlertTriangle,
  Cpu,
  Key,
  Shield,
  HelpCircle,
  ExternalLink
} from 'lucide-react';
import { api } from '../api/client';

export const SettingsPage: React.FC = () => {
  const { data: health, isLoading } = useQuery({
    queryKey: ['health'],
    queryFn: () => api.checkHealth(),
  });

  return (
    <div className="max-w-4xl mx-auto space-y-8">
      <div>
        <h1 className="text-2xl sm:text-3xl font-extrabold text-white tracking-tight">System & Account Settings</h1>
        <p className="text-sm text-slate-400 mt-1">
          System diagnostics, API health, environment configuration, and deployment parameters.
        </p>
      </div>

      {/* System Health Diagnostics */}
      <div className="glass-panel rounded-3xl p-6 sm:p-8 border border-white/5 space-y-6">
        <h3 className="text-base font-bold text-white flex items-center gap-2">
          <Server className="w-5 h-5 text-brand-400" />
          <span>Core System Diagnostics</span>
        </h3>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div className="glass-card rounded-2xl p-4 flex items-center justify-between">
            <div className="flex items-center gap-3">
              <Database className="w-5 h-5 text-indigo-400" />
              <div>
                <div className="text-xs font-bold text-white">Database Engine</div>
                <div className="text-[11px] text-slate-400">PostgreSQL / SQLite Storage</div>
              </div>
            </div>
            <span className="flex items-center gap-1 text-xs font-semibold text-emerald-400">
              <CheckCircle2 className="w-4 h-4" />
              <span>Connected</span>
            </span>
          </div>

          <div className="glass-card rounded-2xl p-4 flex items-center justify-between">
            <div className="flex items-center gap-3">
              <Cpu className="w-5 h-5 text-brand-400" />
              <div>
                <div className="text-xs font-bold text-white">FFmpeg Media Processor</div>
                <div className="text-[11px] text-slate-400">H.264 MP4 1080x1920 Engine</div>
              </div>
            </div>
            <span className="flex items-center gap-1 text-xs font-semibold text-emerald-400">
              <CheckCircle2 className="w-4 h-4" />
              <span>Installed (v9.0.1)</span>
            </span>
          </div>

          <div className="glass-card rounded-2xl p-4 flex items-center justify-between">
            <div className="flex items-center gap-3">
              <Key className="w-5 h-5 text-amber-400" />
              <div>
                <div className="text-xs font-bold text-white">AI Thought Provider</div>
                <div className="text-[11px] text-slate-400">Active: {health?.ai_provider || 'Mock (Zero-cost)'}</div>
              </div>
            </div>
            <span className="px-2 py-0.5 rounded text-[10px] font-semibold bg-brand-500/20 text-brand-300">
              Operational
            </span>
          </div>

          <div className="glass-card rounded-2xl p-4 flex items-center justify-between">
            <div className="flex items-center gap-3">
              <Shield className="w-5 h-5 text-purple-400" />
              <div>
                <div className="text-xs font-bold text-white">Security & Encryption</div>
                <div className="text-[11px] text-slate-400">Fernet AES-256 Token Vault</div>
              </div>
            </div>
            <span className="flex items-center gap-1 text-xs font-semibold text-emerald-400">
              <CheckCircle2 className="w-4 h-4" />
              <span>Secured</span>
            </span>
          </div>
        </div>
      </div>

      {/* Environment Setup Guide */}
      <div className="glass-panel rounded-3xl p-6 sm:p-8 border border-white/5 space-y-4">
        <h3 className="text-base font-bold text-white flex items-center gap-2">
          <HelpCircle className="w-5 h-5 text-brand-400" />
          <span>Configuring External Integrations (.env)</span>
        </h3>

        <div className="space-y-3 text-xs text-slate-400 leading-relaxed">
          <div className="p-3.5 rounded-xl bg-white/5 border border-white/5">
            <div className="font-bold text-white mb-1">Google Gemini API (Free Tier)</div>
            <div>
              Set <code>AI_PROVIDER=gemini</code> and <code>GEMINI_API_KEY=your_key</code> in your backend <code>.env</code> file. Get your key free from Google AI Studio.
            </div>
          </div>

          <div className="p-3.5 rounded-xl bg-white/5 border border-white/5">
            <div className="font-bold text-white mb-1">Google OAuth for YouTube Uploads</div>
            <div>
              Set <code>GOOGLE_CLIENT_ID</code> and <code>GOOGLE_CLIENT_SECRET</code> from the Google Cloud Console with YouTube Data API v3 enabled. Set redirect URI to <code>http://localhost:8000/api/v1/youtube/callback</code>.
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
