import { useEffect, useState } from 'react';
import { HelpCircle, X } from 'lucide-react';
import { useAuth } from '../features/auth/AuthContext';
import { GlassCard } from './GlassCard';

const STEPS = [
  { title: '1. Carte des vergers', text: 'Consultez la maturité des vergers de la région de Sfax en temps réel.' },
  { title: '2. Déclarer un verger & une demande', text: 'Utilisez « Ma position » pour localiser votre verger puis déposez une demande planifiée ou urgente.' },
  { title: '3. Suivi & notifications', text: 'Suivez chaque étape (APPROVED → SCHEDULED → IN_PROGRESS → COMPLETED) et signalez toute alerte terrain.' },
];

export function GuidedTourModal() {
  const { user } = useAuth();
  const [open, setOpen] = useState(false);
  const [step, setStep] = useState(0);

  useEffect(() => {
    if (!user) return;
    const key = `oliveconnect_tour_done_${user.id}`;
    if (!localStorage.getItem(key)) setOpen(true);
  }, [user]);

  if (!open || !user) return null;
  const finish = () => {
    localStorage.setItem(`oliveconnect_tour_done_${user.id}`, '1');
    setOpen(false);
  };

  return (
    <div className="fixed inset-0 z-50 bg-olive-900/40 backdrop-blur-sm grid place-items-center p-4">
      <GlassCard variant="strong" className="max-w-md w-full p-6 space-y-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2 font-display font-bold text-olive-900">
            <HelpCircle className="h-5 w-5 text-olive-600" /> Visite guidée OliveConnect
          </div>
          <button onClick={finish} aria-label="Fermer"><X className="h-4 w-4 text-olive-700" /></button>
        </div>
        <div>
          <h3 className="font-semibold text-base text-olive-900">{STEPS[step].title}</h3>
          <p className="text-sm text-olive-800 mt-1">{STEPS[step].text}</p>
        </div>
        <div className="flex justify-between items-center pt-2">
          <span className="text-xs text-olive-700 tabnum">Étape {step + 1} / {STEPS.length}</span>
          <div className="flex gap-2">
            {step < STEPS.length - 1 ? (
              <button onClick={() => setStep(step + 1)} className="px-4 h-9 rounded-lg bg-olive-600 text-cream-50 text-xs font-semibold">
                Suivant
              </button>
            ) : (
              <button onClick={finish} className="px-4 h-9 rounded-lg bg-olive-600 text-cream-50 text-xs font-semibold">
                Commencer
              </button>
            )}
          </div>
        </div>
      </GlassCard>
    </div>
  );
}
