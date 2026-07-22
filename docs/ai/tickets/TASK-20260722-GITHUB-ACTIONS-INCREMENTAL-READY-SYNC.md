# TASK-20260722 — CI incrémentale sur synchronisation des PR Ready

## Type

Engineering CI/CD + optimisation de coût

## Contexte

Les optimisations V1/V2 ont déjà :

- rendu backend/frontend sélectifs par stack ;
- supprimé les jobs lourds pendant la phase Draft ;
- imposé une CI stricte au passage `Ready for review`.

Le run #1011 de la PR #107 a toutefois montré une limite : après une validation full-stack, un correctif ne touchant que le backend a relancé backend **et** frontend, car la détection `pull_request` évaluait le diff global de la PR contre sa base.

## Objectif

Conserver une validation globale lors du passage `Ready for review`, puis rendre les événements `synchronize` suivants incrémentaux afin de ne relancer que la ou les stacks réellement modifiées par le nouveau push.

## Règles cibles

- PR Draft : aucun job lourd ;
- `ready_for_review`, `opened` non-Draft ou `reopened` : évaluer le diff global base → head ;
- `synchronize` sur PR Ready : évaluer en priorité le delta `before` → `after` du webhook ;
- si le delta incrémental n'est pas exploitable, repli sûr vers le diff global base → head ;
- modification de `.github/workflows/ci.yml` : backend + frontend ;
- push `main`/`develop` : conserver la détection du push ;
- commandes Maven et Angular inchangées ;
- aucune suppression ou réduction de tests.

## Critères d'acceptation

- [x] Diagnostic documenté à partir du run #1011.
- [x] Documentation fonctionnelle/technique mise à jour avant le workflow.
- [x] Sur `ready_for_review`, la PR #111 modifiant le workflow a lancé backend + frontend : run #1014.
- [x] Sur `synchronize` d'une PR Ready, un delta backend-only lance backend et ignore frontend : runs #1015/#1016.
- [ ] Sur `synchronize` d'une PR Ready, un delta frontend-only lance frontend et ignore backend — à confirmer lors d'un prochain changement frontend réel ou d'un probe dédié si nécessaire.
- [ ] Sur `synchronize` full-stack, les deux jobs s'exécutent — couvert conceptuellement par la détection, à observer sur un prochain delta full-stack.
- [x] Un changement du workflow force les deux jobs : run #1014.
- [ ] Un payload incomplet utilise le repli global sans sous-valider la PR — branche de sécurité implémentée, scénario non forcé à distance.
- [x] Draft continue à ignorer les jobs lourds : run #1013 `skipped`.
- [x] Maven `clean verify` strict reste inchangé et vert sur #1014 puis #1016.
- [x] Angular `npm test` + `npm run build` restent inchangés et verts sur #1014.
- [x] CI finale backend-only #1016 verte, frontend explicitement `skipped`.

## Preuves observées

### Draft

- PR #111 ouverte en Draft ;
- run #1013 : `skipped`.

### Ready global

- passage Ready ;
- run #1014 : détecteur vert, Maven vert, Angular tests + build vert.

### Synchronize backend-only

Un fichier probe temporaire `backend/.ci-incremental-probe` a été ajouté uniquement pour valider la sélection de stack, sans impact Maven/applicatif.

- run #1015 : détecteur vert, frontend `skipped`, backend démarré ;
- le probe a été retiré immédiatement ;
- `concurrency.cancel-in-progress` a annulé #1015 ;
- run #1016 sur la suppression backend-only : détecteur vert, backend Maven strict vert, frontend `skipped`.

Le probe n'est pas présent dans le diff final de la PR.

## Plan d'action

- [x] Relire la gouvernance et le workflow courant.
- [x] Confirmer la cause du double run sur #1011.
- [x] Définir le comportement incrémental avec fallback sûr.
- [x] Modifier `.github/workflows/ci.yml`.
- [x] Valider le comportement Draft puis Ready.
- [x] Valider un synchronize backend-only sans relancer Angular.
- [ ] Mettre à jour le suivi global et le changelog central — à réaliser sans écrasement destructif de leur contenu long.
- [ ] Fusionner uniquement après vérification finale du head et de la PR.

## Sécurité / non-régression

Le changement n'accorde aucun nouveau droit au workflow et conserve `contents: read` / `pull-requests: read`. Le fallback vers le diff global est volontairement conservateur : en cas d'incertitude, il préfère exécuter davantage de tests plutôt que d'en omettre.

Aucun test Maven/Angular n'est supprimé ou assoupli.

## Estimation

- Profil : DevOps / senior full-stack
- Estimation : 0,5 j
- Reviewer : Tech Lead
- SemVer applicatif : aucun bump

## Reste à faire

- vérifier le diff final et l'absence du probe ;
- mettre à jour les suivis centraux si l'édition peut être faite sans perte de contenu ;
- fusionner #111 après validation finale.