# Mise à jour React Semaine 1 — Alignement sur le design « Olive Field » (`prototype react`)

> **Objectif** : Mettre à jour uniquement la partie **React de la Semaine 1 (Membre C : `C1` → `C6`)** de [SEMAINE_1_GUIDE.md](file:///c:/Users/GMI/Desktop/My_Projects/jeee/SEMAINE_1_GUIDE.md) pour adopter dès le **Jour 1** le design **« Olive Field » Glassmorphism** de [prototype react](file:///c:/Users/GMI/Desktop/My_Projects/jeee/prototype%20react).
> **Résultat** : Aucun écran S1 à réécrire en Semaine 2 — les pages `C7` → `C12` de [SEMAINE_2_GUIDE.md](file:///c:/Users/GMI/Desktop/My_Projects/jeee/SEMAINE_2_GUIDE.md) s'emboîtent directement dans le même shell (`Sidebar` + `TopBar` + `GlassCard` + `StatusBadge`).

---

## 1. Résumé : ce qui change vs ce qui NE change PAS en Semaine 1

| Tâche S1 | Fichier(s) | Faut-il modifier ? | Pourquoi |
|---|---|---|---|
| **C1** | `package.json`, `vite.config.ts`, `index.html`, `src/index.css` | **OUI** | Remplacer le CSS basique par **Tailwind CSS v4** (`@tailwindcss/vite`), `lucide-react`, `clsx`, `tailwind-merge`, polices `Inter`/`Manrope` et le thème **« Olive Field » Glassmorphism**. |
| **C1 (partagé)** | `src/lib/utils.ts`, `src/components/GlassCard.tsx`, `src/components/StatusBadge.tsx`, `src/features/_shared/statusStyles.ts` | **AJOUTER dès S1** | Composants UI uniques issus de `prototype react` (source unique des couleurs de statuts et surfaces verre). |
| **C2** | `src/types/contract.ts`, `src/api/http.ts`, `src/mocks/handlers.ts`, `src/mocks/browser.ts` | **NON (inchangé)** | Déjà 100 % conformes au contrat gelé `contracts-v1` (aucun impact visuel). |
| **C3** | `src/features/auth/AuthContext.tsx` | **NON (inchangé)** | Logique pure d'authentification JWT. |
| **C3** | `src/components/ProtectedRoute.tsx`, `src/app/Layout.tsx`, `src/features/auth/LoginPage.tsx` | **OUI** | Passer à la `Sidebar` + `TopBar` glassmorphism (7 rôles + mobile 360 px) et à la carte de connexion `GlassCard`. |
| **C4** | `src/features/orchards/OrchardsPage.tsx` | **OUI** | Formulaire + liste en `GlassCard` (grille 12 colonnes), icônes `lucide-react`, bouton « Ma position » stylé, `StatusBadge` (`maturityStyles`). |
| **C5** | `src/features/harvest/HarvestRequestsPage.tsx` | **OUI** | Formulaire Zod + liste « Mes demandes » en `GlassCard`, `StatusBadge` (`harvestStatusStyles`), pastille `URGENT` ocre, erreurs `fieldErrors` stylées. |
| **C6** | `src/features/map/OrchardsMapPage.tsx` | **OUI** | Envelopper la carte Leaflet dans `GlassCard` et utiliser **`maturityStyles`** de `statusStyles.ts` (couleurs exactes du prototype) ; reste sur **MSW en S1**. |

---

## 2. Mise à jour C1 — Installation, Vite, polices et thème « Olive Field »

### 2.1 Installer les dépendances supplémentaires (en plus de celles de `C1`)

```powershell
cd frontend
npm install lucide-react clsx tailwind-merge
npm install -D tailwindcss @tailwindcss/vite
```

### 2.2 `frontend/vite.config.ts`

```ts
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
});
```

### 2.3 Polices `Inter` et `Manrope` dans `frontend/index.html`

Ajouter dans `<head>` de `frontend/index.html` :

```html
<link rel="preconnect" href="https://fonts.googleapis.com" />
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin />
<link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=Manrope:wght@600;700;800&display=swap" rel="stylesheet" />
```

### 2.4 `frontend/src/index.css` (remplace totalement le CSS de `SEMAINE_1_GUIDE.md`)

```css
@import "tailwindcss";

@theme inline {
  --font-sans: "Inter", system-ui, -apple-system, sans-serif;
  --font-display: "Manrope", system-ui, sans-serif;
  --color-olive-50: #f3f6e8;
  --color-olive-100: #e8eed7;
  --color-olive-200: #cdd9a8;
  --color-olive-300: #a8be72;
  --color-olive-400: #8fae5c;
  --color-olive-500: #6b8e3f;
  --color-olive-600: #4a6b2a;
  --color-olive-700: #3a5520;
  --color-olive-800: #2d3a1a;
  --color-olive-900: #1f2418;
  --color-ochre-50: #fdf8ec;
  --color-ochre-100: #f7eccf;
  --color-ochre-200: #eed79b;
  --color-ochre-300: #e0b856;
  --color-ochre-400: #d4a04c;
  --color-ochre-500: #c9962f;
  --color-ochre-600: #b07f1f;
  --color-ochre-700: #8a621a;
  --color-ochre-800: #5f4419;
  --color-cream-50: #faf6ee;
  --color-cream-100: #f2ede0;
  --color-cream-200: #e5dfcf;
  --color-cream-300: #c9c0a8;
}

@layer base {
  html {
    background:
      radial-gradient(ellipse 900px 700px at 10% 8%, rgba(168, 190, 114, 0.7), transparent 55%),
      radial-gradient(ellipse 800px 600px at 92% 5%, rgba(224, 184, 86, 0.65), transparent 55%),
      radial-gradient(ellipse 1100px 900px at 85% 60%, rgba(107, 142, 63, 0.55), transparent 60%),
      radial-gradient(ellipse 800px 700px at 20% 70%, rgba(201, 150, 47, 0.45), transparent 60%),
      radial-gradient(ellipse 900px 700px at 50% 95%, rgba(74, 107, 42, 0.3), transparent 65%),
      linear-gradient(180deg, #faf6ee 0%, #f5eedb 50%, #f3edd9 100%);
    background-repeat: no-repeat;
    min-height: 100%;
    color: #1f2418;
    font-family: var(--font-sans);
  }
  body {
    background: transparent;
    color: #1f2418;
    font-feature-settings: "ss01", "cv11";
    min-height: 100%;
    margin: 0;
  }
}

@layer utilities {
  .glass {
    background-color: rgba(255, 255, 255, 0.42);
    backdrop-filter: blur(22px) saturate(160%);
    -webkit-backdrop-filter: blur(22px) saturate(160%);
    border: 1px solid rgba(255, 255, 255, 0.55);
    box-shadow: 0 6px 20px rgba(31, 36, 24, 0.08), inset 0 1px 0 rgba(255, 255, 255, 0.7);
  }
  .glass-strong {
    background-color: rgba(255, 255, 255, 0.62);
    backdrop-filter: blur(28px) saturate(170%);
    -webkit-backdrop-filter: blur(28px) saturate(170%);
    border: 1px solid rgba(255, 255, 255, 0.65);
    box-shadow: 0 10px 32px rgba(31, 36, 24, 0.10), inset 0 1px 0 rgba(255, 255, 255, 0.75);
  }
  .glass-subtle {
    background-color: rgba(255, 255, 255, 0.28);
    backdrop-filter: blur(16px) saturate(150%);
    -webkit-backdrop-filter: blur(16px) saturate(150%);
    border: 1px solid rgba(255, 255, 255, 0.4);
  }
  .glass-nav {
    background-color: rgba(255, 255, 255, 0.38);
    backdrop-filter: blur(24px) saturate(160%);
    -webkit-backdrop-filter: blur(24px) saturate(160%);
    border: 1px solid rgba(255, 255, 255, 0.5);
  }
  .glass-hover { transition: background-color 200ms ease, box-shadow 200ms ease, transform 200ms ease; }
  .glass-hover:hover {
    background-color: rgba(255, 255, 255, 0.68);
    box-shadow: 0 12px 32px rgba(31, 36, 24, 0.10), inset 0 1px 0 rgba(255, 255, 255, 0.75);
  }
  .tabnum { font-variant-numeric: tabular-nums; }
}
```

### 2.5 Les 4 fichiers UI partagés à créer dès C1

1. `frontend/src/lib/utils.ts` :
   ```ts
   import { clsx, type ClassValue } from 'clsx';
   import { twMerge } from 'tailwind-merge';
   export function cn(...inputs: ClassValue[]) { return twMerge(clsx(inputs)); }
   ```
2. `frontend/src/components/GlassCard.tsx`, `frontend/src/components/StatusBadge.tsx` et `frontend/src/features/_shared/statusStyles.ts` : **copier exactement ceux du [Prérequis Design System de `SEMAINE_2_GUIDE.md`](file:///c:/Users/GMI/Desktop/My_Projects/jeee/SEMAINE_2_GUIDE.md#L2983-L3075)** (ils couvrent déjà tous les statuts S1, S2 et S3).

---

## 3. Mise à jour C3 — `ProtectedRoute`, `Layout` (`Sidebar` + `TopBar`) & `LoginPage`

### 3.1 `frontend/src/components/ProtectedRoute.tsx`

```tsx
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
```

### 3.2 `frontend/src/app/Layout.tsx`

Utiliser directement la version **`Sidebar` + `TopBar` glassmorphism** de [SEMAINE_2_GUIDE.md (Prérequis Design System §4)](file:///c:/Users/GMI/Desktop/My_Projects/jeee/SEMAINE_2_GUIDE.md#L3079-L3225) dès la Semaine 1 (elle gère les **7 rôles** — visiteur inclus — et la barre mobile à 360 px).

### 3.3 `frontend/src/features/auth/LoginPage.tsx`

```tsx
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { KeyRound, LogIn } from 'lucide-react';
import { ApiClientError } from '../../api/http';
import { GlassCard } from '../../components/GlassCard';
import { SEEDED_ACCOUNTS } from '../../mocks/handlers';
import { useAuth } from './AuthContext';

export function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState('farmer@example.test');
  const [password, setPassword] = useState('Secret#123');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      const user = await login({ email, password });
      navigate(user.role === 'FARMER' ? '/farmer/orchards' : '/map');
    } catch (err) {
      setError(err instanceof ApiClientError ? err.apiError.message : 'Erreur de connexion');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="max-w-md mx-auto my-8">
      <GlassCard variant="strong" className="p-6 space-y-5">
        <div className="flex items-center gap-3">
          <div className="h-11 w-11 rounded-xl bg-olive-600 text-cream-50 grid place-items-center font-display font-bold text-base">
            OC
          </div>
          <div>
            <h1 className="font-display font-bold text-xl text-olive-900">Connexion OliveConnect</h1>
            <p className="text-xs text-olive-700/80">Coopérative oléicole de Sfax</p>
          </div>
        </div>

        {error && (
          <div className="px-3.5 py-2.5 rounded-lg bg-[#fee2e2] text-[#b33a3a] text-sm font-medium">{error}</div>
        )}

        <div className="p-3 rounded-xl glass-subtle">
          <label htmlFor="quick-role" className="flex items-center gap-1.5 text-xs font-medium text-olive-800 mb-1.5">
            <KeyRound className="h-3.5 w-3.5" /> Compte de démo (seed B6 / MSW)
          </label>
          <select
            id="quick-role"
            value={email}
            onChange={(e) => {
              setEmail(e.target.value);
              setPassword('Secret#123');
            }}
            className="w-full h-9 px-3 rounded-lg glass-strong text-xs text-olive-900 outline-none"
          >
            {SEEDED_ACCOUNTS.map((acc) => (
              <option key={acc.email} value={acc.email}>
                {acc.label}
              </option>
            ))}
          </select>
        </div>

        <form onSubmit={handleSubmit} className="space-y-3.5">
          <div>
            <label htmlFor="email" className="block text-xs font-medium text-olive-800 mb-1">Email</label>
            <input
              id="email"
              type="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="w-full h-10 px-3 rounded-lg glass-strong text-sm text-olive-900 outline-none"
            />
          </div>
          <div>
            <label htmlFor="password" className="block text-xs font-medium text-olive-800 mb-1">Mot de passe</label>
            <input
              id="password"
              type="password"
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="w-full h-10 px-3 rounded-lg glass-strong text-sm text-olive-900 outline-none"
            />
          </div>
          <button
            type="submit"
            disabled={submitting}
            className="w-full h-10 inline-flex items-center justify-center gap-2 rounded-lg bg-olive-600 hover:bg-olive-700 text-cream-50 text-sm font-semibold transition-colors disabled:opacity-60"
          >
            <LogIn className="h-4 w-4" />
            {submitting ? 'Connexion…' : 'Se connecter'}
          </button>
        </form>
      </GlassCard>
    </div>
  );
}
```

---

## 4. Mise à jour C4 — `frontend/src/features/orchards/OrchardsPage.tsx`

```tsx
import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Crosshair, MapPin, Plus, TreePine } from 'lucide-react';
import { ApiClientError, apiFetch } from '../../api/http';
import { GlassCard } from '../../components/GlassCard';
import { StatusBadge } from '../../components/StatusBadge';
import { maturityStyles } from '../_shared/statusStyles';
import type { OrchardCreateRequest, OrchardResponse, PageResponse } from '../../types/contract';

const orchardSchema = z.object({
  name: z.string().trim().min(1, 'Obligatoire').max(100, '100 caractères maximum'),
  areaHectares: z.coerce.number().positive('Doit être > 0'),
  oliveVariety: z.string().trim().min(1, 'Obligatoire'),
  longitude: z.coerce.number().min(-180, 'Min -180').max(180, 'Max 180'),
  latitude: z.coerce.number().min(-90, 'Min -90').max(90, 'Max 90'),
  publicVisibility: z.boolean(),
});

type OrchardFormValues = z.infer<typeof orchardSchema>;

export function OrchardsPage() {
  const qc = useQueryClient();
  const [serverError, setServerError] = useState<string | null>(null);
  const [geoInfo, setGeoInfo] = useState<string | null>(null);

  const { data, isLoading, isError, error } = useQuery({
    queryKey: ['orchards', 'mine'],
    queryFn: () => apiFetch<PageResponse<OrchardResponse>>('/orchards/mine'),
  });

  const { register, handleSubmit, setValue, setError, reset, formState: { errors } } = useForm<OrchardFormValues>({
    resolver: zodResolver(orchardSchema),
    defaultValues: {
      name: '', areaHectares: 4.2, oliveVariety: 'Chemlali',
      longitude: 10.76, latitude: 34.73, publicVisibility: true,
    },
  });

  const createMutation = useMutation({
    mutationFn: (payload: OrchardCreateRequest) =>
      apiFetch<OrchardResponse>('/orchards', { method: 'POST', body: JSON.stringify(payload) }),
    onSuccess: () => {
      setServerError(null);
      reset();
      qc.invalidateQueries({ queryKey: ['orchards'] });
    },
    onError: (err) => {
      if (err instanceof ApiClientError) {
        setServerError(err.apiError.message);
        Object.entries(err.apiError.fieldErrors).forEach(([field, msg]) => {
          if (field === 'location.coordinates') setError('longitude', { message: msg });
          else setError(field as keyof OrchardFormValues, { message: msg });
        });
      }
    },
  });

  const handleMyPosition = () => {
    if (!navigator.geolocation) {
      setGeoInfo('Géolocalisation non disponible sur ce navigateur.');
      return;
    }
    setGeoInfo('Acquisition GPS en cours…');
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        setValue('longitude', Number(pos.coords.longitude.toFixed(5)));
        setValue('latitude', Number(pos.coords.latitude.toFixed(5)));
        setGeoInfo('Position GPS renseignée ([longitude, latitude]).');
      },
      () => setGeoInfo('Accès GPS refusé — coordonnées de Sfax conservées.'),
    );
  };

  const onSubmit = (v: OrchardFormValues) => {
    createMutation.mutate({
      name: v.name,
      areaHectares: v.areaHectares,
      oliveVariety: v.oliveVariety,
      location: { type: 'Point', coordinates: [v.longitude, v.latitude] }, // GeoJSON [lon, lat]
      publicVisibility: v.publicVisibility,
    });
  };

  return (
    <div className="grid grid-cols-1 lg:grid-cols-12 gap-5">
      <GlassCard className="p-5 lg:col-span-5 self-start space-y-3.5">
        <div className="flex items-center gap-2.5">
          <div className="h-9 w-9 rounded-lg bg-olive-600/15 text-olive-700 grid place-items-center">
            <Plus className="h-4 w-4" />
          </div>
          <div>
            <h1 className="font-display font-bold text-lg text-olive-900">Nouveau verger</h1>
            <p className="text-xs text-olive-700/75">Formulaire court avec géolocalisation</p>
          </div>
        </div>

        {serverError && <div className="px-3 py-2 rounded-lg bg-[#fee2e2] text-[#b33a3a] text-xs font-medium">{serverError}</div>}

        <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-3">
          <div>
            <label htmlFor="name" className="block text-xs font-medium text-olive-800 mb-1">Nom du verger</label>
            <input id="name" {...register('name')} placeholder="Ex. North Orchard" className="w-full h-9 px-3 rounded-lg glass-strong text-sm text-olive-900 outline-none" />
            {errors.name && <p className="text-xs text-[#b33a3a] mt-1">{errors.name.message}</p>}
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label htmlFor="areaHectares" className="block text-xs font-medium text-olive-800 mb-1">Surface (ha)</label>
              <input id="areaHectares" type="number" step="0.1" {...register('areaHectares')} className="w-full h-9 px-3 rounded-lg glass-strong text-sm text-olive-900 tabnum outline-none" />
              {errors.areaHectares && <p className="text-xs text-[#b33a3a] mt-1">{errors.areaHectares.message}</p>}
            </div>
            <div>
              <label htmlFor="oliveVariety" className="block text-xs font-medium text-olive-800 mb-1">Variété</label>
              <select id="oliveVariety" {...register('oliveVariety')} className="w-full h-9 px-3 rounded-lg glass-strong text-sm text-olive-900 outline-none">
                <option value="Chemlali">Chemlali</option>
                <option value="Chetoui">Chetoui</option>
                <option value="Oueslati">Oueslati</option>
                <option value="Zalmati">Zalmati</option>
              </select>
            </div>
          </div>

          <div>
            <label className="block text-xs font-medium text-olive-800 mb-1">Position GeoJSON [longitude, latitude]</label>
            <div className="flex flex-wrap gap-2">
              <input type="number" step="0.0001" aria-label="Longitude" {...register('longitude')} className="flex-1 min-w-24 h-9 px-2.5 rounded-lg glass-strong text-sm text-olive-900 tabnum outline-none" />
              <input type="number" step="0.0001" aria-label="Latitude" {...register('latitude')} className="flex-1 min-w-24 h-9 px-2.5 rounded-lg glass-strong text-sm text-olive-900 tabnum outline-none" />
              <button
                type="button"
                onClick={handleMyPosition}
                className="inline-flex items-center gap-1.5 px-3 h-9 rounded-lg glass-subtle hover:bg-olive-600/15 text-olive-900 text-xs font-medium"
              >
                <Crosshair className="h-3.5 w-3.5" /> Ma position
              </button>
            </div>
            {geoInfo && <p className="text-[11px] text-olive-700/80 mt-1">{geoInfo}</p>}
            {errors.longitude && <p className="text-xs text-[#b33a3a] mt-1">{errors.longitude.message}</p>}
            {errors.latitude && <p className="text-xs text-[#b33a3a] mt-1">{errors.latitude.message}</p>}
          </div>

          <label className="flex items-center gap-2 text-xs text-olive-800 cursor-pointer pt-1">
            <input type="checkbox" {...register('publicVisibility')} /> Visible sur la carte publique
          </label>

          <button
            type="submit"
            disabled={createMutation.isPending}
            className="w-full h-10 rounded-lg bg-olive-600 hover:bg-olive-700 text-cream-50 text-sm font-semibold transition-colors disabled:opacity-60"
          >
            {createMutation.isPending ? 'Enregistrement…' : 'Créer le verger'}
          </button>
        </form>
      </GlassCard>

      <GlassCard className="p-5 lg:col-span-7">
        <div className="flex items-center gap-2.5 mb-4">
          <div className="h-9 w-9 rounded-lg bg-olive-600/15 text-olive-700 grid place-items-center">
            <TreePine className="h-4 w-4" />
          </div>
          <div>
            <h2 className="font-display font-bold text-lg text-olive-900">Mes vergers ({data?.totalElements ?? 0})</h2>
            <p className="text-xs text-olive-700/75">Synchronisés avec MongoDB (GeoJSON 2dsphere)</p>
          </div>
        </div>

        {isLoading && <p className="text-sm text-olive-700/70">Chargement de vos vergers…</p>}
        {isError && <div className="px-3 py-2 rounded-lg bg-[#fee2e2] text-[#b33a3a] text-xs">{(error as Error).message}</div>}
        {data && data.content.length === 0 && <p className="text-sm text-olive-700/70">Aucun verger enregistré.</p>}

        <div className="space-y-2.5">
          {data?.content.map((o) => {
            const mat = maturityStyles[o.maturityStatus];
            return (
              <GlassCard key={o.id} variant="subtle" hover className="p-3.5">
                <div className="flex items-center justify-between gap-2">
                  <strong className="font-display text-sm text-olive-900">{o.name}</strong>
                  <StatusBadge style={{ label: mat.label, fg: '#ffffff', bg: mat.color }} size="sm" />
                </div>
                <div className="flex flex-wrap items-center gap-3 text-xs text-olive-700/80 mt-1 tabnum">
                  <span>{o.areaHectares} ha · {o.oliveVariety}</span>
                  <span className="inline-flex items-center gap-1">
                    <MapPin className="h-3 w-3" /> [{o.location.coordinates[0]}, {o.location.coordinates[1]}]
                  </span>
                </div>
              </GlassCard>
            );
          })}
        </div>
      </GlassCard>
    </div>
  );
}
```

---

## 5. Mise à jour C5 — `frontend/src/features/harvest/HarvestRequestsPage.tsx`

```tsx
import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Inbox, Send } from 'lucide-react';
import { ApiClientError, apiFetch } from '../../api/http';
import { GlassCard } from '../../components/GlassCard';
import { StatusBadge } from '../../components/StatusBadge';
import { harvestStatusStyles } from '../_shared/statusStyles';
import type {
  HarvestRequestCreateRequest, HarvestRequestResponse, OrchardResponse, PageResponse,
} from '../../types/contract';

const harvestSchema = z
  .object({
    orchardId: z.string().min(1, 'Veuillez choisir un verger'),
    type: z.enum(['PLANNED', 'URGENT']),
    preferredDate: z.string().min(10, 'Date obligatoire (YYYY-MM-DD)'),
    estimatedQuantityKg: z.coerce.number().positive('Doit être > 0'),
    urgencyReason: z.string().max(500).optional(),
  })
  .superRefine((val, ctx) => {
    if (val.type === 'URGENT' && (!val.urgencyReason || !val.urgencyReason.trim())) {
      ctx.addIssue({ code: z.ZodIssueCode.custom, path: ['urgencyReason'], message: 'Obligatoire pour une demande urgente' });
    }
  });

type HarvestFormValues = z.infer<typeof harvestSchema>;

export function HarvestRequestsPage() {
  const qc = useQueryClient();
  const [serverError, setServerError] = useState<string | null>(null);

  const orchardsQuery = useQuery({
    queryKey: ['orchards', 'mine'],
    queryFn: () => apiFetch<PageResponse<OrchardResponse>>('/orchards/mine'),
  });
  const requestsQuery = useQuery({
    queryKey: ['harvest-requests', 'mine'],
    queryFn: () => apiFetch<PageResponse<HarvestRequestResponse>>('/harvest-requests/mine'),
  });

  const { register, handleSubmit, watch, setError, reset, formState: { errors } } = useForm<HarvestFormValues>({
    resolver: zodResolver(harvestSchema),
    defaultValues: {
      orchardId: '',
      type: 'PLANNED',
      preferredDate: new Date(Date.now() + 3 * 86400000).toISOString().slice(0, 10),
      estimatedQuantityKg: 1500,
      urgencyReason: '',
    },
  });

  const selectedType = watch('type');

  const createMutation = useMutation({
    mutationFn: (payload: HarvestRequestCreateRequest) =>
      apiFetch<HarvestRequestResponse>('/harvest-requests', { method: 'POST', body: JSON.stringify(payload) }),
    onSuccess: () => {
      setServerError(null);
      reset();
      qc.invalidateQueries({ queryKey: ['harvest-requests', 'mine'] });
    },
    onError: (err) => {
      if (err instanceof ApiClientError) {
        setServerError(err.apiError.message);
        Object.entries(err.apiError.fieldErrors).forEach(([field, msg]) => {
          setError(field as keyof HarvestFormValues, { type: 'server', message: msg });
        });
      }
    },
  });

  const onSubmit = (v: HarvestFormValues) => {
    createMutation.mutate({
      orchardId: v.orchardId,
      type: v.type,
      preferredDate: v.preferredDate,
      estimatedQuantityKg: v.estimatedQuantityKg,
      urgencyReason: v.type === 'URGENT' ? (v.urgencyReason?.trim() ?? null) : null,
    });
  };

  return (
    <div className="grid grid-cols-1 lg:grid-cols-12 gap-5">
      <GlassCard className="p-5 lg:col-span-5 self-start space-y-3.5">
        <div className="flex items-center gap-2.5">
          <div className="h-9 w-9 rounded-lg bg-olive-600/15 text-olive-700 grid place-items-center">
            <Send className="h-4 w-4" />
          </div>
          <div>
            <h1 className="font-display font-bold text-lg text-olive-900">Déposer une demande</h1>
            <p className="text-xs text-olive-700/75">Validation client (Zod) + validation serveur</p>
          </div>
        </div>

        {serverError && <div className="px-3 py-2 rounded-lg bg-[#fee2e2] text-[#b33a3a] text-xs font-medium">{serverError}</div>}

        <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-3">
          <div>
            <label htmlFor="orchardId" className="block text-xs font-medium text-olive-800 mb-1">Verger</label>
            <select id="orchardId" {...register('orchardId')} className="w-full h-9 px-3 rounded-lg glass-strong text-sm text-olive-900 outline-none">
              <option value="">— Choisir un verger —</option>
              {orchardsQuery.data?.content.map((o) => (
                <option key={o.id} value={o.id}>{o.name} ({o.areaHectares} ha)</option>
              ))}
            </select>
            {errors.orchardId && <p className="text-xs text-[#b33a3a] mt-1">{errors.orchardId.message}</p>}
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label htmlFor="type" className="block text-xs font-medium text-olive-800 mb-1">Type</label>
              <select id="type" {...register('type')} className="w-full h-9 px-3 rounded-lg glass-strong text-sm text-olive-900 outline-none">
                <option value="PLANNED">Planifiée</option>
                <option value="URGENT">Urgente</option>
              </select>
            </div>
            <div>
              <label htmlFor="estimatedQuantityKg" className="block text-xs font-medium text-olive-800 mb-1">Quantité estimée (kg)</label>
              <input id="estimatedQuantityKg" type="number" step="50" {...register('estimatedQuantityKg')} className="w-full h-9 px-3 rounded-lg glass-strong text-sm text-olive-900 tabnum outline-none" />
              {errors.estimatedQuantityKg && <p className="text-xs text-[#b33a3a] mt-1">{errors.estimatedQuantityKg.message}</p>}
            </div>
          </div>

          {selectedType === 'URGENT' && (
            <div>
              <label htmlFor="urgencyReason" className="block text-xs font-medium text-olive-800 mb-1">Motif d&apos;urgence (obligatoire)</label>
              <input id="urgencyReason" {...register('urgencyReason')} placeholder="Ex. Maturité optimale / Pluie annoncée" className="w-full h-9 px-3 rounded-lg glass-strong text-sm text-olive-900 outline-none" />
              {errors.urgencyReason && <p className="text-xs text-[#b33a3a] mt-1">{errors.urgencyReason.message}</p>}
            </div>
          )}

          <div>
            <label htmlFor="preferredDate" className="block text-xs font-medium text-olive-800 mb-1">Date souhaitée</label>
            <input id="preferredDate" type="date" {...register('preferredDate')} className="w-full h-9 px-3 rounded-lg glass-strong text-sm text-olive-900 outline-none" />
            {errors.preferredDate && <p className="text-xs text-[#b33a3a] mt-1">{errors.preferredDate.message}</p>}
          </div>

          <button
            type="submit"
            disabled={createMutation.isPending}
            className="w-full h-10 rounded-lg bg-olive-600 hover:bg-olive-700 text-cream-50 text-sm font-semibold transition-colors disabled:opacity-60"
          >
            {createMutation.isPending ? 'Envoi…' : 'Soumettre la demande'}
          </button>
        </form>
      </GlassCard>

      <GlassCard className="p-5 lg:col-span-7">
        <div className="flex items-center gap-2.5 mb-4">
          <div className="h-9 w-9 rounded-lg bg-olive-600/15 text-olive-700 grid place-items-center">
            <Inbox className="h-4 w-4" />
          </div>
          <h2 className="font-display font-bold text-lg text-olive-900">
            Mes demandes ({requestsQuery.data?.totalElements ?? 0})
          </h2>
        </div>

        {requestsQuery.isLoading && <p className="text-sm text-olive-700/70">Chargement des demandes…</p>}
        {requestsQuery.isError && <div className="px-3 py-2 rounded-lg bg-[#fee2e2] text-[#b33a3a] text-xs">{(requestsQuery.error as Error).message}</div>}
        {requestsQuery.data?.content.length === 0 && <p className="text-sm text-olive-700/70">Aucune demande déposée.</p>}

        <div className="space-y-2.5">
          {requestsQuery.data?.content.map((r) => (
            <GlassCard key={r.id} variant="subtle" hover className="p-3.5">
              <div className="flex items-center justify-between gap-2">
                <div className="flex items-center gap-2 flex-wrap">
                  <strong className="font-display text-sm text-olive-900">{r.orchardName}</strong>
                  {r.type === 'URGENT' && (
                    <span className="px-2 py-0.5 rounded-full text-[11px] font-semibold bg-ochre-100 text-ochre-700">URGENT</span>
                  )}
                </div>
                <StatusBadge style={harvestStatusStyles[r.status]} size="sm" />
              </div>
              <p className="text-xs text-olive-700/80 mt-1 tabnum">
                <strong>{r.estimatedQuantityKg} kg</strong> · souhaitée le <strong>{r.preferredDate}</strong>
                {r.actualQuantityKg != null && <> · récolté : <strong className="text-[#1b5e20]">{r.actualQuantityKg} kg</strong></>}
              </p>
              {r.urgencyReason && <p className="text-xs text-olive-700/75 mt-0.5">Motif : {r.urgencyReason}</p>}
            </GlassCard>
          ))}
        </div>
      </GlassCard>
    </div>
  );
}
```

---

## 6. Mise à jour C6 — `frontend/src/features/map/OrchardsMapPage.tsx` (couleurs `maturityStyles` + `GlassCard`, sur MSW en S1)

```tsx
import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { CircleMarker, MapContainer, Popup, TileLayer, useMapEvents } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import { Map as MapIcon } from 'lucide-react';
import { apiFetch } from '../../api/http';
import { GlassCard } from '../../components/GlassCard';
import { maturityStyles } from '../_shared/statusStyles';
import type { MaturityStatus, OrchardMapFeatureCollection } from '../../types/contract';

function BboxWatcher({ onChange }: { onChange: (bbox: string) => void }) {
  useMapEvents({
    moveend: (e) => {
      const b = e.target.getBounds();
      onChange(`${b.getWest().toFixed(4)},${b.getSouth().toFixed(4)},${b.getEast().toFixed(4)},${b.getNorth().toFixed(4)}`);
    },
  });
  return null;
}

export function OrchardsMapPage() {
  const [bbox, setBbox] = useState('10.50,34.50,11.05,35.00');

  const { data, isLoading, isError } = useQuery({
    queryKey: ['orchards-map', bbox],
    queryFn: () => apiFetch<OrchardMapFeatureCollection>(`/orchards/map?bbox=${encodeURIComponent(bbox)}`),
  });

  return (
    <GlassCard className="p-5 space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-2.5">
          <div className="h-9 w-9 rounded-lg bg-olive-600/15 text-olive-700 grid place-items-center">
            <MapIcon className="h-4 w-4" />
          </div>
          <div>
            <h1 className="font-display font-bold text-xl text-olive-900">Carte des vergers — Sfax</h1>
            <p className="text-xs text-olive-700/75 tabnum">Zone visible : {bbox}</p>
          </div>
        </div>

        {/* Légende alignée sur prototype react/statusStyles.ts */}
        <div className="flex flex-wrap gap-1.5">
          {(Object.keys(maturityStyles) as MaturityStatus[]).map((m) => (
            <span
              key={m}
              className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium glass-subtle text-olive-900"
            >
              <span className="h-2 w-2 rounded-full" style={{ backgroundColor: maturityStyles[m].color }} />
              {maturityStyles[m].label}
            </span>
          ))}
        </div>
      </div>

      {isLoading && <p className="text-xs text-olive-700/70">Chargement de la zone…</p>}
      {isError && (
        <div className="px-3.5 py-2.5 rounded-lg bg-ochre-100 text-ochre-800 text-xs">
          En Semaine 1, l&apos;endpoint réel <code>GET /api/v1/orchards/map</code> (tâche B7, S2) n&apos;est pas encore actif — utilisez <code>VITE_USE_MOCKS=true</code> pour tester la carte sur MSW.
        </div>
      )}
      {data?.truncated && (
        <div className="px-3.5 py-2.5 rounded-lg bg-[#fee2e2] text-[#b33a3a] text-xs font-medium">
          Résultat tronqué à 500 vergers : zoomez sur la carte.
        </div>
      )}

      <div className="rounded-xl overflow-hidden border border-white/60 shadow-sm">
        {/* Leaflet attend [lat, lon] alors que le contrat GeoJSON renvoie [lon, lat] */}
        <MapContainer center={[34.73, 10.76]} zoom={11} style={{ height: 460, width: '100%' }}>
          <TileLayer
            attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          />
          <BboxWatcher onChange={setBbox} />
          {data?.features.map((f) => {
            const [lon, lat] = f.geometry.coordinates;
            const mat = maturityStyles[f.properties.maturityStatus];
            return (
              <CircleMarker
                key={f.properties.orchardId}
                center={[lat, lon]}
                radius={9}
                pathOptions={{ color: mat.color, fillColor: mat.color, fillOpacity: 0.78 }}
              >
                <Popup>
                  <strong>{f.properties.name ?? 'Verger public'}</strong>
                  <br />Variété : {f.properties.oliveVariety}
                  <br />Maturité : {mat.label}
                  {f.properties.areaHectares != null && (
                    <><br />Surface : {f.properties.areaHectares} ha</>
                  )}
                </Popup>
              </CircleMarker>
            );
          })}
        </MapContainer>
      </div>
    </GlassCard>
  );
}
```
