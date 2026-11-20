import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Boxes, CalendarCheck, Plus } from 'lucide-react';
import { apiFetch } from '../../api/http';
import { GlassCard } from '../../components/GlassCard';
import { StatusBadge } from '../../components/StatusBadge';
import { resourceStatusStyles } from '../_shared/statusStyles';
import type { PageResponse, ResourceAvailabilityResponse, VehicleResponse } from '../../types/contract';

export function ResourcesPage() {
  const qc = useQueryClient();
  const [date, setDate] = useState(new Date(Date.now() + 86400000).toISOString().slice(0, 10));
  const [registration, setRegistration] = useState('');
  const [capacityKg, setCapacityKg] = useState('3000');

  const vehiclesQuery = useQuery({
    queryKey: ['resources', 'vehicles'],
    queryFn: () => apiFetch<PageResponse<VehicleResponse>>('/resources/vehicles'),
  });
  const availQuery = useQuery({
    queryKey: ['availability', date],
    queryFn: () => apiFetch<ResourceAvailabilityResponse>(`/resources/availability?date=${date}`),
  });

  const addVehicle = useMutation({
    mutationFn: () =>
      apiFetch<VehicleResponse>('/resources/vehicles', {
        method: 'POST',
        body: JSON.stringify({ type: 'TRACTOR', registration, capacityKg: Number(capacityKg) }),
      }),
    onSuccess: () => {
      setRegistration('');
      qc.invalidateQueries({ queryKey: ['resources', 'vehicles'] });
      qc.invalidateQueries({ queryKey: ['availability', date] });
    },
  });

  return (
    <div className="grid grid-cols-1 lg:grid-cols-12 gap-5">
      <GlassCard className="p-5 lg:col-span-7 space-y-4">
        <div className="flex items-center gap-2.5">
          <div className="h-9 w-9 rounded-lg bg-olive-600/15 text-olive-700 grid place-items-center">
            <Boxes className="h-4 w-4" />
          </div>
          <h1 className="font-display font-bold text-xl text-olive-900">
            Véhicules ({vehiclesQuery.data?.totalElements ?? 0})
          </h1>
        </div>

        <div className="flex flex-wrap gap-2">
          <input
            placeholder="Immatriculation (ex. 200 TUN 9999)"
            value={registration}
            onChange={(e) => setRegistration(e.target.value)}
            className="flex-1 min-w-48 h-9 px-3 rounded-lg glass-strong text-sm text-olive-900 outline-none"
          />
          <input
            type="number"
            value={capacityKg}
            onChange={(e) => setCapacityKg(e.target.value)}
            className="w-28 h-9 px-3 rounded-lg glass-strong text-sm text-olive-900 tabnum outline-none"
          />
          <button
            onClick={() => addVehicle.mutate()}
            className="inline-flex items-center gap-1 px-4 h-9 rounded-lg bg-olive-600 hover:bg-olive-700 text-cream-50 text-xs font-semibold"
          >
            <Plus className="h-3.5 w-3.5" /> Ajouter tracteur
          </button>
        </div>

        <div className="space-y-2">
          {vehiclesQuery.data?.content.map((v) => (
            <div key={v.id} className="p-3 rounded-xl glass-subtle flex items-center justify-between gap-2">
              <div>
                <span className="font-semibold text-sm text-olive-900">{v.registration}</span>
                <span className="text-xs text-olive-700/80 ml-2 tabnum">{v.type} · {v.capacityKg} kg</span>
              </div>
              <StatusBadge style={resourceStatusStyles[v.status]} size="sm" />
            </div>
          ))}
        </div>
      </GlassCard>

      <GlassCard className="p-5 lg:col-span-5 space-y-4 self-start">
        <div className="flex items-center justify-between gap-2">
          <div className="flex items-center gap-2">
            <CalendarCheck className="h-4 w-4 text-olive-700" />
            <h2 className="font-display font-semibold text-base text-olive-900">Disponibilité par date</h2>
          </div>
          <input
            type="date"
            value={date}
            onChange={(e) => setDate(e.target.value)}
            className="h-8 px-2.5 rounded-lg glass-strong text-xs text-olive-900"
          />
        </div>

        {availQuery.data && (
          <div className="grid grid-cols-2 gap-3">
            {[
              { label: 'Ouvriers libres', value: availQuery.data.workers.length },
              { label: 'Véhicules libres', value: availQuery.data.vehicles.length },
              { label: 'Bennes libres', value: availQuery.data.bins.length },
              { label: 'Matériel dispo', value: availQuery.data.equipment.length },
            ].map((kpi) => (
              <GlassCard key={kpi.label} variant="subtle" className="p-3.5">
                <div className="text-xs text-olive-700/75">{kpi.label}</div>
                <div className="font-display font-bold text-2xl text-olive-900 tabnum mt-0.5">{kpi.value}</div>
              </GlassCard>
            ))}
          </div>
        )}
      </GlassCard>
    </div>
  );
}
