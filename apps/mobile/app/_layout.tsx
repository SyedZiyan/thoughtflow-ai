import React from 'react';
import { Stack } from 'expo-router';
import { StatusBar } from 'expo-status-bar';

export default function RootLayout() {
  return (
    <>
      <StatusBar style="light" />
      <Stack
        screenOptions={{
          headerStyle: { backgroundColor: '#0B0D13' },
          headerTintColor: '#ffffff',
          headerTitleStyle: { fontWeight: 'bold' },
          contentStyle: { backgroundColor: '#0B0D13' },
        }}
      >
        <Stack.Screen name="(tabs)" options={{ headerShown: false }} />
        <Stack.Screen name="studio" options={{ title: 'Video Studio' }} />
        <Stack.Screen name="youtube" options={{ title: 'YouTube Channel' }} />
        <Stack.Screen name="automation" options={{ title: 'Automation Monitor' }} />
        <Stack.Screen name="calendar" options={{ title: 'Publishing Calendar' }} />
        <Stack.Screen name="login" options={{ title: 'Creator Login' }} />
        <Stack.Screen name="analytics" options={{ title: 'Channel Analytics' }} />
        <Stack.Screen name="logs" options={{ title: 'Execution Logs' }} />
      </Stack>
    </>
  );
}
