# TASK-20260727 — Structured Clinical Fact Revision Planner (#211)

## Type

P0 — fidélité clinique / anti-hallucination / rolling ambient

## Dépendance

Le moteur déterministe #208 / PR #210 est fusionné dans `main` au commit `db8b7cc2fcba5bdb5e8f9e922d3022b879097f47`.

## Objectif

Permettre à un modèle Structured Output de **proposer** des opérations `KEEP / ADD / REPLACE / RETRACT` sans jamais muter directement le Clinical Fact Ledger.

## Incision volontaire

Cette PR est **planner-only** :

- elle ne remplace pas encore `/facts/extract` dans le parcours live ;
- elle ne persiste aucun fait ni aucune révision ;
- elle n'appelle pas `ClinicalFactRevisionService.apply()` ;
- elle retourne un `ApplyRevisionRequest` entièrement normalisé et validé que l'orchestrateur pourra soumettre explicitement au moteur #208 dans une incision ultérieure.

Cette séparation évite deux moteurs concurrents tant que les comparatifs avec #196 et le benchmark clinique ne sont pas passés.

## Contrat d'entrée

Le caller fournit :

- `baseProjectionVersion` exactement relue ;
- jusqu'à 8 `transcriptItemIds` FINAL à analyser.

Les IDs explicites rendent le planner sans état, rejouable et compatible avec les benchmarks. Il n'existe pas de curseur caché qui pourrait marquer un transcript comme traité avant application d'une révision.

## Chaîne

```text
baseProjectionVersion + transcriptItemIds
        ↓
lecture snapshot FINAL
        ↓
validation des items demandés
        ↓
facts effectifs + projection courante
        ↓
Structured Output JSON Schema strict
        ↓
propositions LLM
        ↓
normalisation déterministe
  - UUID factId existant uniquement
  - quote exacte / offsets recalculés
  - enums bornés
  - ADD dupliqué -> KEEP
  - REPLACE sans changement -> KEEP
        ↓
dry-run validators
  - ClinicalFactEvidenceValidator
  - ClinicalFactRetractionValidator
  - règles sémantiques #208
        ↓
revalidation projection + transcript
        ↓
ApplyRevisionRequest proposé
        ↓
AUCUNE MUTATION
```

## Sémantique KEEP / omission

Le planner ne crée **pas** artificiellement un `KEEP` pour tous les faits de la visite : cela gonflerait inutilement les batchs et l'audit lors des consultations longues.

- si le nouveau transcript restate explicitement un fait inchangé, le planner peut produire `KEEP` ;
- si un fait existant n'est pas concerné par le nouveau batch, son omission signifie **aucune opération**, jamais une suppression ;
- le moteur #208 conserve les faits par défaut et ne retire un fait que sur `RETRACT` explicite validé.

## Invariants

- aucune note libre ;
- aucun fallback JSON texte ;
- `targetFactId` inventé = rejet ;
- toute preuve doit appartenir aux items FINAL explicitement fournis au planner ;
- quote exacte et non ambiguë ;
- offsets caractères recalculés côté Joprelys ;
- `ADD` / `REPLACE` sont validés en dry-run par le validateur relationnel existant ;
- `RETRACT` est validé en dry-run par le validateur de rétraction #208 ;
- omission d'un fait existant ne vaut jamais suppression ;
- une même cible ne peut recevoir qu'une opération ;
- `ADD` identique / `REPLACE` sans changement sont normalisés en `KEEP` ;
- `REPLACE` doit conserver factType + conceptCode + authority ; un changement de concept utilise `RETRACT + ADD` ;
- la projection courante doit être égale à `baseProjectionVersion` avant et après l'appel modèle ;
- les transcript items demandés sont revalidés après l'appel modèle pour détecter une correction concurrente ;
- le futur apply repassera de toute façon par le stale-check transactionnel #208 ;
- `applyRequired=false` lorsque le plan est vide ou ne contient que des `KEEP` ;
- aucune écriture DB dans cette incision.

## Endpoint

`POST /api/ai/consultations/{visitId}/facts/revisions/plan`

Autorisation : `CLINICAL_WRITE`.

La réponse retourne :

- `planId` ;
- `visitId` ;
- `baseProjectionVersion` ;
- modèle ayant produit la proposition ;
- `transcriptItemIds` analysés ;
- `ApplyRevisionRequest proposedRevision` normalisé ;
- `applyRequired` ;
- compteurs d'opérations modèle / normalisées.

## Gate

PR Draft pendant tout le développement. Aucun Maven lourd avant stabilisation complète. Un seul passage Ready déclenchera le gate backend sur le HEAD final.
