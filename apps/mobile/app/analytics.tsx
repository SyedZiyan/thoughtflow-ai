import React, { useState, useEffect } from 'react';
import {
  StyleSheet,
  Text,
  View,
  ScrollView,
  ActivityIndicator
} from 'react-native';
import { mobileApi } from '../src/api/mobileClient';

export default function AnalyticsScreen() {
  const [loading, setLoading] = useState(true);
  const [data, setData] = useState<any>(null);

  useEffect(() => {
    mobileApi.getAnalytics()
      .then(setData)
      .catch((err) => console.warn(err.message))
      .finally(() => setLoading(false));
  }, []);

  if (loading) {
    return (
      <View style={styles.centerContainer}>
        <ActivityIndicator color="#a767f7" />
      </View>
    );
  }

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.content}>
      <Text style={styles.title}>Channel Analytics</Text>
      <Text style={styles.subtitle}>Audience reach and performance across your video themes.</Text>

      <View style={styles.metricsRow}>
        <View style={styles.metricCard}>
          <Text style={styles.metricLabel}>TOTAL VIEWS</Text>
          <Text style={styles.metricValue}>{data?.total_views?.toLocaleString() || 0}</Text>
        </View>

        <View style={styles.metricCard}>
          <Text style={styles.metricLabel}>TOTAL LIKES</Text>
          <Text style={styles.metricValue}>{data?.total_likes?.toLocaleString() || 0}</Text>
        </View>
      </View>

      <View style={styles.card}>
        <Text style={styles.cardTitle}>Performance by Theme</Text>
        {(data?.performance_by_theme || []).map((pt: any) => (
          <View key={pt.theme} style={styles.themeRow}>
            <Text style={styles.themeName}>{pt.theme}</Text>
            <Text style={styles.themeStats}>{pt.count} videos • {pt.views} views</Text>
          </View>
        ))}
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#0B0D13' },
  content: { padding: 20, paddingBottom: 40 },
  centerContainer: { flex: 1, backgroundColor: '#0B0D13', justifyContent: 'center', alignItems: 'center' },
  title: { color: '#ffffff', fontSize: 24, fontWeight: 'bold' },
  subtitle: { color: '#94a3b8', fontSize: 12, marginTop: 4, marginBottom: 20 },
  metricsRow: { flexDirection: 'row', gap: 12, marginBottom: 16 },
  metricCard: {
    flex: 1,
    backgroundColor: '#12151F',
    borderRadius: 16,
    padding: 16,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.06)',
  },
  metricLabel: { color: '#64748b', fontSize: 10, fontWeight: 'bold', marginBottom: 6 },
  metricValue: { color: '#ffffff', fontSize: 20, fontWeight: 'bold' },
  card: {
    backgroundColor: '#12151F',
    borderRadius: 18,
    padding: 18,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.05)',
  },
  cardTitle: { color: '#ffffff', fontSize: 14, fontWeight: 'bold', marginBottom: 14 },
  themeRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    paddingVertical: 10,
    borderBottomWidth: 1,
    borderBottomColor: 'rgba(255,255,255,0.04)',
  },
  themeName: { color: '#ffffff', fontSize: 13, textTransform: 'capitalize', fontWeight: '500' },
  themeStats: { color: '#94a3b8', fontSize: 11 },
});
