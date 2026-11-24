#!/usr/bin/env sh
# Sauvegarde de la base PostgreSQL dans ./backups (à lancer depuis la racine du dépôt).
# Exemple de cron quotidien à 3h : 0 3 * * * cd /chemin/vers/chemin-de-guer && ./scripts/backup.sh
set -eu
mkdir -p backups
set -a; [ -f .env ] && . ./.env; set +a
FILE="backups/chemin_de_guer_$(date +%Y%m%d_%H%M%S).sql.gz"
docker compose exec -T cdg-db pg_dump -U "${DB_USER:-postgres}" "${DB_NAME:-chemin_de_guer}" | gzip > "$FILE"
echo "Sauvegarde créée : $FILE"
# Conserve les 14 dernières sauvegardes
ls -1t backups/*.sql.gz | tail -n +15 | xargs -r rm --
