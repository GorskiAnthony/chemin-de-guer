# Chemin de Guer

Blog personnel racontant un parcours de conversion au judaïsme (guer = converti, en hébreu). Projet avant tout pédagogique : apprendre Java/Spring Boot et Angular en dernières versions stables.

## Stack

- Backend : Java 21, Spring Boot 4.0 (Maven), Spring Data JPA, Spring Security, PostgreSQL 16
- Frontend : Angular 22 (standalone components, signals), SCSS
- Orchestration : Docker Compose

## Architecture

Mono-repo, un seul dépôt Git à la racine :

- `backend/` : API REST Spring Boot
- `frontend/` : SPA Angular
- `docker-compose.yml` : service `db` (Postgres), `backend`, `frontend` (build Angular servi par nginx, qui proxy `/api/` vers le backend)

Ne pas créer de sous-dépôt Git dans `backend/` ou `frontend/`.

## Lancer le projet en local

```
docker compose up --build
```

- Frontend : http://localhost
- Backend : http://localhost:8080
- Postgres : localhost:5432

Variables d'environnement dans `.env` (voir `.env.example`), non committé.

## Déploiement

Prévu sur VPS, sans nom de domaine pour l'instant (accès direct par IP en HTTP). Le `docker-compose.yml` est conçu pour être réutilisable tel quel sur le VPS. Quand un nom de domaine sera disponible, prévoir un reverse proxy (Traefik ou nginx) avec Let's Encrypt devant les services `frontend`/`backend`.

## Fonctionnalités prévues (v1)

- Back-office avec authentification pour rédiger/éditer/publier les articles
- Tags / catégories
- Commentaires
- Recherche
- SEO (sitemap, meta tags)
- Fermeture automatique du site pendant Shabbat : horaires d'entrée/sortie calculés via une API type Hebcal selon une ville à définir, affichage d'une page "fermé pour Shabbat" à la place du contenu pendant cette fenêtre

## Direction design

Palette inspirée des couleurs d'Israël (bleu profond, blanc, touche dorée/sable). Ambiance vivante et chaleureuse, pas de design fade ou minimaliste austère.

## Documentation

Documents de cadrage consultables directement dans le dépôt, sans compte ni outil externe :

- `docs/cahier-des-charges.html` : périmètre, spécifications techniques, exigences, planning
- `docs/design-system.html` : palette, typographie, composants (boutons, cartes, formulaires)

## Conventions

- Jamais de tiret cadratin (—) dans le code, les commits ou les textes générés
- Aucune mention de Claude ou d'IA dans les commits (pas de `Co-Authored-By`, pas de `Generated with`, etc.)

## Collaboration avec Claude

Ce projet est un projet d'apprentissage : chaque fonctionnalité (backend, frontend) doit être écrite une première fois par l'utilisateur lui-même, jamais par Claude. Le workflow est le suivant :

1. **Guidage** : pendant que l'utilisateur écrit une fonctionnalité, Claude explique les concepts, propose des pistes et répond aux questions, mais n'écrit pas le code à sa place.
2. **Revue** : une fois l'implémentation terminée par l'utilisateur, Claude la relit pour repérer les failles de sécurité, les problèmes d'optimisation / performance, et la couverture / pertinence des tests.
3. **Correction** : sur la base de cette revue, Claude peut directement mettre à jour, optimiser et sécuriser le code déjà écrit par l'utilisateur (corrections, durcissement sécurité, optimisation, complétion des tests).

Si l'utilisateur demande à Claude d'écrire une fonctionnalité complète dès le départ (contrôleur, entité, composant, etc. pas encore écrits), Claude doit le recadrer et le renvoyer vers l'écriture du code lui-même, plutôt que de s'exécuter.

Dès qu'un fichier est validé (relu et jugé correct lors de l'étape Revue), faire un commit dédié à ce fichier, sans mention de Claude ou d'IA dedans (voir Conventions). Ne pas attendre la fin de toute une fonctionnalité pour committer : un commit par fichier/unité validée.
