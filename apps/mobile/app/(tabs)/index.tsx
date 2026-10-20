import React, { useState, useEffect } from 'react';
import {
  StyleSheet,
  Text,
  View,
  ScrollView,
  TouchableOpacity,
  ActivityIndicator,
  RefreshControl,
  Alert
} from 'react-native';
import { useRouter } from 'expo-router';
import { mobileApi } from '../../src/api/mobileClient';

export default function DashboardScreen() {
  const router = useRouter();
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [dashboard, setDashboard] = useState<any>(null);
  const [runningPipeline, setRunningPipeline] = useState(false);

  const loadData = async () => {
    try {
      const data = await mobileApi.getDashboard();
      setDashboard(data);
    } catch (err: any) {
      console.warn('Dashboard fetch notice:', err.message);
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleRunPipeline = async () => {
    setRunningPipeline(true);
    try {
      await mobileApi.runPipeline();
      Alert.alert('Success', 'Pipeline triggered! Generated thought and rendering 9:16 MP4.');
      loadData();
    } catch (err: any) {
      Alert.alert('Pipeline Error', err.message);
    } finally {
      setRunningPipeline(false);
    }
  };

  if (loading) {
    return (
      <View style={styles.centerContainer}>
        <ActivityIndicator size="large" color="#a767f7" />
        <Text style={styles.loadingText}>Connecting to ThoughtFlow AI...</Text>
      </View>
    );
  }

  return (
    <ScrollView
      style={styles.container}
      contentContainerStyle={styles.content}
      refreshControl={<RefreshControl refreshing={refreshing} onRefresh={() => { setRefreshing(true); loadData(); }} tintColor="#a767f7" />}
    >
      {/* Hero Card */}
      <View style={styles.heroCard}>
        <Text style={styles.badgeText}>DAILY SHORTS AUTOMATION</Text>
        <Text style={styles.heroTitle}>One Thought Every Day.</Text>
        <Text style={styles.heroSubtitle}>
          Next run: {dashboard?.next_scheduled_execution || 'Daily at 07:00 UTC'}
        </Text>

        <TouchableOpacity
          style={styles.primaryButton}
          onPress={handleRunPipeline}
          disabled={runningPipeline}
        >
          {runningPipeline ? (
            <ActivityIndicator size="small" color="#ffffff" />
          ) : (
            <Text style={styles.primaryButtonText}>Run Pipeline Now</Text>
          )}
        </TouchableOpacity>
      </View>

      {/* Status Grid */}
      <View style={styles.grid}>
        <View style={styles.statCard}>
          <Text style={styles.statLabel}>Today's Generation</Text>
          <Text style={styles.statValue}>{dashboard?.todays_generation_status || 'Pending'}</Text>
          <Text style={styles.statSub}>Structured Thought</Text>
        </View>

        <View style={styles.statCard}>
          <Text style={styles.statLabel}>Today's Publishing</Text>
          <Text style={styles.statValue}>{dashboard?.todays_publishing_status || 'Pending'}</Text>
          <Text style={styles.statSub}>YouTube Shorts</Text>
        </View>

        <View style={styles.statCard}>
          <Text style={styles.statLabel}>Total Rendered</Text>
          <Text style={styles.statValue}>{dashboard?.total_videos || 0}</Text>
          <Text style={styles.statSub}>1080x1920 MP4s</Text>
        </View>

        <View style={styles.statCard}>
          <Text style={styles.statLabel}>YouTube Mode</Text>
          <Text style={styles.statValue}>
            {dashboard?.youtube_connected ? 'Live' : 'Dry Run'}
          </Text>
          <Text style={styles.statSub}>Channel State</Text>
        </View>
      </View>

      {/* Quick Nav Links */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Quick Management</Text>
        <View style={styles.actionGrid}>
          <TouchableOpacity
            style={styles.secondaryButton}
            onPress={() => router.push('/studio')}
          >
            <Text style={styles.actionIcon}>🎬</Text>
            <Text style={styles.secondaryButtonText}>Video Studio</Text>
          </TouchableOpacity>
          <TouchableOpacity
            style={styles.secondaryButton}
            onPress={() => router.push('/youtube')}
          >
            <Text style={styles.actionIcon}>📺</Text>
            <Text style={styles.secondaryButtonText}>YouTube Channel</Text>
          </TouchableOpacity>
          <TouchableOpacity
            style={styles.secondaryButton}
            onPress={() => router.push('/automation')}
          >
            <Text style={styles.actionIcon}>⚙️</Text>
            <Text style={styles.secondaryButtonText}>Automation</Text>
          </TouchableOpacity>
          <TouchableOpacity
            style={styles.secondaryButton}
            onPress={() => router.push('/calendar')}
          >
            <Text style={styles.actionIcon}>📅</Text>
            <Text style={styles.secondaryButtonText}>Calendar</Text>
          </TouchableOpacity>
          <TouchableOpacity
            style={styles.secondaryButton}
            onPress={() => router.push('/analytics')}
          >
            <Text style={styles.actionIcon}>📊</Text>
            <Text style={styles.secondaryButtonText}>Analytics</Text>
          </TouchableOpacity>
          <TouchableOpacity
            style={styles.secondaryButton}
            onPress={() => router.push('/logs')}
          >
            <Text style={styles.actionIcon}>📋</Text>
            <Text style={styles.secondaryButtonText}>Execution Logs</Text>
          </TouchableOpacity>
        </View>
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#0B0D13' },
  content: { padding: 20, paddingBottom: 40 },
  centerContainer: { flex: 1, backgroundColor: '#0B0D13', justifyContent: 'center', alignItems: 'center' },
  loadingText: { color: '#94a3b8', fontSize: 13, marginTop: 12 },
  heroCard: {
    backgroundColor: '#161926',
    borderRadius: 24,
    padding: 24,
    borderWidth: 1,
    borderColor: 'rgba(167, 103, 247, 0.25)',
    marginBottom: 20,
  },
  badgeText: { color: '#c084fc', fontSize: 10, fontWeight: 'bold', letterSpacing: 1, marginBottom: 8 },
  heroTitle: { color: '#ffffff', fontSize: 24, fontWeight: '800', marginBottom: 6 },
  heroSubtitle: { color: '#94a3b8', fontSize: 12, marginBottom: 18 },
  primaryButton: {
    backgroundColor: '#8e3ef0',
    paddingVertical: 14,
    borderRadius: 16,
    alignItems: 'center',
    justifyContent: 'center',
  },
  primaryButtonText: { color: '#ffffff', fontSize: 14, fontWeight: 'bold' },
  grid: { flexDirection: 'row', flexWrap: 'wrap', gap: 12, marginBottom: 20 },
  statCard: {
    flex: 1,
    minWidth: '46%',
    backgroundColor: '#12151F',
    borderRadius: 18,
    padding: 16,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.05)',
  },
  statLabel: { color: '#64748b', fontSize: 10, fontWeight: '700', textTransform: 'uppercase', marginBottom: 6 },
  statValue: { color: '#ffffff', fontSize: 18, fontWeight: 'bold', textTransform: 'capitalize' },
  statSub: { color: '#64748b', fontSize: 10, marginTop: 4 },
  section: { marginTop: 10 },
  sectionTitle: { color: '#ffffff', fontSize: 16, fontWeight: 'bold', marginBottom: 12 },
  actionGrid: { flexDirection: 'row', flexWrap: 'wrap', gap: 10 },
  actionIcon: { fontSize: 20, marginBottom: 4 },
  secondaryButton: {
    width: '48%',
    backgroundColor: '#1a1e2c',
    paddingVertical: 14,
    paddingHorizontal: 12,
    borderRadius: 16,
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.08)',
  },
  secondaryButtonText: { color: '#cbd5e1', fontSize: 12, fontWeight: '600', textAlign: 'center' },
});
