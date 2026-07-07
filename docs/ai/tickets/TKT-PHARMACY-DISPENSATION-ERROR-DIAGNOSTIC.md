# TKT-PHARMACY-DISPENSATION-ERROR-DIAGNOSTIC — Causes d'échec de la dispensation en pharmacie

**Mode** : Diagnostic  
**Date** : 2026-07-07  
**Statut** : ✅ DONE  
**Profil** : Tech Lead / Backend  
**Impact version** : None (Diagnostic uniquement)

---

## 1. Description du problème

Lorsqu'un pharmacien se connecte, recherche une ordonnance, remplit le formulaire de dispensation (quantités distribuées, substitutions éventuelles) et clique sur "Enregistrer", l'appel à l'API `/api/public/pharmacy/prescriptions/dispense` échoue avec une erreur.

---

## 2. Analyse technique & Causes racines probables

Après analyse minutieuse des codes source frontend Angular (`pharmacy-dispensation-panel.component.ts`), backend Spring Boot (`PharmacyService.java`, `PharmacyController.java`) et du schéma de base de données PostgreSQL, quatre causes principales et hautement probables ont été identifiées :

### A. Parsing strict des quantités prescrites (`parseQuantity` retournant 0)
* **Symptôme** : Message d'erreur HTTP 400 Bad Request `"Quantité dispensée excède la quantité prescrite pour le médicament : ..."`
* **Cause racine** : 
  Dans `PharmacyService.java`, la quantité prescrite d'un médicament est analysée depuis un champ textuel `item.getQuantity()` :
  ```java
  private int parseQuantity(String quantity) {
      if (quantity == null || quantity.isBlank()) return 0;
      java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\d+").matcher(quantity);
      if (matcher.find()) {
          return Integer.parseInt(matcher.group());
      }
      return 0;
  }
  ```
  Si le médecin a saisi une prescription avec des instructions de quantité textuelles ou sans chiffres au début (ex: `"Selon besoin"`, `"Une boîte"`, `"A volonté"`), `parseQuantity()` renvoie `0`.
  Par conséquent, la validation stricte :
  ```java
  if (qtyAlreadyDispensed + dispItem.quantityDispensed() > qtyPrescribed) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantité dispensée excède la quantité prescrite...");
  }
  ```
  échoue dès que le pharmacien essaie de dispenser une quantité supérieure à `0` (car `0 + X > 0` est toujours vrai).

### B. Dépassement de la longueur maximale des colonnes SQL (Nom/Licence)
* **Symptôme** : Message d'erreur HTTP 500 Internal Server Error ou DataIntegrityViolationException.
* **Cause racine** :
  La table `prescription_dispensations` stocke le nom de la pharmacie et la licence du pharmacien avec des contraintes de taille strictes en base de données (défini dans `V14__add_pharmacy_fields_and_tables.sql`) :
  - `pharmacy_name` : `VARCHAR(200)`
  - `pharmacist_license` : `VARCHAR(50)`
  Si l'utilisateur saisit un nom de pharmacie ou un numéro de licence qui dépasse ces seuils, la base de données rejette l'insertion.

### C. Problème de restriction Multi-Tenant ou d'Audit Log
* **Symptôme** : Erreur 500 ou `EmptyResultDataAccessException`.
* **Cause racine** :
  Lors de la dispensation, l'audit log tente de récupérer le `patientId` globalement par SQL pour s'enregistrer :
  ```java
  String patientIdSql = "SELECT v.patient_id FROM prescriptions pr ... WHERE pr.id = ?";
  UUID patientId = jdbcTemplate.queryForObject(patientIdSql, UUID.class, prescription.getId());
  ```
  Si la prescription en question a un problème de liens (ex: consultation ou visite orpheline), la requête ne renvoie aucun résultat, ce qui provoque une exception `EmptyResultDataAccessException` bloquant la transaction.

---

## 3. Plan d'actions recommandé (Exécuté)

1. **Rendre la validation de la quantité plus tolérante** :
   Modifier `PharmacyService.java` pour que si la quantité prescrite parsée `qtyPrescribed` vaut `0` (indiquant une quantité textuelle non quantifiable de manière stricte par expression régulière), le système n'applique pas le contrôle bloquant ou considère la limite comme infinie/optionnelle.
2. **Améliorer le message d'erreur** au niveau de l'IHM frontend pour afficher explicitement la raison de l'échec retournée par le serveur.
3. **Ajouter des logs de diagnostic détaillés** dans `PharmacyService.dispensePrescription` pour tracer toute exception inattendue.

---

## 4. Résolution apportée (2026-07-07)

1. **Tolérance sur les quantités textuelles** :
   Dans `PharmacyService.java`, la condition de validation a été modifiée pour autoriser la dispensation sans blocage si la quantité prescrite parsée vaut `0` (c'est-à-dire si le médecin a écrit des instructions de posologie en texte libre telles que "Selon besoin").
2. **Robustesse face au dépassement de taille de colonne** :
   - Ajout d'annotations `@jakarta.validation.constraints.Size(max = 200)` pour le nom de la pharmacie et `@Size(max = 50)` pour la licence dans `PharmacyDispenseRequest.java` pour renvoyer une erreur 400 propre en cas de saisie trop longue.
   - Troncature automatique de sécurité dans `PharmacyService.java` à 200 caractères pour `pharmacyName` et 50 pour `pharmacistLicense` avant persistance en base.
3. **Robustesse de l'Audit Log** :
   - Sécurisation de l'extraction SQL de `patientId` dans `PharmacyService.java` avec un bloc `try-catch` capturant `EmptyResultDataAccessException`, et une tentative de secours via `prescription.getVisitId()`. Cela évite qu'un problème d'audit log ne fasse planter l'enregistrement de la dispensation.

