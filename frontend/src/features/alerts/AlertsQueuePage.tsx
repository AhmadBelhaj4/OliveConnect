import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { MapPin, TriangleAlert } from 'lucide-react';
import { ApiClientError, apiFetch } from '../../api/http';
import { GlassCard } from '../../components/GlassCard';
import { StatusBadge } from '../../components/StatusBadge';
import { alertPriorityStyles, alertStatusStyles } from '../_shared/statusStyles';
import type { FieldAlertResponse, PageResponse } from '../../types/contract';

export function AlertsQueuePage() {
  const qc = useQueryClient();
  const [noteById, setNoteById] = useState<Record<string, string>>({});
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const alertsQuery = useQuery({
    queryKey: ['alerts-queue'],
    queryFn: () => apiFetch<PageResponse<FieldAlertResponse>>('/alerts?sort=priority,asc&sort=createdAt,desc'),
  });

  const mutateAlert = useMutation({
    mutationFn: ({ url, body }: { url: string; body?: unknown }) =>
      apiFetch<FieldAlertResponse>(url, { method: 'POST', body: body ? JSON.stringify(body) : undefined }),
    onSuccess: () => {
      setErrorMsg(null);
      qc.invalidateQueries({ queryKey: ['alerts-queue'] });
    },
    onError: (err) => setErrorMsg(err instanceof ApiClientError ? err.apiError.message : 'Erreur alerte'),
  });

  return (
    <GlassCard className="p-5">
      <div className="flex items-center gap-2.5 mb-4">
        <div className="h-9 w-9 rounded-lg bg-[#fee2e2] text-[#b33a3a] grid place-items-center">
          <TriangleAlert className="h-4 w-4" />
        </div>
        <div>
          <h1 className="font-display font-bold text-xl text-olive-900">File des alertes terrain (F2)</h1>
          <p className="text-xs text-olive-700/75">Triées par priorité · Actions OPEN → ACKNOWLEDGED → RESOLVED</p>
        </div>
      </div>

      {errorMsg && <div className="mb-4 px-3.5 py-2.5 rounded-lg bg-[#fee2e2] text-[#b33a3a] text-sm font-medium">{errorMsg}</div>}

      <div className="space-y-3">
        {alertsQuery.data?.content.map((a) => (
          <GlassCard key={a.id} variant="subtle" hover className="p-4 space-y-2">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <div className="flex items-center gap-2">
                <StatusBadge style={alertPriorityStyles[a.priority]} size="sm" />
                <strong className="font-display text-sm text-olive-900">{a.type}</strong>
                <span className="inline-flex items-center gap-1 text-xs text-olive-700/75 tabnum">
                  <MapPin className="h-3 w-3" /> [{a.location.coordinates[0]}, {a.location.coordinates[1]}]
                </span>
              </div>
              <StatusBadge style={alertStatusStyles[a.status]} size="sm" />
            </div>

            <p className="text-sm text-olive-900">{a.description}</p>

            {a.status === 'OPEN' && (
              <div className="flex flex-wrap gap-2 pt-2 border-t border-olive-900/10">
                <button
                  onClick={() => mutateAlert.mutate({ url: `/alerts/${a.id}/acknowledge` })}
                  className="px-3.5 h-8 rounded-lg bg-olive-600 hover:bg-olive-700 text-cream-50 text-xs font-medium"
                >
                  Prendre en charge
                </button>
                <input
                  placeholder="Motif pour écarter…"
                  value={noteById[a.id] ?? ''}
                  onChange={(e) => setNoteById({ ...noteById, [a.id]: e.target.value })}
                  className="flex-1 min-w-44 h-8 px-2.5 rounded-lg glass-strong text-xs text-olive-900 outline-none"
                />
                <button
                  onClick={() => mutateAlert.mutate({ url: `/alerts/${a.id}/dismiss`, body: { reason: noteById[a.id] ?? '' } })}
                  className="px-3 h-8 rounded-lg glass text-xs font-medium text-olive-800"
                >
                  Écarter
                </button>
              </div>
            )}

            {a.status === 'ACKNOWLEDGED' && (
              <div className="flex flex-wrap gap-2 pt-2 border-t border-olive-900/10">
                <input
                  placeholder="Note de résolution obligatoire…"
                  value={noteById[a.id] ?? ''}
                  onChange={(e) => setNoteById({ ...noteById, [a.id]: e.target.value })}
                  className="flex-1 min-w-48 h-8 px-2.5 rounded-lg glass-strong text-xs text-olive-900 outline-none"
                />
                <button
                  onClick={() => mutateAlert.mutate({ url: `/alerts/${a.id}/resolve`, body: { note: noteById[a.id] ?? '' } })}
                  className="px-3.5 h-8 rounded-lg bg-[#2e7d32] text-white text-xs font-semibold"
                >
                  Résoudre
                </button>
              </div>
            )}
          </GlassCard>
        ))}
      </div>
    </GlassCard>
  );
}
