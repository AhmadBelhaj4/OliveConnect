import { useState } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import {
  Map,
  TreePine,
  Inbox,
  Route,
  ClipboardCheck,
  Boxes,
  Factory,
  BarChart3,
  Users,
  LogOut,
  LogIn,
  Menu,
  X,
} from 'lucide-react';
import { useAuth } from '../features/auth/AuthContext';
import { GlassCard } from '../components/GlassCard';
import { cn } from '../lib/utils';
import type { Role } from '../types/contract';

interface MenuItem {
  to: string;
  label: string;
  roles: 'PUBLIC' | Role[];
  icon: React.ComponentType<{ className?: string }>;
}

const MENU_ITEMS: MenuItem[] = [
  { to: '/map', label: 'Carte des vergers', roles: 'PUBLIC', icon: Map },
  { to: '/farmer/orchards', label: 'Mes vergers', roles: ['FARMER'], icon: TreePine },
  { to: '/farmer/requests', label: 'Mes demandes', roles: ['FARMER'], icon: Inbox },
  { to: '/collection/requests', label: 'File demandes & tournées', roles: ['COLLECTION_MANAGER', 'ADMIN'], icon: Route },
  { to: '/leader/tours', label: 'Mes tournées (terrain)', roles: ['TEAM_LEADER'], icon: ClipboardCheck },
  { to: '/logistics/resources', label: 'Ressources', roles: ['LOGISTICS_MANAGER', 'COLLECTION_MANAGER', 'ADMIN'], icon: Boxes },
  { to: '/press/receptions', label: 'Pressoir', roles: ['PRESS_MANAGER', 'COLLECTION_MANAGER', 'ADMIN'], icon: Factory },
  { to: '/dashboard', label: 'Tableau de bord', roles: ['COLLECTION_MANAGER', 'PRESS_MANAGER', 'ADMIN'], icon: BarChart3 },
  { to: '/admin/users', label: 'Comptes & imports', roles: ['ADMIN'], icon: Users },
];

export function Layout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [mobileOpen, setMobileOpen] = useState(false);

  const visibleItems = MENU_ITEMS.filter(
    (item) => item.roles === 'PUBLIC' || (user && item.roles.includes(user.role)),
  );

  return (
    <div className="min-h-screen flex flex-col">
      {/* TopBar Header */}
      <header className="sticky top-0 z-40 px-4 sm:px-6 py-3 glass-nav border-x-0 border-t-0">
        <div className="max-w-7xl mx-auto flex items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <button
              type="button"
              onClick={() => setMobileOpen(!mobileOpen)}
              className="lg:hidden p-2 rounded-lg glass-subtle text-olive-800 hover:bg-olive-900/5"
              aria-label="Toggle navigation"
            >
              {mobileOpen ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
            </button>

            <NavLink to="/map" className="flex items-center gap-2.5 text-inherit no-underline">
              <div className="h-10 w-10 rounded-xl bg-olive-600 text-cream-50 grid place-items-center font-display font-bold text-base shadow-sm">
                OC
              </div>
              <div className="leading-tight">
                <div className="font-display font-bold text-base sm:text-lg text-olive-900">
                  OliveConnect
                </div>
                <div className="text-[11px] text-olive-700/75 tracking-tight hidden sm:block">
                  Coopérative oléicole · Région de Sfax
                </div>
              </div>
            </NavLink>
          </div>

          <div className="flex items-center gap-3">
            {user ? (
              <div className="flex items-center gap-2.5">
                <div className="hidden sm:flex flex-col text-right leading-tight">
                  <span className="font-display font-semibold text-xs text-olive-900">
                    {user.fullName}
                  </span>
                  <span className="text-[10px] font-medium uppercase tracking-wider text-olive-700/80">
                    {user.role}
                  </span>
                </div>
                <button
                  type="button"
                  onClick={() => {
                    logout();
                    navigate('/login');
                  }}
                  className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg glass-subtle hover:bg-olive-600/15 text-xs font-semibold text-olive-900 transition-colors"
                >
                  <LogOut className="h-3.5 w-3.5 text-olive-700" />
                  <span className="hidden sm:inline">Déconnexion</span>
                </button>
              </div>
            ) : (
              <NavLink
                to="/login"
                className="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-lg bg-olive-600 hover:bg-olive-700 text-cream-50 text-xs font-semibold transition-colors shadow-sm"
              >
                <LogIn className="h-3.5 w-3.5" />
                <span>Connexion</span>
              </NavLink>
            )}
          </div>
        </div>
      </header>

      {/* Main Container with Sidebar + Content */}
      <div className="flex-1 max-w-7xl mx-auto w-full flex gap-6 px-4 sm:px-6 py-6">
        {/* Desktop Sidebar */}
        <aside className="hidden lg:flex w-64 shrink-0 sticky top-[84px] self-start">
          <GlassCard variant="subtle" className="w-full p-3 rounded-2xl space-y-1">
            <div className="px-3 py-2 text-[10px] font-bold uppercase tracking-wider text-olive-700/60">
              Navigation ({user ? user.role : 'Visiteur'})
            </div>
            <nav className="space-y-1">
              {visibleItems.map((item) => {
                const Icon = item.icon;
                return (
                  <NavLink
                    key={item.to}
                    to={item.to}
                    className={({ isActive }) =>
                      cn(
                        'flex items-center gap-2.5 px-3 py-2 rounded-xl text-xs font-medium transition-all',
                        isActive
                          ? 'bg-olive-600 text-cream-50 shadow-sm font-semibold'
                          : 'text-olive-800 hover:bg-olive-600/10 hover:text-olive-900',
                      )
                    }
                  >
                    <Icon className="h-4 w-4 shrink-0" />
                    <span>{item.label}</span>
                  </NavLink>
                );
              })}
            </nav>
          </GlassCard>
        </aside>

        {/* Mobile Navigation Drawer */}
        {mobileOpen && (
          <div className="lg:hidden fixed inset-0 z-30 bg-black/20 backdrop-blur-sm pt-16 px-4">
            <GlassCard variant="strong" className="p-4 rounded-2xl space-y-2 mt-2">
              <div className="flex items-center justify-between px-2 pb-2 border-b border-olive-900/10">
                <span className="text-xs font-bold uppercase tracking-wider text-olive-700">Menu</span>
                <button
                  type="button"
                  onClick={() => setMobileOpen(false)}
                  className="p-1 rounded-md text-olive-700 hover:bg-olive-900/5"
                >
                  <X className="h-4 w-4" />
                </button>
              </div>
              <nav className="space-y-1">
                {visibleItems.map((item) => {
                  const Icon = item.icon;
                  return (
                    <NavLink
                      key={item.to}
                      to={item.to}
                      onClick={() => setMobileOpen(false)}
                      className={({ isActive }) =>
                        cn(
                          'flex items-center gap-2.5 px-3 py-2.5 rounded-xl text-xs font-medium transition-all',
                          isActive
                            ? 'bg-olive-600 text-cream-50 shadow-sm font-semibold'
                            : 'text-olive-800 hover:bg-olive-600/10',
                        )
                      }
                    >
                      <Icon className="h-4 w-4 shrink-0" />
                      <span>{item.label}</span>
                    </NavLink>
                  );
                })}
              </nav>
            </GlassCard>
          </div>
        )}

        {/* Main Routed Page Content */}
        <main className="flex-1 min-w-0">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
