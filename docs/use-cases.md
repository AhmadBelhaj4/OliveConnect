# Cas d'Utilisation (Use Cases - Semaine 1)

## UC1 : Authentification de l'Agriculteur
- **Acteur principal** : Agriculteur (`FARMER`).
- **Précondition** : Aucune (accès public).
- **Scénario nominal** :
  1. L'agriculteur soumet ses identifiants (`POST /api/v1/auth/login`).
  2. Le système vérifie l'existence et valide le mot de passe (BCrypt).
  3. Le système renvoie un token JWT valide avec ses informations de rôle.

## UC2 : Création d'un Verger
- **Acteur principal** : Agriculteur (`FARMER`).
- **Précondition** : Utilisateur authentifié avec rôle `FARMER`.
- **Scénario nominal** :
  1. L'agriculteur renseigne le formulaire (nom, surface, variété, coordonnées GPS).
  2. L'agriculteur soumet la requête (`POST /api/v1/orchards`).
  3. Le système injecte le `farmerId` extrait du JWT et enregistre le document avec indexation `2dsphere`.
  4. Le système renvoie le verger créé (201 Created).

## UC3 : Soumission d'une Demande de Récolte
- **Acteur principal** : Agriculteur (`FARMER`).
- **Précondition** : Agriculteur authentifié possédant au moins un verger enregistré.
- **Scénario nominal** :
  1. L'agriculteur sélectionne son verger et indique le type de récolte, la quantité estimée et la date souhaitée.
  2. L'agriculteur soumet la requête (`POST /api/v1/harvest-requests`).
  3. Le système vérifie la propriété du verger (renvoie 404 si non possédé).
  4. Le système enregistre la demande avec le statut `SUBMITTED`.
  5. Le système confirme la création (201 Created).
