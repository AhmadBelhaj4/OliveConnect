# Exigences Fonctionnelles et Non-Fonctionnelles

## 1. Exigences Fonctionnelles
- **EF1 : Authentification sécurisée** : Inscription et connexion avec génération de jetons JWT (durée 7200 s).
- **EF2 : Gestion des vergers** : Création, géolocalisation géographique (coordonnées WGS84) et consultation des vergers par les agriculteurs.
- **EF3 : Demandes de récolte** : Soumission d'une demande de récolte par un agriculteur associée à son verger avec statut initial `SUBMITTED`.
- **EF4 : Visibilité cloisonnée** : Un agriculteur ne peut voir et modifier que ses propres vergers et demandes de récolte.

## 2. Exigences Non-Fonctionnelles
- **ENF1 - Performance** : Requêtes de consultation et de géolocalisation performantes via l'indexation MongoDB `2dsphere`.
- **ENF2 - Sécurité** : Filtrage à deux niveaux (rôle 403, propriété 404), mot de passe haché en BCrypt, secrets injectés par variables d'environnement.
- **ENF3 - Architecture** : Architecture découplée en 4 couches (Controller, Service, Repository, Document) et contrats d'échange DTO stricts.
