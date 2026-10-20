import React, { useState, useEffect } from 'react';
import {
  StyleSheet,
  Text,
  View,
  ScrollView,
  TouchableOpacity,
  ActivityIndicator,
  Alert,
  Linking
} from 'react-native';
import { mobileApi } from '../src/api/mobileClient';

export default function MobileYouTubeChannel() {
  const [loading, setLoading] = useState(true);
  const [status, setStatus] = useState<any>(null);
  const [syncing, setSyncing] = useState(false);
  const [disconnecting, setDisconnecting] = useState(false);

  const loadStatus = async () => {
    try {
      const data = await mobileApi.getYouTubeStatus();
      setStatus(data);
    } catch (err: any) {
      console.warn('Failed to load YouTube status:', err.message);
    } finally {
      setLoading(false);
      setSyncing(false);
    }
  };

  useEffect(() => {
    loadStatus();
  }, []);

  const handleConnectOAuth = async () => {
    try {
      const res = await mobileApi.getYouTubeAuthUrl();
      if (res.auth_url) {
        Linking.openURL(res.auth_url);
      } else {
        Alert.alert('Configuration Notice', 'Google Client ID is not configured. Dry-run mode is active.');
      }
    } catch (err: any) {
      Alert.alert('OAuth Error', err.message);
    }
  };

  const handleSyncMetrics = async () => {
    setSyncing(true);
    try {
      const res = await mobileApi.syncYouTubeMetrics();
      Alert.alert('Metrics Synced', res.message || 'Updated view and like counts.');
      loadStatus();
    } catch (err: any) {
      Alert.alert('Sync Notice', err.message);
      setSyncing(false);
    }
  };

  const handleDisconnect = async () => {
    Alert.alert(
      'Disconnect Channel?',
      'Are you sure you want to revoke and disconnect your YouTube channel?',
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Disconnect',
          style: 'destructive',
          onPress: async () => {
            setDisconnecting(true);
            try {
              await mobileApi.disconnectYouTube();
              Alert.alert('Disconnected', 'YouTube channel authorization removed.');
              loadStatus();
            } catch (err: any) {
              Alert.alert('Error', err.message);
            } finally {
              setDisconnecting(false);
            }
          }
        }
      ]
    );
  };

  if (loading) {
    return (
      <View style={styles.centerContainer}>
        <ActivityIndicator size="large" color="#a767f7" />
        <Text style={styles.loadingText}>Fetching YouTube channel credentials...</Text>
      </View>
    );
  }

  const isConnected = status?.connected;

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.content}>
      <Text style={styles.title}>YouTube Channel</Text>
      <Text style={styles.subtitle}>
        Manage YouTube Data API v3 authorization, upload mode, and Shorts metrics.
      </Text>

      {/* Connection Status Card */}
      <View style={[styles.card, isConnected ? styles.cardConnected : styles.cardDisconnected]}>
        <View style={styles.statusHeader}>
          <View style={[styles.statusDot, { backgroundColor: isConnected ? '#10b981' : '#f59e0b' }]} />
          <Text style={[styles.statusText, { color: isConnected ? '#10b981' : '#f59e0b' }]}>
            {isConnected ? 'LIVE CHANNEL CONNECTED' : 'DRY-RUN / DISCONNECTED'}
          </Text>
        </View>

        {isConnected ? (
          <View style={styles.channelInfo}>
            <Text style={styles.channelName}>{status.channel_title || 'Authorized Creator Channel'}</Text>
            <Text style={styles.channelId}>Channel ID: {status.channel_id}</Text>
            <Text style={styles.tokenStatus}>Encrypted Token: AES-128 Fernet Valid</Text>
          </View>
        ) : (
          <View style={styles.channelInfo}>
            <Text style={styles.channelName}>No YouTube Channel Linked</Text>
            <Text style={styles.channelDesc}>
              {status?.reason || 'System is operating in safe dry-run mode. Video rendering executes fully, and uploads will validate without altering live channels.'}
            </Text>
          </View>
        )}
      </View>

      {/* Channel Metrics */}
      <View style={styles.grid}>
        <View style={styles.statCard}>
          <Text style={styles.statLabel}>SUBSCRIBERS</Text>
          <Text style={styles.statValue}>{status?.subscriber_count ? status.subscriber_count.toLocaleString() : '—'}</Text>
        </View>

        <View style={styles.statCard}>
          <Text style={styles.statLabel}>TOTAL VIEWS</Text>
          <Text style={styles.statValue}>{status?.view_count ? status.view_count.toLocaleString() : '—'}</Text>
        </View>

        <View style={styles.statCard}>
          <Text style={styles.statLabel}>PUBLISHED SHORTS</Text>
          <Text style={styles.statValue}>{status?.published_videos_count || 0}</Text>
        </View>

        <View style={styles.statCard}>
          <Text style={styles.statLabel}>DAILY QUOTA</Text>
          <Text style={styles.statValue}>10,000 pts</Text>
        </View>
      </View>

      {/* Action Buttons */}
      <View style={styles.buttonGroup}>
        {isConnected ? (
          <>
            <TouchableOpacity
              style={styles.primaryButton}
              onPress={handleSyncMetrics}
              disabled={syncing}
            >
              {syncing ? (
                <ActivityIndicator size="small" color="#ffffff" />
              ) : (
                <Text style={styles.buttonText}>Sync Shorts Metrics</Text>
              )}
            </TouchableOpacity>

            <TouchableOpacity
              style={styles.dangerButton}
              onPress={handleDisconnect}
              disabled={disconnecting}
            >
              <Text style={styles.dangerButtonText}>Disconnect Channel</Text>
            </TouchableOpacity>
          </>
        ) : (
          <TouchableOpacity
            style={styles.primaryButton}
            onPress={handleConnectOAuth}
          >
            <Text style={styles.buttonText}>Authorize YouTube Channel (OAuth2)</Text>
          </TouchableOpacity>
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
    marginBottom: 20,
  },
  cardConnected: { borderColor: 'rgba(16, 185, 129, 0.3)' },
  cardDisconnected: { borderColor: 'rgba(245, 158, 11, 0.3)' },
  statusHeader: { flexDirection: 'row', alignItems: 'center', marginBottom: 12 },
  statusDot: { width: 10, height: 10, borderRadius: 5, marginRight: 8 },
  statusText: { fontSize: 11, fontWeight: '800', letterSpacing: 0.5 },
  channelInfo: { marginTop: 4 },
  channelName: { color: '#ffffff', fontSize: 18, fontWeight: 'bold', marginBottom: 4 },
  channelId: { color: '#64748b', fontSize: 11, fontFamily: 'monospace', marginBottom: 2 },
  channelDesc: { color: '#94a3b8', fontSize: 12, lineHeight: 18 },
  tokenStatus: { color: '#a767f7', fontSize: 11, fontWeight: '600', marginTop: 4 },
  grid: { flexDirection: 'row', flexWrap: 'wrap', gap: 12, marginBottom: 20 },
  statCard: {
    flex: 1,
    minWidth: '46%',
    backgroundColor: '#12151F',
    borderRadius: 16,
    padding: 16,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.05)',
  },
  statLabel: { color: '#64748b', fontSize: 10, fontWeight: '700', textTransform: 'uppercase', marginBottom: 6 },
  statValue: { color: '#ffffff', fontSize: 18, fontWeight: 'bold' },
  buttonGroup: { gap: 12, marginTop: 4 },
  primaryButton: {
    backgroundColor: '#8e3ef0',
    paddingVertical: 14,
    borderRadius: 16,
    alignItems: 'center',
    justifyContent: 'center',
  },
  buttonText: { color: '#ffffff', fontSize: 14, fontWeight: 'bold' },
  dangerButton: {
    backgroundColor: 'rgba(239, 68, 68, 0.1)',
    borderWidth: 1,
    borderColor: 'rgba(239, 68, 68, 0.3)',
    paddingVertical: 14,
    borderRadius: 16,
    alignItems: 'center',
    justifyContent: 'center',
  },
  dangerButtonText: { color: '#ef4444', fontSize: 14, fontWeight: 'bold' },
});
