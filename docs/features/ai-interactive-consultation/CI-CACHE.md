# Cache CI — Joprelys Connect

## Objectif

Réduire la durée et la consommation des runners sans réutiliser des dépendances obsolètes ni masquer un défaut de compilation.

## Cache backend Maven

Le cache Maven est géré par `actions/setup-java` avec :

- `cache: maven` ;
- une clé invalidée par `backend/pom.xml` et les fichiers du Maven Wrapper ;
- le dépôt local Maven réutilisé entre les exécutions compatibles ;
- `--no-transfer-progress` pour réduire le volume des logs.

Une modification du `pom.xml` ou du wrapper crée automatiquement une nouvelle clé de cache.

## Cache frontend npm

Le cache npm est géré par `actions/setup-node` avec :

- `cache: npm` ;
- `cache-dependency-path: web/package-lock.json` ;
- `npm ci --prefer-offline --no-audit --fund=false`.

`npm ci` continue de recréer `node_modules` depuis le lockfile. Le cache ne remplace donc pas l’installation déterministe : il évite principalement de retélécharger les archives déjà connues.

## Cache incrémental Angular

Le dossier `web/.angular/cache` est conservé avec `actions/cache`.

La clé inclut :

- le système d’exploitation du runner ;
- le hash de `web/package-lock.json` ;
- le hash des fichiers TypeScript et de configuration Angular pertinents.

Les clés de restauration permettent une réutilisation partielle lorsque seules des sources applicatives changent. Le build de production reste toujours exécuté.

## Concurrence

Le workflow utilise un groupe de concurrence par branche ou pull request avec `cancel-in-progress: true`.

Lorsqu’un nouveau commit arrive sur la même PR :

1. l’ancienne exécution encore active est annulée ;
2. les runners se concentrent sur la nouvelle tête de branche ;
3. les caches déjà sauvegardés restent disponibles pour la nouvelle exécution.

## Principes de sécurité

- Ne jamais mettre de secret, token, audio ou contenu clinique dans un cache.
- Ne pas mettre en cache les rapports contenant des données patient.
- Les rapports de test ne sont téléversés qu’en cas d’échec et ne doivent pas contenir de contenu clinique réel.
- Aucun cache ne doit permettre de sauter les tests ou le build de production.

## Diagnostic

Pour vérifier l’efficacité du cache dans GitHub Actions, consulter les étapes :

- `Set up Java 21` pour Maven ;
- `Set up Node.js 22` pour npm ;
- `Restore Angular incremental cache` pour Angular.

Les journaux indiquent un cache hit, un cache miss ou la clé de restauration utilisée.
