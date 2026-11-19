# Modèle de Données NoSQL (MongoDB)

Ce document formalise les choix de conception NoSQL retenus pour la base de données MongoDB d'OliveConnect.

## 1. Justification NoSQL & Stratégie d'Imbrication vs Référence

- **Imbrication** : Les structures composées fortement couplées et de taille bornée (comme les coordonnées géographiques `GeoLocation` / `GeoJsonPoint`) sont directement imbriquées dans le document parent.
- **Référence** : Les entités ayant leur propre cycle de vie indépendant (`users`, `orchards`, `harvest_requests`) sont stockées dans des collections distinctes et liées par des identifiants (`farmerId`, `orchardId`).
- **Sharding** : Non requis pour la volumétrie initiale (centaines d'exploitations et milliers de demandes).

## 2. Collections et Règles d'Indexation

### 2.1 Collection `users`
- Clé primaire : `_id` (ObjectId hexadécimal).
- Index unique : `{ "email": 1 }` (garantie d'unicité à l'inscription).

### 2.2 Collection `orchards`
- Clé primaire : `_id`.
- Index spatial : `{ "location": "2dsphere" }` (permettant les requêtes géographiques `$nearSphere` et `geoWithin`).
- Index secondaire : `{ "farmerId": 1 }` (optimisation de la recherche par exploitant).

### 2.3 Collection `harvest_requests`
- Clé primaire : `_id`.
- Index secondaire : `{ "farmerId": 1 }` et `{ "status": 1 }` (filtrage rapide des demandes en cours).
- Index de liaison : `{ "orchardId": 1 }`.
