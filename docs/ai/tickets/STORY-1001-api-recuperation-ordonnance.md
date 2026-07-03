# STORY-1001 — API de récupération sécurisée d'ordonnance

> Fichier obligatoire pour le ticket de la User Story STORY-1001.

## 1. Objectif

Permettre aux pharmaciens externes de récupérer de manière sécurisée le contenu d'une ordonnance via son numéro unique (`prescriptionNumber`) et son code PIN de sécurité à 4 chiffres.

## 2. Critères d'acceptation

- [ ] **Modifications de la structure de l'Ordonnance** :
  - Ajouter les champs `pin_code` (VARCHAR 4), `status` (VARCHAR 20, valeurs `DRAFT, ACTIVE, PARTIALLY_DISPENSED, FULLY_DISPENSED, EXPIRED, CANCELLED`), et `expires_at` (TIMESTAMP WITH TIME ZONE) dans `prescriptions`.
  - Lors de la création d'une ordonnance par le médecin (lors de la clôture de visite), générer automatiquement un code PIN aléatoire de 4 caractères alphanumériques (ex: `8F2A`) et définir la date d'expiration à `issued_at + 3 mois` par défaut.
- [ ] **Endpoint public de vérification** :
  - `POST /api/public/pharmacy/prescriptions/verify` accessible sans authentification JWT globale.
  - La requête doit contenir `prescriptionNumber` et `pinCode`.
  - Si le code PIN est incorrect, renvoyer `401 Unauthorized`.
  - Si l'ordonnance est expirée, annulée, ou révoquée, renvoyer son état avec un code d'erreur adéquat.
  - Si valide, renvoyer les détails de l'ordonnance (patientName, doctorName, issuedAt, expiresAt, status) et la liste des médicaments prescrits avec la quantité déjà dispensée (calculée via les dispensations passées).
- [ ] **Sécurité (Lockout brute-force)** :
  - Implémenter un mécanisme de blocage temporaire (Lockout) après 3 tentatives infructueuses de code PIN pour une ordonnance donnée.
- [ ] **Traçabilité** :
  - Enregistrer un log d'audit `PHARMACY_VERIFIED` dans la table `audit_logs`.

## 3. Pilotage projet

| Champ | Valeur |
|---|---|
| Epic parent | EPIC-0010 — Dispensation en Pharmacie & Gestion des Prescriptions |
| User story parent | STORY-1001 |
| Sprint cible | SPRINT-0004 |
| Priorité business | P1 |
| Complexité | M |
| Story points | 2 |
| Profil recommandé | Intermédiaire |
| Effort estimé senior | 0.4j |
| Effort estimé intermédiaire | 0.6j |
| Effort estimé junior | 1.0j |
| Responsable | Antigravity |
| Reviewer obligatoire | Lead |
| Risque fonctionnel | Moyen |
| Risque technique | Moyen |
| Dépendances | Aucun |
| Bloquants connus | Aucun |

## 4. Contexte analysé

- [x] `AGENTS.md` lu
- [x] `SKILL.md` lu
- [x] `PROJECT-TRACKING.md` lu
- [x] `CHANGELOG.md` lu
- [x] Spécifications techniques `TECHNICAL-DESIGN.md` de la pharmacie analysées.

## 5. Hypothèses

- Le code PIN est généré et stocké en clair lors de la prescription.
- Les requêtes se font via des API publiques car les pharmaciens partenaires externes ne se connectent pas au portail clinique interne.

## 6. Risques et impacts

| Risque | Impact | Mitigation |
|---|---|---|
| Brute force de code PIN | Moyen | Blocage temporaire après 3 échecs consécutifs. |

## 7. Action plan

- [ ] **Base de données** : Créer la migration Flyway `V14__add_pharmacy_fields_and_tables.sql` pour altérer `prescriptions` et créer `prescription_dispensations` et `dispensation_items`.
- [ ] **Modèle JPA** : Mettre à jour `PrescriptionEntity` et `PrescriptionItemEntity`. Créer `PrescriptionDispensationEntity` et `DispensationItemEntity`.
- [ ] **Logique métier** :
  - Mettre à jour `PrescriptionService` pour générer le code PIN et l'expiration de 3 mois à la création.
  - Implémenter le service de vérification `PharmacyService` avec le mécanisme de lockout en mémoire ou en DB.
- [ ] **Controller** : Créer `PharmacyController` avec l'endpoint public `/verify`.
- [ ] **Tests** : Écrire `PharmacyControllerTest.java` couvrant la vérification, le code PIN incorrect, l'expiration et le lockout.
- [ ] Mettre à jour `PROJECT-TRACKING.md` et `CHANGELOG.md`.

## 8. Implémentation réalisée

- Correction associee : `TICKET-1010` ajoute la migration additive `V15__repair_pharmacy_schema_validation.sql` pour garantir que les colonnes/tables pharmacie attendues par Hibernate existent aussi sur les bases locales ayant applique une variante incomplete de `V14`.

## 9. Suivi d'exécution

*(À remplir lors de la réalisation)*

## 10. Tests et vérifications

- `mvn test -DskipTests` execute avec succes apres ajout de la migration de reparation de schema.
- `V15__repair_pharmacy_schema_validation.sql` verifie par execution SQL H2 en mode PostgreSQL sur un schema `prescriptions` ancien.
- `mvn test` complet reste a relancer dans un environnement Maven stable.

## 11. Documentation

- Ajout de `docs/features/pharmacy-dispensation/DATA-MODEL.md`.

## 12. Reste à faire

- Finaliser les endpoints publics pharmacie et les tests de verification/dispensation.
- Relancer le backend sur la base PostgreSQL locale ayant declenche l'erreur de demarrage.

## 13. Statut final

Statut : TODO

## 14. Impact version / SemVer

| Champ | Valeur |
|---|---|
| Changement livrable | Oui |
| Type de bump | MINOR |
| Justification | Ajout d'une nouvelle fonctionnalité d'API publique pour la pharmacie. |
| Breaking change | Non |
| Migration DB | Oui |
| Changement API | Oui |
| Impact Angular | Non |
| Impact Flutter | Non |
| Changelog requis | Oui |
| Release note requise | Oui |
