# TECHNICAL-DESIGN — Cloisonnement permission-first de tous les rôles

## Architecture cible

1. Le JWT identifie l'utilisateur et le contexte tenant.
2. Le backend résout à chaque requête les rôles et permissions effectifs.
3. Spring Security exige une authority métier sur chaque endpoint sensible.
4. Angular charge `/api/rbac/me` pour le jeton courant, lie le cache à ce jeton et ignore toute réponse obsolète.
5. Les menus et guards utilisent les permissions ; les rôles ne subsistent que pour les frontières d'identité explicitement documentées.

## Invariants

- Deny-by-default dans `SecurityConfig`.
- Aucun cache RBAC consommable si son jeton propriétaire diffère du jeton courant.
- Une route avec `expectedPermissions` ne peut pas être autorisée par `expectedRoles`.
- `PATIENT` dans un claim mixte produit uniquement `ROLE_PATIENT`.
- `AVAILABILITY_MANAGE` reste lié au praticien courant ; seul `AVAILABILITY_MANAGE_ALL` élargit ce périmètre.
- Le frontend n'est jamais considéré comme une barrière de sécurité suffisante.
- `AuthTokenStorageService.clear()` purge `sessionStorage`, `localStorage`, les cookies accessibles et déclenche les cleanups mémoire enregistrés.
- Le backend expire le refresh cookie HttpOnly et renvoie `Clear-Site-Data: "cache", "cookies", "storage"` ; le login patient applique aussi cette purge pour neutraliser un ancien cookie professionnel.
- `clearAccessToken()` reste réservé à la rotation transparente du jeton du même compte et ne déclenche pas une fausse frontière de session.
- `RbacApiService` expose strictement les permissions reçues ; aucune relation d'implication n'est appliquée côté navigateur.
- Les politiques des destinations composites sont partagées entre navigation et routes afin d'empêcher leur dérive.
- Un écran composite choisit le premier onglet réellement autorisé et ne lance que les appels correspondant aux permissions présentes.
- `RbacCatalog` est la source canonique du caractère attribuable des rôles système. `ADMIN_JOPRELYS` et `SUPER_ADMIN` restent `assignable=false` dans le contexte clinique ; aucun initializer secondaire ne doit inverser cette règle en base.
- Dans `RbacAdministrationService.replaceUserRoles`, l'interdiction explicite d'un rôle plateforme est évaluée avant le contrôle générique `enabled/assignable`, afin de produire un `403` stable pour une tentative d'escalade et de conserver le `400` pour un rôle clinique désactivé/non attribuable.

## Migration

- Lot P0 : cache générique, frontière patient, guard permission-first, disponibilités sur `AVAILABILITY_MANAGE`.
- Lots EPIC-0026 : matrice canonique, routes/menus, tous les contrôleurs contenant encore
  `hasRole`/`hasAnyRole`, suppression des fallbacks des contrôleurs déjà permission-aware,
  capacités patient dédiées et tests matriciels.
- Correctif Go-Live : supprimer toute politique de démarrage qui force les rôles plateforme à `assignable=true` et renforcer les tests du contrat `403` plateforme / `400` rôle clinique non attribuable.
- Chaque lot conserve les contrats métier et ajoute des tests 200/403 par rôle.

## Catalogue ajouté

Les permissions nouvelles sont limitées aux actions qui ne possédaient aucun équivalent
canonique : visites, documents, accueil, webhooks, configuration spatiale, relances de
créances, gestion globale des disponibilités et capacités du portail patient. Le démarrage RBAC synchronise ces permissions et
les affectations des rôles système ; les rôles personnalisés restent inchangés.

## Impacts

- DB : aucun pour le P0 ; migrations possibles uniquement si de nouvelles permissions sont cataloguées.
- API : aucun payload modifié ; politique d'autorisation renforcée.
- API : `GET /api/invoices/conventions` accepte aussi `INSURANCE_BORDEREAU_READ`, car
  les conventions sont une donnée de référence nécessaire au workspace assurance ;
  `GET /api/cash-registers/sessions/active` accepte `CASH_PAYMENT_COLLECT` pour vérifier
  la précondition d'encaissement sans élargir les mutations de caisse.
- UI : aucun nouveau composant, texte, thème ou token.
- Configuration : aucune variable ajoutée ; la variable existante `JOPRELYS_LAB_INTEGRATION_API_KEY` n'a plus de valeur de secours et une configuration vide ferme l'intégration.
- Performance : rechargement RBAC après rotation de jeton, acceptable et sécurisé.
- Dette préexistante : `LabOrdersPageComponent` reste au-dessus de 500 lignes ; le
  correctif P0 ajoute uniquement les contrôles d'autorisation. Son extraction de template
  et de formulaires doit précéder toute nouvelle évolution fonctionnelle.
