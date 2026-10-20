import React, { useState, useEffect } from 'react';
import {
  StyleSheet,
  Text,
  View,
  ScrollView,
  ActivityIndicator,
  RefreshControl,
  TouchableOpacity,
  Alert
} from 'react-native';
import { mobileApi } from '../src/api/mobileClient';

export default function MobilePublishingCalendar() {
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [settings, setSettings] = useState<any>(null);
  const [videos, setVideos] = useState<any[]>([]);

  const loadData = async () => {
    try {
      const [settingsData, videosData] = await Promise.all([
        mobileApi.getSettings(),
        mobileApi.getVideos()
      ]);
      setSettings(settingsData);
      setVideos(videosData || []);
    } catch (err: any) {
      console.warn('Calendar fetch error:', err.message);
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  if (loading) {
    return (
      <View style={styles.centerContainer}>
        <ActivityIndicator size="large" color="#a767f7" />
        <Text style={styles.loadingText}>Loading Publishing Calendar...</Text>
      </View>
    );
  }

  const scheduledTime = settings?.publication_time || '07:00';
  const timezone = settings?.timezone || 'UTC';

  return (
    <ScrollView
      style={styles.container}
      contentContainerStyle={styles.content}
      refreshControl={<RefreshControl refreshing={refreshing} onRefresh={() => { setRefreshing(true); loadData(); }} tintColor="#a767f7" />}
    >
      <Text style={styles.title}>Publishing Calendar</Text>
      <Text style={styles.subtitle}>
        Track scheduled releases and publication history on your YouTube channel.
      </Text>

      {/* Schedule Window Card */}
      <View style={styles.scheduleCard}>
        <View style={styles.badgeRow}>
          <Text style={styles.badgeText}>DAILY SHORTS WINDOW</Text>
          <Text style={styles.timezoneBadge}>{timezone}</Text>
        </View>
        <Text style={styles.scheduleTime}>{scheduledTime} {timezone}</Text>
        <Text style={styles.scheduleDesc}>
          Every day at this time, rendered videos are published directly to your YouTube channel.
        </Text>
      </View>

      {/* Published & Queue List */}
      <View style={styles.card}>
        <Text style={styles.cardHeader}>SHORTS PIPELINE & HISTORY</Text>
        {videos.length === 0 ? (
          <Text style={styles.emptyText}>No videos in the publishing pipeline yet.</Text>
        ) : (
          videos.map((v) => (
            <View key={v.id} style={styles.videoItem}>
              <View style={styles.videoInfo}>
                <Text style={styles.videoTheme}>#{v.visual_theme?.toUpperCase() || 'DEFAULT'}</Text>
                <Text style={styles.videoDuration}>{v.duration_seconds}s Short • {v.resolution || '1080x1920'}</Text>
                <Text style={styles.videoDate}>
                  {v.created_at ? new Date(v.created_at).toLocaleDateString() : 'Today'}
                </Text>
              </View>

              <View style={[
                styles.statusPill,
                v.status === 'published' && styles.statusPublished,
                v.status === 'scheduled' && styles.statusScheduled,
                v.status === 'ready' && styles.statusReady
              ]}>
                <Text style={styles.statusText}>{v.status || 'READY'}</Text>
              </View>
            </View>
          ))
        )}
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#0B0D13' },
  content: { padding: 20, paddingBottom: 60 },
  centerContainer: { flex: 1, backgroundColor: '#0B0D13', justifyContent: 'center', alignItems: 'center' },
  loadingText: { color: '#94a3b8', fontSize: 13, marginTop: 12 },
  title: { color: '#ffffff', fontSize: 24, fontWeight: '800', marginBottom: 4 },
  subtitle: { color: '#94a3b8', fontSize: 12, marginBottom: 20 },
  scheduleCard: {
    backgroundColor: '#161926',
    borderRadius: 20,
    padding: 20,
    borderWidth: 1,
    borderColor: 'rgba(167, 103, 247, 0.25)',
    marginBottom: 20,
  },
  badgeRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 },
  badgeText: { color: '#c084fc', fontSize: 10, fontWeight: '800', letterSpacing: 0.5 },
  timezoneBadge: { color: '#ffffff', backgroundColor: '#8e3ef0', fontSize: 10, fontWeight: 'bold', paddingVertical: 2, paddingHorizontal: 6, borderRadius: 6 },
  scheduleTime: { color: '#ffffff', fontSize: 28, fontWeight: 'bold', marginBottom: 6 },
  scheduleDesc: { color: '#94a3b8', fontSize: 12, lineHeight: 18 },
  card: {
    backgroundColor: '#12151F',
    borderRadius: 20,
    padding: 20,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.06)',
  },
  cardHeader: { color: '#a767f7', fontSize: 11, fontWeight: '700', letterSpacing: 0.5, marginBottom: 12 },
  emptyText: { color: '#64748b', fontSize: 12 },
  videoItem: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: 12,
    borderBottomWidth: 1,
    borderBottomColor: 'rgba(255,255,255,0.04)',
  },
  videoInfo: { flex: 1, marginRight: 10 },
  videoTheme: { color: '#c084fc', fontSize: 10, fontWeight: 'bold' },
  videoDuration: { color: '#e2e8f0', fontSize: 13, fontWeight: '600', marginTop: 2 },
  videoDate: { color: '#64748b', fontSize: 11, marginTop: 2 },
  statusPill: { paddingVertical: 4, paddingHorizontal: 10, borderRadius: 10, backgroundColor: '#1a1e2c' },
  statusPublished: { backgroundColor: 'rgba(16,185,129,0.15)' },
  statusScheduled: { backgroundColor: 'rgba(167,103,247,0.15)' },
  statusReady: { backgroundColor: 'rgba(59,130,246,0.15)' },
  statusText: { color: '#ffffff', fontSize: 10, fontWeight: 'bold', textTransform: 'uppercase' },
});
