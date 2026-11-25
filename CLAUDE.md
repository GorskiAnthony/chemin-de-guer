# Chemin de Guer

Blog personnel racontant un parcours de conversion au judaïsme (guer = converti, en hébreu). Projet avant tout pédagogique : apprendre Java/Spring Boot et Angular en dernières versions stables.

## Stack

- Backend : Java 21, Spring Boot 4.0 (Maven), Spring Data JPA, Spring Security, PostgreSQL 16
- Frontend : Angular 22 (standalone components, signals), SCSS
- Orchestration : Docker Compose (Dokploy en production)

## Architecture

Mono-repo, un seul dépôt Git à la racine :

- `backend/` : API REST Spring Boot
- `frontend/` : SPA Angular
- `docker-compose.yml` : services `cdg-db` (Postgres), `cdg-backup`, `cdg-backend`, `cdg-frontend` (build Angular servi par nginx, qui proxy `/api/` vers le backend)
- `backup/` : image de sauvegarde ; `nas/` : kit de copie sur un NAS ; `docs/` : documentation

Ne pas créer de sous-dépôt Git dans `backend/` ou `frontend/`.

## Lancer le projet en local

```
docker compose up --build
```

- Site : http://localhost (ou `FRONTEND_PORT`), nginx proxifie `/api/` vers le backend
- Backend et Postgres ne sont pas publiés sur la machine

Variables d'environnement dans `.env` (voir `.env.example`), non committé.

## Déploiement

En production sur un VPS OVH via Dokploy (projet Compose, Traefik, Let's Encrypt) : https://chemin-de-guer.fr. Les services s'appellent `cdg-db`, `cdg-backup`, `cdg-backend`, `cdg-frontend` (préfixe `cdg-` pour éviter les collisions de noms sur le réseau partagé de Dokploy). Le domaine pointe sur `cdg-frontend` port 80. `docker-compose.override.yml` ne sert qu'en local. Procédure complète : `docs/deploiement.md`.

## Fonctionnalités v1 (livrées)

- Back-office avec authentification (session + CSRF), articles Markdown avec slug, tags, recherche, brouillons
- Commentaires avec modération et limitation de débit
- Fermeture automatique du site pendant Shabbat (Hebcal, Paris, marge 30 min)
- Accueil avec calendrier hébraïque, onglets Paracha et Fêtes (résumés de l'auteur)
- SEO, partage social, GEO : pages rendues côté serveur par le backend, images Open Graph, sitemap, RSS, `llms.txt`; `SITE_INDEXABLE=false` par défaut (noindex partout)
- Sauvegardes automatiques (`cdg-backup`) et kit NAS (`nas/`)

Reste à faire : voir les issues ouvertes (timeline, glossaire, parcours thématiques, ressources PDF).

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
