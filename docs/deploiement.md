# Mise en ligne sur un VPS

Le projet se déploie par Docker Compose. Seul le service `cdg-frontend` (nginx) reçoit le trafic : la base et le backend restent sur le réseau interne de Docker.

- **Option A : avec Dokploy** (recommandé si Dokploy est déjà installé sur le VPS)
- **Option B : sans Dokploy**, accès direct par IP en HTTP

## Option A : Dokploy

Le projet est un déploiement **Compose** : le fichier `docker-compose.yml` contient déjà PostgreSQL, il ne faut donc **pas** créer de base de données séparée dans Dokploy. Ne pas déployer le backend et le frontend comme deux « Applications » distinctes : nginx joint le backend par le nom `cdg-backend`, qui n'existerait pas.

### 1. DNS

Chez l'hébergeur du domaine, un enregistrement `A` vers l'IP du VPS (et un second pour `www`, ou un `CNAME` vers le domaine). La propagation peut prendre de quelques minutes à quelques heures.

### 2. Créer le service dans Dokploy

1. Dans le projet, **Create Service**, puis **Compose**.
2. **Provider** : GitHub (ou Git avec `https://github.com/GorskiAnthony/chemin-de-guer.git`), branche `main`.
3. **Compose Path** : `./docker-compose.yml`. Type : *Docker Compose*.

Si le projet existe déjà sous forme d'« Applications » séparées, les supprimer et repartir de ce service Compose.

### 3. Variables d'environnement

Onglet **Environment** du service :

```
DB_PASSWORD=<mot de passe long et unique>
ADMIN_EMAIL=<votre email>
ADMIN_PASSWORD=<12 caractères minimum>
APP_PUBLIC_URL=https://votre-domaine.fr
COOKIE_SECURE=true
```

`DB_NAME`, `DB_USER` et les réglages Shabbat ont des valeurs par défaut. Ne pas définir `FRONTEND_PORT` : Dokploy n'utilise pas `docker-compose.override.yml`, aucun port n'est publié sur la machine et c'est Traefik qui route vers le conteneur.

### 4. Domaine et HTTPS

Onglet **Domains**, **Add Domain** :

| Champ | Valeur |
| --- | --- |
| Host | `votre-domaine.fr` |
| Service Name | `cdg-frontend` |
| Port | `80` |
| HTTPS | activé |
| Certificate | Let's Encrypt |

Répéter pour `www.votre-domaine.fr` si besoin.

### 5. Déployer

Bouton **Deploy**, puis suivre les logs. Les services démarrent dans l'ordre : base, backend (healthcheck), frontend. Le compte administrateur est créé au premier démarrage (voir plus bas).

### Pourquoi les services s'appellent `cdg-*`

Sur un VPS qui héberge plusieurs projets, Dokploy place les services sur un réseau Docker partagé. Des noms génériques comme `db` ou `backend` peuvent alors désigner le conteneur d'un autre projet : l'API répond avec les erreurs d'une autre application, ou le backend se connecte à la mauvaise base. Les noms préfixés évitent ce conflit. Dans les réglages avancés du service Compose, activer aussi **Isolated Deployment** ajoute une protection supplémentaire.

### Mises à jour et sauvegardes

- Un nouveau déploiement (bouton **Deploy**, ou *Auto Deploy* par webhook GitHub) reconstruit les images. Les données restent dans le volume `postgres_data`.
- Ne jamais supprimer ce volume `postgres_data`. Les sauvegardes sont automatiques (service `cdg-backup`) : voir la section **Sauvegardes** plus bas, et surtout **configurer la copie hors serveur**.

### Problèmes fréquents avec Dokploy

- **404 ou « Bad Gateway » de Traefik** : vérifier que le domaine pointe sur le service `cdg-frontend` et le port `80`, et que le DNS est propagé.
- **Pas de certificat HTTPS** : le DNS doit déjà pointer vers le VPS, et les ports 80 et 443 doivent être ouverts chez OVH.
- **Connexion impossible au back-office** : `COOKIE_SECURE=true` exige HTTPS. Vérifier que le site est bien servi en `https://`.
- **Plus de connexion possible après 5 erreurs** : normal, limitation de 15 minutes par IP.

## Sauvegardes

Le service `cdg-backup` (inclus dans `docker-compose.yml`, donc déployé aussi par Dokploy) sauvegarde la base **tous les jours à 3 h 30** (heure de Paris) :

- dump compressé, **vérifié** (archive lisible et dump complet, sinon la sauvegarde est supprimée et une erreur est journalisée) ;
- les **30 plus récentes** sont conservées (`BACKUP_KEEP`), dans le volume `backups` du serveur ;
- une première sauvegarde est faite à chaque démarrage du service, pour vérifier immédiatement la configuration ;
- le conteneur passe en **unhealthy** (visible dans `docker ps` et Dokploy) si aucune sauvegarde n'a réussi depuis plus de 2 jours.

