# FEAT-20261018-UI-DIALOG-AND-NOTIFICATION-SYSTEM — Système unifié de boîtes de dialogue et notifications (suppression des fenêtres JavaScript natives)

## Metadata
- **ID** : FEAT-20261018-UI-DIALOG-AND-NOTIFICATION-SYSTEM
- **Epic** : UI / DESIGN_SYSTEM / SHARED_COMPONENTS
- **Type** : Feature / Engineering / Refactoring UI
- **Statut** : DONE
- **Priorité** : P1
- **Auteur** : Antigravity
- **Date** : 2026-10-18
- **Stack** : Angular 20+ (Standalone, Signals) / Tailwind CSS v4 / TypeScript / Vitest

## Objectif
Éliminer totalement les boîtes de dialogue natives du navigateur (`window.confirm()`, `window.alert()`, `window.prompt()`) signalées par l'utilisateur (ex. boîte JavaScript native sur la reprise de patient dans la file d'attente), et introduire un composant et service centralisé de dialogue modal (`DialogService`, `DialogHostComponent`) harmonisé avec le design system Joprelys (tokens CSS, thèmes light/dark, arrondis sobres 4-6px, i18n FR/EN, accessibilité WCAG AA et clavier), tout en enrichissant le système de notification éphémère (`ToastService`, `ToastHostComponent`).

## Contexte & Constat
- Une capture d'écran utilisateur montre la fenêtre modale native du navigateur (« joprelys.com indique : Ce patient est déjà en consultation... OK / Annuler ») générée par `confirm()` dans `visit-details-drawer.component.ts`.
- 5 occurrences de `confirm()` et 19 occurrences de `alert()` sont dispersées dans le code applicatif web en raison de l'absence d'un service programmatique simple (`DialogService.confirm()` / `DialogService.alert()`).
- `ConfirmationDialogComponent` existait mais imposait une déclaration verbeuse dans le template de chaque composant consommateur, réservée aux flux destructifs rouges (`danger`).

## Critères d'acceptation
- [x] Création de `DialogService` injectable (`providedIn: 'root'`) avec les méthodes `confirm(options)` et `alert(options)` retournant des `Promise`.
- [x] Prise en charge des tonalités : `primary`, `warning`, `danger`, `info` avec icônes et styles de boutons adaptés.
- [x] Création du composant `DialogHostComponent` monté globalement dans l'application (`App`), avec support clavier (`Escape`, `Enter`), piège de focus et accessibilité ARIA (`role="alertdialog"`).
- [x] Remplacement de l'appel `confirm()` dans `visit-details-drawer.component.ts` par `dialog.confirm()`.
- [x] Remplacement de l'ensemble des `confirm()` restants (`consultation-prescription.facade.ts`, `patient-medical-info.component.ts`, `pre-registrations-list.component.ts`).
- [x] Remplacement de l'ensemble des `alert()` natifs par `ToastService` ou `dialog.alert()`.
- [x] Enrichissement de `ToastService` (méthodes `success`, `error`, `warning`, `info`, icônes et tonalité warning).
- [x] Demande de confirmation systématique lors de la déconnexion utilisateur (`AppShellComponent.logout`) avec dialogue modal `warning` (`confirmLabel`, `cancelLabel`, titre et message i18n FR/EN).
- [x] Conformité stricte aux standards UI Joprelys : Tailwind CSS v4, tokens CSS, arrondis 4-6px (max 8px), contrastes WCAG AA, thèmes light et dark, zéro texte en dur (i18n FR/EN).
- [x] Tests unitaires Vitest pour `DialogService` et `DialogHostComponent`.
- [x] 100% des tests Angular modifiés verts (`ng test --watch=false`) et build de production valide (`ng build`).

## Actions à réaliser
- [x] 1. Écrire la spécification fonctionnelle `docs/features/ui-dialog-notification-system/FUNCTIONAL-SPEC.md`.
- [x] 2. Écrire la conception technique `docs/features/ui-dialog-notification-system/TECHNICAL-DESIGN.md`.
- [x] 3. Créer `DialogService` et `DialogHostComponent`.
- [x] 4. Mettre à jour `ToastService`, `ToastHostComponent` et les styles de boutons (`.ui-button-warning`).
- [x] 5. Monter `DialogHostComponent` dans `App` (`web/src/app/app.ts` et `app.html`).
- [x] 6. Refactoriser `visit-details-drawer.component.ts` pour utiliser `DialogService.confirm()`.
- [x] 7. Refactoriser les autres occurrences de `confirm()` et `alert()`.
- [x] 8. Ajouter les clés de traduction FR/EN manquantes.
- [x] 9. Intégrer la confirmation de déconnexion dans `AppShellComponent` et ses tests unitaires.
- [x] 10. Écrire et exécuter les tests unitaires.
- [x] 11. Mettre à jour `PROJECT-TRACKING.md` et `CHANGELOG.md`.
