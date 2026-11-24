#!/bin/sh
# Restaure la base depuis une sauvegarde. Le backend doit être arrêté pendant l'opération.
# Usage : restore.sh --yes /backups/chemin_de_guer_AAAAMMJJ_HHMMSS.sql.gz[.enc]
. /etc/backup.env
set -eu

if [ "${1:-}" != "--yes" ] || [ -z "${2:-}" ]; then
  echo "Usage : restore.sh --yes /backups/<fichier>.sql.gz[.enc]"
  echo "ATTENTION : remplace le contenu actuel de la base ${DB_NAME}. Arrêtez le backend avant."
  echo "Sauvegardes disponibles :"; ls -1t /backups/chemin_de_guer_*.sql.gz* 2>/dev/null | head -20
  exit 1
fi
FILE="$2"
[ -f "$FILE" ] || { echo "Fichier introuvable : $FILE"; exit 1; }
export PGPASSWORD="$DB_PASSWORD"

echo "Restauration de $FILE dans ${DB_NAME}..."
case "$FILE" in
  *.enc)
    [ -n "${BACKUP_PASSPHRASE:-}" ] || { echo "Cette sauvegarde est chiffrée : BACKUP_PASSPHRASE est requis."; exit 1; }
    openssl enc -d -aes-256-cbc -pbkdf2 -pass env:BACKUP_PASSPHRASE -in "$FILE" | gunzip | psql -v ON_ERROR_STOP=1 -q -h "${DB_HOST:-cdg-db}" -U "$DB_USER" -d "$DB_NAME" ;;
  *) gunzip -c "$FILE" | psql -v ON_ERROR_STOP=1 -q -h "${DB_HOST:-cdg-db}" -U "$DB_USER" -d "$DB_NAME" ;;
esac
echo "Restauration terminée. Redémarrez le backend."
