# TICKET-VISIT-FORM-UX-IMPROVEMENTS — Amélioration UX du formulaire d'admission de visite patient

> Fichier obligatoire pour chaque ticket ou intervention IA.

## 1. Objectif

Améliorer l'expérience utilisateur (UX) lors de l'admission d'un patient :
1. Remplacer la saisie manuelle de l'UUID du praticien par une liste de sélection dynamique filtrée en fonction de l'orientation choisie (Médecins, Infirmiers, Pharmaciens).
2. Pré-remplir le champ de date/heure d'arrivée avec la date et l'heure courante locale par défaut tout en laissant la possibilité de la modifier.

## 2. Critères d'acceptation

- [x] Charger la liste des membres du personnel clinique via `StaffApiService`.
- [x] Remplacer le champ texte d'ID de praticien par un `<select>` affichant le nom complet (`displayName`).
- [x] Filtrer la liste des praticiens en fonction de l'orientation sélectionnée avec un fallback vers le personnel clinique général (Médecins, Infirmiers, Pharmaciens, Biologistes).
- [x] Initialiser `visitArrivalAt` avec la date et heure locale actuelle au format `YYYY-MM-DDTHH:mm` lors de l'ouverture du formulaire.
- [x] S'assurer que les tests unitaires et le build passent.

## 3. Pilotage projet

| Champ | Valeur |
|---|---|
| Epic parent | EPIC-0004 |
| User story parent | STORY-0401 |
| Sprint cible | SPRINT-0011 |
| Priorité business | P1 |
| Complexité | S |
| Story points | 2 |
| Profil recommandé | Intermédiaire / Senior |
| Effort estimé senior | 0.1j |
| Effort estimé intermédiaire | 0.2j |
| Effort estimé junior | 0.3j |
| Responsable | Antigravity |
| Reviewer obligatoire | Lead Developer |
| Risque fonctionnel | Faible |
| Risque technique | Faible |
| Dépendances | Aucun |
| Bloquants connus | Aucun |

## 4. Contexte analysé

- [x] `AGENTS.md` lu
- [x] `SKILL.md` lu
- [x] `README-IA.md` lu
- [x] `WORKFLOW-IA.md` lu
- [x] `PROJECT-TRACKING.md` lu
- [x] `CHANGELOG.md` lu
- [x] `review-checklist.md` lu
- [x] Code existant analysé : `web/src/app/patient/patient-detail.component.ts`
- [x] Tests existants analysés
- [x] Impacts sécurité analysés (aucune nouvelle API, utilise l'API existante `/api/staff`)
- [x] Impacts Angular analysés
- [x] Frontend Tailwind CSS v4 vérifié si applicable
- [x] Absence Angular Material vérifiée si applicable
- [x] Angular `proxy.conf.json` présent et référencé dans `angular.json` si applicable

## 5. Hypothèses

- `StaffApiService` renvoie tous les comptes du personnel. En les filtrant par rôle et par statut d'activation (`enabled`), nous pouvons fiablement proposer les praticiens de l'établissement sans nécessiter d'UUID.

## 6. Risques et impacts

| Risque | Impact | Mitigation |
|---|---|---|
| Aucun praticien dans la liste | Faible | Fallback automatique affichant tous les personnels cliniques actifs. |

## 7. Action plan

- [x] Ajouter l'injection de `StaffApiService` dans `PatientDetailComponent`.
- [x] Charger la liste du personnel au chargement et stocker les membres actifs dans un signal `staffList`.
- [x] Implémenter la méthode `getFilteredPractitioners()` pour filtrer les praticiens selon l'orientation.
- [x] Implémenter la méthode `onOrientationChange()` pour réinitialiser la sélection du praticien s'il ne fait plus partie de la liste filtrée.
- [x] Initialiser `visitArrivalAt` avec l'heure locale courante dans `openModal()`.
- [x] Modifier le template HTML pour remplacer le champ text d'UUID par un `<select>` branché sur `getFilteredPractitioners()`.
- [x] Mettre à jour les tests unitaires pour inclure le mock de `StaffApiService`.
- [x] Lancer les tests et le build.
- [x] Mettre à jour `CHANGELOG.md` et `PROJECT-TRACKING.md`.

## 8. Implémentation réalisée

- [x] Modification de `web/src/app/patient/patient-detail.component.ts` pour intégrer le select de praticien filtré et l'heure d'arrivée automatique.
- [x] Modification de `web/src/app/patient/patient-detail.component.spec.ts` pour mock-fournir `StaffApiService`.

## 9. Suivi d'exécution

| Date | Développeur | Temps passé | Avancement | Reste à faire | Blocage | Commentaire |
|---|---|---:|---:|---:|---|---|
| 2026-07-06 | Antigravity | 0.1j | 100% | Aucun | Aucun | Modification, tests et compilation de production validés. |

## 10. Tests et vérifications

### Commandes exécutées ou à exécuter

```bash
# Angular tests
npm run test
npm run build
```

### Résultats

- [x] Tests unitaires OK (79/79 au vert)
- [x] Build OK (Compilation de production réussie)

## 11. Documentation

- [x] Changelog mis à jour
- [x] Suivi projet mis à jour

## 12. Reste à faire

- Aucun

## 13. Statut final

Statut : DONE

## 14. Notes finales

Le formulaire d'admission est maintenant beaucoup plus intuitif et ergonomique pour les secrétaires/infirmiers d'accueil : le praticien est choisi dans une liste restreinte de praticiens cohérents avec le service orienté, et la date/heure d'arrivée est pré-remplie à la seconde près avec l'heure courante locale.

## 15. Impact version / SemVer

| Champ | Valeur |
|---|---|
| Changement livrable | Oui |
| Type de bump | PATCH |
| Justification | Amélioration UX de la modale d'admission (sélection de praticien et heure d'arrivée automatique). |
| Breaking change | Non |
| Migration DB | Non |
| Changement API | Non |
| Impact Angular | Oui |
| Impact Flutter | Non |
| Changelog requis | Oui |
| Release note requise | Non |

## 16. Impact thème / i18n / branding

- [x] Impact Angular UI analysé

## 17. Verification `.gitignore`

- [x] `.gitignore` présent à la racine
