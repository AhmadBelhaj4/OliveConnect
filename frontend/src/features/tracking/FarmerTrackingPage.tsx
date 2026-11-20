import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Bell, Check, MapPin, TriangleAlert } from 'lucide-react';
import { ApiClientError, apiFetch } from '../../api/http';
import { GlassCard } from '../../components/GlassCard';
import { StatusBadge } from '../../components/StatusBadge';
import { alertPriorityStyles, alertStatusStyles } from '../_shared/statusStyles';
import type {
  AlertPriority, AlertType, FieldAlertResponse, NotificationResponse, OrchardResponse, PageResponse,
} from '../../types/contract';

export function FarmerTrackingPage() {
  const qc = useQueryClient();
  const [orchardId, setOrchardId] = useState('');
  const [type, setType] = useState<AlertType>('PEST');
  const [priority, setPriority] = useState<AlertPriority>('HIGH');
  const [description, setDescription] = useState('');
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const orchardsQuery = useQuery({
    queryKey: ['orchards', 'mine'],
    queryFn: () => apiFetch<PageResponse<OrchardResponse>>('/orchards/mine'),
  });
  const notifsQuery = useQuery({
    queryKey: ['notifications'],
    queryFn: () => apiFetch<PageResponse<NotificationResponse>>('/notifications'),
  });
  const alertsQuery = useQuery({
    queryKey: ['alerts', 'mine'],
    queryFn: () => apiFetch<PageResponse<FieldAlertResponse>>('/alerts'),
  });

  const markRead = useMutation({
    mutationFn: (id: string) => apiFetch<NotificationResponse>(`/notifications/${id}/read`, { method: 'POST' }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['notifications'] }),
  });

  const createAlert = useMutation({
    mutationFn: () => {
      const selected = orchardsQuery.data?.content.find((o) => o.id === orchardId);
      const location = selected?.location ?? { type: 'Point' as const, coordinates: [10.76, 34.73] as [number, number] };
      return apiFetch<FieldAlertResponse>('/alerts', {
        method: 'POST',
        body: JSON.stringify({ orchardId, type, description, location, priority }),
      });
    },
    onSuccess: () => {
      setErrorMsg(null);
      setDescription('');
      qc.invalidateQueries({ queryKey: ['alerts', 'mine'] });
    },
    onError: (err) => setErrorMsg(err instanceof ApiClientError ? err.apiError.message : 'Erreur alerte'),
  });

  const selectedOrchard = orchardsQuery.data?.content.find((o) => o.id === orchardId);

  return (
    <div className="grid grid-cols-1 lg:grid-cols-12 gap-5">
      <div className="lg:col-span-6 space-y-4">
        <GlassCard className="p-5 space-y-3">
          <div className="flex items-center gap-2.5">
            <div className="h-9 w-9 rounded-lg bg-[#fee2e2] text-[#b33a3a] grid place-items-center">
              <TriangleAlert className="h-4 w-4" />
            </div>
            <div>
              <h1 className="font-display font-bold text-lg text-olive-900">Signaler une alerte terrain (F2)</h1>
              <p className="text-xs text-olive-700/75">Position préremplie depuis le verger sélectionné</p>
            </div>
          </div>

          {errorMsg && <div className="px-3 py-2 rounded-lg bg-[#fee2e2] text-[#b33a3a] text-xs font-medium">{errorMsg}</div>}

          <div>
            <label className="block text-xs font-medium text-olive-800 mb-1">Verger concerné</label>
            <select value={orchardId} onChange={(e) => setOrchardId(e.target.value)} className="w-full h-9 px-3 rounded-lg glass-strong text-sm text-olive-900">
              <option value="">— Choisir un verger —</option>
              {orchardsQuery.data?.content.map((o) => <option key={o.id} value={o.id}>{o.name}</option>)}
            </select>
            {selectedOrchard && (
              <div className="inline-flex items-center gap-1 mt-1 text-[11px] text-olive-700 tabnum">
                <MapPin className="h-3 w-3" /> [{selectedOrchard.location.coordinates[0]}, {selectedOrchard.location.coordinates[1]}]
              </div>
            )}
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-medium text-olive-800 mb-1">Type</label>
              <select value={type} onChange={(e) => setType(e.target.value as AlertType)} className="w-full h-9 px-3 rounded-lg glass-strong text-sm text-olive-900">
                <option value="MATURITY">Maturité optimale</option>
                <option value="PEST">Ravageurs</option>
                <option value="RAIN">Pluie</option>
                <option value="OTHER">Autre</option>
              </select>
            </div>
            <div>
              <label className="block text-xs font-medium text-olive-800 mb-1">Priorité</label>
              <select value={priority} onChange={(e) => setPriority(e.target.value as AlertPriority)} className="w-full h-9 px-3 rounded-lg glass-strong text-sm text-olive-900">
                <option value="HIGH">Haute</option>
                <option value="MEDIUM">Moyenne</option>
                <option value="LOW">Basse</option>
              </select>
            </div>
          </div>

          <div>
            <label className="block text-xs font-medium text-olive-800 mb-1">Description</label>
            <textarea value={description} onChange={(e) => setDescription(e.target.value)} rows={3} className="w-full p-2.5 rounded-lg glass-strong text-sm text-olive-900 outline-none" />
          </div>

          <button onClick={() => createAlert.mutate()} disabled={createAlert.isPending} className="px-4 h-9 rounded-lg bg-olive-600 hover:bg-olive-700 text-cream-50 text-xs font-semibold">
            Envoyer l&apos;alerte
          </button>
        </GlassCard>

        <GlassCard className="p-5">
          <h2 className="font-display font-semibold text-base text-olive-900 mb-3">
            Mes alertes ({alertsQuery.data?.totalElements ?? 0})
          </h2>
          <div className="space-y-2.5">
            {alertsQuery.data?.content.map((a) => (
              <div key={a.id} className="p-3 rounded-xl glass-subtle space-y-1">
                <div className="flex items-center justify-between gap-2">
                  <div className="flex items-center gap-1.5">
                    <StatusBadge style={alertPriorityStyles[a.priority]} size="sm" />
                    <strong className="text-sm text-olive-900">{a.type}</strong>
                  </div>
                  <StatusBadge style={alertStatusStyles[a.status]} size="sm" />
                </div>
                <p className="text-xs text-olive-800">{a.description}</p>
                {a.resolutionNote && <p className="text-xs text-[#2e7d32] font-medium">Réponse : {a.resolutionNote}</p>}
              </div>
            ))}
          </div>
        </GlassCard>
      </div>

      {/* Flux de notifications inspiré de ActivityFeed.tsx */}
      <GlassCard className="p-5 lg:col-span-6 self-start">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-2">
            <Bell className="h-4 w-4 text-olive-700" />
            <h2 className="font-display font-semibold text-base text-olive-900">
              Mes notifications ({notifsQuery.data?.totalElements ?? 0})
            </h2>
          </div>
        </div>
        {notifsQuery.data?.content.length === 0 && <p className="text-sm text-olive-700/70">Aucune notification.</p>}
        <ol className="space-y-2.5">
          {notifsQuery.data?.content.map((n) => (
            <li
              key={n.id}
              className={`p-3 rounded-xl flex items-start gap-3 transition-colors ${n.readAt ? 'glass-subtle' : 'glass-strong border-l-4 border-l-olive-600'}`}
            >
              <div className="h-8 w-8 rounded-md bg-olive-600/15 text-olive-700 grid place-items-center shrink-0">
                <Check className="h-4 w-4" />
              </div>
              <div className="flex-1 min-w-0">
                <div className="text-xs font-semibold text-olive-900">{n.type}</div>
                <p className="text-sm text-olive-800">{n.message}</p>
                <div className="flex items-center justify-between mt-1.5">
                  <span className="text-[11px] text-olive-700/70 tabnum">{n.createdAt.slice(0, 16).replace('T', ' ')}</span>
                  {!n.readAt && (
                    <button onClick={() => markRead.mutate(n.id)} className="px-2.5 py-0.5 rounded-md bg-olive-600/15 text-olive-900 text-[11px] font-medium hover:bg-olive-600 hover:text-cream-50">
                      Marquer lu
                    </button>
                  )}
                </div>
              </div>
            </li>
          ))}
        </ol>
      </GlassCard>
    </div>
  );
}
