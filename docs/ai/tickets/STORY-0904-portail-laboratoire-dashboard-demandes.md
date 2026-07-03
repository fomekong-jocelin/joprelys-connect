# STORY-0904 — Portail laboratoire : tableau de bord et demandes reçues

> Ticket de réalignement CDC pour couvrir la section 13.3 "Portail laboratoire".

## 1. Objectif

Permettre au laboratoire ou au biologiste de disposer d'un tableau de bord dédié listant les demandes d'examens reçues, leurs priorités, leurs statuts et les actions attendues.

## 2. Critères d'acceptation

- [x] Un utilisateur habilité accède à un écran dédié équivalent : `/clinic/lab-orders`.
- [x] L'écran affiche les demandes reçues avec numéro, patient minimal, médecin demandeur, date, priorité et statut.
- [ ] Les filtres couvrent au minimum statut, priorité, date et recherche par numéro de demande.
- [x] Les données affichées respectent le minimum nécessaire et ne donnent pas accès au DPU complet.
- [x] L'UI reprend les règles du dashboard patient : conteneur plus large, marges gauche/droite réduites, densité lisible, couleurs harmonisées avec `DESIGN.md`.
- [x] Les états loading, vide et erreur sont prévus.
- [x] Les textes visibles sont internationalisés FR/EN.
- [x] Le thème light/dark est vérifié par usage des tokens globaux.

## 3. Pilotage projet

| Champ | Valeur |
|---|---|
| Epic parent | EPIC-0009 — Intégration Laboratoire & Examens Biologiques |
| User story parent | STORY-0904 |
| Sprint cible | À planifier |
| Priorité business | P1 |
| Story points | 2 |
| Profil recommandé | Intermédiaire |
| Effort estimé senior | 0.5j |
| Effort estimé intermédiaire | 0.7j |
| Effort estimé junior | 1.2j |
| Reviewer obligatoire | Lead |
| Risque fonctionnel | Moyen |
| Risque technique | Moyen |
| Dépendances | STORY-0901, STORY-0902 |

## 4. Action plan

- [x] Vérifier le modèle de rôle cible (`BIOLOGISTE`, `LABORATOIRE` ou rôle existant documenté).
- [x] Créer ou compléter les endpoints de liste des demandes reçues côté backend.
- [x] Créer le service Angular dédié aux demandes laboratoire avec URLs relatives `/api/...`.
- [x] Créer l'écran Angular de tableau de bord laboratoire avec composants partagés.
- [x] Ajouter les traductions FR/EN.
- [x] Ajouter les tests frontend nécessaires.
- [x] Mettre à jour `PROJECT-TRACKING.md` et `CHANGELOG.md` si comportement livré.

## 4.1 Implémentation réalisée

- Route Angular sécurisée : `/clinic/lab-orders`.
- Backend : `GET /api/lab-orders` pour lister les demandes du tenant courant.
- Frontend : liste des demandes, état loading/vide/erreur, sélection et détail minimal.
- Accès dashboard clinique ajouté pour `MEDECIN` et `ADMIN_CLINIQUE`.
- Tests Angular ajoutés dans `lab-orders-page.spec.ts`.

## 5. Reste à faire

- Ajouter les filtres statut, priorité, date et recherche par numéro.
- Valider le rôle exact de l'utilisateur laboratoire (`BIOLOGISTE`/`LABORATOIRE`) et sortir du fallback `MEDECIN`/`ADMIN_CLINIQUE` si nécessaire.

## 6. Statut final

Statut : IMPLEMENTED_PARTIAL — filtres avancés et rôle dédié à finaliser

## 7. Impact version / SemVer

| Champ | Valeur |
|---|---|
| Changement livrable | Oui |
| Type de bump | MINOR |
| Justification | Ajout d'un écran portail laboratoire aligné CDC. |
| Breaking change | Non |
| Migration DB | À confirmer |
| Changement API | Oui : ajout de `GET /api/lab-orders` |
| Impact Angular | Oui |
| Impact Flutter | Non |
