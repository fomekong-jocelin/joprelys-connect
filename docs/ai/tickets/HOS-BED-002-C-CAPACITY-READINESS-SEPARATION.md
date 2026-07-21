# HOS-BED-002-C — Séparer capacité ouverte, préparation et usage du lit

## Métadonnées

- **Epic** : EPIC-0027 — Organisation hospitalière, capacité et parcours patient
- **Story** : HOS-BED-002 — Cycle de vie et disponibilité des lits
- **Audit** : AUDIT-20260721
- **Écarts réduits** : GAP-006, GAP-007, GAP-010 et GAP-039
- **Priorité** : Critique / phase 0
- **Estimation** : 5 SP / 4 à 6 jours
- **Statut** : QA TECHNIQUE VERTE / VALIDATION MÉTIER ET PRÉFLIGHT REQUIS

## Problème

Le statut historique `FREE | OCCUPIED | CLEANING | MAINTENANCE` mélange :

- existence physique ;
- ouverture de capacité ;
- préparation hygiène/technique ;
- occupation réelle.

Cette confusion rend les KPI ambigus et permettait à un lit fermé ou non prêt d'être traité comme libre par des workflows ne contrôlant que `status=FREE`.

## Solution

1. conserver `beds.status` comme projection legacy ;
2. ajouter `capacity_status=OPEN|CLOSED` ;
3. ajouter `readiness_status=READY|CLEANING|MAINTENANCE` ;
4. dériver l'usage des affectations actives ;
5. renforcer le claim atomique d'admission/transfert ;
6. exposer les axes et compteurs dans l'API ;
7. ajouter une commande ouvrir/fermer ;
8. adapter l'écran spatial et les traductions FR/EN ;
9. installer les colonnes et le backfill par V81, puis les contraintes fortes PostgreSQL par V82.

## Critères d'acceptation

- [x] V81 ajoute et backfille les axes sans supprimer le statut legacy.
- [x] V82 installe les contraintes fortes sur PostgreSQL 16.
- [x] Tous les lits existants restent ouverts après migration.
- [x] Un ancien client ne voit jamais un lit fermé comme `FREE`.
- [x] Un lit nouvellement créé est ouvert, prêt et libre.
- [x] Admission et transfert réclament uniquement un lit ouvert, prêt et libre.
- [x] Le claim repository ouvre lui-même une transaction atomique.
- [x] Un lit affecté ne peut pas être fermé ou modifier sa préparation manuellement.
- [x] Un lit fermé ne peut pas être déclaré libre avant réouverture.
- [x] L'occupation est dérivée des affectations actives en lecture groupée.
- [x] L'API ajoute capacité, préparation, usage et disponibilité au lit.
- [x] L'API ajoute les compteurs ouverts et prêts au service.
- [x] Le taux d'occupation Angular utilise les lits ouverts comme dénominateur.
- [x] L'interface permet d'ouvrir et fermer un lit non affecté.
- [x] Les libellés sont disponibles en français et en anglais.
- [x] Les deux endpoints de mutation utilisent `BED_OPERATIONAL_STATUS_MANAGE`.

## Livrables

- V81 `separate_bed_capacity_and_readiness` ;
- V82 `enforce_bed_capacity_and_readiness_integrity` ;
- `BedCapacityStatus` et `BedReadinessStatus` ;
- projection enrichie `BedResponse` et `WardOccupancyResponse` ;
- claim transactionnel `claimIfAvailable()` ;
- endpoint `/capacity-status` ;
- écran spatial à six compteurs ;
- badges capacité/préparation/usage ;
- tests migration PostgreSQL, repository, service, sécurité et Angular ;
- documentation fonctionnelle et technique.

## Validation automatisée

CI `Joprelys Connect — CI Pipeline`, run **926** :

- backend Maven `clean verify` strict : succès ;
- migrations Flyway H2 : succès ;
- migrations V81/V82 et contraintes PostgreSQL 16 : succès ;
- Testcontainers PostgreSQL 16 : succès ;
- claim atomique ouvert/prêt/libre : succès ;
- refus des lits fermés, nettoyage, maintenance ou affectés : succès ;
- projections et compteurs séparés : succès ;
- tests Angular : succès ;
- build Angular production : succès.

## Risques et validations externes

- valider avec le bed manager que « fermé » ne signifie pas « maintenance » métier, malgré la projection legacy ;
- confirmer que `readyBedsCount` inclut les lits occupés dont la préparation reste `READY` ;
- vérifier sur une copie représentative les divergences entre `status=OCCUPIED` et affectations actives ;
- faire valider les KPI installés/ouverts/prêts/disponibles par la direction hospitalière ;
- définir ultérieurement motifs, dates d'effet et acteurs spécialisés.

## Dépendances

- #98 HOS-BED-002-B ;
- #99 HOS-BED-001-D ;
- #100 HOS-RBAC-001-A.

## Hors périmètre

- historique des changements de capacité ;
- réservation ;
- fermeture planifiée ;
- preuve de nettoyage ;
- séparation complète hygiène/maintenance ;
- sortie physique et turnover ;
- refonte de la capacité théorique des chambres.

## Definition of Done

- [x] code backend et frontend présent ;
- [x] migration et contraintes ajoutées ;
- [x] contrats legacy conservés ;
- [x] tests ciblés ajoutés ;
- [x] documentation créée ;
- [x] CI complète verte ;
- [ ] validation bed manager / direction hospitalière ;
- [ ] préflight sur copie représentative ;
- [ ] matrice d'audit finalisée après validation externe.
