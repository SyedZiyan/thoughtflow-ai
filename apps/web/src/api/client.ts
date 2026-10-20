const API_BASE = '/api/v1';

export interface Thought {
  id: string;
  user_id: string;
  text: string;
  theme: string;
  language: string;
  title: string;
  description: string;
  hashtags: string[];
  narration_script?: string;
  content_hash: string;
  prompt_version: string;
  status: string;
  created_at: string;
}

export interface VideoAsset {
  id: string;
  thought_id: string;
  storage_key: string;
  video_url?: string;
  thumbnail_url?: string;
  title: string;
  description: string;
  duration_seconds: number;
  width: number;
  height: number;
  render_status: string;
  upload_status: string;
  youtube_video_id?: string;
  youtube_url?: string;
  scheduled_at?: string;
  published_at?: string;
  created_at: string;
}

export interface UserSettings {
  id: string;
  user_id: string;
  timezone: string;
  generation_time: string;
  publishing_time: string;
  selected_themes: string[];
  language: string;
  video_duration: number;
  narration_enabled: boolean;
  auto_publish_enabled: boolean;
  automation_enabled: boolean;
  daily_limit: number;
  visual_theme: string;
  animation_style: string;
}

export interface ActivityLog {
  id: string;
  user_id: string;
  job_id?: string;
  event_type: string;
  message: string;
  log_metadata?: any;
  created_at: string;
}

export interface DashboardMetrics {
  todays_generation_status: string;
  todays_publishing_status: string;
  next_scheduled_execution: string | null;
  total_thoughts: number;
  total_videos: number;
  total_published: number;
  failed_jobs: number;
  youtube_connected: boolean;
  automation_enabled: boolean;
  recent_activity: ActivityLog[];
  upcoming_schedule: Array<{
    date: string;
    thought_title: string;
    status: string;
    theme: string;
    time: string;
  }>;
  latest_videos: VideoAsset[];
}

export interface YouTubeStatus {
  connected: boolean;
  channel_id?: string;
  channel_title?: string;
  channel_thumbnail?: string;
  subscriber_count?: number;
  video_count?: number;
  view_count?: number;
  connected_at?: string;
  upload_enabled: boolean;
  dry_run_mode: boolean;
}

export interface AnalyticsData {
  total_published_videos: number;
  total_views: number;
  total_likes: number;
  total_comments: number;
  success_rate: number;
  performance_by_theme: Array<{
    theme: string;
    count: number;
    views: number;
    avg_likes: number;
  }>;
  recent_uploads: Array<{
    id: string;
    title: string;
    youtube_url?: string;
    views: number;
    likes: number;
    published_at: string;
  }>;
}

async function request<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
  const url = `${API_BASE}${endpoint}`;
  const headers = {
    'Content-Type': 'application/json',
    ...(options.headers || {}),
  };

  const response = await fetch(url, { ...options, headers });
  if (!response.ok) {
    const errorBody = await response.text();
    let message = `Request failed: ${response.status} ${response.statusText}`;
    try {
      const parsed = JSON.parse(errorBody);
      message = parsed.detail || message;
    } catch {
      if (errorBody) message = errorBody;
    }
    throw new Error(message);
  }
  return response.json();
}

export const api = {
  // Health
  checkHealth: () => request<any>('/health', { method: 'GET' }),

  // Dashboard
  getDashboard: () => request<DashboardMetrics>('/dashboard'),

  // Thoughts
  getThoughts: (theme?: string, status?: string) => {
    const params = new URLSearchParams();
    if (theme) params.append('theme', theme);
    if (status) params.append('status', status);
    return request<Thought[]>(`/thoughts?${params.toString()}`);
  },
  generateThought: (payload: { theme?: string; tone?: string; language?: string; custom_prompt?: string; auto_render?: boolean }) =>
    request<Thought>('/thoughts/generate', {
      method: 'POST',
      body: JSON.stringify(payload),
    }),
  updateThought: (id: string, payload: Partial<Thought>) =>
    request<Thought>(`/thoughts/${id}`, {
      method: 'PATCH',
      body: JSON.stringify(payload),
    }),
  deleteThought: (id: string) =>
    request<void>(`/thoughts/${id}`, { method: 'DELETE' }),

  // Videos
  getVideos: (status?: string) => {
    const params = new URLSearchParams();
    if (status) params.append('status', status);
    return request<VideoAsset[]>(`/videos?${params.toString()}`);
  },
  getVideoStyles: () => request<{
    visual_themes: Array<{ id: string; name: string; accent: number[] }>;
    animation_styles: Array<{ id: string; name: string; desc: string }>;
    typography_options: Array<{ id: string; name: string; style: string }>;
    text_placements: Array<{ id: string; name: string }>;
    tts_voices: Array<{ id: string; name: string; gender: string; lang: string }>;
  }>('/videos/styles'),
  previewFrame: async (payload: {
    thought_text: string;
    theme?: string;
    visual_theme?: string;
    typography?: string;
    text_placement?: string;
  }): Promise<string> => {
    const resp = await fetch('/api/v1/videos/preview-frame', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    });
    if (!resp.ok) {
      throw new Error(`Failed to generate frame preview: ${resp.statusText}`);
    }
    const blob = await resp.blob();
    return URL.createObjectURL(blob);
  },
  renderVideo: (payload: {
    thought_id: string;
    duration_seconds?: number;
    visual_theme?: string;
    animation_style?: string;
    typography?: string;
    text_placement?: string;
    narration_enabled?: boolean;
    ambient_enabled?: boolean;
    narration_voice?: string;
  }) =>
    request<VideoAsset>('/videos/render', {
      method: 'POST',
      body: JSON.stringify(payload),
    }),
  retryVideo: (id: string) =>
    request<VideoAsset>(`/videos/${id}/retry`, { method: 'POST' }),
  validateVideo: (id: string) =>
    request<any>(`/videos/${id}/validate`, { method: 'POST' }),

  // Automation & Pipeline
  runPipeline: (theme?: string, prompt?: string) =>
    request<any>('/automation/run', {
      method: 'POST',
      body: JSON.stringify({ theme, custom_prompt: prompt }),
    }),
  getAutomationStatus: () => request<any>('/automation/status'),
  getJobs: () => request<any[]>('/automation/jobs'),

  // Settings
  getSettings: () => request<UserSettings>('/settings'),
  updateSettings: (payload: Partial<UserSettings>) =>
    request<UserSettings>('/settings', {
      method: 'PATCH',
      body: JSON.stringify(payload),
    }),

  // YouTube
  getYouTubeStatus: () => request<YouTubeStatus>('/youtube/status'),
  connectYouTube: () => request<{ auth_url: string; configured: boolean; message: string }>('/youtube/connect', { method: 'POST' }),
  uploadVideoToYouTube: (videoId: string, payload?: { title?: string; description?: string; tags?: string[]; privacy_status?: string }) =>
    request<any>(`/youtube/upload/${videoId}`, {
      method: 'POST',
      body: JSON.stringify(payload || {}),
    }),

  // Analytics & Logs
  getAnalytics: () => request<AnalyticsData>('/analytics'),
  getLogs: () => request<ActivityLog[]>('/logs'),
};
