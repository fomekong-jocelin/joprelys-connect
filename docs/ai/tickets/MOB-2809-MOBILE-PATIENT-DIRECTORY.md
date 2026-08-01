# MOB-2809 — Recherche et liste patients mobile

## Infos

- Mode : Engineering + réconciliation de livraison
- Date : 2026-08-01
- Priorité : P0
- Statut : IN_REVIEW — capacité fusionnée par la PR #258 ; documentation de
  feature et gates Android exact-HEAD encore requis avant DONE
- Target Release : candidat MINOR `0.11.0` ; aucune release préparée

## Objectif

En tant que professionnel connecté, je veux rechercher un patient depuis
l'application mobile et ouvrir son dossier, afin d'accéder rapidement au bon
contexte clinique sans dupliquer la vérité métier du backend.

## Traçabilité

La capacité avait été enregistrée à tort sous `MOB-2817` dans le changelog de la
branche cumulative. Elle est réattribuée à `MOB-2809`, ID canonique de
`EPIC-0028`. Le code a été fusionné dans `main` par la PR #258 au merge commit
`e6f7a348`.

## Critères d'acceptation

- [x] La recherche consomme `GET /api/patients?q={query}` via le client réseau
  central, sans URL backend codée en dur.
- [x] Les résultats présentent les identifiants utiles sans exposer de secret.
- [x] L'utilisateur peut ouvrir le dossier patient depuis un résultat.
- [x] Les états file active et annuaire restent distincts.
- [x] Les libellés utilisent l'i18n FR/EN et les thèmes centraux.
- [x] Les spécifications fonctionnelle, technique, API et test de feature sont
  complétées dans `docs/features/mobile-patient-directory`.
- [ ] `flutter analyze`, `flutter test`, build APK et recette Android sont verts
  sur le même HEAD.

## Estimation / responsabilités

- Estimation canonique : 3 SP ; implémentation déjà intégrée, reste QA/doc.
- Profil recommandé : Flutter intermédiaire, reviewer senior.
- Reviewer : Tech Lead Flutter + QA mobile + référent protection des données.

## Sécurité / régression

- Le backend reste maître de la recherche et des autorisations patient.
- Aucun cache DPU persistant supplémentaire n'est autorisé par ce ticket.
- Aucun token, terme recherché ni donnée patient ne doit être journalisé.

## Impact version / SemVer

Nouvelle capacité rétrocompatible : candidat MINOR `0.11.0` lors d'une release
préparée. Aucun bump n'est réalisé par la réconciliation.

## Reste à faire

- Ajouter/identifier les tests ciblés d'annuaire.
- Fermer les gates exact-HEAD et la recette Android avant passage à DONE.
