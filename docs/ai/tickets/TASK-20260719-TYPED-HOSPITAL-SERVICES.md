# TASK-20260719-TYPED-HOSPITAL-SERVICES

## Référence

- GitHub : #73
- Pull request : #75
- Commit fusionné : `2d4b2b092bde7c2f4ada2fdf0a5ad95ad8fbf228`
- CI : workflow #851 vert sur `40a19418e84b527a3df394dad8210ceb844dce49`
- Type : correction métier structurante
- Priorité : P0
- Périmètre : Spring Boot / Angular / PostgreSQL / RBAC / hospitalisation
- Impact SemVer : MAJOR
- Statut : **DONE**

## Problème résolu

Le modèle assimilait tout service à une unité d'hébergement. Il autorisait donc des chambres et des lits sous une caisse, une pharmacie, un laboratoire ou un service administratif.

Deux mécanismes aggravaient ce défaut :

1. l'admission pouvait créer automatiquement un service, une chambre et un lit inexistants ;
2. le seeder injectait une structure clinique générique dans chaque nouvelle organisation.

Ces comportements ont été supprimés. La structure hospitalière doit désormais représenter une configuration métier réelle.

## Décisions appliquées

- Le type d'un service est obligatoire dans la base, le domaine, l'API et Angular.
- Aucune valeur par défaut n'est introduite.
- Aucun type n'est déduit du nom du service.
- Aucun endpoint historique sans `serviceType` n'est conservé.
- Aucune chambre ni aucun lit n'est auto-provisionné pendant une admission.
- Aucune structure hospitalière générique n'est créée automatiquement pour une clinique.
- Une admission exige un lit préalablement configuré, existant, compatible et libre.
- Les règles sont contrôlées côté backend ; le frontend reflète `allowsRooms` renvoyé par l'API.

## Types métier

| Type | Chambres autorisées |
|---|---:|
| `HOSPITALIZATION` | Oui |
| `EMERGENCY` | Oui |
| `OUTPATIENT` | Non |
| `MEDICO_TECHNICAL` | Non |
| `PHARMACY` | Non |
| `ADMINISTRATIVE` | Non |

## Actions réalisées

- [x] Ajouter le type métier et ses capacités dans le domaine.
- [x] Ajouter la migration Flyway V74 sans valeur par défaut métier implicite.
- [x] Rendre `serviceType` obligatoire dans les contrats REST.
- [x] Refuser les chambres sous un service incompatible.
- [x] Refuser le changement de type incompatible lorsqu'il existe des chambres.
- [x] Refuser la création ou le déplacement d'un lit dans une structure incompatible.
- [x] Supprimer l'auto-provisioning de l'admission.
- [x] Supprimer le seeding automatique de la structure hospitalière.
- [x] Exiger un lit configuré et le réserver atomiquement pendant l'admission.
- [x] Sécuriser les transferts de lit dans le tenant de l'hospitalisation.
- [x] Adapter l'interface Angular et l'i18n FR/EN.
- [x] Ajouter les tests backend, frontend et migration.
- [x] Ajouter les spécifications fonctionnelles, techniques, API et données.
- [x] Valider la CI Angular et Maven.

## Critères d'acceptation vérifiés

- [x] `serviceType` est obligatoire à la création et à la modification.
- [x] Une requête historique contenant uniquement `name` échoue en validation.
- [x] Une caisse `ADMINISTRATIVE` ne peut recevoir aucune chambre.
- [x] Une pharmacie, un laboratoire et un service ambulatoire ne peuvent recevoir aucune chambre.
- [x] Un service `HOSPITALIZATION` ou `EMERGENCY` peut recevoir des chambres.
- [x] Un service avec chambres ne peut devenir non spatial.
- [x] L'admission sur un lit absent échoue et ne crée aucune donnée spatiale.
- [x] Une nouvelle clinique ne reçoit aucun service, chambre ou lit artificiel.
- [x] La migration refuse les données historiques ambiguës non qualifiées.
- [x] Les permissions et l'isolation tenant restent appliquées.
- [x] Les textes FR/EN sont présents.

## Preuves principales

- `HospitalServiceType` porte la capacité `allowsRooms()`.
- `WardEntity` impose le type et protège les changements incompatibles.
- `DefaultSpatialConfigurationService` contrôle les créations et déplacements.
- `HospitalizationAdmissionService` vérifie la structure et réserve le lit atomiquement.
- `V74__type_hospital_services.sql` réalise la migration stricte.
- Angular consomme `serviceType` et `allowsRooms`.
- La PR #75 a été fusionnée après CI backend/frontend verte.

## Déploiement et recette

La migration est volontairement stricte. Les services historiques possédant déjà des chambres sont qualifiés `HOSPITALIZATION`, car cette relation est factuelle. Tout service sans chambre doit être classé explicitement avant le déploiement.

La recette métier doit vérifier au minimum :

1. un service `HOSPITALIZATION` avec chambre et lit ;
2. un service `ADMINISTRATIVE` nommé « Caisse » sans action chambre/lit ;
3. le refus API d'une chambre sous un service non spatial ;
4. une admission sur un lit libre puis le refus d'une seconde réservation concurrente.

## Statut final

La fonctionnalité est livrée. L'issue #73 peut être clôturée comme terminée ; les contrôles de préflight des données restent une activité de déploiement, pas un reliquat fonctionnel du ticket.
