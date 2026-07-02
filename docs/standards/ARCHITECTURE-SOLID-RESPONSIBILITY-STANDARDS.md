# Architecture, SOLID & Séparation des responsabilités — Spring Boot / Angular / Flutter

## Objectif

Ce standard empêche les dérives classiques : contrôleurs qui contiennent de la logique métier, composants front trop intelligents, services fourre-tout, duplication des règles métier, classes énormes et architecture impossible à maintenir.

Il est obligatoire pour tout projet Spring Boot, Angular et Flutter.

## Principe fondamental

Le backend est le maître de la vérité métier.

- Le backend valide, calcule, autorise, persiste et décide.
- Le frontend web et le mobile affichent, collectent les entrées, guident l’utilisateur et orchestrent l’expérience.
- Le frontend peut faire de la validation UX immédiate, mais cette validation ne remplace jamais la validation backend.
- Aucune règle métier critique ne doit exister uniquement dans Angular ou Flutter.
- Toute règle dupliquée côté front doit être considérée comme une aide d’affichage, pas comme source de vérité.

## Règle SOLID obligatoire

Chaque intervention IA ou humaine doit respecter SOLID à la lettre.

| Principe | Règle concrète | Rejet immédiat si |
|---|---|---|
| SRP — Single Responsibility | Une classe, composant, widget ou service a une seule raison de changer. | Une classe gère validation + persistence + mapping + appel externe. |
| OCP — Open/Closed | Les variations métier passent par interfaces, stratégies, policies ou handlers dédiés. | On modifie un gros `if/else` central à chaque nouveau cas. |
| LSP — Liskov Substitution | Une implémentation doit respecter le contrat de son abstraction. | Une sous-classe ou implémentation change le comportement attendu du contrat. |
| ISP — Interface Segregation | Les interfaces sont petites, orientées usage, non fourre-tout. | Une interface impose des méthodes inutiles aux implémentations. |
| DIP — Dependency Inversion | Les couches haut niveau dépendent d’abstractions, pas de détails techniques. | Un controller dépend directement d’un repository, client HTTP ou classe infrastructure. |

## Backend Spring Boot — règles strictes

### 1. Le controller ne contient aucune logique métier

Un controller doit uniquement :

- exposer l’endpoint HTTP ;
- recevoir et valider les DTO d’entrée avec `@Valid` ;
- déléguer à un service / use case ;
- transformer le résultat en réponse HTTP ;
- gérer le statut HTTP attendu.

Un controller ne doit jamais :

- contenir une règle métier ;
- calculer un prix, un statut, une éligibilité, un solde, une disponibilité, une autorisation métier ;
- appeler directement un repository ;
- appeler directement un client HTTP externe ;
- ouvrir une transaction ;
- faire du SQL ;
- contenir des `if/else` métier complexes ;
- manipuler des entités JPA comme réponse API.

### 2. Services, interfaces et implémentations

Toute logique métier ou orchestration doit passer par une couche service/use case.

Règle recommandée pour les services exposés aux controllers :

```text
Controller -> UseCase/Service interface -> Service implementation -> Domain / Ports
```

Exemple de structure :

```text
src/main/java/.../patient/
  api/
    PatientController.java
    dto/
      CreatePatientRequest.java
      PatientResponse.java
  application/
    CreatePatientUseCase.java
    GetPatientUseCase.java
    impl/
      DefaultCreatePatientUseCase.java
      DefaultGetPatientUseCase.java
  domain/
    Patient.java
    PatientPolicy.java
    PatientRepositoryPort.java
  infrastructure/
    persistence/
      JpaPatientRepository.java
      PatientPersistenceAdapter.java
```

Règles :

- Les controllers dépendent d’interfaces de service/use case.
- Les implémentations portent un nom explicite : `DefaultCreatePatientUseCase`, `PatientApplicationService`, `JpaPatientRepositoryAdapter`, etc.
- Éviter les noms pauvres comme `PatientServiceImpl` si un nom métier plus clair est possible.
- Les repositories techniques restent dans l’infrastructure.
- Les ports/interfaces métier restent dans `domain` ou `application` selon le besoin.
- Les transactions sont portées par les services applicatifs, jamais par les controllers.

### 3. Domain first

Le domaine doit porter les règles métier importantes :

- invariants ;
- policies ;
- statuts ;
- transitions ;
- décisions ;
- règles de validation métier ;
- événements domaine si nécessaire.

Le domaine ne doit pas dépendre de Spring, JPA, Angular, Flutter ou d’un framework.

### 4. DTO, mapping et entités

- Les DTO API sont immutables.
- Les entités JPA ne sortent jamais directement de l’API.
- Le mapping doit être isolé dans des mappers dédiés.
- Les DTO d’entrée ne doivent pas être réutilisés comme entités métier.
- Les erreurs API doivent être structurées et cohérentes.

### 5. Généricité maîtrisée

La généricité est autorisée et encouragée lorsqu’elle réduit une duplication réelle.

Bon usage :

- `PageResponse<T>` ;
- `BaseMapper<D, E>` si tous les mappers partagent un contrat réel ;
- `Specification<T>` ;
- handlers typés ;
- services techniques transverses.

Mauvais usage :

- créer un `GenericCrudService<T>` pour toute la logique métier ;
- cacher des règles métier dans une classe générique ;
- utiliser des raw types ;
- rendre le code illisible pour éviter 10 lignes de duplication acceptable.

