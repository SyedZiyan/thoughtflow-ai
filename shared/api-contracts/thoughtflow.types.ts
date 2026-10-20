export type ThoughtTheme =
  | 'motivation'
  | 'discipline'
  | 'consistency'
  | 'self-improvement'
  | 'life lessons'
  | 'success'
  | 'confidence'
  | 'relationships'
  | 'resilience'
  | 'personal growth'
  | 'mindfulness'
  | 'productivity';

export type JobStatus =
  | 'pending'
  | 'generating'
  | 'rendering'
  | 'validating'
  | 'uploading'
  | 'scheduled'
  | 'published'
  | 'failed'
  | 'cancelled';

export type VideoPrivacy = 'private' | 'unlisted' | 'public';

export interface User {
  id: string;
  email: string;
  role: string;
  created_at: string;
  updated_at: string;
}

export interface Thought {
  id: string;
  user_id: string;
  text: string;
  theme: ThoughtTheme | string;
  language: string;
  title: string;
  description: string;
  hashtags: string[];
  narration_script?: string;
  content_hash: string;
  prompt_version: string;
  status: 'draft' | 'approved' | 'rendered' | 'rejected';
  created_at: string;
}

export interface VideoAsset {
  id: string;
  thought_id: string;
  storage_key: string;
  video_url: string;
  thumbnail_url?: string;
  title: string;
  description: string;
  duration_seconds: number;
  width: number;
  height: number;
  render_status: 'pending' | 'rendering' | 'completed' | 'failed';
  upload_status: 'not_uploaded' | 'uploading' | 'published' | 'scheduled' | 'failed';
  youtube_video_id?: string;
  youtube_url?: string;
  scheduled_at?: string;
  published_at?: string;
  created_at: string;
}

export interface AutomationJob {
  id: string;
  job_key: string;
  job_type: 'daily_pipeline' | 'manual_generation' | 'manual_render' | 'youtube_upload';
  status: JobStatus;
  current_stage: string;
  attempts: number;
  max_attempts: number;
  error_message?: string;
  started_at?: string;
  finished_at?: string;
  created_at: string;
  metadata?: Record<string, any>;
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

export interface YouTubeIntegrationStatus {
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

export interface ActivityLog {
  id: string;
  user_id: string;
  job_id?: string;
  event_type: 'thought_generated' | 'video_rendered' | 'upload_success' | 'upload_failed' | 'pipeline_error' | 'settings_updated';
  message: string;
  metadata?: Record<string, any>;
  created_at: string;
}

export interface DashboardMetrics {
  todays_generation_status: 'completed' | 'in_progress' | 'pending' | 'failed' | 'not_scheduled';
  todays_publishing_status: 'published' | 'scheduled' | 'in_progress' | 'pending' | 'failed' | 'not_scheduled';
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

export interface ThoughtGenerateRequest {
  theme?: string;
  tone?: string;
  language?: string;
  custom_prompt?: string;
  auto_render?: boolean;
}

export interface VideoRenderRequest {
  thought_id: string;
  duration_seconds?: number;
  visual_theme?: 'gradient_purple' | 'geometric_dark' | 'nebula' | 'gold_luxury' | 'minimal_slate';
  animation_style?: 'smooth_zoom' | 'kinetic_pan' | 'fade_stagger' | 'pulse_glow';
  typography?: 'inter' | 'montserrat' | 'playfair';
  narration_enabled?: boolean;
}

export interface YouTubeUploadRequest {
  video_id: string;
  title?: string;
  description?: string;
  tags?: string[];
  privacy_status?: VideoPrivacy;
  publish_at?: string;
}

export interface AnalyticsSummary {
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
