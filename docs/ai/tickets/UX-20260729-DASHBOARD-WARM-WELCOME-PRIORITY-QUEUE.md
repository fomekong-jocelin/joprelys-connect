# UX-20260729 — Accueil clinique chaleureux et file d’attente prioritaire

GitHub issue : #237

## Contexte

Le dashboard clinique contient déjà une vraie `File d'Attente Active` alimentée par `VisitApiService.getActiveVisits()`. Elle gère déjà le tri, les détails d’admission, les constantes, le démarrage de consultation et la clôture selon le RBAC.

Le besoin n’est donc pas de créer une seconde file, mais de mieux exploiter celle qui existe :

- accueil moins technique ;
- rôle métier lisible au lieu du code brut ;
- file active remontée avant les cartes de navigation ;
- synthèse rapide de l’activité réelle ;
- conservation intégrale des actions existantes.

## Décision fonctionnelle

### En-tête

Afficher :

- `Bonjour, {nom}` ;
- un sous-titre chaleureux et opérationnel ;
- le ou les rôles traduits via `role.<CODE>` ;
- un badge `Accès clinique actif`.

Aucun parsing hasardeux du prénom n’est effectué : le nom de session reste la source d’identité affichée.

### File d’attente

Réutiliser le bloc existant et le placer visuellement avant les cartes de modules.

La synthèse est calculée uniquement depuis `activeVisits()` :

- total des visites actives ;
- visites sans constantes ;
- visites avec constantes ;
- durée de la plus longue attente, calculée depuis `arrivalAt` si disponible, sinon `createdAt`.

Aucun faux KPI `consultation en cours`, `prioritaire`, `labo en attente`, etc. n’est créé sans donnée backend explicite.

### Ordre de page

1. accueil chaleureux ;
2. file d’attente active existante ;
3. cartes de modules / portails ;
4. modales existantes inchangées.

## Critères d’acceptation

- [ ] Message d’accueil FR/EN plus humain.
- [ ] Codes de rôle traduits quand une clé `role.<CODE>` existe.
- [ ] File existante affichée avant les cartes métier sans nouvel appel API.
- [ ] 4 indicateurs réels : total, constantes à saisir, constantes saisies, attente la plus longue.
- [ ] Tri basé sur `arrivalAt ?? createdAt`.
- [ ] Colonne/heure d’arrivée basée sur `arrivalAt ?? createdAt`.
- [ ] Actions actuelles de la file inchangées.
- [ ] Permissions existantes inchangées.
- [ ] Mobile, desktop, light/dark, FR/EN validés.
- [ ] Tests Angular et build production verts.

## Hors périmètre

- backend / DB / migration ;
- nouveau statut de visite ;
- nouvelle API de queue ;
- déploiement recette / production.

## SemVer

MINOR UI/UX non-breaking au niveau produit ; aucun contrat API n’est modifié.
