# Chemin de Guer

Blog personnel retraçant un parcours de conversion au judaïsme (guer, גר : converti, en hébreu). Le projet sert aussi de terrain d'apprentissage pour Java/Spring Boot et Angular dans leurs dernières versions stables.

Documentation complète :

- [Cahier des charges](docs/cahier-des-charges.html) : périmètre, spécifications techniques, exigences, planning
- [Système de design](docs/design-system.html) : palette, typographie, composants
- [Mise en ligne](docs/deploiement.md) : déploiement sur VPS, sauvegardes, dépannage

## Stack

- Backend : Java 21, Spring Boot 4.0 (Maven), Spring Data JPA, Spring Security, PostgreSQL 16
- Frontend : Angular 22 (standalone components, signals), SCSS
- Orchestration : Docker, Docker Compose

## Démarrer en local

Prérequis : Docker et Docker Compose.

```bash
cp .env.example .env
docker compose up --build
```

- Site : http://localhost (ou le port défini par `FRONTEND_PORT` dans `.env`)
- Back-office : `/admin/login`, avec le compte défini par `ADMIN_EMAIL` et `ADMIN_PASSWORD` dans `.env` (créé au premier démarrage)

Le backend et Postgres ne sont pas publiés sur la machine : tout passe par nginx, qui proxifie `/api/` vers le backend.

## Mise en ligne

Voir le guide [docs/deploiement.md](docs/deploiement.md) (VPS, variables d'environnement, sauvegardes, mises à jour).

## Contribuer

Les contributions sont bienvenues. Avant d'ouvrir une pull request, jette un œil à [CONTRIBUTING.md](CONTRIBUTING.md) et à la [charte de bonne conduite](CODE_OF_CONDUCT.md).

Les issues marquées [`good first issue`](https://github.com/GorskiAnthony/chemin-de-guer/issues?q=is%3Aopen+is%3Aissue+label%3A%22good+first+issue%22) sont un bon point de départ pour une première contribution.

Le suivi du projet (milestones, board) est visible sur le [GitHub Project](https://github.com/users/GorskiAnthony/projects/20).

Pour une question générale ou une idée à discuter, passe par les [Discussions](https://github.com/GorskiAnthony/chemin-de-guer/discussions) plutôt que par une issue, qui reste réservée aux bugs et aux demandes de fonctionnalité concrètes.

## Licence

Distribué sous licence [Apache 2.0](LICENSE).
