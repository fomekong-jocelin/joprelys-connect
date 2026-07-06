# TICKET-CLINIC-STAFF-DEPARTMENT-FILTER — Menu déroulant des services et filtrage des praticiens lors de l'admission

## 1. Objectif
Remplacer le champ texte libre "Service clinique" par un menu déroulant dans la fiche collaborateur (StaffManagement/Profile) et lors de l'admission d'une visite patient (PatientDetail). Lorsque le service clinique est sélectionné dans le formulaire d'admission de la visite, filtrer la liste des praticiens disponibles pour afficher uniquement les professionnels (Médecins/Infirmiers) rattachés à ce service/département.

## 2. Critères d'acceptation
- [x] Création d'une liste de services cliniques par défaut (Médecine générale, Pédiatrie, Gynécologie, Urgences, Pharmacie, Laboratoire, Cardiologie).
- [x] Dans le profil du collaborateur et la gestion du personnel (IHM), le champ "Département / Service" devient un menu déroulant basé sur cette liste, avec une option "Autre" permettant de saisir une valeur libre.
- [x] Dans le formulaire d'ouverture de visite/admission du patient (dans `PatientDetailComponent`), le champ "Service clinique" devient également un menu déroulant qui fusionne la liste par défaut et tous les services actifs des collaborateurs enregistrés. Une option "Autre" permet également de saisir une valeur libre.
- [x] Lors de la sélection d'un service clinique dans le formulaire d'admission, la liste "Praticien responsable" est filtrée dynamiquement pour ne proposer que les collaborateurs rattachés à ce service. Si aucun n'est rattaché, le comportement bascule sur le filtre d'orientation générale.
- [x] Tests de compilation et d'exécution frontend et backend au vert.

## 3. Pilotage projet

| Champ | Valeur |
|---|---|
| Epic parent | EPIC-0015 : Profils enrichis et ressources graphiques |
| User story parent | STORY-2006 |
| Sprint cible | SPRINT-0011 |
| Priorité business | P1 |
| Complexité | M |
| Story points | 3 |
| Profil recommandé | Intermédiaire |
| Effort estimé senior | 2h |
| Effort estimé intermédiaire | 4h |
| Effort estimé junior | 8h |
| Responsable | Antigravity |
| Reviewer obligatoire | User |
| Risque fonctionnel | Faible |
| Risque technique | Faible |
| Dépendances | TICKET-CLINIC-STAFF-ENRICHED-PROFILES |
| Bloquants connus | Aucun |

## 4. Contexte analysé
- [x] `AGENTS.md` lu
- [x] `SKILL.md` lu
- [x] `PROJECT-TRACKING.md` lu
- [x] `CHANGELOG.md` lu
- [x] Fichiers `PatientDetailComponent`, `StaffManagementComponent`, `ProfileComponent` analysés

## 5. Hypothèses
- La liste des services cliniques par défaut couvre les besoins courants de la clinique.
- Les départements saisis manuellement via l'option "Autre" sont persistés normalement sous forme de chaînes de caractères dans la colonne `department` existante.

## 6. Risques et impacts
- Aucun risque de régression sur les visites existantes.

## 7. Action plan
- [x] Créer les fichiers de spécifications initiales si requis.
- [x] Modifier `StaffManagementComponent` et son template pour utiliser un menu déroulant pour le service avec option de fallback "Autre".
- [x] Modifier `ProfileComponent` et son template pour appliquer la même logique.
- [x] Modifier `PatientDetailComponent` pour :
  - Générer dynamiquement la liste des départements/services (standard + collaborateurs).
  - Modifier le template HTML pour utiliser un menu déroulant.
  - Mettre à jour `getFilteredPractitioners()` pour filtrer par service en priorité.
- [x] Valider la compilation Angular.
- [x] Mettre à jour `PROJECT-TRACKING.md` et `CHANGELOG.md`.

## 8. Implémentation réalisée
- **Profil & Gestion Personnel** : Dans `ProfileComponent` et `StaffManagementComponent`, remplacement du champ de texte `department` par un `<select>` listant les services par défaut. Si l'utilisateur choisit l'option "Autre", un champ texte apparaît pour saisir manuellement le service.
- **Admission Patient** : Dans `PatientDetailComponent`, remplacement de l'input texte de "Service clinique" par un `<select>` qui fusionne les services par défaut et tous les départements uniques saisis sur les fiches des collaborateurs. Une option "Autre" permet de saisir un service libre.
- **Filtrage Praticiens** : Mise à jour de la fonction `getFilteredPractitioners()` de `PatientDetailComponent` pour filtrer les praticiens assignés à la visite en ciblant d'abord ceux qui appartiennent au service clinique sélectionné. Si aucun médecin n'est affecté à ce service, un repli automatique s'opère vers le filtrage d'orientation ou la liste globale des praticiens qualifiés (Médecin, Infirmier, Pharmacien, Biologiste).
- **Validation** : Les tests unitaires backend passent tous (240/240) et le build Angular de production compile sans erreurs.

## 9. Suivi d'exécution
| Date | Développeur | Temps passé | Avancement | Reste à faire | Blocage | Commentaire |
|---|---|---:|---:|---:|---|---|
| 2026-07-06 | Antigravity | 0.25j | 100% | Aucun | Aucun | Conception et implémentation complètes achevées |

## 10. Tests et vérifications
- [x] Compilation frontend `npm run build` réussie.
- [x] Tests unitaires backend `./mvnw test` réussis (240 tests).

## 11. Documentation
- [x] Documentation fonctionnelle mise à jour : `docs/features/clinic-staff-department-filter/FUNCTIONAL-SPEC.md`
- [x] Documentation technique mise à jour : `docs/features/clinic-staff-department-filter/TECHNICAL-DESIGN.md`

## 12. Reste à faire
Aucun.

## 13. Statut final
Statut : DONE

## 14. Notes finales
L'implémentation répond précisément au besoin exprimé en guidant le choix du service et en automatisant l'association médecin-service au moment de l'admission.

## 15. Impact version / SemVer
| Champ | Valeur |
|---|---|
| Changement livrable | Oui |
| Type de bump | PATCH |
| Justification | Amélioration ergonomique du choix du service et filtrage des praticiens |
