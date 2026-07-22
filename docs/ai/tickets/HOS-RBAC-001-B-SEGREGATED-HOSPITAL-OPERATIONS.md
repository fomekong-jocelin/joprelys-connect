# HOS-RBAC-001-B — Séparer les opérations sensibles d'hospitalisation

## Métadonnées

- **Epic** : EPIC-0027 — Organisation hospitalière, capacité et parcours patient
- **Story** : HOS-RBAC-001 — Autorisations hospitalières contextuelles
- **Audit** : AUDIT-20260721
- **Écart réduit** : GAP-016
- **Priorité** : Critique / phase 0
- **Estimation** : 5 SP / 4 à 6 jours
- **Statut** : IMPLEMENTED / QA EN COURS

## Problème

La permission historique `HOSPITALIZATION_MANAGE` autorisait simultanément l'admission, les notes, les soins, le transfert et la sortie. La permission `BED_OPERATIONAL_STATUS_MANAGE` regroupait capacité, nettoyage et maintenance.

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

Les rôles personnalisés restent configurables depuis le RBAC.

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

## Risques et décisions

- `HOSPITALIZATION_MANAGE` reste encore trop large pour les notes, consentements, soins, médicaments et consommables. Ce découpage relève de HOS-RBAC-001-C.
- La « sortie » actuelle clôt encore le séjour et libère le lit ; HOS-DIS-001 séparera décision médicale, clearance administrative et départ physique.
- Les circuits de nettoyage et maintenance ne portent pas encore motif, ordre de travail, preuve, responsable ou validation à quatre yeux.
- Les nouveaux rôles système doivent être validés par les établissements avant affectation aux utilisateurs.
- Les utilisateurs doivent renouveler leur JWT après resynchronisation du catalogue RBAC.

## Validation automatisée attendue

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
- [ ] CI complète verte ;
- [ ] validation RSSI et responsables métier ;
- [ ] recette avec comptes hygiène, maintenance, infirmier, médecin et responsable hospitalisation ;
- [ ] matrice d'audit actualisée après QA.
