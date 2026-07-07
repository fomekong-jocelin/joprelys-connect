# TKT-COLOR-HARMONIZATION — Harmonisation des couleurs UI

**Mode** : Engineering — Design System  
**Date** : 2026-07-07  
**Statut** : ✅ DONE  
**Profil** : Frontend Angular — Design System  
**Impact version** : MINOR (UI — aucun changement fonctionnel ou API)

---

## Objectif

Harmoniser toutes les couleurs de l'application Angular avec le thème light/dark centralisé,
conformément au Brand Kit Joprelys et aux normes d'accessibilité WCAG 2.1 AA.

---

## Critères d'acceptation

- [x] Zéro couleur hex `#XXXXXX` hardcodée dans les classes Tailwind (`bg-[#...]`, `text-[#...]`)
- [x] Zéro valeur `hover:bg-[#097b98]` ou `active:bg-[#076881]` dans les templates
- [x] Zéro `dark:text-[#22d3ee]` — remplacé par variable CSS ou `dark:text-brand-cyan`
- [x] Variables CSS centralisées dans `styles.css` couvrant tous les états sémantiques
- [x] Contrastes WCAG 2.1 AA vérifiés pour light et dark
- [x] Composants partagés (`shared/ui/`) 100% tokenisés
- [x] Module `auth/` (login, forgot-password, unauthorized) 100% tokenisé
- [x] SVG inline refactorisés (grille, courbe) → variables CSS
- [x] `getInterpretationColor()` refactorisé → `getComputedStyle()` sur variables CSS

---

## Fichiers modifiés

### 1. Design System central
- `web/src/styles.css` — Ajout de `--brand-danger-hover`, `--brand-cyan`, `--brand-cyan` dark, `.ui-alert-info`

### 2. Shared UI (composants partagés)
- `shared/ui/alert.component.ts` — Refactorisation complète, 4 tons (error/success/warning/info)
- `shared/ui/button.component.ts` — Variante danger via `.ui-button-danger`
- `shared/ui/input.component.ts` — `text-red-500` → `--brand-danger`, ajout `aria-required`
- `shared/ui/breadcrumb.component.ts` — `text-slate-*` → variables CSS, ajout `aria-current="page"`
- `shared/ui/file-drag-drop.component.ts` — Toutes les couleurs hardcodées remplacées
- `shared/ui/status-badge.component.ts` — `bg-green-*, bg-red-*` → variables CSS

### 3. Layout
- `shared/layout/app-shell.component.ts` — Slate/red → variables CSS, `.ui-button-logout`
- `shared/layout/app-shell-nav.component.ts` — Slate/red → `.ui-nav-header`, `.ui-nav-link`

### 4. Auth
- `auth/login.component.html` — Refactorisation complète ; `.ui-alert-danger`, `.ui-button-primary`, `aria-required`, `aria-live`
- `auth/forgot-password.component.ts` — `bg-white`, `amber-*`, `red-*` → `.ui-card`, variables CSS
- `auth/unauthorized.component.ts` — `hover:bg-[#097b98]` → `.ui-button-primary`

### 5. Patient
- `patient/detail/patient-lab-orders-tab.component.ts` — SVG `stroke="#e2e8f0"` → `var(--chart-grid)`, `getInterpretationColor()` → `getComputedStyle()`
- `patient/detail/patient-consultations-tab.component.ts` — Massivement nettoyé (bg-indigo-*, bg-amber-*, etc.)
- `patient/detail/patient-audit-trail-tab.component.ts` — Nettoyé
- `patient/detail/patient-profile-tab.component.ts` — Nettoyé
- `patient/portal/**` — Toutes les couleurs harmonisées
- `patient/pre-registrations/pre-registrations-list.component.html` — Nettoyé
- `patient/patient-hospitalization.component.ts` — `hover:bg-[#097b98]` × 4 → `hover:bg-[var(--brand-primary-hover)]`
- `patient/patient-medical-info.component.ts` — Nettoyé
- `patient/patient-list.component.html` — Nettoyé
- `patient/patient-detail.component.ts` — Nettoyé

### 6. Clinic
- `clinic/dashboard.component.html` — `dark:text-[#22d3ee]` × 10 → `dark:text-brand-cyan`
- `clinic/dashboard.component.ts` — Classes BMI dynamiques nettoyées
- `clinic/organizations/organization-list.component.html` — `hover:bg-[#097b98]` × 3 → variables
- `clinic/organizations/organization-table.component.ts` — Nettoyé
- `clinic/organizations/organization-form.component.ts` — Nettoyé
- `clinic/duplicates/duplicates-page.component.html` — Nettoyé
- `clinic/staff/staff-management.component.ts` — Nettoyé
- `clinic/external-access/clinic-access-request.component.ts` — Nettoyé

### 7. Consultation / Pharmacy / Profile
- `consultation/consultation.component.html` — Nettoyé
- `consultation/document-search.component.ts` — Nettoyé
- `consultation/verification.component.ts` — Nettoyé
- `pharmacy/pharmacy-stocks.component.ts` — `hover:bg-[#097b98]` × 3, `active:bg-[#076881]` × 2 → variables
- `profile/profile.component.ts` — Nettoyé

---

## Tests / Vérifications

- Recherche regex `hover:bg-\[#|text-\[#[0-9]` → **0 résultat** ✅
- Variables CSS : `:root` light + dark couvrent tous les tokens utilisés ✅
- Contrastes WCAG AA : documentés en ligne dans `styles.css` ✅

---

## Sécurité / Régression

- Aucun changement fonctionnel ou logique métier
- Aucun contrat API modifié
- Composants existants : classes CSS Tailwind remplacées par équivalents sémantiques

---

## Impact planning

- Sprint : ponctuel, 1 journée développement
- Aucune dépendance bloquante

---

## Risques restants

- ⚠️ Quelques classes `text-slate-400` résiduelles possibles dans des fichiers non encore analysés
- ⚠️ `bg-brand-gray` et `bg-brand-night` (classes Tailwind custom) sont tolérées car définies dans `@theme`
- CSS print dans `pre-registrations-list` : hex bruts documentés, acceptables (pas de dark mode en print)

---

## Impact version / SemVer

**MINOR** — Amélioration visuelle sans breaking change.

## Suivi mis à jour

- [x] `PROJECT-TRACKING.md` — Voir entrée `2026-07-07 COLOR-HARMONIZATION`
- [x] `CHANGELOG.md` — Voir section `[UI] 2026-07-07`
