#!/bin/sh
# Sauvegarde la base : dump compressé et vérifié, chiffrement facultatif, rétention, copie hors serveur.
. /etc/backup.env
set -eu

DIR=/backups
KEEP="${BACKUP_KEEP:-30}"
mkdir -p "$DIR"
STAMP=$(date +%Y%m%d_%H%M%S)
BASE="$DIR/chemin_de_guer_$STAMP.sql.gz"
TMP="$BASE.tmp"
export PGPASSWORD="$DB_PASSWORD"

echo "[$(date -Iseconds)] Sauvegarde de la base ${DB_NAME}..."
# --clean --if-exists : la restauration remplace proprement les tables existantes
pg_dump -h "${DB_HOST:-cdg-db}" -U "$DB_USER" -d "$DB_NAME" --clean --if-exists --no-owner | gzip -9 > "$TMP" || true

# Contrôles : archive lisible ET dump terminé (la dernière ligne d'un pg_dump complet le déclare)
if ! gzip -t "$TMP" 2>/dev/null || ! zcat "$TMP" | tail -n 5 | grep -q "PostgreSQL database dump complete"; then
  rm -f "$TMP"
  echo "ERREUR : la sauvegarde est incomplète ou illisible, elle a été supprimée."
  exit 1
fi

FINAL="$BASE"
if [ -n "${BACKUP_PASSPHRASE:-}" ]; then
  openssl enc -aes-256-cbc -pbkdf2 -salt -pass env:BACKUP_PASSPHRASE -in "$TMP" -out "$BASE.enc"
  rm -f "$TMP"
  FINAL="$BASE.enc"
else
  mv "$TMP" "$BASE"
fi
echo "OK : $FINAL ($(wc -c < "$FINAL") octets)"
echo "$(date -Iseconds) $FINAL" > "$DIR/last_success"

# Rétention locale : les $KEEP plus récentes
ls -1t "$DIR"/chemin_de_guer_*.sql.gz* 2>/dev/null | tail -n +"$((KEEP + 1))" | xargs -r rm -f --

# Copie hors serveur
if [ -n "${BACKUP_REMOTE:-}" ]; then
  if rclone copy "$FINAL" "$BACKUP_REMOTE" --retries 3 --low-level-retries 5; then
    echo "OK : copie hors serveur effectuée."
    echo "$(date -Iseconds) $BACKUP_REMOTE" > "$DIR/last_remote_success"
    # Rétention distante par nombre (jamais par âge : si les sauvegardes s'arrêtent, rien n'est supprimé)
    rclone lsf "$BACKUP_REMOTE" --files-only 2>/dev/null | grep '^chemin_de_guer_' | sort -r | tail -n +"$((KEEP + 1))" \
      | while read -r old; do rclone deletefile "$BACKUP_REMOTE/$old" && echo "Ancienne sauvegarde distante supprimée : $old"; done
  else
    echo "ERREUR : la copie hors serveur a échoué. La sauvegarde locale est conservée."
    exit 2
  fi
else
  echo "ATTENTION : aucune copie hors serveur configurée."
fi
