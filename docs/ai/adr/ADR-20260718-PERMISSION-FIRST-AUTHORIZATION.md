# ADR-20260718 — Autorisation permission-first

## Statut

Accepté et implémenté techniquement — validation métier/RSSI de la matrice encore requise avant livraison.

## Date

2026-07-18

## Contexte

Les rôles système, rôles personnalisés et permissions coexistent. Des fallbacks `rôle OU permission` permettent à un rôle de contourner le retrait d'une permission. Un cache Angular non lié au jeton a aussi exposé des menus d'une session précédente.

## Décision

Une action qui possède une permission catalogue doit être autorisée par cette permission effective, sans fallback sur le nom du rôle. Les rôles deviennent des ensembles de permissions. Les frontières d'identité telles que le portail patient restent explicites et documentées. Le backend décide ; Angular reflète la décision pour l'expérience utilisateur.

## Raisons

- moindre privilège et révocation effective ;
- support cohérent des rôles personnalisés ;
- même vocabulaire d'autorisation sur menu, route et API ;
- réduction des erreurs lors des changements de rôle.

## Conséquences

- Les fallbacks legacy doivent être migrés par domaine après validation métier.
- Une permission manquante provoquera un refus explicite plutôt qu'un accès par rôle.
- Les tests de sécurité deviennent matriciels et obligatoires.
- Le cache RBAC doit être lié au jeton, y compris lors d'un refresh.

## Alternatives rejetées

| Alternative | Raison |
|---|---|
| Rôle-only | Trop grossier, incompatible avec les rôles personnalisés et la révocation fine |
| `rôle OU permission` permanent | Une permission retirée ne retire pas réellement l'accès |
| Contrôle frontend seul | Contournable par appel direct à l'API |

## Impact planning

21 SP estimés sur deux sprints, profils Senior Angular, Senior Backend, QA sécurité et validations RSSI/métiers.

## Références

- `docs/ai/tickets/EPIC-0026-permission-first-role-isolation.md`
- `docs/ai/validation/SECURITY-REVIEW-20260718-RBAC-ROLE-ISOLATION.md`
