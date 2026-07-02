# ADR-0002 — Utilisation Systématique de la Pagination et du Lazy Loading

## Statut

Accepté

## Date

2026-07-01

## Contexte

Le système Joprelys Connect va gérer des volumes de données médicaux de plus en plus importants (patients, historiques de visites, constantes, logs d'audit).
Le chargement complet de ces données en mémoire ou leur retour brut via les API REST présente plusieurs risques majeurs :
1. **Saturation mémoire (OutOfMemoryError)** côté Backend si des listes massives de patients ou d'audit logs sont chargées.
2. **Surcharge réseau** pour le transfert de données inutilisées.
3. **Dégradation du temps de réponse** de l'interface client (UI bloquée).
4. **Problème de requêtes N+1** avec JPA/Hibernate lorsque des relations complexes (comme Clinic -> Patients -> Visites) sont chargées de manière EAGER (immédiate).

## Décision

Nous adoptons deux principes d'architecture fondamentaux pour l'accès aux données :

### 1. Pagination Obligatoire (Pageables)
- Toutes les API REST retournant des collections ou des résultats de recherche doivent accepter les paramètres de pagination standard de Spring Data (`page`, `size`, `sort`) à travers l'objet `Pageable`.
- Les contrôleurs doivent retourner un objet encapsulé de type `Page<T>` (ou une structure équivalente contenant les données de pagination : totalElements, totalPages, content).

### 2. Chargement Différé (Lazy Loading) par Défaut
- **Backend (JPA/Hibernate) :** Toutes les relations entre entités (notamment `@ManyToOne` et `@OneToOne` qui sont EAGER par défaut en JPA) doivent être explicitement configurées avec `fetch = FetchType.LAZY`.
- Si des données liées sont nécessaires pour une transaction spécifique, elles doivent être chargées via une requête JPQL spécifique utilisant `JOIN FETCH`, ou via des EntityGraphs, plutôt que par chargement immédiat automatique.
- **Frontend (Angular / Flutter) :** Les composants de liste doivent implémenter la pagination de table ou le défilement infini (infinite scroll) pour ne charger et n'afficher que les données visibles.

## Raisons

- Assurer la scalabilité de la plateforme face à l'augmentation du nombre de dossiers patients.
- Économiser les ressources matérielles (mémoire RAM des serveurs cliniques locaux et bande passante réseau mobile).
- Éliminer les requêtes SQL implicites redondantes générées par Hibernate.

## Conséquences positives

- **Performance constante :** La taille des données échangées et traitées à chaque requête est bornée.
- **Consommation réseau minimale :** Idéal pour les cliniques pilotes accédant à l'application via des connexions mobiles limitées.
- **Meilleure réactivité de l'UI :** Le client charge uniquement ce qu'il affiche.

## Conséquences négatives / risques

- **Risque de `LazyInitializationException` :** Si les relations Lazy sont accédées hors d'une transaction active ou après la sérialisation des entités.
  *Mitigation :* Mapper systématiquement les entités en DTO (Data Transfer Objects) au sein de la couche Service (sous transaction `@Transactional`) avant de les envoyer au contrôleur REST.
- **Complexité accrue :** Nécessité de gérer les états de pagination et de tri dans les composants graphiques.

## Alternatives rejetées

| Alternative | Raison du rejet |
|---|---|
| EAGER Fetching par défaut | Génère des requêtes SQL N+1 massives et charge des graphes d'objets entiers inutilement. |
| Filtrage et pagination côté client | Non scalable. Si la base contient 10 000 patients, le navigateur plantera en essayant de tous les charger avant de filtrer. |

## Impact planning

| Élément | Impact |
|---|---|
| Charge | Faible (standardisation des pratiques de code) |
| Risque | Faible (prévention des risques de performance ultérieurs) |
| Profils nécessaires | Senior (pour la configuration initiale des filtres JPA) |
| Sprint impacté | SPRINT-0002 |

## Références

- Spring Data Pagination Documentation : https://docs.spring.io/spring-data/commons/docs/current/reference/html/#core.web.pagination
- Hibernate Fetching Strategies Performance : https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html#fetching
