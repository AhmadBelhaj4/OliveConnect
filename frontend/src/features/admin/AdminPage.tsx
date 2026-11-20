import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { FileUp, Users } from 'lucide-react';
import { ApiClientError, apiFetch } from '../../api/http';
import { GlassCard } from '../../components/GlassCard';
import type { ImportJobResponse, ImportType, PageResponse, Role, UserResponse } from '../../types/contract';

export function AdminPage() {
  const qc = useQueryClient();
  const [importType, setImportType] = useState<ImportType>('ORCHARDS');
  const [file, setFile] = useState<File | null>(null);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const usersQ = useQuery({
    queryKey: ['admin-users'],
    queryFn: () => apiFetch<PageResponse<UserResponse>>('/admin/users'),
  });
  const importsQ = useQuery({
    queryKey: ['admin-imports'],
    queryFn: () => apiFetch<PageResponse<ImportJobResponse>>('/admin/imports'),
  });

  const uploadMutation = useMutation({
    mutationFn: () => {
      if (!file) throw new Error('Choisissez un fichier');
      const fd = new FormData();
      fd.append('type', importType);
      fd.append('file', file);
      return apiFetch<ImportJobResponse>('/admin/imports', { method: 'POST', body: fd });
    },
    onSuccess: () => {
      setErrorMsg(null);
      setFile(null);
      qc.invalidateQueries({ queryKey: ['admin-imports'] });
    },
    onError: (err) => setErrorMsg(err instanceof ApiClientError ? err.apiError.message : (err as Error).message),
  });

  const roleMutation = useMutation({
    mutationFn: ({ id, role }: { id: string; role: Role }) =>
      apiFetch<UserResponse>(`/admin/users/${id}/role`, { method: 'PATCH', body: JSON.stringify({ role }) }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin-users'] }),
  });

  const activeMutation = useMutation({
    mutationFn: ({ id, active }: { id: string; active: boolean }) =>
      apiFetch<UserResponse>(`/admin/users/${id}/active`, { method: 'PATCH', body: JSON.stringify({ active }) }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin-users'] }),
  });

  return (
    <div className="grid grid-cols-1 lg:grid-cols-12 gap-5">
      {/* Bloc Import CSV / JSON (Scénario E) */}
      <GlassCard className="p-5 lg:col-span-6 space-y-4">
        <div className="flex items-center gap-2.5">
          <div className="h-9 w-9 rounded-lg bg-olive-600/15 text-olive-700 grid place-items-center">
            <FileUp className="h-4 w-4" />
          </div>
          <div>
            <h1 className="font-display font-bold text-lg text-olive-900">Import CSV / JSON idempotent</h1>
            <p className="text-xs text-olive-700/75">Upsert sur externalId · rapport des lignes rejetées</p>
          </div>
        </div>

        {errorMsg && <div className="px-3 py-2 rounded-lg bg-[#fee2e2] text-[#b33a3a] text-xs font-medium">{errorMsg}</div>}

        <div className="flex flex-wrap gap-2">
          <select value={importType} onChange={(e) => setImportType(e.target.value as ImportType)} className="h-9 px-3 rounded-lg glass-strong text-xs text-olive-900">
            <option value="ORCHARDS">ORCHARDS</option>
            <option value="WORKERS">WORKERS</option>
            <option value="VEHICLES">VEHICLES</option>
            <option value="PRESSES">PRESSES</option>
            <option value="MATURITY_READINGS">MATURITY_READINGS</option>
            <option value="WEATHER_OBSERVATIONS">WEATHER_OBSERVATIONS</option>
            <option value="PRESS_CAPACITY">PRESS_CAPACITY</option>
          </select>
          <input type="file" accept=".csv,.json" onChange={(e) => setFile(e.target.files?.[0] ?? null)} className="text-xs text-olive-800" />
          <button
            onClick={() => uploadMutation.mutate()}
            disabled={!file || uploadMutation.isPending}
            className="px-4 h-9 rounded-lg bg-olive-600 hover:bg-olive-700 text-cream-50 text-xs font-semibold disabled:opacity-50"
          >
            Importer
          </button>
        </div>

        <div className="space-y-3">
          {importsQ.data?.content.map((j) => (
            <GlassCard key={j.id} variant="subtle" className="p-3.5 space-y-1.5">
              <div className="flex justify-between text-xs font-semibold">
                <span>{j.type} — {j.sourceFile}</span>
                <span className="text-olive-800">{j.status}</span>
              </div>
              <p className="text-xs text-olive-700 tabnum">
                Lues : {j.readCount} · Insérées : <strong>{j.insertedCount}</strong> · Mises à jour : <strong>{j.updatedCount}</strong> · Rejetées : <strong className="text-[#b33a3a]">{j.rejectedCount}</strong>
              </p>
              {j.errors.length > 0 && (
                <ul className="text-[11px] text-[#b33a3a] list-disc ml-4">
                  {j.errors.slice(0, 5).map((e, idx) => (
                    <li key={idx}>Ligne {e.row} ({e.field}) : {e.message}</li>
                  ))}
                </ul>
              )}
            </GlassCard>
          ))}
        </div>
      </GlassCard>

      {/* Bloc Comptes et Rôles */}
      <GlassCard className="p-5 lg:col-span-6 space-y-3">
        <div className="flex items-center gap-2.5">
          <div className="h-9 w-9 rounded-lg bg-olive-600/15 text-olive-700 grid place-items-center">
            <Users className="h-4 w-4" />
          </div>
          <h2 className="font-display font-bold text-lg text-olive-900">
            Comptes utilisateurs ({usersQ.data?.totalElements ?? 0})
          </h2>
        </div>

        <div className="space-y-2">
          {usersQ.data?.content.map((u) => (
            <div key={u.id} className="p-3 rounded-xl glass-subtle flex flex-wrap items-center justify-between gap-2">
              <div className="min-w-0">
                <div className="font-semibold text-sm text-olive-900 truncate">{u.fullName}</div>
                <div className="text-xs text-olive-700/75 truncate">{u.email}</div>
              </div>
              <div className="flex items-center gap-2">
                <select
                  value={u.role}
                  onChange={(e) => roleMutation.mutate({ id: u.id, role: e.target.value as Role })}
                  className="h-8 px-2 rounded-lg glass-strong text-xs text-olive-900"
                >
                  {(['FARMER', 'COLLECTION_MANAGER', 'TEAM_LEADER', 'LOGISTICS_MANAGER', 'PRESS_MANAGER', 'ADMIN'] as Role[]).map((r) => (
                    <option key={r} value={r}>{r}</option>
                  ))}
                </select>
                <button
                  onClick={() => activeMutation.mutate({ id: u.id, active: !u.active })}
                  className={`px-2.5 h-8 rounded-lg text-xs font-medium ${u.active ? 'bg-[#d1fae5] text-[#1b5e20]' : 'bg-[#fee2e2] text-[#b33a3a]'}`}
                >
                  {u.active ? 'Actif' : 'Désactivé'}
                </button>
              </div>
            </div>
          ))}
        </div>
      </GlassCard>
    </div>
  );
}
