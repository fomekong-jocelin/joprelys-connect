# Diagnostic — Désalignement des permissions dynamiques

Date : 2026-07-18

## Symptômes observés

- un utilisateur affiché comme `MEDECIN` voit « Facturation & Caisse » ;
- `/clinic/billing` déclenche `GET /api/cash-registers/sessions/active` en `403` ;
- `/clinic/lab-orders` est proposé puis `GET /api/lab-orders` répond `403`.

## Causes confirmées

1. `RbacCatalog` affecte `BILLING_INVOICE_READ` et `BILLING_INVOICE_WRITE` à `MEDECIN`.
2. Le menu, le dashboard et la route `/clinic/lab-orders` acceptent `LAB_ORDER_READ`,
   mais le contrôleur exige `LAB_QUEUE_READ` pour la file globale.
3. `RbacApiService.effectivePermissionSet()` ajoute `LAB_ORDER_READ` lorsqu'il trouve
   `LAB_QUEUE_READ`, créant un droit frontend absent du contrat effectif.
4. `BillingManagementPageComponent` charge systématiquement les conventions, tarifs et
   l'état de caisse, même lorsque l'utilisateur n'a accès qu'à un autre sous-domaine.
5. La navigation patient active ajoute consultations, laboratoire et hospitalisation en
   bloc dès qu'une seule des trois permissions est présente.
6. Les entrées Spatial, Caisse et Facturation ne partagent pas exactement la politique
   de leurs routes.
7. Les sous-écrans financiers exposent ou chargent des capacités plus privilégiées que
   leur droit d'entrée : encaissement, relance, export, historique caisse, résolution
   d'écart et progression de bordereau.

## Risques

- affichage trompeur d'une capacité métier non attribuée ;
- multiplication des `403`, perte de confiance et bruit de supervision ;
- rôles personnalisés incompatibles avec la promesse de permissions dynamiques ;
- risque d'évolution vers un contournement frontend si des permissions sont déduites.

## Décision

Les permissions de `/api/rbac/me` sont la seule vérité frontend. Elles ne sont jamais
étendues implicitement. Menu, dashboard, route, onglet, action et appel initial doivent
vérifier la permission exacte de l'API consommée.

## Résolution vérifiée

- `MEDECIN` ne reçoit plus `BILLING_INVOICE_READ` ni `BILLING_INVOICE_WRITE`.
- La file globale laboratoire est uniformément liée à `LAB_QUEUE_READ`.
- Les politiques partagées empêchent la dérive menu/route.
- Les écrans composites ne chargent plus caisse, résultats, historique, export ou relance
  en l'absence de la permission exacte.
- Les scénarios financiers des tests backend sont désormais exécutés par un acteur de
  facturation, sans redonner de permission financière au médecin.
- Preuves : 267 tests Angular et 453 tests Maven verts, build production et i18n verts.
