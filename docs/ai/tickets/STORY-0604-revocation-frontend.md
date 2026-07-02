# STORY-0604 — Écran de révocation côté frontend

## 1. Objectif

Permettre aux utilisateurs habilités (Médecin, Administrateur Clinique) de révoquer ou d'annuler un document médical directement depuis le portail clinicien (frontend Angular), via un bouton d'action placé dans l'historique des consultations du patient.

## 2. Critères d'acceptation

- [x] Un médecin ou un administrateur clinique connecté voit un bouton "Révoquer / Annuler" à côté du bouton "Télécharger PDF" dans l'historique médical de `PatientDetailComponent` (uniquement pour les documents ayant le statut `VALID`).
- [x] Cliquer sur ce bouton ouvre une modale de confirmation contenant :
  - Un sélecteur d'action : "Révoquer" (document invalide mais conservé historiquement) ou "Annuler" (document considéré comme n'ayant jamais existé).
  - Un champ de saisie obligatoire pour le motif de révocation/annulation (minimum 5 caractères, maximum 500 caractères).
- [x] La modale affiche des boutons "Confirmer" (désactivé si le motif est trop court ou vide) et "Annuler".
- [x] En cas de succès :
  - La modale se ferme.
  - La ligne de consultation affiche un badge visible `RÉVOQUÉ` ou `ANNULÉ` à la place du bouton de révocation.
  - Un message de notification vert confirme la réussite de l'opération.
- [x] En cas d'erreur (ex: conflit, non autorisé) :
  - Un message d'erreur clair s'affiche dans la modale.
- [x] L'historique médical affiche les badges de statut pour tous les documents :
  - Vert / Sans badge ou `VALID` : Document actif.
  - Orange / `RÉVOQUÉ` : Si le document a été révoqué.
  - Rouge / `ANNULÉ` : Si le document a été annulé.

## 3. Pilotage projet

| Champ | Valeur |
|---|---|
| Epic parent | EPIC-0006 (Génération PDF & Vérification par QR Code) |
| User story parent | STORY-0604 |
| Sprint cible | SPRINT-0003 |
| Priorité business | P1 |
| Complexité | S |
| Story points | 2 |
| Profil recommandé | Intermédiaire |
| Effort estimé senior | 0.3j |
| Effort estimé intermédiaire | 0.5j |
| Effort estimé junior | 0.8j |
| Responsable | Antigravity |
| Reviewer obligatoire | Lead Developer |
| Risque fonctionnel | Moyen |
| Risque technique | Faible |
| Dépendances | STORY-0603 (Backend) |

## 4. Contexte analysé

- [x] `AGENTS.md` lu
- [x] `SKILL.md` lu
- [x] `PROJECT-MANAGER-SKILL.md` lu si nécessaire
- [x] `README-IA.md` lu
- [x] `WORKFLOW-IA.md` lu
- [x] `PROJECT-TRACKING.md` lu
- [x] `CHANGELOG.md` lu
- [x] `review-checklist.md` lu
- [x] Code existant analysé (`PatientDetailComponent`, `ConsultationApiService`, `DocumentController`)
- [x] Frontend Tailwind CSS vérifié : Oui
- [x] Absence Angular Material vérifiée : Oui

## 5. Hypothèses

- L'API backend retourne le statut actuel et l'ID du document via le DTO `ConsultationResponse` ou un endpoint dédié pour éviter les appels multiples. Pour simplifier, nous avons enrichi `ConsultationResponse` avec `documentId` et `documentStatus`.

## 6. Risques et impacts

| Risque | Impact | Mitigation |
|---|---|---|
| Révocation accidentelle | Moyen | Double confirmation via modale + motif obligatoire. |

## 7. Action plan

### Phase 1 : Backend Spring Boot
- [x] Mettre à jour `ConsultationResponse.java` pour inclure `documentId` (UUID) et `documentStatus` (String).
- [x] Adapter `ConsultationHistoryController` ou `ConsultationService` pour charger le `MedicalDocumentEntity` associé à la visite et renseigner ces champs.
- [x] Mettre à jour les tests unitaires et d'intégration backend si impactés.

