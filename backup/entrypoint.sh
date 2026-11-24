#!/bin/sh
# Prépare l'environnement du cron, configure la copie hors serveur, planifie la sauvegarde puis lance crond.
set -eu

ENV_FILE=/etc/backup.env
: > "$ENV_FILE"
chmod 600 "$ENV_FILE"
export_var() { # nom valeur : écrit "export nom='valeur'" en protégeant les apostrophes
  printf "export %s='%s'\n" "$1" "$(printf %s "$2" | sed "s/'/'\\\\''/g")" >> "$ENV_FILE"
}

for v in DB_HOST DB_NAME DB_USER DB_PASSWORD TZ BACKUP_KEEP BACKUP_PASSPHRASE; do
  eval "val=\${$v:-}"
  export_var "$v" "$val"
done

# Copie hors serveur : S3 (OVH Object Storage, Backblaze, Scaleway...) ou SFTP
REMOTE=""
if [ -n "${BACKUP_S3_BUCKET:-}" ]; then
  export_var RCLONE_CONFIG_CDG_TYPE s3
  export_var RCLONE_CONFIG_CDG_PROVIDER Other
  export_var RCLONE_CONFIG_CDG_ACCESS_KEY_ID "${BACKUP_S3_ACCESS_KEY:-}"
  export_var RCLONE_CONFIG_CDG_SECRET_ACCESS_KEY "${BACKUP_S3_SECRET_KEY:-}"
  export_var RCLONE_CONFIG_CDG_ENDPOINT "${BACKUP_S3_ENDPOINT:-}"
  export_var RCLONE_CONFIG_CDG_REGION "${BACKUP_S3_REGION:-}"
  export_var RCLONE_CONFIG_CDG_ACL private
  REMOTE="cdg:${BACKUP_S3_BUCKET}/${BACKUP_S3_PREFIX:-chemin-de-guer}"
elif [ -n "${BACKUP_SFTP_HOST:-}" ]; then
  export_var RCLONE_CONFIG_CDG_TYPE sftp
  export_var RCLONE_CONFIG_CDG_HOST "$BACKUP_SFTP_HOST"
  export_var RCLONE_CONFIG_CDG_PORT "${BACKUP_SFTP_PORT:-22}"
  export_var RCLONE_CONFIG_CDG_USER "${BACKUP_SFTP_USER:-}"
  export_var RCLONE_CONFIG_CDG_PASS "$(rclone obscure "${BACKUP_SFTP_PASSWORD:-}")"
  REMOTE="cdg:${BACKUP_SFTP_PATH:-chemin-de-guer}"
fi
export_var BACKUP_REMOTE "$REMOTE"

# Planification (par défaut : tous les jours à 3 h 30, heure de Paris)
CRON="${BACKUP_CRON:-30 3 * * *}"
echo "$CRON /usr/local/bin/backup.sh >> /proc/1/fd/1 2>&1" > /etc/crontabs/root

echo "[$(date -Iseconds)] Service de sauvegarde démarré. Planification : $CRON ($(date +%Z))."
if [ -n "$REMOTE" ]; then
  echo "Copie hors serveur : activée."
else
  echo "ATTENTION : aucune copie hors serveur configurée, les sauvegardes restent sur ce serveur (voir docs/deploiement.md)."
fi
[ -n "${BACKUP_PASSPHRASE:-}" ] && echo "Chiffrement : activé (AES-256)." || echo "Chiffrement : désactivé."

# Une première sauvegarde au démarrage vérifie immédiatement que tout fonctionne
if [ "${BACKUP_ON_START:-true}" = "true" ]; then
  /usr/local/bin/backup.sh || echo "ERREUR : la sauvegarde de démarrage a échoué (voir ci-dessus)."
fi

exec crond -f -l 8
