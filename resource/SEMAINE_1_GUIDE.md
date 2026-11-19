# OliveConnect — Semaine 1 : guide pas à pas (tous les membres)

> **Source** : `OliveConnect_Plan_Projet.pdf` (sections 1 à 10). Ce guide **n'invente aucun contrat** : chaque nom de champ, énumération, code d'erreur, rôle et endpoint vient du plan. En cas de doute, **le PDF (et ensuite `docs/api-contract.md` tagué `contracts-v1`) fait foi**.

**Objectif de la semaine 1** (plan §8) : *Contrat v1 gelé au jour 2 ; la pile démarre chez les trois ; première tranche verticale : connexion → verger → demande de récolte, du formulaire React jusqu'à MongoDB.*

## Périmètre strict de la Semaine 1

La Semaine 1 a trois objectifs principaux :

1. **Environnement commun :** les trois membres peuvent démarrer MongoDB, Redis, le backend et le frontend localement.
2. **Contrat API v1 :** les trois membres valident et figent les structures de données, les rôles, les endpoints et les réponses HTTP avant le développement métier.
3. **Première tranche verticale fonctionnelle :**
   - Connexion d'un agriculteur.
   - Création d'un verger.
   - Création d'une demande de récolte.
   - Enregistrement de la demande dans MongoDB avec le statut `SUBMITTED`.

### Ce qui doit être terminé vendredi

- [ ] Le dépôt Git est opérationnel pour les trois membres.
- [ ] La stack Docker démarre sur les trois machines.
- [ ] Le backend Spring Boot démarre et communique avec MongoDB.
- [ ] Le frontend React démarre.
- [ ] Le contrat API v1 est validé et tagué `contracts-v1`.
- [ ] Les fonctionnalités de connexion, création de verger et demande de récolte fonctionnent.
- [ ] La demande créée est visible dans MongoDB.
- [ ] Les tests de sécurité prévus sont exécutés.
- [ ] Chaque membre peut expliquer son travail et présenter une démonstration.

Les fonctionnalités prévues pour les semaines 2 et 3 ne sont pas des objectifs de livraison de la Semaine 1.

**Démo du vendredi** (à pouvoir montrer sur les 3 machines) :
1. `docker compose up`, backend et front démarrés ;
2. se connecter en agriculteur, créer un verger (position sur la carte), soumettre une demande ;
3. retrouver la demande en `SUBMITTED` dans MongoDB (`mongosh`) ;
4. essayer sans token → **401**, avec un mauvais rôle → **403** ;
5. ouvrir le tag `contracts-v1`.

---

## Sommaire

