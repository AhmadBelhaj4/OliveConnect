# Rapport Membre A — Architecture Backend, Sécurité & Imports Idempotents

## 1. Architecture Backend en 4 Couches & Façades Java

L'architecture backend d'OliveConnect respecte strictement le principe de séparation des responsabilités en couches étanches :
1. **Couche Web / API (`controller/`)** : Endpoints REST (`/api/v1/*`), validation bean validation (`@Valid`), extraction des contextes de sécurité (`@AuthenticationPrincipal`).
2. **Couche Service Métier (`service/`)** : Logique métier, machine à états (`HarvestStatusPolicy`), transactions, coordination entre modules.
3. **Couche Façades (`common/facade/`)** : Découplage inter-domaines :
   - `HarvestRequestFacade` : Exposition contrôlée des demandes de récolte pour les tournées sans dépendance cyclique.
   - `NotificationFacade` : Déclenchement asynchrone des alertes et notifications aux acteurs.
   - `TourFacade` : Orchestration des tournées et arrêts.
   - `CacheInvalidator` : Éviction granulaire du cache Redis lors des mutations.
4. **Couche Persistance (`repository/` & `document/`)** : Documents MongoDB avec indexation géospatiale (`2dsphere`) et index composés.

---

## 2. Sécurité à Deux Niveaux & Rate-Limiting

### 2.1 Authentification & Autorisation
- **JWT HS256 Stateless** : Token signé avec une clé secrète ≥ 32 octets, incluant `userId`, `email` et `role`.
- **Matrice des Rôles** : Contrôles stricts `@PreAuthorize("hasRole(...)")` sur chaque endpoint. Vérifié par `SecurityMatrixTest` (exporté dans `target/security-matrix.md`).
- **Codes d'erreur stricts** :
  - `401 UNAUTHENTICATED` : Absence de token ou token invalide.
  - `403 ACCESS_DENIED` : Rôle insuffisant pour l'endpoint demandé (journalisé dans `audit_events`).
  - `404 NOT_FOUND` : Ressource introuvable ou n'appartenant pas à l'agriculteur (isolation des données).

### 2.2 Durcissement Rate-Limit (429 Redis)
- **Composant `LoginRateLimiter`** : Compte les échecs de connexion sur la clé Redis `oliveconnect:rl:login:<ip>+<email>` (fenêtre glissante de 15 minutes, seuil de 5 échecs).
- **Résilience** : En cas de panne de Redis, le service bascule en mode dégradé (journalise un avertissement et permet la tentative sans bloquer les utilisateurs légitimes).

---

## 3. Mécanisme d'Import Idempotent (`externalId`)

L'endpoint `POST /api/v1/admin/imports` permet l'ingestion de jeux de données CSV ou JSON pour 7 types de ressources (`ORCHARDS`, `WORKERS`, `VEHICLES`, `PRESSES`, `MATURITY_READINGS`, `WEATHER_OBSERVATIONS`, `PRESS_CAPACITY`).

- **Index Unique Sparse** : Créé sur `externalId` pour chaque collection.
- **Opération Upsert Atomique** : `mongo.upsert(...)` insère la ligne si `externalId` est nouveau (`insertedCount++`), ou met à jour les champs si l'identifiant existe déjà (`updatedCount++`).
- **Idempotence (Scénario E)** : Un double import du même fichier produit **0 doublon**, garantissant l'intégrité de la base.
- **Gestion des Rejets** : Les lignes invalides sont isolées avec numéro de ligne, champ et message d'erreur (limité à 100 erreurs dans `ImportJobDocument`).
- **Import Automatisé `@Scheduled`** : `ScheduledInboxImporter` surveille `sample-data/inbox/`, importe les fichiers selon la convention `<TYPE>_*.csv` et les archive dans `sample-data/processed/`.
