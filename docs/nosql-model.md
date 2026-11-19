# Modèle de Données NoSQL (MongoDB) — Spécification v1

Ce document détaille les règles d'architecture NoSQL (R1 à R6), les structures de collections et les stratégies d'indexation pour OliveConnect.

---

## 1. Règles de Conception NoSQL (R1 à R6)

- **Règle R1 (Imbrication bornée)** : Imbriquer les entités enfants lorsque leur cardinalité est strictement bornée et qu'elles partagent le cycle de vie du document parent (ex: `location` GeoJSON dans `orchards`, arrêts d'une tournée ≤ 20 stops).
- **Règle R2 (Référence pour cardinalité ouverte)** : Utiliser des références par identifiant (`String id`) pour les relations 1-à-N où N est potentiellement infini (ex: un exploitant peut posséder des dizaines de vergers et des centaines de demandes au fil des saisons).
- **Règle R3 (Indexation spatiale)** : Toute coordonnée cartographique exploitée pour le calcul de distance ou de proximité géographique doit porter un index `2dsphere` au format standard GeoJSON `[longitude, latitude]`.
- **Règle R4 (Dénormalisation ciblée de lecture)** : Pour éviter des lookups coûteux lors des requêtes fréquentes, certains identifiants pivots sont dénormalisés (ex: `farmerId` présent directement dans `harvest_requests` pour filtrer les demandes d'un agriculteur sans joindre la table des vergers).
- **Règle R5 (Traçabilité et horodatage)** : Chaque document racine porte les métadonnées de création et mise à jour (`createdAt`, `updatedAt`) pour garantir l'auditabilité.
- **Règle R6 (Contraintes d'intégrité)** : Garantir l'unicité fonctionnelle au niveau du moteur de données par des index uniques (ex: champ `email` dans la collection `users`).

---

## 2. Collections et Définitions NoSQL

### 2.1 Collection `users`
Stocke les profils utilisateurs et leurs identifiants de sécurité.
- **Index** :
  - `_id` : Clé primaire.
  - `email` : Unique (`{ "email": 1 }`).
  - `role` : Index simple pour filtrage administratif.

### 2.2 Collection `orchards`
Stocke les parcelles oléicoles et leur position géographique.
- **Index** :
  - `_id` : Clé primaire.
  - `location` : Index géospatial `2dsphere` (`{ "location": "2dsphere" }`).
  - `farmerId` : Index simple pour lister les vergers d'un exploitant.

### 2.3 Collection `harvest_requests`
Stocke les demandes de récolte formulées par les agriculteurs.
- **Index** :
  - `_id` : Clé primaire.
  - `farmerId` : Index simple.
  - `orchardId` : Index simple.
  - `status` : Index simple pour le filtrage par le responsable de collecte.
