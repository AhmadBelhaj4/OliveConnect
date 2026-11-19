import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { KeyRound, LogIn } from 'lucide-react';
import { ApiClientError } from '../../api/http';
import { GlassCard } from '../../components/GlassCard';
import { SEEDED_ACCOUNTS } from '../../mocks/handlers';
import { useAuth } from './AuthContext';

export function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState('farmer@example.test');
  const [password, setPassword] = useState('Secret#123');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      const user = await login({ email, password });
      navigate(user.role === 'FARMER' ? '/farmer/orchards' : '/map');
    } catch (err) {
      setError(err instanceof ApiClientError ? err.apiError.message : 'Erreur de connexion');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="max-w-md mx-auto my-8">
      <GlassCard variant="strong" className="p-6 space-y-5">
        <div className="flex items-center gap-3">
          <div className="h-11 w-11 rounded-xl bg-olive-600 text-cream-50 grid place-items-center font-display font-bold text-base">
            OC
          </div>
          <div>
            <h1 className="font-display font-bold text-xl text-olive-900">Connexion OliveConnect</h1>
            <p className="text-xs text-olive-700/80">Coopérative oléicole de Sfax</p>
          </div>
        </div>

        {error && (
          <div className="px-3.5 py-2.5 rounded-lg bg-[#fee2e2] text-[#b33a3a] text-sm font-medium">{error}</div>
        )}

        <div className="p-3 rounded-xl glass-subtle">
          <label htmlFor="quick-role" className="flex items-center gap-1.5 text-xs font-medium text-olive-800 mb-1.5">
            <KeyRound className="h-3.5 w-3.5" /> Compte de démo (seed B6 / MSW)
          </label>
          <select
            id="quick-role"
            value={email}
            onChange={(e) => {
              setEmail(e.target.value);
              setPassword('Secret#123');
            }}
            className="w-full h-9 px-3 rounded-lg glass-strong text-xs text-olive-900 outline-none"
          >
            {SEEDED_ACCOUNTS.map((acc) => (
              <option key={acc.email} value={acc.email}>
                {acc.label}
              </option>
            ))}
          </select>
        </div>

        <form onSubmit={handleSubmit} className="space-y-3.5">
          <div>
            <label htmlFor="email" className="block text-xs font-medium text-olive-800 mb-1">Email</label>
            <input
              id="email"
              type="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="w-full h-10 px-3 rounded-lg glass-strong text-sm text-olive-900 outline-none"
            />
          </div>
          <div>
            <label htmlFor="password" className="block text-xs font-medium text-olive-800 mb-1">Mot de passe</label>
            <input
              id="password"
              type="password"
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="w-full h-10 px-3 rounded-lg glass-strong text-sm text-olive-900 outline-none"
            />
          </div>
          <button
            type="submit"
            disabled={submitting}
            className="w-full h-10 inline-flex items-center justify-center gap-2 rounded-lg bg-olive-600 hover:bg-olive-700 text-cream-50 text-sm font-semibold transition-colors disabled:opacity-60 cursor-pointer"
          >
            <LogIn className="h-4 w-4" />
            {submitting ? 'Connexion…' : 'Se connecter'}
          </button>
        </form>
      </GlassCard>
    </div>
  );
}
