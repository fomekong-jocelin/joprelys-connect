# Conception Technique — Gestion de la Clinique Pilote & Multi-tenant (STORY-0201)

## 1. Architecture Multi-tenant

Nous choisissons une architecture **multi-tenant logique par discrimination de colonne** :
* Une table unique `organizations` stocke les informations de chaque clinique.
* Toutes les tables dépendantes d'une clinique (dont la table `users`) possèdent une colonne de discrimination `organization_id` (clé étrangère vers `organizations`).
* **Isolation et sécurité** : Toutes les requêtes SQL (lectures et écritures) sur des entités cliniques doivent être interceptées ou filtrées en fonction du `organization_id` de l'utilisateur connecté.

## 2. Modèle de Données (Base de données)

### 2.1 Table `organizations`
Création d'une nouvelle migration Flyway `V2__create_organizations_table.sql` :
```sql
CREATE TABLE organizations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    phone VARCHAR(50),
    address VARCHAR(255),
    city VARCHAR(100) NOT NULL,
    logo_path VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
```

### 2.2 Modification de la table `users`
Ajout de la colonne `organization_id` sur la table `users` :
```sql
ALTER TABLE users ADD COLUMN organization_id UUID REFERENCES organizations(id);
```

## 3. Implémentation Backend (Spring Boot)

### 3.1 Entité `OrganizationEntity`
Une classe Hibernate `@Entity` représentant `organizations` avec ses repositories associés.

### 3.2 Filtrage Multi-tenant logique (Hibernate `@Filter`)
Pour assurer l'isolation automatique sans devoir ajouter des clauses `WHERE` dans chaque méthode de repository, nous activons le filtre Hibernate `@FilterDef` sur les entités cliniques :
```java
@Entity
@Table(name = "patients")
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = UUID.class))
@Filter(name = "tenantFilter", condition = "organization_id = :tenantId")
public class PatientEntity {
    // ...
}
```
Un intercepteur JPA ou un aspect Spring AOP active ce filtre sur la session Hibernate courante en y injectant le `organization_id` extrait du jeton JWT de l'utilisateur authentifié.

### 3.3 Endpoint REST d'Administration
Création de `OrganizationController` sous `/api/organizations` :
* `POST /api/organizations` (créer) - réservé à `ADMIN_JOPRELYS`
* `GET /api/organizations` (lister) - réservé à `ADMIN_JOPRELYS`
* `PUT /api/organizations/{id}/status` (bloquer/activer) - réservé à `ADMIN_JOPRELYS`

## 4. Implémentation Frontend (Angular)

### 4.1 Modèle de Données Frontend
Définition d'un modèle `Organization` dans `src/app/clinic/organizations.models.ts`.

### 4.2 Services API et Écran d'Administration
* Service `OrganizationApiService` pour interagir avec `/api/organizations`.
* Composant `OrganizationListComponent` et `OrganizationFormComponent` pour gérer les organisations cliniques de test.
* L'accès à ces routes est contrôlé par `roleGuard` avec `expectedRoles: ['ADMIN_JOPRELYS']`.

### 4.3 Shell applicatif, thème et mobile-first
* `AppShellComponent` centralise le header applicatif (logo, session utilisateur, rôle, avatar, déconnexion) et le footer.
* `PageHeaderComponent`, `CardComponent`, `ButtonComponent`, `InputComponent`, `AlertComponent`, `EmptyStateComponent` et `StatusBadgeComponent` servent de composants UI réutilisables.
* `ThemeService` applique le thème global via `data-theme`, avec `light` par défaut et `dark` disponible sans classes dispersées par écran.
* `APP_BRAND_CONFIG` centralise le nom de l'application, l'éditeur, les liens publics, la locale et le thème par défaut.
* `I18nService` fournit les libellés `fr` et `en` utilisés par l'écran organisations.
* `OrganizationTableComponent` rend des cartes verticales sur mobile (`md:hidden`) et conserve le tableau uniquement sur écran large (`md:block`).
* `OrganizationListComponent` orchestre l'état et les appels API ; il affiche soit le formulaire, soit la liste, jamais les deux en même temps.

## 5. Stratégie de Validation et Tests

### 5.1 Tests d'isolation (Non-interférence)
* Un test d'intégration `MultiTenantIsolationTest` simule :
  1. La création de deux organisations distinctes (A et B).
  2. L'insertion d'un patient sous l'organisation A.
  3. Une requête GET par un utilisateur associé à l'organisation B ne doit retourner aucun élément (isolation étanche).
  4. Une tentative de lecture directe de l'ID du patient de A par un token de B doit renvoyer une erreur `403 Forbidden` ou `404 Not Found`.

### 5.2 Tests de compilation et build
* Backend : `./mvnw test`
* Frontend : `npm run test -- --watch=false`
* Frontend : `npx tsc -p tsconfig.app.json --noEmit`
* Frontend : `npm run build` sous Node pair/LTS. Node.js `25.9.0` a produit un arrêt Angular CLI sans diagnostic exploitable après avertissement de version impaire.
