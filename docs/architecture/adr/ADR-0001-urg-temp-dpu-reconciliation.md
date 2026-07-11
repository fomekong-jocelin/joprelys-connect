# ADR-0001 — Rapprochement d’un patient URG-TEMP avec un DPU canonique

- **Statut :** ACCEPTÉ
- **Date :** 2026-07-11
- **Décideurs :** Tech Lead, responsable dossiers patients, DPO
- **Story :** STORY-2304 / issue #45
- **Portée :** identité patient, urgences, timeline clinique, documents, hospitalisation et finance

## 1. Contexte

Un patient peut être créé sous une identité provisoire `URG-TEMP`, recevoir des soins, des documents, une hospitalisation et des opérations financières avant que son identité réelle soit connue.

Lorsque l’identité devient suffisamment fiable, le système doit permettre trois décisions humaines :

1. confirmer l’identité et conserver ce dossier comme nouveau DPU définitif ;
2. rattacher le dossier URG-TEMP à un DPU existant du même établissement ;
3. reporter la décision si les preuves restent insuffisantes.

Le rapprochement ne doit jamais :

- fusionner automatiquement deux patients ;
- modifier les numéros de documents, factures ou reçus ;
- supprimer la provenance des données produites pendant l’urgence ;
- déplacer silencieusement des écritures cliniques ou financières ;
- permettre un rapprochement inter-tenant ;
- rendre impossible la correction d’une mauvaise décision.

## 2. Options étudiées

### Option A — rattachement logique à un patient canonique

Le dossier URG-TEMP reste physiquement présent avec ses relations historiques. Une relation explicite et auditée indique qu’il est rattaché à un DPU canonique.

Les écrans longitudinaux agrègent les données du DPU canonique et de ses identités sources. Chaque élément conserve son `patient_id` d’origine et sa provenance.

**Avantages**

- aucune réécriture massive des clés étrangères ;
- aucune modification des numéros, versions ou hashes des documents ;
- conservation intégrale de la provenance clinique, administrative et financière ;
- correction d’un mauvais rapprochement par nouvelle décision, sans restaurer des centaines de relations ;
- risque transactionnel limité ;
- déploiement progressif par domaine ;
- compatibilité avec un historique append-only.

**Inconvénients**

- les lectures longitudinales doivent résoudre le patient canonique ;
- les services doivent distinguer le DPU affiché du `patient_id` d’origine ;
- les exports et recherches doivent utiliser un résolveur commun pour éviter les timelines incomplètes ;
- une gouvernance stricte est nécessaire pour empêcher l’écriture de nouveaux parcours sur un alias clôturé.

### Option B — réaffectation transactionnelle de toutes les clés étrangères

Toutes les lignes rattachées au patient URG-TEMP sont mises à jour pour pointer vers le DPU cible, puis le patient source est marqué comme fusionné.

**Avantages**

- lecture simple : un seul `patient_id` après fusion ;
- moins d’adaptation des requêtes de timeline ;
- modèle intuitif pour les domaines qui supposent un patient unique.

**Inconvénients**

- transaction volumineuse et fragile sur de nombreux domaines ;
- inventaire exhaustif des clés étrangères obligatoire à chaque évolution du schéma ;
- risque élevé de déplacer partiellement des soins, documents ou opérations financières ;
- risque de modifier indirectement la portée probatoire d’un document validé ;
- rollback complexe en cas de mauvaise décision ;
- forte contention et risque d’échec en production ;
- correction d’un rapprochement erroné nécessitant un journal détaillé de chaque ligne déplacée.

## 3. Décision

Nous retenons **l’option A : rattachement logique à un patient canonique avec lecture agrégée et provenance conservée**.

Le patient URG-TEMP n’est jamais supprimé et ses relations historiques ne sont jamais réaffectées lors du rapprochement.

Cette décision est motivée par la sécurité clinique, la traçabilité médico-légale, l’intégrité financière et la possibilité de corriger une erreur sans réécrire les données sources.

## 4. Modèle cible

### 4.1 Relation canonique courante

Une table `patient_canonical_links` porte la relation active :

- `id` ;
- `organization_id` ;
- `source_patient_id` — patient URG-TEMP ou doublon ;
- `canonical_patient_id` — DPU retenu ;
- `decision_event_id` ;
- `linked_at` ;
- contrainte d’unicité sur un lien actif par `source_patient_id` ;
- interdiction de l’auto-rattachement ;
- source et cible obligatoirement dans le même tenant.

Cette table est une projection de l’état courant. Elle peut être remplacée lors d’une correction, mais jamais sans événement probatoire.

### 4.2 Journal immuable des décisions

Une table append-only `patient_reconciliation_events` enregistre :

- `id` ;
- `organization_id` ;
- `source_patient_id` ;
- `candidate_patient_id`, nullable pour la création d’un nouveau DPU ;
- `decision` : `CREATE_NEW_DPU`, `LINK_EXISTING_DPU`, `DEFER`, `CORRECT_LINK` ;
- score présenté au décideur et raisons de correspondance ;
- source et référence de la preuve ;
- justification obligatoire ;
- état avant/après ;
- auteur, date et correlation ID ;
- référence de l’événement corrigé lorsque nécessaire.

