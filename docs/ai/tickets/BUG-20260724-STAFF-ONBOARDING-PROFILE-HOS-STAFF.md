# BUG-20260724 — Onboarding collaborateur et profil professionnel HOS-STAFF

## Contexte

La configuration HOS-ORG / HOS-LOC / HOS-STAFF est désormais structurée et fusionnée, mais l'expérience d'administration du personnel reste incohérente avec le modèle livré :

- la création d'un collaborateur ne demande que nom, e-mail et rôles RBAC ;
- les informations professionnelles (téléphone, numéro d'ordre, bio) ne sont disponibles qu'en édition ;
- les affectations structurées HOS-STAFF ne sont configurables qu'après la création du compte ;
- le profil personnel indique que les affectations sont gérées par l'établissement mais ne montre pas les spécialités/unités actives du collaborateur.

Cet écart est visible en RECETTE pendant la préparation de la démonstration #127.

## Objectif

Aligner l'onboarding et le profil collaborateur sur HOS-STAFF sans réintroduire de champs libres `department` / `specialty`.

## Périmètre

### Création collaborateur

Le parcours d'invitation doit permettre, dans un seul formulaire :

1. identité : nom complet, e-mail, téléphone ;
2. rôles RBAC ;
3. informations professionnelles : numéro d'ordre pour un médecin, bio facultative ;
4. spécialité principale structurée facultative depuis `medical_specialty_catalog` ;
5. unité organisationnelle principale structurée facultative depuis `organizational_units` ;
6. rôle contextuel dans l'unité depuis `staff_assignment_role_catalog` ;
7. date de début de l'affectation.

Le backend doit enregistrer le compte et les affectations initiales dans une transaction cohérente. Un échec de validation d'affectation ne doit pas laisser un collaborateur partiellement créé.

### Profil collaborateur

Le collaborateur doit voir en lecture seule :

- ses rôles de compte ;
- ses spécialités actives, avec indication principale ;
- ses unités organisationnelles actives, rôle contextuel et indication principale ;
- ses informations personnelles/professionnelles modifiables déjà existantes ;
- signature/cachet uniquement pour les médecins.

Les affectations restent administrées par l'établissement ; le collaborateur ne peut pas modifier lui-même ses unités/spécialités.

## Règles métier

- aucun champ libre `department` ou `specialty` ;
- tenant isolation obligatoire ;
- seules des unités actives du même établissement sont sélectionnables ;
- seules des spécialités actives du catalogue HOS-ORG sont sélectionnables ;
- le rôle contextuel est distinct du rôle RBAC ;
- `registrationNumber` est requis lors de la création si le rôle `MEDECIN` est sélectionné ;
- les affectations initiales sont ouvertes à partir de la date choisie, sans date de fin par défaut ;
- le profil personnel affiche les affectations actives mais ne les modifie pas.

## Critères d'acceptation

- [ ] Le formulaire de création ne se limite plus à nom/e-mail/rôle.
- [ ] Un médecin peut être créé avec téléphone, numéro d'ordre, spécialité principale et unité principale en une seule opération.
- [ ] Un infirmier/pharmacien peut recevoir une unité principale et un rôle contextuel adapté sans spécialité obligatoire.
- [ ] Aucun compte partiellement créé n'est conservé si une affectation initiale est invalide.
- [ ] Le profil personnel affiche les rôles, spécialités et unités actives avec leurs libellés métier.
- [ ] Les affectations affichées sur le profil sont en lecture seule.
- [ ] L'éditeur HOS-STAFF historique reste disponible en administration pour les changements ultérieurs.
- [ ] FR/EN, light/dark et mobile vérifiés.
- [ ] Tests backend et Angular verts.
- [ ] Build Angular production vert.

## Documentation / dette

Le ticket historique `TICKET-CLINIC-STAFF-ENRICHED-PROFILES` décrivait encore des champs libres spécialité/département. Le présent correctif remplace cette partie par les affectations structurées HOS-STAFF ; les ressources photo/signature/cachet restent valides.

## Parent

QA / démonstration : #127.
