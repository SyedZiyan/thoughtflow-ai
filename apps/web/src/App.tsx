import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { DashboardLayout } from './layouts/DashboardLayout';
import { Overview } from './pages/Overview';
import { GenerateThought } from './pages/GenerateThought';
import { VideoStudio } from './pages/VideoStudio';
import { ContentLibrary } from './pages/ContentLibrary';
import { PublishingCalendar } from './pages/PublishingCalendar';
import { AutomationSettings } from './pages/AutomationSettings';
import { YouTubeChannel } from './pages/YouTubeChannel';
import { Analytics } from './pages/Analytics';
import { ExecutionLogs } from './pages/ExecutionLogs';
import { SettingsPage } from './pages/SettingsPage';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 1000 * 30,
      retry: 1,
    },
  },
});

export const App: React.FC = () => {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<DashboardLayout />}>
            <Route index element={<Overview />} />
            <Route path="generate" element={<GenerateThought />} />
            <Route path="studio" element={<VideoStudio />} />
            <Route path="library" element={<ContentLibrary />} />
            <Route path="calendar" element={<PublishingCalendar />} />
            <Route path="automation" element={<AutomationSettings />} />
            <Route path="youtube" element={<YouTubeChannel />} />
            <Route path="analytics" element={<Analytics />} />
            <Route path="logs" element={<ExecutionLogs />} />
            <Route path="settings" element={<SettingsPage />} />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </QueryClientProvider>
  );
};

export default App;
