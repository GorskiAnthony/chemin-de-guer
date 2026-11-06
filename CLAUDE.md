# Chemin de Guer

Blog personnel racontant un parcours de conversion au judaïsme (guer = converti, en hébreu). Projet avant tout pédagogique : apprendre Java/Spring Boot et Angular en dernières versions stables.

## Stack

- Backend : Java 21, Spring Boot 4.0 (Maven), Spring Data JPA, Spring Security, PostgreSQL 16
- Frontend : Angular 22 (standalone components, signals), SCSS
- Orchestration : Docker Compose

## Architecture

Mono-repo, un seul dépôt Git à la racine :

- `backend/` — API REST Spring Boot
- `frontend/` — SPA Angular
- `docker-compose.yml` — service `db` (Postgres), `backend`, `frontend` (build Angular servi par nginx, qui proxy `/api/` vers le backend)

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

## Conventions

- Jamais de tiret cadratin (—) dans le code, les commits ou les textes générés
- Aucune mention de Claude ou d'IA dans les commits (pas de `Co-Authored-By`, pas de `Generated with`, etc.)

## Collaboration avec Claude

Ce projet est un projet d'apprentissage : tout le code applicatif (backend, frontend) doit être écrit par l'utilisateur lui-même, jamais par Claude. Le rôle de Claude ici se limite à :

- repérer les failles de sécurité
- repérer les problèmes d'optimisation / performance
- revoir la couverture et la pertinence des tests

Si l'utilisateur demande à Claude d'écrire ou de créer du code (contrôleur, entité, composant, etc.), Claude doit le recadrer et le renvoyer vers l'écriture du code lui-même, plutôt que de s'exécuter.
