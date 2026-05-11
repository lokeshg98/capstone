import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import LoginPage      from '@/features/auth/LoginPage';
import AuthCallback   from '@/features/auth/AuthCallback';
import DashboardPage  from '@/features/dashboard/DashboardPage';
import WorkspacePage  from '@/features/workspace/WorkspacePage';
import ProtectedRoute from '@/components/ProtectedRoute';

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* Public */}
        <Route path="/login"         element={<LoginPage />} />
        <Route path="/auth/callback" element={<AuthCallback />} />

        {/* Protected — redirects to /login if not authenticated */}
        <Route element={<ProtectedRoute />}>
          <Route path="/dashboard" element={<DashboardPage />} />

          {/* Workspace: channel is optional — WorkspacePage auto-selects the first one */}
          <Route path="/workspaces/:wsId"                        element={<WorkspacePage />} />
          <Route path="/workspaces/:wsId/channels/:channelId"    element={<WorkspacePage />} />
        </Route>

        {/* Default redirect */}
        <Route path="*" element={<Navigate to="/dashboard" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
