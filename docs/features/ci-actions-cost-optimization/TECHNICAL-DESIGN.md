# CI Actions Cost Optimization — Technical Design

## Constat

Le workflow `.github/workflows/ci.yml` comporte deux jobs lourds, backend et frontend. La première optimisation a rendu ces jobs sélectifs par stack via `dorny/paths-filter@v3`.

L'audit des PR récentes montre toutefois un second facteur de coût : certaines PR accumulent plusieurs dizaines de commits avant stabilisation. `concurrency.cancel-in-progress` annule bien un run encore actif lorsqu'un nouveau commit arrive, mais un run déjà terminé reste consommé. Une PR en développement actif peut donc déclencher de nombreuses validations complètes successives.

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

## Processus d'équipe recommandé

```text
Développement actif
→ ouvrir/laisser la PR en Draft
→ commits successifs sans CI lourde
→ tests locaux ciblés pendant le développement
→ passer la PR Ready for review
→ CI distante stricte
→ correction éventuelle
→ CI stricte sur chaque correction après Ready
→ merge
→ validation du push main
```

Le mode Draft ne dispense pas le développeur ou l'agent d'exécuter les tests locaux pertinents avant de déclarer la PR prête.

## Sécurité

Le workflow conserve uniquement :

- `contents: read` ;
- `pull-requests: read`.

Aucun secret supplémentaire n'est créé. Le gate Draft ne modifie aucune autorisation applicative.

## Qualité / non-régression

Les commandes de validation distante restent inchangées :

- backend : `./mvnw clean verify -B --no-transfer-progress -Dspring.profiles.active=test` ;
- frontend : `npm test` puis `npm run build`.

Aucun test n'est supprimé ou affaibli. La réduction porte sur **le moment où la CI distante est exécutée**, pas sur son contenu lorsqu'une PR est prête à être revue.

## Cas particuliers

- modification CI : backend + frontend exécutés dès que la PR est Ready ;
- changement `backend/**` + `web/**` : les deux jobs sont exécutés ;
- documentation seule : workflow ignoré par `paths-ignore` ;
- PR Draft : aucun runner lourd ;
- PR Ready : règles V1 normales ;
- push `main`/`develop` : règles V1 normales.

## Risque principal

Une PR pourrait rester en Draft et ne jamais recevoir de validation distante. La règle de gouvernance est donc : **aucune PR ne peut être fusionnée directement depuis Draft ; elle doit passer Ready for review et obtenir une CI verte sur son head courant.**

## SemVer

Aucun bump applicatif : changement d'exploitation/CI uniquement, sans changement du produit livré.
