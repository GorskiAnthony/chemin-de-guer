# Mise en ligne sur un VPS

Déploiement par Docker Compose, accès direct par IP en HTTP (pas de nom de domaine pour l'instant). Seul le service `frontend` (nginx) est exposé : la base et le backend restent sur le réseau interne de Docker.

## Prérequis

- Un VPS Linux (1 Go de RAM minimum, 2 Go recommandés pour la compilation) avec Docker et le plugin Compose
- Le port 80 ouvert vers l'extérieur

```bash
# Pare-feu minimal (Ubuntu/Debian)
sudo ufw allow OpenSSH && sudo ufw allow 80/tcp && sudo ufw enable
```

## Première installation

```bash
git clone https://github.com/GorskiAnthony/chemin-de-guer.git
cd chemin-de-guer
cp .env.example .env
nano .env
```

Dans `.env`, renseigner au minimum :

| Variable | Valeur |
| --- | --- |
| `DB_PASSWORD` | mot de passe long et unique (`openssl rand -base64 24`) |
| `ADMIN_EMAIL` | email de connexion au back-office |
| `ADMIN_PASSWORD` | 12 caractères minimum, unique |
| `APP_PUBLIC_URL` | `http://IP_DU_VPS` (utilisée par le sitemap et le flux RSS) |
| `COOKIE_SECURE` | `false` tant que le site est en HTTP, `true` dès que HTTPS est en place |

Puis lancer :

```bash
docker compose up -d --build
docker compose ps        # les 3 services doivent être "Up", db et backend "healthy"
```

Le site est accessible sur `http://IP_DU_VPS`, le back-office sur `/admin/login`.

Le compte administrateur est créé au premier démarrage, uniquement si la base ne contient aucun utilisateur. Une fois connecté, `ADMIN_PASSWORD` peut être retiré du `.env`.

## Mettre à jour

```bash
git pull
docker compose up -d --build
```

Les données sont conservées dans le volume `postgres_data`. Ne jamais utiliser `docker compose down -v` en production : l'option `-v` supprime la base.

## Sauvegardes

```bash
./scripts/backup.sh      # crée backups/chemin_de_guer_AAAAMMJJ_HHMMSS.sql.gz, garde les 14 dernières
```

Exemple de sauvegarde quotidienne à 3 h (`crontab -e`) :

```
0 3 * * * cd /chemin/vers/chemin-de-guer && ./scripts/backup.sh >> backups/backup.log 2>&1
```

Copier régulièrement le dossier `backups/` hors du serveur. Restauration :

```bash
gunzip -c backups/FICHIER.sql.gz | docker compose exec -T db psql -U postgres chemin_de_guer
```

## Dépannage

```bash
docker compose logs -f backend    # erreurs applicatives
docker compose logs -f frontend   # nginx
docker compose restart backend
```

- **502 Bad Gateway** : le backend n'est pas encore démarré ou a planté, voir `docker compose logs backend`.
- **Impossible de se connecter** : vérifier que `ADMIN_EMAIL` et `ADMIN_PASSWORD` étaient définis au premier démarrage. Si la base contient déjà un utilisateur, ces variables sont ignorées.
- **Site « fermé pour Shabbat » hors Shabbat** : vérifier `SHABBAT_TIMEZONE` et l'heure du serveur, ou désactiver avec `SHABBAT_ENABLED=false`.
- **Calendrier absent de l'accueil** : le backend n'arrive pas à joindre hebcal.com (accès sortant bloqué).

## Quand un nom de domaine sera disponible

Placer un reverse proxy (Traefik ou nginx) avec Let's Encrypt devant le service `frontend`, puis passer `COOKIE_SECURE=true` et `APP_PUBLIC_URL=https://votre-domaine`. Voir l'issue #18.
