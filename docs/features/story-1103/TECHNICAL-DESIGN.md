# STORY-1103 — Design Technique

## 1. Architecture

```
api/
  DrugStockController.java        ← REST endpoints (PHARMACIEN, ADMIN_CLINIQUE)
  DrugStockResponse.java          ← DTO record réponse
  CreateDrugStockRequest.java     ← DTO record requête (validation Bean)

application/
  DrugStockService.java           ← Logique métier (upsert, décrémentation, alertes)

infrastructure/persistence/
  DrugStockEntity.java            ← Entité JPA (@TenantId, @Version)
  DrugStockRepository.java        ← Spring Data JPA

db/migration/
  V17__create_drug_stocks_table.sql   ← Flyway
```

## 2. Couche persistence

### DrugStockEntity
- `@TenantId` sur `organizationId` : isolation automatique par Hibernate Multi-Tenancy
- `@Version` sur `version` : verrouillage optimiste JPA — en cas de mise à jour concurrente, Spring lance une `OptimisticLockException` qui se transforme en `409 Conflict`
- `@PrePersist` / `@PreUpdate` pour les timestamps `createdAt` / `updatedAt`

### DrugStockRepository
- `findAllByOrderByDrugNameAsc()` : tri alphabétique pour la liste principale
- `findByDrugNameIgnoreCase()` : requête JPQL `LOWER(...)` pour l'upsert
- `findBelowThreshold()` : JPQL comparant `quantityAvailable <= minimumThreshold`

## 3. Couche service

### Upsert (createOrUpdateStock)
```
findByDrugNameIgnoreCase(drugName)
  → existe  → modifier les champs non-null
  → absent  → new DrugStockEntity(...)
drugStockRepository.save(entity)
```
**Décision** : organizationId est `null` à la construction — Hibernate injecte le tenant courant via le filter `@TenantId` automatiquement lors du `save()`.

### Décrémentation (checkAndDecrementStock)
```
findByDrugNameIgnoreCase(drugName)
  → absent  → return (stock optionnel)
  → présent
      quantityAvailable < quantityRequired → 409 Conflict
      sinon → entity.decrementStock(qty) → save
```
Le `@Version` garantit qu'une transaction concurrente qui modifie le même stock sera rejetée par Hibernate.

## 4. API REST

| Méthode | URL | Sécurité | Description |
|---|---|---|---|
| GET | `/api/pharmacy/stocks` | PHARMACIEN, ADMIN_CLINIQUE | Liste complète triée par nom |
| POST | `/api/pharmacy/stocks` | PHARMACIEN, ADMIN_CLINIQUE | Création ou mise à jour (upsert) |
| GET | `/api/pharmacy/stocks/alerts` | PHARMACIEN, ADMIN_CLINIQUE | Médicaments sous le seuil minimum |

## 5. Gestion des erreurs

| Situation | Code HTTP | Message |
|---|---|---|
| Quantité insuffisante | 409 Conflict | `"Stock insuffisant pour {nom} (disponible: X, demandé: Y)"` |
| Accès sans rôle autorisé | 403 Forbidden | (Spring Security) |
| `drugName` absent | 400 Bad Request | `"Le nom du médicament est obligatoire."` |
| Conflit d'écriture concurrent | 409 Conflict | (OptimisticLockException via @Version) |

## 6. Décisions d'architecture (ADR)

- **Stock optionnel** : un médicament non référencé en stock ne bloque pas la délivrance (décision de simplification v1 — peut être rendue obligatoire via configuration future).
- **Upsert par nom** : pour la pharmacie de clinique, le nom du médicament est utilisé comme clé naturelle (insensible à la casse). Deux médicaments homonymes dans le même tenant sont fusionnés.
- **@TenantId sans ID explicite** : contrairement aux entités héritées (`PrescriptionEntity`), `DrugStockEntity` ne reçoit pas l'`organizationId` dans son constructeur — Hibernate l'injecte automatiquement, ce qui est plus sûr et conforme à la documentation Hibernate 6.
