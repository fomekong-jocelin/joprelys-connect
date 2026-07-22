# FUNCTIONAL-SPEC — Cloisonnement permission-first de tous les rôles

## Besoin

Un utilisateur ne doit voir, ouvrir ou exécuter que les actions correspondant à ses permissions effectives, indépendamment d'un ancien compte utilisé dans le navigateur ou du nom de son rôle.

## Règles fonctionnelles

- RF-01 : les permissions effectives sont recalculées pour la session courante et ne survivent jamais à un changement de jeton.
- RF-02 : une permission déclarée sur une action est obligatoire ; le nom du rôle ne la remplace pas.
- RF-03 : le menu, les routes et l'API appliquent la même matrice.
- RF-04 : l'API refuse par défaut et reste l'autorité finale.
- RF-05 : les contrôles de permission ne remplacent pas l'isolation tenant, propriétaire ou patient.
- RF-06 : toute session contenant `PATIENT` est traitée comme patient-only jusqu'à correction de la donnée source.
- RF-07 : les rôles personnalisés sont supportés uniquement par leurs permissions.
- RF-08 : une déconnexion efface toutes les données du stockage navigateur, les cookies de l'origine et les états mémoire sensibles ; les préférences thème/langue sont volontairement réinitialisées.
- RF-09 : une connexion avec une autre identité purge l'état précédent avant d'enregistrer la nouvelle session.
- RF-10 : le frontend ne déduit jamais une permission à partir d'une autre permission ; seules les permissions retournées par `/api/rbac/me` sont effectives.
- RF-11 : une entrée de menu, une route, un onglet et ses appels API partagent la même politique d'accès.
- RF-12 : `LAB_ORDER_READ` couvre le laboratoire d'un patient autorisé ; la file laboratoire globale exige `LAB_QUEUE_READ`.
- RF-13 : le rôle système `MEDECIN` ne possède aucune permission de facturation ou de caisse par défaut ; un rôle personnalisé peut les lui apporter explicitement.
- RF-14 : `ADMIN_JOPRELYS` et `SUPER_ADMIN` sont des rôles plateforme ; ils ne sont jamais attribuables depuis l'administration d'un établissement. Toute tentative explicite d'affectation par un administrateur clinique est une interdiction d'autorisation et retourne `403 Forbidden`, indépendamment du flag persistant `assignable`.

## Parcours de recette

Pour chaque rôle système et personnalisé : connexion, contrôle du menu, tentative d'URL directe, appels API autorisés, appels API interdits, déconnexion, connexion avec un autre rôle et répétition sans rechargement complet.

Scénarios Go-Live supplémentaires :

- `ADMIN_CLINIQUE -> ADMIN_JOPRELYS` : `403` ;
- `ADMIN_CLINIQUE -> SUPER_ADMIN` : `403` ;
- un rôle clinique réellement désactivé ou non attribuable conserve le contrat d'erreur métier existant ;
- les rôles plateforme ne sont pas retournés dans la liste des rôles administrables d'un établissement.

## Périmètre de la généralisation

La généralisation couvre tous les contrôleurs contenant encore un contrôle de rôle ou un
fallback rôle, ainsi que toutes les routes et entrées de navigation professionnelles. La
matrice canonique est définie dans `AUTHORIZATION-MATRIX.md`.
