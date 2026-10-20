import React, { useState } from 'react';
import {
  StyleSheet,
  Text,
  View,
  ScrollView,
  TouchableOpacity,
  TextInput,
  ActivityIndicator,
  Alert
} from 'react-native';
import { mobileApi } from '../../src/api/mobileClient';

const THEMES = ['discipline', 'motivation', 'consistency', 'resilience', 'success', 'mindfulness'];

export default function GenerateScreen() {
  const [theme, setTheme] = useState('discipline');
  const [customPrompt, setCustomPrompt] = useState('');
  const [generating, setGenerating] = useState(false);
  const [rendering, setRendering] = useState(false);
  const [thought, setThought] = useState<any>(null);

  const handleGenerate = async () => {
    setGenerating(true);
    try {
      const res = await mobileApi.generateThought({
        theme,
        custom_prompt: customPrompt,
      });
      setThought(res);
    } catch (err: any) {
      Alert.alert('Error', err.message);
    } finally {
      setGenerating(false);
    }
  };

  const handleRender = async () => {
    if (!thought) return;
    setRendering(true);
    try {
      const res = await mobileApi.renderVideo({
        thought_id: thought.id,
        duration_seconds: 15,
        visual_theme: 'gradient_purple'
      });
      Alert.alert('Success', '1080x1920 MP4 Video Rendered Successfully!');
    } catch (err: any) {
      Alert.alert('Render Error', err.message);
    } finally {
      setRendering(false);
    }
  };

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.content}>
      <Text style={styles.title}>Generate Thought</Text>
      <Text style={styles.subtitle}>Select category theme and generate original motivational wisdom.</Text>

      {/* Theme chips */}
      <Text style={styles.label}>SELECT THEME</Text>
      <View style={styles.chipsRow}>
        {THEMES.map((t) => (
          <TouchableOpacity
            key={t}
            style={[styles.chip, theme === t && styles.chipActive]}
            onPress={() => setTheme(t)}
          >
            <Text style={[styles.chipText, theme === t && styles.chipTextActive]}>{t}</Text>
          </TouchableOpacity>
        ))}
      </View>

      <Text style={styles.label}>CUSTOM GUIDANCE (OPTIONAL)</Text>
      <TextInput
        style={styles.input}
        placeholder="e.g. Focus on morning routine..."
        placeholderTextColor="#64748b"
        value={customPrompt}
        onChangeText={setCustomPrompt}
      />

      <TouchableOpacity
        style={styles.generateButton}
        onPress={handleGenerate}
        disabled={generating}
      >
        {generating ? (
          <ActivityIndicator color="#ffffff" />
        ) : (
          <Text style={styles.generateButtonText}>Generate Thought</Text>
        )}
      </TouchableOpacity>

      {/* Result Card */}
      {thought && (
        <View style={styles.resultCard}>
          <Text style={styles.themeTag}>{thought.theme.toUpperCase()}</Text>
          <Text style={styles.thoughtText}>"{thought.text}"</Text>
          <Text style={styles.thoughtTitle}>{thought.title}</Text>

          <View style={styles.tagsRow}>
            {(thought.hashtags || []).map((tag: string, i: number) => (
              <Text key={i} style={styles.tagText}>#{tag}</Text>
            ))}
          </View>

          <TouchableOpacity
            style={styles.renderButton}
            onPress={handleRender}
            disabled={rendering}
          >
            {rendering ? (
              <ActivityIndicator color="#ffffff" />
            ) : (
              <Text style={styles.renderButtonText}>Render 9:16 Video in Studio</Text>
            )}
          </TouchableOpacity>
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
  label: { color: '#64748b', fontSize: 10, fontWeight: 'bold', letterSpacing: 0.8, marginBottom: 8 },
  chipsRow: { flexDirection: 'row', flexWrap: 'wrap', gap: 8, marginBottom: 18 },
  chip: {
    backgroundColor: '#161926',
    paddingHorizontal: 14,
    paddingVertical: 8,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.05)',
  },
  chipActive: {
    backgroundColor: '#8e3ef0',
    borderColor: '#a767f7',
  },
  chipText: { color: '#94a3b8', fontSize: 12, textTransform: 'capitalize' },
  chipTextActive: { color: '#ffffff', fontWeight: 'bold' },
  input: {
    backgroundColor: '#161926',
    borderRadius: 14,
    padding: 12,
    color: '#ffffff',
    fontSize: 13,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.08)',
    marginBottom: 20,
  },
  generateButton: {
    backgroundColor: '#8e3ef0',
    paddingVertical: 14,
    borderRadius: 16,
    alignItems: 'center',
    marginBottom: 24,
  },
  generateButtonText: { color: '#ffffff', fontSize: 14, fontWeight: 'bold' },
  resultCard: {
    backgroundColor: '#12151F',
    borderRadius: 20,
    padding: 20,
    borderWidth: 1,
    borderColor: 'rgba(167, 103, 247, 0.25)',
  },
  themeTag: { color: '#a767f7', fontSize: 10, fontWeight: 'bold', letterSpacing: 1, marginBottom: 8 },
  thoughtText: { color: '#ffffff', fontSize: 16, fontStyle: 'italic', lineHeight: 24, marginBottom: 10 },
  thoughtTitle: { color: '#cbd5e1', fontSize: 13, fontWeight: '600', marginBottom: 12 },
  tagsRow: { flexDirection: 'row', flexWrap: 'wrap', gap: 6, marginBottom: 16 },
  tagText: { color: '#818cf8', fontSize: 11, fontFamily: 'monospace' },
  renderButton: {
    backgroundColor: '#4f46e5',
    paddingVertical: 12,
    borderRadius: 14,
    alignItems: 'center',
  },
  renderButtonText: { color: '#ffffff', fontSize: 13, fontWeight: 'bold' },
});
