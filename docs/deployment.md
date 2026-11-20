# Guide de Déploiement & Exploitation en Production

## 1. Architecture Déployée (Docker Compose Prod)

Le déploiement en production repose sur `infra/compose.prod.yaml` orchestrant 4 conteneurs :
1. **`oliveconnect-web`** (Nginx 1.27 Alpine) :
   - Reverse-proxy HTTP (port 80 exposé).
   - Sert le build statique React (SPA `try_files`).
   - Route `/api/` vers `oliveconnect-backend:8080`.
   - Limite d'upload `client_max_body_size 6M`.
2. **`oliveconnect-backend`** (JRE 21 Alpine) :
   - Application Spring Boot conteneurisée.
   - Utilisateur non-root `app`.
   - Healthcheck automatique sur `/actuator/health`.
3. **`oliveconnect-mongodb`** (Mongo 8.0) :
   - Réseau interne Docker uniquement (**aucun port exposé à l'extérieur**).
   - Volume persistant `mongo_data`.
4. **`oliveconnect-redis`** (Redis 7.4 Alpine) :
   - Réseau interne Docker uniquement (**aucun port exposé à l'extérieur**).
   - Mode volatile en mémoire sans persistance disque (`--save ""` `--appendonly no`).

---

## 2. Procédure de Déploiement sur VM

```bash
# 1. Cloner ou mettre à jour le dépôt sur la VM
cd /opt/oliveconnect
git pull origin main

# 2. Configurer le fichier d'environnement .env
cp .env.example .env
nano .env # Renseigner mot de passe Mongo et JWT secret

# 3. Lancer la pile de production
docker compose -f infra/compose.prod.yaml --env-file .env up -d --build

# 4. Vérifier l'état des services et l'accès
docker compose -f infra/compose.prod.yaml ps
curl -fsS http://localhost/actuator/health # Doit renvoyer {"status":"UP"}
```

---

## 3. Procédure de Sauvegarde & Restauration

### 3.1 Sauvegarde Planifiée
Ajouter dans le crontab système (`crontab -e`) :
```bash
0 2 * * * /opt/oliveconnect/infra/scripts/backup-mongo.sh >> /var/log/oliveconnect-backup.log 2>&1
```

### 3.2 Restauration d'Urgence
```bash
# Identifier la dernière archive disponible
LATEST=$(ls -t /opt/oliveconnect/backups/oliveconnect-*.archive.gz | head -n1)

# Restaurer avec purge automatique des anciennes données
source /opt/oliveconnect/.env
cat "$LATEST" | docker exec -i oliveconnect-mongodb mongorestore \
  --uri="mongodb://${MONGO_ROOT_USERNAME}:${MONGO_ROOT_PASSWORD}@localhost:27017/oliveconnect?authSource=admin" \
  --archive --gzip --drop
```
