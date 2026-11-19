# Modèle de données NoSQL (MongoDB) — OliveConnect

## 1. Règles de transformation UML → NoSQL (R1 à R6)

| Code | Situation dans le diagramme de classes (UML) | Transformation MongoDB retenue | Exemple dans OliveConnect |
|------|----------------------------------------------|--------------------------------|---------------------------|
| **R1** | Classe entité avec cycle de vie indépendant | Une **collection** dédiée ; `_id` (ObjectId 24 hex) exposé en `id` dans les DTO | `User` → `users`, `Orchard` → `orchards` |
| **R2** | Association `1..*` vers une entité indépendante ou partagée | **Référence** par identifiant (`ObjectId`) côté « plusieurs » | `Orchard.farmerId` → `users._id`, `HarvestRequest.orchardId` → `orchards._id` |
| **R3** | Composition `1..*` **petite, bornée** (`≤ 20`) et **toujours lue/écrite avec son parent** | **Imbrication** (tableau de sous-documents dans le parent) | `Tour.stops` (maximum 20 arrêts par tournée) |
| **R4** | Association `1..*` **non bornée** (historique qui grandit indéfiniment) | **Collection séparée** avec référence + index composé `{parentId, date desc}` (jamais d'imbrication) | `maturity_readings`, `weather_observations`, `notifications`, `audit_events` |
| **R5** | Association `*..*` bornée avec besoin de requête d'appartenance (conflits par jour) | Tableau d'identifiants (`vehicleIds`, `workerIds`, `binIds`) + **index multiclé** sur `{tourDate, status, ...}` | `Tour.vehicleIds` (≤ 10), `workerIds` (≤ 30), `binIds` (≤ 20) |
| **R6** | Attribut d'une entité référencée lu très souvent dans des listes / agrégations et quasi-immuable | **Duplication contrôlée** (snapshot à la création), la collection d'origine restant la source de vérité | `orchardName` copié dans `harvest_requests` et `tours.stops` |

## 2. Imbrication vs Référence — Tableau justificatif complet

| Relation | Choix | Cardinalité / Borne | Pattern d'accès | Justification |
|----------|-------|---------------------|-----------------|---------------|
| `User` ↔ `Orchard` | **Référence** (`farmerId`) | 1 à N (quelques dizaines) | Vergers lus sans le compte (carte publique, tournées) | Cycle de vie distinct ; contrôle de propriété via index `{farmerId}` |
| `Orchard` ↔ `HarvestRequest` | **Référence** (`orchardId`) + **duplication contrôlée** (`orchardName`) | 1 à N au fil des saisons | File du responsable triée par statut/date ; historique par verger | Croissance sur plusieurs campagnes ; `orchardName` évite un `$lookup` sur chaque ligne de la file |
| `Tour` ↔ `TourStop` | **Imbrication** (`stops[]`) | **1 à 20 max** (borne métier §5.5) | On affiche et met à jour une tournée avec tous ses arrêts | Document < 16 Ko (très loin de la limite BSON de 16 Mo) ; lecture/écriture atomique d'une tournée |
| `Tour` ↔ `Vehicle / Worker / Bin` | **Référence par tableaux d'ID** | Borné : ≤ 10 véhicules, ≤ 30 ouvriers, ≤ 20 bennes | Vérification de conflit le même jour + fiche ressource indépendante | Les ressources ont leur propre écran CRUD (`/resources/{kind}`) et survivent aux tournées |
| `Orchard` ↔ `MaturityReading` | **Collection séparée** (`maturity_readings`) | **Non bornée** (relevés périodiques / imports CSV) | Dernier état copié dans `Orchard.maturityStatus` ; historique lu à part | Règle §3.2 : *on n'imbrique jamais un historique qui grandit* |
| `PressReception` ↔ `Tour / Bin / Press` | **Référence** (`tourId`, `binId`, `pressId`) | 1 réception par couple `(tourId, binId)` | Saisie au pressoir + agrégations du dashboard | Unicité garantie par index unique composé `{tourId, binId}` |

## 3. Catalogue des collections et structure des documents

| Collection | Propriétaire | Clé primaire | Champs principaux & types BSON |
|------------|--------------|--------------|--------------------------------|
| `users` | A | `_id` (ObjectId) | `fullName`, `email` (unique, normalisé), `passwordHash` (BCrypt), `phone`, `role`, `active`, `createdAt`, `updatedAt` |
| `orchards` | A | `_id` | `farmerId` (ObjectId), `name`, `areaHectares` (Double), `oliveVariety`, `location` (GeoJSON Point `[lon, lat]`), `maturityStatus`, `publicVisibility`, `externalId`, `createdAt`, `updatedAt` |
| `harvest_requests` | A | `_id` | `farmerId`, `orchardId`, `orchardName`, `type`, `urgencyReason`, `status`, `requestedAt`, `preferredDate`, `estimatedQuantityKg` (Decimal128), `actualQuantityKg` (Decimal128), `reviewedBy`, `reviewNote`, `scheduledTourId`, `scheduledAt`, `completedAt`, `createdAt`, `updatedAt` |
| `field_alerts` | A | `_id` | `farmerId`, `orchardId`, `type`, `description`, `location` (GeoJSON Point), `priority`, `status`, `createdAt`, `acknowledgedAt`, `resolvedAt`, `resolutionNote` |
| `notifications` | A | `_id` | `userId`, `type`, `message`, `entityType`, `entityId`, `readAt`, `createdAt` |
| `audit_events` | A | `_id` | `actorId`, `action`, `entityType`, `entityId`, `details`, `createdAt` |
| `import_jobs` | A | `_id` | `type`, `sourceFile`, `status`, `startedAt`, `finishedAt`, `readCount`, `insertedCount`, `updatedCount`, `rejectedCount`, `errors[]` |
| `workers` | B | `_id` | `fullName`, `phone`, `skills[]`, `status`, `externalId` |
| `vehicles` | B | `_id` | `type`, `registration`, `capacityKg` (Int32), `status`, `externalId` |
| `bins` | B | `_id` | `code`, `capacityKg` (Int32), `status`, `externalId` |
| `equipment` | B | `_id` | `type`, `label`, `quantity` (Int32), `status`, `externalId` |
| `tours` | B | `_id` | `tourDate`, `status`, `teamLeaderId`, `vehicleIds[]`, `workerIds[]`, `binIds[]`, `stops[]` (imbriqués), `startedAt`, `completedAt`, `createdAt`, `updatedAt` |
| `presses` | B | `_id` | `name`, `location` (GeoJSON Point), `dailyCapacityKg`, `availableCapacityKg`, `externalId` |
| `press_receptions` | B | `_id` | `tourId`, `binId`, `pressId`, `receivedAt`, `weightKg` (Decimal128), `qualityGrade`, `notes`, `recordedBy` |
| `maturity_readings` | B/A | `_id` | `orchardId`, `maturityStatus`, `observedAt`, `externalId` |
| `weather_observations` | B/A | `_id` | `location` (GeoJSON Point), `temperatureC`, `rainfallMm`, `observedAt`, `externalId` |

> **GeoJSON obligatoire** : `"location": { "type": "Point", "coordinates": [longitude, latitude] }` — Sfax ≈ `[10.76, 34.73]`.

## 4. Index versionnés (créés au démarrage dans `MongoIndexConfig`)

| Collection | Nom de l'index | Définition des clés | Options | Requête / règle couverte |
|------------|----------------|---------------------|---------|--------------------------|
| `users` | `idx_users_email_uq` | `{ email: 1 }` | `unique: true` | Connexion, doublon d'inscription → 409 |
| `orchards` | `idx_orchards_location_2dsphere` | `{ location: "2dsphere" }` | — | Carte `GET /orchards/map?bbox=...` (< 10 s) |
| `orchards` | `idx_orchards_farmer` | `{ farmerId: 1, createdAt: -1 }` | — | `GET /orchards/mine` |
| `harvest_requests` | `idx_hr_farmer_created` | `{ farmerId: 1, createdAt: -1 }` | — | `GET /harvest-requests/mine` |
| `harvest_requests` | `idx_hr_status_prefdate` | `{ status: 1, preferredDate: 1, createdAt: -1 }` | — | File du responsable (`status`, `from`, `to`) |
| `harvest_requests` | `idx_hr_orchard_status` | `{ orchardId: 1, status: 1 }` | — | Statut actif d'un verger pour la carte & dashboard |
| `field_alerts` | `idx_alerts_status_prio_created` | `{ status: 1, priority: 1, createdAt: -1 }` | — | File d'alertes du responsable (tâche A9) |
| `tours` | `idx_tours_date_status` | `{ tourDate: 1, status: 1 }` | — | Détection de conflits de ressources le même jour |
| `tours` | `idx_tours_leader_date` | `{ teamLeaderId: 1, tourDate: -1 }` | — | `GET /tours` pour le chef d'équipe assigné |
| `maturity_readings` | `idx_maturity_orchard_obs` | `{ orchardId: 1, observedAt: -1 }` | — | Dernier relevé de maturité par verger |
| `press_receptions` | `idx_pr_tour_bin_uq` | `{ tourId: 1, binId: 1 }` | `unique: true` | Unicité `(tourId, binId)` → 409 en cas de doublon |
| `press_receptions` | `idx_pr_received_at` | `{ receivedAt: -1 }` | — | Historique et agrégations du dashboard par période |

## 5. Discussion sur le partitionnement (Sharding) — Pourquoi nous ne shardons pas

1. **Volumétrie réelle du besoin** : Le sujet mentionne *« plusieurs centaines d'agriculteurs »* et *« ~100 connexions simultanées »*. Même avec 2 000 agriculteurs, 5 000 vergers et 50 000 demandes sur 5 ans, la base complète pèse **moins de 200 Mo** — elle tient intégralement en RAM sur un seul nœud MongoDB (Working Set < 1 Go).
2. **Coût opérationnel injustifié** : Un cluster shardé impose au minimum un *Config Server Replica Set* (3 nœuds), `mongos` et 2 shards, ce qui alourdit le déploiement sur une seule VM universitaire sans aucun gain de latence (au contraire, les requêtes *scatter-gather* sur la carte 2dsphere seraient plus lentes).
3. **Comment nos index et Redis suffisent** : L'index `2dsphere` sur `orchards.location`, les index composés sur `harvest_requests` et le cache-aside Redis (TTL 45–90 s) permettent de viser un **p95 < 2 s** à 100 VU sous k6 (exigence < 10 s).
4. **Si le système devait passer à l'échelle nationale (évolution future)** :
   - Collections candidates : `maturity_readings`, `weather_observations` et `harvest_requests`.
   - Clé de shard retenue : `{ farmerId: "hashed" }` sur `harvest_requests` (répartition uniforme des écritures et localité des requêtes `GET /mine`), ou `{ orchardId: "hashed", observedAt: 1 }` sur `maturity_readings`.
