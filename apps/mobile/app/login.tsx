import React, { useState } from 'react';
import {
  StyleSheet,
  Text,
  View,
  TextInput,
  TouchableOpacity,
  ActivityIndicator,
  Alert
} from 'react-native';
import { useRouter } from 'expo-router';

export default function LoginScreen() {
  const router = useRouter();
  const [email, setEmail] = useState('creator@thoughtflow.local');
  const [password, setPassword] = useState('thoughtflow2026');
  const [loading, setLoading] = useState(false);

  const handleLogin = async () => {
    setLoading(true);
    setTimeout(() => {
      setLoading(false);
      Alert.alert('Success', 'Authenticated as Creator!');
      router.replace('/(tabs)');
    }, 600);
  };

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Welcome Back</Text>
      <Text style={styles.subtitle}>Sign in to manage your automated YouTube Shorts.</Text>

      <Text style={styles.label}>EMAIL ADDRESS</Text>
      <TextInput
        style={styles.input}
        value={email}
        onChangeText={setEmail}
        autoCapitalize="none"
        keyboardType="email-address"
      />

      <Text style={styles.label}>PASSWORD</Text>
      <TextInput
        style={styles.input}
        value={password}
        onChangeText={setPassword}
        secureTextEntry
      />

      <TouchableOpacity
        style={styles.button}
        onPress={handleLogin}
        disabled={loading}
      >
        {loading ? (
          <ActivityIndicator color="#ffffff" />
        ) : (
          <Text style={styles.buttonText}>Sign In</Text>
        )}
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#0B0D13', padding: 24, justifyContent: 'center' },
  title: { color: '#ffffff', fontSize: 26, fontWeight: '800', marginBottom: 6 },
  subtitle: { color: '#94a3b8', fontSize: 13, marginBottom: 28 },
  label: { color: '#64748b', fontSize: 10, fontWeight: 'bold', letterSpacing: 0.8, marginBottom: 6 },
  input: {
    backgroundColor: '#12151F',
    borderRadius: 14,
    padding: 14,
    color: '#ffffff',
    fontSize: 14,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.06)',
    marginBottom: 16,
  },
  button: {
    backgroundColor: '#8e3ef0',
    paddingVertical: 14,
    borderRadius: 16,
    alignItems: 'center',
    marginTop: 10,
  },
  buttonText: { color: '#ffffff', fontSize: 14, fontWeight: 'bold' },
});