Une sauvegarde qui reste sur le même serveur ne protège pas d'une perte du serveur. **Configurez une copie hors serveur**, au choix :

**S3 compatible** (OVH Object Storage, Scaleway, Backblaze B2...). Créer un conteneur (bucket) privé et un utilisateur avec clés d'accès S3, puis dans Dokploy, onglet Environment :

```
BACKUP_S3_ENDPOINT=https://s3.gra.io.cloud.ovh.net   # exemple OVH Object Storage, région Gravelines
BACKUP_S3_REGION=gra
BACKUP_S3_BUCKET=nom-du-bucket
BACKUP_S3_ACCESS_KEY=<clé d'accès>
BACKUP_S3_SECRET_KEY=<clé secrète>
```

**SFTP** (un autre serveur) : `BACKUP_SFTP_HOST`, `BACKUP_SFTP_USER`, `BACKUP_SFTP_PASSWORD`, et `BACKUP_SFTP_PATH` (dossier distant).

**Sur un NAS** : le NAS peut aller chercher lui-même les sauvegardes sur le VPS, sans rien ouvrir chez vous. Guide complet (testé) dans [`nas/README.md`](../nas/README.md).

**Chiffrement (conseillé pour la copie distante)** : `BACKUP_PASSPHRASE=<phrase longue>` chiffre les sauvegardes en AES-256 (`.sql.gz.enc`). **Conservez cette phrase en lieu sûr, hors du serveur** : sans elle, les sauvegardes chiffrées sont inutilisables.

Après avoir ajouté les variables, redéployer puis vérifier dans les logs du service `cdg-backup` : `Copie hors serveur : activée` puis `OK : copie hors serveur effectuée`. Côté distant, seules les 30 plus récentes sont gardées (la suppression se fait par nombre et non par âge : si les sauvegardes s'arrêtent, rien n'est effacé).

### Restaurer

Le backend doit être arrêté pendant la restauration. Sur le VPS (en SSH) :

```bash
docker ps --format '{{.Names}}' | grep -E 'cdg-(backup|backend)'      # repérer les noms des conteneurs
docker exec <conteneur-backup> ls -1t /backups                         # sauvegardes disponibles
docker stop <conteneur-backend>
docker exec -i <conteneur-backup> restore.sh --yes /backups/chemin_de_guer_AAAAMMJJ_HHMMSS.sql.gz
docker start <conteneur-backend>
```

La restauration **remplace** le contenu actuel de la base. Pour une sauvegarde chiffrée, le fichier se termine par `.enc` et `BACKUP_PASSPHRASE` doit être défini sur le service. Pour récupérer une sauvegarde depuis le stockage distant (par exemple après la perte du serveur) : redéployer le projet sur le nouveau serveur avec les mêmes variables, puis `docker exec <conteneur-backup> sh -c '. /etc/backup.env; rclone copy "$BACKUP_REMOTE/FICHIER" /backups/'` et lancer `restore.sh`.

Cette procédure a été testée : sauvegarde chiffrée, copie S3, suppression volontaire d'un article, restauration, article retrouvé.

## Résumés de paracha et de fêtes

Dans `/admin`, **Nouvelle paracha** et **Nouvelle fête** servent à écrire ce que vous apprenez. Une paracha est découpée en montées, une fête en parties (histoire et origine, date et durée, coutumes et rituels, prières et lectures, vécu : le bouton *Ajouter les parties types* les prépare, vous pouvez en ajouter, en retirer ou les renommer).

- Le **nom en hébreu se remplit tout seul** dès que le nom correspond au catalogue (les 54 paracha, les 7 paracha doubles et les fêtes), avec l'orthographe de Hebcal : pas besoin de clavier hébreu. Il suit si vous changez de nom, et un nom saisi à la main n'est jamais écrasé. Les résumés créés sans nom hébreu sont complétés au démarrage du backend. Le nom enregistré est celui du catalogue, même si vous l'avez tapé en minuscules ou avec une apostrophe simple.
- Pour une fête, choisissez le nom dans la liste de suggestions : la fête est alors classée automatiquement (Fêtes de Tichri, Hiver et printemps, Omer et Chavouot, Été, Jeûnes, Chaque mois), son nom en hébreu est proposé, et elle est **reliée au calendrier de l'accueil**. Un nom hors liste (Yom HaShoah...) est accepté et classé dans « Autres ».
- Une fête publiée affiche sa **prochaine date**, calculée automatiquement à partir du calendrier hébraïque, avec le nombre de jours restants.
- Sur l'accueil, la fête annoncée devient un lien vers votre résumé dès que celui-ci est publié. Le même principe s'applique à la paracha de la semaine.
- Un résumé en brouillon n'est visible nulle part côté public.

