# STORY-0905 — Portail laboratoire : détail demande et changement de statut

> Ticket de réalignement CDC pour couvrir "Détail demande" et le statut selon droits laboratoire.

## 1. Objectif

Permettre au laboratoire ou au biologiste d'ouvrir une demande reçue, d'en consulter le détail utile et de changer son statut selon ses droits.

## 2. Critères d'acceptation

- [x] Le laboratoire peut ouvrir le détail d'une demande depuis le tableau de bord.
- [x] L'écran affiche le numéro de demande, les examens demandés, l'indication clinique, la priorité, le médecin demandeur et les informations patient minimales nécessaires.
- [x] Les transitions de statut autorisées couvrent `SAMPLE_COLLECTED`, `IN_PROGRESS`, `RESULT_AVAILABLE`, `VALIDATED`, `CANCELLED` selon les règles backend.
- [x] Le backend reste maître des transitions et refuse toute transition non autorisée.
- [x] Chaque changement de statut est audité.
- [x] L'UI reprend la densité du dashboard patient : largeur utile accrue, panneaux sobres, couleurs harmonisées, pas de sur-arrondis.
- [x] Les textes visibles sont internationalisés FR/EN.
- [x] Les tests frontend couvrent la transition de statut.

## 3. Pilotage projet

| Champ | Valeur |
|---|---|
| Epic parent | EPIC-0009 — Intégration Laboratoire & Examens Biologiques |
| User story parent | STORY-0905 |
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
| Dépendances | STORY-0904 |

## 4. Action plan

- [x] Documenter les transitions de statut autorisées par rôle.
- [x] Implémenter ou compléter le use case backend de changement de statut.
- [x] Ajouter les endpoints REST sécurisés nécessaires.
- [x] Créer l'écran Angular de détail demande.
- [x] Ajouter les messages d'erreur métier et traductions FR/EN.
- [x] Ajouter les tests frontend d'affichage et d'action.
- [ ] Ajouter les tests backend d'autorisation, transitions et audit.

## 4.1 Implémentation réalisée

- Backend : `PATCH /api/lab-orders/{id}/status` avec liste blanche de statuts CDC.
- Backend : audit `UPDATE_LAB_ORDER_STATUS`.
- Frontend : sélection de statut et mise à jour depuis le détail de la demande.
- Tests Angular : appel `PATCH` et mise à jour de la demande sélectionnée.

## 5. Reste à faire

- Confirmer le modèle de droits laboratoire/biologiste.
- Exécuter/ajouter les tests backend dès que Maven peut résoudre le parent Spring Boot localement ou en CI.

## 6. Statut final

Statut : IMPLEMENTED_QA_BLOCKED

## 7. Impact version / SemVer

| Champ | Valeur |
|---|---|
| Changement livrable | Oui |
| Type de bump | MINOR |
| Justification | Ajout de changement de statut laboratoire et écran de détail. |
| Breaking change | Non |
| Migration DB | À confirmer |
| Changement API | Oui |
| Impact Angular | Oui |
| Impact Flutter | Non |
