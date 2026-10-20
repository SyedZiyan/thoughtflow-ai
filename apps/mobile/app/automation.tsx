import React, { useState, useEffect } from 'react';
import {
  StyleSheet,
  Text,
  View,
  ScrollView,
  TouchableOpacity,
  ActivityIndicator,
  Alert,
  RefreshControl
} from 'react-native';
import { mobileApi } from '../src/api/mobileClient';

export default function MobileAutomationMonitor() {
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [status, setStatus] = useState<any>(null);
  const [jobs, setJobs] = useState<any[]>([]);
  const [triggering, setTriggering] = useState(false);

  const loadData = async () => {
    try {
      const [statusData, jobsData] = await Promise.all([
        mobileApi.getAutomationStatus(),
        mobileApi.getAutomationJobs()
      ]);
      setStatus(statusData);
      setJobs(jobsData || []);
    } catch (err: any) {
      console.warn('Automation fetch error:', err.message);
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleRunPipeline = async () => {
    setTriggering(true);
    try {
      await mobileApi.runPipeline();
      Alert.alert('Pipeline Triggered', 'Autonomous worker pipeline executed! Thought generated and video queued.');
      loadData();
    } catch (err: any) {
      Alert.alert('Trigger Failed', err.message);
    } finally {
      setTriggering(false);
    }
  };

  const handleRetryJob = async (jobId: string) => {
    try {
      await mobileApi.retryAutomationJob(jobId);
      Alert.alert('Job Re-queued', `Job ${jobId} reset to pending.`);
      loadData();
    } catch (err: any) {
      Alert.alert('Retry Failed', err.message);
    }
  };

  if (loading) {
    return (
      <View style={styles.centerContainer}>
        <ActivityIndicator size="large" color="#a767f7" />
        <Text style={styles.loadingText}>Connecting to autonomous daemon...</Text>
      </View>
    );
  }

  const isHealthy = status?.worker_health?.status === 'healthy';

  return (
    <ScrollView
      style={styles.container}
      contentContainerStyle={styles.content}
      refreshControl={<RefreshControl refreshing={refreshing} onRefresh={() => { setRefreshing(true); loadData(); }} tintColor="#a767f7" />}
    >
      <Text style={styles.title}>Automation & Worker</Text>
      <Text style={styles.subtitle}>
        Monitor your background daemon, idempotency locks, and job execution pipeline.
      </Text>

      {/* Daemon Status Card */}
      <View style={styles.card}>
        <View style={styles.headerRow}>
          <Text style={styles.cardHeader}>AUTONOMOUS WORKER DAEMON</Text>
          <View style={[styles.statusBadge, { backgroundColor: isHealthy ? 'rgba(16,185,129,0.15)' : 'rgba(239,68,68,0.15)' }]}>
            <View style={[styles.statusDot, { backgroundColor: isHealthy ? '#10b981' : '#ef4444' }]} />
            <Text style={[styles.badgeText, { color: isHealthy ? '#10b981' : '#ef4444' }]}>
              {status?.worker_health?.status?.toUpperCase() || 'OFFLINE'}
            </Text>
          </View>
        </View>

        <Text style={styles.infoText}>
          PID: {status?.worker_health?.pid || '—'} • Last Heartbeat: {status?.worker_health?.last_heartbeat ? new Date(status.worker_health.last_heartbeat).toLocaleTimeString() : 'No signal'}
        </Text>
        <Text style={styles.subInfoText}>
          Next scheduled tick: {status?.next_execution || 'Daily scheduled time'}
        </Text>

        <TouchableOpacity
          style={styles.primaryButton}
          onPress={handleRunPipeline}
          disabled={triggering}
        >
          {triggering ? (
            <ActivityIndicator size="small" color="#ffffff" />
          ) : (
            <Text style={styles.primaryButtonText}>Run Pipeline Now (Manual Trigger)</Text>
          )}
        </TouchableOpacity>
      </View>

      {/* Execution Jobs Queue */}
      <View style={styles.card}>
        <Text style={styles.cardHeader}>RECENT EXECUTION JOBS</Text>
        {jobs.length === 0 ? (
          <Text style={styles.emptyText}>No recent execution jobs found.</Text>
        ) : (
          jobs.slice(0, 8).map((job) => (
            <View key={job.id} style={styles.jobItem}>
              <View style={styles.jobInfo}>
                <Text style={styles.jobKey} numberOfLines={1}>{job.job_key}</Text>
                <Text style={styles.jobStage}>Stage: {job.current_stage || 'init'} • Attempts: {job.attempts}/{job.max_attempts}</Text>
              </View>
              <View style={styles.jobActions}>
                <View style={[
                  styles.jobStatusPill,
                  job.status === 'completed' && styles.statusCompleted,
                  job.status === 'failed' && styles.statusFailed,
                  job.status === 'pending' && styles.statusPending
                ]}>
                  <Text style={styles.jobStatusText}>{job.status}</Text>
                </View>

                {job.status === 'failed' && (
                  <TouchableOpacity
                    style={styles.retryButton}
                    onPress={() => handleRetryJob(job.id)}
                  >
                    <Text style={styles.retryText}>Retry</Text>
                  </TouchableOpacity>
                )}
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
  card: {
    backgroundColor: '#12151F',
    borderRadius: 20,
    padding: 20,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.06)',
    marginBottom: 16,
  },
  headerRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 10 },
  cardHeader: { color: '#a767f7', fontSize: 11, fontWeight: '700', letterSpacing: 0.5 },
  statusBadge: { flexDirection: 'row', alignItems: 'center', paddingVertical: 4, paddingHorizontal: 8, borderRadius: 10 },
  statusDot: { width: 6, height: 6, borderRadius: 3, marginRight: 6 },
  badgeText: { fontSize: 10, fontWeight: 'bold' },
  infoText: { color: '#ffffff', fontSize: 13, fontWeight: '600', marginBottom: 4 },
  subInfoText: { color: '#64748b', fontSize: 11, marginBottom: 16 },
  primaryButton: {
    backgroundColor: '#8e3ef0',
    paddingVertical: 14,
    borderRadius: 16,
    alignItems: 'center',
    justifyContent: 'center',
  },
  primaryButtonText: { color: '#ffffff', fontSize: 14, fontWeight: 'bold' },
  emptyText: { color: '#64748b', fontSize: 12, marginTop: 6 },
  jobItem: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: 12,
    borderBottomWidth: 1,
    borderBottomColor: 'rgba(255,255,255,0.04)',
  },
  jobInfo: { flex: 1, marginRight: 10 },
  jobKey: { color: '#e2e8f0', fontSize: 12, fontWeight: 'bold' },
  jobStage: { color: '#64748b', fontSize: 10, marginTop: 2 },
  jobActions: { flexDirection: 'row', alignItems: 'center', gap: 6 },
  jobStatusPill: { paddingVertical: 4, paddingHorizontal: 8, borderRadius: 8, backgroundColor: '#1a1e2c' },
  statusCompleted: { backgroundColor: 'rgba(16,185,129,0.15)' },
  statusFailed: { backgroundColor: 'rgba(239,68,68,0.15)' },
  statusPending: { backgroundColor: 'rgba(245,158,11,0.15)' },
  jobStatusText: { color: '#ffffff', fontSize: 10, fontWeight: 'bold', textTransform: 'uppercase' },
  retryButton: { backgroundColor: '#334155', paddingVertical: 4, paddingHorizontal: 8, borderRadius: 8 },
  retryText: { color: '#ffffff', fontSize: 10, fontWeight: 'bold' },
});
