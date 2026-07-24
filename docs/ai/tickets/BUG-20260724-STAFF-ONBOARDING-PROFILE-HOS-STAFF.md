# BUG-20260724 — Onboarding collaborateur et profil professionnel HOS-STAFF

## Contexte

La configuration HOS-ORG / HOS-LOC / HOS-STAFF est structurée et fusionnée, mais la préparation de la démonstration #127 a révélé un écart d'intégration UX :

- la création d'un collaborateur ne demandait que nom, e-mail et rôles RBAC ;
- les informations professionnelles n'étaient disponibles qu'en édition ;
- les affectations structurées HOS-STAFF n'étaient configurables qu'après la création du compte ;
- le profil personnel indiquait que les affectations étaient gérées par l'établissement sans montrer les spécialités/unités actives.

## Objectif

Aligner l'onboarding et le profil collaborateur sur HOS-STAFF sans réintroduire de champs libres `department` / `specialty`.

## Périmètre livré dans PR #144

### Création collaborateur

Le parcours d'invitation permet dans un seul formulaire :

1. identité : nom complet, e-mail, téléphone ;
2. rôles RBAC ;
3. informations professionnelles : numéro d'ordre pour un médecin, bio facultative ;
4. spécialité principale structurée facultative depuis `medical_specialty_catalog` ;
5. unité organisationnelle principale structurée facultative depuis `organizational_units` ;
6. rôle contextuel dans l'unité depuis `staff_assignment_role_catalog` ;
7. date de début de l'affectation.

Le backend enregistre le compte, les rôles et les affectations initiales dans une transaction cohérente. L'e-mail contenant le mot de passe temporaire n'est envoyé qu'après validation des affectations. Une affectation invalide/cross-tenant provoque le rollback complet du compte.

Le rôle contextuel n'est proposé qu'après sélection d'une unité : une invitation sans affectation initiale reste possible sans créer de relation partielle ou ambiguë.

### Profil collaborateur

Le collaborateur voit désormais en lecture seule :

- ses rôles de compte ;
- ses spécialités actives avec indication principale ;
- ses unités organisationnelles actives ;
- son rôle contextuel dans chaque unité ;
- l'indication d'affectation principale ;
- ses informations personnelles/professionnelles modifiables existantes ;
- signature/cachet uniquement pour les médecins.

Les affectations restent administrées par l'établissement ; le collaborateur ne peut pas modifier lui-même ses unités/spécialités depuis `/profile`.

## Règles métier

- aucun champ libre `department` ou `specialty` ;
- tenant isolation obligatoire ;
- seules des unités actives du même établissement sont sélectionnables ;
- seules des spécialités actives du catalogue HOS-ORG sont sélectionnables ;
- le rôle contextuel est distinct du rôle RBAC ;
- `registrationNumber` est requis lors de la création si le rôle `MEDECIN` est sélectionné ;
- les affectations initiales sont ouvertes à partir de la date choisie, sans date de fin par défaut ;
- le profil personnel affiche les affectations actives mais ne les modifie pas ;
- l'éditeur HOS-STAFF complet reste la source d'administration pour l'historique et les changements ultérieurs.

## Critères d'acceptation

- [x] Le formulaire de création ne se limite plus à nom/e-mail/rôle.
- [x] Un médecin peut être créé avec téléphone, numéro d'ordre, spécialité principale et unité principale en une seule opération.
- [x] Un infirmier/pharmacien peut recevoir une unité principale et un rôle contextuel sans spécialité obligatoire.
- [x] Aucun compte partiellement créé n'est conservé si une affectation initiale est invalide.
- [x] Le profil personnel affiche les rôles, spécialités et unités actives avec leurs libellés métier.
- [x] Les affectations affichées sur le profil sont en lecture seule.
- [x] L'éditeur HOS-STAFF historique reste disponible en administration pour les changements ultérieurs.
- [x] Dictionnaires FR/EN dédiés ajoutés.
- [ ] Contrôle humain responsive 320/375/768/desktop, light/dark et FR/EN en RECETTE.
- [x] Tests backend et Angular verts sur gate combiné #1277.
- [x] Build Angular production vert sur gate combiné #1277.

## Tests automatisés ajoutés / adaptés

- `StaffOnboardingControllerTest` : onboarding médecin complet, numéro d'Ordre obligatoire, rollback sur unité cross-tenant ;
- `ProfileAssignmentsControllerTest` : résolution des libellés métier du contexte professionnel ;
- `StaffControllerTest` : invitation médecin historique alignée sur le numéro d'Ordre ;
- `staff-management.component.spec.ts` : catalogues, payload structuré, validation médecin, erreurs mail ;
- `profile.component.spec.ts` : affichage spécialité/unité/rôle contextuel actif.

### Preuve CI intermédiaire

Gate combiné **#1277** sur `0d546f1ce4479bd3f1b8efc2fea6cc8983ef4e9e` :

- Maven strict `clean verify` : SUCCESS ;
- tests Angular : SUCCESS (348 tests) ;
- build Angular production : SUCCESS.

Un gate final sur le head documentaire exact reste requis avant squash merge.

## Documentation / dette

Le ticket historique `TICKET-CLINIC-STAFF-ENRICHED-PROFILES` décrivait encore une époque où spécialité/département étaient portés comme champs de profil. Cette partie est supersédée par HOS-STAFF et le présent correctif ; les ressources photo/signature/cachet ainsi que le service d'upload restent valides.

## Parent

QA / démonstration : #127.

## Statut

**READY TECHNIQUE sous réserve du gate final post-documentation et de la recette visuelle humaine #127.**
