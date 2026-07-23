# ADR-0002 — Séparer organisation, géographie et capacité hospitalière

- **Statut** : Accepted for incremental implementation
- **Date** : 2026-07-21
- **Décision Product Owner pour démarrage** : 2026-07-23
- **Validations externes encore attendues** : médecin responsable, cadre infirmier, admissions, DBA, DPO/RSSI selon les lots
- **Contexte** : AUDIT-20260721, EPIC-0027, HOS-ORG-001-A #130, HOS-LOC-001-A #131, HOS-STAFF-001-A #132

## Contexte

Le modèle actuel `Organization → Ward → Room → Bed` utilise `Ward` à la fois comme service et parent géographique, et `Room` à la fois comme salle et chambre. Il ne peut pas représenter un réseau, un site, une unité, une salle partagée ou un plateau technique. Les noms de service/chambre/lit sont également recopiés dans les visites et hospitalisations.

## Décision

Adopter trois référentiels indépendants reliés par des affectations datées :

1. juridique : groupe facultatif et établissement ;
2. organisationnel : unités typées et hiérarchiques facultatives ;
3. géographique : site/bâtiment/étage/zone/espace avec niveaux facultatifs.

La capacité est un agrégat séparé : chambre d'hospitalisation, lit, périodes d'ouverture, réservation, affectation, turnover et indisponibilité. Les ressources/équipements utilisent les mêmes espaces sans être confondus avec eux.

Les petites structures peuvent omettre tous les niveaux non utiles. Les relations par noms sont remplacées par des UUID et des snapshots documentaires explicites.

### Ordre d'implémentation accepté

1. **HOS-ORG-001-A / #130** : référentiels service/spécialité et unités organisationnelles ;
2. **HOS-LOC-001-A / #131** : site/bâtiment/étage/zone/espace + liens datés unité-espace ;
3. **HOS-STAFF-001-A / #132** : affectations datées du personnel et retrait des champs libres ;
4. poursuite HOS-BED/HOS-DIS/HOS-PATH selon risques et validations.

Cette acceptation autorise l'implémentation incrémentale. Elle ne vaut pas validation métier finale de l'ensemble de l'EPIC-0027.

## Options considérées

### A. Ajouter des colonnes à `wards` et `rooms`

Rejetée : maintient la confusion service/localisation, crée de nombreux champs conditionnels et ne résout pas les salles partagées.

### B. Une seule table d'arbre générique pour tout

Rejetée : met dans un même arbre établissement, service, chambre, personne et équipement ; les règles, cycles et permissions deviennent opaques.

### C. Référentiels séparés avec liens datés

Retenue : plus de tables et de migration, mais concepts explicites, flexibilité, historisation et reporting fiables.

## Conséquences positives

- support des cabinets, cliniques, hôpitaux et réseaux sans niveaux factices ;
- services multi-sites, unités déportées et espaces partagés ;
- calcul reproductible de la capacité installée, ouverte et disponible ;
- rattachements professionnels et permissions contextuels ;
- meilleure correspondance avec les ressources d'interopérabilité `Organization`, `Location`, `HealthcareService`, `Encounter` et `PractitionerRole`.

## Coûts et risques

- migration structurante par vagues ;
- hausse initiale du nombre d'entités/API ;
- besoin de validations métier pluridisciplinaires ;
- tests PostgreSQL réels pour contraintes temporelles ;
- contrats API historiques à retirer lorsque leurs remplacements sont effectivement livrés.

## Garde-fous

- aucune suppression automatique de données historiques ;
- aucun mapping automatique depuis des textes libres ambiguës ;
- migrations explicites et testées ;
- organisation et géographie ne partagent pas une entité générique commune ;
- aucune règle métier dupliquée dans Angular ;
- permissions dédiées par responsabilité ;
- aucune rétrocompatibilité finale par alias ou fallback de champs libres ;
- retrait d'un ancien contrat uniquement lorsque son remplacement structuré est livré et couvert par tests.

## Impact SemVer

Les ajouts parallèles HOS-ORG/HOS-LOC peuvent être livrés en MINOR. Le retrait obligatoire des anciens contrats `wards/rooms` ou des champs libres `department/specialty/serviceName` est breaking et doit être livré comme MAJOR lorsque les lots de remplacement sont complets.

## Conditions de validation de chaque lot

- documentation fonctionnelle/technique/data/API/test à jour ;
- validation Product pour le périmètre livré ;
- tests backend/frontend/migration verts ;
- revue DBA pour les migrations ;
- revue métier/RSSI selon les droits ou workflows impactés ;
- rollback documenté ;
- tracking/changelog/backlog alignés.
