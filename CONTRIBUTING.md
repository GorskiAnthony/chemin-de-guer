# Contribuer à Chemin de Guer

Merci de l'intérêt porté à ce projet. Ce document explique comment proposer un changement.

## Avant de commencer

1. Lis la [charte de bonne conduite](CODE_OF_CONDUCT.md).
2. Regarde le [cahier des charges](docs/cahier-des-charges.html) pour comprendre le périmètre et les choix déjà actés.
3. Vérifie les [issues ouvertes](https://github.com/GorskiAnthony/chemin-de-guer/issues) pour éviter de dupliquer un travail en cours. Les issues [`good first issue`](https://github.com/GorskiAnthony/chemin-de-guer/issues?q=is%3Aopen+is%3Aissue+label%3A%22good+first+issue%22) sont pensées pour une première contribution.

## Signaler un bug ou proposer une fonctionnalité

Utilise les formulaires d'issue dédiés (bug ou fonctionnalité), accessibles depuis l'onglet "Issues" du dépôt. Ils demandent les informations nécessaires pour traiter la demande rapidement.

## Proposer un changement de code

1. Fork le dépôt, puis crée une branche depuis `main` avec un nom descriptif (par exemple `feature/recherche-articles` ou `fix/pagination-commentaires`).
2. Lance le projet en local avec `docker compose up --build` (voir le [README](README.md)).
3. Développe ton changement en respectant la stack et l'architecture décrites dans le cahier des charges.
4. Ajoute des tests pour la logique métier que tu modifies ou ajoutes.
5. Ouvre une pull request en remplissant le template fourni, en liant l'issue correspondante si elle existe.

## Convention de commit

- Messages rédigés en français, clairs sur le "pourquoi" du changement.
- Pas de tiret cadratin (—) dans les messages de commit.
- Pas d'attribution à un outil d'intelligence artificielle dans les commits (pas de `Co-Authored-By`, pas de `Generated with`, etc.), quel que soit l'outil utilisé pour écrire le code.

## Revue

Chaque pull request est relue avant fusion, avec une attention particulière portée à la sécurité, à la performance et à la couverture de tests, conformément aux exigences non fonctionnelles du cahier des charges.
