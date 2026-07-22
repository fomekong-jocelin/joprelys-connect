# HOS-DIS-001-A — Empêcher la libération du lit avant le départ physique

## Métadonnées

- **Epic** : EPIC-0027 — Organisation hospitalière, capacité et parcours patient
- **Story** : HOS-DIS-001 — Processus de sortie hospitalière
- **Audit** : AUDIT-20260721
- **Écarts réduits** : GAP-008, GAP-006 et GAP-016
- **Priorité** : Critique / phase 0
- **Statut** : IMPLEMENTED / QA AUTOMATISÉE VALIDÉE
- **PR** : #103

## Problème

L'endpoint historique `POST /api/hospitalizations/{id}/discharge` réalisait dans une seule transaction :

1. la décision médicale de sortie ;
2. la clôture du séjour ;
3. la génération du document final ;
4. la libération de l'affectation du lit ;
5. le passage du lit en nettoyage.

Un patient administrativement ou physiquement encore présent pouvait donc perdre son lit immédiatement après la seule décision médicale.

## Solution

### Décision médicale

```text
POST /api/hospitalizations/{id}/discharge
Permission : HOSPITALIZATION_DISCHARGE_DECIDE
```

Cette commande enregistre :

- le diagnostic de sortie ;
- les consignes ;
- le caractère contre avis médical ;
- l'auteur et la date de décision.

Elle ne modifie pas le statut `EN_COURS`, ne renseigne pas `dischargedAt`, ne clôt pas l'affectation et ne modifie pas le lit.

### Départ physique

```text
POST /api/hospitalizations/{id}/physical-departure
Permission : HOSPITALIZATION_PHYSICAL_DEPARTURE_CONFIRM
```

Cette commande exige une confirmation explicite et une décision médicale préalable. Elle seule :

- renseigne le départ physique et son auteur ;
- clôt le séjour en `SORTI` ou `SORTI_CONTRE_AVIS` ;
- renseigne `dischargedAt` avec la date réelle du départ ;
- génère le document final ;
- clôt l'affectation active ;
- place le lit en nettoyage.

## Invariants

- Un séjour décidé sortant reste actif jusqu'au départ physique.
- Le lit reste occupé et indisponible jusqu'au départ physique.
- Une nouvelle admission du même patient reste refusée.
- Le transfert est refusé après la décision médicale.
- Une décision ne peut pas être enregistrée deux fois.
- Un départ ne peut pas être confirmé deux fois.
- Le domaine refuse toute finalisation sans `physicalDepartureAt`.
- Le médecin ne peut pas confirmer le départ physique.
- L'agent d'hygiène et le technicien de maintenance ne peuvent pas confirmer le départ.

## Migration V83

Colonnes ajoutées à `hospitalizations` :

- `discharge_decided_at` ;
- `discharge_decided_by` ;
- `discharge_against_medical_advice` ;
- `physical_departure_at` ;
- `physical_departure_by` ;
- `physical_departure_note`.

Les sorties historiques sont considérées comme décidées et physiquement réalisées à leur ancien `discharged_at`.

## Interface

- badge « Sortie médicale décidée » ;
- indication claire que le patient et le lit restent actifs ;
- disparition des actions transfert et décision ;
- action de départ physique conditionnée par permission ;
- dialogue avec avertissement, note facultative, case de confirmation et état de soumission.

## Critères d'acceptation

- [x] La décision médicale ne libère pas le lit.
- [x] Le séjour reste `EN_COURS` après la décision.
- [x] `dischargedAt` reste vide avant le départ réel.
- [x] L'affectation reste active après la décision.
- [x] Le lit reste `OCCUPIED` après la décision.
- [x] Le transfert est refusé après la décision.
- [x] Le départ sans décision préalable est refusé.
- [x] Le médecin reçoit 403 sur le départ physique.
- [x] Le responsable hospitalisation peut confirmer le départ.
- [x] La confirmation clôt l'affectation et place le lit en nettoyage.
- [x] Le PDF final n'est disponible qu'après le départ physique.
- [x] Le contre avis médical n'est final qu'après le départ réel.
- [x] Les sorties historiques sont migrées sans perte.

## Validation automatisée

CI **Joprelys Connect — CI Pipeline**, run **934** :

- backend Maven `clean verify` strict : succès ;
- migration V83 dans la chaîne H2 et PostgreSQL 16/Testcontainers : succès ;
- décision médicale sans clôture du séjour ni de l'affectation : succès ;
- refus du départ sans décision préalable : succès ;
- refus du départ au médecin et contrôle de la permission dédiée : succès ;
- confirmation par le responsable hospitalisation : succès ;
- libération de l'affectation, passage du lit en nettoyage et PDF uniquement au départ réel : succès ;
- blocage du transfert après la décision : succès ;
- tests Angular : succès ;
- build Angular de production : succès.

Le premier passage frontend a détecté une erreur de typage limitée au test du dialogue. Le tableau de boutons est désormais explicitement typé `HTMLButtonElement[]` ; aucune règle métier ni permission n'a été assouplie.

## Risques résiduels

- La clearance administrative et financière n'est pas encore séparée.
- L'annulation ou la correction append-only d'une décision médicale reste à concevoir.
- La remise des médicaments, documents et effets personnels n'est pas modélisée.
- Le nettoyage est encore un état, pas une tâche de turnover assignée et horodatée.
- Le rôle autorisé à confirmer le départ doit être validé par les établissements et le RSSI.

## Commandes de validation

```bash
cd backend
./mvnw -Dtest=HospitalizationControllerTest,HospitalizationControllerAuthorizationTest,RbacCatalogBedOperationalStatusPermissionTest,SpatialServiceDischargeDecisionTransferTest test
./mvnw clean verify -Dspring.profiles.active=test

cd ../web
npm test -- --run
npm run build
```

## Definition of Done

- [x] migration et backfill ajoutés ;
- [x] modèle de données étendu ;
- [x] orchestration décision/départ ajoutée ;
- [x] permission dédiée ajoutée ;
- [x] garde-fou de domaine ajouté ;
- [x] transfert post-décision interdit ;
- [x] interface et dialogue ajoutés ;
- [x] tests unitaires, intégration et sécurité ajoutés ;
- [x] CI complète verte ;
- [x] contrat API actualisé ;
- [x] matrice d'audit actualisée après QA automatisée ;
- [ ] recette métier avec médecin et responsable hospitalisation ;
- [ ] validation RSSI et direction hospitalière.
