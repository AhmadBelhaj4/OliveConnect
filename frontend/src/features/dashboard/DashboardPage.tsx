import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  Bar, BarChart, Cell, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis,
} from 'recharts';
import { Factory, Route, TriangleAlert, Wheat } from 'lucide-react';
import { apiFetch } from '../../api/http';
import { GlassCard } from '../../components/GlassCard';
import { harvestStatusStyles } from '../_shared/statusStyles';
import type {
  DashboardBinFillRatesResponse,
  DashboardCollectionDelaysResponse,
  DashboardSummaryResponse,
  DashboardYieldByOrchardResponse,
  HarvestStatus,
} from '../../types/contract';

export function DashboardPage() {
  const [from, setFrom] = useState('2026-11-01');
  const [to, setTo] = useState('2026-11-30');
  const qs = `?from=${from}&to=${to}`;

  const summaryQ = useQuery({
    queryKey: ['dashboard', 'summary', from, to],
    queryFn: () => apiFetch<DashboardSummaryResponse>(`/dashboard/summary${qs}`),
  });
  const yieldQ = useQuery({
    queryKey: ['dashboard', 'yield', from, to],
    queryFn: () => apiFetch<DashboardYieldByOrchardResponse>(`/dashboard/yield-by-orchard${qs}`),
  });
  const delaysQ = useQuery({
    queryKey: ['dashboard', 'delays', from, to],
    queryFn: () => apiFetch<DashboardCollectionDelaysResponse>(`/dashboard/collection-delays${qs}`),
  });
  const binsQ = useQuery({
    queryKey: ['dashboard', 'bins', from, to],
    queryFn: () => apiFetch<DashboardBinFillRatesResponse>(`/dashboard/bin-fill-rates${qs}`),
  });

  const s = summaryQ.data;
  const statusPie = s && s.requestsByStatus
    ? (Object.entries(s.requestsByStatus) as [HarvestStatus, number][])
        .filter(([, count]) => count > 0)
        .map(([status, value]) => ({
          name: harvestStatusStyles[status]?.label ?? status,
          value,
          color: harvestStatusStyles[status]?.fg ?? '#4a6b2a',
        }))
    : [];

  const yieldRows = [...(yieldQ.data?.items ?? [])].slice(0, 7).reverse();
  const maxYield = Math.max(1, ...yieldRows.map((r) => r.totalHarvestedKg));

  return (
    <div className="space-y-5">
      {/* Filtre de dates style TopBar */}
      <GlassCard variant="subtle" className="p-4 flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="font-display font-bold text-xl sm:text-2xl text-olive-900">Tableau de bord des performances</h1>
          <p className="text-xs text-olive-700/75">Agrégations MongoDB + cache Redis (TTL 90 s)</p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <input type="date" value={from} onChange={(e) => setFrom(e.target.value)} className="h-9 px-3 rounded-lg glass-strong text-xs text-olive-900" />
          <span className="text-xs text-olive-700">→</span>
          <input type="date" value={to} onChange={(e) => setTo(e.target.value)} className="h-9 px-3 rounded-lg glass-strong text-xs text-olive-900" />
        </div>
      </GlassCard>

      {/* 4 cartes KPI (style KpiGrid.tsx de prototype react) */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        {[
          { label: 'Total récolté', value: s ? `${(s.totalHarvestedKg / 1000).toFixed(1)} t` : '—', icon: Wheat, bg: 'rgba(74,107,42,0.15)', color: '#4a6b2a' },
          { label: 'Total reçu pressoir', value: s ? `${(s.totalReceivedKg / 1000).toFixed(1)} t` : '—', icon: Factory, bg: 'rgba(74,107,42,0.15)', color: '#4a6b2a' },
          { label: 'Tournées terminées', value: s ? String(s.completedTours) : '—', icon: Route, bg: 'rgba(201,150,47,0.15)', color: '#b07f1f' },
          { label: 'Alertes ouvertes', value: s ? String(s.openAlerts) : '—', icon: TriangleAlert, bg: 'rgba(179,58,58,0.15)', color: '#b33a3a' },
        ].map((k) => {
          const Icon = k.icon;
          return (
            <GlassCard key={k.label} hover className="p-5 flex items-start justify-between">
              <div>
                <p className="text-[11px] font-medium uppercase tracking-wide text-olive-700/70">{k.label}</p>
                <p className="mt-2 font-display text-2xl sm:text-3xl font-bold tabnum text-olive-900">{k.value}</p>
              </div>
              <div className="h-9 w-9 rounded-md grid place-items-center shrink-0" style={{ backgroundColor: k.bg, color: k.color }}>
                <Icon className="h-4 w-4" />
              </div>
            </GlassCard>
          );
        })}
      </div>

      {/* Grille 2×2 des 4 graphiques Recharts */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        {/* 1. Rendement par verger (style YieldByOrchardChart.tsx) */}
        <GlassCard hover className="p-5">
          <h2 className="font-display font-semibold text-base text-olive-900 mb-3">Rendement par verger (kg)</h2>
          {yieldRows.length === 0 ? (
            <p className="text-sm text-olive-700/70">Aucune donnée sur cette période.</p>
          ) : (
            <div style={{ height: Math.max(220, yieldRows.length * 36) }}>
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={yieldRows} layout="vertical" margin={{ left: 4, right: 16 }}>
                  <XAxis type="number" tick={{ fontSize: 10, fill: '#8b917c' }} axisLine={false} tickLine={false} />
                  <YAxis type="category" dataKey="orchardName" width={115} tick={{ fontSize: 11, fill: '#5e6650' }} axisLine={false} tickLine={false} />
                  <Tooltip />
                  <Bar dataKey="totalHarvestedKg" radius={[0, 4, 4, 0]}>
                    {yieldRows.map((r) => {
                      const ratio = r.totalHarvestedKg / maxYield;
                      const fill = ratio > 0.85 ? '#4a6b2a' : ratio > 0.6 ? '#6b8e3f' : '#a8be72';
                      return <Cell key={r.orchardId} fill={fill} />;
                    })}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            </div>
          )}
        </GlassCard>

        {/* 2. Répartition des demandes par statut */}
        <GlassCard hover className="p-5">
          <h2 className="font-display font-semibold text-base text-olive-900 mb-3">Demandes par statut</h2>
          {statusPie.length === 0 ? (
            <p className="text-sm text-olive-700/70">Aucune demande sur cette période.</p>
          ) : (
            <div style={{ height: 240 }}>
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie data={statusPie} dataKey="value" nameKey="name" innerRadius={55} outerRadius={85} paddingAngle={3}>
                    {statusPie.map((entry) => <Cell key={entry.name} fill={entry.color} />)}
                  </Pie>
                  <Tooltip />
                </PieChart>
              </ResponsiveContainer>
            </div>
          )}
        </GlassCard>

        {/* 3. Délais moyens de collecte */}
        <GlassCard hover className="p-5">
          <h2 className="font-display font-semibold text-base text-olive-900 mb-3">
            Délais moyens de collecte ({delaysQ.data?.sampleSize ?? 0} collectes)
          </h2>
          <div className="grid grid-cols-2 gap-3 mt-4">
            <GlassCard variant="subtle" className="p-4">
              <div className="text-xs text-olive-700/75">Soumise → Planifiée</div>
              <div className="font-display font-bold text-2xl text-olive-900 tabnum mt-1">
                {delaysQ.data?.avgSubmittedToScheduledHours != null ? `${delaysQ.data.avgSubmittedToScheduledHours} h` : '—'}
              </div>
            </GlassCard>
            <GlassCard variant="subtle" className="p-4">
              <div className="text-xs text-olive-700/75">Planifiée → Terminée</div>
              <div className="font-display font-bold text-2xl text-olive-900 tabnum mt-1">
                {delaysQ.data?.avgScheduledToCompletedHours != null ? `${delaysQ.data.avgScheduledToCompletedHours} h` : '—'}
              </div>
            </GlassCard>
          </div>
        </GlassCard>

        {/* 4. Taux de remplissage des bennes */}
        <GlassCard hover className="p-5">
          <h2 className="font-display font-semibold text-base text-olive-900 mb-3">Taux de remplissage des bennes</h2>
          {(binsQ.data?.items ?? []).length === 0 ? (
            <p className="text-sm text-olive-700/70">Aucune réception sur cette période.</p>
          ) : (
            <div className="space-y-3">
              {binsQ.data!.items.slice(0, 6).map((b) => {
                const pct = b.avgFillRate != null ? Math.round(b.avgFillRate * 100) : 0;
                return (
                  <div key={b.binId}>
                    <div className="flex justify-between text-xs mb-1 tabnum">
                      <span className="font-semibold text-olive-900">{b.binCode} ({b.capacityKg} kg)</span>
                      <span className="text-olive-700">{pct} % ({b.receptionCount} pesées)</span>
                    </div>
                    <div className="h-2 rounded-full bg-olive-900/10 overflow-hidden">
                      <div className="h-full rounded-full bg-ochre-500" style={{ width: `${Math.min(100, pct)}%` }} />
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </GlassCard>
      </div>
    </div>
  );
}
