# TASK-20260727 — Clinical Fact Rolling Updates (#208)

## Type

P0 — fidélité clinique / anti-hallucination / consultation longue

## Objectif

Faire évoluer le Clinical Fact Ledger par opérations atomiques explicites sans jamais régénérer ni remplacer toute la note.

```text
projectionVersion relue
        ↓
verrou pessimiste visite
        ↓
KEEP / ADD / REPLACE / RETRACT
        ↓
Fact Ledger append-only + Revision Ledger
        ↓
projection déterministe suivante
```

## Invariants

- `KEEP` ne crée et ne modifie aucun fait ;
- `ADD` crée un fait ASSERTED validé par les règles relationnelles existantes ;
- `REPLACE` cible un fait effectif de la projection de base et crée un nouveau fait ASSERTED qui le supersède ;
- `RETRACT` ne DELETE jamais le fait : une opération append-only documente le retrait et rend la cible non effective ;
- `RETRACT` exige une preuve FINAL exacte et une justification explicite de correction/annulation/négation ;
- deux opérations d'un même batch ne peuvent pas muter la même cible ;
- aucun fait ne disparaît parce qu'un LLM l'a omis ;
- la `baseProjectionVersion` doit correspondre exactement à la projection courante sous verrou ;
- un `revisionId` rejoué avec le même payload retourne le résultat historique ;
- un `revisionId` réutilisé avec un payload différent échoue ;
- batch et opérations sont tenant-scoped et auditables ;
- la projection validée existante devient naturellement stale si la version change ;
- aucune génération libre de note.

## Incision A — moteur déterministe

- Flyway V105 : batchs et opérations de révision ;
- contrat API fortement typé ;
- executor transactionnel ;
- validation des preuves de RETRACT ;
- intégration RETRACT dans la résolution des faits effectifs ;
- historique des batchs ;
- tests stale, idempotence, conflits, audit, KEEP/ADD/REPLACE/RETRACT.

## Incision B — planner structuré

Seulement après validation et fusion de A : Structured Output strict proposant des opérations sur les `factId` effectifs, suivi du même validateur déterministe.

## Gate

PR Draft pendant tout le développement. Un seul passage Ready déclenchera Maven strict sur le HEAD final avec le workflow coût V2.
