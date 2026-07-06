# Conception Technique : Examens médicaux conformes CDC (STORY-1905)

## 1. Stack impactée
- **Backend** : Spring Boot, JPA, Flyway (migration V34), Hibernate Multi-tenancy
- **Frontend** : Angular (formulaires réactifs, types, i18n, services d'API)

## 2. Modèle de données
### Nouveaux Éléments et Tables
- **Migration Flyway `V34__lab_orders_cdc_alignment.sql`** :
  - Création de la table `lab_order_items` :
    ```sql
    CREATE TABLE lab_order_items (
        id UUID PRIMARY KEY,
        lab_order_id UUID NOT NULL REFERENCES lab_orders(id) ON DELETE CASCADE,
        exam_name VARCHAR(255) NOT NULL
    );
    ```
  - Migration des données depuis `lab_orders.exams` (CSV) vers la table `lab_order_items`.
  - Ajout de la colonne `source_organization_id UUID REFERENCES organizations(id)` à `lab_orders`.
  - Mise à jour de `source_organization_id` avec `organization_id` pour l'historique des données.
  - Suppression de la colonne `exams` de `lab_orders`.

### Mappings JPA
- **`ExamType` (Enum)** :
  ```java
  public enum ExamType {
      LABORATOIRE, IMAGERIE, CARDIOLOGIE, ORL, OPHTALMOLOGIE, AUTRE
  }
  ```
- **`LabOrderStatus` (Enum)** :
  ```java
  public enum LabOrderStatus {
      REQUESTED, AWAITING_PAYMENT, PAID, SAMPLE_COLLECTED, IN_PROGRESS, RESULT_AVAILABLE, VALIDATED, CANCELLED
  }
  ```
- **`LabOrderEntity`** :
  - `examType` de type `ExamType` annoté `@Enumerated(EnumType.STRING)`
  - `status` de type `LabOrderStatus` annoté `@Enumerated(EnumType.STRING)`
  - `sourceOrganizationId` (UUID)
  - Relation `@OneToMany(mappedBy = "labOrder", cascade = CascadeType.ALL, orphanRemoval = true) List<LabOrderItemEntity> items`
- **`LabOrderItemEntity`** (Nouveau) :
  - Table `lab_order_items`
  - Clé primaire UUID
  - `labOrder` (ManyToOne, fetch = FetchType.LAZY)
  - `examName` (String)

## 3. Contrat API et DTOs
- **`CreateLabOrderRequest`** :
  - `List<String> exams`
  - `ExamType examType` (Enum)
  - `UUID targetOrganizationId`
  - `String reason`
  - `String priority`
- **`LabOrderResponse`** :
  - `List<String> exams` (mappé depuis les entités enfants)
  - `String examType`
  - `UUID sourceOrganizationId`
  - `UUID targetOrganizationId`
  - `String status`

## 4. Sécurité & Contrôle d'accès
- **`LabOrderService.updateStatus(...)`** :
  - Reçoit l'adresse email de l'opérateur (depuis `Authentication.getName()`).
  - Charge le compte utilisateur de l'opérateur.
  - Vérifie que si `targetOrganizationId` est renseigné sur la demande d'examen, il correspond bien à l'organisation de l'opérateur (`user.getOrganizationId()`).
  - Lève une exception `AccessDeniedException` en cas d'incohérence.
