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

## Migration

- Lot P0 : cache générique, frontière patient, guard permission-first, disponibilités sur `AVAILABILITY_MANAGE`.
- Lots EPIC-0026 : matrice canonique, routes/menus, tous les contrôleurs contenant encore
  `hasRole`/`hasAnyRole`, suppression des fallbacks des contrôleurs déjà permission-aware,
  capacités patient dédiées et tests matriciels.
- Chaque lot conserve les contrats métier et ajoute des tests 200/403 par rôle.

## Catalogue ajouté

Les permissions nouvelles sont limitées aux actions qui ne possédaient aucun équivalent
canonique : visites, documents, accueil, webhooks, configuration spatiale, relances de
créances, gestion globale des disponibilités et capacités du portail patient. Le démarrage RBAC synchronise ces permissions et
les affectations des rôles système ; les rôles personnalisés restent inchangés.

## Impacts

- DB : aucun pour le P0 ; migrations possibles uniquement si de nouvelles permissions sont cataloguées.
- API : aucun payload modifié ; politique d'autorisation renforcée.
- UI : aucun nouveau composant, texte, thème ou token.
- Configuration : aucune variable ajoutée ; la variable existante `JOPRELYS_LAB_INTEGRATION_API_KEY` n'a plus de valeur de secours et une configuration vide ferme l'intégration.
- Performance : rechargement RBAC après rotation de jeton, acceptable et sécurisé.
