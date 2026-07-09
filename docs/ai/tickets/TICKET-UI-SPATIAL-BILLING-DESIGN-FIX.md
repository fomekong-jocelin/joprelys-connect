# TICKET-UI-SPATIAL-BILLING-DESIGN-FIX — Alignement design system des pages Gestion Spatiale et Facturation (emojis, ombres, radius, i18n, layout)

## 1. Objectif

Les pages Gestion Spatiale (`/clinic/spatial`) et Facturation (`/clinic/billing`) ont été conçues sans intégrer le composant de mise en page global `AppShellComponent`. De ce fait, elles ne possèdent ni en-tête (topbar), ni menu latéral (sidebar), ni fil d'Ariane (breadcrumb), ni pied de page (footer), et ne s'adaptent pas au thème (clair/sombre) sélectionné. L'objectif est d'intégrer `<app-shell>` et `<app-page-header>` sur ces deux pages pour assurer la cohérence ergonomique et visuelle.

## 2. Critères d'acceptation

- [x] Importer `AppShellComponent` et `PageHeaderComponent` dans `SpatialManagementPageComponent`.
- [x] Wrapper le template de `SpatialManagementPageComponent` avec `<app-shell>` et utiliser `<app-page-header>` pour le titre, le sous-titre et le sélecteur de service.
- [x] Importer `AppShellComponent` et `PageHeaderComponent` dans `BillingManagementPageComponent`.
- [x] Wrapper le template de `BillingManagementPageComponent` avec `<app-shell>` et utiliser `<app-page-header>` pour le titre et le sous-titre.
- [x] S'assurer que le contenu principal est encapsulé dans `.app-container` avec les espacements adéquats.
- [x] Valider la compilation d'Angular (`npm run build`).

## 3. Pilotage projet

| Champ | Valeur |
|---|---|
| Epic parent | EPIC-0014 — Alignement modules 4 à 12 du CDC |
| Sprint cible | SPRINT-0012 |
| Priorité business | P1 |
| Complexité | S |
| Story points | 2 |
| Profil recommandé | Intermédiaire |
| Effort estimé senior | 0.15j |
| Effort estimé intermédiaire | 0.25j |
| Effort estimé junior | 0.5j |
| Responsable | Antigravity |
| Reviewer obligatoire | Lead Developer |
| Risque fonctionnel | Faible |
| Risque technique | Faible |

## 4. Contexte analysé

- [x] `AGENTS.md` lu
- [x] `SKILL.md` lu
- [x] `README-IA.md` lu
- [x] `WORKFLOW-IA.md` lu
- [x] `PROJECT-TRACKING.md` lu
- [x] `CHANGELOG.md` lu
- [x] `review-checklist.md` lu
- [x] Code existant analysé (`spatial-management-page.component.ts` et `billing-management-page.component.ts`)
- [x] Captures d'écran fournies par l'utilisateur analysées

## 5. Action plan

- [x] Analyser la structure des pages `duplicates-page.component.ts` pour référence d'intégration d'app-shell.
- [x] Modifier `SpatialManagementPageComponent` pour ajouter les imports et restructurer le template.
- [x] Modifier `BillingManagementPageComponent` pour ajouter les imports et restructurer le template.
- [x] Exécuter le build Angular pour s'assurer qu'il n'y a pas d'erreur de compilation.
- [x] Mettre à jour `CHANGELOG.md`.
- [x] Mettre à jour `PROJECT-TRACKING.md`.

## 6. Implémentation réalisée

- [x] **Spatial**:
  - Ajout des imports `AppShellComponent` et `PageHeaderComponent`.
  - Ajout au tableau `imports` de la directive `@Component`.
  - Modification du template : enveloppement dans `<app-shell>` et déportation du sélecteur de service dans l'en-tête de page `<app-page-header>`.
  - Utilisation du conteneur `.app-container` pour la grille et le reste de la page.
  - Changement de `ui-card-subtle` à `ui-card` pour la grille des chambres pour plus d'harmonie.
- [x] **Billing**:
  - Ajout des imports `AppShellComponent` et `PageHeaderComponent`.
  - Ajout au tableau `imports` de la directive `@Component`.
  - Modification du template : enveloppement dans `<app-shell>`, utilisation de `<app-page-header>` pour le titre/sous-titre et encapsulation du contenu dans `.app-container`.

## 7. Tests et vérifications

### Commandes exécutées ou à exécuter

```bash
# Angular
npm run build
```

## 8. Statut final

Statut : **DONE**

Dernière mise à jour : 2026-07-08

## 9. Impact version / SemVer

| Champ | Valeur |
|---|---|
| Changement livrable | Oui |
| Type de bump | PATCH |
| Justification | Alignement du layout et support du thème global pour les pages Gestion Spatiale et Facturation. |
| Breaking change | Non |
| Impact Angular | Oui |
| Changelog requis | Oui |
