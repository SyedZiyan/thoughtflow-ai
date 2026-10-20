import React, { useState, useEffect } from 'react';
import {
  StyleSheet,
  Text,
  View,
  ScrollView,
  TouchableOpacity,
  Switch,
  TextInput,
  ActivityIndicator,
  Alert
} from 'react-native';
import { mobileApi, getApiHost, setApiHost } from '../../src/api/mobileClient';

export default function SettingsScreen() {
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [automationEnabled, setAutomationEnabled] = useState(false);
  const [generationTime, setGenerationTime] = useState('07:00');
  const [publishingTime, setPublishingTime] = useState('09:00');
  const [dailyLimit, setDailyLimit] = useState('5');
  const [customHost, setCustomHost] = useState(getApiHost());

  useEffect(() => {
    mobileApi.getSettings()
      .then((data) => {
        setAutomationEnabled(data.automation_enabled);
        setGenerationTime(data.generation_time);
        setPublishingTime(data.publishing_time);
        setDailyLimit(String(data.daily_limit));
      })
      .catch((err) => console.warn('Settings load notice:', err.message))
      .finally(() => setLoading(false));
  }, []);

  const handleSave = async () => {
    setSaving(true);
    setApiHost(customHost);
    try {
      await mobileApi.updateSettings({
        automation_enabled: automationEnabled,
        generation_time: generationTime,
        publishing_time: publishingTime,
        daily_limit: parseInt(dailyLimit) || 5,
      });
      Alert.alert('Saved', 'Settings updated on server!');
    } catch (err: any) {
      Alert.alert('Error', err.message);
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <View style={styles.centerContainer}>
        <ActivityIndicator color="#a767f7" />
      </View>
    );
  }

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.content}>
      <Text style={styles.title}>Settings & Automation</Text>
      <Text style={styles.subtitle}>Configure daily automated pipeline and server connection.</Text>

      {/* Automation Switch */}
      <View style={styles.settingRow}>
        <View style={{ flex: 1 }}>
          <Text style={styles.settingLabel}>Daily Automation</Text>
          <Text style={styles.settingSub}>Trigger daily thought & video at set time</Text>
        </View>
        <Switch
          value={automationEnabled}
          onValueChange={setAutomationEnabled}
          trackColor={{ false: '#334155', true: '#8e3ef0' }}
          thumbColor="#ffffff"
        />
      </View>

      {/* Time inputs */}
      <Text style={styles.inputLabel}>GENERATION TIME (UTC)</Text>
      <TextInput
        style={styles.input}
        value={generationTime}
        onChangeText={setGenerationTime}
        placeholder="07:00"
        placeholderTextColor="#64748b"
      />

      <Text style={styles.inputLabel}>PUBLISHING TIME (UTC)</Text>
      <TextInput
        style={styles.input}
        value={publishingTime}
        onChangeText={setPublishingTime}
        placeholder="09:00"
        placeholderTextColor="#64748b"
      />

      <Text style={styles.inputLabel}>DAILY GENERATION LIMIT</Text>
      <TextInput
        style={styles.input}
        value={dailyLimit}
        onChangeText={setDailyLimit}
        keyboardType="numeric"
        placeholder="5"
        placeholderTextColor="#64748b"
      />

      <Text style={styles.inputLabel}>FASTAPI SERVER HOST (IP / URL)</Text>
      <TextInput
        style={styles.input}
        value={customHost}
        onChangeText={setCustomHost}
        placeholder="http://10.0.2.2:8000"
        placeholderTextColor="#64748b"
        autoCapitalize="none"
      />

      <TouchableOpacity
        style={styles.saveButton}
        onPress={handleSave}
        disabled={saving}
      >
        {saving ? (
          <ActivityIndicator color="#ffffff" />
        ) : (
          <Text style={styles.saveButtonText}>Save Settings</Text>
        )}
      </TouchableOpacity>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#0B0D13' },
  content: { padding: 20, paddingBottom: 40 },
  centerContainer: { flex: 1, backgroundColor: '#0B0D13', justifyContent: 'center', alignItems: 'center' },
  title: { color: '#ffffff', fontSize: 24, fontWeight: 'bold' },
  subtitle: { color: '#94a3b8', fontSize: 12, marginTop: 4, marginBottom: 20 },
  settingRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    backgroundColor: '#12151F',
    padding: 16,
    borderRadius: 16,
    marginBottom: 20,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.05)',
  },
  settingLabel: { color: '#ffffff', fontSize: 14, fontWeight: 'bold' },
  settingSub: { color: '#64748b', fontSize: 11, marginTop: 2 },
  inputLabel: { color: '#64748b', fontSize: 10, fontWeight: 'bold', letterSpacing: 0.8, marginBottom: 6 },
  input: {
    backgroundColor: '#12151F',
    borderRadius: 14,
    padding: 12,
    color: '#ffffff',
    fontSize: 13,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.06)',
    marginBottom: 16,
    fontFamily: 'monospace',
  },
  saveButton: {
    backgroundColor: '#8e3ef0',
    paddingVertical: 14,
    borderRadius: 16,
    alignItems: 'center',
    marginTop: 10,
  },
  saveButtonText: { color: '#ffffff', fontSize: 14, fontWeight: 'bold' },
});
