# Rapport Membre C — Conception Ergonomique & Manuel Utilisateur (« Olive Field »)

## 1. Principes Ergonomiques & Charte « Olive Field »

L'interface d'OliveConnect a été pensée pour répondre aux contraintes du terrain oléicole tunisien :
- **Atmosphère Visuelle Naturelle** :
  - Nuances de vert olive (`#4a6b2a`, `#6b8e3f`), terre cuite (`#b07f1f`), crème (`#fdfcf7`) et fond de champ (`#f4f6ee`).
  - Effets de carte verre subtil (`GlassCard`) assurant une lisibilité maximale en plein soleil.
- **Accessibilité Mobile (Viewport 360 px)** :
  - Tableaux avec défilement horizontal contenu ou bascule en cartes accordéons.
  - Boutons d'action tactiles ≥ 44 px de hauteur.
  - Cartographie Leaflet adaptée au tactile avec contrôles agrandis.
- **Accompagnement à l'Accueil (Onboarding)** :
  - Modale interactive `GuidedTourModal` guidant l'utilisateur étape par étape lors de sa première visite.
  - Page `/help` documentant exhaustivement chaque profil métier.

---

## 2. Manuel Utilisateur par Rôle

### 2.1 Espace Agriculteur (`FARMER`)
1. **Mes vergers (`/farmer/orchards`)** :
   - Formulaire de création avec géolocalisation (« Ma position »).
   - Renseignement de la variété (Chemlali, Chetoui, etc.) et de la surface.
2. **Mes demandes (`/farmer/requests`)** :
   - Dépôt d'une demande planifiée ou urgente.
   - Suivi visuel du badge de statut (`SUBMITTED`, `APPROVED`, `SCHEDULED`, `COMPLETED`).
3. **Signalement d'alertes & Suivi (`/farmer/tracking`)** :
   - Signalement d'une anomalie terrain (mouche de l'olivier, gel, pluie prématurée).
   - Flux de notifications en temps réel des étapes de collecte.

### 2.2 Espace Responsable de Collecte (`COLLECTION_MANAGER`)
1. **File des demandes (`/collection/requests`)** :
   - Examen des demandes entrantes avec actions « Approuver » ou « Rejeter » avec motif obligatoire.
2. **Planificateur de tournées (`/collection/tours`)** :
   - Choix de la date et affectation d'un véhicule disponible et d'un chef d'équipe.
   - Sélection et ordonnancement des arrêts de collecte.
   - Détection proactive des conflits d'affectation (alerte `409 Conflict`).
3. **File des alertes (`/collection/alerts`)** :
   - Triage par niveau de gravité (`CRITICAL`, `HIGH`, `MEDIUM`, `LOW`).
   - Workflow d'acquittement et de résolution avec rapport de clôture.

### 2.3 Espace Chef d’Équipe (`TEAM_LEADER`)
1. **Mes tournées (`/leader/tours`)** :
   - Démarrage de la tournée le matin.
   - Check-list des arrêts chez les agriculteurs.
   - Saisie du nombre de bennes et du poids net mesuré sur place.
   - Clôture et validation de la tournée.

### 2.4 Espace Opérateur Pressoir (`PRESS_MANAGER`)
1. **Pressoir (`/press/receptions`)** :
   - Enregistrement des réceptions de chargements.
   - Saisie du poids net d'olives et attribution d'une note de qualité (A, B, C).
   - Suivi en temps réel de la jauge de capacité journalière de broyage.

### 2.5 Espace Administrateur (`ADMIN`)
1. **Comptes & Imports (`/admin/users`)** :
   - Gestion des comptes et modification des rôles d'accès.
   - Activation ou suspension immédiate d'un compte.
   - Module d'upload de fichiers volumineux CSV / JSON avec rapport détaillé d'erreurs et compteurs d'idempotence.
