import { Platform } from 'react-native';

// Default to your host machine's LAN IP so physical Android phones and emulators connect seamlessly
const DEFAULT_HOST = Platform.OS === 'web' ? 'http://localhost:8000' : 'http://192.168.0.101:8000';
let API_HOST = DEFAULT_HOST;

export const setApiHost = (host: string) => {
  API_HOST = host;
};

export const getApiHost = () => API_HOST;

async function mobileRequest<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
  const url = `${API_HOST}/api/v1${endpoint}`;
  const response = await fetch(url, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(options.headers || {}),
    },
  });

  if (!response.ok) {
    const errorText = await response.text();
    let msg = `API Error ${response.status}`;
    try {
      const parsed = JSON.parse(errorText);
      msg = parsed.detail || msg;
    } catch {
      if (errorText) msg = errorText;
    }
    throw new Error(msg);
  }
  return response.json();
}

export const mobileApi = {
  checkHealth: () => fetch(`${API_HOST}/health`).then(r => r.json()),
  getDashboard: () => mobileRequest<any>('/dashboard'),
  getThoughts: (theme?: string) => mobileRequest<any[]>(theme ? `/thoughts?theme=${theme}` : '/thoughts'),
  generateThought: (payload: { theme?: string; tone?: string; custom_prompt?: string; auto_render?: boolean }) =>
    mobileRequest<any>('/thoughts/generate', {
      method: 'POST',
      body: JSON.stringify(payload),
    }),
  getVideos: () => mobileRequest<any[]>('/videos'),
  getVideoStyles: () => mobileRequest<any>('/videos/styles'),
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
    mobileRequest<any>('/videos/render', {
      method: 'POST',
      body: JSON.stringify(payload),
    }),
  runPipeline: () => mobileRequest<any>('/automation/run', { method: 'POST', body: JSON.stringify({}) }),
  getAutomationStatus: () => mobileRequest<any>('/automation/status'),
  getAutomationJobs: () => mobileRequest<any[]>('/automation/jobs'),
  retryAutomationJob: (jobId: string) =>
    mobileRequest<any>(`/automation/jobs/${jobId}/retry`, { method: 'POST' }),
  getSettings: () => mobileRequest<any>('/settings'),
  updateSettings: (payload: any) =>
    mobileRequest<any>('/settings', {
      method: 'PATCH',
      body: JSON.stringify(payload),
    }),
  getYouTubeStatus: () => mobileRequest<any>('/youtube/status'),
  getYouTubeAuthUrl: () => mobileRequest<{ auth_url: string }>('/youtube/auth-url'),
  disconnectYouTube: () => mobileRequest<any>('/youtube/disconnect', { method: 'POST' }),
  syncYouTubeMetrics: () => mobileRequest<any>('/youtube/sync-metrics', { method: 'POST' }),
  uploadVideoToYouTube: (videoId: string, title?: string, description?: string) =>
    mobileRequest<any>(`/youtube/upload/${videoId}`, {
      method: 'POST',
      body: JSON.stringify({ title, description }),
    }),
  getAnalytics: () => mobileRequest<any>('/analytics'),
  getLogs: () => mobileRequest<any[]>('/logs'),
};
