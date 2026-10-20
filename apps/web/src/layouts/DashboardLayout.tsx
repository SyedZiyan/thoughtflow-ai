import React, { useState } from 'react';
import { NavLink, Outlet, useLocation } from 'react-router-dom';
import {
  LayoutDashboard,
  Sparkles,
  Film,
  FolderKanban,
  CalendarDays,
  Bot,
  Youtube,
  BarChart3,
  ListOrdered,
  Settings,
  Menu,
  X,
  PlayCircle,
  Activity,
  CheckCircle2,
  AlertCircle
} from 'lucide-react';

const navigationItems = [
  { name: 'Overview', href: '/', icon: LayoutDashboard },
  { name: 'Generate Thought', href: '/generate', icon: Sparkles },
  { name: 'Video Studio', href: '/studio', icon: Film },
  { name: 'Content Library', href: '/library', icon: FolderKanban },
  { name: 'Publishing Calendar', href: '/calendar', icon: CalendarDays },
  { name: 'Automation', href: '/automation', icon: Bot },
  { name: 'YouTube Channel', href: '/youtube', icon: Youtube },
  { name: 'Analytics', href: '/analytics', icon: BarChart3 },
  { name: 'Execution Logs', href: '/logs', icon: ListOrdered },
  { name: 'Settings', href: '/settings', icon: Settings },
];

export const DashboardLayout: React.FC = () => {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const location = useLocation();

  return (
    <div className="flex h-screen overflow-hidden bg-[#0B0D13]">
      {/* Sidebar - Desktop */}
      <aside className="hidden md:flex md:w-64 md:flex-col glass-panel border-r border-white/5">
        <div className="flex items-center gap-3 px-6 py-5 border-b border-white/5">
          <img src="/logo.svg" alt="ThoughtFlow AI Logo" className="w-9 h-9 rounded-xl shadow-lg shadow-purple-500/20" />
          <div>
            <span className="text-lg font-bold bg-gradient-to-r from-purple-400 via-brand-400 to-indigo-300 bg-clip-text text-transparent tracking-tight">
              ThoughtFlow
            </span>
            <span className="text-xs ml-1 font-semibold px-1.5 py-0.5 rounded bg-brand-500/20 text-brand-300 border border-brand-500/30">
              AI
            </span>
          </div>
        </div>

        {/* Navigation list */}
        <nav className="flex-1 space-y-1 px-3 py-4 overflow-y-auto">
          {navigationItems.map((item) => {
            const Icon = item.icon;
            const isActive = location.pathname === item.href;
            return (
              <NavLink
                key={item.name}
                to={item.href}
                className={`flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium transition-all ${
                  isActive
                    ? 'bg-brand-600/20 text-brand-300 border border-brand-500/30 shadow-sm shadow-brand-500/10'
                    : 'text-slate-400 hover:text-slate-100 hover:bg-white/5'
                }`}
              >
                <Icon className={`w-4 h-4 ${isActive ? 'text-brand-400' : 'text-slate-400'}`} />
                <span>{item.name}</span>
              </NavLink>
            );
          })}
        </nav>

        {/* System status pill in sidebar footer */}
        <div className="p-4 border-t border-white/5">
          <div className="glass-card rounded-xl p-3 flex items-center justify-between">
            <div className="flex items-center gap-2">
              <span className="relative flex h-2.5 w-2.5">
                <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
                <span className="relative inline-flex rounded-full h-2.5 w-2.5 bg-emerald-500"></span>
              </span>
              <span className="text-xs font-medium text-slate-300">Daily Pipeline</span>
            </div>
            <span className="text-[10px] font-semibold uppercase tracking-wider px-2 py-0.5 rounded-full bg-brand-500/20 text-brand-300 border border-brand-500/30">
              Active
            </span>
          </div>
        </div>
      </aside>

      {/* Main Content Area */}
      <div className="flex-1 flex flex-col min-w-0 overflow-hidden">
        {/* Top Navbar */}
        <header className="glass-panel border-b border-white/5 px-6 py-4 flex items-center justify-between">
          <div className="flex items-center gap-3 md:hidden">
            <button
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              className="p-2 rounded-lg bg-white/5 text-slate-300 hover:text-white"
            >
              {mobileMenuOpen ? <X className="w-5 h-5" /> : <Menu className="w-5 h-5" />}
            </button>
            <div className="flex items-center gap-2">
              <img src="/logo.svg" alt="Logo" className="w-7 h-7" />
              <span className="font-bold text-slate-100">ThoughtFlow AI</span>
            </div>
          </div>

          <div className="hidden md:flex items-center gap-2 text-sm text-slate-400">
            <span>Platform</span>
            <span>/</span>
            <span className="text-slate-200 capitalize font-medium">
              {location.pathname === '/' ? 'Dashboard Overview' : location.pathname.slice(1).replace('-', ' ')}
            </span>
          </div>

          <div className="flex items-center gap-3">
            <div className="hidden sm:flex items-center gap-2 px-3 py-1.5 rounded-lg bg-white/5 border border-white/10 text-xs text-slate-300">
              <span className="text-brand-400 font-semibold">Dry-Run Ready:</span>
              <span>Local 9:16 MP4 Engine</span>
            </div>
            <div className="w-8 h-8 rounded-full bg-gradient-to-tr from-purple-600 to-indigo-500 flex items-center justify-center text-xs font-bold text-white shadow">
              TF
            </div>
          </div>
        </header>

        {/* Mobile menu overlay */}
        {mobileMenuOpen && (
          <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm md:hidden flex flex-col p-6">
            <div className="flex justify-between items-center mb-6">
              <div className="flex items-center gap-2">
                <img src="/logo.svg" alt="Logo" className="w-8 h-8" />
                <span className="font-bold text-lg text-white">ThoughtFlow AI</span>
              </div>
              <button onClick={() => setMobileMenuOpen(false)} className="p-2 text-slate-300">
                <X className="w-6 h-6" />
              </button>
            </div>
            <nav className="space-y-2">
              {navigationItems.map((item) => {
                const Icon = item.icon;
                const isActive = location.pathname === item.href;
                return (
                  <NavLink
                    key={item.name}
                    to={item.href}
                    onClick={() => setMobileMenuOpen(false)}
                    className={`flex items-center gap-3 px-4 py-3 rounded-xl text-base font-medium ${
                      isActive ? 'bg-brand-600 text-white' : 'text-slate-300 hover:bg-white/5'
                    }`}
                  >
                    <Icon className="w-5 h-5" />
                    <span>{item.name}</span>
                  </NavLink>
                );
              })}
            </nav>
          </div>
        )}

        {/* Page Content Body */}
        <main className="flex-1 overflow-y-auto p-4 sm:p-6 lg:p-8">
          <Outlet />
        </main>
      </div>
    </div>
  );
};
