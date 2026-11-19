import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider } from '../features/auth/AuthContext';
import { LoginPage } from '../features/auth/LoginPage';
import { OrchardsPage } from '../features/orchards/OrchardsPage';
import { HarvestRequestsPage } from '../features/harvest/HarvestRequestsPage';
import { OrchardsMapPage } from '../features/map/OrchardsMapPage';
import { CollectionRequestsPage } from '../features/collection/CollectionRequestsPage';
import { TourPlannerPage } from '../features/tours/TourPlannerPage';
import { AlertsQueuePage } from '../features/alerts/AlertsQueuePage';
import { TeamLeaderToursPage } from '../features/leader/TeamLeaderToursPage';
import { PressPage } from '../features/press/PressPage';
import { FarmerTrackingPage } from '../features/tracking/FarmerTrackingPage';
import { ResourcesPage } from '../features/logistics/ResourcesPage';
import { DashboardPage } from '../features/dashboard/DashboardPage';
import { AdminPage } from '../features/admin/AdminPage';
import { HelpPage } from '../features/help/HelpPage';
import { ProtectedRoute } from '../components/ProtectedRoute';
import { Layout } from './Layout';

const queryClient = new QueryClient();

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
                path="/farmer/tracking"
                element={
                  <ProtectedRoute roles={['FARMER']}>
                    <FarmerTrackingPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/collection/requests"
                element={
                  <ProtectedRoute roles={['COLLECTION_MANAGER', 'ADMIN']}>
                    <CollectionRequestsPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/collection/tours"
                element={
                  <ProtectedRoute roles={['COLLECTION_MANAGER', 'ADMIN']}>
                    <TourPlannerPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/collection/alerts"
                element={
                  <ProtectedRoute roles={['COLLECTION_MANAGER', 'ADMIN']}>
                    <AlertsQueuePage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/leader/tours"
                element={
                  <ProtectedRoute roles={['TEAM_LEADER']}>
                    <TeamLeaderToursPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/logistics/resources"
                element={
                  <ProtectedRoute roles={['LOGISTICS_MANAGER', 'COLLECTION_MANAGER', 'ADMIN']}>
                    <ResourcesPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/press/receptions"
                element={
                  <ProtectedRoute roles={['PRESS_MANAGER', 'COLLECTION_MANAGER', 'ADMIN']}>
                    <PressPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/dashboard"
                element={
                  <ProtectedRoute roles={['COLLECTION_MANAGER', 'PRESS_MANAGER', 'ADMIN']}>
                    <DashboardPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/admin/users"
                element={
                  <ProtectedRoute roles={['ADMIN']}>
                    <AdminPage />
                  </ProtectedRoute>
                }
              />
              <Route path="/help" element={<HelpPage />} />
            </Route>
          </Routes>
        </BrowserRouter>
      </AuthProvider>
    </QueryClientProvider>
  );
}
