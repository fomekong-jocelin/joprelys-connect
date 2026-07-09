# BUG-20260708-spatial-wards-500-error — Erreur 500 / Crash de démarrage (mismatch Tenant ID) et Services vides lors de l'admission patient / occupation des lits

## Diagnostic

Lors de l'accès à la page d'occupation des lits (`/clinic/spatial`) ou lors de l'ouverture du tiroir d'hospitalisation d'un patient pour l'admettre, l'application levait une erreur HTTP 500 sur le endpoint `/api/spatial/wards` et les dropdowns de sélection de services / départements et lits libres étaient vides.

1. **Erreur 500 (Cause racine)** : L'erreur 500 provenait de la non-reconnaissance par Hibernate des nouvelles classes d'entités JPA spatiales (`WardEntity`, `RoomEntity`, `BedEntity`, `BedAssignmentEntity`) dans le conteneur Spring Boot en cours d'exécution. Les classes ont été modifiées et hot-swappées par Spring Boot DevTools sans redémarrage complet de la JVM, bloquant l'initialisation des métadonnées Hibernate.
2. **Services / Dropdowns vides (Cause de blocage métier)** : Même après redémarrage, si la base de données PostgreSQL de développement ne contient aucune ligne dans la table `wards`, le endpoint renvoie un tableau vide `[]`. De ce fait, l'utilisateur se retrouvait bloqué car il était impossible de sélectionner un service ou un lit libre pour soumettre le formulaire d'admission (qui est le seul moyen dynamique de peupler les tables spatiales).
3. **Crash de démarrage (Seeding automatique & multi-tenant)** : L'introduction du seeding automatique dans `AdminUserSeeder` a provoqué un crash de démarrage du backend (`DataIntegrityViolationException` causée par `PropertyValueException: assigned tenant id differs from current tenant id [32d7c853-54d5-4eb6-9949-19dbb8f33705 != 00000000-0000-0000-0000-000000000000]`).
   - *Pourquoi* : Lors de l'initialisation au démarrage de l'application (CommandLineRunner), il n'y a pas d'utilisateur authentifié (pas de session, pas de requête HTTP), donc `TenantContext.getTenantId()` est `null`, ce qui résout le locataire actuel en `DEFAULT_TENANT` (`00000000-0000-0000-0000-000000000000`).
   - *Le conflit* : Le seeder tente de persister des entités spatiales avec le locataire d'une clinique spécifique (ex. `32d7c853-54d5-4eb6-9949-19dbb8f33705`). Hibernate intercepte cette persistence et refuse l'enregistrement car la valeur du champ `@TenantId` diffère de celle résolue par le contexte de multi-tenancy.
   - *Rôle de @Transactional* : La méthode `run` du seeder était annotée avec `@Transactional`. Spring Boot a donc créé une transaction active *avant* le début de la méthode, liant définitivement le Session d'Hibernate au locataire par défaut `00000000-0000-0000-0000-000000000000`. L'appel à `TenantContext.setTenantId(org.getId())` en cours de transaction ne permettait pas à Hibernate de réévaluer le locataire de la session active lors du commit, déclenchant l'exception.

## Résolution

1. **Seeding automatique des Lits & Services** : Injection de `WardRepository`, `RoomRepository` et `BedRepository` dans [AdminUserSeeder.java](file:///C:/MES-APPLICATIONS/joprelys-connect/backend/src/main/java/com/joprelys/backend/auth/application/AdminUserSeeder.java).
2. **Fix du Crash de Démarrage (Tenant Context Seeding)** : Encapsulation de la boucle de seeding par clinique avec une configuration explicite temporaire du tenant courant dans le seeder :
   ```java
   try {
       TenantContext.setTenantId(org.getId());
       // ... vérification et persistence des entités spatiales ...
   } finally {
       TenantContext.clear();
   }
   ```
3. **Suppression de @Transactional sur la méthode run()** : La suppression de l'annotation `@Transactional` sur `run()` garantit qu'aucune transaction globale n'est démarrée au préalable. Chaque opération d'écriture en base ou requête par repository s'exécute ainsi sous sa propre session Hibernate, résolvant correctement et dynamiquement le Tenant ID courant défini dans le `TenantContext`.
4. **Mise à jour des Tests** : Modification de [AdminUserSeederTest.java](file:///C:/MES-APPLICATIONS/joprelys-connect/backend/src/test/java/com/joprelys/backend/auth/application/AdminUserSeederTest.java) pour mock les nouveaux repositories requis par le constructeur du seeder et ajout d'un test unitaire `shouldSeedSpatialDataWhenOrganizationsExist` pour valider le comportement du seeder avec des cliniques existantes.

## Fichiers modifiés

- [AdminUserSeeder.java](file:///C:/MES-APPLICATIONS/joprelys-connect/backend/src/main/java/com/joprelys/backend/auth/application/AdminUserSeeder.java) : Injection des repositories, configuration dynamique du context `TenantContext`, et retrait de `@Transactional` de la méthode `run`.
- [AdminUserSeederTest.java](file:///C:/MES-APPLICATIONS/joprelys-connect/backend/src/test/java/com/joprelys/backend/auth/application/AdminUserSeederTest.java) : Mocking des nouveaux repositories et ajout de tests unitaires pour le seeding spatial.
- [BUG-20260708-spatial-wards-500-error.md](file:///C:/MES-APPLICATIONS/joprelys-connect/docs/ai/tickets/BUG-20260708-spatial-wards-500-error.md) : Ce ticket de suivi.

## Statut

- [x] Diagnostic de l'erreur 500 et de l'absence de données spatiales.
- [x] Écriture de la méthode de peuplement par défaut dans le seeder.
- [x] Diagnostic du crash de démarrage multi-tenant lors du run du seeder.
- [x] Implémentation du fix avec `TenantContext.setTenantId` et `TenantContext.clear()`.
- [x] Suppression de l'annotation `@Transactional` globale pour permettre la réévaluation dynamique du tenant ID par session Hibernate.
- [x] Validation des tests unitaires et d'intégration (tous les tests au vert).
- [x] Nettoyage des fichiers temporaires de diagnostic.
- [x] Documentation et instruction de redémarrage.
