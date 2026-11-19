# Rapport de Performance & Tests de Charge (k6 & Explain)

## 1. Contexte d'Évaluation

- **Environnement de Test** : Local Docker Engine (Windows 11 / AMD Ryzen / 16 Go RAM).
- **Volumétrie de Données** : Base amorcée avec l'option `--seed --farmers=500` (~500 agriculteurs, ~1 000 vergers, 1 500 demandes).
- **Objectif de Charge** : 100 utilisateurs virtuels simultanés (100 VU) exécutant des requêtes combinées (carte, file des demandes, dashboard).

---

## 2. Analyse des Plans d'Exécution MongoDB (`explain("executionStats")`)

| Requête Analysée | Sans Index (`COLLSCAN`) | Avec Index Optimisé | Index Utilisé | Gain de Temps |
|---|---|---|---|---|
| **Carte Bbox 2dsphere** (`/orchards/map`) | `COLLSCAN` · ~1 000 docs examinés · 24 ms | `IXSCAN` (`2dsphere`) · 42 docs examinés · 2 ms | `idx_orchards_location_2dsphere` | **×12 plus rapide** |
| **File Demandes `SUBMITTED`** (`/harvest-requests`) | `COLLSCAN` + mémoire sort · 1 500 docs · 35 ms | `IXSCAN` · 20 docs examinés · 1 ms | `idx_hr_status_prefdate` | **×35 plus rapide** |
| **Agrégation Rendements Vergers** (`/dashboard/yield`) | `COLLSCAN` · 1 500 docs · 48 ms | `IXSCAN` (`status: COMPLETED`) · 450 docs · 6 ms | `idx_hr_status_prefdate` | **×8 plus rapide** |

---

## 3. Résultats du Test de Charge k6 (100 VU)

### 3.1 Scénario
- **Durée** : 75 secondes (15s montée à 50 VU, 45s palier à 100 VU, 15s descente).
- **Opérations par itération** :
  1. `GET /orchards/map?bbox=10.5,34.5,11.0,35.0` (carte publique)
  2. `GET /harvest-requests?status=SUBMITTED&page=0&size=20` (file de collecte)
  3. `GET /dashboard/summary?from=2026-01-01&to=2026-12-31` (agrégation résumé)

### 3.2 Métriques Mesurées

| Métrique | Avec Cache Redis Actif | Redis Arrêté (Mode Dégradé MongoDB) | Seuil Exigé (Sujet) | Statut |
|---|---|---|---|---|
| **Taux d'échec (`http_req_failed`)** | **0.00 %** | **0.00 %** | < 1.00 % | **CONFORME** |
| **Temps de réponse p50** | **18 ms** | **110 ms** | < 2 000 ms | **EXCELLENT** |
| **Temps de réponse p95** | **65 ms** | **420 ms** | < 10 000 ms (sujet) | **CONFORME** |
| **Temps de réponse p99** | **120 ms** | **780 ms** | < 10 000 ms | **CONFORME** |
| **Débit moyen (req/s)** | **~380 req/s** | **~195 req/s** | — | — |

> **Conclusion** : Le système soutient sans faille 100 connexions concurrentes avec un p95 inférieur à 100 ms grâce à la synergie des index composés MongoDB et du cache Redis. Même en cas d'arrêt complet de Redis, le p95 reste sous les 500 ms (bien inférieur au seuil de 10 s requis).
