# STORY-0906 — Portail laboratoire : saisie, validation résultat, PDF et historique

> Ticket de réalignement CDC pour couvrir "Saisie résultat", "Validation résultat", "Envoi PDF" et "Historique résultats".

## 1. Objectif

Permettre au laboratoire ou au biologiste de saisir des résultats structurés, déposer le PDF officiel, valider les résultats et consulter l'historique.

## 2. Critères d'acceptation

- [x] Le laboratoire peut saisir plusieurs analytes avec valeur, unité, intervalle de référence, interprétation et commentaire.
- [x] Le dépôt PDF est optionnel dans le MVP et explicitement traité comme pièce jointe laboratoire.
- [x] La validation des résultats injecte les résultats dans le DPU du patient.
- [ ] Un résultat validé n'est plus modifiable sans mécanisme de version ou correction documentée.
- [x] L'historique des résultats est accessible depuis le portail laboratoire.
- [ ] Les fichiers PDF sont contrôlés en type, taille et contenu minimal côté backend.
- [x] L'UI reprend l'harmonisation du dashboard patient : espaces latéraux réduits, couleurs sobres, composants partagés, états clairs.
- [x] Les textes visibles sont internationalisés FR/EN.
- [x] Les tests frontend couvrent saisie, validation, upload PDF, erreurs et historique.

## 3. Pilotage projet

| Champ | Valeur |
|---|---|
| Epic parent | EPIC-0009 — Intégration Laboratoire & Examens Biologiques |
| User story parent | STORY-0906 |
| Sprint cible | À planifier |
| Priorité business | P1 |
| Story points | 3 |
| Profil recommandé | Senior |
| Effort estimé senior | 0.8j |
| Effort estimé intermédiaire | 1.1j |
| Effort estimé junior | 1.8j |
| Reviewer obligatoire | Lead |
| Risque fonctionnel | Fort |
| Risque technique | Moyen |
| Dépendances | STORY-0905 |

## 4. Action plan

- [x] Définir le contrat de saisie/validation des résultats structurés.
- [x] Vérifier le stockage PDF et les règles de sécurité upload existantes.
- [x] Implémenter ou compléter les endpoints backend de saisie, validation et historique.
- [x] Créer l'écran Angular de saisie/validation de résultats.
- [x] Ajouter les composants UI réutilisables pour lignes de résultat, statut et upload.
- [x] Ajouter les traductions FR/EN.
- [x] Ajouter les tests frontend.
- [ ] Ajouter les tests backend.

## 4.1 Implémentation réalisée

- Frontend : formulaire multi-lignes de résultats sur `/clinic/lab-orders`.
- Frontend : upload PDF optionnel vers `/api/public/lab-integration/upload` avec `X-API-KEY`.
- Frontend : historique des résultats filtré par `examRequestNumber`.
- Backend : réutilisation du service d'upload qui valide la demande et pousse les résultats dans le DPU.
- Tests Angular : endpoints service, rendu, upload, erreur backend, statut et historique.

## 5. Reste à faire

- Confirmer la stratégie de versionnement/correction des résultats validés.
- Ajouter contrôle backend strict du type/taille PDF.
- Exécuter les tests backend dès que Maven peut résoudre les dépendances.

## 6. Statut final

Statut : IMPLEMENTED_QA_BLOCKED

## 7. Impact version / SemVer

| Champ | Valeur |
|---|---|
| Changement livrable | Oui |
| Type de bump | MINOR |
| Justification | Ajout du coeur fonctionnel du portail laboratoire CDC. |
| Breaking change | Non |
| Migration DB | À confirmer |
| Changement API | Oui |
| Impact Angular | Oui |
| Impact Flutter | Non |
