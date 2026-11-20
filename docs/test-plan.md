# Plan de Tests de Bout en Bout — Validation Scénarios A à E

## 1. Grille de Couverture des Scénarios

| Scénario | Intitulé & Flux | Acteurs Impliqués | Critère de Réussite | Statut |
|---|---|---|---|---|
| **Scénario A** | **Flux Complet F1 + F3**<br>Déclaration verger → Demande récolte → Approbation → Tournée planifiée → Collecte terrain → Pesée pressoir | Agriculteur, Resp. Collecte, Chef d'équipe, Opérateur Pressoir | Statut de la demande passe à `COMPLETED`, poids pesé reporté, notification reçue par l'agriculteur. | **VALIDÉ** |
| **Scénario B** | **Signalement & Traitement Alerte (F2)**<br>Alerte `PEST` ou `RAIN` signalée sur parcelle → Prise en compte → Résolution avec note | Agriculteur, Resp. Collecte | Alerte passe de `OPEN` → `ACKNOWLEDGED` → `RESOLVED`, notification transmise. | **VALIDÉ** |
| **Scénario C** | **Sécurité & Contrôle d'Accès**<br>Requête sans token, accès avec mauvais rôle, tentative de lecture de données privées d'un tiers, brute-force login | Visiteur, Utilisateur non autorisé, Attaquant | Réponses strictes : `401 Unauthorized`, `403 Forbidden`, `404 Not Found`, `429 Rate Limited` sur le 6e échec. | **VALIDÉ** |
| **Scénario D** | **Détection de Conflit d'Affectation (409)**<br>Planification d'un même véhicule pour deux tournées distinctes sur la même date | Resp. Collecte | Rejet avec code `409 Conflict` et message explicite `RESOURCE_CONFLICT` affiché à l'écran. | **VALIDÉ** |
| **Scénario E** | **Import de Données Idempotent**<br>Import d'un fichier CSV contenant des lignes valides et invalides, puis ré-import du même fichier | Administrateur | 1er import : rejet des lignes erronées avec rapport sans arrêt total ; 2e import : `0` doublon créé (`insertedCount = 0`, `updatedCount = 1`). | **VALIDÉ** |

---

## 2. Tests de Résilience Système

### 2.1 Résilience Panne Redis
- **Test** : Arrêt forcé du conteneur Redis (`docker stop oliveconnect-redis`).
- **Comportement constaté** : L'accès à la carte `/api/v1/orchards/map` et au tableau de bord `/api/v1/dashboard/summary` bascule de manière transparente sur MongoDB sans erreur applicative (`HTTP 200 OK`).

### 2.2 Sauvegarde et Restauration
- **Test** :
  1. Sauvegarde d'archive avec `backup-mongo.sh`.
  2. Suppression contrôlée des vergers dans MongoDB (`db.orchards.deleteMany({})`).
  3. Restauration via `mongorestore --drop`.
- **Résultat** : L'ensemble des vergers, index géospatiaux et documents sont restaurés à l'identique.
