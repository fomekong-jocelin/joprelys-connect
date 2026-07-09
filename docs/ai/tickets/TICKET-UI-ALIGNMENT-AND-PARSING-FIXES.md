# TICKET-UI-ALIGNMENT-AND-PARSING-FIXES — Résolution des anomalies visuelles, de traduction brute, et alignement avec la charte graphique

## Diagnostic

Lors de la revue des pages d'Occupation des Lits et de Facturation, plusieurs défauts visuels et non-respects de la charte graphique ont été constatés :

1. **Rupture de ligne de ponctuation (Spatial)** : Le sélecteur de service affichait le label `SERVICE` et le caractère `:` sur deux lignes distinctes, détériorant le rendu et l'alignement.
2. **Affichage brut des interpolations de traduction (Billing)** : Dans la page de facturation, les sous-titres et les onglets affichaient la syntaxe brute Angular de traduction, par exemple :
   `{{ t('billing.subtitle', 'Générez les factures de soins, gérez les conventions d\'assurance, appliquez la grille tarifaire et encaissez les règlements.') }}`
   - *Pourquoi* : Les expressions de traduction utilisaient des apostrophes échappées `\'` (ex: `conventions d\'assurance`). Dans la grammaire des expressions de templates d'Angular, l'antislash d'échappement n'est pas supporté ou cause une erreur de syntaxe de l'expression, forçant le compilateur à l'ignorer et à restituer l'expression sous forme de texte brut statique.
3. **Non-respect du Design System (Billing)** : Plusieurs boutons de la page de facturation utilisaient des classes ad-hoc (ex: `bg-brand-cyan`, `rounded-md`, `px-3`, `py-2`) au lieu d'utiliser uniquement les classes globales centralisées du design system (`ui-button`, `ui-button-primary`, `ui-button-secondary`). Cela causait des incohérences de hauteur (les boutons faisaient moins de 42px) et violait la consigne des arrondis sobres.

## Résolution

1. **Correction du label (Spatial)** : Ajout de la classe `whitespace-nowrap` et utilisation d'un espace insécable `&nbsp;:` dans [spatial-management-page.component.ts](file:///C:/MES-APPLICATIONS/joprelys-connect/web/src/app/clinic/spatial/spatial-management-page.component.ts) pour figer la ponctuation avec son label.
2. **Résolution du parsing i18n (Billing)** : Remplacement des apostrophes échappées par des guillemets doubles entourant la chaîne par défaut (ex: `"Conventions d'Assurance"`) dans [billing-management-page.component.ts](file:///C:/MES-APPLICATIONS/joprelys-connect/web/src/app/clinic/billing/billing-management-page.component.ts), permettant à l'expression d'être correctement parsée et évaluée par le moteur Angular.
3. **Alignement Charte Graphique (Billing)** : Remplacement de toutes les classes de boutons ad-hoc par les classes officielles `.ui-button .ui-button-primary` et `.ui-button .ui-button-secondary`. Cela harmonise les dimensions avec les inputs (hauteur minimale 42px), respecte les arrondis, et réutilise les transitions et états du design system central.

## Fichiers modifiés

- [spatial-management-page.component.ts](file:///C:/MES-APPLICATIONS/joprelys-connect/web/src/app/clinic/spatial/spatial-management-page.component.ts) : Correction du style du label de sélection de service.
- [billing-management-page.component.ts](file:///C:/MES-APPLICATIONS/joprelys-connect/web/src/app/clinic/billing/billing-management-page.component.ts) : Correction des interpolations de traduction et remplacement des boutons ad-hoc par les boutons de la charte.
- [TICKET-UI-ALIGNMENT-AND-PARSING-FIXES.md](file:///C:/MES-APPLICATIONS/joprelys-connect/docs/ai/tickets/TICKET-UI-ALIGNMENT-AND-PARSING-FIXES.md) : Ce ticket de suivi.

## Statut

- [x] Correction de la césure du label de service (Spatial).
- [x] Résolution des chaînes de traduction brutes (Billing).
- [x] Harmonisation des boutons avec la charte graphique et hauteurs d'inputs (Billing).
- [ ] Passage de la compilation et des tests d'intégration.
