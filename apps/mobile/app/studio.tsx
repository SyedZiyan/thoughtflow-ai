import React, { useState, useEffect } from 'react';
import {
  StyleSheet,
  Text,
  View,
  ScrollView,
  TouchableOpacity,
  ActivityIndicator,
  Alert,
  Switch
} from 'react-native';
import { mobileApi } from '../src/api/mobileClient';

const THEMES = [
  { id: 'cozy_balcony', label: 'Cozy Sunset Balcony (Ref 1)', color: '#f5c276' },
  { id: 'mountain_summit', label: 'Alpine Sunrise Summit (Ref 2)', color: '#60a5fa' },
  { id: 'misty_forest', label: 'Misty Pine Forest', color: '#34d399' },
  { id: 'ocean_sunset', label: 'Ocean Twilight', color: '#fb923c' },
  { id: 'starry_night', label: 'Cosmic Starry Night', color: '#a855f7' },
  { id: 'golden_lake', label: 'Reflective Lake', color: '#fbbf24' },
  { id: 'rainy_window', label: 'Rainy Window', color: '#38bdf8' },
  { id: 'golden_desert', label: 'Golden Dunes', color: '#f59e0b' },
  { id: 'gradient_purple', label: 'Cosmic Purple (Grad)', color: '#a878ff' },
  { id: 'geometric_dark', label: 'Emerald Obsidian (Grad)', color: '#10b981' }
];

const DURATIONS = [15, 30, 45, 60];

