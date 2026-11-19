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
                className="inline-flex items-center gap-1.5 px-3 h-9 rounded-lg glass-subtle hover:bg-olive-600/15 text-olive-900 text-xs font-medium cursor-pointer"
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
            className="w-full h-10 rounded-lg bg-olive-600 hover:bg-olive-700 text-cream-50 text-sm font-semibold transition-colors disabled:opacity-60 cursor-pointer"
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