Aucun événement n’est modifié ou supprimé.

### 4.3 Alias permanents

Une table `patient_identity_aliases` conserve les identifiants historiques :

- code `URG-TEMP` ;
- ancien numéro local ;
- ancien numéro global si applicable ;
- identifiant source ;
- DPU canonique courant ;
- date de création.

Le code URG-TEMP reste recherchable même après rapprochement ou correction.

### 4.4 Statuts patient

- Le patient source passe à `MERGED` uniquement après une décision `LINK_EXISTING_DPU` validée.
- Un patient `MERGED` devient en lecture seule pour les nouveaux parcours métier.
- Les corrections médico-légales de son urgence d’origine restent possibles avec les permissions dédiées.
- Le DPU cible reste `VERIFIED`.
- Une correction restaure le statut approprié du patient source ou le rattache à une nouvelle cible, via un nouvel événement.

## 5. Résolution et règles d’écriture

Un service unique `PatientCanonicalResolver` devient la seule manière de résoudre :

- le DPU à afficher ;
- l’ensemble des `patient_id` contribuant à la timeline ;
- les alias recherchables ;
- le statut source/canonique.

### Lecture

La timeline canonique agrège les données du DPU canonique et de toutes ses sources actives. Chaque élément retourne :

- son `originPatientId` ;
- son identifiant d’origine ;
- son établissement source ;
- sa date et son auteur ;
- le type de rattachement ayant permis son affichage.

### Écriture

- Aucun nouveau parcours ne peut être ouvert directement sur un patient `MERGED`.
- Une requête utilisant un alias URG-TEMP reçoit le DPU canonique et doit demander confirmation côté interface avant une nouvelle admission ou visite.
- Les données historiques de l’urgence restent sur le patient source.
- Les nouveaux parcours explicitement créés après rapprochement sont rattachés au DPU canonique.
- Les documents déjà validés ne sont jamais régénérés ou renumérotés par le rapprochement.

## 6. Candidats et décision humaine

Le moteur de similarité ne décide jamais. Il fournit un score explicable avec des raisons séparées :

- nom normalisé ;
- date de naissance ;
- sexe ;
- téléphone ;
- adresse/quartier ;
- pièce ou preuve d’identité ;
- déclarations de tiers vérifiées.

L’API retourne les candidats sans sélection automatique. Le rapprochement exige :

- une permission dédiée ;
- une justification ;
- une preuve ou une source documentée ;
- une confirmation explicite ;
- un verrouillage pessimiste des patients source et cible ;
- une clé d’idempotence.

## 7. Correction d’un mauvais rapprochement

La correction ne supprime pas l’événement initial.

Le système :

1. verrouille la source et les cibles concernées ;
2. vérifie le tenant et les permissions renforcées ;
3. crée un événement `CORRECT_LINK` référant l’événement erroné ;
4. remplace ou retire la projection `patient_canonical_links` ;
5. recalcule les alias courants sans supprimer l’alias URG-TEMP ;
6. restaure le statut du patient source si nécessaire ;
7. journalise l’avant/après et déclenche une revue métier.

Comme aucune clé étrangère historique n’a été déplacée, les soins, documents, factures, reçus, hospitalisations et audits retrouvent automatiquement leur périmètre d’origine après correction.

## 8. Sécurité et tenant

- source et cible doivent avoir le même `organization_id` ;
- aucune API globale ne permet un rapprochement ;
- la recherche de candidats est tenantée ;
- les preuves d’identité et justifications utilisent les permissions sensibles existantes ;
- l’audit ne doit pas exposer de données d’identité complètes dans les messages libres ;
- la lecture agrégée respecte les permissions de chaque domaine.

## 9. Conséquences techniques

### Obligatoire dans STORY-2304

- migration additive des trois tables ;
- résolveur canonique partagé ;
- file des URG-TEMP à régulariser ;
- candidats scorés et expliqués ;
- décisions `CREATE_NEW_DPU`, `LINK_EXISTING_DPU` et `DEFER` ;
- correction contrôlée ;
- recherche par alias ;
- timeline patient agrégée au minimum pour urgences et identité ;
- RBAC, audit et idempotence ;
- API et interface FR/EN conformes à `DESIGN.md` ;
- tests tenant, concurrence et correction.

### Extension progressive dans STORY-2305 et STORY-2306

Le résolveur sera étendu aux domaines :

- hospitalisation ;
- documents ;
- facturation et reçus ;
- laboratoire et prescriptions ;
- exports et portail patient.

Aucun domaine ne doit implémenter sa propre logique de résolution.

## 10. Tests de décision

- création d’un nouveau DPU depuis URG-TEMP ;
- rattachement à un DPU existant ;
- plusieurs candidats sans sélection automatique ;
- décision reportée ;
- double soumission idempotente ;
- concurrence entre deux agents ;
- refus cross-tenant ;
- recherche par alias URG-TEMP ;
- conservation des numéros et hashes ;
- timeline agrégée avec provenance ;
- correction d’un mauvais rapprochement ;
- vérification qu’aucune clé étrangère historique n’a été réaffectée.

## 11. Conséquence de version

Cette évolution est **MINOR** tant que les contrats existants restent compatibles et que les nouveaux champs de provenance sont additifs.