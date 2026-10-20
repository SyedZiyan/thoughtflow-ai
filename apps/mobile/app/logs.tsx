import React, { useState, useEffect } from 'react';
import {
  StyleSheet,
  Text,
  View,
  ScrollView,
  ActivityIndicator
} from 'react-native';
import { mobileApi } from '../src/api/mobileClient';

export default function LogsScreen() {
  const [loading, setLoading] = useState(true);
  const [logs, setLogs] = useState<any[]>([]);

  useEffect(() => {
    mobileApi.getLogs()
      .then(setLogs)
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
      <Text style={styles.title}>Execution Logs</Text>
      <Text style={styles.subtitle}>Audit logs of daily generation, rendering, and uploads.</Text>

      <View style={styles.list}>
        {logs.map((log) => (
          <View key={log.id} style={styles.card}>
            <Text style={styles.message}>{log.message}</Text>
            <View style={styles.footer}>
              <Text style={styles.typeTag}>{log.event_type}</Text>
              <Text style={styles.time}>{new Date(log.created_at).toLocaleTimeString()}</Text>
            </View>
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
  list: { gap: 10 },
  card: {
    backgroundColor: '#12151F',
    borderRadius: 14,
    padding: 14,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.05)',
  },
  message: { color: '#ffffff', fontSize: 13, lineHeight: 18, marginBottom: 8 },
  footer: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  typeTag: {
    color: '#c084fc',
    fontSize: 10,
    fontWeight: 'bold',
    backgroundColor: 'rgba(167, 103, 247, 0.15)',
    paddingHorizontal: 8,
    paddingVertical: 2,
    borderRadius: 6,
    textTransform: 'uppercase',
  },
  time: { color: '#64748b', fontSize: 10, fontFamily: 'monospace' },
});
