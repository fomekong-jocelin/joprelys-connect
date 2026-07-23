# HOS-RBAC-001-B — Séparer les opérations sensibles d'hospitalisation

> **État actuel** : ce ticket décrit l'incrément livré par la PR #102. La dette qu'il signalait encore autour de `HOSPITALIZATION_MANAGE` a ensuite été réduite par HOS-RBAC-001-C (#107) puis supprimée définitivement par HOS-RBAC-001-D / #121 avec Flyway V86. Les mentions historiques ci-dessous décrivent l'état au moment de 001-B et ne constituent plus le modèle RBAC courant.

## Métadonnées

- **Epic** : EPIC-0027 — Organisation hospitalière, capacité et parcours patient
- **Story** : HOS-RBAC-001 — Autorisations hospitalières contextuelles
- **Audit** : AUDIT-20260721
- **Écart réduit** : GAP-016
- **Priorité** : Critique / phase 0
- **Estimation** : 5 SP / 4 à 6 jours
- **Statut** : IMPLEMENTED / QA AUTOMATISÉE VALIDÉE
- **PR** : #102

## Problème

Au moment de cet incrément, la permission historique `HOSPITALIZATION_MANAGE` autorisait simultanément l'admission, les notes, les soins, le transfert et la sortie. La permission `BED_OPERATIONAL_STATUS_MANAGE` regroupait capacité, nettoyage et maintenance.

Ces regroupements permettaient notamment :

- à un infirmier de décider une sortie médicale ;
- à un utilisateur de transfert de modifier des opérations techniques du lit ;
- à un agent d'hygiène de terminer une maintenance ;
- à un technicien de maintenance de valider un nettoyage ;
- à l'interface de présenter les mêmes actions à tous les gestionnaires du séjour.

## Solution

Ajouter quatre permissions distinctes :

- `HOSPITALIZATION_TRANSFER` ;
- `HOSPITALIZATION_DISCHARGE_DECIDE` ;
- `BED_CLEANING_MANAGE` ;
- `BED_MAINTENANCE_MANAGE`.

Conserver `BED_OPERATIONAL_STATUS_MANAGE` pour la supervision de capacité et la compatibilité du endpoint legacy, puis créer deux endpoints correspondant à des intentions métier explicites :

```text
POST /api/spatial/beds/{id}/cleaning-status
POST /api/spatial/beds/{id}/maintenance-status
```

## Matrice des rôles par défaut

| Rôle | Transfert | Décision sortie | Capacité | Nettoyage | Maintenance |
|---|---:|---:|---:|---:|---:|
| `ADMIN_CLINIQUE` | Oui | Oui | Oui | Oui | Oui |
| `MEDECIN` | Oui | Oui | Non | Non | Non |
| `INFIRMIER` | Oui | Non | Non | Non | Non |
| `RESPONSABLE_HOSPITALISATION` | Oui | Non | Oui | Oui | Oui |
| `AGENT_HYGIENE` | Non | Non | Non | Oui | Non |
| `TECHNICIEN_MAINTENANCE` | Non | Non | Non | Non | Oui |

Les rôles personnalisés restent configurables depuis le RBAC, exclusivement avec les permissions présentes dans le catalogue courant.

## Critères d'acceptation

- [x] Les quatre permissions sont enregistrées dans le catalogue RBAC.
- [x] Les rôles hygiène et maintenance sont assignables et strictement séparés.
- [x] Le transfert exige `HOSPITALIZATION_TRANSFER`.
- [x] La sortie exige `HOSPITALIZATION_DISCHARGE_DECIDE`.
- [x] Le nettoyage exige `BED_CLEANING_MANAGE`.
- [x] La maintenance exige `BED_MAINTENANCE_MANAGE`.
- [x] L'ouverture/fermeture reste réservée à `BED_OPERATIONAL_STATUS_MANAGE`.
- [x] Un circuit de nettoyage ne peut pas terminer une maintenance.
- [x] Un circuit de maintenance ne peut pas terminer un nettoyage.
- [x] Aucun circuit technique ne modifie un lit affecté.
- [x] L'interface masque chaque commande indépendamment.
- [x] Les traductions françaises et anglaises sont disponibles.
- [x] Les endpoints historiques non concernés ne sont pas renommés.

## Validation automatisée

CI **Joprelys Connect — CI Pipeline**, run **932** :

- backend Maven `clean verify` strict : succès ;
- catalogue RBAC et rôles système : succès ;
- annotations d'autorisation transfert/sortie/nettoyage/maintenance : succès ;
- transitions spécialisées et refus inter-circuits : succès ;
- refus des mutations sur lit affecté : succès ;
- migrations H2 et PostgreSQL 16/Testcontainers de la pile : succès ;
- tests Angular : succès ;
- build Angular production : succès.

Le seul échec observé lors du premier passage concernait un test historique qui appelait le endpoint de supervision avec un jeton médecin. Le test utilise désormais `ADMIN_CLINIQUE`; la permission médecin n'a pas été réélargie.

## Risques et décisions

- Le risque historique « `HOSPITALIZATION_MANAGE` reste trop large » a été traité par HOS-RBAC-001-C puis HOS-RBAC-001-D : la permission est désormais supprimée au lieu d'être conservée pour compatibilité.
- La « sortie » a ensuite été séparée par HOS-DIS-001-A entre décision médicale et départ physique ; la clearance administrative reste à traiter.
- Les circuits de nettoyage et maintenance ne portent pas encore toutes les preuves opérationnelles attendues : tâche, responsable, validation et éléments de contrôle restent des suites de HOS-BED-002-E.
- Les nouveaux rôles système doivent être validés par les établissements avant affectation aux utilisateurs.
- Les utilisateurs doivent renouveler leur JWT après resynchronisation du catalogue RBAC.

## Commandes de validation historiques

```bash
cd backend
./mvnw -Dtest=RbacCatalogBedOperationalStatusPermissionTest,SpatialControllerAuthorizationTest,HospitalizationControllerAuthorizationTest,SpatialServiceBedStatusTest test
./mvnw clean verify -Dspring.profiles.active=test

cd ../web
npm test -- --run
npm run build
```

## Definition of Done

- [x] catalogue et rôles système modifiés ;
- [x] contrôleurs protégés par les permissions dédiées ;
- [x] règles de transition inter-circuits ajoutées ;
- [x] interface conditionnée par permission ;
- [x] tests unitaires et contrats ajoutés ;
- [x] documentation API mise à jour ;
- [x] CI complète verte ;
- [x] matrice d'audit actualisée après QA automatisée ;
- [ ] validation RSSI et responsables métier ;
- [ ] recette avec comptes hygiène, maintenance, infirmier, médecin et responsable hospitalisation.
