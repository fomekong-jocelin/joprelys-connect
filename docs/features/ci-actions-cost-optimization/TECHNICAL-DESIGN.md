# CI Actions Cost Optimization — Technical Design

## Constat

Le workflow `.github/workflows/ci.yml` comporte deux jobs parallèles, backend et frontend. Dès qu'un fichier non documentaire change, les deux jobs sont exécutés, même si une seule stack est concernée.

Le dépôt a récemment produit de nombreuses itérations de PR. Le mécanisme `concurrency.cancel-in-progress` limite déjà les runs obsolètes, mais il ne supprime pas le coût des jobs déjà exécutés ni le coût de la stack non concernée.

## Solution

Ajouter un job léger `changes` basé sur `dorny/paths-filter@v3` :

- sortie `backend=true` pour `backend/**` ou `.github/workflows/ci.yml` ;
- sortie `frontend=true` pour `web/**` ou `.github/workflows/ci.yml`.

Les jobs existants conservent leur nom et leur contenu, mais deviennent conditionnels via les outputs du job `changes`.

Ajouter des timeouts :

- backend : 30 minutes ;
- frontend : 20 minutes ;
- détection : 5 minutes.

Ces limites sont volontairement supérieures aux durées normales observées afin de ne pas interrompre un run légitime tout en plafonnant un runner bloqué.

## Sécurité

Le job de détection reçoit uniquement `contents: read` et `pull-requests: read`. Les jobs de build conservent les permissions minimales par défaut. Aucun secret supplémentaire n'est créé.

## Qualité / non-régression

Les commandes de validation restent inchangées :

- backend : `./mvnw clean verify -B --no-transfer-progress -Dspring.profiles.active=test` ;
- frontend : `npm test` puis `npm run build`.

Aucun test n'est supprimé. La sélection porte uniquement sur la stack à exécuter selon les fichiers modifiés.

## Cas particuliers

- modification CI : backend + frontend exécutés ;
- changement `backend/**` + `web/**` : backend + frontend exécutés ;
- documentation seule : le workflow reste ignoré par `paths-ignore` ;
- autre fichier racine sans impact backend/web : aucun job lourd n'est lancé. Une évolution future d'un fichier racine partagé devra l'ajouter explicitement aux filtres.

## SemVer

Aucun bump applicatif : changement d'exploitation/CI uniquement, sans changement du produit livré.