import React, { useState, useEffect } from 'react';
import {
  StyleSheet,
  Text,
  View,
  ScrollView,
  TouchableOpacity,
  ActivityIndicator,
  RefreshControl,
  Linking
} from 'react-native';
import { mobileApi } from '../../src/api/mobileClient';

export default function LibraryScreen() {
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [videos, setVideos] = useState<any[]>([]);

  const loadVideos = async () => {
    try {
      const data = await mobileApi.getVideos();
      setVideos(data);
    } catch (err: any) {
      console.warn('Library load notice:', err.message);
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  useEffect(() => {
    loadVideos();
  }, []);

  return (
    <ScrollView
      style={styles.container}
      contentContainerStyle={styles.content}
      refreshControl={<RefreshControl refreshing={refreshing} onRefresh={() => { setRefreshing(true); loadVideos(); }} tintColor="#a767f7" />}
    >
      <Text style={styles.title}>Content Library</Text>
      <Text style={styles.subtitle}>All rendered vertical video assets and YouTube publishing states.</Text>

      {loading ? (
        <ActivityIndicator color="#a767f7" style={{ marginTop: 40 }} />
      ) : videos.length === 0 ? (
        <View style={styles.emptyContainer}>
          <Text style={styles.emptyTitle}>No rendered videos yet</Text>
          <Text style={styles.emptySub}>Generate a thought and render a video in the Generate tab.</Text>
        </View>
      ) : (
        <View style={styles.list}>
          {videos.map((vid) => (
            <View key={vid.id} style={styles.card}>
              <View style={styles.cardHeader}>
                <Text style={styles.durationBadge}>{vid.duration_seconds}s • 9:16</Text>
                <Text style={[styles.statusBadge, vid.upload_status === 'published' && styles.statusPublished]}>
                  {vid.upload_status}
                </Text>
              </View>

              <Text style={styles.cardTitle}>{vid.title}</Text>
              <Text style={styles.cardDesc} numberOfLines={2}>{vid.description}</Text>

              {vid.youtube_url && (
                <TouchableOpacity
                  style={styles.ytButton}
                  onPress={() => Linking.openURL(vid.youtube_url)}
                >
                  <Text style={styles.ytButtonText}>Open on YouTube</Text>
                </TouchableOpacity>
              )}
            </View>
          ))}
        </View>
      )}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#0B0D13' },
  content: { padding: 20, paddingBottom: 40 },
  title: { color: '#ffffff', fontSize: 24, fontWeight: 'bold' },
  subtitle: { color: '#94a3b8', fontSize: 12, marginTop: 4, marginBottom: 20 },
  list: { gap: 14 },
  card: {
    backgroundColor: '#12151F',
    borderRadius: 18,
    padding: 16,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.05)',
  },
  cardHeader: { flexDirection: 'row', justifyContent: 'space-between', marginBottom: 10 },
  durationBadge: { color: '#94a3b8', fontSize: 10, fontFamily: 'monospace' },
  statusBadge: {
    color: '#c084fc',
    fontSize: 10,
    fontWeight: 'bold',
    textTransform: 'uppercase',
    backgroundColor: 'rgba(167, 103, 247, 0.15)',
    paddingHorizontal: 8,
    paddingVertical: 2,
    borderRadius: 8,
  },
  statusPublished: {
    color: '#34d399',
    backgroundColor: 'rgba(52, 211, 153, 0.15)',
  },
  cardTitle: { color: '#ffffff', fontSize: 14, fontWeight: 'bold', marginBottom: 6 },
  cardDesc: { color: '#64748b', fontSize: 12, lineHeight: 18, marginBottom: 12 },
  ytButton: {
    backgroundColor: '#dc2626',
    paddingVertical: 8,
    borderRadius: 10,
    alignItems: 'center',
  },
  ytButtonText: { color: '#ffffff', fontSize: 11, fontWeight: 'bold' },
  emptyContainer: { alignItems: 'center', marginTop: 40, padding: 20 },
  emptyTitle: { color: '#ffffff', fontSize: 16, fontWeight: 'bold', marginBottom: 6 },
  emptySub: { color: '#64748b', fontSize: 12, textAlign: 'center' },
});
