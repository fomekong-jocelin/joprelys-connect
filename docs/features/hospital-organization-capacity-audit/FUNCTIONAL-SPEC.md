# Spécification fonctionnelle initiale — Audit organisation hospitalière

## Problème métier

Joprelys doit servir des structures allant du cabinet mono-site au réseau hospitalier. Le modèle actuel doit être vérifié avant toute extension afin d'éviter de confondre organisation, spécialité, unité fonctionnelle, localisation physique, capacité d'hébergement et rôle applicatif.

## Objectif

Établir un état des lieux prouvé, identifier les risques métier et définir une cible configurable couvrant l'organisation, les ressources, l'hospitalisation et le parcours patient.

## Utilisateurs concernés

Direction, administration, accueil/admissions, médecins, cadres infirmiers, infirmiers, bloc opératoire, urgences, laboratoire, imagerie, pharmacie, biomédical, facturation, DPO/RSSI et patients.

## Périmètre inclus

- structures juridiques, organisationnelles, fonctionnelles et géographiques ;
- services, unités, spécialités, salles, chambres, lits et équipements ;
- affectations de personnel datées ;
- demandes, admissions, réservations, occupations, transferts et sorties ;
- urgences, soins critiques, laboratoire, imagerie, pharmacie et bloc ;
- permissions, traçabilité, reporting et concurrence.

## Périmètre exclu

- implémentation immédiate du modèle cible ;
- validation juridique définitive pour tous les pays ;
- certification clinique ou réglementaire de Joprelys.

## Critères d'acceptation

- L'existant est décrit à partir des sources réellement accessibles.
- Les absences et contradictions documentaires sont signalées.
- Une cible minimale et extensible est proposée sans imposer les niveaux inutiles.
- Les règles critiques de sécurité des soins et d'intégrité sont transformables en critères et tests.
- Les recommandations nécessitant une validation humaine spécialisée sont explicitement identifiées.

## Hypothèses et validations requises

- Les règles nationales de conservation, confidentialité et identification patient varient selon le pays de déploiement.
- Les nomenclatures de services et de lits doivent être validées par un médecin responsable et un cadre infirmier.
- Les indicateurs de capacité doivent être validés par la direction médicale et le contrôle de gestion.
- Le modèle de données et la stratégie de migration doivent être validés par l'architecte et le DBA avant développement.

## Résultat de l'audit

La fonctionnalité est aujourd'hui **partielle et structurellement insuffisante** pour le périmètre demandé. Le rapport de référence est [AUDIT-REPORT.md](AUDIT-REPORT.md). Le modèle cible, le contrat API et la recette sont détaillés dans [DATA-MODEL.md](DATA-MODEL.md), [API-CONTRACT.md](API-CONTRACT.md) et [TEST-PLAN.md](TEST-PLAN.md).

## Décisions fonctionnelles proposées

- séparer organisation, géographie et capacité ;
- rendre chaque niveau hiérarchique facultatif ;
- historiser rattachements, présences, réservations et mouvements ;
- distinguer sortie médicale, administrative et physique ;
- contextualiser les permissions par affectation et relation de soin ;
- traiter le backend comme maître des règles de compatibilité et de concurrence.

## État de validation

- Audit du dépôt : terminé.
- Validation médecin/cadre/admissions : à faire.
- Validation biomédical/laboratoire/pharmacie/imagerie : à faire.
- Validation DPO/RSSI/réglementaire locale : à faire.
- Engagement en sprint : non autorisé sans capacité et ADR acceptée.
