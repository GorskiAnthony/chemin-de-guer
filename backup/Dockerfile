FROM postgres:16-alpine
# rclone : copie hors serveur (S3, SFTP...) ; openssl : chiffrement facultatif ; tzdata : heure de Paris pour le cron
RUN apk add --no-cache rclone openssl tzdata
COPY entrypoint.sh backup.sh restore.sh /usr/local/bin/
RUN chmod +x /usr/local/bin/entrypoint.sh /usr/local/bin/backup.sh /usr/local/bin/restore.sh && mkdir -p /backups
ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]
