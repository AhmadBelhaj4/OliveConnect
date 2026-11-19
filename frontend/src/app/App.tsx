import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider } from '../features/auth/AuthContext';
import { LoginPage } from '../features/auth/LoginPage';
import { OrchardsPage } from '../features/orchards/OrchardsPage';
import { HarvestRequestsPage } from '../features/harvest/HarvestRequestsPage';
import { OrchardsMapPage } from '../features/map/OrchardsMapPage';
import { ProtectedRoute } from '../components/ProtectedRoute';
import { Layout } from './Layout';

const queryClient = new QueryClient();

function RolePlaceholder({ title }: { title: string }) {
  return (
    <div className="card">
      <h2>{title}</h2>
      <p>Squelette par rôle prêt (C1/C3) — branchement API en semaine 2 (C7 à C12).</p>
    </div>
  );
}

export function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <BrowserRouter>
          <Routes>
            <Route element={<Layout />}>
              <Route index element={<Navigate to="/map" replace />} />
              <Route path="/login" element={<LoginPage />} />
              <Route path="/map" element={<OrchardsMapPage />} />
              <Route
                path="/farmer/orchards"
                element={
                  <ProtectedRoute roles={['FARMER']}>
                    <OrchardsPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/farmer/requests"
                element={
                  <ProtectedRoute roles={['FARMER']}>
                    <HarvestRequestsPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/collection/requests"
                element={
                  <ProtectedRoute roles={['COLLECTION_MANAGER', 'ADMIN']}>
                    <RolePlaceholder title="File des demandes & planification des tournées (S2 · C7/C8)" />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/leader/tours"
                element={
                  <ProtectedRoute roles={['TEAM_LEADER']}>
                    <RolePlaceholder title="Mes tournées assignées — Chef d'équipe (S2 · C9)" />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/logistics/resources"
                element={
                  <ProtectedRoute roles={['LOGISTICS_MANAGER', 'COLLECTION_MANAGER', 'ADMIN']}>
                    <RolePlaceholder title="Gestion des ressources — Logistique (S2 · C12)" />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/press/receptions"
                element={
                  <ProtectedRoute roles={['PRESS_MANAGER', 'COLLECTION_MANAGER', 'ADMIN']}>
                    <RolePlaceholder title="Réceptions au pressoir (S2 · C10)" />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/dashboard"
                element={
                  <ProtectedRoute roles={['COLLECTION_MANAGER', 'PRESS_MANAGER', 'ADMIN']}>
                    <RolePlaceholder title="Tableau de bord des performances (S3 · C14)" />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/admin/users"
                element={
                  <ProtectedRoute roles={['ADMIN']}>
                    <RolePlaceholder title="Administration des comptes & imports (S3 · C15)" />
                  </ProtectedRoute>
                }
              />
            </Route>
          </Routes>
        </BrowserRouter>
      </AuthProvider>
    </QueryClientProvider>
  );
}
