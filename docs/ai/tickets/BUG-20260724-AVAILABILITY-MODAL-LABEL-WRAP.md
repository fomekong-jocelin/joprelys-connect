# BUG — Libellés de la modal Disponibilités sur deux lignes

## Constat
Dans la modal de création/modification d'une plage de disponibilité, le libellé `Valable jusqu'au (optionnel)` se replie sur deux lignes sur desktop, ce qui casse l'alignement visuel des champs.

## Cause
Le suffixe `(optionnel)` est redondant : les champs obligatoires sont déjà identifiés par `*`. Sur une grille compacte, ce suffixe allonge inutilement les labels.

## Correctif
- FR : `Valable jusqu'au (optionnel)` → `Valable jusqu'au`.
- FR : `Motif (optionnel)` → `Motif`.
- EN : `Valid until (optional)` → `Valid until`.
- EN : `Reason (optional)` → `Reason`.

## Règle UX
Dans les formulaires Joprelys, les champs obligatoires portent `*`; l'absence de `*` signifie que le champ est optionnel. On évite donc de répéter `(optionnel)` dans les labels quand cela dégrade la mise en page.

## Non-régression
Aucune logique métier, API, validation ou composant Angular modifié. Correctif i18n uniquement.
