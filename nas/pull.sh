#!/bin/sh
# Récupère les sauvegardes de Chemin de Guer depuis le VPS, depuis le NAS.
# C'est le NAS qui se connecte au VPS (rien à ouvrir chez vous), en SSH, avec une clé en lecture seule.
#
# Usage (dans le conteneur) : pull.sh [loop | once | keygen | trust]
set -eu

apk add --no-cache rsync openssh-client tzdata >/dev/null

VPS_HOST="${VPS_HOST:?VPS_HOST manquant}"
VPS_USER="${VPS_USER:-cdgbackup}"
VPS_PORT="${VPS_PORT:-22}"
EVERY="${PULL_EVERY_HOURS:-6}"
KEEP="${KEEP_COUNT:-0}"   # 0 = tout conserver
KEY=/ssh/id_ed25519
KNOWN=/ssh/known_hosts
DEST=/backups

SSH="ssh -i $KEY -p $VPS_PORT -o UserKnownHostsFile=$KNOWN -o StrictHostKeyChecking=yes -o BatchMode=yes -o ConnectTimeout=20"

keygen() {
  mkdir -p /ssh && chmod 700 /ssh
  [ -f "$KEY" ] || ssh-keygen -t ed25519 -N "" -C "nas-cdg-backup" -f "$KEY" >/dev/null
  echo "=== 1. Clé publique à ajouter sur le VPS (fichier authorized_keys de l'utilisateur $VPS_USER) ==="
  cat "$KEY.pub"
  echo
  echo "=== 2. Empreinte du serveur vue depuis le NAS ==="
  ssh-keyscan -p "$VPS_PORT" -t ed25519 "$VPS_HOST" 2>/dev/null > /ssh/known_hosts.candidate
  ssh-keygen -lf /ssh/known_hosts.candidate
  echo
  echo "Comparez cette empreinte à celle du VPS (commande sur le VPS : ssh-keygen -lf /etc/ssh/ssh_host_ed25519_key.pub)."
  echo "Si elles sont identiques, lancez : trust. Sinon, ne continuez pas."
}

trust() {
  [ -f /ssh/known_hosts.candidate ] || { echo "Lancez d'abord : keygen"; exit 1; }
  cp /ssh/known_hosts.candidate "$KNOWN"
  echo "Serveur approuvé. Les connexions suivantes vérifient son identité."
}

# Contrôle des sauvegardes reçues : archive lisible (.sql.gz) ou en-tête de chiffrement présent (.enc)
verify() {
  bad=0
  for f in $(ls -1t "$DEST"/chemin_de_guer_*.sql.gz* 2>/dev/null | head -3); do
    case "$f" in
      *.enc) [ "$(head -c 8 "$f")" = "Salted__" ] && [ "$(wc -c < "$f")" -gt 100 ] || { echo "ATTENTION : $f semble invalide"; bad=1; } ;;
      *.gz)  gzip -t "$f" 2>/dev/null || { echo "ATTENTION : $f est illisible"; bad=1; } ;;
    esac
  done
  return $bad
}

prune() {
  [ "$KEEP" -gt 0 ] || return 0
  ls -1t "$DEST"/chemin_de_guer_*.sql.gz* 2>/dev/null | tail -n +"$((KEEP + 1))" | xargs -r rm -f --
}

pull() {
  mkdir -p "$DEST"
  echo "[$(date -Iseconds)] Récupération des sauvegardes depuis $VPS_HOST..."
  before=$(ls -1 "$DEST" 2>/dev/null | wc -l)
  if rsync -a --ignore-existing --partial -e "$SSH" "$VPS_USER@$VPS_HOST:./" "$DEST/" --exclude 'last_*' --exclude '*.tmp'; then
    after=$(ls -1 "$DEST" 2>/dev/null | wc -l)
    echo "OK : $((after - before)) nouvelle(s) sauvegarde(s), $after au total."
    if verify; then date -Iseconds > "$DEST/.last_pull_ok"; echo "Contrôle : OK."; fi
    prune
  else
    echo "ERREUR : récupération impossible (VPS éteint, réseau, clé ou empreinte non approuvée ?)."
    return 1
  fi
}

case "${1:-loop}" in
  keygen) keygen ;;
  trust)  trust ;;
  once)   pull ;;
  loop)
    [ -f "$KEY" ] && [ -f "$KNOWN" ] || { echo "Configuration incomplète : lancez d'abord keygen puis trust (voir nas/README.md)."; sleep 30; exit 1; }
    echo "Service de récupération démarré : toutes les ${EVERY} h."
    while true; do
      pull || true
      sleep "$((EVERY * 3600))"
    done ;;
  *) echo "Usage : pull.sh [loop|once|keygen|trust]"; exit 1 ;;
esac