Règle : la généricité ne doit jamais effacer le vocabulaire métier.

### 6. Héritage, abstractions et composition

- Privilégier la composition à l’héritage.
- L’héritage est autorisé uniquement si la relation `is-a` est vraie et stable.
- Les classes abstraites sont autorisées pour factoriser un algorithme invariant ou un comportement technique clairement partagé.
- Les interfaces sont préférées pour définir un contrat métier.
- Pas de hiérarchie profonde : maximum 2 niveaux d’héritage sauf ADR.
- Toute abstraction doit avoir au moins une raison réelle : variation, testabilité, découplage, contrat stable.

Interdit :

- abstraction spéculative ;
- classe `AbstractManager` fourre-tout ;
- héritage pour simplement partager deux méthodes utilitaires ;
- `BaseService` qui devient un god service.

## Angular — responsabilités strictes

### 1. Le front affiche, le backend décide

Angular ne doit pas être la source de vérité métier.

Angular peut :

- afficher les données ;
- gérer les états UI ;
- appeler les API ;
- gérer les formulaires ;
- faire une validation UX ;
- traduire les libellés ;
- gérer thème, langue, configuration publique ;
- composer les composants.

Angular ne doit pas :

- calculer une règle métier critique ;
- déterminer seul un statut métier ;
- appliquer une autorisation métier réelle ;
- contenir une logique complexe dans un composant ;
- appeler `HttpClient` directement depuis un composant ;
- dupliquer des règles backend sans justification.

### 2. Architecture Angular attendue

```text
feature/
  pages/              # composants pages / containers
  components/         # composants UI réutilisables
  services/           # appels API, facades, orchestration légère
  models/             # DTO et modèles d’affichage
  state/              # stores/signals si besoin
  i18n/               # traductions si spécifique
shared/
  ui/                 # design system interne
  services/
  config/
  theme/
```

Règles :

- Les composants pages orchestrent mais ne portent pas de règles métier complexes.
- Les composants UI sont réutilisables et reçoivent leurs données via inputs/signals.
- Les services encapsulent les appels API et la transformation légère.
- Les facades/stores gèrent l’état front si nécessaire.
- Les règles d’affichage complexes doivent être extraites dans helpers/pipes/services dédiés et testés.

## Flutter — responsabilités strictes

Flutter suit la même règle : le mobile affiche, le backend décide.

Architecture attendue :

```text
features/<feature>/
  domain/          # entities, use cases, repository interfaces
  data/            # DTO, data sources, repository impl
  presentation/    # screens, widgets, controllers/providers
```

Règles :

- Les widgets ne portent pas de logique métier complexe.
- Les use cases orchestrent les actions côté app.
- Les repository interfaces restent dans `domain`.
- Les implémentations restent dans `data`.
- Les providers/controllers gèrent l’état de présentation, pas les décisions métier critiques.

## Limites de taille obligatoires

| Élément | Limite dure | Seuil d’alerte |
|---|---:|---:|
| Classe Java / service / controller | 500 lignes | 300 lignes |
| Composant Angular TS | 500 lignes | 300 lignes |
| Template Angular HTML | 300 lignes | 200 lignes |
| Widget Flutter | 500 lignes | 250 lignes |
| Méthode / fonction | 40 lignes | 25 lignes |
| Paramètres de méthode | 5 | 3 |
| Profondeur d’imbrication | 3 niveaux | 2 niveaux |
| Complexité cyclomatique | 10 | 5 |

Dès qu’un seuil d’alerte est atteint, l’IA doit proposer une extraction.
Si une limite dure est dépassée, la PR/MR doit être refusée sauf ADR temporaire.

## Découpage obligatoire avant développement

Avant de coder, l’IA doit vérifier :

- quelle couche porte la responsabilité ;
- quel contrat est exposé ;
- quel service/use case porte l’orchestration ;
- quelle règle appartient au backend ;
- quelle partie est uniquement présentation ;
- quels tests prouvent la règle métier ;
- quels composants sont réutilisables ;
- si une abstraction est nécessaire ou spéculative.

## Tests attendus

- Les règles métier sont testées côté backend.
- Les controllers sont testés uniquement sur le contrat HTTP et la délégation.
- Les services/use cases sont testés sur les scénarios métier.
- Les mappers sont testés si le mapping est non trivial.
- Les composants Angular/Flutter sont testés sur l’affichage, les interactions et les états UI.
- Les validations front ne remplacent jamais les tests backend.

## Critères de rejet immédiat

Une PR/MR doit être rejetée si :

- logique métier dans un controller ;
- logique métier critique uniquement côté Angular/Flutter ;
- controller qui appelle directement repository, client HTTP ou SQL ;
- composant Angular/Flutter qui contient une logique complexe non extraite ;
- classe/composant/widget dépassant 500 lignes ;
- méthode dépassant 40 lignes sans justification ;
- interface fourre-tout ;
- service générique qui cache les règles métier ;
- entité JPA exposée directement dans une réponse API ;
- règle métier dupliquée front/back sans justification ;
- abstraction ou héritage introduit sans besoin réel.

## Résumé opérationnel

```text
Backend = vérité métier, validation, sécurité, persistance, décision.
Frontend/Mobile = affichage, expérience, saisie, état de présentation.
Controller = délégation.
Service/UseCase = orchestration.
Domain = règles métier.
Infrastructure = détails techniques.
UI Component/Widget = présentation.
```
