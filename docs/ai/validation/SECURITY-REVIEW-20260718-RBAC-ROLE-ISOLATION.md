# Security review — RBAC et cloisonnement des rôles

Date : 2026-07-18
Périmètre : cache RBAC Angular, navigation, guards, filtre JWT, endpoints disponibilités, inventaire global des contrôleurs et routes.

## SEC-RBAC-001 — Cache de permissions non lié à la session

- Sévérité : High / P0
- Localisation : `web/src/app/clinic/rbac/rbac-api.service.ts:18-86`
- Risque : les permissions d'un compte pouvaient être consommées après connexion d'un autre compte dans la même SPA.
- Traitement : cache et requête liés au jeton propriétaire ; réponse obsolète ignorée ; tests médecin→patient et caissier→infirmier.
- Statut : corrigé, tests Angular verts.

## SEC-RBAC-002 — Fallback rôle sur une route permissionnée

- Sévérité : High
- Localisation : `web/src/app/auth/role.guard.ts:18-47`
- Risque : retirer une permission n'interdisait pas la route si un rôle attendu subsistait.
- Traitement : `expectedPermissions` devient obligatoire ; aucun fallback de rôle.
- Statut : corrigé pour toutes les routes déclarant déjà une permission.

## SEC-RBAC-003 — Claim patient mixte

- Sévérité : High
- Localisation : `backend/src/main/java/com/joprelys/backend/auth/security/JwtAuthenticationFilter.java:65-66,103-106`
- Risque : un claim contenant `PATIENT,MEDECIN` pouvait produire des authorities patient et professionnelles.
- Traitement : toute branche patient ne produit que `ROLE_PATIENT`; tests backend ajoutés sur les disponibilités.
- Statut : corrigé et validé par la suite Maven complète.

## SEC-RBAC-004 — Disponibilités protégées par rôle au lieu de permission

- Sévérité : Medium
- Localisation : `backend/src/main/java/com/joprelys/backend/appointment/api/AvailabilityController.java:43-152`
- Risque : une révocation de `AVAILABILITY_MANAGE` n'était pas suffisante tant que le rôle médecin/admin subsistait.
- Traitement : les sept méthodes exigent `hasAuthority('AVAILABILITY_MANAGE')`; la gestion d'un autre praticien exige séparément `AVAILABILITY_MANAGE_ALL`; menu et route alignés.
- Statut : corrigé et validé par les tests d'intégration.

## SEC-RBAC-005 — Autorisation globale rôle-only

- Sévérité : Medium, potentiellement High selon le domaine
- Preuve initiale : 21 contrôleurs et plusieurs routes professionnelles autorisaient encore directement des noms de rôle.
- Risque : incohérence avec les rôles personnalisés et révocations de permissions ; aucun accès indu n'est affirmé sans analyse métier endpoint par endpoint.
- Traitement : catalogue enrichi, contrôleurs migrés vers les authorities, élargissements de périmètre disponibilités/spatial/RBAC migrés vers des permissions explicites, routes/menus/actions Angular alignés et test statique de non-régression ajouté.
- Statut : corrigé techniquement ; validation de la matrice par le RSSI et les responsables métiers encore requise.

## SEC-RBAC-006 — État navigateur résiduel après logout/changement de compte

- Sévérité : High
- Localisation : `web/src/app/auth/auth-token-storage.service.ts:49-130`, `backend/src/main/java/com/joprelys/backend/auth/api/RefreshTokenCookieManager.java:52-60`, `backend/src/main/java/com/joprelys/backend/patient/api/PatientAuthController.java:38-46`
- Risque : préférences, scopes fonctionnels, patient actif ou cookie de refresh pouvaient survivre à une frontière de compte.
- Traitement : purge complète des stockages et états mémoire ; expiration HttpOnly + `Clear-Site-Data`; login patient neutralise un ancien cookie professionnel.
- Limite : si le backend est totalement inaccessible au logout, JavaScript ne peut pas supprimer physiquement un cookie HttpOnly ; les données locales et cookies accessibles sont néanmoins purgés immédiatement, puis le serveur purge le cookie dès qu'une réponse de sortie/refresh invalide est reçue.
- Statut : corrigé ; tests Angular et backend verts.

## SEC-RBAC-007 — Secret laboratoire par défaut

- Sévérité : High.
- Localisation : `backend/src/main/resources/application.yml`, `LabResultUploadController`.
- Risque : une installation hors profil production pouvait accepter une clé publique connue.
- Traitement : valeur par défaut supprimée ; configuration vide refusée ; comparaison de clé en temps constant.
- Statut : corrigé et couvert par la suite backend.

## SEC-RBAC-008 — Permissions financières attribuées au rôle médecin

- Sévérité : High.
- Localisation : `backend/src/main/java/com/joprelys/backend/auth/rbac/RbacCatalog.java`.
- Risque : le rôle système `MEDECIN` recevait `BILLING_INVOICE_READ` et `BILLING_INVOICE_WRITE`, ce qui rendait légitime côté frontend l'affichage de « Facturation & Caisse ».
- Traitement : retrait de ces deux permissions du catalogue médecin ; les rôles financiers et les rôles personnalisés explicitement habilités restent autorisés.
- Statut : corrigé et couvert par les tests du catalogue RBAC.

## SEC-RBAC-009 — Désalignement portail laboratoire et inférence de permission

- Sévérité : High.
- Localisation : politiques de navigation Angular, `RbacApiService` et endpoint `GET /api/lab-orders`.
- Risque : `LAB_ORDER_READ` rendait visible la file laboratoire alors que l'API exigeait `LAB_QUEUE_READ`; une inférence côté client transformait aussi implicitement une permission en une autre.
- Traitement : suppression de toute inférence ; le portail global exige exactement `LAB_QUEUE_READ`, les résultats patient exactement `LAB_ORDER_READ`, et les écritures exactement `LAB_ORDER_WRITE`.
- Statut : corrigé et couvert par les tests de routes, navigation, RBAC et page laboratoire.

## SEC-RBAC-010 — Chargements API composites sans permission exacte

- Sévérité : Medium.
- Localisation : workspace facturation/caisse et sous-composants Angular.
- Risque : l'accès à un seul sous-espace déclenchait des appels vers des API voisines non autorisées, générant des `403` et exposant une surface fonctionnelle incohérente.
- Traitement : politiques centralisées, sélection du premier onglet autorisé, chargements conditionnels et gardes au niveau des méthodes et actions.
- Statut : corrigé ; profils minimaux finance, caisse, assurance, recouvrement, DAF et laboratoire testés.

## Contrôles complémentaires

- `SecurityConfig` applique `anyRequest().authenticated()` hors endpoints explicitement publics.
- L'isolation tenant et propriétaire doit rester testée indépendamment des permissions.
- Aucun secret, nouvelle dépendance, URL backend codée en dur, Angular Material, Tailwind v3, `application.properties` ou changement DB n'est introduit.

## Preuves finales

- Maven : 453 tests, 0 échec, 0 erreur, 1 ignoré.
- Angular : 267 tests, 0 échec.
- Build Angular production : vert.
- i18n shell : 47 clés FR/EN présentes.
- Scan statique : aucun `hasRole`/`hasAnyRole` dans une annotation `@PreAuthorize` de contrôleur.
- Routes Angular : `expectedRoles` subsiste uniquement sur les 13 routes du portail patient.
