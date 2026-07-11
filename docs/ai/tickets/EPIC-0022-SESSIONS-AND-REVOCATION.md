# EPIC-0022 — Sessions persistantes et révocation distribuée

## Statut

- **Issue GitHub :** #29
- **Priorité :** P0
- **État :** READY FOR PLANNING
- **Dépendance payante :** aucune
- **Hors périmètre :** fournisseur OTP SMS/e-mail, reporté à la fin du projet

## Décision produit

L'envoi OTP via un fournisseur externe n'est pas engagé dans cette vague pour raison budgétaire. Le projet poursuit toutefois l'industrialisation des sessions et de la révocation, qui ne nécessite aucun service payant.

Les mesures sans coût restent obligatoires :

- aucun code OTP dans les logs de production ;
- aucune exposition du code OTP au frontend hors profil local/test explicitement contrôlé ;
- aucun secret par défaut utilisable en production.

## Objectif

Remplacer la blacklist JWT en mémoire et la session uniquement stateless par :

- access tokens courts ;
- refresh tokens opaques et rotatifs ;
- sessions persistantes ;
- détection du rejeu ;
- révocation d'une session ou de toutes les sessions ;
- fonctionnement cohérent après redémarrage et sur plusieurs instances.

## Stories

| Story | Issue | SP | Dépendances | Résultat |
|---|---:|---:|---|---|
| STORY-2401 — Sessions persistantes et rotation | #31 | 8 | Aucune | Modèle DB, login enrichi et endpoint refresh atomique |
| STORY-2402 — Révocation et rejeu | #33 | 5 | #31 | Logout courant/global, liste de sessions, révocation et audit |
| STORY-2403 — Angular et gestion des sessions | #34 | 5 | #31, #33 | Refresh contrôlé, UX des sessions et fin de session propre |

## Ordonnancement

```text
STORY-2401 → STORY-2402 → STORY-2403
```

## Architecture cible initiale

- table `auth_sessions` tenantée ;
- refresh token stocké uniquement sous forme de hash ;
- famille de tokens et rotation à usage unique ;
- verrouillage optimiste ou pessimiste ciblé pour les refresh concurrents ;
- expiration absolue et expiration d'inactivité ;
- audit de création, rotation, révocation, logout-all et rejeu ;
- nettoyage planifié des sessions expirées.

Un ADR doit fixer le mode de transport/stockage du refresh token côté navigateur, avec préférence à une solution limitant l'accès JavaScript lorsque compatible avec l'architecture de déploiement.

## Definition of Done

- H2 et PostgreSQL 16 validés ;
- tests de concurrence et rejeu ;
- persistance prouvée après redémarrage ;
- aucun token brut en base ou logs ;
- refus cross-user et cross-tenant ;
- Angular sans boucle de refresh ;
- documentation fonctionnelle, technique, API, data et test ;
- changelog et décision SemVer ;
- CI complète verte.
