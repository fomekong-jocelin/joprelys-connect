# TASK-20260719-TYPED-HOSPITAL-SERVICES

## Référence

- GitHub : #73
- Type : correction métier structurante
- Priorité : P0
- Périmètre : Spring Boot / Angular / PostgreSQL / RBAC / hospitalisation
- Impact SemVer : MAJOR

## Problème

Le modèle actuel assimile tout service à une unité d'hébergement. Il autorise donc des chambres et des lits sous une caisse, une pharmacie, un laboratoire ou un service administratif.

Deux mécanismes aggravent ce défaut :

1. `HospitalizationService` crée automatiquement un service, une chambre et un lit lorsqu'ils n'existent pas, au nom d'une compatibilité ascendante ;
2. `AdminUserSeeder` injecte automatiquement une structure clinique générique dans chaque nouvelle organisation.

Ces comportements fabriquent une réalité hospitalière fictive. Ils sont supprimés, pas maintenus derrière un fallback.

## Décisions fermes

- Le type d'un service devient obligatoire dans la base et dans l'API.
- Aucune valeur par défaut n'est introduite.
- Aucun type n'est déduit du nom du service.
- Aucun endpoint ancien sans `serviceType` n'est conservé.
- Aucune chambre ni aucun lit n'est auto-provisionné pendant une admission.
- Aucune structure hospitalière générique n'est créée automatiquement pour une clinique.
- Une admission exige un lit préalablement configuré, existant et libre.
- Les règles sont contrôlées côté backend ; le frontend ne fait que refléter les capacités renvoyées par l'API.

## Types métier

| Type | Chambres autorisées |
|---|---:|
| `HOSPITALIZATION` | Oui |
| `EMERGENCY` | Oui |
| `OUTPATIENT` | Non |
| `MEDICO_TECHNICAL` | Non |
| `PHARMACY` | Non |
| `ADMINISTRATIVE` | Non |

## Actions

- [ ] Ajouter le type métier et ses capacités dans le domaine.
- [ ] Ajouter la migration Flyway V74 sans valeur par défaut.
- [ ] Rendre `serviceType` obligatoire dans les contrats REST.
- [ ] Refuser les chambres sous un service incompatible.
- [ ] Refuser le changement de type incompatible lorsqu'il existe des chambres.
- [ ] Supprimer l'auto-provisioning de l'admission.
- [ ] Supprimer le seeding automatique de la structure hospitalière.
- [ ] Adapter l'interface Angular et l'i18n FR/EN.
- [ ] Ajouter les tests backend, frontend et migration.
- [ ] Mettre à jour l'ADR, la documentation produit, le suivi et le changelog.

## Critères d'acceptation

- [ ] `serviceType` est obligatoire à la création et à la modification.
- [ ] Une requête ancienne contenant uniquement `name` échoue en validation.
- [ ] Une caisse `ADMINISTRATIVE` ne peut recevoir aucune chambre.
- [ ] Une pharmacie, un laboratoire et un service ambulatoire ne peuvent recevoir aucune chambre.
- [ ] Un service `HOSPITALIZATION` ou `EMERGENCY` peut recevoir des chambres.
- [ ] Un service avec chambres ne peut devenir non spatial.
- [ ] L'admission sur un lit absent échoue et ne crée aucune donnée spatiale.
- [ ] Une nouvelle clinique ne reçoit aucun service/chambre/lit artificiel.
- [ ] La migration bloque tant que les services historiques sans type explicite ne sont pas qualifiés.
- [ ] Les permissions et l'isolation tenant restent inchangées.

## Déploiement

La migration est volontairement stricte. Les services historiques possédant déjà des chambres sont qualifiés `HOSPITALIZATION`, car cette relation constitue une preuve structurelle. Tout service sans chambre reste non qualifié et bloque le `NOT NULL` : il doit être classé ou supprimé explicitement avant le déploiement.

## Reste à faire

- CI complète Maven/Angular.
- Préflight des données de recette puis production.
- Recette métier avec référent clinique.
