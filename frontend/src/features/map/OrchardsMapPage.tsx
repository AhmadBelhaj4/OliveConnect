import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { CircleMarker, MapContainer, Popup, TileLayer, useMapEvents } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import { Map as MapIcon } from 'lucide-react';
import { apiFetch } from '../../api/http';
import { GlassCard } from '../../components/GlassCard';
import { maturityStyles } from '../_shared/statusStyles';
import type { MaturityStatus, OrchardMapFeatureCollection } from '../../types/contract';

function BboxWatcher({ onChange }: { onChange: (bbox: string) => void }) {
  useMapEvents({
    moveend: (e) => {
      const b = e.target.getBounds();
      onChange(`${b.getWest().toFixed(4)},${b.getSouth().toFixed(4)},${b.getEast().toFixed(4)},${b.getNorth().toFixed(4)}`);
    },
  });
  return null;
}

export function OrchardsMapPage() {
  const [bbox, setBbox] = useState('10.50,34.50,11.05,35.00');

  const { data, isLoading, isError } = useQuery({
    queryKey: ['orchards-map', bbox],
    queryFn: () => apiFetch<OrchardMapFeatureCollection>(`/orchards/map?bbox=${encodeURIComponent(bbox)}`),
  });

  return (
    <GlassCard className="p-5 space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-2.5">
          <div className="h-9 w-9 rounded-lg bg-olive-600/15 text-olive-700 grid place-items-center">
            <MapIcon className="h-4 w-4" />
          </div>
          <div>
            <h1 className="font-display font-bold text-xl text-olive-900">Carte des vergers — Sfax</h1>
            <p className="text-xs text-olive-700/75 tabnum">Zone visible : {bbox}</p>
          </div>
        </div>

        {/* Légende alignée sur prototype react/statusStyles.ts */}
        <div className="flex flex-wrap gap-1.5">
          {(Object.keys(maturityStyles) as MaturityStatus[]).map((m) => (
            <span
              key={m}
              className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium glass-subtle text-olive-900"
            >
              <span className="h-2 w-2 rounded-full" style={{ backgroundColor: maturityStyles[m].color }} />
              {maturityStyles[m].label}
            </span>
          ))}
        </div>
      </div>

      {isLoading && <p className="text-xs text-olive-700/70">Chargement de la zone…</p>}
      {isError && (
        <div className="px-3.5 py-2.5 rounded-lg bg-ochre-100 text-ochre-800 text-xs">
          En Semaine 1, l&apos;endpoint réel <code>GET /api/v1/orchards/map</code> (tâche B7, S2) n&apos;est pas encore actif — utilisez <code>VITE_USE_MOCKS=true</code> pour tester la carte sur MSW.
        </div>
      )}
      {data?.truncated && (
        <div className="px-3.5 py-2.5 rounded-lg bg-[#fee2e2] text-[#b33a3a] text-xs font-medium">
          Résultat tronqué à 500 vergers : zoomez sur la carte.
        </div>
      )}

      <div className="rounded-xl overflow-hidden border border-white/60 shadow-sm">
        {/* Leaflet attend [lat, lon] alors que le contrat GeoJSON renvoie [lon, lat] */}
        <MapContainer center={[34.73, 10.76]} zoom={11} style={{ height: 460, width: '100%' }}>
          <TileLayer
            attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          />
          <BboxWatcher onChange={setBbox} />
          {data?.features.map((f) => {
            const [lon, lat] = f.geometry.coordinates;
            const mat = maturityStyles[f.properties.maturityStatus];
            return (
              <CircleMarker
                key={f.properties.orchardId}
                center={[lat, lon]}
                radius={9}
                pathOptions={{ color: mat.color, fillColor: mat.color, fillOpacity: 0.78 }}
              >
                <Popup>
                  <strong>{f.properties.name ?? 'Verger public'}</strong>
                  <br />Variété : {f.properties.oliveVariety}
                  <br />Maturité : {mat.label}
                  {f.properties.areaHectares != null && (
                    <><br />Surface : {f.properties.areaHectares} ha</>
                  )}
                </Popup>
              </CircleMarker>
            );
          })}
        </MapContainer>
      </div>
    </GlassCard>
  );
}
