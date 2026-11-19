# 🫒 OliveConnect — Plateforme Logistique Oléicole (Sfax)

> **OliveConnect** est une plateforme web moderne conçue pour optimiser et coordonner la récolte des olives dans le gouvernorat de Sfax (Tunisie). Elle interconnecte les agriculteurs, les équipes de collecte, la logistique et les pressoirs.

**Technologies :**
- **Backend :** Java 21 · Spring Boot 3.4.2 · Spring Security (JWT HS256) · Spring Data MongoDB & Redis
- **Bases de données :** MongoDB 8.0 (index géospatiaux `2dsphere` + index versionnés) · Redis 7.4 (cache)
- **Frontend :** React 18 · TypeScript · Vite 6 · Tailwind CSS v4 · Lucide Icons · Leaflet (Cartographie OSM) · TanStack Query · React Hook Form + Zod

---

## 🚀 Guide de démarrage rapide (Pour l'équipe)

Suivez ces étapes dans l'ordre après avoir cloné ou tiré (`pull`) le dépôt.

### 1. Prérequis sur votre machine
- **Git**
- **Docker Desktop** (doit être démarré)
- **Java 21 (JDK 21)**
- **Node.js 18+** & **npm**

---

### 2. Étape 1 : Récupérer le code
```bash
git pull origin main
```

---

### 3. Étape 2 : Configurer les variables d'environnement
À la racine du projet (`OliveConnect`), copiez le fichier d'exemple `.env.example` vers `.env` :

**Sur Windows (PowerShell) :**
```powershell
Copy-Item .env.example .env
```

**Sur Linux / macOS :**
```bash
cp .env.example .env
```

> ⚠️ Ne modifiez pas les valeurs par défaut pour un développement local standard. Le fichier `.env` est ignoré par Git.

---

### 4. Étape 3 : Démarrer MongoDB & Redis avec Docker
Depuis la racine du projet (`OliveConnect`) :

```powershell
docker compose -f infra/compose.yaml --env-file .env up -d
```

Vérifiez que les deux conteneurs sont sains (`healthy`) :
```powershell
docker compose -f infra/compose.yaml ps
```
* MongoDB tourne sur `localhost:27017`
* Redis tourne sur `localhost:6379`

---

### 5. Étape 4 : Lancer le Backend Spring Boot (avec données de démo)

Ouvrez un terminal et allez dans le dossier `backend` :

**Sur Windows (PowerShell) :**
```powershell
cd backend

# Si vous avez plusieurs versions de Java, assurez-vous d'utiliser Java 21 :
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

# Lancer Spring Boot avec l'argument de peuplement (20 agriculteurs + comptes de démo) :
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--seed --farmers=20"
```

**Sur Linux / macOS :**
```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.arguments="--seed --farmers=20"
```

Le backend démarrera sur **`http://localhost:8080`**.  
L'argument `--seed --farmers=20` crée automatiquement tous les index MongoDB, 6 comptes de test pour chaque rôle, 20 agriculteurs avec leurs vergers autour de Sfax, des ouvriers, véhicules, bennes, matériels et pressoirs.

---

### 6. Étape 5 : Lancer le Frontend React

Dans un **deuxième terminal**, allez dans le dossier `frontend` :

```powershell
cd frontend
npm install
npm run dev
```

L'application est accessible sur : **[http://localhost:5173](http://localhost:5173)**

---

## 👥 Comptes de démonstration pré-configurés

Tous les comptes ont le même mot de passe : **`Secret#123`**

Sur la page de connexion ([http://localhost:5173/login](http://localhost:5173/login)), utilisez le menu déroulant **« Compte de démo (seed B6 / MSW) »** pour vous connecter instantanément avec n'importe quel rôle :

| Rôle | Email | Mot de passe | Accès & Fonctionnalités |
|---|---|---|---|
| **Agriculteur** | `farmer@example.test` | `Secret#123` | Gestion de ses vergers, bouton « Ma position », dépôt de demandes de récolte |
| **Resp. Collecte** | `collection@example.test` | `Secret#123` | File de demandes, validation/rejet, changement de maturité, tournées |
| **Resp. Logistique** | `logistics@example.test` | `Secret#123` | CRUD ouvriers, tracteurs/camions, bennes et équipements de récolte |
| **Chef d'Équipe** | `leader@example.test` | `Secret#123` | Suivi et démarrage des tournées terrain |
| **Resp. Pressoir** | `press@example.test` | `Secret#123` | Réception des bennes, pesée et enregistrement des qualités |
| **Administrateur** | `admin@example.test` | `Secret#123` | Accès complet, gestion des utilisateurs, imports et tableaux de bord |

---

## 🧪 Lancer les tests automatisés

### Tests d'intégration Backend (Spring Boot + MongoDB)
Exécute l'ensemble des 26 tests (Indexation versionnée, Authentification JWT, Vergers, Demandes de récolte, Ressources) :

```powershell
cd backend
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"; $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
.\mvnw.cmd test
```

### Vérification du Build Frontend (TypeScript + Vite)
```powershell
cd frontend
npm run build
```

---

## 🛠️ Dépannage fréquent

1. **Erreur port 8080 déjà utilisé :**
   Trouvez le processus qui utilise le port 8080 et arrêtez-le :
   ```powershell
   Get-NetTCPConnection -LocalPort 8080 -State Listen | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force }
   ```
2. **Erreur de compilation Java / Lombok :**
   Assurez-vous que votre terminal utilise bien le JDK 21 et non une version supérieure (Java 22/25) :
   ```powershell
   java -version
   ```
3. **Erreur de connexion Docker :**
   Assurez-vous que Docker Desktop est ouvert et actif, puis relancez `docker compose -f infra/compose.yaml up -d`.
