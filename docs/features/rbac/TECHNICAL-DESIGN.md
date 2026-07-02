# Conception Technique — Contrôle d'Accès Basé sur les Rôles (RBAC)

## 1. Stack et Impacts

- **Back-end** : Spring Boot 4.1.0, Spring Security (Java 21).
  * Fichiers modifiés/créés : `SecurityConfig.java`, création de `ClinicController.java` (contrôleur de test d'autorisation).
- **Front-end** : Angular (Tailwind CSS v4).
  * Fichiers modifiés/créés : `app.routes.ts`, création d'un guard `role.guard.ts` (ou `auth.guard.ts`), création de `DashboardComponent` dynamique.

## 2. Architecture Technique (Backend)

### 2.1 Activation de la Sécurité Globale sur les Méthodes
Dans la classe de configuration [SecurityConfig.java](file:///C:/MES-APPLICATIONS/joprelys-connect/backend/src/main/java/com/joprelys/backend/auth/config/SecurityConfig.java), nous activons la sécurité par méthode avec l'annotation suivante :
```java
@Configuration
@EnableMethodSecurity // Activation du RBAC sur les méthodes via @PreAuthorize
public class SecurityConfig { ... }
```

### 2.2 Endpoints de validation RBAC
Pour valider le fonctionnement et tester le cloisonnement, un contrôleur [ClinicController.java](file:///C:/MES-APPLICATIONS/joprelys-connect/backend/src/main/java/com/joprelys/backend/clinic/api/ClinicController.java) est créé :
* `GET /api/clinic/admin` : Restreint par `@PreAuthorize("hasRole('ADMIN_JOPRELYS')")`
* `GET /api/clinic/medecin` : Restreint par `@PreAuthorize("hasAnyRole('MEDECIN', 'ADMIN_JOPRELYS')")`
* `GET /api/clinic/pharmacien` : Restreint par `@PreAuthorize("hasAnyRole('PHARMACIEN', 'ADMIN_JOPRELYS')")`

### 2.3 Mapping des Rôles
Le rôle extrait du token JWT (ex: `ADMIN_JOPRELYS`) est automatiquement préfixé par `ROLE_` par le filtre `JwtAuthenticationFilter` pour correspondre à la convention d'autorité par défaut de Spring Security :
```java
var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + claims.role()));
```
Cela permet d'utiliser directement `hasRole('ADMIN_JOPRELYS')` dans les expressions `@PreAuthorize`.

## 3. Architecture Technique (Frontend)

### 3.1 Gardien de Routes (`role.guard.ts`)
Un guard Angular fonctionnel vérifie l'existence de la session de l'utilisateur et valide son rôle par rapport au rôle requis spécifié dans les données de la route (`data: { expectedRoles: [...] }`) :
```typescript
export const roleGuard: CanActivateFn = (route, state) => {
  const tokenStorage = inject(AuthTokenStorageService);
  const router = inject(Router);
  const session = tokenStorage.session();
  
  if (!session) {
    return router.parseUrl('/');
  }
  
  const expectedRoles = route.data['expectedRoles'] as string[];
  if (expectedRoles && !expectedRoles.includes(session.role)) {
    return router.parseUrl('/unauthorized');
  }
  
  return true;
};
```

### 3.2 Configuration des Routes (`app.routes.ts`)
Nous configurons le routage pour inclure `/dashboard`, `/unauthorized` et les routes filles selon le profil de rôle attendu :
```typescript
export const routes: Routes = [
  { path: '', loadComponent: () => import('./auth/login.component').then(m => m.LoginComponent) },
  { 
    path: 'dashboard', 
    loadComponent: () => import('./clinic/dashboard.component').then(m => m.DashboardComponent),
    canActivate: [roleGuard]
  },
  { path: 'unauthorized', loadComponent: () => import('./auth/unauthorized.component').then(m => m.UnauthorizedComponent) }
];
```

## 4. Stratégie de Tests

### 4.1 Backend (Tests d'intégration)
Création d'une classe de tests d'intégration `ClinicControllerTest` simulant l'envoi de requêtes GET avec différents jetons JWT ou d'authentifications Mockées pour valider :
* L'accès autorisé (HTTP `200 OK`)
* L'accès rejeté (HTTP `403 Forbidden` ou `401 Unauthorized`)

### 4.2 Frontend (Tests Unitaires)
Tests du `roleGuard` sous Vitest :
* Retourne `true` si la session est valide et le rôle correspond.
* Redirige vers `/` si aucune session n'est active.
* Redirige vers `/unauthorized` si le rôle ne fait pas partie de la liste autorisée.

## 5. Impact SemVer

* **Bump** : MINOR
* **Justification** : Ajout du support de contrôle d'accès multi-rôles et création des points d'accès restreints pour le cabinet clinique.
