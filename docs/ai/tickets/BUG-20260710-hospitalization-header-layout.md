# BUG-20260710 — En-tête d’hospitalisation écrasé

| Champ | Valeur |
|---|---|
| **Type** | Diagnostic + correction UI |
| **Statut** | DONE |
| **Priorité** | P1 |
| **Stack** | Angular / Tailwind CSS v4 |
| **Estimation** | 0,25 j |
| **Reviewer** | Lead Developer |
| **Date** | 2026-07-10 |

## Contexte

Sur la fiche d’hospitalisation, la zone d’informations du séjour et la zone d’actions partageaient une ligne flexible trop étroite. Les libellés longs provoquaient un rabattement visuellement collé au contenu et aux bords du panneau.

## Critères d’acceptation

- [x] Les informations du séjour et les actions disposent de colonnes distinctes sur grand écran.
- [x] Les trois actions restent lisibles et espacées, y compris avec les libellés FR longs.
- [x] La mise en page se replie en une colonne sur les écrans plus étroits.
- [x] Le contenu de la carte conserve un espace interne visible entre les éléments et la bordure.
- [x] Le bouton « Synthèse PDF » dispose d’un padding cohérent avec les autres actions.
- [x] Le titre service/chambre/lit reste lisible sans dominer le panneau.
- [x] Aucun contrat API, appel métier ou règle d’autorisation n’est modifié.

## Action plan

- [x] Analyser le composant d’en-tête existant et le design system.
- [x] Corriger la grille responsive et le comportement de retour à la ligne.
- [x] Ajouter la documentation fonctionnelle et technique.
- [x] Mettre à jour le suivi et le changelog.
- [ ] Exécuter la validation visuelle dans le navigateur à 1366 px et sur mobile.

## Fichiers impactés

- `web/src/app/patient/hospitalization-stay-header.component.ts`
- `docs/features/hospitalization-header-layout/FUNCTIONAL-SPEC.md`
- `docs/features/hospitalization-header-layout/TECHNICAL-DESIGN.md`

## Sécurité / régression

Correction limitée à la présentation. Les événements Angular existants (`entryPdf`, `transfer`, `discharge`) et les contrôles `[canModify]` sont conservés.

## SemVer

Correction rétrocompatible de présentation : impact PATCH, sans bump applicatif préparé dans cette intervention.
