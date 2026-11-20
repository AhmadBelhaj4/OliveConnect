import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { AlertOctagon, Package, Route, Truck, Users } from 'lucide-react';
import { ApiClientError, apiFetch } from '../../api/http';
import { GlassCard } from '../../components/GlassCard';
import { StatusBadge } from '../../components/StatusBadge';
import { tourStatusStyles } from '../_shared/statusStyles';
import type {
  HarvestRequestResponse,
  PageResponse,
  ResourceAvailabilityResponse,
  TourResponse,
} from '../../types/contract';

export function TourPlannerPage() {
  const qc = useQueryClient();
  const [tourDate, setTourDate] = useState(new Date(Date.now() + 86400000).toISOString().slice(0, 10));
  const [teamLeaderId, setTeamLeaderId] = useState('');
  const [selectedRequests, setSelectedRequests] = useState<string[]>([]);
  const [selectedVehicles, setSelectedVehicles] = useState<string[]>([]);
  const [selectedWorkers, setSelectedWorkers] = useState<string[]>([]);
  const [selectedBins, setSelectedBins] = useState<string[]>([]);
  const [conflictBanner, setConflictBanner] = useState<{ code: string; message: string; fields: Record<string, string> } | null>(null);

  const approvedQuery = useQuery({
    queryKey: ['approved-requests'],
    queryFn: () => apiFetch<PageResponse<HarvestRequestResponse>>('/harvest-requests?status=APPROVED'),
  });
  const availQuery = useQuery({
    queryKey: ['availability', tourDate],
    queryFn: () => apiFetch<ResourceAvailabilityResponse>(`/resources/availability?date=${tourDate}`),
  });
  const toursQuery = useQuery({
    queryKey: ['tours'],
    queryFn: () => apiFetch<PageResponse<TourResponse>>('/tours'),
  });

  const toggle = (list: string[], id: string, setter: (v: string[]) => void) =>
    setter(list.includes(id) ? list.filter((x) => x !== id) : [...list, id]);

  const planMutation = useMutation({
    mutationFn: async () => {
      const draft = await apiFetch<TourResponse>('/tours', { method: 'POST', body: JSON.stringify({ tourDate }) });
      return apiFetch<TourResponse>(`/tours/${draft.id}/plan`, {
        method: 'POST',
        body: JSON.stringify({
          teamLeaderId,
          vehicleIds: selectedVehicles,
          workerIds: selectedWorkers,
          binIds: selectedBins,
          stops: selectedRequests.map((id, i) => ({ harvestRequestId: id, sequence: i + 1 })),
        }),
      });
    },
    onSuccess: () => {
      setConflictBanner(null);
      setSelectedRequests([]);
      qc.invalidateQueries({ queryKey: ['tours'] });
      qc.invalidateQueries({ queryKey: ['approved-requests'] });
      qc.invalidateQueries({ queryKey: ['availability', tourDate] });
    },
    onError: (err) => {
      if (err instanceof ApiClientError) {
        setConflictBanner({ code: err.apiError.code, message: err.apiError.message, fields: err.apiError.fieldErrors || {} });
      }
    },
  });

  return (
    <div className="grid grid-cols-1 lg:grid-cols-12 gap-5">
      <GlassCard className="p-5 lg:col-span-7 space-y-4">
        <div className="flex items-center gap-2.5">
          <div className="h-9 w-9 rounded-lg bg-olive-600/15 text-olive-700 grid place-items-center">
            <Route className="h-4 w-4" />
          </div>
          <div>
            <h1 className="font-display font-bold text-xl text-olive-900">Planificateur de tournée</h1>
            <p className="text-xs text-olive-700/75">Vérification temps réel des disponibilités dans MongoDB</p>
          </div>
        </div>

        {/* Bandeau 409 RESOURCE_CONFLICT — preuve visuelle Scénario D */}
        {conflictBanner && (
          <div className="p-3.5 rounded-xl bg-[#fee2e2] border border-[#b33a3a]/30 text-[#b33a3a] text-sm">
            <div className="flex items-center gap-2 font-semibold">
              <AlertOctagon className="h-4 w-4 shrink-0" />
              <span>{conflictBanner.code} — {conflictBanner.message}</span>
            </div>
            {Object.keys(conflictBanner.fields).length > 0 && (
              <ul className="mt-1.5 ml-6 list-disc text-xs space-y-0.5">
                {Object.entries(conflictBanner.fields).map(([k, v]) => (
                  <li key={k}><code className="font-semibold">{k}</code> : {v}</li>
                ))}
              </ul>
            )}
          </div>
        )}

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          <div>
            <label htmlFor="tourDate" className="block text-xs font-medium text-olive-800 mb-1">Date de tournée</label>
            <input
              id="tourDate"
              type="date"
              value={tourDate}
              onChange={(e) => setTourDate(e.target.value)}
              className="w-full h-9 px-3 rounded-lg glass-strong text-sm text-olive-900 outline-none"
            />
          </div>
          <div>
            <label htmlFor="leaderId" className="block text-xs font-medium text-olive-800 mb-1">ID Chef d&apos;équipe</label>
            <input
              id="leaderId"
              value={teamLeaderId}
              onChange={(e) => setTeamLeaderId(e.target.value)}
              placeholder="ObjectId 24 hex (TEAM_LEADER)"
              className="w-full h-9 px-3 rounded-lg glass-strong text-sm text-olive-900 outline-none"
            />
          </div>
        </div>

        {/* 1. Demandes APPROVED */}
        <div className="p-3 rounded-xl glass-subtle space-y-1.5">
          <div className="text-xs font-semibold uppercase tracking-wide text-olive-800">
            1. Demandes APPROVED ({approvedQuery.data?.totalElements ?? 0})
          </div>
          {approvedQuery.data?.content.map((r) => (
            <label key={r.id} className="flex items-center gap-2 text-sm text-olive-900 cursor-pointer">
              <input type="checkbox" checked={selectedRequests.includes(r.id)} onChange={() => toggle(selectedRequests, r.id, setSelectedRequests)} />
              <span className="font-medium">{r.orchardName}</span>
              <span className="text-xs text-olive-700 tabnum">({r.estimatedQuantityKg} kg · {r.preferredDate})</span>
            </label>
          ))}
        </div>

        {/* 2. Véhicules libres */}
        <div className="p-3 rounded-xl glass-subtle space-y-1.5">
          <div className="flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wide text-olive-800">
            <Truck className="h-3.5 w-3.5" /> 2. Véhicules libres le {tourDate}
          </div>
          <div className="flex flex-wrap gap-2">
            {availQuery.data?.vehicles.map((v) => (
              <label key={v.id} className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg glass text-xs cursor-pointer">
                <input type="checkbox" checked={selectedVehicles.includes(v.id)} onChange={() => toggle(selectedVehicles, v.id, setSelectedVehicles)} />
                <span>{v.registration} ({v.capacityKg} kg)</span>
              </label>
            ))}
          </div>
        </div>

        {/* 3 & 4. Ouvriers + Bennes */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          <div className="p-3 rounded-xl glass-subtle space-y-1.5">
            <div className="flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wide text-olive-800">
              <Users className="h-3.5 w-3.5" /> 3. Ouvriers libres
            </div>
            <div className="flex flex-wrap gap-1.5">
              {availQuery.data?.workers.slice(0, 10).map((w) => (
                <label key={w.id} className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md glass text-xs cursor-pointer">
                  <input type="checkbox" checked={selectedWorkers.includes(w.id)} onChange={() => toggle(selectedWorkers, w.id, setSelectedWorkers)} />
                  <span>{w.fullName}</span>
                </label>
              ))}
            </div>
          </div>

          <div className="p-3 rounded-xl glass-subtle space-y-1.5">
            <div className="flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wide text-olive-800">
              <Package className="h-3.5 w-3.5" /> 4. Bennes libres
            </div>
            <div className="flex flex-wrap gap-1.5">
              {availQuery.data?.bins.slice(0, 10).map((b) => (
                <label key={b.id} className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md glass text-xs cursor-pointer">
                  <input type="checkbox" checked={selectedBins.includes(b.id)} onChange={() => toggle(selectedBins, b.id, setSelectedBins)} />
                  <span>{b.code}</span>
                </label>
              ))}
            </div>
          </div>
        </div>

        <button
          onClick={() => planMutation.mutate()}
          disabled={planMutation.isPending}
          className="w-full sm:w-auto px-5 h-10 rounded-lg bg-olive-600 hover:bg-olive-700 text-cream-50 text-sm font-semibold transition-colors disabled:opacity-60"
        >
          {planMutation.isPending ? 'Planification…' : 'Créer et planifier la tournée'}
        </button>
      </GlassCard>

      <GlassCard className="p-5 lg:col-span-5">
        <h2 className="font-display font-semibold text-base text-olive-900 mb-3">
          Tournées planifiées ({toursQuery.data?.totalElements ?? 0})
        </h2>
        <div className="space-y-2.5">
          {toursQuery.data?.content.map((t) => (
            <GlassCard key={t.id} variant="subtle" hover className="p-3.5">
              <div className="flex items-center justify-between gap-2">
                <span className="font-semibold text-sm text-olive-900 tabnum">Tournée du {t.tourDate}</span>
                <StatusBadge style={tourStatusStyles[t.status]} size="sm" />
              </div>
              <p className="text-xs text-olive-700/80 mt-1 tabnum">
                {t.stops.length} arrêt(s) · {t.vehicleIds.length} véhicule(s) · {t.workerIds.length} ouvrier(s) · {t.binIds.length} benne(s)
              </p>
            </GlassCard>
          ))}
        </div>
      </GlassCard>
    </div>
  );
}