## Indexation par les moteurs de recherche

Par défaut (`SITE_INDEXABLE=false`) le site demande à **ne pas être indexé** :

- chaque page porte `<meta name="robots" content="noindex, nofollow, noarchive">` et l'en-tête `X-Robots-Tag` équivalent (pages, images de partage, flux, API, administration) ;
- `/robots.txt` refuse les robots d'IA ; il n'interdit pas l'exploration aux autres robots, volontairement : un robot doit pouvoir lire la directive `noindex` (sinon l'adresse peut quand même apparaître dans les résultats), et les aperçus de partage doivent continuer de fonctionner ;
- `/sitemap.xml`, `/llms.txt` et `/llms-full.txt` répondent 404 ; le flux RSS reste disponible.

Les **aperçus de partage** (WhatsApp, Facebook, LinkedIn, X) ne sont pas affectés : titre, description et image restent présents. Le `noindex` n'est pas une protection d'accès : toute personne qui connaît l'adresse peut lire le site, et les moteurs déjà passés avant cette directive peuvent mettre plusieurs semaines à retirer une page de leurs résultats.

Pour rouvrir le site aux moteurs : `SITE_INDEXABLE=true` dans Dokploy puis redéployer (balises `index`, sitemap, `llms.txt` et `robots.txt` ouvert reviennent).

## Référencement, partage et moteurs de réponse (SEO / GEO)

Le site est une application Angular : les robots des réseaux sociaux et de la plupart des IA n'exécutent pas JavaScript. Pour qu'ils voient le vrai contenu, nginx envoie les pages publiques (`/`, `/articles`, `/articles/<slug>`, `/paracha`, `/paracha/<slug>`, `/fetes`, `/fetes/<slug>`) au backend, qui renvoie `index.html` enrichi :

