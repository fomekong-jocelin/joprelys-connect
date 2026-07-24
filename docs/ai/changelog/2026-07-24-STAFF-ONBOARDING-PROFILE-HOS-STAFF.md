# 2026-07-24 — Onboarding et profil professionnel HOS-STAFF

## Contexte

La répétition générale #127 a révélé que le modèle HOS-STAFF était correctement structuré en backend mais encore présenté comme une fonction secondaire dans l'UX : création collaborateur limitée à nom/e-mail/rôles et affectations visibles uniquement après création.

## Correctif PR #144

### Onboarding administrateur

- ajout téléphone et bio dès la création ;
- numéro d'inscription à l'Ordre obligatoire pour un médecin ;
- sélection facultative d'une spécialité principale depuis le catalogue HOS-ORG ;
- sélection facultative d'une unité principale ;
- rôle contextuel HOS-STAFF lié à l'unité ;
- date de début structurée ;
- rôle contextuel activé uniquement lorsqu'une unité est choisie ;
- compte, rôles RBAC et affectations initiales enregistrés dans une même transaction ;
- rollback complet si une affectation initiale est invalide ou cross-tenant ;
- envoi du mot de passe temporaire seulement après validation des affectations.

### Profil collaborateur

Ajout de `GET /api/profile/assignments` et d'une section `Mon exercice professionnel` en lecture seule :

- rôles de compte ;
- spécialités actives avec indication principale ;
- unités actives ;
- rôle contextuel et indication principale.

Les affectations restent modifiables uniquement depuis l'administration HOS-STAFF afin de préserver l'historique.

## Internationalisation

Ajout des dictionnaires :

- `web/src/assets/i18n/features/staff-onboarding/fr.json` ;
- `web/src/assets/i18n/features/staff-onboarding/en.json`.

## Validation

Gate combiné #1277 sur `0d546f1ce4479bd3f1b8efc2fea6cc8983ef4e9e` :

- Maven strict : SUCCESS ;
- tests Angular : SUCCESS (348) ;
- build Angular production : SUCCESS.

Un gate final post-documentation reste requis avant squash merge.

## Dette historique

Les mentions anciennes de spécialité/département comme champs du profil dans `TICKET-CLINIC-STAFF-ENRICHED-PROFILES.md` doivent être lues comme historiques. Depuis HOS-STAFF, l'identité d'exercice est portée par les affectations structurées et datées ; seuls les éléments photo/signature/cachet/téléphone/bio du ticket historique restent directement applicables.
