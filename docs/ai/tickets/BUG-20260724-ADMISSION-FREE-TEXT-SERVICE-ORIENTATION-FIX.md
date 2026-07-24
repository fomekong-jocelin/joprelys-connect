# BUG-20260724-ADMISSION-FREE-TEXT-SERVICE-ORIENTATION-FIX — Correction des saisies libres pour les services et orientations lors de l'admission patient

## Statut
- **Statut** : IN_PROGRESS
- **Date de création** : 2026-07-24
- **Sprint** : SPRINT-0014
- **Priorité** : P1
- **Profil recommandé** : Senior Frontend Angular / Full-stack
- **Impact Version** : PATCH (v1.4.1)

## Description du problème
Dans le formulaire d'admission patient (`app-unified-admission`), les champs **Orientation** et **Service** (étape 2 Visite) étaient sous forme d'éléments `<input>` à saisie libre.
Cela entrainait des incohérences de données (fautes de frappe, dénominations sauvages) alors même que la configuration des services hospitaliers et la gestion spatiale/organisationnelle sont pleinement paramétrées dans l'application.

## Actions à effectuer
- [x] Créer la documentation fonctionnelle et technique dans `docs/features/admission-service-orientation/`.
- [x] Ajouter les clés de traduction FR et EN pour les orientations et les services d'admission.
- [x] Remplacer l'élément `<input id="admission-orientation">` par un menu déroulant `<select>` d'orientations normées.
- [x] Remplacer l'élément `<input id="admission-service">` par un menu déroulant `<select>` dynamique alimenté par `HospitalOrganizationApiService`.
- [x] Enrichir la liste des départements dans la modal de visite de `patient-detail.component.ts` via les services configurés.
- [x] Exécuter les tests Angular et vérifier la compilation.
- [x] Mettre à jour `PROJECT-TRACKING.md` et `CHANGELOG.md`.

## Fichiers impactés
- `docs/features/admission-service-orientation/FUNCTIONAL-SPEC.md`
- `docs/features/admission-service-orientation/TECHNICAL-DESIGN.md`
- `web/src/assets/i18n/features/admission/fr.json`
- `web/src/assets/i18n/features/admission/en.json`
- `web/src/app/admission/unified-admission.component.ts`
- `web/src/app/admission/unified-admission.component.html`
- `web/src/app/patient/patient-detail.component.ts`
- `docs/ai/tickets/BUG-20260724-ADMISSION-FREE-TEXT-SERVICE-ORIENTATION-FIX.md`
- `docs/ai/PROJECT-TRACKING.md`
- `docs/ai/CHANGELOG.md`
