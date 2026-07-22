# Preuve d’audit — HOS-RBAC-001-C

## Audit concerné

- Audit : `AUDIT-20260721`
- Écart principal : `GAP-016 — Séparation des tâches`
- Statut de la preuve : `IN_PROGRESS` tant que la PR #108 n’est pas fusionnée et la recette multi-profils non signée.

## Risque réduit

Le droit historique `HOSPITALIZATION_MANAGE` autorisait indistinctement six écritures : admission, notes, consentements, soins, administration médicamenteuse et consommables. Un profil autorisé pour une seule de ces missions disposait donc potentiellement des cinq autres.

## Contrôles introduits

1. six permissions métier distinctes ;
2. annotations Spring Security dédiées sur chaque commande HTTP ;
3. matrice séparée médecin, infirmier et responsable hospitalisation ;
4. retrait du droit historique aux rôles système migrés ;
5. contrôles Angular par action et garde dans les méthodes de soumission ;
6. test négatif empêchant un droit de lecture de prescription d’autoriser une administration ;
7. maintien des permissions spécialisées déjà livrées pour transfert, sortie et départ physique.

## Matrice de preuve

| Contrôle | Preuve dépôt |
|---|---|
| Permissions et matrice | `HospitalizationPermissionCatalog.java` |
| Migration idempotente des rôles | `HospitalizationRbacBootstrap.java` |
| Barrière serveur | `HospitalizationController.java` |
| Assertions des annotations | `HospitalizationControllerClinicalAuthorizationTest.java` |
| Assertions de matrice | `HospitalizationPermissionCatalogTest.java` |
| Barrière interface | composants hospitalisation Angular |
| Tests négatifs interface | specs notes, soins, médicaments et consommables |
| Spécification | `docs/ai/tickets/HOS-RBAC-001-C-CLINICAL-OPERATION-SEGREGATION.md` |
| Conception | `HOS-RBAC-001-C-TECHNICAL-DESIGN.md` |

## Reste indispensable avant `PARTIAL`

- CI Maven et Angular entièrement verte sur le head final ;
- recette API/UI avec profils représentatifs ;
- vérification des rôles personnalisés ;
- renouvellement des JWT après resynchronisation ;
- mise à jour de la matrice globale avec le numéro de CI final.

## Reste indispensable avant `COVERED`

- ABAC par unité, affectation et relation de soin ;
- délégation temporelle et remplacement ;
- prescription hospitalière distincte de l’administration ;
- validation RSSI, direction médicale et direction des soins ;
- clearance administrative traitée dans HOS-DIS-001-B.
