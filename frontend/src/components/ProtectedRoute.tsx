import { Navigate } from 'react-router-dom';
import { ShieldAlert } from 'lucide-react';
import { useAuth } from '../features/auth/AuthContext';
import { GlassCard } from './GlassCard';
import type { Role } from '../types/contract';

export function ProtectedRoute({ roles, children }: { roles: Role[]; children: JSX.Element }) {
  const { user, loading } = useAuth();
  if (loading) return <GlassCard className="p-6 text-sm text-olive-700">Chargement de la session…</GlassCard>;
  if (!user) return <Navigate to="/login" replace />;
  if (!roles.includes(user.role)) {
    return (
      <GlassCard className="p-6 flex items-center gap-3 text-[#b33a3a]">
        <ShieldAlert className="h-5 w-5 shrink-0" />
        <p className="text-sm font-medium">
          403 — Votre rôle (<code>{user.role}</code>) n&apos;autorise pas l&apos;accès à cet écran.
        </p>
      </GlassCard>
    );
  }
  return children;
}
