import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuthStore } from '@/features/auth/useAuthStore';

/**
 * Wrap any routes that require authentication.
 * Unauthenticated users are redirected to /login with a "from" param
 * so we can send them back after login (wired up in Milestone 2).
 */
export default function ProtectedRoute() {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated());
  const location        = useLocation();

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  return <Outlet />;
}
