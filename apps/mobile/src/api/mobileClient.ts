import { Platform } from 'react-native';

// Android Emulator connects to host machine via 10.0.2.2; physical device uses LAN IP
const DEFAULT_HOST = Platform.OS === 'android' ? 'http://10.0.2.2:8000' : 'http://localhost:8000';
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
  getSettings: () => mobileRequest<any>('/settings'),
  updateSettings: (payload: any) =>
    mobileRequest<any>('/settings', {
      method: 'PATCH',
      body: JSON.stringify(payload),
    }),
  getYouTubeStatus: () => mobileRequest<any>('/youtube/status'),
  getAnalytics: () => mobileRequest<any>('/analytics'),
  getLogs: () => mobileRequest<any[]>('/logs'),
};