- [0. Avant de commencer (tous)](#0-avant-de-commencer-tous)
- [1. Planning jour par jour](#1-planning-jour-par-jour)
- [2. COMMUN — COM1 à COM4](#2-commun--com1-à-com4)
- [3. MEMBRE A — Backend cœur et sécurité (A1 → A6)](#3-membre-a--backend-cœur-et-sécurité-a1--a6)
- [4. MEMBRE B — Données, logistique, performance (B1 → B6)](#4-membre-b--données-logistique-performance-b1--b6)
- [5. MEMBRE C — Frontend, carte, déploiement (C1 → C6)](#5-membre-c--frontend-carte-déploiement-c1--c6)
- [6. Intégration et démo du vendredi](#6-intégration-et-démo-du-vendredi)
- [7. Checklists « fait » et pièges](#7-checklists-fait-et-pièges)

---

## 0. Avant de commencer (tous)

### 0.1 Outils à installer (les 3 machines)

| Outil | Version | Vérification (PowerShell) |
|---|---|---|
| Git | récent | `git --version` |
| JDK | **21** (Temurin conseillé) | `java -version` → `21.x` |
| Docker Desktop (WSL2) | récent | `docker compose version` |
| Node.js | **LTS** (≥ 20) | `node -v` ; `npm -v` |
| IDE | IntelliJ IDEA (back) / VS Code (front) | — |
| mongosh (optionnel) | — | on utilisera celui du conteneur |

> [!IMPORTANT]
> Maven n'a **pas** besoin d'être installé : on utilise le wrapper `mvnw` / `mvnw.cmd` généré par Spring Initializr.

### 0.2 Règles non négociables (plan §10) à garder sous les yeux

- **MongoDB = source de vérité.** Redis n'est **pas** utilisé avant la semaine 3 (sauf le `CacheInvalidator` vide).
- `farmerId` vient **du token**, jamais du corps. Un controller renvoie **un DTO**, jamais un Document.
- GeoJSON = **`[longitude, latitude]`**. Instants en **UTC ISO-8601**. Listes **toujours paginées** (`size ≤ 100`) avec `PageResponse`.
- **Pas de secret dans Git** (`.env` ignoré). Mots de passe et tokens **jamais** dans les logs.
- **Personne ne code un endpoint avant le tag `contracts-v1`** (fin du jour 2).
- Petites PR, branche `feature/<module>-<sujet>`, `main` toujours démarrable. **Revue croisée** : A relit B, B relit C, C relit A.
- **Un seul propriétaire par package.**

### 0.3 Propriété des packages backend (évite les conflits de fusion)

| Package Java | Propriétaire |
|---|---|
| `tn.oliveconnect` (classe main), `common`, `security`, `user`, `orchard`, `harvest` | **A** |
| `tn.oliveconnect.data` (index, seed), `cache`, `resource` (puis `tour`, `press`, `map`, `dashboard` en S2/S3) | **B** |
| `tn.oliveconnect.common.facade` | **COMMUN** (figé au tag, modifié uniquement par PR `contract-change`) |
| `frontend/**`, `infra/nginx/**` | **C** |
| `infra/compose.yaml`, `.env.example`, `sample-data/` | **B** |

### 0.4 Traçabilité complète : Cahier des charges (`OliveConnect_Plan_Projet.md`) → Semaine 1

| Exigence du sujet (Cahier des charges §1) | Réponse architecturale | Ce qui est livré et prouvé dès la Semaine 1 |
|---|---|---|
| **Backend en 4 couches** (`Controller`, `Service`, `Repository/DAO`, `Document/Entity`) + injection de dépendances + Spring Data + DTO | `tn.oliveconnect.<module>.{controller,service,repository,document,dto}` ; aucun `Document` ne sort d'un controller | Modules `user`, `orchard`, `harvest` (**A1–A6**) et `resource` (**B5**) structurés en 4 couches + DTO + tests de contrat `assertExactKeys` |
| **NoSQL justifié** (imbrication vs référence, partitionnement/sharding, index) + règles UML→NoSQL | MongoDB source de vérité ; `Tour.stops` imbriqués (≤ 20) vs historiques en collections séparées ; pas de sharding justifié | `docs/class-diagram.md` (**COM3**), `docs/nosql-model.md` avec règles `R1–R6` (**B1**), index versionnés dans `MongoIndexConfig` + test Testcontainers (**B3**) |
| **Volumétrie & performance** : centaines d'agriculteurs, ~100 connexions simultanées, carte/recherche < 10 s (objectif p95 < 2 s) | Index `2dsphere` `[lon, lat]`, pagination `PageResponse` (`size ≤ 100`), cache-aside Redis (S3) | Index `2dsphere` testé avec `nearSphere` (**B3**), `CacheInvalidator` branché dès S1 (**B4**), script seed `--farmers=20` et `--farmers=500` pour k6 (**B6**) |
| **Sécurité** : authentification, droits par profil, données d'un agriculteur visibles par les seuls profils autorisés | JWT HS256 (7200 s, secret en variable d'env) + Niveau 1 Rôle (`403`) + Niveau 2 Propriété (`404`) ; `farmerId` issu du JWT | `SecurityConfig` + `JwtAuthenticationFilter` + `RestSecurityErrorHandler` (**A3–A4**), contrôle de propriété `404` sur vergers/demandes (**A5–A6**) |
| **Ergonomie mobile** : formulaires courts, géolocalisation proposée, aide en ligne et visite guidée | CSS mobile-first (`360 px`), bouton « Ma position » (`navigator.geolocation`), React Hook Form + Zod, Leaflet avec `bbox` | Layout mobile-first (**C1**), bouton « Ma position » sur création de verger (**C4**), formulaire court demande (**C5**), carte Leaflet `bbox` + attribution OSM (**C6**) |
| **Sources externes CSV/JSON & application intranet** | Import idempotent sur `externalId` (S3) ; « intranet » = zones protégées par rôle de la même app React | Champ interne `externalId` déjà prévu sur les documents (**A5, B5**), routes protégées et menus pour les 7 acteurs (**C3**), confirmation envoyée à l'encadrant (**COM1**) |
| **Hébergement serveur web, sauvegardes, supervision** | Nginx reverse-proxy `/api/` (S3), `mongodump` (S3), Actuator `/actuator/health`, Mongo/Redis non exposés | Demande VM SSH + Docker dès J1 (**COM1**), `/actuator/health` = `UP` (**A1**), ports Mongo/Redis liés à `127.0.0.1` uniquement (**B2**) |
| **Cohérence & Rapport au fil de l'eau** | Contrat JSON v1 gelé au J2 (`contracts-v1`) ; chaque membre rédige `docs/` dès que sa tâche est finie | `docs/requirements.md` & `docs/use-cases.md` (**COM2**), `docs/class-diagram.md` (**COM3**), `docs/api-contract.md` + `openapi.yaml` (**COM4**), `docs/nosql-model.md` (**B1**) |

### 0.5 Vérification obligatoire de l'environnement

Chaque membre doit exécuter ces commandes avant de commencer les tâches métier.

```powershell
git --version
java -version
node -v
npm -v
docker --version
docker compose version
```

Vérifier ensuite :

```powershell
# Depuis la racine du projet
Test-Path .env
Test-Path .env.example
Test-Path infra/compose.yaml
Test-Path backend
Test-Path frontend
```

Résultats attendus :

- Java 21 disponible.
- Node.js compatible avec le projet.
- Docker Desktop démarré.
- Fichier `.env` présent localement.
- Fichier `infra/compose.yaml` accessible.
- Backend et frontend présents dans le dépôt.

#### Test de la stack Docker

```powershell
docker compose -f infra/compose.yaml --env-file .env up -d
docker compose -f infra/compose.yaml ps
```

Vérifier que MongoDB et Redis sont en état de fonctionnement :

```powershell
docker exec oliveconnect-mongodb mongosh --eval "db.adminCommand('ping')"
docker exec oliveconnect-redis redis-cli ping
```

Résultats attendus :

- MongoDB : réponse contenant `ok: 1`.
- Redis : `PONG`.

Chaque membre doit obtenir ces résultats sur sa propre machine.

---

## 1. Planning jour par jour

> Hypothèse du plan : 30 h / semaine / membre. Charge S1 prévue : **A 29 h · B 28 h · C 29 h** (communs inclus).

| Jour | Tous (COMMUN) | Membre A | Membre B | Membre C |
|---|---|---|---|---|
| **J1** | COM1 Dépôt + VM (1,5 h) · COM2 Besoins & cas d'utilisation (1,5 h) | A1 Projet Spring Boot (2 h) | B2 Pile locale compose (2 h) · début B1 | C1 Projet React (3 h) |
| **J2** | COM3 Diagramme de classes (1,5 h) · **COM4 Gel du contrat → tag `contracts-v1`** (1,5 h) | A2 Socle commun (3 h) *(pas d'endpoint métier)* | B1 Modèle NoSQL (suite) | C2 Types `contract.ts` (début) |
| **J3** | Point 15 min | A3 Comptes et connexion (5 h) | B1 **relu par A et C avant ce soir** · B3 Index (4 h) · **B4 CacheInvalidator publié** (1 h) | C2 client HTTP + mocks MSW · C3 Connexion et rôles (4 h) |
| **J4** | Point 15 min | A4 Sécurité JWT (4 h) · A5 Vergers (début) | B5 Ressources (7 h) | C4 Vergers agriculteur (4 h) · C5 Demandes (début) |
| **J5** | Intégration l'après-midi · **démo** · présentations 5 min | A5 (fin) · A6 Demandes de récolte (4 h) | B6 Données de démo (3 h) | C5 (fin) · C6 Carte de base sur mocks (3 h) · bascule C3/C4/C5 → API réelle |

**Présentations de 5 min du vendredi S1** (plan §9) :
- **A** : les 4 couches et un test 403 / 404 ;
- **B** : imbrication ou référence, ordre `[lon, lat]` ;
- **C** : mocks du contrat et carte.

### 1.1 Critères d'acceptation des tâches de la Semaine 1

Une tâche est considérée comme terminée uniquement lorsque son résultat est vérifiable.

| Tâche | Responsable | Critère d'acceptation |
|---|---|---|
| COM1 | Tous | Les trois membres ont cloné le dépôt et peuvent créer une PR. |
| COM2 | Tous | Les exigences et cas d'utilisation sont documentés et validés. |
| COM3 | Tous | Le diagramme de classes est validé par les trois membres. |
| COM4 | Tous | Le contrat est validé, les fichiers sont fusionnés et le tag `contracts-v1` existe. |
| A1 | A | Spring Boot démarre et `/actuator/health` retourne `UP`. |
| A2 | A | Les packages communs, enums et DTO nécessaires compilent. |
| A3 | A | L'inscription et la connexion fonctionnent avec MongoDB. |
| A4 | A | Les tests JWT et les contrôles de rôles fonctionnent. |
| A5 | A | Un agriculteur peut créer et consulter ses propres vergers. |
| A6 | A | Un agriculteur peut créer une demande de récolte. |
| B1 | B | Le modèle NoSQL est documenté et validé par A et C. |
| B2 | B | MongoDB et Redis démarrent avec Docker Compose. |
| B3 | B | Les index prévus sont créés et vérifiés. |
| B4 | B | L'interface `CacheInvalidator` et son implémentation vide compilent. |
| B5 | B | Le module Resource prévu pour S1 est structuré et testé selon sa tâche. |
| B6 | B | Les données de démonstration nécessaires sont disponibles. |
| C1 | C | Le frontend React démarre sans erreur. |
| C2 | C | Les types correspondent au contrat et les mocks MSW fonctionnent. |
| C3 | C | La connexion et les routes protégées fonctionnent. |
| C4 | C | La création et l'affichage des vergers fonctionnent. |
| C5 | C | Le formulaire de demande fonctionne avec les mocks puis avec l'API réelle. |
| C6 | C | La carte fonctionne avec les données disponibles et les mocks prévus. |

---

## 2. COMMUN — COM1 à COM4

### COM1 · Dépôt et VM (1,5 h chacun) — J1

**Livrable** : dépôt prêt ; accès VM confirmé.

#### Étape 1 — Créer le dépôt (une personne, les autres clonent)

```powershell
mkdir oliveconnect; cd oliveconnect
git init -b main
mkdir backend, frontend, infra, docs, sample-data, .github
New-Item README.md, .gitignore, .env.example -ItemType File
```

Disposition imposée (plan §3) :

```text
oliveconnect/
  backend/       Spring Boot (package-by-feature)
  frontend/      React + TypeScript + Vite
  infra/         compose.yaml, nginx/
  docs/          requirements, use-cases, class-diagram, nosql-model, api-contract,
                 test-plan, deployment, perf-report, data-dictionary
  sample-data/   fichiers CSV/JSON d'exemple, script seed
  README.md  .gitignore  .env.example   (jamais de .env ni de secret dans Git)
```

#### Étape 2 — `.gitignore` (racine)

```gitignore
# Secrets
.env
*.env
!.env.example

# Java / Maven
backend/target/
*.class
*.log
hs_err_pid*

# IDE
.idea/
*.iml
.vscode/
*.swp

# Node / Vite
frontend/node_modules/
frontend/dist/
frontend/.vite/

# OS
.DS_Store
Thumbs.db

# Sauvegardes locales
backups/
```

#### Étape 3 — `.env.example` (noms + valeurs factices uniquement)

> Le fichier `.env` réel est lu **à la fois** par Docker Compose (`--env-file`) **et** par Spring Boot (`spring.config.import`, voir A1). Chacun copie `.env.example` en `.env` et change les valeurs.

```dotenv
# --- MongoDB (conteneur) ---
MONGO_ROOT_USERNAME=oliveconnect
MONGO_ROOT_PASSWORD=change-me
# URI utilisée par Spring Boot (encoder les caractères spéciaux du mot de passe en %XX)
MONGODB_URI=mongodb://oliveconnect:change-me@localhost:27017/oliveconnect?authSource=admin

# --- Redis (conteneur) ---
REDIS_HOST=localhost
REDIS_PORT=6379

# --- Sécurité ---
# >= 32 caractères aléatoires (HS256). Ne JAMAIS committer la vraie valeur.
JWT_SECRET=change-me-with-at-least-32-random-characters
CORS_ALLOWED_ORIGINS=http://localhost:5173
```

Générer un vrai secret (PowerShell 5 ou 7) :

```powershell
$b = New-Object byte[] 48; [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); [Convert]::ToBase64String($b)
```

#### Étape 4 — Modèle de PR : `.github/pull_request_template.md`

```markdown
## Quoi
<!-- Tâche (ex. A5 Vergers) + résumé en 2 lignes -->

## Contrat
- [ ] Les réponses respectent docs/api-contract.md (mêmes clés, ni plus ni moins)
- [ ] Aucun champ / enum / code d'erreur renommé (sinon PR `contract-change`)

## Tests
- [ ] Cas nominal testé
- [ ] Au moins un cas d'erreur (400 / 401 / 403 / 404 / 409)
- [ ] `mvnw verify` ou `npm run build` vert

## Règles
- [ ] Pas de secret, pas de mot de passe / token dans les logs
- [ ] Controller → DTO (jamais un Document) ; farmerId depuis le token
- [ ] Docs du module à jour dans docs/

## Relecteur
<!-- A relit B, B relit C, C relit A -->
```

#### Étape 5 — Branches, issues, protection

```powershell
git add . ; git commit -m "chore: structure initiale du dépôt"
git remote add origin <URL_DU_DEPOT>
git push -u origin main
```

- Sur GitHub/GitLab : **protéger `main`** (PR obligatoire, 1 approbation, pas de push direct).
- Créer un **tableau d'issues** (colonnes : À faire / En cours / En revue / Fait) avec **une issue par tâche** (COM1…COM4, A1…A6, B1…B6, C1…C6) et le label du membre.
- Convention : `feature/<module>-<sujet>`, ex. `feature/auth-jwt`, `feature/resources-crud`, `feature/front-map`.

```powershell
git switch -c feature/auth-jwt
# ... travail ...
git push -u origin feature/auth-jwt   # puis ouvrir la PR
```

#### Étape 6 — Demander la VM (à faire dès J1 : risque « déploiement tardif »)

Courriel type à l'encadrant / service informatique :

> Objet : Demande de VM — projet OliveConnect (2ème année ING INFO)
> Bonjour, dans le cadre du projet OliveConnect (J2EE / BD massives / Administration des serveurs Web), notre équipe de 3 a besoin d'une VM Linux avec **accès SSH**, **Docker + Docker Compose**, ports **80/443** ouverts vers l'extérieur, 2 vCPU / 4 Go RAM / 20 Go disque. Nous souhaitons aussi confirmer l'interprétation suivante : l'« application intranet » de saisie = zones protégées par rôle de la même application React. Merci.

✅ **Fini quand** : les 3 ont cloné, ouvert une PR de test relue, et l'accès VM est confirmé (ou la demande tracée dans une issue).

---

### COM2 · Besoins et cas d'utilisation (1,5 h chacun) — J1

**Livrables** : `docs/requirements.md`, `docs/use-cases.md`.

#### `docs/requirements.md` — format imposé : fonction, acteur, action, validation, résultat

````markdown
# Exigences fonctionnelles — OliveConnect

| ID | Fonction | Acteur | Action | Validation | Résultat |
|----|----------|--------|--------|------------|----------|
| R-F1-01 | F1 Vergers | Agriculteur | Créer un verger | name ≤ 100 car., areaHectares > 0, coordonnées [-180,180]/[-90,90] | 201, verger maturityStatus=UNKNOWN |
| R-F1-02 | F1 Vergers | Agriculteur | Lister ses vergers | token FARMER | 200, PageResponse de ses vergers seulement |
| R-F1-03 | F1 Vergers | Resp. collecte | Modifier maturityStatus | rôle COLLECTION_MANAGER/ADMIN | 200 ; agriculteur → 403 |
| R-F1-04 | F1 Demandes | Agriculteur | Déposer une demande | quantité > 0 ; urgencyReason si URGENT ; verger à lui | 201, status=SUBMITTED, farmerId du token |
| R-F1-05 | F1 Demandes | Resp. collecte | Approuver / rejeter | SUBMITTED seulement ; motif si REJECT | 200 ; sinon 409 / 400 |
| R-F1-06 | F1 Demandes | Agriculteur | Modifier / annuler avant départ | SUBMITTED/APPROVED (modif) ; pas IN_PROGRESS ni final (annul.) | 200 ; sinon 409 |
| R-F1-07 | F1 Tournées | Chef d'équipe | Démarrer, saisir quantités | tournée assignée | 200 ; autre chef → 403 |
| R-F1-08 | F1 Pressoir | Resp. pressoir | Enregistrer une réception | weightKg > 0 et ≤ capacité benne ; (tourId, binId) unique | 201 ; doublon → 409 |
| R-F2-01 | F2 Alertes | Agriculteur | Signaler une alerte | location obligatoire ; verger à lui | 201, status=OPEN |
| R-F2-02 | F2 Alertes | Resp. collecte | Reconnaître / résoudre / écarter | note ou motif obligatoire | 200 ; sinon 400 / 409 |
| R-F3-01 | F3 Ressources | Resp. logistique | Gérer ouvriers, tracteurs, bennes, matériel | champs valides | 201 / 200 ; autre rôle → 403 |
| R-F3-02 | F3 Ressources | Resp. collecte | Planifier une tournée | ressources libres ce jour-là | 200 ; conflit → 409 RESOURCE_CONFLICT |
| R-F4-01 | F4 Carte | Visiteur | Voir la carte publique | bbox obligatoire | 200, seulement publicVisibility=true, champs privés null |
| R-F4-02 | F4 Carte | Agriculteur / Resp. | Carte opérationnelle | token | ses vergers + publics / tous |
| R-F5-01 | F5 Dashboard (bonus) | Resp. collecte, pressoir, admin | Consulter les KPI | from ≤ to, ≤ 366 jours | 200 |
| R-SEC-01 | Sécurité | Tous | Appel sans token | — | 401 UNAUTHENTICATED |
| R-SEC-02 | Sécurité | Tous | Mauvais rôle | — | 403 ACCESS_DENIED |
| R-SEC-03 | Sécurité | Agriculteur | Lire la ressource d'un autre | — | 404 NOT_FOUND |
| R-NF-01 | Performance | — | ~100 connexions simultanées | carte/recherche < 10 s (objectif interne p95 < 2 s) | rapport k6 |
| R-NF-02 | Ergonomie | — | Mobile, formulaires courts, « Ma position », aide | 360 px | captures |
| R-NF-03 | Imports | Admin | CSV/JSON validés, idempotents, planifiés | externalId unique | 0 doublon au 2e import |
| R-NF-04 | Hébergement | — | Nginx, sauvegardes, supervision | — | URL externe + restauration testée |
````

#### `docs/use-cases.md` — les 7 acteurs

> Mermaid n'a pas de diagramme UML de cas d'utilisation : on utilise un `flowchart` (ou on dessine sur draw.io / StarUML et on exporte une image dans `docs/img/`).

````markdown
# Cas d'utilisation

```mermaid
flowchart LR
  V([Visiteur]) --> UC1[Consulter la carte publique]
  F([Agriculteur]) --> UC2[S'inscrire / se connecter]
  F --> UC3[Gérer ses vergers]
  F --> UC4[Déposer / modifier / annuler une demande]
  F --> UC5[Signaler une alerte]
  F --> UC6[Consulter notifications et suivi]
  CM([Resp. collecte]) --> UC7[Valider / rejeter une demande]
  CM --> UC8[Planifier une tournée]
  CM --> UC9[Traiter les alertes]
  CM --> UC10[Modifier la maturité d'un verger]
  TL([Chef d'équipe]) --> UC11[Démarrer une tournée / saisir les quantités]
  LM([Resp. logistique]) --> UC12[Gérer les ressources]
  PM([Resp. pressoir]) --> UC13[Enregistrer une réception / capacité]
  AD([Admin]) --> UC14[Gérer comptes, rôles et imports]
  CM --> UC15[Tableau de bord]
  PM --> UC15
  AD --> UC15
```

## Fiche type (une par cas)
| Champ | Contenu |
|---|---|
| Nom | UC4 Déposer une demande de récolte |
| Acteur | Agriculteur |
| Préconditions | Connecté (FARMER), possède au moins un verger |
| Scénario nominal | 1. choisit un verger 2. choisit PLANNED/URGENT 3. date + quantité 4. valide → 201 SUBMITTED |
| Alternatives | URGENT sans motif → 400 ; verger d'un autre → 404 ; quantité ≤ 0 → 400 |
| Postconditions | Demande SUBMITTED visible dans « Mes demandes » et dans la file du resp. collecte |
````

✅ **Fini quand** : les deux fichiers sont fusionnés dans `main` via PR relue.

---

### COM3 · Diagramme de classes (1,5 h chacun) — J2

**Livrable** : `docs/class-diagram.md` — **un seul diagramme dessiné ensemble** ; chaque classe persistée reçoit sa collection (règles de B, voir B1).

> [!WARNING]
> Ce diagramme, le schéma NoSQL (B1), l'API (contrat) et le code doivent avoir **exactement les mêmes noms**. C'est le point « cohérence » vérifié à l'évaluation.

````markdown
# Diagramme de classes (UML → collections MongoDB)

```mermaid
classDiagram
  class User {
    <<users>>
    String id
    String fullName
    String email
    String passwordHash
    String phone
    Role role
    boolean active
    Instant createdAt
    Instant updatedAt
  }
  class Orchard {
    <<orchards>>
    String id
    String farmerId
    String name
    Double areaHectares
    String oliveVariety
    GeoJsonPoint location
    MaturityStatus maturityStatus
    boolean publicVisibility
    Instant createdAt
    Instant updatedAt
  }
  class HarvestRequest {
    <<harvest_requests>>
    String id
    String farmerId
    String orchardId
    String orchardName
    HarvestType type
    String urgencyReason
    HarvestStatus status
    Instant requestedAt
    LocalDate preferredDate
    BigDecimal estimatedQuantityKg
    BigDecimal actualQuantityKg
    String reviewedBy
    String reviewNote
    String scheduledTourId
    Instant scheduledAt
    Instant completedAt
  }
  class FieldAlert {
    <<field_alerts>>
    String id
    String farmerId
    String orchardId
    AlertType type
    String description
    GeoJsonPoint location
    AlertPriority priority
    AlertStatus status
    Instant acknowledgedAt
    Instant resolvedAt
    String resolutionNote
  }
  class Notification {
    <<notifications>>
    String id
    String userId
    NotificationType type
    String message
    EntityType entityType
    String entityId
    Instant readAt
  }
  class AuditEvent {
    <<audit_events>>
    String id
    String actorId
    String action
    String entityType
    String entityId
    Instant at
  }
  class Worker {
    <<workers>>
    String id
    String fullName
    String phone
    List~String~ skills
    ResourceStatus status
  }
  class Vehicle {
    <<vehicles>>
    String id
    VehicleType type
    String registration
    Integer capacityKg
    ResourceStatus status
  }
  class Bin {
    <<bins>>
    String id
    String code
    Integer capacityKg
    ResourceStatus status
  }
  class Equipment {
    <<equipment>>
    String id
    EquipmentType type
    String label
    Integer quantity
    ResourceStatus status
  }
  class Tour {
    <<tours>>
    String id
    LocalDate tourDate
    TourStatus status
    String teamLeaderId
    List~String~ vehicleIds
    List~String~ workerIds
    List~String~ binIds
    Instant startedAt
    Instant completedAt
  }
  class TourStop {
    <<imbriqué dans tours>>
    String stopId
    int sequence
    String harvestRequestId
    String orchardId
    String orchardName
    BigDecimal plannedQuantityKg
    BigDecimal actualQuantityKg
    StopStatus status
  }
  class Press {
    <<presses>>
    String id
    String name
    GeoJsonPoint location
    Integer dailyCapacityKg
    Integer availableCapacityKg
  }
  class PressReception {
    <<press_receptions>>
    String id
    String tourId
    String binId
    String pressId
    Instant receivedAt
    BigDecimal weightKg
    QualityGrade qualityGrade
    String notes
    String recordedBy
  }
  class MaturityReading {
    <<maturity_readings>>
    String id
    String orchardId
    MaturityStatus status
    Instant observedAt
  }
  class WeatherObservation {
    <<weather_observations>>
    String id
    GeoJsonPoint location
    Double rainMm
    Double temperatureC
    Instant observedAt
  }
  class ImportJob {
    <<import_jobs>>
    String id
    ImportType type
    String sourceFile
    ImportStatus status
    int readCount
    int insertedCount
    int updatedCount
    int rejectedCount
  }

  User "1" --> "0..*" Orchard : farmerId
  User "1" --> "0..*" HarvestRequest : farmerId
  Orchard "1" --> "0..*" HarvestRequest : orchardId
  Orchard "1" --> "0..*" FieldAlert : orchardId
  Orchard "1" --> "0..*" MaturityReading : orchardId
  Tour "1" *-- "0..20" TourStop : stops (imbriqués)
  TourStop --> HarvestRequest : harvestRequestId
  Tour --> User : teamLeaderId
  Tour --> "0..10" Vehicle : vehicleIds
  Tour --> "0..30" Worker : workerIds
  Tour --> "0..20" Bin : binIds
  PressReception --> Tour : tourId
  PressReception --> Bin : binId
  PressReception --> Press : pressId
  Notification --> User : userId
```

Légende : `<<nom>>` = collection MongoDB. `*--` = composition = **imbrication** ; `-->` = **référence** par identifiant.
````

> Les champs des classes **non utilisées en S1** (`MaturityReading`, `WeatherObservation`, `AuditEvent`, `ImportJob`) sont une proposition à valider au jour 2 ; leurs formats définitifs sont figés par A dans `docs/data-dictionary.md` en fin de S2 (plan §5.5 Import).

---

### COM4 · Gel du contrat v1 (1,5 h chacun) — **fin du J2**

**Livrable** : `docs/api-contract.md` + `openapi.yaml` (squelette) + signatures des façades + **tag `contracts-v1`**.

#### Étape 1 — Relecture ligne à ligne (ensemble, 45 min)

Recopier **sections 5 et 6 du PDF** dans `docs/api-contract.md` (conventions §5.2, énumérations §5.3, enveloppes §5.4, objets §5.5, façades §5.6, écarts §5.7, tableau des endpoints §6). Puis valider la liste « **À valider ensemble au jour 2** » (plan §10) et noter la décision en tête du fichier :

````markdown
# Contrat API v1 — OliveConnect (gelé : tag contracts-v1)

## 1. Décisions du jour 2 (§10 « À valider ensemble au jour 2 »)
| Sujet | Décision |
|---|---|
| Enums MaturityStatus / AlertType / QualityGrade | validées telles que proposées dans le plan |
| TTL Redis 45/60/90 s ; 5 échecs / 15 min ; JWT 2 h (7200 s) | validés |
| Bornes : 20 arrêts, 30 ouvriers, 10 véhicules, 20 bennes, 500 vergers carte, 366 jours, size ≤ 100 | validées |
| Objectif interne p95 < 2 s (exigence sujet < 10 s) | validé |
| Conflits de ressources au niveau du **jour** ; matériel en catalogue seulement | validés |
| « Application intranet » = zones protégées par rôle de la même app React | à confirmer encadrant |
| PATCH /resources/{kind}/{id} : status IN_USE refusé (géré par les tournées) | décidé J2 |
| Filtres from/to de GET /harvest-requests portent sur preferredDate | décidé J2 |
| Agriculteur qui envoie maturityStatus dans PATCH /orchards/{id} → 403 | décidé J2 |
| Montants en kg côté Mongo : Decimal128 ; capacités (capacityKg) : entiers | décidé J2 |

## 2. Matrice des droits des 7 acteurs (§4 — source des tests 403 / 404)
| Action | Visiteur | Agric. | Resp. coll. | Chef éq. | Resp. log. | Resp. press. | Admin |
|---|---|---|---|---|---|---|---|
| Carte publique des vergers | oui | oui | oui | oui | oui | oui | oui |
| Carte opérationnelle | — | propres | oui | — | — | — | oui |
| Vergers (créer, modifier) | — | propres | modifier | — | — | — | oui |
| Demandes de récolte | — | propres | valider, annuler | — | — | — | lecture |
| Signaler une alerte | — | propres | — | — | — | — | — |
| Traiter les alertes | — | voir les siennes | oui | — | — | — | lecture |
| Ressources (ouvriers, tracteurs, bennes, matériel) | — | — | lecture | — | oui | — | oui |
| Créer et planifier une tournée | — | — | oui | — | lecture | — | lecture |
| Démarrer, terminer, saisir les quantités | — | — | oui | assignées | — | — | — |
| Réception au pressoir, capacité | — | — | lecture | — | — | oui | lecture |
| Tableau de bord | — | — | oui | — | — | oui | oui |
| Comptes, rôles, imports | — | — | — | — | — | — | oui |

> Règle : chaque case `—` doit avoir un test qui renvoie **403 ACCESS_DENIED** ; chaque case `propres` (ou `assignées`) doit avoir un test qui renvoie **404 NOT_FOUND** sur la ressource d'un autre (et 403 pour un autre chef sur `POST /tours/{id}/start`).

## 3. Machines à états (§4)

### Cycle de vie d'une demande de récolte (`HarvestStatus`, sans `UNDER_REVIEW` §5.7)
```mermaid
stateDiagram-v2
  [*] --> SUBMITTED : POST /harvest-requests (FARMER)
  SUBMITTED --> APPROVED : POST /review APPROVE (COLLECTION_MANAGER)
  SUBMITTED --> REJECTED : POST /review REJECT + reason (COLLECTION_MANAGER)
  SUBMITTED --> CANCELLED : POST /cancel (FARMER propre / COLLECTION_MANAGER)
  APPROVED --> SCHEDULED : [système] POST /tours/{id}/plan
  APPROVED --> CANCELLED : POST /cancel (FARMER propre / COLLECTION_MANAGER)
  SCHEDULED --> IN_PROGRESS : [système] POST /tours/{id}/start
  SCHEDULED --> APPROVED : [système] POST /tours/{id}/cancel
  SCHEDULED --> CANCELLED : POST /cancel -> TourFacade.removeStop
  IN_PROGRESS --> COMPLETED : [système] POST /tours/{id}/stops/{stopId}/complete
  REJECTED --> [*]
  CANCELLED --> [*]
  COMPLETED --> [*]
```

### Cycle de vie d'une alerte terrain (`AlertStatus`)
```mermaid
stateDiagram-v2
  [*] --> OPEN : POST /alerts (FARMER)
  OPEN --> ACKNOWLEDGED : POST /alerts/{id}/acknowledge (COLLECTION_MANAGER)
  OPEN --> DISMISSED : POST /alerts/{id}/dismiss + reason (COLLECTION_MANAGER)
  ACKNOWLEDGED --> RESOLVED : POST /alerts/{id}/resolve + note (COLLECTION_MANAGER)
  ACKNOWLEDGED --> DISMISSED : POST /alerts/{id}/dismiss + reason (COLLECTION_MANAGER)
  RESOLVED --> [*]
  DISMISSED --> [*]
```

### Cycle de vie d'une tournée (`TourStatus`)
```mermaid
stateDiagram-v2
  [*] --> DRAFT : POST /tours (COLLECTION_MANAGER)
  DRAFT --> PLANNED : POST /tours/{id}/plan (COLLECTION_MANAGER)
  DRAFT --> CANCELLED : POST /tours/{id}/cancel (COLLECTION_MANAGER)
  PLANNED --> IN_PROGRESS : POST /tours/{id}/start (TEAM_LEADER assigné / COLLECTION_MANAGER)
  PLANNED --> CANCELLED : POST /tours/{id}/cancel -> demandes reviennent à APPROVED
  IN_PROGRESS --> COMPLETED : POST /tours/{id}/complete (tous arrêts COMPLETED)
  CANCELLED --> [*]
  COMPLETED --> [*]
```

## 4. Procédure `contract-change` (§5.1)
Ajout de champ optionnel en lecture uniquement, accord des 3, PR qui modifie en même temps :
`docs/api-contract.md`, `docs/openapi.yaml`, `frontend/src/types/contract.ts` et les exemples.
````

> [!CAUTION]
> Après le tag : **interdit** de renommer/supprimer un champ, changer un type, une unité, une valeur d'énumération, un code d'erreur ou un statut HTTP.

#### Étape 2 — Squelette `docs/openapi.yaml`

> springdoc générera la version complète en S2 (A13). Ce squelette fige les schémas communs et sert de référence de nommage.

```yaml
openapi: 3.0.3
info:
  title: OliveConnect API
  version: v1
servers:
  - url: /api/v1
components:
  securitySchemes:
    bearerAuth: { type: http, scheme: bearer, bearerFormat: JWT }
  schemas:
    ApiError:
      type: object
      required: [timestamp, status, code, message, fieldErrors]
      properties:
        timestamp: { type: string, format: date-time }
        status: { type: integer }
        code:
          type: string
          enum: [VALIDATION_ERROR, UNAUTHENTICATED, ACCESS_DENIED, NOT_FOUND,
                 INVALID_STATE_TRANSITION, RESOURCE_CONFLICT, DUPLICATE_RESOURCE,
                 RATE_LIMITED, INTERNAL_ERROR]
        message: { type: string }
        fieldErrors: { type: object, additionalProperties: { type: string } }
    PageMeta:
      type: object
      required: [content, page, size, totalElements, totalPages]
      properties:
        content: { type: array, items: {} }
        page: { type: integer }
        size: { type: integer }
        totalElements: { type: integer, format: int64 }
        totalPages: { type: integer }
    GeoPoint:
      type: object
      required: [type, coordinates]
      properties:
        type: { type: string, enum: [Point] }
        coordinates: { type: array, minItems: 2, maxItems: 2, items: { type: number },
                       description: "[longitude, latitude]" }
    Role: { type: string, enum: [FARMER, COLLECTION_MANAGER, TEAM_LEADER, LOGISTICS_MANAGER, PRESS_MANAGER, ADMIN] }
    UserResponse:
      type: object
      properties:
        id: { type: string }
        fullName: { type: string }
        email: { type: string }
        role: { $ref: '#/components/schemas/Role' }
        active: { type: boolean }
        createdAt: { type: string, format: date-time }
    # ... OrchardResponse, HarvestRequestResponse, etc. : recopier §5.5
security:
  - bearerAuth: []
paths:
  /auth/register: { post: { tags: [auth], security: [], responses: { '201': { description: UserResponse }, '400': { description: ApiError }, '409': { description: ApiError } } } }
  /auth/login:    { post: { tags: [auth], security: [], responses: { '200': { description: LoginResponse }, '400': { description: ApiError }, '401': { description: ApiError }, '429': { description: ApiError } } } }
  /users/me:      { get:  { tags: [auth], responses: { '200': { description: UserResponse }, '401': { description: ApiError } } } }
  # ... un chemin par ligne du tableau §6
```

#### Étape 3 — Signatures Java des façades (plan §5.6), commitées **avant** le tag

Fichiers dans `backend/src/main/java/tn/oliveconnect/common/facade/` (A crée le projet en A1 le J1, donc le dossier existe) :

```java
// HarvestRequestView.java
package tn.oliveconnect.common.facade;

import java.math.BigDecimal;
import tn.oliveconnect.common.enums.HarvestStatus;

public record HarvestRequestView(
        String id, String farmerId, String orchardId, String orchardName,
        BigDecimal estimatedQuantityKg, HarvestStatus status) {}
```

```java
// HarvestRequestFacade.java — fourni par A (implémentation en S2, tâche A7)
package tn.oliveconnect.common.facade;

import java.math.BigDecimal;
import java.util.List;

public interface HarvestRequestFacade {
    List<HarvestRequestView> findApproved(List<String> ids);
    void markScheduled(List<String> ids, String tourId);
    void markInProgress(List<String> ids);
    void markCompleted(String id, BigDecimal actualKg);
    void revertToApproved(List<String> ids);
}
```

```java
// EntityRef.java
package tn.oliveconnect.common.facade;

import tn.oliveconnect.common.enums.EntityType;

public record EntityRef(EntityType entityType, String entityId) {}
```

```java
// NotificationFacade.java — fourni par A (implémentation en S2, tâche A10)
package tn.oliveconnect.common.facade;

import tn.oliveconnect.common.enums.NotificationType;

public interface NotificationFacade {
    void notify(String userId, NotificationType type, String message, EntityRef ref);
}
```

```java
// TourFacade.java — fourni par B (S2)
package tn.oliveconnect.common.facade;

public interface TourFacade {
    /** Appelé par A quand une demande SCHEDULED est annulée : retire son arrêt de la tournée. */
    void removeStop(String harvestRequestId);
}
```

```java
// CacheInvalidator.java — fourni par B ; implémentation VIDE en S1 (B4), réelle en S3
package tn.oliveconnect.common.facade;

/** Ne lève jamais d'exception. */
public interface CacheInvalidator {
    void orchardChanged(String orchardId);
    void mapChanged();
    void dashboardChanged();
}
```

> Les enums référencées (`HarvestStatus`, `NotificationType`, `EntityType`…) sont créées par A en A2 ; pour que le tag compile, A pousse les enums **le J2 matin** (elles sont la copie exacte de §5.3).

#### Étape 4 — Poser le tag

```powershell
git switch main; git pull
git tag -a contracts-v1 -m "Contrat API v1 gelé (sections 5 et 6 du plan)"
git push origin contracts-v1
```

✅ **Fini quand** : `git show contracts-v1` affiche le commit contenant `docs/api-contract.md`, `docs/openapi.yaml`, les façades et les enums.

---

## 3. MEMBRE A — Backend cœur et sécurité (A1 → A6)

**Preuve à livrer vendredi** : PR fusionnée : login, verger, demande ; `mvnw test` vert (401, 403, doublon d'email, propriété).

### Vue d'ensemble des 4 couches (à savoir expliquer en 5 min)

```mermaid
flowchart LR
  R["React (formulaire)"] -->|"POST /api/v1/harvest-requests + JSON"| C["HarvestRequestController<br/>(DTO, @Valid, codes HTTP)"]
  C --> S["HarvestRequestService<br/>(autorisation, règles, farmerId du token)"]
  S --> D["HarvestRequestRepository<br/>(Spring Data, requêtes seulement)"]
  D --> M[("MongoDB<br/>harvest_requests")]
  S -->|"Document → DTO"| C
  C -->|"201 HarvestRequestResponse"| R
```

Organisation : `tn.oliveconnect.<module>.{controller,service,repository,document,dto}`.

---

### A1 · Projet Spring Boot (2 h) — J1

**Preuve** : `/actuator/health` = `UP` avec MongoDB et Redis du compose.

#### Étape 1 — Générer avec Spring Initializr (https://start.spring.io)

| Champ | Valeur |
|---|---|
| Project | **Maven** |
| Language | Java |
| Spring Boot | **dernière 3.5.x stable proposée** (voir encadré) |
| Group / Artifact | `tn.oliveconnect` / `oliveconnect` |
| Package name | `tn.oliveconnect` |
| Java | **21** |
| Dépendances | Spring Web, Spring Data MongoDB, Spring Data Redis, Spring Security, Validation, Spring Boot Actuator, Lombok, Testcontainers |

Dézipper le contenu dans `backend/`.

> [!WARNING]
> **Version de Spring Boot.** Le code de ce guide est écrit pour **Spring Boot 3.5.x**. Si Initializr ne propose que **4.x**, c'est possible mais adaptez : propriété `spring.data.mongodb.uri` → `spring.mongodb.uri` ; Testcontainers 2 (`org.testcontainers.mongodb.MongoDBContainer`, artefact `testcontainers-mongodb`) ; package de `UserDetailsServiceAutoConfiguration` changé ; starter `spring-boot-starter-webmvc`. Règle du plan : **ne pas copier de vieux tutoriels**, laisser Spring Boot gérer les versions.

#### Étape 2 — Ajouter la librairie JWT dans `backend/pom.xml` (dans `<dependencies>`)

```xml
<!-- JWT HS256 (jjwt 0.12.x) -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
```

#### Étape 3 — Structure des packages (créer les dossiers vides)

```text
backend/src/main/java/tn/oliveconnect/
  OliveConnectApplication.java
  common/            (A)  error/, web/, enums/, facade/
  security/          (A)
  user/              (A)  controller/ service/ repository/ document/ dto/
  orchard/           (A)  idem
  harvest/           (A)  idem
  data/              (B)  MongoIndexConfig, seed/
  cache/             (B)  NoOpCacheInvalidator
  resource/          (B)  controller/ service/ repository/ document/ dto/
```

#### Étape 4 — Classe principale

```java
package tn.oliveconnect;

import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableMongoAuditing   // remplit @CreatedDate / @LastModifiedDate
public class OliveConnectApplication {

    public static void main(String[] args) {
        // Contrat : instants en UTC. Évite aussi qu'un LocalDate soit décalé
        // d'un jour lors de sa conversion en Date par Spring Data MongoDB.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(OliveConnectApplication.class, args);
    }
}
```

#### Étape 5 — `backend/src/main/resources/application.yml` (variables d'environnement)

> Supprimez `application.properties` généré. Aucun secret en dur : `JWT_SECRET` **n'a pas de valeur par défaut** (l'application refuse de démarrer sans).

```yaml
spring:
  application:
    name: oliveconnect
  config:
    # Lit le .env de la racine du dépôt (format clé=valeur) quand on lance depuis backend/
    import: optional:file:../.env[.properties]
  autoconfigure:
    exclude:
      # On gère nous-mêmes l'authentification : pas d'utilisateur "user" + mot de passe généré
      - org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration
  data:
    mongodb:
      uri: ${MONGODB_URI:mongodb://localhost:27017/oliveconnect}
      auto-index-creation: false      # les index sont versionnés par B (B3)
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
    web:
      pageable:
        default-page-size: 20
        max-page-size: 100            # contrat : size <= 100
  jackson:
    time-zone: UTC
    serialization:
      write-dates-as-timestamps: false   # Instant -> "2026-11-17T10:00:00Z"
    # NB : on garde l'inclusion des null (contrat : la clé est toujours présente)

server:
  port: 8080
  error:
    include-stacktrace: never
    include-message: never

oliveconnect:
  jwt:
    secret: ${JWT_SECRET}
    expiration-seconds: 7200
  cors:
    allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:5173}

management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      show-details: never     # réponse minimale (endpoint public)

logging:
  level:
    root: INFO
```

#### Étape 6 — Configuration Testcontainers partagée (tests d'intégration)

`backend/src/test/java/tn/oliveconnect/TestcontainersConfiguration.java` :

```java
package tn.oliveconnect;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    MongoDBContainer mongoDbContainer() {
        return new MongoDBContainer(DockerImageName.parse("mongo:8.0"));
    }

    @Bean
    @ServiceConnection(name = "redis")
    GenericContainer<?> redisContainer() {
        return new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine")).withExposedPorts(6379);
    }
}
```

`backend/src/test/resources/application-test.yml` :

```yaml
oliveconnect:
  jwt:
    secret: test-secret-only-for-tests-0123456789-abcdefghijklmnop
```

#### Étape 7 — Vérifier

```powershell
# 1) la pile de B (B2) doit tourner :
docker compose -f infra/compose.yaml --env-file .env up -d
# 2) lancer le backend
cd backend
.\mvnw.cmd spring-boot:run
# 3) dans un autre terminal
curl.exe -s http://localhost:8080/actuator/health
# attendu : {"status":"UP"}   (arrêtez Mongo : docker compose ... stop mongodb  -> {"status":"DOWN"})
```

> Si B2 n'est pas encore prêt le J1 : `docker run -d -p 127.0.0.1:27017:27017 mongo:8.0` et `docker run -d -p 127.0.0.1:6379:6379 redis:7.4-alpine` en attendant.

---

### A2 · Socle commun (3 h) — J2

**Preuve** : test — un 400, un 404 et un 409 renvoient **exactement** l'enveloppe du contrat.

#### Étape 1 — Les énumérations du contrat (§5.3) dans `common/enums/`

Un fichier par enum, **copie exacte** du contrat (ne rien renommer) :

```java
package tn.oliveconnect.common.enums;
public enum Role { FARMER, COLLECTION_MANAGER, TEAM_LEADER, LOGISTICS_MANAGER, PRESS_MANAGER, ADMIN }
```

```java
public enum MaturityStatus { UNKNOWN, IMMATURE, APPROACHING, OPTIMAL, OVERRIPE }
public enum HarvestType { PLANNED, URGENT }
public enum HarvestStatus { SUBMITTED, APPROVED, REJECTED, SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED }
public enum AlertType { MATURITY, PEST, RAIN, OTHER }
public enum AlertPriority { LOW, MEDIUM, HIGH }
public enum AlertStatus { OPEN, ACKNOWLEDGED, RESOLVED, DISMISSED }
public enum TourStatus { DRAFT, PLANNED, IN_PROGRESS, COMPLETED, CANCELLED }
public enum StopStatus { PLANNED, COMPLETED }
public enum ResourceStatus { AVAILABLE, IN_USE, MAINTENANCE }
public enum VehicleType { TRACTOR, TRUCK }
public enum EquipmentType { NET, VIBRATOR, OTHER }
public enum QualityGrade { A, B, C }
public enum NotificationType { REQUEST_APPROVED, REQUEST_REJECTED, TOUR_SCHEDULED, TOUR_CHANGED,
    COLLECTION_STARTED, COLLECTION_COMPLETED, ALERT_ACKNOWLEDGED, ALERT_RESOLVED, ALERT_DISMISSED }
public enum ImportType { ORCHARDS, WORKERS, VEHICLES, PRESSES, MATURITY_READINGS, WEATHER_OBSERVATIONS, PRESS_CAPACITY }
public enum ImportStatus { RUNNING, COMPLETED, PARTIAL, FAILED }
public enum EntityType { HARVEST_REQUEST, TOUR, ALERT }   // entityType des notifications (§5.5)
```

> ⚠️ Pas d'`UNDER_REVIEW` (supprimé volontairement, §5.7).

#### Étape 2 — Codes d'erreur figés (§5.4) : `common/error/ErrorCode.java`

```java
package tn.oliveconnect.common.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Requête invalide."),
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "Authentification requise."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "Accès refusé."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "Ressource introuvable."),
    INVALID_STATE_TRANSITION(HttpStatus.CONFLICT, "Transition de statut interdite."),
    RESOURCE_CONFLICT(HttpStatus.CONFLICT, "Ressource déjà affectée à une autre tournée ce jour-là."),
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "Ressource déjà existante."),
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "Trop de tentatives. Réessayez plus tard."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur interne. Réessayez plus tard.");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }
    public HttpStatus status() { return status; }
    public String defaultMessage() { return defaultMessage; }
}
```

#### Étape 3 — L'enveloppe : `common/error/ApiError.java`

```java
package tn.oliveconnect.common.error;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/** Enveloppe de TOUTES les réponses 4xx/5xx. Jamais de stack trace ni de chaîne de connexion. */
public record ApiError(Instant timestamp, int status, String code, String message,
                       Map<String, String> fieldErrors) {

    public static ApiError of(ErrorCode code, String message, Map<String, String> fieldErrors) {
        return new ApiError(Instant.now().truncatedTo(ChronoUnit.SECONDS),
                code.status().value(), code.name(),
                message != null ? message : code.defaultMessage(),
                fieldErrors != null ? fieldErrors : Map.of());   // {} et jamais null
    }
}
```

#### Étape 4 — Exception métier : `common/error/ApiException.java`

```java
package tn.oliveconnect.common.error;

import java.util.Map;

/** Lancée par les services ; traduite en ApiError par le GlobalExceptionHandler. */
public class ApiException extends RuntimeException {
    private final ErrorCode code;
    private final Map<String, String> fieldErrors;

    public ApiException(ErrorCode code, String message, Map<String, String> fieldErrors) {
        super(message != null ? message : code.defaultMessage());
        this.code = code;
        this.fieldErrors = fieldErrors != null ? fieldErrors : Map.of();
    }
    public ErrorCode getCode() { return code; }
    public Map<String, String> getFieldErrors() { return fieldErrors; }

    public static ApiException notFound(String message) { return new ApiException(ErrorCode.NOT_FOUND, message, null); }
    public static ApiException accessDenied(String message) { return new ApiException(ErrorCode.ACCESS_DENIED, message, null); }
    public static ApiException unauthenticated(String message) { return new ApiException(ErrorCode.UNAUTHENTICATED, message, null); }
    public static ApiException duplicate(String field, String message) {
        return new ApiException(ErrorCode.DUPLICATE_RESOURCE, message, field == null ? null : Map.of(field, message));
    }
    public static ApiException validation(String field, String message) {
        return new ApiException(ErrorCode.VALIDATION_ERROR, ErrorCode.VALIDATION_ERROR.defaultMessage(), Map.of(field, message));
    }
    public static ApiException invalidTransition(String message) {
        return new ApiException(ErrorCode.INVALID_STATE_TRANSITION, message, null);
    }
}
```

#### Étape 5 — `common/error/GlobalExceptionHandler.java` (`@RestControllerAdvice`)

```java
package tn.oliveconnect.common.error;

import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private ResponseEntity<ApiError> build(ErrorCode code, String message, Map<String, String> fieldErrors) {
        return ResponseEntity.status(code.status()).body(ApiError.of(code, message, fieldErrors));
    }

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiError> handleApi(ApiException ex) {
        return build(ex.getCode(), ex.getMessage(), ex.getFieldErrors());
    }

    /** @Valid sur un @RequestBody : on remplit fieldErrors (premier message par champ). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleBodyValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
          .forEach(fe -> errors.putIfAbsent(fe.getField(), fe.getDefaultMessage()));
        ex.getBindingResult().getGlobalErrors()
          .forEach(ge -> errors.putIfAbsent(ge.getObjectName(), ge.getDefaultMessage()));
        return build(ErrorCode.VALIDATION_ERROR, null, errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex) {
        return build(ErrorCode.VALIDATION_ERROR, "JSON mal formé ou valeur invalide.", null);
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class,
            HandlerMethodValidationException.class, ConstraintViolationException.class})
    ResponseEntity<ApiError> handleParams(Exception ex) {
        return build(ErrorCode.VALIDATION_ERROR, "Paramètre invalide ou manquant.", null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ApiError> handleMethod(HttpRequestMethodNotSupportedException ex) {
        return build(ErrorCode.VALIDATION_ERROR, "Méthode HTTP non supportée.", null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiError> handleNoRoute(NoResourceFoundException ex) {
        return build(ErrorCode.NOT_FOUND, null, null);
    }

    /** Index unique violé (ex. email) : filet de sécurité si deux inscriptions arrivent en même temps. */
    @ExceptionHandler(DuplicateKeyException.class)
    ResponseEntity<ApiError> handleDuplicate(DuplicateKeyException ex) {
        return build(ErrorCode.DUPLICATE_RESOURCE, null, null);
    }

    /** @PreAuthorize refusé -> 403 (l'utilisateur EST connecté, sinon le filtre aurait renvoyé 401). */
    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
    ResponseEntity<ApiError> handleDenied(Exception ex) {
        return build(ErrorCode.ACCESS_DENIED, null, null);
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ApiError> handleAuth(AuthenticationException ex) {
        return build(ErrorCode.UNAUTHENTICATED, null, null);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> handleOther(Exception ex) {
        log.error("Erreur inattendue", ex);          // la trace reste côté serveur
        return build(ErrorCode.INTERNAL_ERROR, null, null);  // message générique côté client
    }
}
```

> `AuthorizationDeniedException` est une sous-classe d'`AccessDeniedException` (Spring Security 6.3+) ; si votre version ne la connaît pas, retirez-la de la liste.

#### Étape 6 — `common/web/PageResponse.java` (pas l'enveloppe de Spring)

```java
package tn.oliveconnect.common.web;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <D, T> PageResponse<T> from(Page<D> page, Function<D, T> mapper) {
        return new PageResponse<>(page.getContent().stream().map(mapper).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
```

#### Étape 7 — `common/web/GeoPointDto.java` + conversion (utilisé par vergers, alertes, carte, pressoirs)

```java
package tn.oliveconnect.common.web;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import tn.oliveconnect.common.error.ApiException;

/** GeoJSON Point : coordinates = [longitude, latitude] (PAS l'inverse). */
public record GeoPointDto(
        @NotNull @Pattern(regexp = "Point", message = "Doit valoir Point") String type,
        @NotNull @Size(min = 2, max = 2, message = "Doit contenir [longitude, latitude]") List<Double> coordinates) {

    public static GeoPointDto from(GeoJsonPoint p) {
        return p == null ? null : new GeoPointDto("Point", List.of(p.getX(), p.getY()));
    }

    /** Valide les bornes puis convertit ; field = nom du champ pour fieldErrors (ex. "location"). */
    public GeoJsonPoint toGeoJson(String field) {
        Double lon = coordinates.get(0), lat = coordinates.get(1);
        if (lon == null || lat == null || lon < -180 || lon > 180 || lat < -90 || lat > 90) {
            throw ApiException.validation(field + ".coordinates",
                    "Longitude dans [-180, 180] et latitude dans [-90, 90]");
        }
        return new GeoJsonPoint(lon, lat);   // x = longitude, y = latitude
    }
}
```

> Spring Data `GeoJsonPoint` se sérialise mal en JSON (il ajoute `x`, `y`…). On ne le renvoie **jamais** : on passe toujours par `GeoPointDto` (règle « controller → DTO »).

#### Étape 8 — Test A2 (enveloppe exacte)

Base commune des tests d'intégration (utilisée aussi par B) — `src/test/java/tn/oliveconnect/AbstractIntegrationTest.java` :

```java
package tn.oliveconnect;

import com.jayway.jsonpath.JsonPath;
import java.util.Map;
import java.util.Set;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
public abstract class AbstractIntegrationTest {

    @Autowired protected MockMvc mvc;
    @Autowired protected MongoTemplate mongo;

    /** Vide les collections SANS supprimer les index (les tests d'unicité en ont besoin). */
    @BeforeEach
    void cleanDatabase() {
        mongo.getCollectionNames().forEach(c -> mongo.getCollection(c).deleteMany(new Document()));
    }

    /** Test de contrat : les clés JSON de premier niveau sont EXACTEMENT celles attendues. */
    protected static void assertExactKeys(String json, String... expected) {
        Map<String, Object> root = JsonPath.read(json, "$");
        assertThat(root.keySet()).containsExactlyInAnyOrderElementsOf(Set.of(expected));
    }
}
```

Contrôleur **de test uniquement** (dans `src/test`, donc jamais livré) — `src/test/java/tn/oliveconnect/common/ErrorProbeController.java` :

```java
package tn.oliveconnect.common;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.*;
import tn.oliveconnect.common.error.ApiException;

@RestController
@RequestMapping("/test-probe")
class ErrorProbeController {
    record Body(@Positive(message = "Doit être > 0") int estimatedQuantityKg) {}

    @PostMapping("/validation") void validation(@Valid @RequestBody Body body) {}
    @GetMapping("/not-found") void notFound() { throw ApiException.notFound(null); }
    @GetMapping("/conflict") void conflict() { throw ApiException.invalidTransition(null); }
}
```

`src/test/java/tn/oliveconnect/common/ErrorEnvelopeTest.java` :

```java
package tn.oliveconnect.common;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import tn.oliveconnect.AbstractIntegrationTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WithMockUser   // le probe n'est pas public : on simule un utilisateur connecté
class ErrorEnvelopeTest extends AbstractIntegrationTest {

    private static final String[] KEYS = {"timestamp", "status", "code", "message", "fieldErrors"};

    @Test
    void badRequest_hasContractEnvelope() throws Exception {
        String json = mvc.perform(post("/test-probe/validation").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estimatedQuantityKg\": 0}")
                        // csrf() : nécessaire tant que la config par défaut de Spring Security est active (avant A4)
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.estimatedQuantityKg").value("Doit être > 0"))
                .andReturn().getResponse().getContentAsString();
        assertExactKeys(json, KEYS);
    }

    @Test
    void notFound_hasContractEnvelope() throws Exception {
        String json = mvc.perform(get("/test-probe/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.fieldErrors").isMap())
                .andReturn().getResponse().getContentAsString();
        assertExactKeys(json, KEYS);
    }

    @Test
    void conflict_hasContractEnvelope() throws Exception {
        String json = mvc.perform(get("/test-probe/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATE_TRANSITION"))
                .andReturn().getResponse().getContentAsString();
        assertExactKeys(json, KEYS);
    }
}
```

Ajouter `spring-security-test` en scope test si absent du `pom.xml` (Initializr l'ajoute avec Security) :

```xml
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-test</artifactId>
    <scope>test</scope>
</dependency>
```

```powershell
cd backend; .\mvnw.cmd test     # Docker Desktop doit tourner (Testcontainers)
```

---

### A3 · Comptes et connexion (5 h) — J3

**Preuve** : tests — email en double → 409 ; mot de passe jamais en clair en base.

Endpoints (§6) : `POST /auth/register` (public, 201 / 400, 409) · `POST /auth/login` (public, 200 / 400, 401, 429) · `GET /users/me` (connecté, 200 / 401).

#### Étape 1 — Document : `user/document/UserDocument.java`

```java
package tn.oliveconnect.user.document;

import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import tn.oliveconnect.common.enums.Role;

@Document("users")
@Getter @Setter @NoArgsConstructor
public class UserDocument {
    @Id private String id;
    private String fullName;
    private String email;          // toujours normalisé (trim + minuscules) ; index unique (B3)
    private String passwordHash;   // BCrypt, jamais le mot de passe en clair
    private String phone;
    private Role role;
    private boolean active;
    @CreatedDate private Instant createdAt;
    @LastModifiedDate private Instant updatedAt;
}
```

#### Étape 2 — Repository : `user/repository/UserRepository.java`

```java
package tn.oliveconnect.user.repository;

import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import tn.oliveconnect.user.document.UserDocument;

public interface UserRepository extends MongoRepository<UserDocument, String> {
    Optional<UserDocument> findByEmail(String email);
    boolean existsByEmail(String email);
}
```

#### Étape 3 — DTOs (`user/dto/`) — clés **exactement** celles du contrat

```java
package tn.oliveconnect.user.dto;

import jakarta.validation.constraints.*;

public record RegisterRequest(
        @NotBlank(message = "Obligatoire") @Size(max = 100, message = "100 caractères maximum") String fullName,
        @NotBlank(message = "Obligatoire") @Email(message = "Email invalide") String email,
        @NotBlank(message = "Obligatoire") @Size(min = 8, message = "8 caractères minimum") String password,
        @Size(max = 30, message = "30 caractères maximum") String phone) {}
```

```java
public record LoginRequest(
        @NotBlank(message = "Obligatoire") @Email(message = "Email invalide") String email,
        @NotBlank(message = "Obligatoire") String password) {}
```

```java
package tn.oliveconnect.user.dto;

import java.time.Instant;
import tn.oliveconnect.common.enums.Role;
import tn.oliveconnect.user.document.UserDocument;

/** Contrat §5.5 : id, fullName, email, role, active, createdAt (PAS de phone, PAS de passwordHash). */
public record UserResponse(String id, String fullName, String email, Role role, boolean active, Instant createdAt) {
    public static UserResponse from(UserDocument u) {
        return new UserResponse(u.getId(), u.getFullName(), u.getEmail(), u.getRole(), u.isActive(), u.getCreatedAt());
    }
}
```

```java
public record LoginResponse(String accessToken, String tokenType, long expiresInSeconds, UserResponse user) {}
```

#### Étape 4 — Encodeur de mot de passe : `security/PasswordConfig.java`

```java
package tn.oliveconnect.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();   // hash salé ; coût 10 par défaut
    }
}
```

#### Étape 5 — Génération du token : `security/JwtProperties.java`, `AuthUser.java`, `JwtService.java`

```java
package tn.oliveconnect.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("oliveconnect.jwt")
public record JwtProperties(String secret, long expirationSeconds) {}
```

```java
package tn.oliveconnect.security;

import tn.oliveconnect.common.enums.Role;

/** L'utilisateur connecté, reconstruit à partir du JWT (principal Spring Security). */
public record AuthUser(String id, String email, Role role) {
    public boolean hasRole(Role r) { return role == r; }
    public boolean isStaff() { return role == Role.COLLECTION_MANAGER || role == Role.ADMIN; }
}
```

```java
package tn.oliveconnect.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;
import tn.oliveconnect.common.enums.Role;

@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationSeconds;

    public JwtService(JwtProperties props) {
        if (props.secret() == null || props.secret().getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET absent ou trop court (32 octets minimum pour HS256)");
        }
        this.key = Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8));
        this.expirationSeconds = props.expirationSeconds();
    }

    public long expirationSeconds() { return expirationSeconds; }

    public String generate(String userId, String email, Role role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId)
                .claim("email", email)
                .claim("role", role.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expirationSeconds)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /** Vide si signature invalide, token expiré ou mal formé (verifyWith refuse aussi alg=none). */
    public Optional<AuthUser> parse(String token) {
        try {
            Claims c = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            return Optional.of(new AuthUser(c.getSubject(), c.get("email", String.class),
                    Role.valueOf(c.get("role", String.class))));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
```

#### Étape 6 — Service : `user/service/AuthService.java`

```java
package tn.oliveconnect.user.service;

import java.util.Locale;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import tn.oliveconnect.common.enums.Role;
import tn.oliveconnect.common.error.ApiException;
import tn.oliveconnect.security.AuthUser;
import tn.oliveconnect.security.JwtService;
import tn.oliveconnect.user.document.UserDocument;
import tn.oliveconnect.user.dto.*;
import tn.oliveconnect.user.repository.UserRepository;

@Service
public class AuthService {

    private static final String BAD_CREDENTIALS = "Email ou mot de passe incorrect.";

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users; this.encoder = encoder; this.jwt = jwt;
    }

    public static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    public UserResponse register(RegisterRequest req) {
        String email = normalizeEmail(req.email());
        if (users.existsByEmail(email)) {
            throw ApiException.duplicate("email", "Cet email est déjà utilisé.");
        }
        UserDocument u = new UserDocument();
        u.setFullName(req.fullName().trim());
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(req.password()));
        u.setPhone(req.phone());
        u.setRole(Role.FARMER);   // imposé : l'inscription publique ne crée que des agriculteurs
        u.setActive(true);
        try {
            return UserResponse.from(users.save(u));
        } catch (DuplicateKeyException e) {   // course entre deux inscriptions simultanées
            throw ApiException.duplicate("email", "Cet email est déjà utilisé.");
        }
    }

    public LoginResponse login(LoginRequest req) {
        UserDocument u = users.findByEmail(normalizeEmail(req.email()))
                .filter(UserDocument::isActive)
                .filter(x -> encoder.matches(req.password(), x.getPasswordHash()))
                .orElseThrow(() -> ApiException.unauthenticated(BAD_CREDENTIALS)); // même message : on ne révèle rien
        String token = jwt.generate(u.getId(), u.getEmail(), u.getRole());
        return new LoginResponse(token, "Bearer", jwt.expirationSeconds(), UserResponse.from(u));
    }

    public UserResponse me(AuthUser me) {
        return users.findById(me.id()).map(UserResponse::from)
                .orElseThrow(() -> ApiException.unauthenticated(null));
    }
}
```

> Le 429 (5 échecs / 15 min, compteur Redis) est la tâche **A18 en S3** : ne pas le faire maintenant (Redis n'arrive qu'en S3).

#### Étape 7 — Controllers : `user/controller/AuthController.java` et `UserController.java`

```java
package tn.oliveconnect.user.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import tn.oliveconnect.user.dto.*;
import tn.oliveconnect.user.service.AuthService;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService auth;
    public AuthController(AuthService auth) { this.auth = auth; }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest req) { return auth.register(req); }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) { return auth.login(req); }
}
```

```java
package tn.oliveconnect.user.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tn.oliveconnect.security.AuthUser;
import tn.oliveconnect.user.dto.UserResponse;
import tn.oliveconnect.user.service.AuthService;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final AuthService auth;
    public UserController(AuthService auth) { this.auth = auth; }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AuthUser me) { return auth.me(me); }
}
```

> `/users/me` ne marche réellement qu'une fois le filtre JWT branché (A4). Les tests A3 sont écrits maintenant et passent après A4.

---

### A4 · Sécurité JWT (4 h) — J4

**Preuve** : tests — sans token → 401, mauvais rôle → 403.

#### Étape 1 — Filtre : `security/JwtAuthenticationFilter.java`

```java
package tn.oliveconnect.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Lit "Authorization: Bearer <jwt>". Token valide -> utilisateur authentifié avec ROLE_<role>.
 * Token absent/invalide -> requête anonyme ; Spring Security renverra 401 si l'endpoint est protégé.
 * Volontairement PAS un @Component (sinon Spring Boot l'enregistrerait une 2e fois comme filtre servlet).
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwt;
    public JwtAuthenticationFilter(JwtService jwt) { this.jwt = jwt; }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String header = req.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            jwt.parse(header.substring(7)).ifPresent(user -> {
                var auth = new UsernamePasswordAuthenticationToken(user, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + user.role().name())));
                SecurityContextHolder.getContext().setAuthentication(auth);
            });
        }
        chain.doFilter(req, res);   // ne JAMAIS journaliser le header (token)
    }
}
```

#### Étape 2 — 401 / 403 au format du contrat depuis le filtre de sécurité

Les erreurs levées **dans la chaîne de filtres** n'atteignent pas le `@RestControllerAdvice`. On les **délègue** au `HandlerExceptionResolver` de Spring MVC, qui appelle notre `GlobalExceptionHandler` → une seule source de format.

```java
package tn.oliveconnect.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Component
public class RestSecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final HandlerExceptionResolver resolver;

    public RestSecurityErrorHandler(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        this.resolver = resolver;
    }

    @Override   // non authentifié -> 401 UNAUTHENTICATED
    public void commence(HttpServletRequest req, HttpServletResponse res, AuthenticationException ex) {
        resolver.resolveException(req, res, null, ex);
    }

    @Override   // authentifié mais refusé par la configuration d'URL -> 403 ACCESS_DENIED
    public void handle(HttpServletRequest req, HttpServletResponse res, AccessDeniedException ex) {
        resolver.resolveException(req, res, null, ex);
    }
}
```

#### Étape 3 — `security/SecurityConfig.java`

```java
package tn.oliveconnect.security;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity   // active @PreAuthorize (niveau 1 : le rôle)
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwt,
                                            RestSecurityErrorHandler errors) throws Exception {
        http
            .csrf(csrf -> csrf.disable())                       // API stateless avec Bearer, pas de cookie
            .cors(Customizer.withDefaults())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .httpBasic(b -> b.disable())
            .formLogin(f -> f.disable())
            .authorizeHttpRequests(auth -> auth
                // Les SEULS endpoints sans token (§6) :
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/register", "/api/v1/auth/login").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/orchards/map").permitAll()
                .requestMatchers("/actuator/health", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/error").permitAll()
                .anyRequest().authenticated())
            .exceptionHandling(e -> e.authenticationEntryPoint(errors).accessDeniedHandler(errors))
            .addFilterBefore(new JwtAuthenticationFilter(jwt), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** CORS limité à l'origine du front (durci en S3, A18). En dev, le proxy Vite rend CORS inutile. */
    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${oliveconnect.cors.allowed-origins}") List<String> origins) {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(origins);
        cfg.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cfg);
        return source;
    }
}
```

**Les deux niveaux de sécurité** (plan §3.4) :

| Niveau | Où | Résultat si refusé |
|---|---|---|
| 1. Rôle | `@PreAuthorize("hasRole('FARMER')")` sur le controller | **403** ACCESS_DENIED |
| 2. Propriété | dans le **service** : `farmerId == me.id()` | **404** NOT_FOUND (on ne révèle pas l'existence) |

#### Étape 4 — Aide aux tests : ajouter dans `AbstractIntegrationTest`

```java
    @Autowired protected tn.oliveconnect.user.repository.UserRepository userRepository;
    @Autowired protected org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    @Autowired protected tn.oliveconnect.security.JwtService jwtService;

    /** Crée un compte du rôle donné et renvoie "Bearer <jwt>". */
    protected String bearer(tn.oliveconnect.common.enums.Role role, String email) {
        var u = new tn.oliveconnect.user.document.UserDocument();
        u.setFullName("Test " + role);
        u.setEmail(email);
        u.setPasswordHash(passwordEncoder.encode("Secret#123"));
        u.setRole(role);
        u.setActive(true);
        u = userRepository.save(u);
        return "Bearer " + jwtService.generate(u.getId(), u.getEmail(), u.getRole());
    }
```

#### Étape 5 — Tests A3 + A4 : `src/test/java/tn/oliveconnect/user/AuthIntegrationTest.java`

```java
package tn.oliveconnect.user;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import tn.oliveconnect.AbstractIntegrationTest;
import tn.oliveconnect.common.enums.Role;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthIntegrationTest extends AbstractIntegrationTest {

    private static final String REGISTER = """
        {"fullName":"Example Farmer","email":"Farmer@Example.test","password":"Secret#123","phone":null}""";

    @Test
    void register_returns201_withExactUserResponse_andForcedFarmerRole() throws Exception {
        String json = mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(REGISTER))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("farmer@example.test"))   // normalisé
                .andExpect(jsonPath("$.role").value("FARMER"))
                .andExpect(jsonPath("$.active").value(true))
                .andReturn().getResponse().getContentAsString();
        assertExactKeys(json, "id", "fullName", "email", "role", "active", "createdAt");
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(REGISTER))
           .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER.replace("Farmer@Example.test", "  farmer@example.TEST ")))
           .andExpect(status().isConflict())
           .andExpect(jsonPath("$.code").value("DUPLICATE_RESOURCE"))
           .andExpect(jsonPath("$.fieldErrors.email").exists());
    }

    @Test
    void password_isNeverStoredInClear() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(REGISTER))
           .andExpect(status().isCreated());
        var stored = userRepository.findByEmail("farmer@example.test").orElseThrow();
        assertThat(stored.getPasswordHash()).isNotEqualTo("Secret#123").startsWith("$2");  // BCrypt
        assertThat(passwordEncoder.matches("Secret#123", stored.getPasswordHash())).isTrue();
    }

    @Test
    void register_shortPassword_returns400() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER.replace("Secret#123", "short")))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    void login_ok_thenMe() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(REGISTER));
        String json = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"farmer@example.test\",\"password\":\"Secret#123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresInSeconds").value(7200))
                .andReturn().getResponse().getContentAsString();
        assertExactKeys(json, "accessToken", "tokenType", "expiresInSeconds", "user");
        String token = com.jayway.jsonpath.JsonPath.read(json, "$.accessToken");
        mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + token))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.email").value("farmer@example.test"));
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(REGISTER));
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"farmer@example.test\",\"password\":\"WrongPass1\"}"))
           .andExpect(status().isUnauthorized())
           .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void noToken_returns401_withContractEnvelope() throws Exception {
        String json = mvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
                .andReturn().getResponse().getContentAsString();
        assertExactKeys(json, "timestamp", "status", "code", "message", "fieldErrors");
    }

    @Test
    void invalidToken_returns401() throws Exception {
        mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer abc.def.ghi"))
           .andExpect(status().isUnauthorized());
    }

    @Test
    void wrongRole_returns403() throws Exception {
        // POST /orchards est réservé à FARMER (§6) : un responsable de collecte reçoit 403
        String manager = bearer(Role.COLLECTION_MANAGER, "manager@example.test");
        mvc.perform(post("/api/v1/orchards").header("Authorization", manager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                          {"name":"X","areaHectares":1,"oliveVariety":"Chemlali",
                           "location":{"type":"Point","coordinates":[10.76,34.73]},"publicVisibility":true}"""))
           .andExpect(status().isForbidden())
           .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }
}
```

> Le test `wrongRole_returns403` passe dès que A5 (vergers) est fusionné.

---

### A5 · Vergers (5 h) — J4/J5

**Preuve** : test — l'agriculteur A ne lit pas le verger de l'agriculteur B (404).

Endpoints (§6) : `POST /orchards` (FARMER) · `GET /orchards/mine` (FARMER) · `GET /orchards/{id}` (propriétaire ; COLLECTION_MANAGER, ADMIN) · `PATCH /orchards/{id}` (propriétaire ; COLLECTION_MANAGER, ADMIN + maturityStatus).

Règles (§5.5) : `areaHectares > 0`, `name ≤ 100`, coordonnées dans [-180,180] / [-90,90]. L'agriculteur modifie nom, surface, variété, position, visibilité ; **seuls le responsable de collecte et l'admin modifient `maturityStatus`**.

#### Étape 1 — `orchard/document/OrchardDocument.java`

```java
package tn.oliveconnect.orchard.document;

import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;
import tn.oliveconnect.common.enums.MaturityStatus;

@Document("orchards")
@Getter @Setter @NoArgsConstructor
public class OrchardDocument {
    @Id private String id;
    @Field(targetType = FieldType.OBJECT_ID) private String farmerId;   // référence (règle R2 de B1)
    private String name;
    private Double areaHectares;
    private String oliveVariety;
    private GeoJsonPoint location;          // { type: "Point", coordinates: [lon, lat] } + index 2dsphere (B3)
    private MaturityStatus maturityStatus;
    private boolean publicVisibility;
    private String externalId;              // interne : idempotence des imports (S3), jamais dans un DTO
    @CreatedDate private Instant createdAt;
    @LastModifiedDate private Instant updatedAt;
}
```

#### Étape 2 — `orchard/repository/OrchardRepository.java`

```java
package tn.oliveconnect.orchard.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import tn.oliveconnect.orchard.document.OrchardDocument;

public interface OrchardRepository extends MongoRepository<OrchardDocument, String> {
    Page<OrchardDocument> findByFarmerId(String farmerId, Pageable pageable);
}
```

#### Étape 3 — DTOs `orchard/dto/`

```java
package tn.oliveconnect.orchard.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import tn.oliveconnect.common.web.GeoPointDto;

public record OrchardCreateRequest(
        @NotBlank(message = "Obligatoire") @Size(max = 100, message = "100 caractères maximum") String name,
        @NotNull(message = "Obligatoire") @Positive(message = "Doit être > 0") Double areaHectares,
        @NotBlank(message = "Obligatoire") @Size(max = 50) String oliveVariety,
        @NotNull(message = "Obligatoire") @Valid GeoPointDto location,
        @NotNull(message = "Obligatoire") Boolean publicVisibility) {}
```

```java
// imports : jakarta.validation.Valid, jakarta.validation.constraints.*, tn.oliveconnect.common.enums.MaturityStatus, tn.oliveconnect.common.web.GeoPointDto
/** PATCH : même corps, tous les champs optionnels (null = inchangé). */
public record OrchardPatchRequest(
        @Size(min = 1, max = 100, message = "1 à 100 caractères") String name,
        @Positive(message = "Doit être > 0") Double areaHectares,
        @Size(min = 1, max = 50) String oliveVariety,
        @Valid GeoPointDto location,
        Boolean publicVisibility,
        MaturityStatus maturityStatus) {}    // réservé COLLECTION_MANAGER / ADMIN
```

```java
package tn.oliveconnect.orchard.dto;

import java.time.Instant;
import tn.oliveconnect.common.enums.MaturityStatus;
import tn.oliveconnect.common.web.GeoPointDto;
import tn.oliveconnect.orchard.document.OrchardDocument;

/** Contrat §5.5 OrchardResponse — 10 clés exactement. */
public record OrchardResponse(String id, String farmerId, String name, Double areaHectares, String oliveVariety,
                              GeoPointDto location, MaturityStatus maturityStatus, boolean publicVisibility,
                              Instant createdAt, Instant updatedAt) {
    public static OrchardResponse from(OrchardDocument o) {
        return new OrchardResponse(o.getId(), o.getFarmerId(), o.getName(), o.getAreaHectares(), o.getOliveVariety(),
                GeoPointDto.from(o.getLocation()), o.getMaturityStatus(), o.isPublicVisibility(),
                o.getCreatedAt(), o.getUpdatedAt());
    }
}
```

#### Étape 4 — `orchard/service/OrchardService.java` (autorisation niveau 2 + invalidation)

```java
package tn.oliveconnect.orchard.service;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import tn.oliveconnect.common.enums.MaturityStatus;
import tn.oliveconnect.common.enums.Role;
import tn.oliveconnect.common.error.ApiException;
import tn.oliveconnect.common.facade.CacheInvalidator;
import tn.oliveconnect.common.web.PageResponse;
import tn.oliveconnect.orchard.document.OrchardDocument;
import tn.oliveconnect.orchard.dto.*;
import tn.oliveconnect.orchard.repository.OrchardRepository;
import tn.oliveconnect.security.AuthUser;

@Service
public class OrchardService {

    private static final String NOT_FOUND = "Verger introuvable.";
    private final OrchardRepository orchards;
    private final CacheInvalidator cache;   // implémentation vide de B en S1 (B4), réelle en S3

    public OrchardService(OrchardRepository orchards, CacheInvalidator cache) {
        this.orchards = orchards; this.cache = cache;
    }

    public OrchardResponse create(AuthUser me, OrchardCreateRequest req) {
        OrchardDocument o = new OrchardDocument();
        o.setFarmerId(me.id());                       // TOUJOURS depuis le token
        o.setName(req.name().trim());
        o.setAreaHectares(req.areaHectares());
        o.setOliveVariety(req.oliveVariety().trim());
        o.setLocation(req.location().toGeoJson("location"));
        o.setMaturityStatus(MaturityStatus.UNKNOWN);  // valeur serveur
        o.setPublicVisibility(req.publicVisibility());
        o = orchards.save(o);
        cache.orchardChanged(o.getId());
        cache.mapChanged();
        return OrchardResponse.from(o);
    }

    public PageResponse<OrchardResponse> mine(AuthUser me, Pageable pageable) {
        return PageResponse.from(orchards.findByFarmerId(me.id(), pageable), OrchardResponse::from);
    }

    public OrchardResponse get(AuthUser me, String id) {
        return OrchardResponse.from(loadReadable(me, id));
    }

    public OrchardResponse patch(AuthUser me, String id, OrchardPatchRequest req) {
        OrchardDocument o = loadReadable(me, id);
        if (req.maturityStatus() != null && me.hasRole(Role.FARMER)) {
            throw ApiException.accessDenied("Seul le responsable de collecte peut modifier la maturité.");
        }
        if (req.name() != null) o.setName(req.name().trim());
        if (req.areaHectares() != null) o.setAreaHectares(req.areaHectares());
        if (req.oliveVariety() != null) o.setOliveVariety(req.oliveVariety().trim());
        if (req.location() != null) o.setLocation(req.location().toGeoJson("location"));
        if (req.publicVisibility() != null) o.setPublicVisibility(req.publicVisibility());
        if (req.maturityStatus() != null) o.setMaturityStatus(req.maturityStatus());
        o = orchards.save(o);
        cache.orchardChanged(o.getId());
        cache.mapChanged();
        return OrchardResponse.from(o);
    }

    /** Niveau 2 : un agriculteur ne voit que les siens -> 404 sinon (on ne révèle pas l'existence). */
    private OrchardDocument loadReadable(AuthUser me, String id) {
        OrchardDocument o = orchards.findById(id).orElseThrow(() -> ApiException.notFound(NOT_FOUND));
        if (me.isStaff()) return o;
        if (me.hasRole(Role.FARMER) && me.id().equals(o.getFarmerId())) return o;
        throw ApiException.notFound(NOT_FOUND);
    }
}
```

> Un `id` mal formé (pas 24 hex) : `findById` renvoie vide → 404. C'est le comportement voulu.

#### Étape 5 — `orchard/controller/OrchardController.java` (niveau 1 : rôles)

```java
package tn.oliveconnect.orchard.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tn.oliveconnect.common.web.PageResponse;
import tn.oliveconnect.orchard.dto.*;
import tn.oliveconnect.orchard.service.OrchardService;
import tn.oliveconnect.security.AuthUser;

@RestController
@RequestMapping("/api/v1/orchards")
public class OrchardController {

    private final OrchardService service;
    public OrchardController(OrchardService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('FARMER')")
    public OrchardResponse create(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody OrchardCreateRequest req) {
        return service.create(me, req);
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('FARMER')")
    public PageResponse<OrchardResponse> mine(@AuthenticationPrincipal AuthUser me,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.mine(me, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('FARMER','COLLECTION_MANAGER','ADMIN')")
    public OrchardResponse get(@AuthenticationPrincipal AuthUser me, @PathVariable String id) {
        return service.get(me, id);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('FARMER','COLLECTION_MANAGER','ADMIN')")
    public OrchardResponse patch(@AuthenticationPrincipal AuthUser me, @PathVariable String id,
                                 @Valid @RequestBody OrchardPatchRequest req) {
        return service.patch(me, id, req);
    }
    // GET /orchards/map appartient à B (B7, semaine 2)
}
```

#### Étape 6 — Tests `src/test/java/tn/oliveconnect/orchard/OrchardIntegrationTest.java`

```java
package tn.oliveconnect.orchard;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import tn.oliveconnect.AbstractIntegrationTest;
import tn.oliveconnect.common.enums.Role;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class OrchardIntegrationTest extends AbstractIntegrationTest {

    static final String BODY = """
        {"name":"North Orchard","areaHectares":4.2,"oliveVariety":"Chemlali",
         "location":{"type":"Point","coordinates":[10.76,34.73]},"publicVisibility":true}""";

    String create(String token) throws Exception {
        String json = mvc.perform(post("/api/v1/orchards").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.id");
    }

    @Test
    void create_returnsContractShape_lonLatOrderKept() throws Exception {
        String farmer = bearer(Role.FARMER, "a@example.test");
        String json = mvc.perform(post("/api/v1/orchards").header("Authorization", farmer)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.maturityStatus").value("UNKNOWN"))
                .andExpect(jsonPath("$.location.coordinates[0]").value(10.76))   // longitude d'abord
                .andExpect(jsonPath("$.location.coordinates[1]").value(34.73))
                .andReturn().getResponse().getContentAsString();
        assertExactKeys(json, "id", "farmerId", "name", "areaHectares", "oliveVariety", "location",
                "maturityStatus", "publicVisibility", "createdAt", "updatedAt");
    }

    @Test
    void farmerA_cannotReadFarmerB_orchard_404() throws Exception {
        String farmerA = bearer(Role.FARMER, "a@example.test");
        String farmerB = bearer(Role.FARMER, "b@example.test");
        String idOfA = create(farmerA);
        mvc.perform(get("/api/v1/orchards/" + idOfA).header("Authorization", farmerB))
           .andExpect(status().isNotFound())
           .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        mvc.perform(patch("/api/v1/orchards/" + idOfA).header("Authorization", farmerB)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Hack\"}"))
           .andExpect(status().isNotFound());
    }

    @Test
    void manager_canReadAnyOrchard_andSetMaturity() throws Exception {
        String id = create(bearer(Role.FARMER, "a@example.test"));
        String manager = bearer(Role.COLLECTION_MANAGER, "m@example.test");
        mvc.perform(patch("/api/v1/orchards/" + id).header("Authorization", manager)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"maturityStatus\":\"OPTIMAL\"}"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.maturityStatus").value("OPTIMAL"));
    }

    @Test
    void farmer_cannotSetMaturity_403() throws Exception {
        String farmer = bearer(Role.FARMER, "a@example.test");
        String id = create(farmer);
        mvc.perform(patch("/api/v1/orchards/" + id).header("Authorization", farmer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"maturityStatus\":\"OPTIMAL\"}"))
           .andExpect(status().isForbidden());
    }

    @Test
    void invalidArea_andCoordinates_return400() throws Exception {
        String farmer = bearer(Role.FARMER, "a@example.test");
        mvc.perform(post("/api/v1/orchards").header("Authorization", farmer)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY.replace("4.2", "0")))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.fieldErrors.areaHectares").exists());
        mvc.perform(post("/api/v1/orchards").header("Authorization", farmer)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY.replace("34.73", "95")))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.fieldErrors['location.coordinates']").exists());
    }

    @Test
    void mine_returnsOnlyOwnOrchards_inPageResponse() throws Exception {
        String a = bearer(Role.FARMER, "a@example.test");
        create(a); create(bearer(Role.FARMER, "b@example.test"));
        String json = mvc.perform(get("/api/v1/orchards/mine").header("Authorization", a))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andReturn().getResponse().getContentAsString();
        assertExactKeys(json, "content", "page", "size", "totalElements", "totalPages");
    }
}
```

---

### A6 · Demandes de récolte — dépôt (4 h) — J5

**Preuve** : tests — quantité ≤ 0 → 400 ; un `farmerId` envoyé dans le corps est ignoré.

Endpoints S1 (§6) : `POST /harvest-requests` (FARMER) · `GET /harvest-requests/mine` (FARMER) · `GET /harvest-requests` (COLLECTION_MANAGER, ADMIN ; filtres `status`, `from`, `to`) · `GET /harvest-requests/{id}` (propriétaire ; COLLECTION_MANAGER, ADMIN).
`review`, `cancel`, `PATCH` = **semaine 2** (A8).

#### Étape 1 — `harvest/document/HarvestRequestDocument.java`

```java
package tn.oliveconnect.harvest.document;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;
import tn.oliveconnect.common.enums.HarvestStatus;
import tn.oliveconnect.common.enums.HarvestType;

@Document("harvest_requests")
@Getter @Setter @NoArgsConstructor
public class HarvestRequestDocument {
    @Id private String id;
    @Field(targetType = FieldType.OBJECT_ID) private String farmerId;
    @Field(targetType = FieldType.OBJECT_ID) private String orchardId;
    private String orchardName;               // copie à la création (« duplication contrôlée », B1)
    private HarvestType type;
    private String urgencyReason;
    private HarvestStatus status;
    private Instant requestedAt;
    private LocalDate preferredDate;
    @Field(targetType = FieldType.DECIMAL128) private BigDecimal estimatedQuantityKg;  // nombre, agrégeable
    @Field(targetType = FieldType.DECIMAL128) private BigDecimal actualQuantityKg;
    @Field(targetType = FieldType.OBJECT_ID) private String reviewedBy;
    private String reviewNote;
    @Field(targetType = FieldType.OBJECT_ID) private String scheduledTourId;
    private Instant scheduledAt;
    private Instant completedAt;
    @CreatedDate private Instant createdAt;
    @LastModifiedDate private Instant updatedAt;
}
```

> Sans `DECIMAL128`, Spring Data stocke un `BigDecimal` en **chaîne** : les sommes du dashboard (B11) seraient impossibles.

#### Étape 2 — Repositories `harvest/repository/`

```java
package tn.oliveconnect.harvest.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import tn.oliveconnect.harvest.document.HarvestRequestDocument;

public interface HarvestRequestRepository extends MongoRepository<HarvestRequestDocument, String> {
    Page<HarvestRequestDocument> findByFarmerId(String farmerId, Pageable pageable);
}
```

```java
package tn.oliveconnect.harvest.repository;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import tn.oliveconnect.common.enums.HarvestStatus;
import tn.oliveconnect.harvest.document.HarvestRequestDocument;

/** Filtres dynamiques de la file du responsable (DAO : uniquement de la requête). */
@Repository
public class HarvestRequestSearchRepository {
    private final MongoTemplate mongo;
    public HarvestRequestSearchRepository(MongoTemplate mongo) { this.mongo = mongo; }

    public Page<HarvestRequestDocument> search(HarvestStatus status, LocalDate from, LocalDate to, Pageable pageable) {
        Query q = new Query();
        if (status != null) q.addCriteria(Criteria.where("status").is(status));
        if (from != null || to != null) {
            Criteria c = Criteria.where("preferredDate");
            if (from != null) c = c.gte(from);
            if (to != null) c = c.lte(to);
            q.addCriteria(c);
        }
        long total = mongo.count(q, HarvestRequestDocument.class);
        List<HarvestRequestDocument> content = mongo.find(Query.of(q).with(pageable), HarvestRequestDocument.class);
        return new PageImpl<>(content, pageable, total);
    }
}
```

#### Étape 3 — DTOs `harvest/dto/`

```java
package tn.oliveconnect.harvest.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import tn.oliveconnect.common.enums.HarvestType;

/** farmerId, status, etc. ne sont PAS des champs du record : s'ils sont envoyés, Jackson les ignore. */
public record HarvestRequestCreateRequest(
        @NotBlank(message = "Obligatoire") String orchardId,
        @NotNull(message = "Obligatoire") HarvestType type,
        @NotNull(message = "Obligatoire") LocalDate preferredDate,
        @NotNull(message = "Obligatoire") @Positive(message = "Doit être > 0") BigDecimal estimatedQuantityKg,
        @Size(max = 500, message = "500 caractères maximum") String urgencyReason) {}
```

```java
package tn.oliveconnect.harvest.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import tn.oliveconnect.common.enums.HarvestStatus;
import tn.oliveconnect.common.enums.HarvestType;
import tn.oliveconnect.harvest.document.HarvestRequestDocument;

/** Contrat §5.5 HarvestRequestResponse — 18 clés exactement. */
public record HarvestRequestResponse(
        String id, String farmerId, String orchardId, String orchardName, HarvestType type, String urgencyReason,
        HarvestStatus status, Instant requestedAt, LocalDate preferredDate, BigDecimal estimatedQuantityKg,
        BigDecimal actualQuantityKg, String reviewedBy, String reviewNote, String scheduledTourId,
        Instant scheduledAt, Instant completedAt, Instant createdAt, Instant updatedAt) {

    public static HarvestRequestResponse from(HarvestRequestDocument d) {
        return new HarvestRequestResponse(d.getId(), d.getFarmerId(), d.getOrchardId(), d.getOrchardName(),
                d.getType(), d.getUrgencyReason(), d.getStatus(), d.getRequestedAt(), d.getPreferredDate(),
                d.getEstimatedQuantityKg(), d.getActualQuantityKg(), d.getReviewedBy(), d.getReviewNote(),
                d.getScheduledTourId(), d.getScheduledAt(), d.getCompletedAt(), d.getCreatedAt(), d.getUpdatedAt());
    }
}
```

#### Étape 4 — `harvest/service/HarvestRequestService.java`

```java
package tn.oliveconnect.harvest.service;

import java.time.Instant;
import java.time.LocalDate;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import tn.oliveconnect.common.enums.HarvestStatus;
import tn.oliveconnect.common.enums.HarvestType;
import tn.oliveconnect.common.enums.Role;
import tn.oliveconnect.common.error.ApiException;
import tn.oliveconnect.common.facade.CacheInvalidator;
import tn.oliveconnect.common.web.PageResponse;
import tn.oliveconnect.harvest.document.HarvestRequestDocument;
import tn.oliveconnect.harvest.dto.*;
import tn.oliveconnect.harvest.repository.*;
import tn.oliveconnect.orchard.document.OrchardDocument;
import tn.oliveconnect.orchard.repository.OrchardRepository;
import tn.oliveconnect.security.AuthUser;

@Service
public class HarvestRequestService {

    private static final String NOT_FOUND = "Demande introuvable.";
    private final HarvestRequestRepository requests;
    private final HarvestRequestSearchRepository search;
    private final OrchardRepository orchards;     // même module (A) : accès direct autorisé
    private final CacheInvalidator cache;

    public HarvestRequestService(HarvestRequestRepository requests, HarvestRequestSearchRepository search,
                                 OrchardRepository orchards, CacheInvalidator cache) {
        this.requests = requests; this.search = search; this.orchards = orchards; this.cache = cache;
    }

    public HarvestRequestResponse create(AuthUser me, HarvestRequestCreateRequest req) {
        if (req.type() == HarvestType.URGENT && (req.urgencyReason() == null || req.urgencyReason().isBlank())) {
            throw ApiException.validation("urgencyReason", "Obligatoire pour une demande urgente");
        }
        // Le verger doit exister ET appartenir à l'agriculteur : sinon 404 (§6 : erreurs 400, 403, 404)
        OrchardDocument orchard = orchards.findById(req.orchardId())
                .filter(o -> me.id().equals(o.getFarmerId()))
                .orElseThrow(() -> ApiException.notFound("Verger introuvable."));

        HarvestRequestDocument d = new HarvestRequestDocument();
        d.setFarmerId(me.id());                       // du token, jamais du corps
        d.setOrchardId(orchard.getId());
        d.setOrchardName(orchard.getName());          // duplication contrôlée
        d.setType(req.type());
        d.setUrgencyReason(req.type() == HarvestType.URGENT ? req.urgencyReason().trim() : null);
        d.setStatus(HarvestStatus.SUBMITTED);         // imposé par le serveur
        d.setRequestedAt(Instant.now());
        d.setPreferredDate(req.preferredDate());
        d.setEstimatedQuantityKg(req.estimatedQuantityKg());
        d = requests.save(d);
        cache.orchardChanged(orchard.getId());        // harvestStatus visible sur la carte
        cache.mapChanged();
        return HarvestRequestResponse.from(d);
    }

    public PageResponse<HarvestRequestResponse> mine(AuthUser me, Pageable pageable) {
        return PageResponse.from(requests.findByFarmerId(me.id(), pageable), HarvestRequestResponse::from);
    }

    public PageResponse<HarvestRequestResponse> queue(HarvestStatus status, LocalDate from, LocalDate to, Pageable p) {
        if (from != null && to != null && from.isAfter(to)) {
            throw ApiException.validation("from", "from doit être ≤ to");
        }
        return PageResponse.from(search.search(status, from, to, p), HarvestRequestResponse::from);
    }

    public HarvestRequestResponse get(AuthUser me, String id) {
        HarvestRequestDocument d = requests.findById(id).orElseThrow(() -> ApiException.notFound(NOT_FOUND));
        boolean owner = me.hasRole(Role.FARMER) && me.id().equals(d.getFarmerId());
        if (!owner && !me.isStaff()) throw ApiException.notFound(NOT_FOUND);
        return HarvestRequestResponse.from(d);
    }
}
```

#### Étape 5 — `harvest/controller/HarvestRequestController.java`

```java
package tn.oliveconnect.harvest.controller;

import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tn.oliveconnect.common.enums.HarvestStatus;
import tn.oliveconnect.common.web.PageResponse;
import tn.oliveconnect.harvest.dto.*;
import tn.oliveconnect.harvest.service.HarvestRequestService;
import tn.oliveconnect.security.AuthUser;

@RestController
@RequestMapping("/api/v1/harvest-requests")
public class HarvestRequestController {

    private final HarvestRequestService service;
    public HarvestRequestController(HarvestRequestService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('FARMER')")
    public HarvestRequestResponse create(@AuthenticationPrincipal AuthUser me,
                                         @Valid @RequestBody HarvestRequestCreateRequest req) {
        return service.create(me, req);
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('FARMER')")
    public PageResponse<HarvestRequestResponse> mine(@AuthenticationPrincipal AuthUser me,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.mine(me, pageable);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('COLLECTION_MANAGER','ADMIN')")
    public PageResponse<HarvestRequestResponse> queue(
            @RequestParam(required = false) HarvestStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.queue(status, from, to, pageable);   // status invalide -> 400 (type mismatch)
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('FARMER','COLLECTION_MANAGER','ADMIN')")
    public HarvestRequestResponse get(@AuthenticationPrincipal AuthUser me, @PathVariable String id) {
        return service.get(me, id);
    }
}
```

#### Étape 6 — Tests `src/test/java/tn/oliveconnect/harvest/HarvestRequestIntegrationTest.java`

```java
package tn.oliveconnect.harvest;

import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import tn.oliveconnect.AbstractIntegrationTest;
import tn.oliveconnect.common.enums.Role;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class HarvestRequestIntegrationTest extends AbstractIntegrationTest {

    String farmer; String orchardId;

    void setup() throws Exception {
        farmer = bearer(Role.FARMER, "a@example.test");
        String json = mvc.perform(post("/api/v1/orchards").header("Authorization", farmer)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                          {"name":"North Orchard","areaHectares":4.2,"oliveVariety":"Chemlali",
                           "location":{"type":"Point","coordinates":[10.76,34.73]},"publicVisibility":true}"""))
                .andReturn().getResponse().getContentAsString();
        orchardId = JsonPath.read(json, "$.id");
    }

    String body(String qty, String type, String reason) {
        return """
            {"orchardId":"%s","type":"%s","preferredDate":"%s","estimatedQuantityKg":%s,"urgencyReason":%s,
             "farmerId":"6741a0c2e4b0a1b2c3d4e5f6","status":"COMPLETED"}"""
            .formatted(orchardId, type, LocalDate.now().plusDays(3), qty, reason == null ? "null" : "\"" + reason + "\"");
    }

    @Test
    void create_201_submitted_farmerIdFromToken_bodyFieldsIgnored() throws Exception {
        setup();
        String json = mvc.perform(post("/api/v1/harvest-requests").header("Authorization", farmer)
                        .contentType(MediaType.APPLICATION_JSON).content(body("2500", "URGENT", "Maturité optimale")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUBMITTED"))          // pas COMPLETED
                .andExpect(jsonPath("$.orchardName").value("North Orchard"))
                .andExpect(jsonPath("$.actualQuantityKg").isEmpty())
                .andReturn().getResponse().getContentAsString();
        String farmerId = JsonPath.read(json, "$.farmerId");
        org.assertj.core.api.Assertions.assertThat(farmerId).isNotEqualTo("6741a0c2e4b0a1b2c3d4e5f6");
        assertExactKeys(json, "id", "farmerId", "orchardId", "orchardName", "type", "urgencyReason", "status",
                "requestedAt", "preferredDate", "estimatedQuantityKg", "actualQuantityKg", "reviewedBy", "reviewNote",
                "scheduledTourId", "scheduledAt", "completedAt", "createdAt", "updatedAt");
    }

    @Test
    void quantityZeroOrNegative_400() throws Exception {
        setup();
        for (String q : new String[]{"0", "-5"}) {
            mvc.perform(post("/api/v1/harvest-requests").header("Authorization", farmer)
                            .contentType(MediaType.APPLICATION_JSON).content(body(q, "PLANNED", null)))
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath("$.fieldErrors.estimatedQuantityKg").value("Doit être > 0"));
        }
    }

    @Test
    void urgentWithoutReason_400() throws Exception {
        setup();
        mvc.perform(post("/api/v1/harvest-requests").header("Authorization", farmer)
                        .contentType(MediaType.APPLICATION_JSON).content(body("100", "URGENT", null)))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.fieldErrors.urgencyReason").exists());
    }

    @Test
    void orchardOfAnotherFarmer_404() throws Exception {
        setup();
        String other = bearer(Role.FARMER, "b@example.test");
        mvc.perform(post("/api/v1/harvest-requests").header("Authorization", other)
                        .contentType(MediaType.APPLICATION_JSON).content(body("100", "PLANNED", null)))
           .andExpect(status().isNotFound());
    }

    @Test
    void managerQueue_filtersByStatus_farmerGets403() throws Exception {
        setup();
        mvc.perform(post("/api/v1/harvest-requests").header("Authorization", farmer)
                .contentType(MediaType.APPLICATION_JSON).content(body("100", "PLANNED", null)));
        String manager = bearer(Role.COLLECTION_MANAGER, "m@example.test");
        mvc.perform(get("/api/v1/harvest-requests?status=SUBMITTED").header("Authorization", manager))
           .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/v1/harvest-requests?status=FOO").header("Authorization", manager))
           .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/harvest-requests").header("Authorization", farmer))
           .andExpect(status().isForbidden());
    }
}
```

✅ **A a fini la semaine quand** : `.\mvnw.cmd verify` est vert, la PR est relue par **C**, et la démo `login → verger → demande` passe avec le front.

---

## 4. MEMBRE B — Données, logistique, performance (B1 → B6)

**Preuve à livrer vendredi** : `docker compose up` OK ; `docs/nosql-model.md` relu ; index présents (test Testcontainers) ; CRUD ressources ; seed (`--farmers 20` et `--farmers 500`).

---

### B1 · Modèle NoSQL (`docs/nosql-model.md`) (5 h) — J1 à J3

**Preuve** : relu et validé par A et C avant la fin du jour 3 (sera réutilisé tel quel dans le rapport final en S3, tâche B15).

Créer `docs/nosql-model.md` avec **toutes les sections exigées par le sujet** (règles UML→NoSQL, imbrication vs référence, duplication contrôlée, index candidats, partitionnement/sharding) :

````markdown
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
````

---

### B2 · Pile locale (`infra/compose.yaml` + README) (2 h) — J1

**Preuve** : les 3 machines démarrent la pile en une commande.

#### Étape 1 — Créer `infra/compose.yaml`

> **Sécurité (§3.6 et B15)** : En local, on lie les ports à `127.0.0.1` uniquement (jamais `0.0.0.0`) pour que MongoDB et Redis ne soient pas exposés sur le réseau Wi-Fi/LAN. Sur la VM en S3, on retirera même la section `ports` (seul le réseau interne Docker sera utilisé).

```yaml
name: oliveconnect

services:
  mongodb:
    image: mongo:8.0
    container_name: oliveconnect-mongodb
    restart: unless-stopped
    environment:
      MONGO_INITDB_ROOT_USERNAME: ${MONGO_ROOT_USERNAME:-oliveconnect}
      MONGO_INITDB_ROOT_PASSWORD: ${MONGO_ROOT_PASSWORD:-change-me}
      MONGO_INITDB_DATABASE: oliveconnect
    ports:
      - "127.0.0.1:27017:27017"
    volumes:
      - mongo_data:/data/db
    healthcheck:
      test: ["CMD", "mongosh", "--quiet", "mongodb://localhost:27017/admin", "--eval", "db.runCommand({ ping: 1 }).ok"]
      interval: 5s
      timeout: 5s
      retries: 10

  redis:
    image: redis:7.4-alpine
    container_name: oliveconnect-redis
    restart: unless-stopped
    command: ["redis-server", "--save", "", "--appendonly", "no"]   # cache jetable (§3.3)
    ports:
      - "127.0.0.1:6379:6379"
    volumes:
      - redis_data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 5s
      timeout: 3s
      retries: 10

volumes:
  mongo_data:
  redis_data:
```

#### Étape 2 — Documenter « tout lancer en une commande » dans `README.md`

```markdown
# OliveConnect

Plateforme web de gestion des collectes d'olives (Spring Boot + MongoDB + Redis + React).

## Démarrage rapide (développement local)

1. Copier `.env.example` vers `.env` (ne jamais committer `.env`) :
   ```powershell
   Copy-Item .env.example .env
   ```
2. Démarrer MongoDB et Redis :
   ```powershell
   docker compose -f infra/compose.yaml --env-file .env up -d
   ```
3. Démarrer le backend (port 8080) :
   ```powershell
   cd backend; .\mvnw.cmd spring-boot:run
   ```
4. Charger les données de démo (20 agriculteurs + 1 compte par rôle) :
   ```powershell
   cd backend; .\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--seed --farmers=20"
   ```
5. Démarrer le frontend (port 5173) :
   ```powershell
   cd frontend; npm install; npm run dev
   ```
```

#### Étape 3 — Vérifier sur les 3 machines

```powershell
Copy-Item .env.example .env
docker compose -f infra/compose.yaml --env-file .env up -d
docker compose -f infra/compose.yaml ps
# Les deux conteneurs doivent être "Up (healthy)"
```

---

### B3 · Index versionnés (`MongoIndexConfig`) (4 h) — J3

**Preuve** : test Testcontainers prouvant que l'index `2dsphere` et les index d'unicité sont créés et effectifs.

> Rappel : `auto-index-creation: false` dans `application.yml` (A1). **Tous** les index sont créés de manière explicite et reproductible au démarrage par `MongoIndexConfig`.

#### Étape 1 — `backend/src/main/java/tn/oliveconnect/data/MongoIndexConfig.java`

```java
package tn.oliveconnect.data;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.CompoundIndexDefinition;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeospatialIndex;
import org.springframework.data.mongodb.core.index.Index;
import org.bson.Document;

/**
 * Crée de façon idempotente tous les index décrits dans docs/nosql-model.md (tâche B3).
 */
@Configuration
public class MongoIndexConfig {

    private final MongoTemplate mongo;

    public MongoIndexConfig(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @PostConstruct
    public void ensureIndexes() {
        // 1. users : email unique
        mongo.indexOps("users").createIndex(
                new Index().on("email", Sort.Direction.ASC).unique().named("idx_users_email_uq"));

        // 2. orchards : 2dsphere sur location + index farmerId
        mongo.indexOps("orchards").createIndex(
                new GeospatialIndex("location").typed(GeoSpatialIndexType.GEO_2DSPHERE)
                        .named("idx_orchards_location_2dsphere"));
        mongo.indexOps("orchards").createIndex(
                new Index().on("farmerId", Sort.Direction.ASC).on("createdAt", Sort.Direction.DESC)
                        .named("idx_orchards_farmer"));

        // 3. harvest_requests : 3 index (mes demandes, file responsable, statut par verger)
        mongo.indexOps("harvest_requests").createIndex(
                new Index().on("farmerId", Sort.Direction.ASC).on("createdAt", Sort.Direction.DESC)
                        .named("idx_hr_farmer_created"));
        mongo.indexOps("harvest_requests").createIndex(
                new CompoundIndexDefinition(new Document("status", 1).append("preferredDate", 1).append("createdAt", -1))
                        .named("idx_hr_status_prefdate"));
        mongo.indexOps("harvest_requests").createIndex(
                new Index().on("orchardId", Sort.Direction.ASC).on("status", Sort.Direction.ASC)
                        .named("idx_hr_orchard_status"));

        // 4. field_alerts : {status, priority, createdAt}
        mongo.indexOps("field_alerts").createIndex(
                new CompoundIndexDefinition(new Document("status", 1).append("priority", 1).append("createdAt", -1))
                        .named("idx_alerts_status_prio_created"));

        // 5. tours : 2 index (date + statut pour conflits ; chef d'équipe + date)
        mongo.indexOps("tours").createIndex(
                new Index().on("tourDate", Sort.Direction.ASC).on("status", Sort.Direction.ASC)
                        .named("idx_tours_date_status"));
        mongo.indexOps("tours").createIndex(
                new Index().on("teamLeaderId", Sort.Direction.ASC).on("tourDate", Sort.Direction.DESC)
                        .named("idx_tours_leader_date"));

        // 6. maturity_readings
        mongo.indexOps("maturity_readings").createIndex(
                new Index().on("orchardId", Sort.Direction.ASC).on("observedAt", Sort.Direction.DESC)
                        .named("idx_maturity_orchard_obs"));

        // 7. press_receptions : couple (tourId, binId) unique + date de réception
        mongo.indexOps("press_receptions").createIndex(
                new CompoundIndexDefinition(new Document("tourId", 1).append("binId", 1))
                        .unique().named("idx_pr_tour_bin_uq"));
        mongo.indexOps("press_receptions").createIndex(
                new Index().on("receivedAt", Sort.Direction.DESC).named("idx_pr_received_at"));
    }
}
```

#### Étape 2 — Test d'intégration `src/test/java/tn/oliveconnect/data/MongoIndexConfigTest.java`

```java
package tn.oliveconnect.data;

import java.util.List;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import tn.oliveconnect.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MongoIndexConfigTest extends AbstractIntegrationTest {

    @Test
    void allVersionedIndexesExist() {
        assertThat(indexNames("users")).contains("idx_users_email_uq");
        assertThat(indexNames("orchards")).contains("idx_orchards_location_2dsphere", "idx_orchards_farmer");
        assertThat(indexNames("harvest_requests")).contains(
                "idx_hr_farmer_created", "idx_hr_status_prefdate", "idx_hr_orchard_status");
        assertThat(indexNames("field_alerts")).contains("idx_alerts_status_prio_created");
        assertThat(indexNames("tours")).contains("idx_tours_date_status", "idx_tours_leader_date");
        assertThat(indexNames("maturity_readings")).contains("idx_maturity_orchard_obs");
        assertThat(indexNames("press_receptions")).contains("idx_pr_tour_bin_uq", "idx_pr_received_at");
    }

    @Test
    void geo2dsphereIndex_allowsNearSphereQuery() {
        Document sfaxOrchard = new Document("name", "Sfax")
                .append("location", new Document("type", "Point").append("coordinates", List.of(10.76, 34.73)));
        mongo.getCollection("orchards").insertOne(sfaxOrchard);

        // nearSphere exige un index 2dsphere actif : échoue si l'index n'est pas créé
        Query near = new Query(Criteria.where("location").nearSphere(new Point(10.76, 34.73)).maxDistance(0.01));
        List<Document> found = mongo.find(near, Document.class, "orchards");
        assertThat(found).hasSize(1);
    }

    @Test
    void uniqueIndexes_rejectDuplicates() {
        mongo.getCollection("users").insertOne(new Document("email", "dup@example.test"));
        assertThatThrownBy(() ->
                mongo.getCollection("users").insertOne(new Document("email", "dup@example.test")))
                .isInstanceOf(DuplicateKeyException.class);

        mongo.getCollection("press_receptions").insertOne(new Document("tourId", "t1").append("binId", "b1"));
        assertThatThrownBy(() ->
                mongo.getCollection("press_receptions").insertOne(new Document("tourId", "t1").append("binId", "b1")))
                .isInstanceOf(DuplicateKeyException.class);
    }

    private List<String> indexNames(String collection) {
        return mongo.indexOps(collection).getIndexInfo().stream().map(IndexInfo::getName).toList();
    }
}
```

---

### B4 · `CacheInvalidator` vide (1 h) — **avant la fin du J3**

**Preuve** : publié dans `main` avant la fin du jour 3 pour que `OrchardService` et `HarvestRequestService` de A puissent l'injecter sans erreur.

`backend/src/main/java/tn/oliveconnect/cache/NoOpCacheInvalidator.java` :

```java
package tn.oliveconnect.cache;

import org.springframework.stereotype.Component;
import tn.oliveconnect.common.facade.CacheInvalidator;

/**
 * Implémentation sans effet livrée en S1 (tâche B4).
 * Remplacée en S3 (tâche B12) par l'invalidation Redis réelle. Ne lève jamais d'exception.
 */
@Component
public class NoOpCacheInvalidator implements CacheInvalidator {

    @Override
    public void orchardChanged(String orchardId) {
        // S1 : no-op (Redis arrive en S3)
    }

    @Override
    public void mapChanged() {
        // S1 : no-op
    }

    @Override
    public void dashboardChanged() {
        // S1 : no-op
    }
}
```

---

### B5 · Ressources (`workers`, `vehicles`, `bins`, `equipment`) (7 h) — J4

**Preuve** : tests CRUD + contrôle de rôle (`LOGISTICS_MANAGER`, `COLLECTION_MANAGER`, `ADMIN`).

Rappel du contrat (§5.5 et §6) :
- `GET /api/v1/resources/{kind}` (`LOGISTICS_MANAGER`, `COLLECTION_MANAGER`, `ADMIN`) → 200 `PageResponse<...>` (filtre optionnel `?status=AVAILABLE`, pagination) ; erreurs : 400 (`kind` ou `status` inconnu), 403.
- `POST /api/v1/resources/{kind}` (`LOGISTICS_MANAGER`, `ADMIN`) → 201 ; le corps est l'objet sans `id` (`status` vaut `AVAILABLE` par défaut s'il est absent).
- `PATCH /api/v1/resources/{kind}/{id}` (`LOGISTICS_MANAGER`, `ADMIN`) → 200 (tous les champs optionnels) ; erreurs : 400, 403, 404.
- `kind` ∈ `{workers, vehicles, bins, equipment}` — toute autre valeur renvoie **400 VALIDATION_ERROR**.

#### Étape 1 — Les 4 Documents (`resource/document/`)

```java
// WorkerDocument.java
package tn.oliveconnect.resource.document;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import tn.oliveconnect.common.enums.ResourceStatus;

@Document("workers")
@Getter @Setter @NoArgsConstructor
public class WorkerDocument {
    @Id private String id;
    private String fullName;
    private String phone;
    private List<String> skills;
    private ResourceStatus status;
    private String externalId;   // interne (imports S3), jamais exposé dans les DTO
}
```

```java
// VehicleDocument.java
package tn.oliveconnect.resource.document;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import tn.oliveconnect.common.enums.ResourceStatus;
import tn.oliveconnect.common.enums.VehicleType;

@Document("vehicles")
@Getter @Setter @NoArgsConstructor
public class VehicleDocument {
    @Id private String id;
    private VehicleType type;
    private String registration;
    private Integer capacityKg;
    private ResourceStatus status;
    private String externalId;
}
```

```java
// BinDocument.java
package tn.oliveconnect.resource.document;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import tn.oliveconnect.common.enums.ResourceStatus;

@Document("bins")
@Getter @Setter @NoArgsConstructor
public class BinDocument {
    @Id private String id;
    private String code;
    private Integer capacityKg;
    private ResourceStatus status;
    private String externalId;
}
```

```java
// EquipmentDocument.java
package tn.oliveconnect.resource.document;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import tn.oliveconnect.common.enums.EquipmentType;
import tn.oliveconnect.common.enums.ResourceStatus;

@Document("equipment")
@Getter @Setter @NoArgsConstructor
public class EquipmentDocument {
    @Id private String id;
    private EquipmentType type;
    private String label;
    private Integer quantity;
    private ResourceStatus status;
    private String externalId;
}
```

#### Étape 2 — Les 4 Repositories (`resource/repository/`)

```java
// WorkerRepository.java
package tn.oliveconnect.resource.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import tn.oliveconnect.common.enums.ResourceStatus;
import tn.oliveconnect.resource.document.WorkerDocument;

public interface WorkerRepository extends MongoRepository<WorkerDocument, String> {
    Page<WorkerDocument> findByStatus(ResourceStatus status, Pageable pageable);
}
```

```java
// VehicleRepository.java
package tn.oliveconnect.resource.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import tn.oliveconnect.common.enums.ResourceStatus;
import tn.oliveconnect.resource.document.VehicleDocument;

public interface VehicleRepository extends MongoRepository<VehicleDocument, String> {
    Page<VehicleDocument> findByStatus(ResourceStatus status, Pageable pageable);
}
```

```java
// BinRepository.java
package tn.oliveconnect.resource.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import tn.oliveconnect.common.enums.ResourceStatus;
import tn.oliveconnect.resource.document.BinDocument;

public interface BinRepository extends MongoRepository<BinDocument, String> {
    Page<BinDocument> findByStatus(ResourceStatus status, Pageable pageable);
}
```

```java
// EquipmentRepository.java
package tn.oliveconnect.resource.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import tn.oliveconnect.common.enums.ResourceStatus;
import tn.oliveconnect.resource.document.EquipmentDocument;

public interface EquipmentRepository extends MongoRepository<EquipmentDocument, String> {
    Page<EquipmentDocument> findByStatus(ResourceStatus status, Pageable pageable);
}
```

#### Étape 3 — DTOs (`resource/dto/ResourceDtos.java`) — clés **exactement** conformes à §5.5

```java
package tn.oliveconnect.resource.dto;

import jakarta.validation.constraints.*;
import java.util.List;
import tn.oliveconnect.common.enums.EquipmentType;
import tn.oliveconnect.common.enums.ResourceStatus;
import tn.oliveconnect.common.enums.VehicleType;
import tn.oliveconnect.resource.document.*;

public final class ResourceDtos {
    private ResourceDtos() {}

    // --- WORKER ---
    public record WorkerRequest(
            @NotBlank(message = "Obligatoire") @Size(max = 100) String fullName,
            @NotBlank(message = "Obligatoire") @Size(max = 30) String phone,
            @NotNull(message = "Obligatoire") List<@NotBlank String> skills,
            ResourceStatus status) {}

    public record WorkerPatch(
            @Size(min = 1, max = 100) String fullName,
            @Size(min = 1, max = 30) String phone,
            List<@NotBlank String> skills,
            ResourceStatus status) {}

    public record WorkerResponse(String id, String fullName, String phone, List<String> skills, ResourceStatus status) {
        public static WorkerResponse from(WorkerDocument d) {
            return new WorkerResponse(d.getId(), d.getFullName(), d.getPhone(), d.getSkills(), d.getStatus());
        }
    }

    // --- VEHICLE ---
    public record VehicleRequest(
            @NotNull(message = "Obligatoire") VehicleType type,
            @NotBlank(message = "Obligatoire") @Size(max = 40) String registration,
            @NotNull(message = "Obligatoire") @Positive(message = "Doit être > 0") Integer capacityKg,
            ResourceStatus status) {}

    public record VehiclePatch(
            VehicleType type,
            @Size(min = 1, max = 40) String registration,
            @Positive(message = "Doit être > 0") Integer capacityKg,
            ResourceStatus status) {}

    public record VehicleResponse(String id, VehicleType type, String registration, Integer capacityKg, ResourceStatus status) {
        public static VehicleResponse from(VehicleDocument d) {
            return new VehicleResponse(d.getId(), d.getType(), d.getRegistration(), d.getCapacityKg(), d.getStatus());
        }
    }

    // --- BIN ---
    public record BinRequest(
            @NotBlank(message = "Obligatoire") @Size(max = 40) String code,
            @NotNull(message = "Obligatoire") @Positive(message = "Doit être > 0") Integer capacityKg,
            ResourceStatus status) {}

    public record BinPatch(
            @Size(min = 1, max = 40) String code,
            @Positive(message = "Doit être > 0") Integer capacityKg,
            ResourceStatus status) {}

    public record BinResponse(String id, String code, Integer capacityKg, ResourceStatus status) {
        public static BinResponse from(BinDocument d) {
            return new BinResponse(d.getId(), d.getCode(), d.getCapacityKg(), d.getStatus());
        }
    }

    // --- EQUIPMENT ---
    public record EquipmentRequest(
            @NotNull(message = "Obligatoire") EquipmentType type,
            @NotBlank(message = "Obligatoire") @Size(max = 100) String label,
            @NotNull(message = "Obligatoire") @Positive(message = "Doit être > 0") Integer quantity,
            ResourceStatus status) {}

    public record EquipmentPatch(
            EquipmentType type,
            @Size(min = 1, max = 100) String label,
            @Positive(message = "Doit être > 0") Integer quantity,
            ResourceStatus status) {}

    public record EquipmentResponse(String id, EquipmentType type, String label, Integer quantity, ResourceStatus status) {
        public static EquipmentResponse from(EquipmentDocument d) {
            return new EquipmentResponse(d.getId(), d.getType(), d.getLabel(), d.getQuantity(), d.getStatus());
        }
    }
}
```

#### Étape 4 — Service : `resource/service/ResourceService.java`

```java
package tn.oliveconnect.resource.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import tn.oliveconnect.common.enums.ResourceStatus;
import tn.oliveconnect.common.error.ApiException;
import tn.oliveconnect.common.error.ErrorCode;
import tn.oliveconnect.common.web.PageResponse;
import tn.oliveconnect.resource.document.*;
import tn.oliveconnect.resource.dto.ResourceDtos.*;
import tn.oliveconnect.resource.repository.*;

@Service
public class ResourceService {

    private final WorkerRepository workers;
    private final VehicleRepository vehicles;
    private final BinRepository bins;
    private final EquipmentRepository equipment;
    private final ObjectMapper mapper;
    private final Validator validator;

    public ResourceService(WorkerRepository workers, VehicleRepository vehicles,
                           BinRepository bins, EquipmentRepository equipment,
                           ObjectMapper mapper, Validator validator) {
        this.workers = workers; this.vehicles = vehicles; this.bins = bins; this.equipment = equipment;
        this.mapper = mapper; this.validator = validator;
    }

    public PageResponse<?> list(String kind, ResourceStatus status, Pageable pageable) {
        return switch (kind) {
            case "workers" -> PageResponse.from(
                    status == null ? workers.findAll(pageable) : workers.findByStatus(status, pageable),
                    WorkerResponse::from);
            case "vehicles" -> PageResponse.from(
                    status == null ? vehicles.findAll(pageable) : vehicles.findByStatus(status, pageable),
                    VehicleResponse::from);
            case "bins" -> PageResponse.from(
                    status == null ? bins.findAll(pageable) : bins.findByStatus(status, pageable),
                    BinResponse::from);
            case "equipment" -> PageResponse.from(
                    status == null ? equipment.findAll(pageable) : equipment.findByStatus(status, pageable),
                    EquipmentResponse::from);
            default -> throw ApiException.validation("kind", "Doit valoir workers, vehicles, bins ou equipment");
        };
    }

    public Object create(String kind, JsonNode body) {
        return switch (kind) {
            case "workers" -> {
                WorkerRequest req = parseAndValidate(body, WorkerRequest.class);
                WorkerDocument d = new WorkerDocument();
                d.setFullName(req.fullName().trim());
                d.setPhone(req.phone().trim());
                d.setSkills(req.skills());
                d.setStatus(ResourceStatus.AVAILABLE); // §5.2 & §5.5 : status vaut AVAILABLE par défaut à la création
                yield WorkerResponse.from(workers.save(d));
            }
            case "vehicles" -> {
                VehicleRequest req = parseAndValidate(body, VehicleRequest.class);
                VehicleDocument d = new VehicleDocument();
                d.setType(req.type());
                d.setRegistration(req.registration().trim());
                d.setCapacityKg(req.capacityKg());
                d.setStatus(ResourceStatus.AVAILABLE);
                yield VehicleResponse.from(vehicles.save(d));
            }
            case "bins" -> {
                BinRequest req = parseAndValidate(body, BinRequest.class);
                BinDocument d = new BinDocument();
                d.setCode(req.code().trim());
                d.setCapacityKg(req.capacityKg());
                d.setStatus(ResourceStatus.AVAILABLE);
                yield BinResponse.from(bins.save(d));
            }
            case "equipment" -> {
                EquipmentRequest req = parseAndValidate(body, EquipmentRequest.class);
                EquipmentDocument d = new EquipmentDocument();
                d.setType(req.type());
                d.setLabel(req.label().trim());
                d.setQuantity(req.quantity());
                d.setStatus(ResourceStatus.AVAILABLE);
                yield EquipmentResponse.from(equipment.save(d));
            }
            default -> throw ApiException.validation("kind", "Doit valoir workers, vehicles, bins ou equipment");
        };
    }

    public Object patch(String kind, String id, JsonNode body) {
        return switch (kind) {
            case "workers" -> {
                WorkerPatch req = parseAndValidate(body, WorkerPatch.class);
                WorkerDocument d = workers.findById(id).orElseThrow(() -> ApiException.notFound("Ouvrier introuvable."));
                if (req.fullName() != null) d.setFullName(req.fullName().trim());
                if (req.phone() != null) d.setPhone(req.phone().trim());
                if (req.skills() != null) d.setSkills(req.skills());
                if (req.status() != null) d.setStatus(req.status());
                yield WorkerResponse.from(workers.save(d));
            }
            case "vehicles" -> {
                VehiclePatch req = parseAndValidate(body, VehiclePatch.class);
                VehicleDocument d = vehicles.findById(id).orElseThrow(() -> ApiException.notFound("Véhicule introuvable."));
                if (req.type() != null) d.setType(req.type());
                if (req.registration() != null) d.setRegistration(req.registration().trim());
                if (req.capacityKg() != null) d.setCapacityKg(req.capacityKg());
                if (req.status() != null) d.setStatus(req.status());
                yield VehicleResponse.from(vehicles.save(d));
            }
            case "bins" -> {
                BinPatch req = parseAndValidate(body, BinPatch.class);
                BinDocument d = bins.findById(id).orElseThrow(() -> ApiException.notFound("Benne introuvable."));
                if (req.code() != null) d.setCode(req.code().trim());
                if (req.capacityKg() != null) d.setCapacityKg(req.capacityKg());
                if (req.status() != null) d.setStatus(req.status());
                yield BinResponse.from(bins.save(d));
            }
            case "equipment" -> {
                EquipmentPatch req = parseAndValidate(body, EquipmentPatch.class);
                EquipmentDocument d = equipment.findById(id).orElseThrow(() -> ApiException.notFound("Matériel introuvable."));
                if (req.type() != null) d.setType(req.type());
                if (req.label() != null) d.setLabel(req.label().trim());
                if (req.quantity() != null) d.setQuantity(req.quantity());
                if (req.status() != null) d.setStatus(req.status());
                yield EquipmentResponse.from(equipment.save(d));
            }
            default -> throw ApiException.validation("kind", "Doit valoir workers, vehicles, bins ou equipment");
        };
    }

    private <T> T parseAndValidate(JsonNode body, Class<T> type) {
        final T dto;
        try {
            dto = mapper.treeToValue(body, type);
        } catch (Exception e) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "JSON mal formé ou valeur invalide.", null);
        }
        Set<ConstraintViolation<T>> violations = validator.validate(dto);
        if (!violations.isEmpty()) {
            Map<String, String> errors = new LinkedHashMap<>();
            violations.forEach(v -> errors.putIfAbsent(v.getPropertyPath().toString(), v.getMessage()));
            throw new ApiException(ErrorCode.VALIDATION_ERROR, null, errors);
        }
        return dto;
    }
}
```

#### Étape 5 — Controller : `resource/controller/ResourceController.java`

```java
package tn.oliveconnect.resource.controller;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.oliveconnect.common.enums.ResourceStatus;
import tn.oliveconnect.common.web.PageResponse;
import tn.oliveconnect.resource.service.ResourceService;

@RestController
@RequestMapping("/api/v1/resources")
public class ResourceController {

    private final ResourceService service;
    public ResourceController(ResourceService service) { this.service = service; }

    @GetMapping("/{kind}")
    @PreAuthorize("hasAnyRole('LOGISTICS_MANAGER','COLLECTION_MANAGER','ADMIN')")
    public PageResponse<?> list(@PathVariable String kind,
                                @RequestParam(required = false) ResourceStatus status,
                                @PageableDefault(size = 20) Pageable pageable) {
        return service.list(kind, status, pageable);
    }

    @PostMapping("/{kind}")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('LOGISTICS_MANAGER','ADMIN')")
    public Object create(@PathVariable String kind, @RequestBody JsonNode body) {
        return service.create(kind, body);
    }

    @PatchMapping("/{kind}/{id}")
    @PreAuthorize("hasAnyRole('LOGISTICS_MANAGER','ADMIN')")
    public Object patch(@PathVariable String kind, @PathVariable String id, @RequestBody JsonNode body) {
        return service.patch(kind, id, body);
    }
    // GET /resources/availability?date= est en semaine 2 (tâche B9)
}
```

#### Étape 6 — Tests : `src/test/java/tn/oliveconnect/resource/ResourceIntegrationTest.java`

```java
package tn.oliveconnect.resource;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import tn.oliveconnect.AbstractIntegrationTest;
import tn.oliveconnect.common.enums.Role;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ResourceIntegrationTest extends AbstractIntegrationTest {

    @Test
    void logisticsManager_crudAllFourKinds_withExactContractKeys() throws Exception {
        String logistics = bearer(Role.LOGISTICS_MANAGER, "log@example.test");

        // 1. Worker (status omis -> AVAILABLE par défaut)
        String workerJson = mvc.perform(post("/api/v1/resources/workers").header("Authorization", logistics)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Ouvrier 01\",\"phone\":\"+216 00 000 000\",\"skills\":[\"PICKING\",\"TRACTOR\"]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("AVAILABLE"))
                .andReturn().getResponse().getContentAsString();
        assertExactKeys(workerJson, "id", "fullName", "phone", "skills", "status");

        // 2. Vehicle
        String vehicleJson = mvc.perform(post("/api/v1/resources/vehicles").header("Authorization", logistics)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"TRACTOR\",\"registration\":\"123 TUN 4567\",\"capacityKg\":3000}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        assertExactKeys(vehicleJson, "id", "type", "registration", "capacityKg", "status");
        String vehicleId = JsonPath.read(vehicleJson, "$.id");

        // 3. Bin
        String binJson = mvc.perform(post("/api/v1/resources/bins").header("Authorization", logistics)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"BIN-014\",\"capacityKg\":500}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        assertExactKeys(binJson, "id", "code", "capacityKg", "status");

        // 4. Equipment
        String eqJson = mvc.perform(post("/api/v1/resources/equipment").header("Authorization", logistics)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"NET\",\"label\":\"Filet de récolte\",\"quantity\":40}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        assertExactKeys(eqJson, "id", "type", "label", "quantity", "status");

        // PATCH Vehicle -> MAINTENANCE + filtre ?status=MAINTENANCE
        mvc.perform(patch("/api/v1/resources/vehicles/" + vehicleId).header("Authorization", logistics)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"MAINTENANCE\"}"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.status").value("MAINTENANCE"));

        mvc.perform(get("/api/v1/resources/vehicles?status=MAINTENANCE").header("Authorization", logistics))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void collectionManager_canRead_butCannotCreateOrPatch_403() throws Exception {
        String manager = bearer(Role.COLLECTION_MANAGER, "cm@example.test");
        mvc.perform(get("/api/v1/resources/workers").header("Authorization", manager))
           .andExpect(status().isOk());
        mvc.perform(post("/api/v1/resources/workers").header("Authorization", manager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"X\",\"phone\":\"1\",\"skills\":[]}"))
           .andExpect(status().isForbidden())
           .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void farmer_cannotReadResources_403_andUnknownKind_400() throws Exception {
        String farmer = bearer(Role.FARMER, "f@example.test");
        mvc.perform(get("/api/v1/resources/workers").header("Authorization", farmer))
           .andExpect(status().isForbidden());

        String logistics = bearer(Role.LOGISTICS_MANAGER, "log@example.test");
        mvc.perform(get("/api/v1/resources/unknown").header("Authorization", logistics))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
```

---

### B6 · Données de démo (`--seed --farmers=N`) (3 h) — J5

**Preuve** : `seed --farmers=20` pour C (démo locale) ; `--farmers=500` prêt pour le test de charge k6 en S3.

Contenu exigé (§7 B6) :
- **1 compte par rôle** (6 rôles authentifiés) avec un mot de passe de démo connu (`Secret#123`) ;
- `N` agriculteurs supplémentaires (`farmer001@example.test` …) ;
- Vergers autour de **Sfax `[10.76, 34.73]`** (longitude d'abord !) avec variétés (`Chemlali`, `Chetoui`, `Oueslati`) et statuts de maturité variés ;
- Demandes de récolte initiales, ouvriers, tracteurs/camions, bennes, matériel et **2 pressoirs** dans la collection `presses`.

`backend/src/main/java/tn/oliveconnect/data/seed/DataSeeder.java` :

```java
package tn.oliveconnect.data.seed;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import tn.oliveconnect.common.enums.*;
import tn.oliveconnect.harvest.document.HarvestRequestDocument;
import tn.oliveconnect.harvest.repository.HarvestRequestRepository;
import tn.oliveconnect.orchard.document.OrchardDocument;
import tn.oliveconnect.orchard.repository.OrchardRepository;
import tn.oliveconnect.resource.document.*;
import tn.oliveconnect.resource.repository.*;
import tn.oliveconnect.user.document.UserDocument;
import tn.oliveconnect.user.repository.UserRepository;

/**
 * S'exécute uniquement si l'argument "--seed" est passé (ex. --seed --farmers=20 ou --seed --farmers=500).
 * Idempotent : vide et recrée les données de démo.
 */
@Component
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final String[] VARIETIES = {"Chemlali", "Chetoui", "Oueslati", "Zalmati"};
    private static final MaturityStatus[] MATURITIES = MaturityStatus.values();

    private final MongoTemplate mongo;
    private final UserRepository users;
    private final OrchardRepository orchards;
    private final HarvestRequestRepository requests;
    private final WorkerRepository workers;
    private final VehicleRepository vehicles;
    private final BinRepository bins;
    private final EquipmentRepository equipment;
    private final PasswordEncoder encoder;

    public DataSeeder(MongoTemplate mongo, UserRepository users, OrchardRepository orchards,
                      HarvestRequestRepository requests, WorkerRepository workers,
                      VehicleRepository vehicles, BinRepository bins, EquipmentRepository equipment,
                      PasswordEncoder encoder) {
        this.mongo = mongo; this.users = users; this.orchards = orchards; this.requests = requests;
        this.workers = workers; this.vehicles = vehicles; this.bins = bins; this.equipment = equipment;
        this.encoder = encoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<String> raw = Arrays.asList(args.getSourceArgs());
        boolean requested = args.containsOption("seed") || raw.contains("--seed") || raw.contains("seed");
        if (!requested) return;

        int farmersCount = parseFarmersCount(args, raw);
        log.info("Seed OliveConnect démarré (--farmers={})...", farmersCount);

        for (String col : List.of("users", "orchards", "harvest_requests", "workers", "vehicles", "bins", "equipment", "presses")) {
            mongo.getCollection(col).deleteMany(new Document());
        }

        String hash = encoder.encode("Secret#123");

        // 1. Un compte de référence par rôle (menus de C3)
        UserDocument mainFarmer = saveUser("Example Farmer", "farmer@example.test", Role.FARMER, hash);
        saveUser("Resp. Collecte", "collection@example.test", Role.COLLECTION_MANAGER, hash);
        saveUser("Chef Équipe 01", "leader@example.test", Role.TEAM_LEADER, hash);
        saveUser("Resp. Logistique", "logistics@example.test", Role.LOGISTICS_MANAGER, hash);
        saveUser("Resp. Pressoir", "press@example.test", Role.PRESS_MANAGER, hash);
        saveUser("Administrateur", "admin@example.test", Role.ADMIN, hash);

        // 2. Agriculteurs + vergers autour de Sfax [10.76, 34.73] + demandes
        Random rnd = new Random(2026L);   // graine fixe -> données reproductibles
        createOrchardsForFarmer(mainFarmer.getId(), 0, 2, rnd);
        for (int i = 2; i <= farmersCount; i++) {
            UserDocument f = saveUser("Agriculteur %03d".formatted(i),
                    "farmer%03d@example.test".formatted(i), Role.FARMER, hash);
            createOrchardsForFarmer(f.getId(), i, 2, rnd);
        }

        // 3. Ressources : 15 ouvriers, 6 véhicules, 20 bennes, 3 matériels
        for (int i = 1; i <= 15; i++) {
            WorkerDocument w = new WorkerDocument();
            w.setFullName("Ouvrier %02d".formatted(i));
            w.setPhone("+216 74 %03d %03d".formatted(100 + i, 200 + i));
            w.setSkills(i % 3 == 0 ? List.of("PICKING", "TRACTOR") : List.of("PICKING"));
            w.setStatus(ResourceStatus.AVAILABLE);
            workers.save(w);
        }
        for (int i = 1; i <= 6; i++) {
            VehicleDocument v = new VehicleDocument();
            v.setType(i <= 4 ? VehicleType.TRACTOR : VehicleType.TRUCK);
            v.setRegistration("%d TUN %d".formatted(120 + i, 4500 + i));
            v.setCapacityKg(i <= 4 ? 3000 : 8000);
            v.setStatus(ResourceStatus.AVAILABLE);
            vehicles.save(v);
        }
        for (int i = 1; i <= 20; i++) {
            BinDocument b = new BinDocument();
            b.setCode("BIN-%03d".formatted(i));
            b.setCapacityKg(500);
            b.setStatus(ResourceStatus.AVAILABLE);
            bins.save(b);
        }
        saveEquipment(EquipmentType.NET, "Filet de récolte", 40);
        saveEquipment(EquipmentType.VIBRATOR, "Peigne vibrant", 12);
        saveEquipment(EquipmentType.OTHER, "Échelle aluminium", 25);

        // 4. Deux pressoirs autour de Sfax (§5.5 Press)
        mongo.getCollection("presses").insertMany(List.of(
                new Document("name", "Pressoir de la coopérative Sfax Nord")
                        .append("location", new Document("type", "Point").append("coordinates", List.of(10.75, 34.74)))
                        .append("dailyCapacityKg", 50000).append("availableCapacityKg", 32000),
                new Document("name", "Pressoir Sfax Sud - Agareb")
                        .append("location", new Document("type", "Point").append("coordinates", List.of(10.68, 34.71)))
                        .append("dailyCapacityKg", 40000).append("availableCapacityKg", 28000)
        ));

        log.info("Seed terminé : {} comptes, {} vergers, {} demandes.",
                users.count(), orchards.count(), requests.count());
    }

    private UserDocument saveUser(String name, String email, Role role, String hash) {
        UserDocument u = new UserDocument();
        u.setFullName(name); u.setEmail(email); u.setPasswordHash(hash);
        u.setRole(role); u.setActive(true);
        return users.save(u);
    }

    private void createOrchardsForFarmer(String farmerId, int idx, int count, Random rnd) {
        for (int k = 1; k <= count; k++) {
            double lon = Math.round((10.76 + (rnd.nextDouble() - 0.5) * 0.30) * 10000.0) / 10000.0;
            double lat = Math.round((34.73 + (rnd.nextDouble() - 0.5) * 0.25) * 10000.0) / 10000.0;
            OrchardDocument o = new OrchardDocument();
            o.setFarmerId(farmerId);
            o.setName(idx == 0 && k == 1 ? "North Orchard" : "Verger Sfax %d-%d".formatted(idx, k));
            o.setAreaHectares(Math.round((1.5 + rnd.nextDouble() * 8.0) * 10.0) / 10.0);
            o.setOliveVariety(VARIETIES[(idx + k) % VARIETIES.length]);
            o.setLocation(new GeoJsonPoint(lon, lat));   // [longitude, latitude]
            o.setMaturityStatus(MATURITIES[(idx + k) % MATURITIES.length]);
            o.setPublicVisibility(k % 2 == 1);
            o = orchards.save(o);

            if (k == 1) {
                HarvestRequestDocument hr = new HarvestRequestDocument();
                hr.setFarmerId(farmerId);
                hr.setOrchardId(o.getId());
                hr.setOrchardName(o.getName());
                hr.setType(idx % 3 == 0 ? HarvestType.URGENT : HarvestType.PLANNED);
                hr.setUrgencyReason(hr.getType() == HarvestType.URGENT ? "Maturité optimale" : null);
                hr.setStatus(HarvestStatus.SUBMITTED);
                hr.setRequestedAt(Instant.now());
                hr.setPreferredDate(LocalDate.now().plusDays(2 + (idx % 7)));
                hr.setEstimatedQuantityKg(BigDecimal.valueOf(1000 + (idx % 10) * 250L));
                requests.save(hr);
            }
        }
    }

    private void saveEquipment(EquipmentType type, String label, int qty) {
        EquipmentDocument e = new EquipmentDocument();
        e.setType(type); e.setLabel(label); e.setQuantity(qty); e.setStatus(ResourceStatus.AVAILABLE);
        equipment.save(e);
    }

    private int parseFarmersCount(ApplicationArguments args, List<String> raw) {
        if (args.containsOption("farmers")) {
            return Integer.parseInt(args.getOptionValues("farmers").get(0));
        }
        int i = raw.indexOf("--farmers");
        return (i >= 0 && i + 1 < raw.size()) ? Integer.parseInt(raw.get(i + 1)) : 20;
    }
}
```

Lancer le seed (20 agriculteurs pour C, ou 500 pour k6) :

```powershell
cd backend
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--seed --farmers=20"
```

✅ **B a fini la semaine quand** : `docs/nosql-model.md` est validé par A et C, `docker compose up` fonctionne chez les 3, `MongoIndexConfigTest` et `ResourceIntegrationTest` sont verts, et le seed charge les 6 comptes de démo + vergers autour de Sfax.

---

## 5. MEMBRE C — Frontend, carte, déploiement (C1 → C6)

**Preuve à livrer vendredi** : front qui tourne d'abord sur mocks MSW puis sur l'API réelle : connexion par rôle (7 rôles dont visiteur), création d'un verger (« Ma position » + clic carte), dépôt d'une demande (validation Zod + `fieldErrors` serveur), carte Leaflet avec `bbox` et légende de maturité ; `types/contract.ts` strictement identique au contrat.

---

### C1 · Projet React + TypeScript + Vite (3 h) — J1

**Preuve** : `npm run dev` démarre et affiche la structure de navigation et une page par rôle.

#### Étape 1 — Initialiser le projet et installer les dépendances du plan (§3.6)

```powershell
cd oliveconnect
npm create vite@latest frontend -- --template react-ts
cd frontend
npm install react-router-dom @tanstack/react-query react-hook-form @hookform/resolvers zod leaflet react-leaflet recharts
npm install -D @types/leaflet msw
npx msw init public/ --save
```

#### Étape 2 — Arborescence imposée (`app/ api/ components/ features/ types/`)

```powershell
mkdir src/app, src/api, src/components, src/features, src/types, src/mocks
mkdir src/features/auth, src/features/orchards, src/features/harvest, src/features/map
```

```text
frontend/src/
  app/           App.tsx, router.tsx, Layout.tsx
  api/           http.ts
  components/    StatusBadge.tsx, ProtectedRoute.tsx
  features/
    auth/        AuthContext.tsx, LoginPage.tsx
    orchards/    OrchardsPage.tsx
    harvest/     HarvestRequestsPage.tsx
    map/         OrchardsMapPage.tsx
  mocks/         handlers.ts, browser.ts
  types/         contract.ts
  index.css      (CSS simple, variables, flex/grid, mobile-first 360 px)
  main.tsx
```

#### Étape 3 — Proxy Vite vers Spring Boot (`frontend/vite.config.ts`)

> Le proxy `/api` → `http://localhost:8080` reproduit en développement le comportement exact de Nginx en production (§3.6). Quand `VITE_USE_MOCKS=true`, MSW intercepte `/api/v1/*` dans le navigateur avant même que la requête ne sorte ; quand `VITE_USE_MOCKS=false`, l'appel part vers Spring Boot.

```ts
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
});
```

#### Étape 4 — CSS simple, variables et mobile-first (`frontend/src/index.css`)

```css
:root {
  --olive-900: #1f3318;
  --olive-700: #365928;
  --olive-500: #55823e;
  --olive-100: #eef4ea;
  --surface: #ffffff;
  --bg: #f6f8f5;
  --text: #1c2419;
  --muted: #5a6655;
  --danger: #b42318;
  --danger-bg: #fee4e2;
  --warn: #b54708;
  --warn-bg: #fef0c7;
  --ok: #067647;
  --ok-bg: #d1fadf;
  --border: #d5ddd1;
  --radius: 8px;
}

* { box-sizing: border-box; }
body {
  margin: 0;
  font-family: system-ui, -apple-system, Segoe UI, Roboto, sans-serif;
  background: var(--bg);
  color: var(--text);
  line-height: 1.45;
}

/* Mobile-first (360 px) */
.app-shell { display: flex; flex-direction: column; min-height: 100vh; }
.topbar {
  background: var(--olive-900);
  color: #fff;
  padding: 0.75rem 1rem;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
}
.nav-links { display: flex; flex-wrap: wrap; gap: 0.5rem; }
.nav-links a {
  color: #fff;
  text-decoration: none;
  padding: 0.35rem 0.65rem;
  border-radius: var(--radius);
  font-size: 0.9rem;
}
.nav-links a.active, .nav-links a:hover { background: var(--olive-700); }

.container { width: 100%; max-width: 1100px; margin: 0 auto; padding: 1rem; }
.grid-2 { display: grid; grid-template-columns: 1fr; gap: 1rem; }
@media (min-width: 768px) {
  .grid-2 { grid-template-columns: 1fr 1.2fr; }
}

.card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  padding: 1rem;
}
.form-row { display: flex; flex-direction: column; gap: 0.25rem; margin-bottom: 0.75rem; }
.form-row label { font-weight: 600; font-size: 0.9rem; }
.form-row input, .form-row select, .form-row textarea {
  padding: 0.55rem 0.65rem;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  font-size: 1rem;
}
.field-error { color: var(--danger); font-size: 0.82rem; margin: 0; }
.alert-error {
  background: var(--danger-bg);
  color: var(--danger);
  padding: 0.65rem 0.85rem;
  border-radius: var(--radius);
  margin-bottom: 0.75rem;
}

.btn {
  cursor: pointer;
  border: none;
  border-radius: var(--radius);
  padding: 0.6rem 1rem;
  font-weight: 600;
  background: var(--olive-700);
  color: #fff;
}
.btn-secondary { background: var(--olive-100); color: var(--olive-900); border: 1px solid var(--border); }
.btn:disabled { opacity: 0.6; cursor: not-allowed; }

.badge {
  display: inline-block;
  padding: 0.2rem 0.55rem;
  border-radius: 999px;
  font-size: 0.78rem;
  font-weight: 700;
}
.badge-SUBMITTED, .badge-APPROACHING { background: var(--warn-bg); color: var(--warn); }
.badge-APPROVED, .badge-OPTIMAL, .badge-COMPLETED { background: var(--ok-bg); color: var(--ok); }
.badge-REJECTED, .badge-CANCELLED, .badge-OVERRIPE { background: var(--danger-bg); color: var(--danger); }
.badge-SCHEDULED, .badge-IN_PROGRESS, .badge-UNKNOWN, .badge-IMMATURE { background: var(--olive-100); color: var(--olive-900); }
```

---

### C2 · Types du contrat, client HTTP et mocks MSW (5 h) — J2 / J3

**Preuve** : le front tourne **sans backend** (`VITE_USE_MOCKS=true`) avec exactement les JSON du contrat.

#### Étape 1 — `frontend/src/types/contract.ts` (figé avec le tag `contracts-v1`)

```ts
// ============================================================================
// CONTRAT API v1 FIGÉ (sections 5.2 à 5.5 du plan) — NE RIEN RENOMMER
// ============================================================================

export type Role =
  | 'FARMER'
  | 'COLLECTION_MANAGER'
  | 'TEAM_LEADER'
  | 'LOGISTICS_MANAGER'
  | 'PRESS_MANAGER'
  | 'ADMIN';

export type MaturityStatus = 'UNKNOWN' | 'IMMATURE' | 'APPROACHING' | 'OPTIMAL' | 'OVERRIPE';
export type HarvestType = 'PLANNED' | 'URGENT';
export type HarvestStatus =
  | 'SUBMITTED'
  | 'APPROVED'
  | 'REJECTED'
  | 'SCHEDULED'
  | 'IN_PROGRESS'
  | 'COMPLETED'
  | 'CANCELLED';

export type AlertType = 'MATURITY' | 'PEST' | 'RAIN' | 'OTHER';
export type AlertPriority = 'LOW' | 'MEDIUM' | 'HIGH';
export type AlertStatus = 'OPEN' | 'ACKNOWLEDGED' | 'RESOLVED' | 'DISMISSED';
export type TourStatus = 'DRAFT' | 'PLANNED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';
export type StopStatus = 'PLANNED' | 'COMPLETED';
export type ResourceStatus = 'AVAILABLE' | 'IN_USE' | 'MAINTENANCE';
export type VehicleType = 'TRACTOR' | 'TRUCK';
export type EquipmentType = 'NET' | 'VIBRATOR' | 'OTHER';
export type QualityGrade = 'A' | 'B' | 'C';

export type NotificationType =
  | 'REQUEST_APPROVED'
  | 'REQUEST_REJECTED'
  | 'TOUR_SCHEDULED'
  | 'TOUR_CHANGED'
  | 'COLLECTION_STARTED'
  | 'COLLECTION_COMPLETED'
  | 'ALERT_ACKNOWLEDGED'
  | 'ALERT_RESOLVED'
  | 'ALERT_DISMISSED';

export type EntityType = 'HARVEST_REQUEST' | 'TOUR' | 'ALERT';

export type ImportType =
  | 'ORCHARDS'
  | 'WORKERS'
  | 'VEHICLES'
  | 'PRESSES'
  | 'MATURITY_READINGS'
  | 'WEATHER_OBSERVATIONS'
  | 'PRESS_CAPACITY';

export type ImportStatus = 'RUNNING' | 'COMPLETED' | 'PARTIAL' | 'FAILED';

export type ErrorCode =
  | 'VALIDATION_ERROR'
  | 'UNAUTHENTICATED'
  | 'ACCESS_DENIED'
  | 'NOT_FOUND'
  | 'INVALID_STATE_TRANSITION'
  | 'RESOURCE_CONFLICT'
  | 'DUPLICATE_RESOURCE'
  | 'RATE_LIMITED'
  | 'INTERNAL_ERROR';

export interface ApiError {
  timestamp: string;
  status: number;
  code: ErrorCode;
  message: string;
  fieldErrors: Record<string, string>;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

/** GeoJSON Point : coordinates = [longitude, latitude] (Sfax = [10.76, 34.73]) */
export interface GeoPoint {
  type: 'Point';
  coordinates: [number, number];
}

export interface UserResponse {
  id: string;
  fullName: string;
  email: string;
  role: Role;
  active: boolean;
  createdAt: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  fullName: string;
  email: string;
  password: string;
  phone?: string | null;
}

export interface LoginResponse {
  accessToken: string;
  tokenType: 'Bearer';
  expiresInSeconds: number;
  user: UserResponse;
}

export interface OrchardCreateRequest {
  name: string;
  areaHectares: number;
  oliveVariety: string;
  location: GeoPoint;
  publicVisibility: boolean;
}

export interface OrchardPatchRequest {
  name?: string;
  areaHectares?: number;
  oliveVariety?: string;
  location?: GeoPoint;
  publicVisibility?: boolean;
  maturityStatus?: MaturityStatus;
}

export interface OrchardResponse {
  id: string;
  farmerId: string;
  name: string;
  areaHectares: number;
  oliveVariety: string;
  location: GeoPoint;
  maturityStatus: MaturityStatus;
  publicVisibility: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface HarvestRequestCreateRequest {
  orchardId: string;
  type: HarvestType;
  preferredDate: string; // YYYY-MM-DD
  estimatedQuantityKg: number;
  urgencyReason: string | null;
}

export interface HarvestRequestReviewRequest {
  decision: 'APPROVE' | 'REJECT';
  reason: string | null;
}

export interface HarvestRequestCancelRequest {
  reason: string | null;
}

export interface HarvestRequestResponse {
  id: string;
  farmerId: string;
  orchardId: string;
  orchardName: string;
  type: HarvestType;
  urgencyReason: string | null;
  status: HarvestStatus;
  requestedAt: string;
  preferredDate: string;
  estimatedQuantityKg: number;
  actualQuantityKg: number | null;
  reviewedBy: string | null;
  reviewNote: string | null;
  scheduledTourId: string | null;
  scheduledAt: string | null;
  completedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface FieldAlertCreateRequest {
  orchardId: string;
  type: AlertType;
  description: string;
  location: GeoPoint;
  priority: AlertPriority;
}

export interface FieldAlertResponse {
  id: string;
  farmerId: string;
  orchardId: string;
  type: AlertType;
  description: string;
  location: GeoPoint;
  priority: AlertPriority;
  status: AlertStatus;
  createdAt: string;
  acknowledgedAt: string | null;
  resolvedAt: string | null;
  resolutionNote: string | null;
}

export interface TourPlanStopRequest {
  harvestRequestId: string;
  sequence: number;
}

export interface TourPlanRequest {
  teamLeaderId: string;
  vehicleIds: string[];
  workerIds: string[];
  binIds: string[];
  stops: TourPlanStopRequest[];
}

export interface TourStopResponse {
  stopId: string;
  sequence: number;
  harvestRequestId: string;
  orchardId: string;
  orchardName: string;
  plannedQuantityKg: number;
  actualQuantityKg: number | null;
  status: StopStatus;
}

export interface TourResponse {
  id: string;
  tourDate: string;
  status: TourStatus;
  teamLeaderId: string | null;
  vehicleIds: string[];
  workerIds: string[];
  binIds: string[];
  stops: TourStopResponse[];
  startedAt: string | null;
  completedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface PressReceptionCreateRequest {
  tourId: string;
  binId: string;
  pressId: string;
  weightKg: number;
  qualityGrade: QualityGrade;
  notes: string | null;
  receivedAt: string | null;
}

export interface PressReceptionResponse {
  id: string;
  tourId: string;
  binId: string;
  pressId: string;
  receivedAt: string;
  weightKg: number;
  qualityGrade: QualityGrade;
  notes: string | null;
  recordedBy: string;
}

export interface WorkerResponse {
  id: string;
  fullName: string;
  phone: string;
  skills: string[];
  status: ResourceStatus;
}

export interface VehicleResponse {
  id: string;
  type: VehicleType;
  registration: string;
  capacityKg: number;
  status: ResourceStatus;
}

export interface BinResponse {
  id: string;
  code: string;
  capacityKg: number;
  status: ResourceStatus;
}

export interface EquipmentResponse {
  id: string;
  type: EquipmentType;
  label: string;
  quantity: number;
  status: ResourceStatus;
}

export interface PressResponse {
  id: string;
  name: string;
  location: GeoPoint;
  dailyCapacityKg: number;
  availableCapacityKg: number;
}

export interface ResourceAvailabilityResponse {
  date: string;
  workers: WorkerResponse[];
  vehicles: VehicleResponse[];
  bins: BinResponse[];
  equipment: EquipmentResponse[];
}

export interface NotificationResponse {
  id: string;
  type: NotificationType;
  message: string;
  entityType: EntityType;
  entityId: string;
  readAt: string | null;
  createdAt: string;
}

export interface OrchardMapProperties {
  orchardId: string;
  name: string | null;
  oliveVariety: string;
  areaHectares: number | null;
  farmerId: string | null;
  maturityStatus: MaturityStatus;
  harvestStatus: HarvestStatus | null;
}

export interface OrchardMapFeature {
  type: 'Feature';
  geometry: GeoPoint;
  properties: OrchardMapProperties;
}

export interface OrchardMapFeatureCollection {
  type: 'FeatureCollection';
  truncated: boolean;
  features: OrchardMapFeature[];
}

export interface DashboardSummaryResponse {
  from: string;
  to: string;
  totalHarvestedKg: number;
  totalReceivedKg: number;
  completedTours: number;
  openAlerts: number;
  requestsByStatus: Record<HarvestStatus, number>;
}

export interface DashboardYieldByOrchardResponse {
  from: string;
  to: string;
  items: {
    orchardId: string;
    orchardName: string;
    totalHarvestedKg: number;
    collectionCount: number;
  }[];
}

export interface DashboardCollectionDelaysResponse {
  from: string;
  to: string;
  sampleSize: number;
  avgSubmittedToScheduledHours: number | null;
  avgScheduledToCompletedHours: number | null;
}

export interface DashboardBinFillRatesResponse {
  from: string;
  to: string;
  items: {
    binId: string;
    binCode: string;
    capacityKg: number;
    receptionCount: number;
    avgFillRate: number | null;
  }[];
}

export interface ImportJobError {
  row: number;
  field: string;
  message: string;
}

export interface ImportJobResponse {
  id: string;
  type: ImportType;
  sourceFile: string;
  status: ImportStatus;
  startedAt: string;
  finishedAt: string | null;
  readCount: number;
  insertedCount: number;
  updatedCount: number;
  rejectedCount: number;
  errors: ImportJobError[];
}
```

#### Étape 2 — Client HTTP avec Bearer et lecture de `ApiError` : `frontend/src/api/http.ts`

```ts
import type { ApiError } from '../types/contract';

const TOKEN_KEY = 'oliveconnect_jwt';

export const tokenStore = {
  get: (): string | null => localStorage.getItem(TOKEN_KEY),
  set: (token: string) => localStorage.setItem(TOKEN_KEY, token),
  clear: () => localStorage.removeItem(TOKEN_KEY),
};

export class ApiClientError extends Error {
  readonly apiError: ApiError;
  constructor(apiError: ApiError) {
    super(apiError.message);
    this.apiError = apiError;
  }
}

export async function apiFetch<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  if (!headers.has('Content-Type') && !(init.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json');
  }
  const token = tokenStore.get();
  if (token) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  const res = await fetch(`/api/v1${path}`, { ...init, headers });
  if (!res.ok) {
    let err: ApiError;
    try {
      err = (await res.json()) as ApiError;
    } catch {
      err = {
        timestamp: new Date().toISOString(),
        status: res.status,
        code: 'INTERNAL_ERROR',
        message: `Erreur HTTP ${res.status}`,
        fieldErrors: {},
      };
    }
    throw new ApiClientError(err);
  }
  return (await res.json()) as T;
}
```

#### Étape 3 — Mocks MSW tirés des exemples JSON du contrat : `frontend/src/mocks/handlers.ts`

```ts
import { http, HttpResponse } from 'msw';
import type {
  HarvestRequestCreateRequest,
  HarvestRequestResponse,
  LoginRequest,
  OrchardCreateRequest,
  OrchardMapFeatureCollection,
  OrchardResponse,
  Role,
  UserResponse,
} from '../types/contract';

const DEMO_USERS: Record<string, UserResponse> = {
  'farmer@example.test': {
    id: '6741a0c2e4b0a1b2c3d4e5f6',
    fullName: 'Example Farmer',
    email: 'farmer@example.test',
    role: 'FARMER',
    active: true,
    createdAt: '2026-11-17T10:00:00Z',
  },
  'collection@example.test': {
    id: '6741a0c2e4b0a1b2c3d4e501',
    fullName: 'Resp. Collecte',
    email: 'collection@example.test',
    role: 'COLLECTION_MANAGER',
    active: true,
    createdAt: '2026-11-17T10:00:00Z',
  },
  'leader@example.test': {
    id: '6741b0c2e4b0a1b2c3d4e502',
    fullName: 'Chef Équipe 01',
    email: 'leader@example.test',
    role: 'TEAM_LEADER',
    active: true,
    createdAt: '2026-11-17T10:00:00Z',
  },
  'logistics@example.test': {
    id: '6741a0c2e4b0a1b2c3d4e503',
    fullName: 'Resp. Logistique',
    email: 'logistics@example.test',
    role: 'LOGISTICS_MANAGER',
    active: true,
    createdAt: '2026-11-17T10:00:00Z',
  },
  'press@example.test': {
    id: '6741ab00e4b0a1b2c3d4e504',
    fullName: 'Resp. Pressoir',
    email: 'press@example.test',
    role: 'PRESS_MANAGER',
    active: true,
    createdAt: '2026-11-17T10:00:00Z',
  },
  'admin@example.test': {
    id: '6741a0c2e4b0a1b2c3d4e505',
    fullName: 'Administrateur',
    email: 'admin@example.test',
    role: 'ADMIN',
    active: true,
    createdAt: '2026-11-17T10:00:00Z',
  },
};

const orchards: OrchardResponse[] = [
  {
    id: '6741a1c2e4b0a1b2c3d4e5f6',
    farmerId: '6741a0c2e4b0a1b2c3d4e5f6',
    name: 'North Orchard',
    areaHectares: 4.2,
    oliveVariety: 'Chemlali',
    location: { type: 'Point', coordinates: [10.76, 34.73] },
    maturityStatus: 'OPTIMAL',
    publicVisibility: true,
    createdAt: '2026-11-17T10:00:00Z',
    updatedAt: '2026-11-17T10:00:00Z',
  },
];

const harvestRequests: HarvestRequestResponse[] = [
  {
    id: '6741a2c2e4b0a1b2c3d4e5f6',
    farmerId: '6741a0c2e4b0a1b2c3d4e5f6',
    orchardId: '6741a1c2e4b0a1b2c3d4e5f6',
    orchardName: 'North Orchard',
    type: 'URGENT',
    urgencyReason: 'Maturité optimale',
    status: 'SUBMITTED',
    requestedAt: '2026-11-17T10:30:00Z',
    preferredDate: '2026-11-20',
    estimatedQuantityKg: 2500,
    actualQuantityKg: null,
    reviewedBy: null,
    reviewNote: null,
    scheduledTourId: null,
    scheduledAt: null,
    completedAt: null,
    createdAt: '2026-11-17T10:30:00Z',
    updatedAt: '2026-11-17T10:30:00Z',
  },
];

let currentEmail = 'farmer@example.test';

export const handlers = [
  http.post('/api/v1/auth/login', async ({ request }) => {
    const body = (await request.json()) as LoginRequest;
    const user = DEMO_USERS[body.email.trim().toLowerCase()];
    if (!user || body.password !== 'Secret#123') {
      return HttpResponse.json(
        {
          timestamp: new Date().toISOString(),
          status: 401,
          code: 'UNAUTHENTICATED',
          message: 'Email ou mot de passe incorrect.',
          fieldErrors: {},
        },
        { status: 401 },
      );
    }
    currentEmail = user.email;
    return HttpResponse.json({
      accessToken: `mock-jwt-${user.role}`,
      tokenType: 'Bearer',
      expiresInSeconds: 7200,
      user,
    });
  }),

  http.get('/api/v1/users/me', ({ request }) => {
    const auth = request.headers.get('Authorization');
    if (!auth) {
      return HttpResponse.json(
        { timestamp: new Date().toISOString(), status: 401, code: 'UNAUTHENTICATED', message: 'Token manquant.', fieldErrors: {} },
        { status: 401 },
      );
    }
    return HttpResponse.json(DEMO_USERS[currentEmail] ?? DEMO_USERS['farmer@example.test']);
  }),

  http.get('/api/v1/orchards/mine', () =>
    HttpResponse.json({
      content: orchards,
      page: 0,
      size: 20,
      totalElements: orchards.length,
      totalPages: 1,
    }),
  ),

  http.post('/api/v1/orchards', async ({ request }) => {
    const body = (await request.json()) as OrchardCreateRequest;
    if (body.areaHectares <= 0) {
      return HttpResponse.json(
        {
          timestamp: new Date().toISOString(),
          status: 400,
          code: 'VALIDATION_ERROR',
          message: 'Requête invalide.',
          fieldErrors: { areaHectares: 'Doit être > 0' },
        },
        { status: 400 },
      );
    }
    const created: OrchardResponse = {
      id: `6741a1c2e4b0a1b2c3d4e${String(orchards.length + 100)}`,
      farmerId: '6741a0c2e4b0a1b2c3d4e5f6',
      name: body.name,
      areaHectares: body.areaHectares,
      oliveVariety: body.oliveVariety,
      location: body.location,
      maturityStatus: 'UNKNOWN',
      publicVisibility: body.publicVisibility,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    };
    orchards.unshift(created);
    return HttpResponse.json(created, { status: 201 });
  }),

  http.get('/api/v1/harvest-requests/mine', () =>
    HttpResponse.json({
      content: harvestRequests,
      page: 0,
      size: 20,
      totalElements: harvestRequests.length,
      totalPages: 1,
    }),
  ),

  http.post('/api/v1/harvest-requests', async ({ request }) => {
    const body = (await request.json()) as HarvestRequestCreateRequest;
    const fieldErrors: Record<string, string> = {};
    if (body.estimatedQuantityKg <= 0) fieldErrors.estimatedQuantityKg = 'Doit être > 0';
    if (body.type === 'URGENT' && (!body.urgencyReason || !body.urgencyReason.trim())) {
      fieldErrors.urgencyReason = 'Obligatoire pour une demande urgente';
    }
    if (Object.keys(fieldErrors).length > 0) {
      return HttpResponse.json(
        { timestamp: new Date().toISOString(), status: 400, code: 'VALIDATION_ERROR', message: 'Requête invalide.', fieldErrors },
        { status: 400 },
      );
    }
    const orchard = orchards.find((o) => o.id === body.orchardId);
    const created: HarvestRequestResponse = {
      id: `6741a2c2e4b0a1b2c3d4e${String(harvestRequests.length + 200)}`,
      farmerId: '6741a0c2e4b0a1b2c3d4e5f6',
      orchardId: body.orchardId,
      orchardName: orchard?.name ?? 'North Orchard',
      type: body.type,
      urgencyReason: body.type === 'URGENT' ? body.urgencyReason : null,
      status: 'SUBMITTED',
      requestedAt: new Date().toISOString(),
      preferredDate: body.preferredDate,
      estimatedQuantityKg: body.estimatedQuantityKg,
      actualQuantityKg: null,
      reviewedBy: null,
      reviewNote: null,
      scheduledTourId: null,
      scheduledAt: null,
      completedAt: null,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    };
    harvestRequests.unshift(created);
    return HttpResponse.json(created, { status: 201 });
  }),

  http.get('/api/v1/orchards/map', ({ request }) => {
    const url = new URL(request.url);
    if (!url.searchParams.get('bbox')) {
      return HttpResponse.json(
        { timestamp: new Date().toISOString(), status: 400, code: 'VALIDATION_ERROR', message: 'bbox obligatoire', fieldErrors: { bbox: 'Obligatoire' } },
        { status: 400 },
      );
    }
    const isAuth = Boolean(request.headers.get('Authorization'));
    const fc: OrchardMapFeatureCollection = {
      type: 'FeatureCollection',
      truncated: false,
      features: orchards.map((o) => ({
        type: 'Feature',
        geometry: o.location,
        properties: {
          orchardId: o.id,
          name: isAuth ? o.name : null,
          oliveVariety: o.oliveVariety,
          areaHectares: isAuth ? o.areaHectares : null,
          farmerId: isAuth ? o.farmerId : null,
          maturityStatus: o.maturityStatus,
          harvestStatus: 'SCHEDULED',
        },
      })),
    };
    return HttpResponse.json(fc);
  }),
];

export const SEEDED_ACCOUNTS: { label: string; email: string; role: Role }[] = Object.values(DEMO_USERS).map((u) => ({
  label: `${u.role} (${u.email})`,
  email: u.email,
  role: u.role,
}));
```

`frontend/src/mocks/browser.ts` :

```ts
import { setupWorker } from 'msw/browser';
import { handlers } from './handlers';

export const worker = setupWorker(...handlers);
```

---

### C3 · Connexion, `AuthContext`, routes protégées et menus par rôle (4 h) — J3

**Preuve** : les menus changent selon le compte seedé choisi (les 6 rôles authentifiés + le visiteur non connecté).

#### Étape 1 — `frontend/src/features/auth/AuthContext.tsx`

```tsx
import React, { createContext, useContext, useEffect, useState } from 'react';
import { apiFetch, tokenStore } from '../../api/http';
import type { LoginRequest, LoginResponse, UserResponse } from '../../types/contract';

interface AuthState {
  user: UserResponse | null;
  loading: boolean;
  login: (req: LoginRequest) => Promise<UserResponse>;
  logout: () => void;
}

const AuthContext = createContext<AuthState | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<UserResponse | null>(null);
  const [loading, setLoading] = useState<boolean>(Boolean(tokenStore.get()));

  useEffect(() => {
    if (!tokenStore.get()) return;
    apiFetch<UserResponse>('/users/me')
      .then(setUser)
      .catch(() => {
        tokenStore.clear();
        setUser(null);
      })
      .finally(() => setLoading(false));
  }, []);

  const login = async (req: LoginRequest) => {
    const res = await apiFetch<LoginResponse>('/auth/login', {
      method: 'POST',
      body: JSON.stringify(req),
    });
    tokenStore.set(res.accessToken);
    setUser(res.user);
    return res.user;
  };

  const logout = () => {
    tokenStore.clear();
    setUser(null);
  };

  return <AuthContext.Provider value={{ user, loading, login, logout }}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth doit être utilisé dans AuthProvider');
  return ctx;
}
```

#### Étape 2 — `frontend/src/components/ProtectedRoute.tsx`

```tsx
import { Navigate } from 'react-router-dom';
import { useAuth } from '../features/auth/AuthContext';
import type { Role } from '../types/contract';

export function ProtectedRoute({ roles, children }: { roles: Role[]; children: JSX.Element }) {
  const { user, loading } = useAuth();
  if (loading) return <div className="container">Chargement de la session…</div>;
  if (!user) return <Navigate to="/login" replace />;
  if (!roles.includes(user.role)) {
    return (
      <div className="container">
        <div className="alert-error">403 — Votre rôle ({user.role}) n&apos;autorise pas l&apos;accès à cet écran.</div>
      </div>
    );
  }
  return children;
}
```

#### Étape 3 — Layout et menu dynamique pour les **7 rôles** (§4) : `frontend/src/app/Layout.tsx`

```tsx
import { NavLink, Outlet } from 'react-router-dom';
import { useAuth } from '../features/auth/AuthContext';
import type { Role } from '../types/contract';

interface MenuItem {
  to: string;
  label: string;
  roles: 'PUBLIC' | Role[];
}

// Matrice §4 "Qui a le droit de faire quoi"
const MENU_ITEMS: MenuItem[] = [
  { to: '/map', label: 'Carte des vergers', roles: 'PUBLIC' },
  { to: '/farmer/orchards', label: 'Mes vergers', roles: ['FARMER'] },
  { to: '/farmer/requests', label: 'Mes demandes', roles: ['FARMER'] },
  { to: '/collection/requests', label: 'File demandes & tournées', roles: ['COLLECTION_MANAGER', 'ADMIN'] },
  { to: '/leader/tours', label: 'Mes tournées (terrain)', roles: ['TEAM_LEADER'] },
  { to: '/logistics/resources', label: 'Ressources', roles: ['LOGISTICS_MANAGER', 'COLLECTION_MANAGER', 'ADMIN'] },
  { to: '/press/receptions', label: 'Pressoir', roles: ['PRESS_MANAGER', 'COLLECTION_MANAGER', 'ADMIN'] },
  { to: '/dashboard', label: 'Tableau de bord', roles: ['COLLECTION_MANAGER', 'PRESS_MANAGER', 'ADMIN'] },
  { to: '/admin/users', label: 'Comptes & imports', roles: ['ADMIN'] },
];

export function Layout() {
  const { user, logout } = useAuth();
  const visibleItems = MENU_ITEMS.filter(
    (item) => item.roles === 'PUBLIC' || (user && item.roles.includes(user.role)),
  );

  return (
    <div className="app-shell">
      <header className="topbar">
        <strong>OliveConnect · Sfax</strong>
        <nav className="nav-links">
          {visibleItems.map((item) => (
            <NavLink key={item.to} to={item.to}>
              {item.label}
            </NavLink>
          ))}
        </nav>
        <div>
          {user ? (
            <span>
              {user.fullName} (<code>{user.role}</code>){' '}
              <button className="btn btn-secondary" onClick={logout}>
                Déconnexion
              </button>
            </span>
          ) : (
            <NavLink to="/login" style={{ color: '#fff' }}>
              Connexion (Visiteur)
            </NavLink>
          )}
        </div>
      </header>
      <main className="container">
        <Outlet />
      </main>
    </div>
  );
}
```

#### Étape 4 — Page de connexion avec sélecteur rapide des 6 comptes seedés : `frontend/src/features/auth/LoginPage.tsx`

```tsx
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ApiClientError } from '../../api/http';
import { SEEDED_ACCOUNTS } from '../../mocks/handlers';
import { useAuth } from './AuthContext';

export function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState('farmer@example.test');
  const [password, setPassword] = useState('Secret#123');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      const user = await login({ email, password });
      navigate(user.role === 'FARMER' ? '/farmer/orchards' : '/map');
    } catch (err) {
      setError(err instanceof ApiClientError ? err.apiError.message : 'Erreur de connexion');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="card" style={{ maxWidth: 460, margin: '1.5rem auto' }}>
      <h2>Connexion OliveConnect</h2>
      {error && <div className="alert-error">{error}</div>}

      <div className="form-row">
        <label htmlFor="quick-role">Compte de démo (seed B6)</label>
        <select
          id="quick-role"
          value={email}
          onChange={(e) => {
            setEmail(e.target.value);
            setPassword('Secret#123');
          }}
        >
          {SEEDED_ACCOUNTS.map((acc) => (
            <option key={acc.email} value={acc.email}>
              {acc.label}
            </option>
          ))}
        </select>
      </div>

      <form onSubmit={handleSubmit}>
        <div className="form-row">
          <label htmlFor="email">Email</label>
          <input id="email" type="email" required value={email} onChange={(e) => setEmail(e.target.value)} />
        </div>
        <div className="form-row">
          <label htmlFor="password">Mot de passe</label>
          <input id="password" type="password" required value={password} onChange={(e) => setPassword(e.target.value)} />
        </div>
        <button className="btn" type="submit" disabled={submitting}>
          {submitting ? 'Connexion…' : 'Se connecter'}
        </button>
      </form>
    </div>
  );
}
```

---

### C4 · Agriculteur : vergers + bouton « Ma position » (4 h) — J4

**Preuve** : liste des vergers de l'agriculteur + formulaire court avec bouton « Ma position » (`navigator.geolocation`) qui envoie bien `[longitude, latitude]` au contrat.

`frontend/src/features/orchards/OrchardsPage.tsx` :

```tsx
import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { ApiClientError, apiFetch } from '../../api/http';
import type { OrchardCreateRequest, OrchardResponse, PageResponse } from '../../types/contract';

const orchardSchema = z.object({
  name: z.string().trim().min(1, 'Obligatoire').max(100, '100 caractères maximum'),
  areaHectares: z.coerce.number().positive('Doit être > 0'),
  oliveVariety: z.string().trim().min(1, 'Obligatoire'),
  longitude: z.coerce.number().min(-180, 'Min -180').max(180, 'Max 180'),
  latitude: z.coerce.number().min(-90, 'Min -90').max(90, 'Max 90'),
  publicVisibility: z.boolean(),
});

type OrchardFormValues = z.infer<typeof orchardSchema>;

export function OrchardsPage() {
  const qc = useQueryClient();
  const [serverError, setServerError] = useState<string | null>(null);
  const [geoInfo, setGeoInfo] = useState<string | null>(null);

  const { data, isLoading, isError, error } = useQuery({
    queryKey: ['orchards', 'mine'],
    queryFn: () => apiFetch<PageResponse<OrchardResponse>>('/orchards/mine'),
  });

  const {
    register,
    handleSubmit,
    setValue,
    setError,
    reset,
    formState: { errors },
  } = useForm<OrchardFormValues>({
    resolver: zodResolver(orchardSchema),
    defaultValues: {
      name: '',
      areaHectares: 4.2,
      oliveVariety: 'Chemlali',
      longitude: 10.76, // Sfax par défaut
      latitude: 34.73,
      publicVisibility: true,
    },
  });

  const createMutation = useMutation({
    mutationFn: (payload: OrchardCreateRequest) =>
      apiFetch<OrchardResponse>('/orchards', { method: 'POST', body: JSON.stringify(payload) }),
    onSuccess: () => {
      setServerError(null);
      reset();
      qc.invalidateQueries({ queryKey: ['orchards'] });
    },
    onError: (err) => {
      if (err instanceof ApiClientError) {
        setServerError(err.apiError.message);
        Object.entries(err.apiError.fieldErrors).forEach(([field, msg]) => {
          if (field === 'location.coordinates') setError('longitude', { message: msg });
          else setError(field as keyof OrchardFormValues, { message: msg });
        });
      }
    },
  });

  const handleMyPosition = () => {
    if (!navigator.geolocation) {
      setGeoInfo('Géolocalisation non disponible sur ce navigateur.');
      return;
    }
    setGeoInfo('Acquisition GPS en cours…');
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        setValue('longitude', Number(pos.coords.longitude.toFixed(5)));
        setValue('latitude', Number(pos.coords.latitude.toFixed(5)));
        setGeoInfo('Position GPS renseignée ([longitude, latitude]).');
      },
      () => setGeoInfo('Accès GPS refusé — coordonnées de Sfax conservées.'),
    );
  };

  const onSubmit = (v: OrchardFormValues) => {
    // ATTENTION CONTRAT §5.2 : GeoJSON = [longitude, latitude]
    createMutation.mutate({
      name: v.name,
      areaHectares: v.areaHectares,
      oliveVariety: v.oliveVariety,
      location: { type: 'Point', coordinates: [v.longitude, v.latitude] },
      publicVisibility: v.publicVisibility,
    });
  };

  return (
    <div className="grid-2">
      <section className="card">
        <h2>Nouveau verger</h2>
        {serverError && <div className="alert-error">{serverError}</div>}
        <form onSubmit={handleSubmit(onSubmit)} noValidate>
          <div className="form-row">
            <label htmlFor="name">Nom du verger</label>
            <input id="name" {...register('name')} placeholder="Ex. North Orchard" />
            {errors.name && <p className="field-error">{errors.name.message}</p>}
          </div>

          <div className="form-row">
            <label htmlFor="areaHectares">Surface (hectares)</label>
            <input id="areaHectares" type="number" step="0.1" {...register('areaHectares')} />
            {errors.areaHectares && <p className="field-error">{errors.areaHectares.message}</p>}
          </div>

          <div className="form-row">
            <label htmlFor="oliveVariety">Variété d&apos;olive</label>
            <select id="oliveVariety" {...register('oliveVariety')}>
              <option value="Chemlali">Chemlali</option>
              <option value="Chetoui">Chetoui</option>
              <option value="Oueslati">Oueslati</option>
              <option value="Zalmati">Zalmati</option>
            </select>
          </div>

          <div className="form-row">
            <label>Position GeoJSON [longitude, latitude]</label>
            <div style={{ display: 'flex', gap: '0.5rem' }}>
              <input type="number" step="0.0001" aria-label="Longitude" {...register('longitude')} />
              <input type="number" step="0.0001" aria-label="Latitude" {...register('latitude')} />
              <button type="button" className="btn btn-secondary" onClick={handleMyPosition}>
                Ma position
              </button>
            </div>
            {geoInfo && <small>{geoInfo}</small>}
            {errors.longitude && <p className="field-error">{errors.longitude.message}</p>}
            {errors.latitude && <p className="field-error">{errors.latitude.message}</p>}
          </div>

          <div className="form-row">
            <label>
              <input type="checkbox" {...register('publicVisibility')} /> Visible sur la carte publique
            </label>
          </div>

          <button className="btn" type="submit" disabled={createMutation.isPending}>
            {createMutation.isPending ? 'Enregistrement…' : 'Créer le verger'}
          </button>
        </form>
      </section>

      <section className="card">
        <h2>Mes vergers ({data?.totalElements ?? 0})</h2>
        {isLoading && <p>Chargement de vos vergers…</p>}
        {isError && <div className="alert-error">{(error as Error).message}</div>}
        {data && data.content.length === 0 && <p>Aucun verger enregistré.</p>}
        {data && data.content.length > 0 && (
          <ul style={{ listStyle: 'none', padding: 0, margin: 0 }}>
            {data.content.map((o) => (
              <li key={o.id} style={{ padding: '0.75rem 0', borderBottom: '1px solid var(--border)' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <strong>{o.name}</strong>
                  <span className={`badge badge-${o.maturityStatus}`}>{o.maturityStatus}</span>
                </div>
                <small>
                  {o.areaHectares} ha · {o.oliveVariety} · [{o.location.coordinates[0]}, {o.location.coordinates[1]}]
                </small>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}
```

---

### C5 · Agriculteur : demandes de récolte (Zod + `fieldErrors`) (4 h) — J4 / J5

**Preuve** : formulaire court (planifiée / urgente) + liste « Mes demandes » avec pastilles de statut, états chargement / vide / erreur, et affichage des erreurs client **et** des `fieldErrors` serveur.

`frontend/src/features/harvest/HarvestRequestsPage.tsx` :

```tsx
import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { ApiClientError, apiFetch } from '../../api/http';
import type {
  HarvestRequestCreateRequest,
  HarvestRequestResponse,
  OrchardResponse,
  PageResponse,
} from '../../types/contract';

const harvestSchema = z
  .object({
    orchardId: z.string().min(1, 'Veuillez choisir un verger'),
    type: z.enum(['PLANNED', 'URGENT']),
    preferredDate: z.string().min(10, 'Date obligatoire (YYYY-MM-DD)'),
    estimatedQuantityKg: z.coerce.number().positive('Doit être > 0'),
    urgencyReason: z.string().max(500).optional(),
  })
  .superRefine((val, ctx) => {
    if (val.type === 'URGENT' && (!val.urgencyReason || !val.urgencyReason.trim())) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ['urgencyReason'],
        message: 'Obligatoire pour une demande urgente',
      });
    }
  });

type HarvestFormValues = z.infer<typeof harvestSchema>;

export function HarvestRequestsPage() {
  const qc = useQueryClient();
  const [serverError, setServerError] = useState<string | null>(null);

  const orchardsQuery = useQuery({
    queryKey: ['orchards', 'mine'],
    queryFn: () => apiFetch<PageResponse<OrchardResponse>>('/orchards/mine'),
  });

  const requestsQuery = useQuery({
    queryKey: ['harvest-requests', 'mine'],
    queryFn: () => apiFetch<PageResponse<HarvestRequestResponse>>('/harvest-requests/mine'),
  });

  const {
    register,
    handleSubmit,
    watch,
    setError,
    reset,
    formState: { errors },
  } = useForm<HarvestFormValues>({
    resolver: zodResolver(harvestSchema),
    defaultValues: {
      orchardId: '',
      type: 'PLANNED',
      preferredDate: new Date(Date.now() + 3 * 86400000).toISOString().slice(0, 10),
      estimatedQuantityKg: 1500,
      urgencyReason: '',
    },
  });

  const selectedType = watch('type');

  const createMutation = useMutation({
    mutationFn: (payload: HarvestRequestCreateRequest) =>
      apiFetch<HarvestRequestResponse>('/harvest-requests', {
        method: 'POST',
        body: JSON.stringify(payload),
      }),
    onSuccess: () => {
      setServerError(null);
      reset();
      qc.invalidateQueries({ queryKey: ['harvest-requests', 'mine'] });
    },
    onError: (err) => {
      if (err instanceof ApiClientError) {
        setServerError(err.apiError.message);
        Object.entries(err.apiError.fieldErrors).forEach(([field, msg]) => {
          setError(field as keyof HarvestFormValues, { type: 'server', message: msg });
        });
      }
    },
  });

  const onSubmit = (v: HarvestFormValues) => {
    createMutation.mutate({
      orchardId: v.orchardId,
      type: v.type,
      preferredDate: v.preferredDate,
      estimatedQuantityKg: v.estimatedQuantityKg,
      urgencyReason: v.type === 'URGENT' ? (v.urgencyReason?.trim() ?? null) : null,
    });
  };

  return (
    <div className="grid-2">
      <section className="card">
        <h2>Déposer une demande de récolte</h2>
        {serverError && <div className="alert-error">{serverError}</div>}

        <form onSubmit={handleSubmit(onSubmit)} noValidate>
          <div className="form-row">
            <label htmlFor="orchardId">Verger</label>
            <select id="orchardId" {...register('orchardId')}>
              <option value="">— Choisir un verger —</option>
              {orchardsQuery.data?.content.map((o) => (
                <option key={o.id} value={o.id}>
                  {o.name} ({o.areaHectares} ha)
                </option>
              ))}
            </select>
            {errors.orchardId && <p className="field-error">{errors.orchardId.message}</p>}
          </div>

          <div className="form-row">
            <label htmlFor="type">Type de demande</label>
            <select id="type" {...register('type')}>
              <option value="PLANNED">PLANNED (Planifiée)</option>
              <option value="URGENT">URGENT (Urgente)</option>
            </select>
          </div>

          {selectedType === 'URGENT' && (
            <div className="form-row">
              <label htmlFor="urgencyReason">Motif d&apos;urgence (obligatoire)</label>
              <input id="urgencyReason" {...register('urgencyReason')} placeholder="Ex. Maturité optimale / Pluie annoncée" />
              {errors.urgencyReason && <p className="field-error">{errors.urgencyReason.message}</p>}
            </div>
          )}

          <div className="form-row">
            <label htmlFor="preferredDate">Date souhaitée</label>
            <input id="preferredDate" type="date" {...register('preferredDate')} />
            {errors.preferredDate && <p className="field-error">{errors.preferredDate.message}</p>}
          </div>

          <div className="form-row">
            <label htmlFor="estimatedQuantityKg">Quantité estimée (kg)</label>
            <input id="estimatedQuantityKg" type="number" step="50" {...register('estimatedQuantityKg')} />
            {errors.estimatedQuantityKg && <p className="field-error">{errors.estimatedQuantityKg.message}</p>}
          </div>

          <button className="btn" type="submit" disabled={createMutation.isPending}>
            {createMutation.isPending ? 'Envoi…' : 'Soumettre la demande'}
          </button>
        </form>
      </section>

      <section className="card">
        <h2>Mes demandes ({requestsQuery.data?.totalElements ?? 0})</h2>
        {requestsQuery.isLoading && <p>Chargement des demandes…</p>}
        {requestsQuery.isError && <div className="alert-error">{(requestsQuery.error as Error).message}</div>}
        {requestsQuery.data && requestsQuery.data.content.length === 0 && <p>Aucune demande déposée.</p>}
        {requestsQuery.data && requestsQuery.data.content.length > 0 && (
          <ul style={{ listStyle: 'none', padding: 0, margin: 0 }}>
            {requestsQuery.data.content.map((r) => (
              <li key={r.id} style={{ padding: '0.75rem 0', borderBottom: '1px solid var(--border)' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <strong>{r.orchardName}</strong>
                  <span className={`badge badge-${r.status}`}>{r.status}</span>
                </div>
                <div>
                  {r.estimatedQuantityKg} kg · souhaitée le <strong>{r.preferredDate}</strong> · type{' '}
                  <code>{r.type}</code>
                </div>
                {r.urgencyReason && <small>Motif : {r.urgencyReason}</small>}
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}
```

---

### C6 · Carte de base Leaflet avec `bbox`, légende de maturité et attribution OSM (3 h) — J5

**Preuve** : la carte envoie `?bbox=minLon,minLat,maxLon,maxLat`, affiche le `FeatureCollection` GeoJSON du contrat (attention : GeoJSON est `[lon, lat]`, Leaflet attend `[lat, lon]` !), la légende de maturité et l'attribution OpenStreetMap obligatoire (§10 Risques).

#### Intégration de la carte pendant la Semaine 1

La carte Leaflet est développée et vérifiée avec MSW pendant la Semaine 1.

L'endpoint réel `GET /api/v1/orchards/map` étant prévu en Semaine 2 (tâche B7), la carte ne doit pas être considérée comme connectée au backend réel à la fin de la Semaine 1.

Critères d'acceptation S1 :

- [ ] La carte Leaflet s'affiche.
- [ ] Les données GeoJSON de démonstration sont affichées.
- [ ] Les coordonnées sont correctement converties `[lon, lat]` vers `[lat, lon]`.
- [ ] Le paramètre `bbox` est correctement construit.
- [ ] L'attribution OpenStreetMap est visible.
- [ ] Les mocks MSW permettent de tester la carte sans endpoint réel.

Le branchement sur l'endpoint réel sera effectué lorsque celui-ci sera disponible en Semaine 2.

`frontend/src/features/map/OrchardsMapPage.tsx` :

```tsx
import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { CircleMarker, MapContainer, Popup, TileLayer, useMapEvents } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import { apiFetch } from '../../api/http';
import type { MaturityStatus, OrchardMapFeatureCollection } from '../../types/contract';

const MATURITY_COLORS: Record<MaturityStatus, string> = {
  UNKNOWN: '#667085',
  IMMATURE: '#2e90fa',
  APPROACHING: '#f79009',
  OPTIMAL: '#12b76a',
  OVERRIPE: '#f04438',
};

function BboxWatcher({ onChange }: { onChange: (bbox: string) => void }) {
  useMapEvents({
    moveend: (e) => {
      const b = e.target.getBounds();
      onChange(`${b.getWest().toFixed(4)},${b.getSouth().toFixed(4)},${b.getEast().toFixed(4)},${b.getNorth().toFixed(4)}`);
    },
  });
  return null;
}

export function OrchardsMapPage() {
  // Zone initiale autour de Sfax : minLon,minLat,maxLon,maxLat
  const [bbox, setBbox] = useState('10.50,34.50,11.05,35.00');

  const { data, isLoading, isError } = useQuery({
    queryKey: ['orchards-map', bbox],
    queryFn: () => apiFetch<OrchardMapFeatureCollection>(`/orchards/map?bbox=${encodeURIComponent(bbox)}`),
  });

  return (
    <div className="card">
      <div style={{ display: 'flex', flexWrap: 'wrap', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.75rem' }}>
        <h2 style={{ margin: 0 }}>Carte des vergers — Région de Sfax</h2>
        <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
          {(Object.keys(MATURITY_COLORS) as MaturityStatus[]).map((m) => (
            <span key={m} className={`badge badge-${m}`}>
              ● {m}
            </span>
          ))}
        </div>
      </div>

      {isLoading && <p>Chargement de la zone ({bbox})…</p>}
      {isError && (
        <p className="field-error">
          L&apos;endpoint <code>GET /api/v1/orchards/map</code> (tâche B7, semaine 2) n&apos;est pas encore actif côté serveur — activez{' '}
          <code>VITE_USE_MOCKS=true</code> pour tester la carte en S1.
        </p>
      )}
      {data?.truncated && <div className="alert-error">Résultat tronqué à 500 vergers : zoomez sur la carte.</div>}

      {/* Leaflet attend [latitude, longitude] alors que GeoJSON fournit [longitude, latitude] */}
      <MapContainer center={[34.73, 10.76]} zoom={11} style={{ height: 440, width: '100%', borderRadius: 8 }}>
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />
        <BboxWatcher onChange={setBbox} />
        {data?.features.map((f) => {
          const [lon, lat] = f.geometry.coordinates;
          return (
            <CircleMarker
              key={f.properties.orchardId}
              center={[lat, lon]}
              radius={9}
              pathOptions={{ color: MATURITY_COLORS[f.properties.maturityStatus], fillOpacity: 0.8 }}
            >
              <Popup>
                <strong>{f.properties.name ?? 'Verger public'}</strong>
                <br />
                Variété : {f.properties.oliveVariety}
                <br />
                Maturité : {f.properties.maturityStatus}
                {f.properties.areaHectares != null && (
                  <>
                    <br />
                    Surface : {f.properties.areaHectares} ha
                  </>
                )}
              </Popup>
            </CircleMarker>
          );
        })}
      </MapContainer>
    </div>
  );
}
```

#### Étape finale C — `frontend/src/app/App.tsx` et `frontend/src/main.tsx`

`frontend/src/app/App.tsx` :

```tsx
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider } from '../features/auth/AuthContext';
import { LoginPage } from '../features/auth/LoginPage';
import { OrchardsPage } from '../features/orchards/OrchardsPage';
import { HarvestRequestsPage } from '../features/harvest/HarvestRequestsPage';
import { OrchardsMapPage } from '../features/map/OrchardsMapPage';
import { ProtectedRoute } from '../components/ProtectedRoute';
import { Layout } from './Layout';

const queryClient = new QueryClient();

function RolePlaceholder({ title }: { title: string }) {
  return (
    <div className="card">
      <h2>{title}</h2>
      <p>Squelette par rôle prêt (C1/C3) — branchement API en semaine 2 (C7 à C12).</p>
    </div>
  );
}

export function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <BrowserRouter>
          <Routes>
            <Route element={<Layout />}>
              <Route index element={<Navigate to="/map" replace />} />
              <Route path="/login" element={<LoginPage />} />
              <Route path="/map" element={<OrchardsMapPage />} />
              <Route
                path="/farmer/orchards"
                element={
                  <ProtectedRoute roles={['FARMER']}>
                    <OrchardsPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/farmer/requests"
                element={
                  <ProtectedRoute roles={['FARMER']}>
                    <HarvestRequestsPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/collection/requests"
                element={
                  <ProtectedRoute roles={['COLLECTION_MANAGER', 'ADMIN']}>
                    <RolePlaceholder title="File des demandes & planification des tournées (S2 · C7/C8)" />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/leader/tours"
                element={
                  <ProtectedRoute roles={['TEAM_LEADER']}>
                    <RolePlaceholder title="Mes tournées assignées — Chef d'équipe (S2 · C9)" />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/logistics/resources"
                element={
                  <ProtectedRoute roles={['LOGISTICS_MANAGER', 'COLLECTION_MANAGER', 'ADMIN']}>
                    <RolePlaceholder title="Gestion des ressources — Logistique (S2 · C12)" />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/press/receptions"
                element={
                  <ProtectedRoute roles={['PRESS_MANAGER', 'COLLECTION_MANAGER', 'ADMIN']}>
                    <RolePlaceholder title="Réceptions au pressoir (S2 · C10)" />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/dashboard"
                element={
                  <ProtectedRoute roles={['COLLECTION_MANAGER', 'PRESS_MANAGER', 'ADMIN']}>
                    <RolePlaceholder title="Tableau de bord des performances (S3 · C14)" />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/admin/users"
                element={
                  <ProtectedRoute roles={['ADMIN']}>
                    <RolePlaceholder title="Administration des comptes & imports (S3 · C15)" />
                  </ProtectedRoute>
                }
              />
            </Route>
          </Routes>
        </BrowserRouter>
      </AuthProvider>
    </QueryClientProvider>
  );
}
```

`frontend/src/main.tsx` (active MSW uniquement quand `VITE_USE_MOCKS=true`) :

```tsx
import React from 'react';
import ReactDOM from 'react-dom/client';
import { App } from './app/App';
import './index.css';

async function bootstrap() {
  if (import.meta.env.VITE_USE_MOCKS === 'true') {
    const { worker } = await import('./mocks/browser');
    await worker.start({ onUnhandledRequest: 'bypass' });
  }
  ReactDOM.createRoot(document.getElementById('root')!).render(
    <React.StrictMode>
      <App />
    </React.StrictMode>,
  );
}

bootstrap();
```

Commandes pour lancer le front :
- **Sur mocks (J2 → J4, sans attendre A et B)** :
  ```powershell
  cd frontend
  $env:VITE_USE_MOCKS="true"; npm run dev
  ```
- **Sur le vrai backend Spring Boot (J5, après fusion des PR de A et B)** :
  ```powershell
  cd frontend
  $env:VITE_USE_MOCKS="false"; npm run dev
  ```

---

## 6. Intégration et démo du vendredi (Semaine 1)

Voici le **script exact de vérification** à exécuter ensemble le vendredi après-midi sur **chacune des 3 machines** (plan §8 « Vendredi, on doit pouvoir montrer ») :

### Étape 1 — Vérifier le tag `contracts-v1`

```powershell
git fetch --tags
git show contracts-v1 --stat
```

### Étape 2 — Démarrer la pile, exécuter les tests et charger le seed

```powershell
# 1. Pile MongoDB + Redis
docker compose -f infra/compose.yaml --env-file .env up -d

# 2. Suite de tests d'intégration (A2, A3, A4, A5, A6, B3, B5)
cd backend
.\mvnw.cmd test

# 3. Charger les données de démo (20 agriculteurs + les 6 comptes par rôle)
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--seed --farmers=20"

# 4. Lancer le backend normalement
.\mvnw.cmd spring-boot:run
```

### Étape 3 — Démarrer le frontend branché sur l'API réelle

```powershell
cd frontend
$env:VITE_USE_MOCKS="false"
npm run dev
```

### Étape 4 — Jouer la tranche verticale dans le navigateur (`http://localhost:5173`)

1. Se connecter avec `farmer@example.test` / `Secret#123`.
2. Aller dans **Mes vergers**, cliquer sur **Ma position** (ou garder `[10.76, 34.73]`), créer un verger `Verger Démo Vendredi` (3.5 ha, `Chemlali`) → il apparaît dans la liste avec `UNKNOWN`.
3. Aller dans **Mes demandes**, sélectionner `Verger Démo Vendredi`, choisir `URGENT`, saisir `2200` kg et le motif `Maturité optimale`, soumettre → la demande apparaît avec la pastille `SUBMITTED`.

### Étape 5 — Retrouver la demande en `SUBMITTED` dans MongoDB (`mongosh`)

```powershell
docker exec -it oliveconnect-mongodb mongosh "mongodb://oliveconnect:change-me@localhost:27017/oliveconnect?authSource=admin" --eval "db.harvest_requests.find({ status: 'SUBMITTED' }, { orchardName: 1, status: 1, estimatedQuantityKg: 1, farmerId: 1 }).limit(3)"
```

### Étape 6 — Démontrer les codes de sécurité **401**, **403** et **404** en direct

```powershell
# 1) Sans token -> 401 UNAUTHENTICATED (enveloppe du contrat)
curl.exe -i http://localhost:8080/api/v1/orchards/mine

# 2) Connexion en responsable de collecte, puis tentative de créer un verger (réservé FARMER) -> 403 ACCESS_DENIED
$login = Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/auth/login -ContentType "application/json" -Body '{"email":"collection@example.test","password":"Secret#123"}'
curl.exe -i -X POST http://localhost:8080/api/v1/orchards -H "Authorization: Bearer $($login.accessToken)" -H "Content-Type: application/json" -d "{\"name\":\"Test\",\"areaHectares\":2,\"oliveVariety\":\"Chemlali\",\"location\":{\"type\":\"Point\",\"coordinates\":[10.76,34.73]},\"publicVisibility\":true}"
```

### Étape 7 — Fiches des 3 présentations de 5 minutes du vendredi (plan §2 & §9)

> **Règle §9** : *« Chaque vendredi, démo de l'équipe puis chacun explique en 5 minutes un module d'un autre. C'est ce qui garantit que les trois comprennent toute l'architecture pour la soutenance. »*

1. **Présentation 1 — MEMBRE A (5 min) : *Les 4 couches, DTO, sécurité JWT et un test 403 / 404***
   - Montrer le chemin d'une requête `POST /api/v1/orchards` : `OrchardController` (`@Valid OrchardCreateRequest`, `@PreAuthorize("hasRole('FARMER')")`, code `201`) → `OrchardService` (prend `farmerId` depuis `AuthUser` du JWT, jamais depuis le JSON ; appelle `CacheInvalidator`) → `OrchardRepository` (Spring Data MongoDB) → `OrchardDocument` → reconverti en `OrchardResponse` (jamais de `Document` ni de `GeoJsonPoint` brut dans la réponse).
   - Montrer la différence entre **Niveau 1 (Rôle → 403)** dans `AuthIntegrationTest.wrongRole_returns403` et **Niveau 2 (Propriété → 404)** dans `OrchardIntegrationTest.farmerA_cannotReadFarmerB_orchard_404` (`loadReadable` renvoie `404` pour ne pas révéler l'existence du verger d'un autre agriculteur).

2. **Présentation 2 — MEMBRE B (5 min) : *Imbrication vs référence, règles UML→NoSQL et ordre GeoJSON `[lon, lat]`***
   - Ouvrir `docs/nosql-model.md` : expliquer pourquoi les arrêts d'une tournée (`Tour.stops`, bornés à **20 maximum** et toujours lus avec la tournée) sont **imbriqués** (règle `R3`), tandis que les vergers (`orchards`), les demandes (`harvest_requests`) et surtout les historiques non bornés (`maturity_readings`, `weather_observations`) sont dans des **collections séparées** avec références et index composés (règles `R2` et `R4`), plus la duplication contrôlée de `orchardName` (règle `R6`).
   - Ouvrir `MongoIndexConfigTest` : montrer l'index `2dsphere` sur `orchards.location`, rappeler que GeoJSON impose **`[longitude, latitude]`** (`[10.76, 34.73]` pour Sfax), et montrer pourquoi `nearSphere` échoue si l'index `2dsphere` n'existe pas.

3. **Présentation 3 — MEMBRE C (5 min) : *Mocks MSW du contrat et carte Leaflet avec `bbox`***
   - Ouvrir `frontend/src/types/contract.ts` et `frontend/src/mocks/handlers.ts` : montrer comment MSW intercepte `/api/v1/*` avec les exemples exacts du contrat gelé (`contracts-v1`), ce qui permet au front d'avancer dès le jour 3 sans attendre le backend, puis de basculer la tranche verticale (connexion, vergers, demandes) sur Spring Boot en passant `VITE_USE_MOCKS=false`.
   - Ouvrir `OrchardsMapPage.tsx` : montrer que la carte ne charge jamais toute la collection mais envoie la zone visible `?bbox=minLon,minLat,maxLon,maxLat`, affiche l'attribution OpenStreetMap et la légende de maturité, et inverse `[lon, lat]` (GeoJSON du contrat) en `[lat, lon]` uniquement au moment d'afficher le `<CircleMarker>` Leaflet.

### 6.1 Checklist finale d'intégration — Vendredi

Chaque point doit être vérifié sur les trois machines.

#### Environnement

- [ ] Le dépôt est à jour (`git pull`).
- [ ] Le tag `contracts-v1` est disponible.
- [ ] MongoDB démarre.
- [ ] Redis démarre.
- [ ] Le backend démarre.
- [ ] Le frontend démarre.

#### Authentification

- [ ] Connexion avec un compte FARMER.
- [ ] Réponse de connexion conforme à `LoginResponse`.
- [ ] Accès à une route protégée avec token.
- [ ] Appel sans token : `401 UNAUTHENTICATED`.

#### Vergers

- [ ] Création d'un verger.
- [ ] Vérification de ses coordonnées GeoJSON.
- [ ] Vérification du statut initial `UNKNOWN`.
- [ ] Affichage du verger dans la liste.
- [ ] Tentative d'accès à un verger appartenant à un autre agriculteur : `404 NOT_FOUND`.

#### Demandes de récolte

- [ ] Création d'une demande `PLANNED`.
- [ ] Création d'une demande `URGENT` avec motif.
- [ ] Vérification du statut initial `SUBMITTED`.
- [ ] Vérification de la présence du document dans MongoDB.
- [ ] Quantité invalide : `400 VALIDATION_ERROR`.
- [ ] Motif absent pour une demande urgente : `400 VALIDATION_ERROR`.

#### Autorisations

- [ ] Un utilisateur sans token reçoit `401`.
- [ ] Un utilisateur avec un rôle incorrect reçoit `403`.
- [ ] Un agriculteur ne peut pas accéder aux données privées d'un autre agriculteur.

#### Qualité

- [ ] `mvnw test` passe.
- [ ] `npm run build` passe.
- [ ] Aucune erreur critique dans la console navigateur.
- [ ] Aucune erreur critique dans les logs backend.
- [ ] Les trois membres peuvent reproduire la démonstration.

---

## 7. Checklists « fait » et pièges à éviter

### 7.1 Définition de « fait » (plan §10) — à cocher sur chaque PR

- [ ] Code fusionné dans `main` par une PR **relue par un autre membre** (A→B, B→C, C→A), build et tests verts.
- [ ] La réponse JSON respecte le contrat (`assertExactKeys` : mêmes clés, ni plus ni moins).
- [ ] Le cas nominal **et au moins un cas d'erreur** (`400`, `401`, `403`, `404` ou `409`) sont testés.
- [ ] La preuve de la colonne « Livrable » existe (test, capture ou document dans `docs/`).
- [ ] La documentation du module est à jour dans `docs/` (`requirements.md`, `use-cases.md`, `class-diagram.md`, `api-contract.md`, `nosql-model.md`).

### 7.2 Les 8 pièges classiques de la Semaine 1

| Piège | Conséquence | Parade appliquée dans ce guide |
|---|---|---|
| Inverser `[latitude, longitude]` dans MongoDB | Requêtes `2dsphere` fausses ou rejetées ; points en mer sur Leaflet | Toujours **`[longitude, latitude]`** (`[10.76, 34.73]`) dans l'API et Mongo ; conversion `[lat, lon]` uniquement dans le composant Leaflet (`C6`) |
| Renvoyer `GeoJsonPoint` de Spring Data directement dans un DTO | JSON pollué avec `"x": 10.76, "y": 34.73` → viole le test de contrat | Toujours convertir en `GeoPointDto` (`type` + `coordinates` uniquement) |
| Stocker `BigDecimal` sans `@Field(targetType = DECIMAL128)` | Stocké en `String` dans Mongo → `$sum` / `$avg` du dashboard (S3) renvoient `0` | `@Field(targetType = FieldType.DECIMAL128)` dès S1 sur `estimatedQuantityKg` et `actualQuantityKg` |
| Accepter `farmerId` ou `status` dans le JSON de requête | Usurpation d'identité ou saut d'état | Ne pas mettre ces champs dans `OrchardCreateRequest` / `HarvestRequestCreateRequest` ; prendre `me.id()` depuis le JWT |
| Renvoyer `403` quand l'agriculteur A demande le verger de B | Révèle l'existence de l'ID d'un autre agriculteur (viole §3.4) | Renvoyer **`404 NOT_FOUND`** dans `loadReadable(...)` |
| Laisser l'enveloppe d'erreur par défaut de Spring Security sur 401/403 | JSON différent de `ApiError` quand le filtre bloque la requête | `RestSecurityErrorHandler` délègue au `HandlerExceptionResolver` → passe toujours par `GlobalExceptionHandler` |
| Exposer `27017` ou `6379` sur `0.0.0.0` | Base ouverte sur le réseau local / la VM | `"127.0.0.1:27017:27017"` et `"127.0.0.1:6379:6379"` dans `infra/compose.yaml` |
| Coder des endpoints avant le tag `contracts-v1` | Front et back divergent dès le jour 3 | Gel obligatoire en fin de J2 (`COM4`) avant `A3` / `B5` / `C3` |

### 7.3 Problèmes fréquents pendant la Semaine 1

| Problème | Cause probable | Vérification / Solution |
|---|---|---|
| Docker ne démarre pas | Docker Desktop arrêté | Démarrer Docker Desktop et vérifier `docker info`. |
| MongoDB connection refused | Conteneur arrêté ou mauvaise URI | Vérifier `docker compose ps` et `MONGODB_URI`. |
| Redis connection refused | Conteneur arrêté ou mauvais port | Vérifier `docker compose ps` et `REDIS_HOST`. |
| Backend ne démarre pas | Variable JWT absente | Vérifier le fichier `.env` et `JWT_SECRET`. |
| Backend ne compile pas | Version Java incorrecte | Vérifier `java -version` et utiliser Java 21. |
| Frontend ne démarre pas | Dépendances absentes | Exécuter `npm install`. |
| Frontend ne trouve pas l'API | Backend arrêté ou mauvais proxy | Vérifier le backend et la configuration Vite. |
| Login mock fonctionne mais pas réel | Configuration MSW encore active | Vérifier `VITE_USE_MOCKS=false`. |
| Erreur 401 | Token absent ou invalide | Vérifier le header `Authorization: Bearer`. |
| Erreur 403 | Rôle insuffisant | Vérifier le rôle du compte et les autorisations. |
| Carte vide | Mauvais bbox ou données mock absentes | Vérifier le format bbox et les handlers MSW (`VITE_USE_MOCKS=true` pour C6 en S1). |
| PR impossible à fusionner | Conflit Git | Récupérer `main`, résoudre les conflits et relancer les tests. |