export default function MobileVideoStudio() {
  const [loading, setLoading] = useState(true);
  const [thoughts, setThoughts] = useState<any[]>([]);
  const [selectedThoughtId, setSelectedThoughtId] = useState<string>('');
  const [selectedTheme, setSelectedTheme] = useState('cozy_balcony');
  const [selectedDuration, setSelectedDuration] = useState(15);
  const [narrationEnabled, setNarrationEnabled] = useState(true);
  const [ambientEnabled, setAmbientEnabled] = useState(true);
  const [rendering, setRendering] = useState(false);
  const [renderedVideo, setRenderedVideo] = useState<any>(null);

  useEffect(() => {
    mobileApi.getThoughts()
      .then((data) => {
        setThoughts(data || []);
        if (data && data.length > 0) {
          setSelectedThoughtId(data[0].id);
        }
      })
      .catch((err) => console.warn('Fetch thoughts error:', err.message))
      .finally(() => setLoading(false));
  }, []);

  const handleRender = async () => {
    if (!selectedThoughtId) {
      Alert.alert('Selection Required', 'Please select a thought to render into a video.');
      return;
    }

    setRendering(true);
    setRenderedVideo(null);

    try {
      const res = await mobileApi.renderVideo({
        thought_id: selectedThoughtId,
        duration_seconds: selectedDuration,
        visual_theme: selectedTheme,
        animation_style: 'fade_zoom',
        typography: 'editorial_serif',
        text_placement: 'center',
        narration_enabled: narrationEnabled,
        ambient_enabled: ambientEnabled,
        narration_voice: 'en-US-ChristopherNeural'
      });
      setRenderedVideo(res);
      Alert.alert('Video Rendered!', `Successfully rendered 1080x1920 MP4 (${res.resolution || '1080x1920'}).`);
    } catch (err: any) {
      Alert.alert('Render Failed', err.message);
    } finally {
      setRendering(false);
    }
  };

  const selectedThought = thoughts.find((t) => t.id === selectedThoughtId);

  if (loading) {
    return (
      <View style={styles.centerContainer}>
        <ActivityIndicator size="large" color="#a767f7" />
        <Text style={styles.loadingText}>Loading Video Studio assets...</Text>
      </View>
    );
  }

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.content}>
      <Text style={styles.title}>Vertical Video Studio</Text>
      <Text style={styles.subtitle}>
        Transform your thoughts into high-converting 1080×1920 animated YouTube Shorts.
      </Text>

      {/* Thought Selector */}
      <View style={styles.card}>
        <Text style={styles.cardHeader}>1. SELECT SOURCE THOUGHT</Text>
        {thoughts.length === 0 ? (
          <Text style={styles.emptyText}>No thoughts found. Generate one in the Generate tab first!</Text>
        ) : (
          <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.horizontalScroll}>
            {thoughts.slice(0, 8).map((t) => (
              <TouchableOpacity
                key={t.id}
                style={[
                  styles.thoughtPill,
                  selectedThoughtId === t.id && styles.thoughtPillActive
                ]}
                onPress={() => setSelectedThoughtId(t.id)}
              >
                <Text style={styles.pillTheme}>{t.theme?.toUpperCase()}</Text>
                <Text style={styles.pillText} numberOfLines={2}>{t.text}</Text>
              </TouchableOpacity>
            ))}
          </ScrollView>
        )}

        {selectedThought && (
          <View style={styles.previewBox}>
            <Text style={styles.previewQuote}>"{selectedThought.text}"</Text>
            <Text style={styles.previewMeta}>#{selectedThought.theme} • {selectedThought.language?.toUpperCase()}</Text>
          </View>
        )}
      </View>

      {/* Visual Theme Palette */}
      <View style={styles.card}>
        <Text style={styles.cardHeader}>2. VISUAL LUXURY PALETTE</Text>
        <View style={styles.paletteGrid}>
          {THEMES.map((theme) => (
            <TouchableOpacity
              key={theme.id}
              style={[
                styles.paletteButton,
                selectedTheme === theme.id && styles.paletteButtonActive
              ]}
              onPress={() => setSelectedTheme(theme.id)}
            >
              <View style={[styles.paletteDot, { backgroundColor: theme.color }]} />
              <Text style={styles.paletteText}>{theme.label}</Text>
            </TouchableOpacity>
          ))}
        </View>
      </View>

      {/* Video Duration */}
      <View style={styles.card}>
        <Text style={styles.cardHeader}>3. DURATION (SECONDS)</Text>
        <View style={styles.durationRow}>
          {DURATIONS.map((dur) => (
            <TouchableOpacity
              key={dur}
              style={[
                styles.durButton,
                selectedDuration === dur && styles.durButtonActive
              ]}
              onPress={() => setSelectedDuration(dur)}
            >
              <Text style={[
                styles.durText,
                selectedDuration === dur && styles.durTextActive
              ]}>
                {dur}s
              </Text>
            </TouchableOpacity>
          ))}
        </View>
      </View>

      {/* Audio Engine Toggles */}
      <View style={styles.card}>
        <Text style={styles.cardHeader}>4. AUDIO CONFIGURATION</Text>
        <View style={styles.toggleRow}>
          <View>
            <Text style={styles.toggleLabel}>Neural AI Narration</Text>
            <Text style={styles.toggleSub}>EdgeTTS Christopher Neural Voice</Text>
          </View>
          <Switch
            value={narrationEnabled}
            onValueChange={setNarrationEnabled}
            trackColor={{ false: '#334155', true: '#a767f7' }}
            thumbColor="#ffffff"
          />
        </View>

        <View style={[styles.toggleRow, { borderTopWidth: 1, borderTopColor: 'rgba(255,255,255,0.06)', paddingTop: 12 }]}>
          <View>
            <Text style={styles.toggleLabel}>Ambient Harmonic Chords</Text>
            <Text style={styles.toggleSub}>Synthetic multi-layer relaxing background audio</Text>
          </View>
          <Switch
            value={ambientEnabled}
            onValueChange={setAmbientEnabled}
            trackColor={{ false: '#334155', true: '#a767f7' }}
            thumbColor="#ffffff"
          />
        </View>
      </View>

      {/* Render Action */}
      <TouchableOpacity
        style={[styles.renderButton, rendering && styles.renderButtonDisabled]}
        onPress={handleRender}
        disabled={rendering}
      >
        {rendering ? (
          <View style={styles.renderLoadingRow}>
            <ActivityIndicator size="small" color="#ffffff" />
            <Text style={styles.renderButtonText}>Rendering 1080x1920 MP4 via FFmpeg...</Text>
          </View>
        ) : (
          <Text style={styles.renderButtonText}>Render 9:16 Short Video</Text>
        )}
      </TouchableOpacity>

      {/* Render Output Info */}
      {renderedVideo && (
        <View style={styles.resultCard}>
          <Text style={styles.resultBadge}>SUCCESSFULLY RENDERED</Text>
          <Text style={styles.resultTitle}>Resolution: {renderedVideo.resolution || '1080x1920'}</Text>
          <Text style={styles.resultSub}>Duration: {renderedVideo.duration_seconds}s • Status: {renderedVideo.status}</Text>
          <Text style={styles.resultPath} numberOfLines={1}>{renderedVideo.file_path}</Text>
        </View>
      )}
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
    padding: 18,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.06)',
    marginBottom: 16,
  },
  cardHeader: { color: '#a767f7', fontSize: 11, fontWeight: '700', letterSpacing: 0.5, marginBottom: 12 },
  emptyText: { color: '#64748b', fontSize: 12 },
  horizontalScroll: { marginBottom: 12 },
  thoughtPill: {
    backgroundColor: '#1a1e2c',
    borderRadius: 14,
    padding: 12,
    marginRight: 10,
    width: 220,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.08)',
  },
  thoughtPillActive: {
    borderColor: '#a767f7',
    backgroundColor: 'rgba(167, 103, 247, 0.12)',
  },
  pillTheme: { color: '#c084fc', fontSize: 9, fontWeight: '800', marginBottom: 4 },
  pillText: { color: '#e2e8f0', fontSize: 11, lineHeight: 16 },
  previewBox: {
    backgroundColor: '#161926',
    borderRadius: 14,
    padding: 14,
    borderWidth: 1,
    borderColor: 'rgba(167, 103, 247, 0.2)',
  },
  previewQuote: { color: '#ffffff', fontSize: 13, fontStyle: 'italic', lineHeight: 18, marginBottom: 6 },
  previewMeta: { color: '#64748b', fontSize: 10, fontWeight: 'bold' },
  paletteGrid: { flexDirection: 'row', flexWrap: 'wrap', gap: 10 },
  paletteButton: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#1a1e2c',
    paddingVertical: 10,
    paddingHorizontal: 14,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.06)',
    width: '48%',
  },
  paletteButtonActive: {
    borderColor: '#a767f7',
    backgroundColor: 'rgba(167, 103, 247, 0.12)',
  },
  paletteDot: { width: 10, height: 10, borderRadius: 5, marginRight: 8 },
  paletteText: { color: '#e2e8f0', fontSize: 11, fontWeight: '600' },
  durationRow: { flexDirection: 'row', gap: 10 },
  durButton: {
    flex: 1,
    backgroundColor: '#1a1e2c',
    paddingVertical: 12,
    borderRadius: 12,
    alignItems: 'center',
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.06)',
  },
  durButtonActive: {
    borderColor: '#a767f7',
    backgroundColor: '#8e3ef0',
  },
  durText: { color: '#94a3b8', fontSize: 13, fontWeight: 'bold' },
  durTextActive: { color: '#ffffff' },
  toggleRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginVertical: 6 },
  toggleLabel: { color: '#ffffff', fontSize: 13, fontWeight: '600' },
  toggleSub: { color: '#64748b', fontSize: 10, marginTop: 2 },
  renderButton: {
    backgroundColor: '#8e3ef0',
    paddingVertical: 16,
    borderRadius: 18,
    alignItems: 'center',
    justifyContent: 'center',
    marginTop: 8,
  },
  renderButtonDisabled: { opacity: 0.6 },
  renderLoadingRow: { flexDirection: 'row', alignItems: 'center', gap: 10 },
  renderButtonText: { color: '#ffffff', fontSize: 14, fontWeight: 'bold' },
  resultCard: {
    backgroundColor: '#161926',
    borderRadius: 18,
    padding: 16,
    borderWidth: 1,
    borderColor: 'rgba(16, 185, 129, 0.3)',
    marginTop: 16,
  },
  resultBadge: { color: '#10b981', fontSize: 10, fontWeight: 'bold', letterSpacing: 1, marginBottom: 6 },
  resultTitle: { color: '#ffffff', fontSize: 14, fontWeight: 'bold', marginBottom: 2 },
  resultSub: { color: '#94a3b8', fontSize: 11, marginBottom: 6 },
  resultPath: { color: '#64748b', fontSize: 10, fontFamily: 'monospace' },
});
