# ADR-0005 — Contrat SOAP simplifié des notes cliniques Web et Mobile

## Statut

Décision produit validée le 2026-08-01 ; review technique et validation de la
migration requises avant fusion.

## Date

2026-08-01

## Contexte

Le backend Spring Boot expose actuellement une consultation via
`GET/POST /api/visits/{visitId}/consultation` avec les champs :

- `symptoms` ;
- `clinicalExam` ;
- `diagnosis` ;
- `conclusion` ;
- `advice` ;
- `followUp`.

Angular présente déjà ces données dans quatre blocs proches de SOAP : histoire de
la maladie, examen clinique, évaluation diagnostique, puis synthèse et conduite à
tenir.

La branche Flutter avait créé une seconde représentation réduite à
`subjective/objective/assessment/plan` et appelle une route
`/consultation-notes` qui n'existe pas. Cette représentation est pertinente pour
la hiérarchie visuelle, mais elle ne correspond pas au contrat backend.

L'alignement initial avait exposé trois champs dans la section Assessment :
`suspectedDiagnosis`, `diagnosis` et `finalDiagnosis`. Or le produit ne gère ni
statut diagnostique, ni événement de confirmation, ni workflow longitudinal
permettant de faire évoluer formellement une hypothèse vers un diagnostic final.
Ces trois champs sont donc redondants et donnent une précision clinique que le
système ne sait pas garantir.

Le produit doit éviter deux expériences cliniques divergentes tout en conservant
le backend comme maître de la vérité métier.

## Décision

### 1. Une seule sémantique SOAP pour les trois couches

La note clinique canonique est organisée ainsi :

| Section SOAP | Champs canoniques existants | Règle d'interface |
|---|---|---|
| S — Subjectif | `symptoms` | Histoire de la maladie, plainte et symptômes ; obligatoire |
| O — Objectif | `clinicalExam` | Examen clinique ; les constantes restent des données structurées associées |
| A — Évaluation | `diagnosis` | Diagnostic retenu et documenté par le praticien ; obligatoire |
| P — Plan | `conclusion`, `advice`, `followUp` | Synthèse, consignes et suivi séparés ; prescriptions et examens restent des ressources dédiées |

SOAP définit l'organisation clinique et l'expérience de saisie. Il ne doit pas
être réduit à quatre colonnes texte ni dupliquer prescriptions, examens ou
constantes.

### 2. Le contrat backend existant reste la base de compatibilité

- La route canonique reste `GET/POST /api/visits/{visitId}/consultation`.
- Aucun alias `/consultation-notes` n'est ajouté uniquement pour le mobile.
- La validation des champs obligatoires et des longueurs appartient au backend.
- `suspectedDiagnosis` et `finalDiagnosis` sont retirés du contrat public, des
  modèles, de l'IA, des interfaces et de la table active `consultations`.
- Une migration sauvegarde les trois valeurs antérieures dans une table d'audit,
  puis initialise le champ unique avec la première valeur non vide selon l'ordre
  `finalDiagnosis`, `diagnosis`, `suspectedDiagnosis` avant de supprimer les deux
  colonnes redondantes.

### 3. Parité fonctionnelle Angular / Flutter

Angular et Flutter doivent partager :

- les quatre sections S/O/A/P dans le même ordre ;
- les mêmes sous-champs métier ;
- les mêmes champs obligatoires et limites ;
- les mêmes états lecture, édition, chargement, absence, erreur et conflit ;
- les mêmes permissions et erreurs API ;
- les mêmes libellés fonctionnels FR/EN.

La parité n'impose pas une copie pixel à pixel. Angular conserve une composition
desktop responsive et Flutter une composition native mobile, toutes deux basées
sur `DESIGN.md`.

### 4. Migration du mobile vers le contrat canonique

Le modèle Flutter simplifié est remplacé par un modèle aligné sur le contrat
backend. La surface SOAP mobile existante est conservée ; la section A n'expose
qu'un champ Diagnostic et la section P conserve ses trois sous-champs utiles.

### 5. Interopérabilité future

Une future exportation FHIR pourra représenter la note comme une composition à
sections avec références vers les données structurées. Cette évolution n'est pas
requise pour fermer le correctif actuel.

## Raisons

- une seule vérité métier et un seul contrat de validation ;
- même expérience clinique sur desktop et mobile ;
- suppression d'une granularité non soutenue par un workflow métier réel ;
- migration explicite avec conservation des valeurs historiques dans une archive
  technique non exposée par l'API ;
- réduction du risque de notes différentes selon le terminal ;
- préparation d'une interopérabilité structurée sans imposer FHIR au stockage
  interne immédiat.

## Conséquences positives

- le travail visuel SOAP mobile est conservé ;
- Angular nécessite surtout une harmonisation explicite des titres S/O/A/P ;
- le modèle actif devient plus simple et sans colonnes diagnostiques redondantes ;
- les tests de contrat peuvent être communs aux deux clients ;
- les prescriptions, examens et constantes ne sont pas aplatis dans du texte.

## Conséquences négatives / risques

- le contrat API change et tous les consommateurs doivent être livrés ensemble ;
- la migration doit être vérifiée sur PostgreSQL et H2 avant fusion ;
- la parité FR/EN, validation et erreurs doit être testée sur deux clients ;
- l'interface Flutter actuelle ne peut pas être déclarée DONE avant intégration
  réelle.

## Alternatives rejetées

| Alternative | Raison du rejet |
|---|---|
| Conserver quatre champs libres uniquement sur mobile | Crée deux vérités de présentation incompatibles |
| Conserver les trois diagnostics | Le produit ne sait pas gérer leur cycle de vie et l'interface reste ambiguë |
| Supprimer directement les colonnes | Perte potentielle de données cliniques historiques |
| Ajouter un alias `/consultation-notes` pour Flutter | Duplique le contrat et reporte la divergence |
| Copier visuellement Angular sans aligner les DTO | Parité apparente seulement ; les données resteraient incompatibles |

## Impact planning

| Élément | Impact |
|---|---|
| Charge | Incluse dans les 5 SP / 2 à 3 jours de la consolidation SOAP |
| Risque | Moyen avec archive de migration ; élevé sans test DB et contrat réel |
| Profils nécessaires | Senior Spring + Angular/Flutter + reviewer clinique |
| Sprint impacté | Inclus dans TASK-20260801 avant toute nouvelle feature mobile |

## Impact version

La suppression de champs d'un contrat déjà exposé est un changement cassant. Si
ce contrat est déjà publié hors de la branche d'intégration, la prochaine release
doit être MAJOR ; sinon le changement reste intégré au candidat MINOR `0.11.0`
avant première livraison du contrat mobile unifié. Aucune release n'est préparée
ici.

## Références

- `docs/standards/ARCHITECTURE-SOLID-RESPONSIBILITY-STANDARDS.md`
- `docs/features/mobile-consultation-notes/`
- `docs/ai/tickets/STORY-1902-visites-consultations-cdc.md`
- `web/src/app/consultation/clinical-note-editor.component.ts`
- `backend/src/main/java/com/joprelys/backend/consultation/api/SaveConsultationRequest.java`
- HL7 FHIR Composition : https://hl7.org/fhir/composition.html
- HL7 US Core Clinical Notes : https://hl7.org/fhir/us/core/clinical-notes.html
