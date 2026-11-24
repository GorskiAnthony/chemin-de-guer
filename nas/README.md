# Copier les sauvegardes sur un NAS

Le NAS (testé pour un **UGREEN**, mais la méthode est la même sur n'importe quel NAS qui fait tourner Docker) **se connecte au VPS et copie les sauvegardes**. Rien n'est ouvert chez vous : pas de port, pas d'adresse IP fixe, pas de DynDNS. Le NAS peut être éteint un moment : il rattrape tout à son retour.

```
VPS (sauvegarde chiffrée chaque nuit, 3 h 30)  <──SSH, lecture seule──  NAS (copie toutes les 6 h)
```

La clé SSH donnée au NAS ne permet **que de lire** le dossier des sauvegardes : pas de shell, pas d'écriture, pas de suppression, pas d'accès au reste du VPS (vérifié par des tests).

## Avant de commencer

- **Docker sur le NAS.** Sur les NAS UGREEN, c'est l'application Docker d'UGOS Pro, avec la fonction « Project » qui accepte un fichier compose. Cette fonction n'existe pas sur tous les modèles (certains modèles d'entrée de gamme n'ont pas de support Docker officiel) : vérifiez la fiche de votre modèle.
- Activer `BACKUP_PASSPHRASE` dans Dokploy (voir `docs/deploiement.md`, section Sauvegardes). Les sauvegardes sont alors chiffrées : même la copie sur le NAS est illisible sans la phrase, que vous conservez **ailleurs que sur le VPS ni sur le NAS**.

## 1. Sur le VPS (en SSH, une seule fois)

**a. Un dossier fixe pour les sauvegardes.** Dans Dokploy, onglet Environment, ajouter :

```
BACKUP_DIR=/var/backups/chemin-de-guer
```

Puis, en SSH sur le VPS, créer le dossier, et redéployer dans Dokploy :

```bash
sudo mkdir -p /var/backups/chemin-de-guer
```

Après le déploiement, `ls -l /var/backups/chemin-de-guer` doit montrer une première sauvegarde. Les sauvegardes déjà faites restent dans l'ancien volume Docker : elles ne sont pas déplacées.

**b. Un utilisateur dédié, sans mot de passe** (il ne sert qu'à cette copie) :

```bash
sudo adduser --disabled-password --gecos "" cdgbackup
sudo mkdir -p /home/cdgbackup/.ssh
```

La clé du NAS sera ajoutée à l'étape 2.

## 2. Sur le NAS

**a. Copier le dossier `nas/`** de ce dépôt sur le NAS (par exemple dans un dossier partagé `docker/chemin-de-guer-pull`). Il contient `docker-compose.yml`, `pull.sh` et `.env.example`.

**b. Créer le fichier `.env`** à côté du `docker-compose.yml`, à partir de `.env.example` :

```
VPS_HOST=chemin-de-guer.fr
DEST_DIR=/volume1/backups/chemin-de-guer     # un dossier de votre NAS (adaptez le chemin à votre volume)
```

**c. Générer la clé du NAS.** Dans un terminal sur le NAS (SSH du NAS, ou terminal intégré à l'application Docker), depuis ce dossier :

```bash
docker compose run --rm cdg-pull keygen
```

La commande affiche deux choses : la **clé publique** du NAS et l'**empreinte** du VPS.

**d. Autoriser cette clé sur le VPS.** Sur le VPS, en remplaçant `ssh-ed25519 AAAA… nas-cdg-backup` par la ligne affichée :

```bash
echo 'restrict,command="/usr/bin/rrsync -ro /var/backups/chemin-de-guer" ssh-ed25519 AAAA… nas-cdg-backup' | sudo tee /home/cdgbackup/.ssh/authorized_keys
sudo chmod 700 /home/cdgbackup/.ssh && sudo chmod 600 /home/cdgbackup/.ssh/authorized_keys
sudo chown -R cdgbackup:cdgbackup /home/cdgbackup/.ssh
```

`rrsync -ro` est ce qui limite la clé à la **lecture seule de ce seul dossier**. Si `/usr/bin/rrsync` n'existe pas, installez `rsync` (`sudo apt install rsync`).

**e. Vérifier l'identité du VPS.** Sur le VPS :

```bash
ssh-keygen -lf /etc/ssh/ssh_host_ed25519_key.pub
```

Comparez avec l'empreinte affichée à l'étape c. **Si elles sont identiques**, sur le NAS :

```bash
docker compose run --rm cdg-pull trust
```

Sinon, ne continuez pas : quelqu'un se fait passer pour votre serveur.

**f. Premier essai, puis démarrage.**

```bash
docker compose run --rm cdg-pull once      # une récupération immédiate
docker compose up -d                       # puis en continu, toutes les 6 heures
```

Dans l'application Docker du NAS, vous pouvez aussi créer un **Project** à partir de ce `docker-compose.yml` plutôt que de lancer `up -d` à la main.

Contrôle : le dossier `DEST_DIR` contient les fichiers `chemin_de_guer_….sql.gz.enc` et un fichier caché `.last_pull_ok` daté de la dernière récupération réussie. Les journaux du conteneur `cdg-backup-pull` indiquent `OK : N nouvelle(s) sauvegarde(s)` et `Contrôle : OK`.

## Ce que le NAS vérifie à chaque passage

- les nouvelles sauvegardes sont copiées (rien n'est jamais recopié ni écrasé) ;
- les trois plus récentes sont contrôlées : une archive illisible ou un fichier chiffré sans en-tête déclenche un `ATTENTION` dans le journal ;
- l'identité du VPS est vérifiée à chaque connexion : si l'empreinte change, la connexion est refusée.

`KEEP_COUNT` (dans `.env`) limite le nombre de sauvegardes gardées sur le NAS. Laissez `0` (tout garder), ou une valeur **d'au moins 30** : en dessous, les plus anciennes seraient recopiées depuis le VPS puis supprimées à chaque passage.

## Restaurer depuis le NAS

Avec le dossier fixe, c'est simple. Copier le fichier voulu du NAS vers le VPS :

```bash
scp chemin_de_guer_AAAAMMJJ_HHMMSS.sql.gz.enc VOTRE_UTILISATEUR@chemin-de-guer.fr:/tmp/
sudo mv /tmp/chemin_de_guer_AAAAMMJJ_HHMMSS.sql.gz.enc /var/backups/chemin-de-guer/
```

puis suivre la procédure **Restaurer** de `docs/deploiement.md` (arrêter le backend, `restore.sh --yes /backups/<fichier>`, redémarrer). Pour un fichier chiffré, `BACKUP_PASSPHRASE` doit être défini sur le service `cdg-backup`.

## En cas de problème

| Message dans le journal | Cause probable |
| --- | --- |
| `Configuration incomplète : lancez d'abord keygen puis trust` | les étapes c et e n'ont pas été faites (le dossier `ssh/` est vide) |
| `Host key verification failed` / `REMOTE HOST IDENTIFICATION HAS CHANGED` | le VPS a changé de clé (réinstallation ?) : vérifiez l'empreinte comme à l'étape e, puis supprimez `ssh/known_hosts` et refaites `keygen` et `trust` |
| `Permission denied (publickey)` | la ligne `authorized_keys` est incorrecte, ou les droits de `/home/cdgbackup/.ssh` sont trop larges |
| `ERREUR : récupération impossible` | VPS injoignable, ou clé non autorisée ; le NAS réessaiera au prochain passage |
| `rrsync: command not found` côté VPS | installer `rsync` sur le VPS |

## Pour arrêter ou désactiver

Côté NAS : `docker compose down`. Côté VPS : supprimer le fichier `/home/cdgbackup/.ssh/authorized_keys` coupe immédiatement tout accès du NAS.
