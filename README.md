# Chemin de Guer

Blog personnel retraçant un parcours de conversion au judaïsme (guer, גר : converti, en hébreu). Le projet sert aussi de terrain d'apprentissage pour Java/Spring Boot et Angular dans leurs dernières versions stables.

Documentation complète :

- [Cahier des charges](docs/cahier-des-charges.html) : périmètre, spécifications techniques, exigences, planning
- [Système de design](docs/design-system.html) : palette, typographie, composants

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

- Frontend : http://localhost
- Backend : http://localhost:8080
- Postgres : localhost:5432

## Contribuer

Les contributions sont bienvenues. Avant d'ouvrir une pull request, jette un œil à [CONTRIBUTING.md](CONTRIBUTING.md) et à la [charte de bonne conduite](CODE_OF_CONDUCT.md).

Les issues marquées [`good first issue`](https://github.com/GorskiAnthony/chemin-de-guer/issues?q=is%3Aopen+is%3Aissue+label%3A%22good+first+issue%22) sont un bon point de départ pour une première contribution.

Le suivi du projet (milestones, board) est visible sur le [GitHub Project](https://github.com/users/GorskiAnthony/projects/20).

## Licence

Distribué sous licence [Apache 2.0](LICENSE).
