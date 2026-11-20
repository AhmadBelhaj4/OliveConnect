import { BookOpen, ShieldCheck, TreePine, Route, Factory, HelpCircle, Truck, Users } from 'lucide-react';
import { GlassCard } from '../../components/GlassCard';

export function HelpPage() {
  const rolesGuide = [
    {
      role: 'Visiteur (Public)',
      desc: 'Accès sans compte à la carte publique des vergers et à la maturité des parcelles visibles.',
      icon: TreePine,
    },
    {
      role: 'Agriculteur (FARMER)',
      desc: 'Déclaration de ses parcelles oléicoles, dépôt de demandes de récolte (PLANNED / URGENT), suivi en temps réel et signalement d’alertes terrain.',
      icon: TreePine,
    },
    {
      role: 'Responsable de collecte (COLLECTION_MANAGER)',
      desc: 'Revue et arbitrage des demandes de récolte, planification des tournées, affectation des chefs d’équipe et gestion des alertes.',
      icon: Route,
    },
    {
      role: 'Chef d’équipe (TEAM_LEADER)',
      desc: 'Exécution terrain des tournées : pointage des arrêts, pesée réelle des récoltes et clôture des tournées.',
      icon: ShieldCheck,
    },
    {
      role: 'Gestionnaire logistique (LOGISTICS_MANAGER)',
      desc: 'Supervision du parc de véhicules, disponibilité par date et gestion du matériel et des ouvriers.',
      icon: Truck,
    },
    {
      role: 'Opérateur pressoir (PRESS_MANAGER)',
      desc: 'Enregistrement des réceptions d’olives, pesée des bennes et mise à jour de la capacité journalière de trituration.',
      icon: Factory,
    },
    {
      role: 'Administrateur (ADMIN)',
      desc: 'Gestion des comptes utilisateurs, attribution des rôles, import de données volumineuses CSV/JSON et audit sécurité.',
      icon: Users,
    },
  ];

  return (
    <div className="space-y-6 max-w-4xl mx-auto">
      <GlassCard variant="subtle" className="p-5 flex items-center gap-3">
        <div className="h-10 w-10 rounded-xl bg-olive-600 text-cream-50 grid place-items-center">
          <BookOpen className="h-5 w-5" />
        </div>
        <div>
          <h1 className="font-display font-bold text-xl sm:text-2xl text-olive-900">Guide & Aide OliveConnect</h1>
          <p className="text-xs text-olive-700/80">Plateforme de coordination oléicole de la région de Sfax</p>
        </div>
      </GlassCard>

      <GlassCard className="p-6 space-y-4">
        <h2 className="font-display font-semibold text-lg text-olive-900 flex items-center gap-2">
          <HelpCircle className="h-4 w-4 text-olive-600" /> Les 5 Étapes du Flux Oléicole
        </h2>
        <div className="grid grid-cols-1 md:grid-cols-5 gap-3 text-xs">
          <div className="p-3 rounded-lg glass-subtle space-y-1">
            <span className="font-bold text-olive-800">1. Déclaration</span>
            <p className="text-olive-700">L'agriculteur localise sa parcelle et soumet une demande de récolte.</p>
          </div>
          <div className="p-3 rounded-lg glass-subtle space-y-1">
            <span className="font-bold text-olive-800">2. Validation</span>
            <p className="text-olive-700">Le responsable de collecte approuve et intègre la demande dans une tournée.</p>
          </div>
          <div className="p-3 rounded-lg glass-subtle space-y-1">
            <span className="font-bold text-olive-800">3. Récolte</span>
            <p className="text-olive-700">Le chef d’équipe démarre la tournée, effectue les arrêts et enregistre le tonnage.</p>
          </div>
          <div className="p-3 rounded-lg glass-subtle space-y-1">
            <span className="font-bold text-olive-800">4. Trituration</span>
            <p className="text-olive-700">Le pressoir réceptionne les bennes et décharge les olives pour extraction.</p>
          </div>
          <div className="p-3 rounded-lg glass-subtle space-y-1">
            <span className="font-bold text-olive-800">5. Analyse</span>
            <p className="text-olive-700">Le dashboard présente les rendements, les délais et les taux de remplissage.</p>
          </div>
        </div>
      </GlassCard>

      <div className="space-y-3">
        <h2 className="font-display font-semibold text-base text-olive-900 px-1">
          Rôles et Permissions dans OliveConnect
        </h2>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
          {rolesGuide.map((item) => {
            const Icon = item.icon;
            return (
              <GlassCard key={item.role} variant="subtle" className="p-4 space-y-2">
                <div className="flex items-center gap-2 text-olive-900 font-semibold text-sm">
                  <Icon className="h-4 w-4 text-olive-600" />
                  <span>{item.role}</span>
                </div>
                <p className="text-xs text-olive-700 leading-relaxed">{item.desc}</p>
              </GlassCard>
            );
          })}
        </div>
      </div>
    </div>
  );
}