- balises `title`, `description`, `canonical`, `hreflang`, Open Graph et Twitter Card, propres à chaque page ;
- données structurées JSON-LD (`WebSite`, `Person`, `Blog`, `BlogPosting`/`Article`, `BreadcrumbList`) ;
- le texte complet de la page (masqué visuellement, remplacé au démarrage de l'application), donc lisible sans JavaScript ;
- un vrai code **404** pour les adresses inconnues et une redirection **301** de `/articles/<id>` vers `/articles/<slug>`.

Chaque article, chaque paracha et chaque fête a **sa propre image de partage** (1200 x 630, le titre sur le fond du site), générée à la demande sur `/og/articles/<slug>.png`, `/og/paracha/<slug>.png` et `/og/fetes/<slug>.png` ; les pages d'ensemble utilisent `/og-default.png`.

Autres fichiers : `/sitemap.xml` (pages, articles, paracha avec date de modification), `/rss.xml` (texte intégral), `/robots.txt`, et pour les moteurs de réponse `/llms.txt` (résumé du site) et `/llms-full.txt` (texte intégral). `/admin` et `/api` ne sont jamais indexés (`X-Robots-Tag: noindex`).

**Écrire une description** : dans l'éditeur d'article, le champ *Description* devient la balise `description` et l'aperçu des réseaux (160 caractères conseillés). Si elle est vide, le premier vrai paragraphe est utilisé.

Ces éléments (sitemap, `llms.txt`, balises `index`) ne sont actifs que si `SITE_INDEXABLE=true` (voir ci-dessus). Variables : `APP_PUBLIC_URL` (adresse utilisée dans toutes les balises, obligatoirement en `https://` en production), `AUTHOR_NAME` et `AUTHOR_URL` (auteur des données structurées).

### Vérifier après un déploiement

1. Ouvrir `https://votre-domaine/articles/<slug>` puis « Afficher le code source » : les balises et le texte doivent y figurer.
2. Aperçu de partage : [Facebook Sharing Debugger](https://developers.facebook.com/tools/debug/) (aussi valable pour WhatsApp), [LinkedIn Post Inspector](https://www.linkedin.com/post-inspector/), validateur de cartes de X. Ces outils permettent de forcer le rafraîchissement du cache d'un lien déjà partagé.
3. Données structurées : [Rich Results Test](https://search.google.com/test/rich-results) de Google.
4. Google Search Console : ajouter le site, soumettre `https://votre-domaine/sitemap.xml`, puis demander l'indexation des premières pages.

Le backend lit `index.html` sur `http://cdg-frontend/index.html` (variable `SEO_SHELL_URL` pour la changer). Si le backend est indisponible, nginx sert la page statique de l'application.

## Modération des commentaires

Aucun commentaire n'est public avant votre approbation. Dans `/admin` :

- le compteur **« N en attente »** et l'onglet **À modérer** (affiché par défaut, les plus récents d'abord) ;
- **Approuver** ou **Supprimer** chaque commentaire ;
- **Fermer tous les commentaires** (interrupteur général, immédiat, sans redéploiement) ;
- sur chaque article, la case **« Autoriser les commentaires »** dans l'éditeur.

Les commentaires déjà approuvés restent visibles quand les commentaires sont fermés.

Protections automatiques : un visiteur ne peut pas envoyer plus de **3 commentaires par 10 minutes**, la file de modération est plafonnée à 300 commentaires en attente, et un champ piège écarte les robots simples. Le site ne conserve ni adresse IP ni email des commentateurs.

### Être prévenu par email (facultatif)

Sans serveur SMTP configuré, rien n'est envoyé et le site fonctionne normalement. Pour recevoir un email à chaque nouveau commentaire, ajouter dans l'onglet Environment de Dokploy :

```
MAIL_HOST=smtp.votre-fournisseur.fr
MAIL_PORT=587
MAIL_USERNAME=<identifiant SMTP>
MAIL_PASSWORD=<mot de passe SMTP ou mot de passe d'application>
MAIL_FROM=<adresse expéditrice, souvent identique à l'identifiant>
MAIL_TO=<adresse qui reçoit les alertes, par défaut ADMIN_EMAIL>
```

Puis redéployer. Port 587 avec STARTTLS (valeur par défaut). Pour le port 465 (SSL direct), ajouter `MAIL_STARTTLS=false` et utiliser un fournisseur compatible avec STARTTLS sur 587 de préférence. Un échec d'envoi (mauvais mot de passe, serveur injoignable) est noté dans les logs du backend (`Notification email impossible`) mais n'empêche jamais l'enregistrement du commentaire.

Le message contient l'article, le nom, un extrait du commentaire et le lien vers `/admin`.

## Option B : VPS sans Dokploy

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
docker compose ps        # les 3 services doivent être "Up", cdg-db et cdg-backend "healthy"
```

Le site est accessible sur `http://IP_DU_VPS`, le back-office sur `/admin/login`. Dans ce mode, `docker-compose.override.yml` est chargé automatiquement et publie le port défini par `FRONTEND_PORT` (80 par défaut).

Le compte administrateur est créé au premier démarrage, uniquement si la base ne contient aucun utilisateur. Une fois connecté, `ADMIN_PASSWORD` peut être retiré du `.env`.

## Mettre à jour

```bash
git pull
docker compose up -d --build
```

Les données sont conservées dans le volume `postgres_data`. Ne jamais utiliser `docker compose down -v` en production : l'option `-v` supprime la base.

## Dépannage

```bash
docker compose logs -f cdg-backend    # erreurs applicatives
docker compose logs -f cdg-frontend   # nginx
docker compose restart cdg-backend
```

- **502 Bad Gateway** : le backend n'est pas encore démarré ou a planté, voir `docker compose logs cdg-backend`.
- **Impossible de se connecter** : vérifier que `ADMIN_EMAIL` et `ADMIN_PASSWORD` étaient définis au premier démarrage. Si la base contient déjà un utilisateur, ces variables sont ignorées.
- **Site « fermé pour Shabbat » hors Shabbat** : vérifier `SHABBAT_TIMEZONE` et l'heure du serveur, ou désactiver avec `SHABBAT_ENABLED=false`.
- **Calendrier absent de l'accueil** : le backend n'arrive pas à joindre hebcal.com (accès sortant bloqué).

## Quand un nom de domaine sera disponible

Placer un reverse proxy (Traefik ou nginx) avec Let's Encrypt devant le service `cdg-frontend`, puis passer `COOKIE_SECURE=true` et `APP_PUBLIC_URL=https://votre-domaine`. Voir l'issue #18.

## Statistiques de fréquentation

Le site mesure sa fréquentation lui-même, sans service tiers : page `/admin/statistiques` (bouton « Statistiques » de l'administration).

- Chaque page vue est enregistrée dans la table `page_view` : jour, adresse de la page, site d'origine (nom de domaine seulement), type d'appareil et une empreinte de visiteur.
- Aucune adresse IP n'est conservée et aucun cookie n'est posé. L'empreinte est calculée avec un sel tiré au sort chaque jour et oublié aussitôt : un visiteur ne peut pas être suivi d'un jour à l'autre. « Visites » est donc la somme des visiteurs distincts de chaque jour.
- Ne sont pas comptés : l'administrateur connecté, les robots, les navigateurs qui envoient `Do Not Track` ou `Sec-GPC`, et toute page hors du site public.
- Conservation : 400 jours, purge automatique chaque nuit à 4 h 15.
- Les données font partie de la base : elles sont incluses dans les sauvegardes.
