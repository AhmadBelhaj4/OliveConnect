# Rapport Membre B — Modélisation NoSQL, Agrégations, Cache Redis & Sauvegardes

## 1. Modélisation NoSQL & Indexation

### 1.1 Collections & Relations
- `orchards` : Stockage GeoJSON `Point [longitude, latitude]`, surface, variété, statut de maturité. Index `2dsphere` pour requêtes spatiales bounding box.
- `harvest_requests` : Suivi du cycle de vie de la demande de récolte (`SUBMITTED` → `APPROVED` → `SCHEDULED` → `COMPLETED`). Index composé `{status: 1, preferredDate: 1, createdAt: -1}`.
- `tours` : Tournées de collecte avec arrêts ordonnés, véhicule et équipe affectés. Index unique conflictuel pour empêcher la double affectation le même jour.
- `press_receptions` : Enregistrement des pesées de bennes au pressoir avec contrainte d'unicité `(tourId, binId)`.

---

## 2. Tableau de Bord & Agrégations MongoDB

Quatre pipelines d'agrégation optimisés via `MongoTemplate` :
1. **Summary** (`/dashboard/summary`) :
   - Calcule le total récolté (somme de `actualQuantityKg` sur les demandes `COMPLETED`).
   - Calcule le total reçu au pressoir (somme de `weightKg`).
   - Compte les tournées terminées et les alertes ouvertes.
   - Fournit la distribution exhaustive sur tous les 7 statuts de demandes.
2. **Yield By Orchard** (`/dashboard/yield-by-orchard`) :
   - Groupe les récoltes par verger avec tri décroissant sur le tonnage.
3. **Collection Delays** (`/dashboard/collection-delays`) :
   - Évalue les temps d'attente moyens :
     - *Délai de planification* : `scheduledAt - requestedAt` en heures.
     - *Délai d'exécution* : `completedAt - scheduledAt` en heures.
4. **Bin Fill Rates** (`/dashboard/bin-fill-rates`) :
   - Jointure `$lookup` avec la collection `bins` pour calculer le ratio `weightKg / bin.capacityKg`.

---

## 3. Stratégie de Cache Redis (Cache-Aside)

- **TTL Différenciés** :
  - Carte des vergers : `45 secondes`, clé intégrant le scope utilisateur (`PUBLIC`, `FARMER_<id>`, `STAFF`) et le hash SHA-256 des filtres.
  - Dashboard : `90 secondes`, clé `oliveconnect:dashboard:<type>:<from>_<to>`.
- **Invalidation Événementielle** :
  - `RedisCacheInvalidator` écoute les mutations sur les vergers, demandes, tournées et pressoirs pour invalider immédiatement les motifs `oliveconnect:map:*` et `oliveconnect:dashboard:*`.
- **Résilience Panne (Mode Dégradé)** :
  - `CacheService` intercepte toute exception Redis (connexion perdue, timeout) pour requêter directement MongoDB sans lever d'erreur HTTP 500 (`200 OK` garanti).

---

## 4. Politique de Sauvegarde & Restauration

- **Script `backup-mongo.sh`** :
  - Exécute `mongodump --archive --gzip` quotidiennement avec horodatage UTC.
  - Rotation automatique avec purge des archives antérieures à 7 jours.
- **Restauration `mongorestore`** :
  - Testée et validée avec bascule et purge (`--drop`), restaurant l'intégralité des documents et index à l'état initial.
