# Conception Technique — Gestion des Visites, Constantes Vitales & IMC (EPIC-0004)

## 1. Modèle de Données (Base de données)

Nous concevons deux tables : `visits` (pour le cycle de vie) et `vitals` (pour les constantes de tri de tri initial).

### 1.1 Table `visits`
Gérée dans la migration V4.

### 1.2 Table `vitals`
Création d'une nouvelle migration Flyway `V5__create_vitals_table.sql` :
```sql
CREATE TABLE vitals (
    id UUID PRIMARY KEY,
    visit_id UUID NOT NULL UNIQUE REFERENCES visits(id) ON DELETE CASCADE,
    temperature DECIMAL(3,1),
    weight DECIMAL(4,1),
    height INTEGER,
    pulse INTEGER,
    systolic INTEGER,
    diastolic INTEGER,
    spo2 INTEGER,
    glycemia DECIMAL(3,2),
    respiratory_rate INTEGER,
    bmi DECIMAL(4,2),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_vitals_visit_id ON vitals (visit_id);
```

## 2. Implémentation Backend (Spring Boot)

### 2.1 Entité `VitalsEntity`
* `@Entity` et `@Table(name = "vitals")`.
* Relation avec la visite :
  ```java
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "visit_id", nullable = false, unique = true)
  private VisitEntity visit;
  ```
* L'isolation multi-tenant de `vitals` est héritée directement de la visite à laquelle elle est rattachée (la visite portant elle-même le filtre `@TenantId`).

### 2.2 Endpoints API REST (`VisitController`)
Nous étendons le contrôleur de visite pour y intégrer les opérations de constantes :
* `POST /api/visits/{id}/vitals` : Enregistre ou met à jour les constantes vitales de la visite.
  * Habilités : `AGENT_ACCUEIL`, `INFIRMIER`, `MEDECIN`, `ADMIN_CLINIQUE`.
  * Validation des bornes : Annotations jakarta Bean Validation (`@DecimalMin`, `@DecimalMax`, `@Min`, `@Max`).
  * Logique IMC : Calculé côté serveur lors de la persistance si le poids et la taille sont fournis.
* `GET /api/visits/{id}/vitals` : Récupère les constantes associées à la visite.
  * Habilités : `AGENT_ACCUEIL`, `INFIRMIER`, `MEDECIN`, `ADMIN_CLINIQUE`.

## 3. Implémentation Frontend (Angular)

### 3.1 Modèle de Données TypeScript
```typescript
export interface Vitals {
  temperature?: number;
  weight?: number;
  height?: number;
  pulse?: number;
  systolic?: number;
  diastolic?: number;
  spo2?: number;
  glycemia?: number;
  respiratoryRate?: number;
  bmi?: number;
}
```

### 3.2 Intégration Formulaire (`VitalsFormComponent`)
* Une boîte de dialogue modale s'ouvre depuis le dashboard lors du clic sur "Saisir constantes".
* Des formules réactives de validation sont utilisées.
* Calcul IMC réactif :
  ```typescript
  calculateBmi(weight: number, heightCm: number): number | null {
    if (!weight || !heightCm || heightCm <= 0) return null;
    const heightM = heightCm / 100;
    return parseFloat((weight / (heightM * heightM)).toFixed(2));
  }
  ```

### 3.3 Affichage des Constantes dans la file d'attente
* Si les constantes de la visite sont renseignées, le tableau affiche une icône ou un bouton permettant d'afficher une popover avec les constantes (Ex: Temp, Tension, SpO2, IMC).

## 4. Stratégie de Validation et Tests

* **Backend** : Écrire `VisitControllerTest` pour valider :
  1. L'insertion réussie de constantes dans une fourchette valide.
  2. Le rejet si les valeurs sortent des limites autorisées.
  3. L'isolation (impossible pour une clinique d'associer des constantes à une visite d'une autre clinique).
