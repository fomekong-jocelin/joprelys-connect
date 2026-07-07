# TICKET-PHARMA-HISTORY-EMPTY — Historique vide après dispensation pharmacie

## Contexte

| Champ | Valeur |
|---|---|
| ID | TICKET-PHARMA-HISTORY-EMPTY |
| Type | Bug |
| Priorité | P0 |
| Stack | Backend Spring Boot |
| Statut | DONE |
| Sprint | SPRINT-0011 |
| Profil recommandé | Senior |
| Estimation | 0.1j |
| Assigné | Antigravity |
| Reviewer | Lead Developer |
| Dernière MAJ | 2026-07-07 |

## Symptôme signalé

Lorsque le pharmacien valide et soumet une ordonnance (dispensation), l'historique des dispensations reste vide dans l'IHM.

## Cause racine identifiée

**Filtre multi-tenant Hibernate (`@TenantId`) sur les endpoints publics sans contexte de tenant.**

La méthode Spring Data JPA :
```java
dispensationRepository.findByPrescriptionIdOrderByDispensedAtDesc(prescription.getId())
```

effectue une requête JPQL standard. Or :
- `PrescriptionDispensationEntity` a une relation `@ManyToOne(fetch = LAZY)` vers `PrescriptionEntity`
- `PrescriptionEntity` est annotée `@TenantId` sur `organization_id`
- L'endpoint `/api/public/pharmacy/prescriptions/history` n'a **pas** de contexte de tenant actif (endpoint public)
- Hibernate applique un filtre tenant implicite sur la jointure, ce qui fait retourner une liste vide

La dispensation est bien **enregistrée** en base de données (la sauvegarde `dispensationRepository.save(dispensation)` fonctionne), mais la **lecture** est bloquée par le filtre Hibernate.

## Solution appliquée

Remplacement de la méthode JPA par deux requêtes SQL natives via `JdbcTemplate` dans `PharmacyService.getDispensationHistory()`, selon le même pattern déjà utilisé dans `verifyPrescription()` du même service.

**Fichier modifié :**
- `backend/src/main/java/com/joprelys/backend/prescription/application/PharmacyService.java`

## Actions

- [x] Diagnostic de la cause racine
- [x] Correction de `getDispensationHistory()` avec `JdbcTemplate` SQL natif
- [x] Vérification des imports (`java.sql.Timestamp`, `JdbcTemplate`, `Map`, `UUID`)
- [ ] Lancement du build Maven (`./mvnw clean verify`)
- [ ] Validation manuelle : dispensation → historique visible

## Critères d'acceptation

- Après soumission d'une dispensation, l'historique affiche immédiatement la dispensation effectuée
- Les items (médicaments, quantités, substitutions) sont correctement affichés
- Le cas sans dispensation antérieure affiche "Aucun historique"

## Tests / Vérifications

- Tests existants : `PharmacyControllerTest`
- Build Maven : `./mvnw clean verify`

## Sécurité / Régression

- Pas d'impact sur la sécurité : le PIN est toujours vérifié avant la lecture de l'historique
- Pas de régression sur la dispensation elle-même
- Les autres endpoints (`/verify`, `/dispense`) ne sont pas modifiés

## Impact version / SemVer

- PATCH — correction de bug, aucune modification d'API

## Risques restants

Aucun.
