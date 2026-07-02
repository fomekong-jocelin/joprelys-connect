# Configuration standards — Spring Boot / Angular

## 1. Spring Boot configuration

### Règle obligatoire

Tout backend Spring Boot doit utiliser :

```text
src/main/resources/application.yml
src/main/resources/application-dev.yml
src/main/resources/application-test.yml
src/main/resources/application-prod.yml
```

`application.properties` est interdit pour les nouvelles configurations.

### Pourquoi

- YAML est plus lisible pour les configurations hiérarchiques.
- Les profils sont plus clairs.
- Les configurations complexes Spring Boot, sécurité, datasource, observabilité et CORS sont plus faciles à relire.
- Cela évite la dispersion des formats dans l’équipe.

### Règles

- Ne pas créer `application.properties`.
- Ne pas mélanger YAML et properties dans le même projet.
- Utiliser les variables d’environnement via `${VAR_NAME:default}`.
- Ne jamais mettre de secret réel dans les fichiers versionnés.
- Créer un ADR si un projet existant impose temporairement `application.properties`.
- Prévoir une tâche de migration vers YAML si un fichier `.properties` existe déjà.

## 2. Angular proxy configuration

### Règle obligatoire

Tout frontend Angular doit contenir un proxy de développement :

```text
proxy.conf.json
```

Et `angular.json` doit référencer :

```json
"proxyConfig": "proxy.conf.json"
```

### Règles Angular

- Les services Angular doivent appeler l’API avec des chemins relatifs : `/api/...`, `/auth/...`.
- Ne jamais hardcoder `http://localhost:8080`, une IP ou un domaine backend dans un service Angular.
- Le proxy sert au développement local uniquement.
- En production, utiliser un reverse proxy, gateway, ingress ou configuration serveur.
- Ne pas stocker de secrets dans le proxy.

## 3. Critères de review bloquants

Une review doit être bloquée si :

- un nouveau `application.properties` est introduit ;
- le backend Spring Boot n’a pas de `application.yml` ;
- le frontend Angular n’a pas de `proxy.conf.json` ;
- `angular.json` ne référence pas le proxy ;
- un service Angular hardcode l’URL backend ;
- une exception n’est pas couverte par ADR.

## Configuration Frontend/Mobile

Les projets Angular et Flutter doivent centraliser les informations applicatives et de branding.

### Paramètres minimum

| Paramètre | Description |
|---|---|
| `appName` | Nom affiché de l’application |
| `appShortName` | Nom court si nécessaire |
| `appSlogan` | Baseline ou promesse courte |
| `publisherName` | Organisation / éditeur |
| `logoPath` | Logo principal |
| `logoDarkPath` | Logo adapté au thème dark si nécessaire |
| `defaultLocale` | Locale par défaut, recommandée : `fr` |
| `supportedLocales` | Minimum : `fr`, `en` |
| `defaultTheme` | `light`, `dark` ou `system` |
| `supportEmail` | Contact support si applicable |
| `publicLinks` | CGU, confidentialité, contact, site web |

Ces valeurs ne doivent pas être dupliquées dans les écrans.


## Règle obligatoire — SOLID, responsabilités et backend maître

Toute intervention Spring Boot, Angular ou Flutter doit appliquer `docs/standards/ARCHITECTURE-SOLID-RESPONSIBILITY-STANDARDS.md`.

Règles courtes :
- SOLID doit être respecté à la lettre.
- Le backend est le maître de la vérité métier : validation, décisions, sécurité, persistance et règles critiques.
- Le front web/mobile affiche, collecte et orchestre l’expérience ; il ne porte pas de logique métier complexe ni de vérité critique.
- Les controllers Spring Boot ne contiennent aucune logique métier et ne doivent jamais appeler directement repositories, SQL ou clients externes.
- Les controllers délèguent à des services/use cases ; les services exposés aux controllers doivent avoir un contrat clair et une implémentation dédiée.
- Les composants Angular/Flutter ne doivent pas être monolithiques ; extraire services, facades, use cases, widgets/composants réutilisables.
- Privilégier composition, interfaces et abstractions utiles ; éviter héritage profond et abstractions spéculatives.
- Aucune classe, composant ou widget ne doit dépasser 500 lignes ; alerte dès 300 lignes.
