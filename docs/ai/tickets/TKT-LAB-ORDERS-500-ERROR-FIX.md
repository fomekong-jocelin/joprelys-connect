# TKT-LAB-ORDERS-500-ERROR-FIX — Résolution de l'erreur 500 sur la création des demandes d'examens de laboratoire

**Mode** : Engineering — Bug Fix  
**Date** : 2026-07-07  
**Statut** : ✅ DONE  
**Profil** : Backend  
**Impact version** : PATCH (Correction de bug)

---

## 1. Problématique & Cause racine

### Symptôme :
Lorsqu'un médecin tente d'enregistrer une consultation contenant des prescriptions d'examens de laboratoire, la consultation elle-même s'enregistre avec succès mais la création des demandes d'examens échoue avec une erreur `500 (Internal Server Error)` (visible dans la console réseau Chrome DevTools sur `POST /api/lab-orders`).

### Cause racine :
Dans `LabOrderService.java`, le numéro unique de demande d'examen est généré sous le format `EXAM-REQ-YYYYMMDD-XXXXXX` en comptant les demandes existantes via :
`long countToday = labOrderRepository.countByExamRequestNumberStartingWith(prefix);`

En raison du multi-tenancy basé sur le filtrage par tenant d'Hibernate, ce comptage standard `.countByExamRequestNumberStartingWith()` est automatiquement limité au tenant (clinique) de l'utilisateur connecté.
Si deux cliniques différentes (ex: Clinique A et Clinique B) émettent une demande d'examen le même jour :
- Chaque clinique obtient `0` comme compte de départ pour le jour J.
- Les deux cliniques génèrent et insèrent le même numéro de document : `EXAM-REQ-20260707-000001`.
- Puisque la colonne `exam_request_number` dans la table `lab_orders` possède une contrainte d'unicité globale (`UNIQUE` NOT NULL), l'insertion du doublon échoue avec une exception de violation de contrainte d'unicité, levant une erreur 500.

---

## 2. Résolution apportée

1. **Comptage global par requête native** :
   Ajout de la méthode `countByExamRequestNumberStartingWithGlobally(prefix)` annotée avec `@org.springframework.data.jpa.repository.Query` et configurée en `nativeQuery = true` dans `LabOrderRepository.java`. Cela permet de bypasser le filtre de tenant d'Hibernate lors du comptage.
2. **Génération de séquence unique** :
   Remplacement de l'appel standard par `countByExamRequestNumberStartingWithGlobally(prefix)` dans `LabOrderService.java`. Les numéros de demandes d'examens sont désormais calculés à l'échelle globale de la base de données, évitant toute collision inter-tenant tout en préservant le multi-tenant pour le reste des opérations logiques.

---

## 3. Fichiers modifiés

* [LabOrderRepository.java](file:///C:/MES-APPLICATIONS/joprelys-connect/backend/src/main/java/com/joprelys/backend/lab/infrastructure/persistence/LabOrderRepository.java)
* [LabOrderService.java](file:///C:/MES-APPLICATIONS/joprelys-connect/backend/src/main/java/com/joprelys/backend/lab/application/LabOrderService.java)
