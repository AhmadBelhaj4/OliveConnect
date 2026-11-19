import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { CheckCircle2, Flag, Play, Route } from 'lucide-react';
import { ApiClientError, apiFetch } from '../../api/http';
import { GlassCard } from '../../components/GlassCard';
import { StatusBadge } from '../../components/StatusBadge';
import { tourStatusStyles } from '../_shared/statusStyles';
import type { PageResponse, TourResponse } from '../../types/contract';

export function TeamLeaderToursPage() {
  const qc = useQueryClient();
  const [qtyByStop, setQtyByStop] = useState<Record<string, string>>({});
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const toursQuery = useQuery({
    queryKey: ['leader-tours'],
    queryFn: () => apiFetch<PageResponse<TourResponse>>('/tours'),
  });

  const action = useMutation({
    mutationFn: ({ url, body }: { url: string; body?: unknown }) =>
      apiFetch<TourResponse>(url, { method: 'POST', body: body ? JSON.stringify(body) : undefined }),
    onSuccess: () => {
      setErrorMsg(null);
      qc.invalidateQueries({ queryKey: ['leader-tours'] });
    },
    onError: (err) => setErrorMsg(err instanceof ApiClientError ? err.apiError.message : 'Erreur tournée'),
  });

  return (
    <div className="space-y-4">
      <div className="flex items-center gap-2.5">
        <div className="h-9 w-9 rounded-lg bg-olive-600/15 text-olive-700 grid place-items-center">
          <Route className="h-4 w-4" />
        </div>
        <h1 className="font-display font-bold text-xl text-olive-900">Mes tournées assignées</h1>
      </div>

      {errorMsg && <div className="px-3.5 py-2.5 rounded-lg bg-[#fee2e2] text-[#b33a3a] text-sm font-medium">{errorMsg}</div>}
      {toursQuery.isLoading && <p className="text-sm text-olive-700/70">Chargement…</p>}
      {toursQuery.data?.content.length === 0 && <GlassCard className="p-5 text-sm text-olive-700">Aucune tournée assignée.</GlassCard>}

      {toursQuery.data?.content.map((t) => (
        <GlassCard key={t.id} className="p-5 space-y-4">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <div>
              <h2 className="font-display font-bold text-lg text-olive-900 tabnum">Tournée du {t.tourDate}</h2>
              <p className="text-xs text-olive-700/75 tabnum">{t.stops.length} arrêt(s) ordonné(s)</p>
            </div>
            <div className="flex items-center gap-2">
              <StatusBadge style={tourStatusStyles[t.status]} />
              {t.status === 'PLANNED' && (
                <button
                  onClick={() => action.mutate({ url: `/tours/${t.id}/start` })}
                  className="inline-flex items-center gap-1.5 px-3.5 h-8 rounded-lg bg-olive-600 hover:bg-olive-700 text-cream-50 text-xs font-semibold"
                >
                  <Play className="h-3.5 w-3.5" /> Démarrer
                </button>
              )}
              {t.status === 'IN_PROGRESS' && (
                <button
                  onClick={() => action.mutate({ url: `/tours/${t.id}/complete` })}
                  className="inline-flex items-center gap-1.5 px-3.5 h-8 rounded-lg bg-ochre-500 hover:bg-ochre-600 text-cream-50 text-xs font-semibold"
                >
                  <Flag className="h-3.5 w-3.5" /> Clôturer la tournée
                </button>
              )}
            </div>
          </div>

          <ol className="space-y-2.5">
            {t.stops.map((s) => (
              <li key={s.stopId} className="p-3.5 rounded-xl glass-subtle flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                <div className="flex items-start gap-3">
                  <div className="h-7 w-7 rounded-full bg-olive-600/15 text-olive-800 font-display font-bold text-xs grid place-items-center shrink-0 tabnum">
                    {s.sequence}
                  </div>
                  <div>
                    <div className="font-semibold text-sm text-olive-900">{s.orchardName}</div>
                    <div className="text-xs text-olive-700/80 tabnum">
                      Prévu : <strong>{s.plannedQuantityKg} kg</strong>
                      {s.status === 'COMPLETED' && (
                        <> · Récolté : <strong className="text-[#1b5e20]">{s.actualQuantityKg} kg</strong></>
                      )}
                    </div>
                  </div>
                </div>

                {t.status === 'IN_PROGRESS' && s.status === 'PLANNED' ? (
                  <div className="flex items-center gap-2">
                    <input
                      type="number"
                      placeholder="Quantité (kg)"
                      value={qtyByStop[s.stopId] ?? String(s.plannedQuantityKg)}
                      onChange={(e) => setQtyByStop({ ...qtyByStop, [s.stopId]: e.target.value })}
                      className="w-28 h-8 px-2.5 rounded-lg glass-strong text-sm text-olive-900 tabnum outline-none"
                    />
                    <button
                      onClick={() =>
                        action.mutate({
                          url: `/tours/${t.id}/stops/${s.stopId}/complete`,
                          body: { actualQuantityKg: Number(qtyByStop[s.stopId] ?? s.plannedQuantityKg), note: null },
                        })
                      }
                      className="inline-flex items-center gap-1 px-3 h-8 rounded-lg bg-olive-600 text-cream-50 text-xs font-medium"
                    >
                      <CheckCircle2 className="h-3.5 w-3.5" /> Valider
                    </button>
                  </div>
                ) : (
                  <span className="text-xs font-medium text-olive-700">{s.status}</span>
                )}
              </li>
            ))}
          </ol>
        </GlassCard>
      ))}
    </div>
  );
}
