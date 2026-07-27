# TASK-20260722 — Réduire la consommation GitHub Actions

## Type

Diagnostic + Engineering CI/CD

## Contexte

Le dépôt `joprelys-connect` a un rythme élevé de commits et de PR. La première optimisation a rendu la CI path-aware afin d'éviter de lancer simultanément Maven et Angular lorsque seule une stack est modifiée.

Après remise en service du budget Actions, la consommation reste trop élevée : le coût vient surtout des **synchronisations successives de PR déjà passées Ready**, qui relancent le gate lourd à chaque commit alors que le développement n'est pas terminé.

Le niveau de non-régression ne doit pas être réduit : Maven `clean verify`, les tests Angular et le build production restent obligatoires avant fusion lorsque leur stack est concernée.

## Objectif

Réduire fortement les minutes facturables **sans supprimer de tests et sans permettre à un commit non testé d'être fusionné**.

## Stratégie cible V2

### Cycle de développement

```text
PR Draft
  ↓
commits / corrections / revue
  ↓
aucun runner lourd
  ↓
PR Ready
  ↓
1 gate complet sur le HEAD final
```

### Commit ajouté après le gate

Un commit poussé sur une PR déjà Ready **ne relance pas Maven/Angular automatiquement**. À la place, un job `PR gate freshness` sur `ubuntu-slim` échoue volontairement et indique que le gate est devenu stale.

La PR doit alors repasser :

```text
Ready → Draft → corrections → Ready
```

Le nouveau passage Ready déclenche le gate complet sur le nouveau HEAD.

Cette règle empêche de fusionner en considérant comme valide un gate exécuté sur un commit précédent.

## Critères d'acceptation

- [x] Conserver `paths-ignore` pour la documentation.
- [x] Conserver `concurrency.cancel-in-progress`.
- [x] Détecter les changements backend/frontend avant les jobs lourds.
- [x] Exécuter Maven uniquement lorsque `backend/**` ou le workflow CI change.
- [x] Exécuter Angular uniquement lorsque `web/**` ou le workflow CI change.
- [x] Conserver Maven `clean verify` strict.
- [x] Conserver les tests Angular et le build production.
- [x] Conserver les timeouts de sécurité.
- [x] Conserver les permissions GitHub Actions minimales explicites.
- [x] Utiliser `ubuntu-slim` pour les jobs légers de détection/fraîcheur.
- [x] Une PR Draft ne doit lancer aucun build lourd.
- [x] Un `synchronize` sur une PR Ready ne doit pas relancer les builds lourds.
- [x] Un `synchronize` après gate doit rendre explicitement le gate stale.
- [x] Le passage `ready_for_review` doit exécuter le gate complet sur le HEAD courant.
- [x] Les pushes `main/develop` conservent une vérification post-fusion path-aware.
- [ ] Valider le workflow modifié sur une PR Ready réelle.
- [ ] Mesurer la consommation après un cycle de développement et ajuster si nécessaire.

## Diagnostic

### Cause principale V1 — corrigée

La CI lançait historiquement Maven et Angular ensemble, même lorsqu'une seule stack était modifiée. La détection de chemins a supprimé ce gaspillage.

### Cause principale V2

Les synchronisations de PR (`pull_request.synchronize`) relancent le workflow à chaque commit d'une PR non-Draft. Sur un chantier itératif, un même gate Maven/Angular peut donc être payé plusieurs fois avant que la PR soit réellement terminée.

`concurrency.cancel-in-progress` limite les runs simultanés mais ne rembourse pas les minutes déjà consommées avant annulation.

## Architecture CI V2

### `PR gate freshness`

- runner : `ubuntu-slim` ;
- uniquement sur `synchronize` d'une PR Ready ;
- aucun checkout ;
- échoue volontairement avec une instruction `Draft → Ready` ;
- coût minimal par rapport à un build Maven/Angular complet.

### `Detect changed stacks`

- runner : `ubuntu-slim` ;
- exécuté uniquement sur push `main/develop` ou événement de gate PR explicite ;
- comparaison globale base/head pour un gate PR ;
- fail-safe : si la plage Git est indisponible, les deux stacks sont validées.

### Backend

- runner standard Linux ;
- Java 21 ;
- cache Maven ;
- `./mvnw clean verify` strict ;
- aucun test supprimé.

### Frontend

- runner standard Linux ;
- Node 22 ;
- cache npm + cache Angular ;
- tests complets ;
- build production ;
- aucun test supprimé.

## Règle d'équipe

Une PR doit rester **Draft pendant tout le développement**.

Elle ne passe **Ready qu'une fois l'incision terminée et localement cohérente**. Si un commit supplémentaire est nécessaire après le gate, elle repasse Draft avant les corrections, puis Ready lorsque le nouveau HEAD est réellement final.

## Gains attendus

Pour une PR nécessitant 10 commits de correction :

- avant V2 : jusqu'à 10 gates lourds selon le timing des commits ;
- après V2 : 1 gate lourd final + éventuellement quelques jobs `ubuntu-slim` de fraîcheur si la règle Draft n'a pas été respectée.

Les pushes sur `main/develop` restent volontairement vérifiés après fusion : cette redondance est conservée comme filet de sécurité contre les merges/direct pushes et sera réévaluée uniquement après mesure réelle.

## Non-objectifs

- supprimer des tests pour économiser ;
- remplacer Maven `clean verify` par une validation partielle avant merge ;
- supprimer le build Angular production ;
- exécuter la CI de branches de développement sur un serveur PROD/RECETTE ;
- considérer un gate vert sur un ancien SHA comme valable pour un nouveau commit.

## Tests / validation

### V1

CI GitHub Actions `Joprelys Connect — CI Pipeline`, run **#1003** : succès avec détection backend/frontend et gate complet.

### V2

À valider sur la PR d'optimisation elle-même :

1. ouverture en Draft → aucun gate lourd ;
2. passage Ready → `Detect changed stacks` sur `ubuntu-slim` ;
3. modification de `.github/workflows/ci.yml` → backend + frontend obligatoires ;
4. Maven `clean verify` strict vert ;
5. tests Angular verts ;
6. build Angular production vert ;
7. aucun merge avant ce gate.

## Reste à faire

- mesurer la consommation Actions pendant une semaine de développement ;
- comparer nombre de gates lourds / PR avant et après V2 ;
- envisager ensuite un runner self-hosted **dédié CI** si le coût des deux gates (PR + post-merge) reste significatif ;
- ne jamais utiliser PROD/RECETTE comme runner d'exécution de code de branche.
