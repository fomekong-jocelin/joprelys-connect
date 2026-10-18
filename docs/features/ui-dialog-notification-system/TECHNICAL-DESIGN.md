# TECHNICAL DESIGN — Système unifié de dialogues et notifications (Dialog & Notification System)

## 1. Architecture générale

Le système de dialogue et de notification repose sur une architecture découplée pilotée par des signaux Angular (`signal`), sans dépendance tierce, conforme aux standards Joprelys Connect (Tailwind CSS v4 CSS-first, tokens CSS, thèmes light/dark, accessibilité WAI-ARIA) :

```text
[Composant Métier / Service]
         │
         │ inject(DialogService) / inject(ToastService)
         ▼
 ┌───────────────────────────┐         ┌───────────────────────────┐
 │       DialogService       │         │       ToastService        │
 │  (Signal: currentDialog)  │         │     (Signal: toasts)      │
 └─────────────┬─────────────┘         └─────────────┬─────────────┘
               │                                     │
               ▼                                     ▼
 ┌───────────────────────────┐         ┌───────────────────────────┐
 │    DialogHostComponent    │         │    ToastHostComponent     │
 │ (Monté à la racine App)   │         │  (Monté à la racine App)  │
 └───────────────────────────┘         └───────────────────────────┘
```

## 2. Spécification technique des composants

### 2.1. `DialogService` (`web/src/app/shared/ui/dialog.service.ts`)
- **Portée** : `@Injectable({ providedIn: 'root' })`.
- **Modèle de données** :
  ```typescript
  export type DialogTone = 'primary' | 'warning' | 'danger' | 'info';

  export interface DialogConfig {
    title: string;
    message: string;
    confirmLabel?: string;
    cancelLabel?: string;
    tone?: DialogTone;
    icon?: string;
    isAlert?: boolean; // Si true, seul le bouton confirmLabel est affiché
  }

  interface DialogSession extends DialogConfig {
    id: number;
    resolve: (value: boolean) => void;
  }
  ```
- **Méthodes** :
  - `confirm(config: DialogConfig): Promise<boolean>` : ouvre le dialogue et retourne une Promise résolue avec `true` (confirmation) ou `false` (annulation / fermeture).
  - `alert(config: DialogConfig): Promise<void>` : ouvre le dialogue d'alerte sans bouton d'annulation et résout la Promise lors de l'acquittement.
  - `close(result: boolean)` : méthode interne appelée par l'hôte pour résoudre la Promise en cours.

### 2.2. `DialogHostComponent` (`web/src/app/shared/ui/dialog-host.component.ts`)
- **Sélecteur** : `app-dialog-host`.
- **Accessibilité** :
  - `role="alertdialog"`, `aria-modal="true"`.
  - Capture de l'événement clavier `@HostListener('document:keydown.escape')` pour annulation.
  - Piège de focus et restauration du focus précédent lors de la fermeture.
- **Rendu & Tokens CSS** :
  - Arrière-plan : `bg-[var(--overlay-bg)] backdrop-blur-[1px]`.
  - Carte : `border border-[var(--app-border)] bg-[var(--app-surface)] shadow-[var(--shadow-panel)] rounded-[var(--radius-brand-md)]`.
  - Arrondis stricts : 6px (`rounded-[6px]` ou `var(--radius-brand-md)`). Pas de `rounded-2xl` ni `rounded-full`.
  - Bouton confirmation :
    - `tone === 'danger'` : `.ui-button-danger`
    - `tone === 'warning'` : `.ui-button-warning`
    - `tone === 'primary' | 'info'` : `.ui-button-primary`
  - Bouton annulation : `.ui-button-secondary`.

### 2.3. Évolution de `ToastService` & `ToastHostComponent`
- Ajout de la tonalité `'warning'` (`ToastTone = 'success' | 'info' | 'warning' | 'error'`).
- Méthodes helpers :
  - `success(message: string, duration?: number)`
  - `error(message: string, duration?: number)`
  - `warning(message: string, duration?: number)`
  - `info(message: string, duration?: number)`
- Intégration d'icônes adaptées dans chaque toast (`app-ui-icon`).

### 2.4. Styles CSS de marque (`web/src/styles.css`)
- Ajout de la classe `.ui-button-warning` :
  ```css
  .ui-button-warning {
    background: var(--brand-warning);
    color: var(--text-inverse);
    border: 1px solid transparent;
  }
  .ui-button-warning:hover:not(:disabled) {
    background: var(--brand-warning-hover);
  }
  ```

## 3. Plan de migration des dialogues natifs

| Fichier | Ancien appel | Nouveau remplacement |
|---|---|---|
| `visit-details-drawer.component.ts` | `confirm(...)` | `dialog.confirm({ tone: 'warning', title, message, confirmLabel, cancelLabel })` |
| `consultation-prescription.facade.ts` | `confirm(...)` | `dialog.confirm({ tone: 'danger', ... })` |
| `patient-medical-info.component.ts` | `confirm(...)` | `dialog.confirm({ tone: 'danger', ... })` |
| `pre-registrations-list.component.ts` | `confirm(...)` | `dialog.confirm({ tone: 'danger', ... })` |
| `patient-consultations-tab.component.ts` | `alert(...)` | `toasts.error(...)` |
| `patient-detail.component.ts` | `alert(...)` | `toasts.error(...)` |
| `patient-consents-list.component.ts` | `alert(...)` | `toasts.error(...)` |
| `patient-notifications.component.ts` | `alert(...)` | `toasts.error(...)` |
| `patient-requests-list.component.ts` | `alert(...)` | `toasts.error(...)` |
| `patient-documents-page.component.ts` | `alert(...)` | `toasts.error(...)` |
| `patient-prescriptions-page.component.ts` | `alert(...)` | `toasts.error(...)` |
| `patient-results-page.component.ts` | `alert(...)` | `toasts.error(...)` |
| `patient-summary-page.component.ts` | `alert(...)` | `toasts.error(...)` |
| `app-shell.component.ts` | Déconnexion directe sans confirmation | `dialog.confirm({ tone: 'warning', title, message, confirmLabel, cancelLabel })` |
