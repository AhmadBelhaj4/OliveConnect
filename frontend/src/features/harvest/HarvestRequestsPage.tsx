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
            className="w-full h-10 rounded-lg bg-olive-600 hover:bg-olive-700 text-cream-50 text-sm font-semibold transition-colors disabled:opacity-60 cursor-pointer"
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
