# STORY-1004 — Portail pharmacie : vérification et détail ordonnance

> Ticket de réalignement CDC pour couvrir "Vérification ordonnance" et "Détail ordonnance".

## 1. Objectif

Permettre au pharmacien de vérifier une ordonnance par numéro/QR code et PIN, puis d'afficher uniquement les informations nécessaires à la délivrance.

## 2. Critères d'acceptation

- [x] Un écran pharmacie dédié existe pour vérifier une ordonnance.
- [x] Le pharmacien peut saisir le numéro d'ordonnance et le PIN.
- [x] En cas de succès, l'écran affiche statut, patient minimal, prescripteur, date, expiration et lignes de médicaments.
- [x] En cas d'ordonnance expirée, annulée, révoquée ou entièrement délivrée, l'état est affiché clairement via le statut renvoyé par le backend.
- [x] Les données exposées respectent `FR-PRESC-004` : pas d'accès au DPU complet.
- [x] L'UI reprend le style du dashboard patient : largeur utile, marges gauche/droite réduites, couleurs harmonisées, cards sobres.
- [x] Les textes visibles sont internationalisés FR/EN.
- [x] Les tests frontend couvrent succès, erreur de vérification, ordonnance expirée et état vide.

## 3. Pilotage projet

| Champ | Valeur |
|---|---|
| Epic parent | EPIC-0010 — Dispensation en Pharmacie & Gestion des Prescriptions |
| User story parent | STORY-1004 |
| Sprint cible | À planifier |
| Priorité business | P1 |
| Story points | 2.5 |
| Profil recommandé | Intermédiaire |
| Effort estimé senior | 0.6j |
| Effort estimé intermédiaire | 0.8j |
| Effort estimé junior | 1.3j |
| Reviewer obligatoire | Lead |
| Risque fonctionnel | Moyen |
| Risque technique | Faible |
| Dépendances | STORY-1001 |

## 4. Action plan

- [x] Vérifier le contrat réel de `POST /api/public/pharmacy/prescriptions/verify`.
- [x] Créer la route Angular publique ou semi-publique du portail pharmacie.
- [x] Créer le service Angular pharmacie avec URLs relatives `/api/public/pharmacy/...`.
- [x] Créer les composants de vérification et détail ordonnance.
- [x] Ajouter traductions FR/EN, light/dark et états UI.
- [x] Ajouter les tests frontend.

## 4.1 Implémentation réalisée

- Ajout de la route Angular publique `/pharmacy/prescriptions`.
- Ajout de `PharmacyApiService` avec appel relatif `POST /api/public/pharmacy/prescriptions/verify`.
- Ajout de `PharmacyPrescriptionVerifyPageComponent` :
  - formulaire numéro ordonnance + PIN ;
  - affichage patient minimal, prescripteur, dates, statut et lignes de médicaments ;
  - design dense aligné sur le dashboard patient avec `app-container-wide`, `ui-card-subtle`, couleurs de `DESIGN.md` et rayons sobres.
- Ajout des traductions FR/EN dans `I18nService`.
- Raccordement de la carte pharmacien du dashboard vers le nouvel écran.
- Ajout de tests unitaires frontend dans `pharmacy-portal.spec.ts`.

## 5. Reste à faire

- Le scan QR code physique est à traiter en amélioration séparée si nécessaire.
- La délivrance partielle/totale et l'historique restent dans `STORY-1005`.

## 6. Statut final

Statut : DONE

## 6.1 Tests et vérifications

- `npm test -- --watch=false` : OK, 45 tests passés.
- `npm run build` : non validé en environnement courant, car Angular tente d'inliner Google Fonts et le réseau est bloqué (`connect EACCES`).
- `npm run build -- --optimization=false` : compilation générée, puis échec sur le budget initial non optimisé (1.55 MB > 1 MB), attendu avec l'optimisation désactivée.

## 7. Impact version / SemVer

| Champ | Valeur |
|---|---|
| Changement livrable | Oui |
| Type de bump | MINOR |
| Justification | Ajout d'un écran portail pharmacie aligné CDC. |
| Breaking change | Non |
| Migration DB | Non |
| Changement API | Non si STORY-1001 est terminé |
| Impact Angular | Oui |
| Impact Flutter | Non |
