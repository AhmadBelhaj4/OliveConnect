# OliveConnect — Contrat d'API v1 (contracts-v1)

Ce document décrit les spécifications des endpoints REST, les rôles autorisés et les structures des requêtes/réponses.

## 1. Rôles et Droits

- `FARMER` : Gestion de ses propres vergers et soumission de demandes de récolte.
- `COLLECTION_MANAGER` : Consultation et approbation des demandes de récolte, planification des tournées.
- `TEAM_LEADER` : Suivi d'exécution terrain des tournées et arrêts.
- `LOGISTICS_MANAGER` : Gestion et suivi des ressources matérielles et véhicules.
- `PRESS_MANAGER` : Réception des récoltes au pressoir et contrôle qualité.
- `ADMIN` : Administration globale et gestion des utilisateurs.

## 2. Authentification et Sécurité

- **Type de jeton** : JWT HS256 (`Bearer <token>`).
- **Durée de validité** : 7200 secondes (2 heures).
- **Contrôle d'accès** :
  - *Niveau 1 (Rôle)* : Si le rôle n'est pas autorisé, l'API renvoie **403 Forbidden**.
  - *Niveau 2 (Propriété)* : Si un agriculteur tente d'accéder à un verger ou une demande qui ne lui appartient pas, l'API renvoie **404 Not Found** (pour éviter la divulgation d'identifiants).
  - *Token absent ou invalide* : **401 Unauthorized**.

---

## 3. Spécification des Endpoints

### 3.1 Authentification (`/api/v1/auth`)

#### `POST /api/v1/auth/register`
- **Accès** : Public.
- **Requête** :
  ```json
  {
    "email": "agriculteur@example.com",
    "password": "Password123!",
    "fullName": "Ahmed Ben Ali",
    "phone": "+21620000000",
    "role": "FARMER"
  }
  ```
- **Réponse (201 Created)** :
  ```json
  {
    "token": "eyJhbGciOi...",
    "tokenType": "Bearer",
    "expiresIn": 7200,
    "userId": "673cf1...",
    "email": "agriculteur@example.com",
    "fullName": "Ahmed Ben Ali",
    "role": "FARMER"
  }
  ```

#### `POST /api/v1/auth/login`
- **Accès** : Public.
- **Requête** :
  ```json
  {
    "email": "agriculteur@example.com",
    "password": "Password123!"
  }
  ```
- **Réponse (200 OK)** : même structure que `register`.

#### `GET /api/v1/auth/me`
- **Accès** : Authentifié.
- **Réponse (200 OK)** : Profil de l'utilisateur connecté.

---

### 3.2 Vergers (`/api/v1/orchards`)

#### `POST /api/v1/orchards`
- **Accès** : Rôle `FARMER`.
- **Requête** :
  ```json
  {
    "name": "Verger Nord - Chemlali",
    "surfaceHectares": 4.5,
    "treeCount": 180,
    "oliveVariety": "Chemlali",
    "location": {
      "latitude": 34.85,
      "longitude": 10.75
    },
    "maturityStatus": "APPROACHING"
  }
  ```
- **Réponse (201 Created)** : Détails du verger créé avec son identifiant généré.

#### `GET /api/v1/orchards`
- **Accès** : Authentifié (un agriculteur ne voit que ses vergers).
- **Réponse (200 OK)** : Liste des vergers.

#### `GET /api/v1/orchards/{id}`
- **Accès** : Authentifié (vérification stricte de propriété pour `FARMER`).
- **Réponse (200 OK)** : Détails du verger.

---

### 3.3 Demandes de récolte (`/api/v1/harvest-requests`)

#### `POST /api/v1/harvest-requests`
- **Accès** : Rôle `FARMER`.
- **Statut initial** : Toujours initialisé à `SUBMITTED`.
- **Requête** :
  ```json
  {
    "orchardId": "673cf1...",
    "type": "PLANNED",
    "estimatedQuantityKg": 2500.0,
    "desiredStartDate": "2026-11-25",
    "notes": "Récolte urgente avant pluie annoncée"
  }
  ```
- **Réponse (201 Created)** : Demande enregistrée avec le statut `SUBMITTED`.

#### `GET /api/v1/harvest-requests`
- **Accès** : Authentifié.
- **Réponse (200 OK)** : Liste des demandes.

#### `GET /api/v1/harvest-requests/{id}`
- **Accès** : Authentifié.
- **Réponse (200 OK)** : Détail de la demande.
