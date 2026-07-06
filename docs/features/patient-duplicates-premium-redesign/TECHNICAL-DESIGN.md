# Spécification Technique — Refonte Premium de la Page de Gestion des Doublons Patients

## 1. Architecture Frontend

La refonte s'appuie sur le framework Angular et l'intégration des variables CSS du thème global.

### Composants Utilisés

- **`AppShellComponent`** (`app-shell`) : Fournit le conteneur principal de la page incluant la barre latérale de navigation et l'en-tête utilisateur connecté.
- **`PageHeaderComponent`** (`app-page-header`) : Standardise le titre et le sous-titre de la page.
- **`EmptyStateComponent`** (`app-empty-state`) : Gère l'affichage d'état vide en utilisant la classe CSS centralisée `.ui-card-muted`.
- **`ButtonComponent`** (`app-ui-button`) : Uniformise l'apparence des boutons d'actions (Rejeter, Fusionner, Confirmer, Annuler) avec l'arrondi standardisé de 4px (`rounded-sm`).

## 2. Design System & Intégration Visuelle

### Couleurs et Styles CSS

- **Card Structure** : Les cartes de suspicions de doublons utilisent la classe `.ui-card` du thème centralisé (bordure `1px solid var(--app-border)`, ombre légère `var(--shadow-panel)` et coins arrondis à `8px` max).
- **Sub-cards** : Les détails des dossiers Patient A et Patient B dans la grille utilisent un fond désaturé `.bg-slate-50/50` / `.dark:bg-slate-950/20` avec une bordure fine pour faire ressortir les informations techniques comme le numéro DPU.
- **Bannière Score** : Un badge en dégradé ou couleur contrastée (fond ambré désaturé `bg-amber-50` / `dark:bg-amber-950/30`) affiche le pourcentage de similarité.
- **Tableau de Comparaison** : 
  - La méthode `hasDiff(field)` détermine si les valeurs des champs du Patient A et Patient B sont différentes.
  - Si `hasDiff(field)` retourne `true`, la classe CSS `bg-amber-500/5` / `dark:bg-amber-950/10` est appliquée à la ligne (`<tr>`) de la table pour signaler visuellement le conflit à résoudre.

### Conformité Thème (Mode Clair / Sombre)

L'utilisation exclusive de classes Tailwind CSS v4 sémantiques ou de variables CSS centralisées (comme `var(--app-border)`, `var(--brand-primary)`) garantit une adaptabilité immédiate aux modes Clair et Sombre sans duplication de styles.

## 3. Robustesse & Tests

- Un fichier de test unitaire Vitest complet (`duplicates-page.component.spec.ts`) valide les comportements du composant :
  - Le chargement initial des doublons via `PatientApiService.getDuplicates()`.
  - L'ouverture/fermeture et la sélection de la fiche principale (`Primary`) dans l'assistant de fusion.
  - L'invocation correcte des méthodes de rejet (`ignoreDuplicate`) et de fusion (`mergePatients`) suivies du rechargement de la liste.
