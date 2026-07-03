# FUNCTIONAL-SPEC — Validation de demande d'accès externe - STORY-1302

## 1. Résumé métier

Lorsqu'un établissement externe formule une demande d'accès au DPU d'un patient, ce dernier doit pouvoir contrôler souverainement l'accès à ses données de santé. Depuis son portail patient personnel sécurisé, le patient consulte la liste des demandes d'accès formulées par des tiers, lit le motif et la durée demandée, puis choisit d'approuver (pour ouvrir ses données à cet établissement) ou de rejeter (pour refuser l'accès) chaque demande.

## 2. Objectifs

- Offrir au patient une interface sécurisée sur son portail pour lister les demandes d'accès externes en attente.
- Permettre au patient d'approuver une demande (passage du statut à `APPROUVEE` avec calcul de la date d'expiration).
- Permettre au patient de rejeter une demande (passage du statut à `REFUSEE`).
- Garantir le respect des droits et la transparence pour le patient sur l'accès à ses données.

## 3. Utilisateurs / acteurs concernés

| Acteur | Besoin | Droits / limites |
|---|---|---|
| Patient | Consulter les demandes d'accès et décider d'approuver ou refuser | Peut voir les demandes le concernant sur son portail. Peut approuver ou rejeter les demandes en attente. |

## 4. Périmètre

### Inclus

- API REST pour le patient connecté afin de lister ses demandes d'accès (`GET /api/patient/me/access-requests`).
- API REST pour approuver (`POST /api/patient/me/access-requests/{id}/approve`) et rejeter (`POST /api/patient/me/access-requests/{id}/reject`).
- Interface utilisateur (IHM Angular, Tailwind CSS v4) intégrée dans le portail patient (onglet "Demandes d'accès").
- Traduction complète en français (FR) et anglais (EN) de tous les éléments d'interface.
- Notifications toast de succès ou d'erreur sur l'IHM après action.

### Exclus

- Mécanisme de filtrage automatique des requêtes cliniques des praticiens (couvert par STORY-1303).
- Notification asynchrone par e-mail ou SMS (mockée dans STORY-1501).

## 5. Parcours utilisateur

1. Le patient se connecte sur son portail patient Joprelys Connect.
2. Il navigue vers l'onglet **Sécurité & Consentements** ou un nouvel onglet dédié **Demandes d'accès**.
3. L'application affiche un badge rouge avec le nombre de demandes en attente (ex: "1 demande en attente").
4. Dans la liste, le patient voit le nom de la clinique requérante, le motif saisi par le praticien, la durée demandée (ex: "24 heures") et la date de la demande.
5. Le patient clique sur le bouton **Approuver** :
   - Un toast vert "Accès autorisé avec succès" s'affiche.
   - La demande disparaît de la liste des demandes en attente et s'affiche dans l'historique des accès accordés avec sa date d'expiration.
6. Si le patient clique sur **Rejeter** :
   - Un toast rouge "Demande d'accès rejetée" s'affiche.
   - La demande passe au statut rejeté.

## 6. Règles métier

| ID | Règle | Priorité | Source |
|---|---|---|---|
| BR-1302-01 | Un patient ne peut visualiser, approuver ou rejeter que les demandes d'accès qui le concernent directement (corrélation stricte avec son DPU). | P0 | Spécification |
| BR-1302-02 | Seules les demandes au statut `EN_ATTENTE` peuvent être approuvées ou rejetées. | P0 | Spécification |
| BR-1302-03 | L'approbation d'une demande change son statut en `APPROUVEE` et calcule sa date d'expiration : `expires_at = NOW() + requested_duration_hours`. | P0 | Spécification |
| BR-1302-04 | Le rejet d'une demande change son statut en `REFUSEE`. | P0 | Spécification |

## 7. Critères d’acceptation

- [ ] L'API patient rejette toute action sur une demande appartenant à un autre patient (HTTP 403 Forbidden).
- [ ] L'IHM du portail patient affiche de manière sobre et claire la liste des demandes en attente, le motif, l'établissement demandeur, et la durée.
- [ ] Le clic sur "Approuver" ou "Rejeter" désactive temporairement le bouton (spinners de chargement) pour éviter les double-clics, puis met à jour la liste.
- [ ] Toutes les chaînes de caractères de l'IHM sont gérées par `I18nService`.

## 8. Cas limites / erreurs attendues

| Cas | Comportement attendu |
|---|---|
| Demande déjà approuvée/rejetée | HTTP 400 Bad Request avec message indiquant que la demande n'est plus en attente. |
| Demande inexistante | HTTP 404 Not Found. |

## 9. Textes / i18n

| Clé | Français | English |
|---|---|---|
| `patient.access.requests.title` | Demandes d'accès externes | External access requests |
| `patient.access.requests.empty` | Aucune demande d'accès en attente. | No pending access requests. |
| `patient.access.requests.reason` | Motif : | Reason: |
| `patient.access.requests.duration` | Durée demandée : | Requested duration: |
| `patient.access.requests.hours` | {0} heures | {0} hours |
| `patient.access.requests.approve` | Approuver | Approve |
| `patient.access.requests.reject` | Rejeter | Reject |
| `patient.access.requests.approved` | Accès autorisé avec succès. | Access successfully authorized. |
| `patient.access.requests.rejected` | Demande d'accès rejetée. | Access request rejected. |

## 10. Impacts UI / branding

| Point | Impact |
|---|---|
| Nom de l’app | Non |
| Logo | Non |
| Thème light/dark | Oui (styles conformes à `DESIGN.md`) |
| Composants réutilisables | Oui (utilisation de boutons et cartes standardisés, arrondis sobres `rounded-[var(--radius-brand-sm)]`) |

## 11. Hypothèses et questions ouvertes

- **Consentements** : L'approbation d'une demande d'accès externe crée-t-elle aussi une entité `PatientConsentEntity` dans la base ?
  - *Décision* : Non, pour garder une traçabilité propre des durées et dates d'expiration spécifiques de chaque demande, la table `external_access_requests` avec le statut `APPROUVEE` et `expires_at` servira directement de source de vérité pour le contrôle d'accès dans la STORY-1303.

## 12. Historique des mises à jour

| Date | Auteur | Changement |
|---|---|---|
| 2026-07-04 | Antigravity | Création de la spécification |