### Phase 2 : Frontend Angular
- [x] Mettre à jour le modèle TypeScript `Consultation` dans `consultation.models.ts` pour inclure `documentId` et `documentStatus`.
- [x] Ajouter les méthodes `revokeDocument(documentId, reason)` et `cancelDocument(documentId, reason)` dans `ConsultationApiService`.
- [x] Concevoir l'interface utilisateur dans `PatientDetailComponent` :
  - [x] Affichage du badge de statut (`VALID`, `REVOKED`, `CANCELLED`).
  - [x] Bouton "Révoquer / Annuler" réservé aux rôles `MEDECIN` et `ADMIN_CLINIQUE`.
  - [x] Intégration de la modale de saisie de motif.
- [x] Ajouter les traductions nécessaires dans `I18nService`.
- [x] Écrire les tests unitaires frontend dans `patient-detail.component.spec.ts`.

## 8. Implémentation réalisée

- Backend : Injection de `MedicalDocumentRepository` dans `ConsultationController` et `ConsultationHistoryController` pour alimenter le DTO `ConsultationResponse` avec l'UUID du document et son statut de validité.
- Frontend :
  - Mise à jour du modèle `Consultation` TypeScript.
  - Ajout des méthodes patch `revokeDocument` et `cancelDocument` dans `ConsultationApiService`.
  - Intégration dans `PatientDetailComponent` d'un bouton de révocation visible uniquement pour `MEDECIN` / `ADMIN_CLINIQUE` lorsque le document est au statut `VALID`.
  - Ajout d'une modale responsive avec radio-boutons pour sélectionner le type (Révocation / Annulation), zone de saisie pour le motif d'audit et validation d'au moins 5 caractères.
  - Gestion réactive avec mise à jour immédiate du statut dans la liste locale Angular (évite les rafraîchissements inutiles).
  - Ajout de 25 traductions FR/EN complètes dans `I18nService`.
  - Écriture de 4 tests unitaires dans `patient-detail.component.spec.ts` vérifiant l'ouverture de la modale et les retours d'API.

## 9. Suivi d'exécution

| Date | Développeur | Temps passé | Avancement | Reste à faire | Blocage | Commentaire |
|---|---|---:|---:|---:|---|---|
| 2026-07-02 | Antigravity | 0.05j | 10% | Cadrage initial | Aucun | Cadrage et ticket créé |
| 2026-07-02 | Antigravity | 0.45j | 100% | Aucun | Aucun | Tout est implémenté, build success et tous tests OK |

## 10. Tests et vérifications

### Commandes exécutées ou à exécuter

```bash
# Backend
mvn clean test-compile
mvn -Dtest=ConsultationControllerTest test

# Angular
npm run build -- --configuration development
npm run test -- --watch=false
```

### Résultats

- [x] Tests unitaires OK (25/25 tests unitaires frontend réussis)
- [x] Tests intégration OK (11/11 tests backend ConsultationControllerTest réussis)
- [x] Build OK (Angular dev build réussi)
- [x] Analyse statique OK

## 11. Documentation

- [x] Mettre à jour `docs/features/pdf/FUNCTIONAL-SPEC.md`
- [x] Mettre à jour `docs/features/pdf/TECHNICAL-DESIGN.md`
- [x] Mettre à jour `CHANGELOG.md`
- [x] Mettre à jour `PROJECT-TRACKING.md`

## 12. Reste à faire

- Aucun.

## 13. Statut final

Statut : DONE

## 14. Impact version / SemVer

| Champ | Valeur |
|---|---|
| Changement livrable | Oui |
| Type de bump | PATCH |
| Justification | Ajout de l'interface frontend de révocation et enrichissement DTO backend |
| Breaking change | Non |
| Migration DB | Non |
| Changement API | Oui (champs additionnels dans le DTO) |
| Impact Angular | Oui |
| Impact Flutter | Non |
| Changelog requis | Oui |
| Release note requise | Non |

## 15. Impact thème / i18n / branding

- [x] Impact Angular UI analysé
- [x] Thème centralisé vérifié
- [x] Thèmes light/dark vérifiés
- [x] Textes `fr` / `en` prévus
- [x] Composants/widgets réutilisables prévus

## Documentation First

- [x] Documentation fonctionnelle initiale mise à jour : `docs/features/pdf/FUNCTIONAL-SPEC.md`
- [x] Documentation technique initiale mise à jour : `docs/features/pdf/TECHNICAL-DESIGN.md`
