# CI Actions Cost Optimization — Technical Design

## Constat

Le workflow `.github/workflows/ci.yml` comporte deux jobs lourds, backend et frontend. La première optimisation a rendu ces jobs sélectifs par stack via `dorny/paths-filter@v3`.

L'audit des PR récentes montre toutefois un second facteur de coût : certaines PR accumulent plusieurs dizaines de commits avant stabilisation. `concurrency.cancel-in-progress` annule bien un run encore actif lorsqu'un nouveau commit arrive, mais un run déjà terminé reste consommé. Une PR en développement actif peut donc déclencher de nombreuses validations complètes successives.

Le run #1011 de la PR #107 a révélé une troisième limite : après passage Ready et validation full-stack, un correctif backend-only a relancé Angular parce que la détection `pull_request` portait encore sur le diff global de la PR contre `main`.

## Solution V1 — sélection par stack

Le job `changes` expose :

- `backend=true` pour `backend/**` ou `.github/workflows/ci.yml` ;
- `frontend=true` pour `web/**` ou `.github/workflows/ci.yml`.

Les jobs backend/frontend ne s'exécutent que pour les stacks concernées.

Timeouts :

- backend : 30 minutes ;
- frontend : 20 minutes ;
- détection : 5 minutes.

## Solution V2 — gate Draft → Ready for review

Les PR en **Draft** sont considérées comme du travail encore instable. Elles ne doivent pas consommer de runner GitHub Actions lourd à chaque `synchronize`.

Le workflow écoute explicitement :

- `opened` ;
- `synchronize` ;
- `reopened` ;
- `ready_for_review` ;
- `converted_to_draft`.

Le job `changes` ne démarre que si :

- l'événement est un `push` vers `main`/`develop`, ou
- la PR n'est plus en Draft.

Conséquences :

1. PR Draft + nouveaux commits : jobs lourds ignorés ;
2. passage **Ready for review** : déclenchement automatique de la CI complète nécessaire selon les chemins ;
3. PR Ready + nouveau commit : CI relancée normalement ;
4. retour en Draft : le nouvel événement partage le même groupe de concurrence et peut annuler un run encore en cours ;
5. `push` sur `main`/`develop` : validation conservée.

## Solution V3 — delta incrémental après Ready

### Principe

La première validation d'une PR Ready reste **globale**. Les synchronisations suivantes deviennent **incrémentales**.

Le job `changes` détermine une paire de références à comparer :

- `push` : `before` → `after` du push ;
- `pull_request` `ready_for_review`, `opened` ou `reopened` : `base.sha` → `head.sha` pour couvrir tout le contenu de la PR ;
- `pull_request` `synchronize` sur une PR déjà Ready : `before` → `after` du webhook pour ne mesurer que le nouveau push ;
- si `before`/`after` sont absents, nuls, identiques ou non comparables : fallback `base.sha` → `head.sha`.

GitHub documente que `pull_request` expose les activités `synchronize` et `ready_for_review`, et que le SHA de head doit être lu via `github.event.pull_request.head.sha` plutôt que via `GITHUB_SHA`, lequel correspond au merge ref. La stratégie V3 utilise donc explicitement les données du payload de l'événement au lieu de dépendre du merge SHA.

### Détection

La V3 remplace la détection implicite du diff global par un petit script shell dans le job `changes` :

1. checkout avec historique suffisant ;
2. résolution de `FROM_SHA` et `TO_SHA` selon l'événement ;
3. vérification que les deux commits sont présents ;
4. fallback global si nécessaire ;
5. `git diff --name-only FROM_SHA TO_SHA` ;
6. calcul des outputs `backend` et `frontend` ;
7. `.github/workflows/ci.yml` force les deux outputs à `true`.

Le fallback est volontairement conservateur : si la CI ne peut pas prouver le delta incrémental, elle revient à une validation plus large plutôt qu'à une sous-validation.

### Pourquoi ne pas utiliser uniquement `HEAD^..HEAD`

Un événement `synchronize` peut représenter plusieurs commits ou une réécriture d'historique. Limiter la comparaison au dernier parent risquerait d'ignorer des fichiers réellement poussés. La paire `before`/`after` de l'événement est donc prioritaire, avec fallback global.

## Processus d'équipe recommandé

```text
Développement actif
→ ouvrir/laisser la PR en Draft
→ commits successifs sans CI lourde
→ tests locaux ciblés pendant le développement
→ passer la PR Ready for review
→ CI distante globale stricte
→ correction éventuelle
→ synchronize incrémental sur la/les stacks réellement modifiées
→ CI verte du head courant
→ merge
→ validation du push main
```

Le mode Draft ne dispense pas le développeur ou l'agent d'exécuter les tests locaux pertinents avant de déclarer la PR prête.

## Sécurité

Le workflow conserve uniquement :

- `contents: read` ;
- `pull-requests: read`.

Aucun secret supplémentaire n'est créé. Aucun script provenant de la PR n'est exécuté dans le job de détection. Le checkout et `git diff` ne font que lire les métadonnées du dépôt.

## Qualité / non-régression

Les commandes de validation distante restent inchangées :

- backend : `./mvnw clean verify -B --no-transfer-progress -Dspring.profiles.active=test` ;
- frontend : `npm test` puis `npm run build`.

Aucun test n'est supprimé ou affaibli. La réduction porte sur **le périmètre de stack relancé après une validation globale Ready**, pas sur le contenu des tests.

## Cas particuliers

- modification CI : backend + frontend exécutés ;
- passage Ready d'une PR full-stack : backend + frontend exécutés ;
- synchronize backend-only après Ready : backend seul ;
- synchronize frontend-only après Ready : frontend seul ;
- synchronize full-stack : les deux jobs ;
- delta non résolvable : fallback diff global ;
- documentation seule : workflow ignoré par `paths-ignore` ;
- PR Draft : aucun runner lourd ;
- push `main`/`develop` : détection du delta push conservée.

## Risque principal

Le risque V3 est une interprétation incorrecte d'un événement `synchronize` atypique. La mitigation est double : validation globale obligatoire au passage Ready et fallback automatique vers `base.sha` → `head.sha` dès qu'un delta incrémental ne peut pas être établi de manière fiable.

## SemVer

Aucun bump applicatif : changement d'exploitation/CI uniquement, sans changement du produit livré.