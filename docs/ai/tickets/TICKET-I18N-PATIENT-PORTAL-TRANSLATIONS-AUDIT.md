# TICKET-I18N-PATIENT-PORTAL-TRANSLATIONS-AUDIT

## Titre
Audit et correction des traductions manquantes du portail patient

## Type
Engineering — Front-end

## Contexte
L'utilisateur rapporte que de nombreux textes du portail patient ne sont pas traduits, avec l'exemple `patients.medicalInfo.allergies.add`. Cette clé existe déjà dans les dictionnaires FR/EN ; le problème vient de textes codés en dur dans les templates/composants, de clés i18n manquantes et de statuts/enum affichés bruts.

## Objectif
Auditer et corriger les traductions manquantes dans le périmètre du portail patient Angular.

## Périmètre
- `web/src/app/patient/portal/components/*`
- `web/src/app/patient/portal/pages/*`
- `web/src/app/patient/portal/patient-dashboard.component.ts`
- `web/src/app/patient/portal/patient-login.component.ts`
- `web/src/app/patient/patient-medical-info.component.ts`
- `web/src/assets/i18n/fr.json`
- `web/src/assets/i18n/en.json`

## Critères d'acceptation
- [x] Tous les textes visibles utilisateur codés en dur sont remplacés par des clés i18n.
- [x] Les clés i18n utilisées existent dans `fr.json` et `en.json`.
- [x] Les statuts/enum affichés bruts sont traduits via des clés dynamiques.
- [x] Les placeholders d'exemple des formulaires sont internationalisés.
- [x] `npm run test` passe.
- [x] `npm run build` passe.

## Fichiers modifiés
- `web/src/assets/i18n/fr.json`
- `web/src/assets/i18n/en.json`
- `web/src/app/patient/portal/patient-dashboard.component.ts`
- `web/src/app/patient/portal/patient-login.component.ts`
- `web/src/app/patient/patient-medical-info.component.ts`
- `web/src/app/patient/portal/components/patient-requests-list.component.ts`
- `web/src/app/patient/portal/components/patient-consents-list.component.ts`
- `web/src/app/patient/portal/components/patient-audit-list.component.ts`
- `web/src/app/patient/portal/components/patient-notifications.component.ts`
- `web/src/app/patient/portal/components/patient-profile-card.component.ts`
- `web/src/app/patient/portal/pages/patient-summary-page.component.ts`
- `web/src/app/patient/portal/pages/patient-results-page.component.ts`
- `web/src/app/patient/portal/patient-portal.spec.ts` (mise à jour d'un test obsolète suite à la traduction du statut d'audit)

## Clés i18n ajoutées
Environ 136 clés ajoutées dans `fr.json` et `en.json`.

## Tests
- `npm run test` : 79 tests passés, 0 échec.
- `npm run build` : build production réussi.

## Risques / Suivi
- Le fichier `patient-portal.spec.ts` a dû être mis à jour car il attendait le statut brut `SUCCESS` ; il vérifie maintenant `Succès` (traduction FR).

## Estimation
0.5j Senior

## Statut
DONE
