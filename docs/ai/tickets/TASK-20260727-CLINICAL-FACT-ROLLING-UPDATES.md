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
- `ADD` crée un fait `ASSERTED` validé par le validateur relationnel existant ;
- `ADD` d'un fait strictement identique déjà effectif est refusé : l'opération correcte est `KEEP` ;
- `REPLACE` cible uniquement un fait effectif de la projection de base et crée un nouveau fait `ASSERTED` qui le supersède ;
- `REPLACE` sans changement clinique ni changement de preuve est refusé : l'opération correcte est `KEEP` ;
- `RETRACT` ne fait jamais de `DELETE` physique : une opération append-only documente le retrait ;
- `RETRACT` exige une preuve `FINAL` exacte et une justification explicite de correction, négation ou annulation ;
- une information issue d'une décision/observation du clinicien ne peut pas être rétractée à partir d'une parole patient seule ;
- une rétraction n'est effective que tant que toutes ses preuves restent dans le transcript `FINAL` effectif ; une correction du transcript qui invalide la preuve invalide donc aussi la rétraction ;
- deux opérations d'un même batch ne peuvent pas muter la même cible ;
- un `operationId` ne peut pas être réutilisé dans l'historique d'une visite ;
- aucun fait ne disparaît parce qu'un LLM l'a omis ;
- la `baseProjectionVersion` doit correspondre exactement à la projection courante après acquisition du verrou pessimiste de la visite ;
- deux révisions concurrentes basées sur la même projection sont sérialisées par le verrou ; la seconde devient stale après la première mutation ;
- un `revisionId` rejoué par le même médecin avec le même payload canonique retourne le résultat historique sans réexécuter les mutations ;
- un `revisionId` réutilisé avec un payload différent ou par un autre médecin échoue ;
- batchs, opérations et preuves de rétraction sont tenant-scoped et visit-scoped ;
- les FK composites empêchent une opération de référencer un batch ou un fait d'une autre organisation/visite ;
- la projection médicale déjà validée devient naturellement stale dès qu'une révision change la `projectionVersion` ;
- aucune génération libre de note.

## Contrats API

### Appliquer un batch

`POST /api/ai/consultations/{visitId}/facts/revisions`

Autorisation : `CLINICAL_WRITE`.

Entrée :

- `revisionId` UUID idempotent ;
- `baseProjectionVersion` exactement relue ;
- liste ordonnée d'opérations `KEEP | ADD | REPLACE | RETRACT` ;
- `operationId` UUID par opération ;
- payload factuel uniquement pour `ADD` / `REPLACE` ;
- payload de rétraction uniquement pour `RETRACT`.

### Historique

`GET /api/ai/consultations/{visitId}/facts/revisions`

Autorisation : `CLINICAL_READ` ou `CLINICAL_WRITE`.

L'historique expose les versions de projection avant/après, l'auteur, l'ordre des opérations, les cibles/résultats et les preuves persistées.

## Persistance — Flyway V105

### `ai_clinical_fact_revision_batches`

- requête de révision ;
- `base_projection_version` ;
- `result_projection_version` ;
- SHA-256 canonique du payload ;
- médecin auteur ;
- date de création.

### `ai_clinical_fact_revision_operations`

- ordre dans le batch ;
- type `KEEP / ADD / REPLACE / RETRACT` ;
- `target_fact_id` et/ou `result_fact_id` selon une contrainte de forme SQL ;
- raison de rétraction ;
- identité organisation/visite.

### `ai_clinical_fact_revision_evidence`

- preuve persistée des seules opérations `RETRACT` ;
- transcript item ;
- offsets caractères ;
- quote exacte ;
- preuve principale ;
- identité organisation/visite.

Les preuves de `ADD` / `REPLACE` restent portées par le nouveau fait immuable dans `ai_clinical_fact_evidence` afin d'éviter une seconde source de vérité.

## Validation de RETRACT

Une rétraction doit :

1. cibler un fait effectif de la projection de base ;
2. référencer exclusivement des items `FINAL` effectifs ;
3. contenir exactement une preuve principale ;
4. correspondre exactement aux offsets et à la quote du transcript ;
5. citer le concept du fait cible ;
6. respecter la politique de locuteur ;
7. porter une raison explicite :
   - `EXPLICIT_CORRECTION` ;
   - `EXPLICIT_NEGATION` ;
   - `CLINICIAN_CANCELLATION`.

`CLINICIAN_CANCELLATION` est limitée aux médicaments, ordres et plans et requiert une parole médecin explicite.

## Tests de non-régression ajoutés

- stale-check avant toute mutation ;
- acquisition du verrou visite avant lecture de la projection de base ;
- `KEEP` sans création de fait ;
- `ADD` via le validateur factuel existant ;
- rejet d'un `ADD` strictement identique à un fait effectif ;
- `REPLACE` par supersession append-only ;
- rejet d'un `REPLACE` sans changement réel ;
- `RETRACT` sans suppression physique ;
- preuve `FINAL` exacte pour la rétraction ;
- patient incapable de rétracter seul une décision clinique ;
- conflit si deux opérations ciblent le même fait ;
- refus d'une cible non effective ;
- retry strictement idempotent ;
- canonicalisation du SHA-256 indépendante de l'ordre des preuves ;
- refus d'un `revisionId` réutilisé avec un payload différent ;
- invalidation d'une rétraction lorsque sa preuve n'est plus effective ;
- historique des batchs sans réexécution clinique ;
- compatibilité des anciens tests du Fact Ledger avec les nouveaux repositories de révision.

## Incision B — planner structuré

Seulement après validation et fusion de l'incision A : Structured Output strict proposant des opérations sur les `factId` effectifs, puis passage obligatoire par ce moteur déterministe.

Le planner n'aura aucun droit de mutation directe sur le ledger.

## Gate

La PR reste Draft pendant tout le développement. Un seul passage `Ready for review` déclenche Maven strict + Flyway V105 sur le HEAD final via le workflow coût V2. Aucun commit ne doit être ajouté entre le gate vert et la fusion.
