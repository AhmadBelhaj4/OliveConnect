import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ClipboardCheck, Factory } from 'lucide-react';
import { ApiClientError, apiFetch } from '../../api/http';
import { GlassCard } from '../../components/GlassCard';
import { StatusBadge } from '../../components/StatusBadge';
import { qualityGradeStyles } from '../_shared/statusStyles';
import type { PageResponse, PressReceptionResponse, PressResponse, QualityGrade } from '../../types/contract';

export function PressPage() {
  const qc = useQueryClient();
  const [tourId, setTourId] = useState('');
  const [binId, setBinId] = useState('');
  const [pressId, setPressId] = useState('');
  const [weightKg, setWeightKg] = useState('450');
  const [qualityGrade, setQualityGrade] = useState<QualityGrade>('A');
  const [notes, setNotes] = useState('Accepté');
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const pressesQuery = useQuery({
    queryKey: ['presses'],
    queryFn: () => apiFetch<PageResponse<PressResponse>>('/presses'),
  });
  const receptionsQuery = useQuery({
    queryKey: ['press-receptions'],
    queryFn: () => apiFetch<PageResponse<PressReceptionResponse>>('/press-receptions'),
  });

  const recordMutation = useMutation({
    mutationFn: () =>
      apiFetch<PressReceptionResponse>('/press-receptions', {
        method: 'POST',
        body: JSON.stringify({
          tourId, binId, pressId, weightKg: Number(weightKg), qualityGrade, notes, receivedAt: null,
        }),
      }),
    onSuccess: () => {
      setErrorMsg(null);
      qc.invalidateQueries({ queryKey: ['press-receptions'] });
    },
    onError: (err) => setErrorMsg(err instanceof ApiClientError ? err.apiError.message : 'Erreur réception'),
  });

  const capacityMutation = useMutation({
    mutationFn: ({ id, availableCapacityKg }: { id: string; availableCapacityKg: number }) =>
      apiFetch<PressResponse>(`/presses/${id}/capacity`, {
        method: 'PATCH',
        body: JSON.stringify({ availableCapacityKg }),
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['presses'] }),
  });

  return (
    <div className="grid grid-cols-1 lg:grid-cols-12 gap-5">
      <GlassCard className="p-5 lg:col-span-5 space-y-3">
        <div className="flex items-center gap-2.5 mb-2">
          <div className="h-9 w-9 rounded-lg bg-olive-600/15 text-olive-700 grid place-items-center">
            <ClipboardCheck className="h-4 w-4" />
          </div>
          <h1 className="font-display font-bold text-lg text-olive-900">Enregistrer une réception</h1>
        </div>

        {errorMsg && <div className="px-3 py-2 rounded-lg bg-[#fee2e2] text-[#b33a3a] text-xs font-medium">{errorMsg}</div>}

        <div>
          <label className="block text-xs font-medium text-olive-800 mb-1">Pressoir</label>
          <select value={pressId} onChange={(e) => setPressId(e.target.value)} className="w-full h-9 px-3 rounded-lg glass-strong text-sm text-olive-900">
            <option value="">— Choisir un pressoir —</option>
            {pressesQuery.data?.content.map((p) => (
              <option key={p.id} value={p.id}>{p.name} ({p.availableCapacityKg} / {p.dailyCapacityKg} kg)</option>
            ))}
          </select>
        </div>
        <div>
          <label className="block text-xs font-medium text-olive-800 mb-1">ID Tournée (IN_PROGRESS ou COMPLETED)</label>
          <input value={tourId} onChange={(e) => setTourId(e.target.value)} className="w-full h-9 px-3 rounded-lg glass-strong text-sm text-olive-900" />
        </div>
        <div>
          <label className="block text-xs font-medium text-olive-800 mb-1">ID Benne</label>
          <input value={binId} onChange={(e) => setBinId(e.target.value)} className="w-full h-9 px-3 rounded-lg glass-strong text-sm text-olive-900" />
        </div>
        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="block text-xs font-medium text-olive-800 mb-1">Poids (kg)</label>
            <input type="number" value={weightKg} onChange={(e) => setWeightKg(e.target.value)} className="w-full h-9 px-3 rounded-lg glass-strong text-sm text-olive-900 tabnum" />
          </div>
          <div>
            <label className="block text-xs font-medium text-olive-800 mb-1">Qualité</label>
            <select value={qualityGrade} onChange={(e) => setQualityGrade(e.target.value as QualityGrade)} className="w-full h-9 px-3 rounded-lg glass-strong text-sm text-olive-900">
              <option value="A">Grade A</option>
              <option value="B">Grade B</option>
              <option value="C">Grade C</option>
            </select>
          </div>
        </div>
        <div>
          <label className="block text-xs font-medium text-olive-800 mb-1">Notes</label>
          <input value={notes} onChange={(e) => setNotes(e.target.value)} className="w-full h-9 px-3 rounded-lg glass-strong text-sm text-olive-900" />
        </div>
        <button
          onClick={() => recordMutation.mutate()}
          disabled={recordMutation.isPending}
          className="w-full h-10 rounded-lg bg-olive-600 hover:bg-olive-700 text-cream-50 text-sm font-semibold"
        >
          Confirmer la réception
        </button>
      </GlassCard>

      <div className="lg:col-span-7 space-y-4">
        <GlassCard className="p-5">
          <div className="flex items-center gap-2 mb-3">
            <Factory className="h-4 w-4 text-olive-700" />
            <h2 className="font-display font-semibold text-base text-olive-900">Capacité des pressoirs</h2>
          </div>
          <div className="space-y-3">
            {pressesQuery.data?.content.map((p) => {
              const pct = Math.round((p.availableCapacityKg / p.dailyCapacityKg) * 100);
              return (
                <div key={p.id} className="p-3.5 rounded-xl glass-subtle">
                  <div className="flex items-center justify-between gap-2 mb-1.5">
                    <span className="font-semibold text-sm text-olive-900">{p.name}</span>
                    <button
                      onClick={() => capacityMutation.mutate({ id: p.id, availableCapacityKg: Math.max(0, p.availableCapacityKg - 1000) })}
                      className="px-2.5 py-1 rounded-md glass text-xs font-medium text-olive-800 hover:bg-olive-600/10"
                    >
                      -1 000 kg
                    </button>
                  </div>
                  <div className="h-2 rounded-full bg-olive-900/10 overflow-hidden">
                    <div className="h-full rounded-full bg-olive-600 transition-all" style={{ width: `${pct}%` }} />
                  </div>
                  <div className="text-xs text-olive-700/80 mt-1 tabnum">
                    Disponible : <strong>{p.availableCapacityKg.toLocaleString('fr-FR')} kg</strong> / {p.dailyCapacityKg.toLocaleString('fr-FR')} kg ({pct} %)
                  </div>
                </div>
              );
            })}
          </div>
        </GlassCard>

        <GlassCard className="p-5">
          <h2 className="font-display font-semibold text-base text-olive-900 mb-3">
            Historique des réceptions ({receptionsQuery.data?.totalElements ?? 0})
          </h2>
          <div className="space-y-2">
            {receptionsQuery.data?.content.map((r) => (
              <div key={r.id} className="p-3 rounded-lg glass-subtle flex items-center justify-between gap-2">
                <div className="tabnum">
                  <strong className="text-sm text-olive-900">{r.weightKg} kg</strong>
                  <span className="text-xs text-olive-700/75 ml-2">{r.receivedAt.slice(0, 16).replace('T', ' ')}</span>
                </div>
                <StatusBadge style={qualityGradeStyles[r.qualityGrade]} size="sm" />
              </div>
            ))}
          </div>
        </GlassCard>
      </div>
    </div>
  );
}
