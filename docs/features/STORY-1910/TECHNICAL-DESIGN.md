# STORY-1910 — Conception technique

## Alignement Frontend : Portails patient, pro, labo, pharmacie et vérification publique conformes CDC

---

## 1. Stack

- Angular 22 (standalone components, signals)
- TypeScript 6
- Tailwind CSS v4 CSS-first (`@import "tailwindcss"`, `@theme`)
- RxJS
- Vitest (tests unitaires via `@angular/build:unit-test`)

## 2. Architecture cible

### 2.1 Structure des fichiers

```text
web/src/
  app/
    core/
      config/
        app-brand.config.ts
      i18n/
        i18n.service.ts          # charge assets/i18n/{fr|en}.json
        i18n-loader.service.ts   # utilitaire de chargement JSON
      theme/
        theme.service.ts
      title/
        app-title.service.ts     # mise à jour du <title>
    shared/
      layout/
        app-shell.component.ts   # refactoré, < 500 lignes
        app-shell-nav.component.ts # navigation extraite
      ui/
        button.component.ts
        input.component.ts
        card.component.ts
        page-header.component.ts
        empty-state.component.ts
        status-badge.component.ts
    patient/
      portal/
        pages/
          patient-profile-page.component.ts
          patient-documents-page.component.ts
          patient-qr-code-page.component.ts
          patient-privacy-page.component.ts
          (pages existantes harmonisées)
    clinic/
      documents/
        clinic-documents-page.component.ts
      history/
        patient-history-page.component.ts
      lab/
        lab-dashboard-page.component.ts
        lab-order-detail-page.component.ts
        lab-result-validation.component.ts
        lab-pdf-send.component.ts
    pharmacy/
      pharmacy-prescription-detail-page.component.ts
    consultation/
      components/
        prescription-form.component.ts
        lab-order-form.component.ts
      consultation.component.ts  # refactoré
    verify/
      document-search.component.ts
      verification.component.ts
      access-request.component.ts
  assets/
    i18n/
      fr.json
      en.json
```

### 2.2 Services et responsabilités

| Service | Responsabilité |
|---|---|
| `I18nService` | Stocke la locale courante, charge le fichier JSON de traduction, expose `t(key)` et `setLocale(lang)`. |
| `AppTitleService` | Écoute les événements de navigation et met à jour `<title>` avec `APP_BRAND_CONFIG.appName`. |
| `PatientPortalService` | Déjà existant ; fournit les données du portail patient. |
| `ConsultationApiService` | Déjà existant ; API consultation/prescription/document. |
| `LabOrderApiService` | Déjà existant ; API labo. |
| `PharmacyApiService` | Déjà existant ; API pharmacie. |

## 3. Internationalisation

### 3.1 Principe

Les dictionnaires FR et EN actuellement inline dans `i18n.service.ts` sont déplacés vers :

```text
web/src/assets/i18n/fr.json
web/src/assets/i18n/en.json
```

`I18nService` charge le fichier JSON correspondant à la locale via `HttpClient`. Le service reste synchrone pour `t(key)` après chargement initial (chargement dans `APP_INITIALIZER`).

### 3.2 Format des clés

Les clés existantes sont conservées pour éviter les régressions :

```text
common.save
common.cancel
patient.dashboard.title
pharmacy.title
lab.title
verify.search.title
...
```

## 4. Titre dynamique

Un `AppTitleService` est créé dans `core/title/`.

- Écoute `Router.events` (NavigationEnd).
- Lit la route la plus profonde et son éventuelle donnée `title`.
- Met à jour `document.title` sous la forme : `"Titre de page | Joprelys Connect"`.
- Par défaut, le titre est `APP_BRAND_CONFIG.appName`.

## 5. Composants UI partagés

Les composants existants de `shared/ui` sont réutilisés :

- `<app-ui-button>`
- `<app-ui-input>`
- `<app-ui-card>`
- `<app-page-header>`
- `<app-empty-state>`
- `<app-status-badge>`

Les nouveaux écrans doivent privilégier ces composants au lieu de styles natifs répétés.

## 6. Routes

### 6.1 Nouvelles routes patient

```ts
{ path: 'patient/profile', loadComponent: () => import('./patient/portal/pages/patient-profile-page.component').then(m => m.PatientProfilePageComponent), canActivate: [roleGuard], data: { expectedRoles: ['PATIENT'] } },
{ path: 'patient/documents', loadComponent: () => import('./patient/portal/pages/patient-documents-page.component').then(m => m.PatientDocumentsPageComponent), canActivate: [roleGuard], data: { expectedRoles: ['PATIENT'] } },
{ path: 'patient/qr-code', loadComponent: () => import('./patient/portal/pages/patient-qr-code-page.component').then(m => m.PatientQrCodePageComponent), canActivate: [roleGuard], data: { expectedRoles: ['PATIENT'] } },
{ path: 'patient/privacy', loadComponent: () => import('./patient/portal/pages/patient-privacy-page.component').then(m => m.PatientPrivacyPageComponent), canActivate: [roleGuard], data: { expectedRoles: ['PATIENT'] } },
```

### 6.2 Nouvelles routes pro/clinic

```ts
{ path: 'clinic/documents', loadComponent: () => import('./clinic/documents/clinic-documents-page.component').then(m => m.ClinicDocumentsPageComponent), canActivate: [roleGuard], data: { expectedRoles: ['MEDECIN', 'ADMIN_CLINIQUE'] } },
{ path: 'patients/:id/history', loadComponent: () => import('./clinic/history/patient-history-page.component').then(m => m.PatientHistoryPageComponent), canActivate: [roleGuard], data: { expectedRoles: ['MEDECIN', 'INFIRMIER', 'ADMIN_CLINIQUE'] } },
```

### 6.3 Nouvelles routes labo

```ts
{ path: 'lab/dashboard', loadComponent: () => import('./clinic/lab/lab-dashboard-page.component').then(m => m.LabDashboardPageComponent), canActivate: [roleGuard], data: { expectedRoles: ['BIOLOGISTE', 'ADMIN_JOPRELYS'] } },
{ path: 'lab/orders/:id', loadComponent: () => import('./clinic/lab/lab-order-detail-page.component').then(m => m.LabOrderDetailPageComponent), canActivate: [roleGuard], data: { expectedRoles: ['BIOLOGISTE', 'ADMIN_JOPRELYS'] } },
```

### 6.4 Routes pharmacie

La page `/pharmacy/prescriptions` est enrichie ; une route `/pharmacy/prescriptions/:id` peut être ajoutée ultérieurement.

## 7. Sécurité

- `roleGuard` est conservé sur toutes les routes protégées.
- Aucune URL backend hardcodée : le proxy de développement reste utilisé.
- Aucune donnée sensible dans `localStorage` (hors préférence locale de sidebar et thème déjà existantes).

## 8. Tests

### 8.1 Tests unitaires

- Un test par nouvelle page : vérification du rendu, des états loading/empty/error.
- Tests du `I18nService` refactoré.
- Tests du `AppTitleService`.

### 8.2 Vérifications de build

```bash
cd web
npm run test
npm run build
```

## 9. Impact version

- Bump **MINOR** (`0.10.0`) : ajout de pages et de fonctionnalités utilisateur rétrocompatibles.

## 10. Risques et mitigation

| Risque | Mitigation |
|---|---|
| Régression sur les traductions existantes | Conserver les clés existantes ; vérifier chaque écran après migration. |
| Composants > 500 lignes | Refactor progressif ; extraction de sous-composants. |
| Build allongé par les nouvelles routes lazy-loaded | Aucun impact significatif attendu. |
