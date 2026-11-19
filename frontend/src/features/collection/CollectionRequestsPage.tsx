import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Check, Inbox, X } from 'lucide-react';
import { ApiClientError, apiFetch } from '../../api/http';
import { GlassCard } from '../../components/GlassCard';
import { StatusBadge } from '../../components/StatusBadge';
import { harvestStatusStyles } from '../_shared/statusStyles';
import { cn } from '../../lib/utils';
import type { HarvestRequestResponse, HarvestStatus, PageResponse } from '../../types/contract';

const FILTERS: { value: HarvestStatus | ''; label: string }[] = [
  { value: 'SUBMITTED', label: 'À traiter' },
  { value: 'APPROVED', label: 'Approuvées' },
  { value: 'SCHEDULED', label: 'Planifiées' },
  { value: 'IN_PROGRESS', label: 'En cours' },
  { value: 'COMPLETED', label: 'Terminées' },
  { value: '', label: 'Toutes' },
];

export function CollectionRequestsPage() {
  const qc = useQueryClient();
  const [statusFilter, setStatusFilter] = useState<HarvestStatus | ''>('SUBMITTED');
  const [rejectId, setRejectId] = useState<string | null>(null);
  const [rejectReason, setRejectReason] = useState('');
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const query = useQuery({
    queryKey: ['collection-requests', statusFilter],
    queryFn: () => {
      const qs = statusFilter ? `?status=${statusFilter}` : '';
      return apiFetch<PageResponse<HarvestRequestResponse>>(`/harvest-requests${qs}`);
    },
  });

  const reviewMutation = useMutation({
    mutationFn: ({ id, decision, reason }: { id: string; decision: 'APPROVE' | 'REJECT'; reason: string | null }) =>
      apiFetch<HarvestRequestResponse>(`/harvest-requests/${id}/review`, {
        method: 'POST',
        body: JSON.stringify({ decision, reason }),
      }),
    onSuccess: () => {
      setErrorMsg(null);
      setRejectId(null);
      setRejectReason('');
      qc.invalidateQueries({ queryKey: ['collection-requests'] });
    },
    onError: (err) => setErrorMsg(err instanceof ApiClientError ? err.apiError.message : 'Erreur lors de la revue'),
  });

  return (
    <GlassCard className="p-5">
      <div className="flex flex-wrap items-center justify-between gap-3 mb-5">
        <div className="flex items-center gap-2.5">
          <div className="h-9 w-9 rounded-lg bg-olive-600/15 text-olive-700 grid place-items-center">
            <Inbox className="h-4 w-4" />
          </div>
          <div>
            <h1 className="font-display font-bold text-xl text-olive-900">File des demandes de récolte</h1>
            <p className="text-xs text-olive-700/75 tabnum">{query.data?.totalElements ?? 0} demande(s)</p>
          </div>
        </div>

        {/* Pilules de filtre style TopBar du prototype */}
        <div className="inline-flex flex-wrap rounded-lg glass-subtle p-0.5 gap-0.5">
          {FILTERS.map((f) => (
            <button
              key={f.label}
              onClick={() => setStatusFilter(f.value)}
              className={cn(
                'px-3 h-8 text-xs font-medium rounded-md transition-colors',
                statusFilter === f.value ? 'bg-olive-600 text-cream-50' : 'text-olive-700 hover:bg-olive-900/5',
              )}
            >
              {f.label}
            </button>
          ))}
        </div>
      </div>

      {errorMsg && (
        <div className="mb-4 px-3.5 py-2.5 rounded-lg bg-[#fee2e2] text-[#b33a3a] text-sm font-medium">{errorMsg}</div>
      )}
      {query.isLoading && <p className="text-sm text-olive-700/70">Chargement de la file…</p>}
      {query.data?.content.length === 0 && <p className="text-sm text-olive-700/70">Aucune demande pour ce filtre.</p>}

      <div className="space-y-3">
        {query.data?.content.map((r) => (
          <GlassCard key={r.id} variant="subtle" hover className="p-4">
            <div className="flex flex-wrap items-start justify-between gap-2">
              <div>
                <div className="flex items-center gap-2 flex-wrap">
                  <span className="font-display font-semibold text-base text-olive-900">{r.orchardName}</span>
                  <span className="text-sm font-semibold text-olive-700 tabnum">{r.estimatedQuantityKg} kg</span>
                  {r.type === 'URGENT' && (
                    <span className="px-2 py-0.5 rounded-full text-[11px] font-semibold bg-ochre-100 text-ochre-700">
                      URGENT
                    </span>
                  )}
                </div>
                <p className="text-xs text-olive-700/80 mt-0.5 tabnum">
                  Date souhaitée : <strong>{r.preferredDate}</strong>
                  {r.urgencyReason ? ` · Motif : ${r.urgencyReason}` : ''}
                </p>
              </div>
              <StatusBadge style={harvestStatusStyles[r.status]} />
            </div>

            {r.status === 'SUBMITTED' && (
              <div className="flex flex-wrap items-center gap-2 mt-3 pt-3 border-t border-olive-900/10">
                <button
                  onClick={() => reviewMutation.mutate({ id: r.id, decision: 'APPROVE', reason: null })}
                  disabled={reviewMutation.isPending}
                  className="inline-flex items-center gap-1.5 px-3.5 h-8 rounded-lg bg-olive-600 hover:bg-olive-700 text-cream-50 text-xs font-medium transition-colors"
                >
                  <Check className="h-3.5 w-3.5" /> Approuver
                </button>
                <button
                  onClick={() => setRejectId(rejectId === r.id ? null : r.id)}
                  className="inline-flex items-center gap-1.5 px-3.5 h-8 rounded-lg glass-subtle hover:bg-[#fee2e2] text-[#b33a3a] text-xs font-medium transition-colors"
                >
                  <X className="h-3.5 w-3.5" /> Rejeter
                </button>
              </div>
            )}

            {rejectId === r.id && (
              <div className="flex flex-col sm:flex-row gap-2 mt-3">
                <input
                  placeholder="Motif obligatoire du rejet…"
                  value={rejectReason}
                  onChange={(e) => setRejectReason(e.target.value)}
                  className="flex-1 h-9 px-3 rounded-lg glass-strong text-sm text-olive-900 outline-none placeholder:text-olive-700/50"
                />
                <button
                  onClick={() => reviewMutation.mutate({ id: r.id, decision: 'REJECT', reason: rejectReason })}
                  className="px-4 h-9 rounded-lg bg-[#b33a3a] text-white text-xs font-semibold"
                >
                  Confirmer le rejet
                </button>
              </div>
            )}
          </GlassCard>
        ))}
      </div>
    </GlassCard>
  );
}
