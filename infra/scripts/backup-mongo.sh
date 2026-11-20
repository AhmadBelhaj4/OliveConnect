#!/usr/bin/env bash
set -euo pipefail
STAMP=$(date -u +"%Y%m%dT%H%M%SZ")
BACKUP_DIR="/opt/oliveconnect/backups"
mkdir -p "$BACKUP_DIR"

source /opt/oliveconnect/.env
docker exec oliveconnect-mongodb mongodump \
  --uri="mongodb://${MONGO_ROOT_USERNAME}:${MONGO_ROOT_PASSWORD}@localhost:27017/oliveconnect?authSource=admin" \
  --archive --gzip > "${BACKUP_DIR}/oliveconnect-${STAMP}.archive.gz"

# Rétention 7 jours
find "$BACKUP_DIR" -name "oliveconnect-*.archive.gz" -mtime +7 -delete
echo "Sauvegarde OK : ${BACKUP_DIR}/oliveconnect-${STAMP}.archive.gz"
