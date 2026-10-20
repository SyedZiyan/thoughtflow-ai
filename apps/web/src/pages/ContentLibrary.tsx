import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  FolderKanban,
  Search,
  Filter,
  PlayCircle,
  Film,
  Trash2,
  ExternalLink,
  RefreshCw,
  Sparkles,
  Download
} from 'lucide-react';
import { api, Thought, VideoAsset } from '../api/client';

export const ContentLibrary: React.FC = () => {
  const queryClient = useQueryClient();
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedTheme, setSelectedTheme] = useState('');
  const [activeTab, setActiveTab] = useState<'thoughts' | 'videos'>('videos');
  const [previewVideoUrl, setPreviewVideoUrl] = useState<string | null>(null);

  const { data: thoughts, isLoading: thoughtsLoading } = useQuery<Thought[]>({
    queryKey: ['thoughts', selectedTheme],
    queryFn: () => api.getThoughts(selectedTheme || undefined),
  });

  const { data: videos, isLoading: videosLoading } = useQuery<VideoAsset[]>({
    queryKey: ['videos'],
    queryFn: () => api.getVideos(),
  });

  const deleteThoughtMutation = useMutation({
    mutationFn: (id: string) => api.deleteThought(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['thoughts'] });
      alert('Thought deleted.');
    }
  });

  const filteredThoughts = (thoughts || []).filter((t) => {
    const matchesSearch =
      t.text.toLowerCase().includes(searchTerm.toLowerCase()) ||
      t.title.toLowerCase().includes(searchTerm.toLowerCase());
    return matchesSearch;
  });

  const filteredVideos = (videos || []).filter((v) => {
    return (
      v.title.toLowerCase().includes(searchTerm.toLowerCase()) ||
      v.description.toLowerCase().includes(searchTerm.toLowerCase())
    );
  });

  return (
    <div className="max-w-7xl mx-auto space-y-8">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-white tracking-tight">Content Library</h1>
          <p className="text-sm text-slate-400 mt-1">
            Search, preview, and manage generated daily thoughts and animated video assets.
          </p>
        </div>

        {/* Tab switch */}
        <div className="flex items-center p-1 rounded-2xl bg-white/5 border border-white/5 w-fit">
          <button
            onClick={() => setActiveTab('videos')}
            className={`px-4 py-2 rounded-xl text-xs font-bold transition-all ${
              activeTab === 'videos'
                ? 'bg-brand-600 text-white shadow'
                : 'text-slate-400 hover:text-white'
            }`}
          >
            Videos ({videos?.length || 0})
          </button>
          <button
            onClick={() => setActiveTab('thoughts')}
            className={`px-4 py-2 rounded-xl text-xs font-bold transition-all ${
              activeTab === 'thoughts'
                ? 'bg-brand-600 text-white shadow'
                : 'text-slate-400 hover:text-white'
            }`}
          >
            Thoughts ({thoughts?.length || 0})
          </button>
        </div>
      </div>

      {/* Filter and Search Bar */}
      <div className="glass-panel rounded-2xl p-4 border border-white/5 flex flex-col sm:flex-row items-center gap-3">
        <div className="relative flex-1 w-full">
          <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            placeholder="Search titles, thoughts, or tags..."
            className="w-full pl-10 pr-4 py-2 rounded-xl bg-white/5 border border-white/10 text-white text-xs placeholder-slate-500 focus:outline-none focus:border-brand-500"
          />
        </div>

        <select
          value={selectedTheme}
          onChange={(e) => setSelectedTheme(e.target.value)}
          className="w-full sm:w-48 px-3 py-2 rounded-xl bg-white/5 border border-white/10 text-white text-xs focus:outline-none focus:border-brand-500 capitalize"
        >
          <option value="" className="bg-dark-800">All Themes</option>
          {['motivation', 'discipline', 'consistency', 'resilience', 'success', 'mindfulness'].map((th) => (
            <option key={th} value={th} className="bg-dark-800">{th}</option>
          ))}
        </select>
      </div>

      {/* Video Cards Grid */}
      {activeTab === 'videos' && (
        <div className="space-y-4">
          {videosLoading ? (
            <div className="p-12 text-center text-slate-400 text-xs">Loading video library...</div>
          ) : filteredVideos.length === 0 ? (
            <div className="glass-panel rounded-3xl p-12 text-center border border-dashed border-white/10">
              <Film className="w-12 h-12 text-slate-600 mx-auto mb-3" />
              <h3 className="text-sm font-bold text-white">No videos found</h3>
              <p className="text-xs text-slate-400 mt-1">Render videos in the Video Studio or trigger the daily pipeline.</p>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {filteredVideos.map((vid) => (
                <div key={vid.id} className="glass-panel rounded-3xl p-5 border border-white/5 flex flex-col justify-between space-y-4 glass-card-hover">
                  <div>
                    <div className="flex items-center justify-between text-xs mb-3">
                      <span className="font-mono text-[10px] px-2 py-0.5 rounded bg-white/5 text-slate-400">
                        {vid.duration_seconds}s • 1080x1920
                      </span>
                      <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${
                        vid.upload_status === 'published' ? 'bg-emerald-500/20 text-emerald-400' : 'bg-brand-500/20 text-brand-300'
                      }`}>
                        {vid.upload_status}
                      </span>
                    </div>

                    <h3 className="text-sm font-bold text-white line-clamp-1">{vid.title}</h3>
                    <p className="text-xs text-slate-400 line-clamp-2 mt-1.5 leading-relaxed">{vid.description}</p>
                  </div>

                  <div className="pt-3 border-t border-white/5 flex items-center justify-between text-xs">
                    <button
                      onClick={() => setPreviewVideoUrl(vid.video_url || null)}
                      className="flex items-center gap-1.5 text-brand-400 hover:text-brand-300 font-semibold"
                    >
                      <PlayCircle className="w-4 h-4" />
                      <span>Preview</span>
                    </button>

                    <div className="flex items-center gap-3">
                      {vid.video_url && (
                        <a href={vid.video_url} download className="text-slate-400 hover:text-white" title="Download">
                          <Download className="w-4 h-4" />
                        </a>
                      )}
                      {vid.youtube_url && (
                        <a href={vid.youtube_url} target="_blank" rel="noreferrer" className="text-red-400 hover:text-red-300" title="YouTube">
                          <ExternalLink className="w-4 h-4" />
                        </a>
                      )}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* Thoughts List */}
      {activeTab === 'thoughts' && (
        <div className="space-y-3">
          {thoughtsLoading ? (
            <div className="p-12 text-center text-slate-400 text-xs">Loading thoughts...</div>
          ) : filteredThoughts.length === 0 ? (
            <div className="glass-panel rounded-3xl p-12 text-center border border-dashed border-white/10">
              <Sparkles className="w-12 h-12 text-slate-600 mx-auto mb-3" />
              <h3 className="text-sm font-bold text-white">No thoughts found</h3>
            </div>
          ) : (
            filteredThoughts.map((th) => (
              <div key={th.id} className="glass-panel rounded-2xl p-5 border border-white/5 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                <div className="space-y-1">
                  <div className="flex items-center gap-2">
                    <span className="px-2 py-0.5 rounded text-[10px] font-semibold bg-brand-500/20 text-brand-300 capitalize">
                      {th.theme}
                    </span>
                    <span className="text-[10px] text-slate-500 font-mono">
                      {new Date(th.created_at).toLocaleDateString()}
                    </span>
                  </div>
                  <h4 className="text-sm font-bold text-white">{th.title}</h4>
                  <p className="text-xs text-slate-300 italic">"{th.text}"</p>
                </div>

                <div className="flex items-center gap-2 self-end sm:self-center">
                  <button
                    onClick={() => deleteThoughtMutation.mutate(th.id)}
                    className="p-2 rounded-xl bg-white/5 text-slate-400 hover:text-red-400 hover:bg-white/10"
                    title="Delete"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              </div>
            ))
          )}
        </div>
      )}

      {/* Preview Modal */}
      {previewVideoUrl && (
        <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-md flex items-center justify-center p-4">
          <div className="glass-panel rounded-3xl p-6 max-w-sm w-full border border-white/10 relative">
            <button
              onClick={() => setPreviewVideoUrl(null)}
              className="absolute top-4 right-4 p-1.5 rounded-full bg-white/10 text-slate-300 hover:text-white"
            >
              ✕
            </button>
            <h3 className="text-sm font-bold text-white mb-3">Vertical 9:16 Preview</h3>
            <div className="rounded-2xl overflow-hidden bg-black aspect-[9/16] w-full flex items-center justify-center border border-white/10">
              <video src={previewVideoUrl} controls autoPlay loop className="w-full h-full object-cover" />
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
