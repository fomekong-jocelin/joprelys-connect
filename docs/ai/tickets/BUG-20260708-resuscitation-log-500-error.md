# BUG-20260708 — Erreur 500 (NonUniqueObjectException) lors de l'enregistrement d'un acte de réanimation

## Statut : RÉSOLU ✅

**Date** : 2026-07-08
**Priorité** : CRITIQUE (empêche d'enregistrer des soins dans le dossier d'urgence)
**Mode d'intervention** : Diagnostic + Engineering
**Composant** : `backend/src/main/java/com/joprelys/backend/emergency/application/EmergencyService.java`
**SemVer** : PATCH

---

## Symptôme

Lorsqu'un médecin ou un infirmier tente d'ajouter un acte de réanimation (par exemple, "Remplissage (bolus)" de 45 ml) à un dossier d'urgence depuis le tiroir de soins, l'API backend renvoie une erreur HTTP 500 avec la cause racine suivante :
`org.hibernate.NonUniqueObjectException: A different object with the same identifier value was already associated with this persistence context for entity [com.joprelys.backend.emergency.infrastructure.persistence.ResuscitationLogEntity with id '...']`

---

## Cause racine

Dans la méthode `addResuscitationLog` de `EmergencyService` :
1. L'entité `ResuscitationLogEntity` est instanciée avec un UUID généré dans son constructeur.
2. L'entité est associée à l'urgence via `emergency.addResuscitationLog(log)`.
3. Comme la relation entre l'urgence et ses actes est configurée en cascade complète (`cascade = CascadeType.ALL`), Hibernate commence automatiquement à suivre cette entité comme devant être persistée.
4. L'appel explicite à `resuscitationLogRepository.save(log)` a ensuite été fait. Comme l'ID était pré-assigné, Spring Data JPA considérait que l'entité n'était pas nouvelle et appelait `entityManager.merge(log)`.
5. Le `merge()` tentait de fusionner la nouvelle entité avec celle déjà suivie en cascade dans le persistence context de Hibernate, levant ainsi la `NonUniqueObjectException`.

---

## Correction appliquée

1. Retrait de l'appel redondant `resuscitationLogRepository.save(log)` dans `EmergencyService.java`. L'acte est persisté par cascade automatique à la fin de la transaction `@Transactional`.
2. Création du test d'intégration `EmergencyControllerTest.java` pour tester l'endpoint `POST /api/emergencies/{id}/resuscitation`.
3. Correction de l'ordre de nettoyage dans `setUp()` du test d'intégration pour vider `visits` avant `patients` afin d'éviter les violations de contrainte de clé étrangère au niveau de la suite complète de tests de l'application.

---

## Fichiers modifiés

- `backend/src/main/java/com/joprelys/backend/emergency/application/EmergencyService.java`
- `backend/src/test/java/com/joprelys/backend/emergency/api/EmergencyControllerTest.java`

---

## Reste à faire

- Aucun. Tous les tests unitaires et d'intégration de l'application (247 tests au total) passent désormais au vert avec succès.
