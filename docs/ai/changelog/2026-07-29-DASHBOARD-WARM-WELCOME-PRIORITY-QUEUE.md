# 2026-07-29 — Accueil clinique chaleureux et file active prioritaire

Issue #237 / PR #238.

- L’accueil du dashboard utilise désormais un message plus humain et un rôle métier traduit au lieu du code technique brut lorsqu’une traduction existe.
- La file d’attente déjà présente sur le dashboard reste l’unique source opérationnelle ; aucun endpoint ni second composant de queue n’est créé.
- Cette file est placée visuellement avant les cartes de modules afin de faire passer l’activité à traiter avant la navigation.
- Son sous-titre devient une synthèse déterministe des visites déjà chargées : total actif, constantes à saisir, constantes saisies et attente maximale.
- Le tri de la file utilise `arrivalAt` lorsqu’il est disponible, avec `createdAt` comme repli.
- Les actions détails, constantes, consultation et clôture ainsi que le RBAC restent inchangés.
- Aucun backend, contrat API, modèle de données ou déploiement n’est inclus.
