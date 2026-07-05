# Conception Technique — Alignement esthétique des contrôles de formulaires

## 1. Stack technique concernée

- **Frontend** : Angular (Tailwind CSS v4 CSS-first)
- **Fichiers impactés** :
  - `web/src/styles.css` (Définitions globales des classes `.ui-input`, `.ui-checkbox`, `.ui-radio`, `.ui-select`, `.ui-textarea`)
  - `web/src/app/patient/patient-medical-info.component.ts` (Formulaires d'antécédents)
  - `web/src/app/patient/patient-hospitalization.component.ts` (Formulaires d'hospitalisation)
  - `web/src/app/patient/portal/components/patient-consents-list.component.ts` (Cases à cocher et radios des consentements)
  - `web/src/app/patient/portal/components/patient-requests-list.component.ts` (Cases à cocher des demandes d'accès)
  - `web/src/app/clinic/staff/staff-management.component.ts` (Select du rôle)
  - `web/src/app/clinic/lab/lab-orders-page.component.ts` (Select de statut)
  - `web/src/app/consultation/consultation.component.ts` (Select de priorité)

## 2. Architecture & Design Cible des Classes CSS

Les styles seront refactorisés dans `web/src/styles.css` sous la couche `@layer components` pour utiliser les variables CSS de thème existantes :

| Variable CSS | Usage | Valeur Light | Valeur Dark |
|---|---|---|---|
| `--radius-brand-sm` | Coins des contrôles | `4px` | `4px` |
| `--bg-input` | Fond des champs | `#F2F4F7` | `#0f172a` |
| `--border-input` | Bordure standard | `#e4e7ec` | `#1e293b` |
| `--border-input-focus` | Bordure au focus | `#0b91b2` | `#22a8c8` |
| `--brand-primary` | Couleur active / cochée | `#0b91b2` | `#22a8c8` |
| `--focus-ring` | Ombre de focus (box-shadow) | `rgba(11, 145, 178, 0.18)` | `rgba(34, 168, 200, 0.22)` |

### Modification des classes :
- Remplacer `border-radius: 12px;` par `border-radius: var(--radius-brand-sm);` pour `.ui-input`, `.ui-select`, et `.ui-textarea`.
- Remplacer `border-radius: 4px;` par `border-radius: var(--radius-brand-sm);` pour `.ui-checkbox` pour respecter la cohérence globale.
- Ajouter des pseudo-classes d'état :
  - `:hover:not(:disabled)` : Légère accentuation de la bordure (`color-mix` avec la couleur primaire).
  - `:focus` ou `:focus-visible` : Application de `box-shadow: 0 0 0 3px var(--focus-ring);` pour assurer un focus visible accessible.
  - `:disabled` : Opacité réduite, fond grisé, curseur interdit.

## 3. Remplacement des styles ad-hoc dans les templates

Les formulaires qui possédaient des classes ad-hoc de type `border border-slate-100 bg-slate-50 rounded-[var(--radius-brand-sm)]` ou similaires seront nettoyés pour n'utiliser que les classes utilitaires standard de notre système :
- Les inputs de texte deviennent de simples `<input class="ui-input" />`.
- Les zones de texte deviennent de simples `<textarea class="ui-textarea"></textarea>`.
- Les dropdowns de sélection deviennent des `<select class="ui-select"></select>`.
- Les cases à cocher deviennent des `<input type="checkbox" class="ui-checkbox" />`.
- Les boutons radio deviennent des `<input type="radio" class="ui-radio" />`.

Cela garantit une réduction significative du code dupliqué et une propagation instantanée de toute future mise à jour de thème.

## 4. Stratégie de tests et non-régression

- **Compilation** : Lancer `npm run build` dans le dossier `web/` pour s'assurer qu'aucun changement de template ou de syntaxe CSS n'introduit d'erreur de compilation.
- **Régression visuelle** : Vérifier visuellement le bon alignement des coins, l'affichage en mode light et dark, ainsi que la visibilité de la bague de focus lors du parcours clavier.
