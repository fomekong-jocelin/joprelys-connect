# Conception technique — Historique et bordereau de clôture

## Backend

- `CashSessionHistoryResponse` agrège la session persistée et `CashSessionSummaryResponse`.
- `GET /api/cash-registers/sessions/mine` retourne les vingt dernières sessions ouvertes par l’utilisateur connecté.
- `GET /api/cash-registers/sessions/{id}/closeout-report` génère le PDF à la demande.
- `CashRegisterService` reste la source d’autorisation et de rapprochement.
- `CashSessionCloseoutReportService` ne recalcule rien : il consomme le read model et les mouvements autorisés.
- Le numéro `CLS-yyyyMMdd-XXXXXXXX` est déterministe et ne nécessite aucune migration.

## Sécurité

- le filtre tenant Hibernate reste la première barrière ;
- le propriétaire de la session est contrôlé explicitement ;
- `DAF` et `ADMIN_CLINIQUE` peuvent accéder aux sessions tenantées ;
- les rôles cliniques sont exclus par `@PreAuthorize` ;
- l’endpoint des mouvements applique désormais la même règle de propriété/supervision.

## Frontend

- `BillingApiService` expose un flux `cashSessionClosed$` émis uniquement après succès HTTP de la clôture ;
- `BillingCashSessionHistoryComponent` recharge alors l’historique et met en évidence la session ;
- les mouvements sont chargés à l’ouverture du détail pour limiter les appels ;
- le PDF est téléchargé comme `Blob` ;
- `:host { display: block; }` rétablit l’espacement Tailwind entre composants Angular personnalisés.

## Performance

- historique borné à vingt sessions ;
- mouvements chargés à la demande ;
- PDF généré à la demande ;
- pas de migration, stockage ou duplication de totals.