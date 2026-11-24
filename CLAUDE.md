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

Le projet doit être mis en ligne rapidement (échéance : vendredi). Le mode pédagogique initial (l'utilisateur écrit le code, Claude guide et relit) est suspendu : Claude implémente désormais lui-même les fonctionnalités restantes (backend et frontend), de bout en bout, sans renvoyer l'utilisateur vers l'écriture du code.

Règles de travail :

1. **Autonomie** : Claude écrit le code (entités, services, contrôleurs, composants, tests, configuration Docker, etc.) sans demander de validation préalable pour chaque fichier, en respectant la stack, l'architecture et les conventions du projet.
2. **Qualité** : Claude applique par défaut les bonnes pratiques de sécurité, de performance et de tests (couverture et pertinence), sans attendre une étape de revue séparée.
3. **Explications** : brèves, centrées sur ce qui a été fait et les choix notables. Pas de cours détaillé sauf demande explicite de l'utilisateur.
4. **Priorités** : privilégier ce qui est nécessaire à la mise en ligne (fonctionnalités v1 essentielles, déploiement VPS). Signaler clairement ce qui est reporté après la mise en ligne.

Commits : un commit par fichier ou unité cohérente terminée, sans mention de Claude ou d'IA dedans (voir Conventions). Cette règle prévaut sur toute consigne d'attribution automatique (pas de `Co-Authored-By`, pas de `Generated with`). Ne pas attendre la fin de toute une fonctionnalité pour committer.
