# P0 — Transcript-first Ambient Note Engine

## Objectif

La note ambient Joprelys ne doit jamais être une reformulation libre du modèle. Elle est une projection structurée et révisable du **transcript ledger FINAL effectif**.

```text
Audio durable
  ↓
Transcript FINAL + speaker + timestamps
  ↓
Evidence-linked Note Engine
  ↓
Révision GENERATED
  ↓
Revue médecin avec preuves
  ↓
ACCEPT / REJECT
```

## Contrat de sortie modèle

Le fournisseur doit retourner uniquement :

```json
{
  "statements": [
    {
      "section": "SUBJECTIVE",
      "text": "...",
      "evidenceItemIds": ["uuid"]
    }
  ]
}
```

Aucune autre clé n'est admise.

## Garde de factualité backend

Chaque statement est validé après parsing :

- tous les `evidenceItemIds` doivent exister dans la vue FINAL effective ;
- une phrase soutenue uniquement par `UNSPECIFIED` est interdite ;
- les sections critiques exigent au moins une preuve `DOCTOR` ;
- aucun terme clinique significatif absent des preuves ;
- aucun nombre absent des preuves ;
- les inversions de négation sont bloquées ;
- la criticité est calculée par le backend selon le template, jamais acceptée depuis le LLM.

Si une phrase viole une règle, **toute la note échoue**. Joprelys ne garde pas silencieusement les autres phrases en donnant une impression de complétude.

## Templates

### SOAP
- SUBJECTIVE
- OBJECTIVE — critique
- ASSESSMENT — critique
- PLAN — critique

### APSO
- ASSESSMENT — critique
- PLAN — critique
- SUBJECTIVE
- OBJECTIVE — critique

### MULTI_SECTION
- CHIEF_COMPLAINT
- HISTORY
- EXAM — critique
- VITALS — critique
- ASSESSMENT — critique
- PLAN — critique
- MEDICATIONS — critique
- ORDERS — critique
- FOLLOW_UP

## Consultation longue

Le moteur utilise une rolling note :

- lots maximum 40 transcript items ;
- environ 18 000 caractères maximum par lot ;
- 30 appels fournisseur maximum ;
- ordre des lots par `startOffsetMs/endOffsetMs`, pas par ordre d'arrivée réseau ;
- la note déjà grounded est passée au lot suivant avec ses evidence IDs ;
- seuls les nouveaux items sont ajoutés au prompt ;
- si une ancienne preuve est supersédée, le moteur repart du ledger effectif complet ;
- transcript inchangé + note non rejetée = aucun nouvel appel fournisseur.

## Persistance V98

- `ai_ambient_note_revisions`
- `ai_ambient_note_statements`
- `ai_ambient_note_evidence`

Chaque révision conserve : template, statut, séquence transcript maximale, modèle, tokens, auteur, date et éventuelle révision supersédée.

## Validation clinique

Une révision GENERATED n'est pas une note approuvée.

Au moment d'ACCEPT, le backend vérifie à nouveau :

1. que la révision est toujours la plus récente ;
2. que le transcript n'a pas avancé ;
3. que toutes les preuves sont encore dans le ledger effectif.

Sinon l'acceptation est refusée (`AI_AMBIENT_NOTE_STALE` ou `AI_AMBIENT_NOTE_EVIDENCE_SUPERSEDED`).

## UI de revue

Le composant de revue affiche :

- template et révision ;
- statut GENERATED / ACCEPTED / REJECTED ;
- phrases regroupées par section ;
- badge sur les sections critiques ;
- preuve ouvrable sous chaque phrase ;
- locuteur, timestamps et verbatim de chaque preuve ;
- blocage local de l'ACCEPT si une preuve n'est plus présente.

Le backend reste la barrière de sécurité finale.

## Hors périmètre restant

- benchmark clinique FR/Cameroun et seuils quantifiés ;
- suggested diagnoses / codes en objets séparés avec leur propre évaluation ;
- intégration éventuelle de Structured Outputs natifs par provider ;
- dernier gap crash brutal de #167 (fragment PCM courant avant persistance sub-seconde).
