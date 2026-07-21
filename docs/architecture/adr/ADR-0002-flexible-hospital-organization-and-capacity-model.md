# ADR-0002 — Séparer organisation, géographie et capacité hospitalière

- **Statut** : Proposed
- **Date** : 2026-07-21
- **Décideurs attendus** : Product Owner, architecte, médecin responsable, cadre infirmier, admissions, DBA, DPO/RSSI
- **Contexte** : AUDIT-20260721, EPIC-0027

## Contexte

Le modèle actuel `Organization → Ward → Room → Bed` utilise `Ward` à la fois comme service et parent géographique, et `Room` à la fois comme salle et chambre. Il ne peut pas représenter un réseau, un site, une unité, une salle partagée ou un plateau technique. Les noms de service/chambre/lit sont également recopiés dans les visites et hospitalisations.

## Décision proposée

Adopter trois référentiels indépendants reliés par des affectations datées :

1. juridique : groupe facultatif et établissement ;
2. organisationnel : unités typées et hiérarchiques facultatives ;
3. géographique : site/bâtiment/étage/zone/espace avec niveaux facultatifs.

La capacité est un agrégat séparé : chambre d'hospitalisation, lit, périodes d'ouverture, réservation, affectation, turnover et indisponibilité. Les ressources/équipements utilisent les mêmes espaces sans être confondus avec eux.

Les petites structures peuvent omettre tous les niveaux non utiles. Les relations par noms sont remplacées par des UUID et des snapshots documentaires explicites.

## Options considérées

### A. Ajouter des colonnes à `wards` et `rooms`

Rejetée : maintient la confusion service/localisation, crée de nombreux champs conditionnels et ne résout pas les salles partagées.

### B. Une seule table d'arbre générique pour tout

Rejetée : met dans un même arbre établissement, service, chambre, personne et équipement ; les règles, cycles et permissions deviennent opaques.

### C. Référentiels séparés avec liens datés

Proposée : plus de tables et de migration, mais concepts explicites, flexibilité, historisation et reporting fiables.

## Conséquences positives

- support des cabinets, cliniques, hôpitaux et réseaux sans niveaux factices ;
- services multi-sites, unités déportées et espaces partagés ;
- calcul reproductible de la capacité installée, ouverte et disponible ;
- rattachements professionnels et permissions contextuels ;
- meilleure correspondance avec les ressources d'interopérabilité `Organization`, `Location`, `HealthcareService`, `Encounter` et `PractitionerRole`.

## Coûts et risques

- migration structurante et période de double écriture contrôlée ;
- hausse initiale du nombre d'entités/API ;
- besoin de validations métier pluridisciplinaires ;
- tests PostgreSQL réels pour contraintes temporelles ;
- contrats API historiques à déprécier puis retirer.

## Garde-fous

- aucune suppression automatique de données historiques ;
- rapport de réconciliation avant activation des FK/exclusions ;
- API v2 et couche anti-corruption ;
- métriques de comparaison ancien/nouveau calcul de capacité ;
- feature flags par établissement ;
- plan de rollback explicite par vague.

## Impact SemVer

L'ajout parallèle peut être livré en MINOR. Le retrait ou changement obligatoire des contrats `wards/rooms/beds` est breaking et doit suivre la règle MAJOR du projet, après période de dépréciation.

## Conditions d'acceptation de l'ADR

- ateliers médecin/cadre/admissions/biomédical/labo/pharmacie terminés ;
- modèle de tenant groupe/établissement tranché ;
- prototype d'exclusion temporelle PostgreSQL validé ;
- stratégie de migration et restauration testée ;
- matrice RBAC/ABAC signée ;
- budget et capacité de l'EPIC-0027 approuvés.
