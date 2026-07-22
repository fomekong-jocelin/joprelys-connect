# TASK-20260722 — CI incrémentale sur synchronisation des PR Ready

## Type

Engineering CI/CD + optimisation de coût

## Contexte

Les optimisations V1/V2 ont déjà :

- rendu backend/frontend sélectifs par stack ;
- supprimé les jobs lourds pendant la phase Draft ;
- imposé une CI stricte au passage `Ready for review`.

Le run #1011 de la PR #107 a toutefois montré une limite : après une validation full-stack, un correctif ne touchant que le backend a relancé backend **et** frontend, car `dorny/paths-filter` évalue le diff global de la PR contre sa base pour les événements `pull_request`.

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
- [ ] Sur `ready_for_review`, une PR full-stack lance backend + frontend.
- [ ] Sur `synchronize` d'une PR Ready, un delta backend-only lance backend et ignore frontend.
- [ ] Sur `synchronize` d'une PR Ready, un delta frontend-only lance frontend et ignore backend.
- [ ] Sur `synchronize` full-stack, les deux jobs s'exécutent.
- [ ] Un changement du workflow force les deux jobs.
- [ ] Un payload incomplet utilise le repli global sans sous-valider la PR.
- [ ] Draft continue à ignorer les jobs lourds.
- [ ] Maven `clean verify` strict reste inchangé.
- [ ] Angular `npm test` + `npm run build` restent inchangés.
- [ ] CI de la PR d'optimisation verte avant fusion.

## Plan d'action

- [x] Relire la gouvernance et le workflow courant.
- [x] Confirmer la cause du double run sur #1011.
- [x] Définir le comportement incrémental avec fallback sûr.
- [ ] Modifier `.github/workflows/ci.yml`.
- [ ] Valider le comportement Draft puis Ready.
- [ ] Valider un synchronize backend-only sans relancer Angular.
- [ ] Mettre à jour le suivi global et le changelog.
- [ ] Fusionner uniquement après preuves CI.

## Sécurité / non-régression

Le changement n'accorde aucun nouveau droit au workflow et conserve `contents: read` / `pull-requests: read`. Le fallback vers le diff global est volontairement conservateur : en cas d'incertitude, il préfère exécuter davantage de tests plutôt que d'en omettre.

## Estimation

- Profil : DevOps / senior full-stack
- Estimation : 0,5 j
- Reviewer : Tech Lead
- SemVer applicatif : aucun bump

## Reste à faire

Implémenter et valider la stratégie incrémentale sur une PR dédiée.