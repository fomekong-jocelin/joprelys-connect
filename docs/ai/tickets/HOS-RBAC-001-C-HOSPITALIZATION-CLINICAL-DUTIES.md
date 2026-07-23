# HOS-RBAC-001-C — Séparation des tâches cliniques d’hospitalisation

## Statut

**CODE FUSIONNÉ DANS `main` — QA AUTOMATISÉE VERTE — validations externes et préparation au déploiement en attente**

- PR canonique : **#107** ;
- commit fusionné : `4df94f43ee5943a55ae60bac22794b1a8ff746a4` ;
- branche `main` de référence après optimisation CI : `078c3dc5f913f615910fad9f061085bc7acdcfec` ;
- workflow PR #107 final observé vert : **#1011** ;
- workflow `main` après fusion observé vert : **#1012** ;
- aucune réimplémentation de ce ticket ne doit être créée dans une nouvelle PR.

## Contexte

L’audit `AUDIT-20260721` identifie GAP-016 : plusieurs opérations de nature différente étaient regroupées derrière la permission historique `HOSPITALIZATION_MANAGE`.

Avant cet incrément, la même permission permettait de :

- créer une hospitalisation ;
- ajouter une note ;
- enregistrer un consentement ;
- tracer un soin ;
- tracer une administration médicamenteuse ;
- tracer un consommable patient.

Cette agrégation ne respectait pas le principe du moindre privilège et empêchait de distinguer les responsabilités médicales, infirmières et administratives.

## Objectif livré

Remplacer l’autorisation générique par six permissions orientées action, sans modifier les URL ni les payloads HTTP :

- `HOSPITALIZATION_ADMIT` ;
- `HOSPITALIZATION_NOTE_WRITE` ;
- `HOSPITALIZATION_CONSENT_RECORD` ;
- `HOSPITALIZATION_CARE_WRITE` ;
- `HOSPITALIZATION_MEDICATION_ADMINISTER` ;
- `HOSPITALIZATION_CONSUMABLE_RECORD`.

`HOSPITALIZATION_MANAGE` reste temporairement au catalogue pour permettre la migration contrôlée des rôles personnalisés, mais ne protège plus les six endpoints concernés.

## Preuves dans le code de `main`

### Catalogue RBAC

`RbacCatalog` :

- déclare les six permissions dédiées ;
- conserve `HOSPITALIZATION_MANAGE` comme permission legacy uniquement ;
- attribue au médecin : admission, notes, consentement, soins, transfert et décision médicale de sortie ;
- n’attribue pas par défaut au médecin l’administration médicamenteuse ni les consommables ;
- attribue à l’infirmier : notes, consentement, soins, administration médicamenteuse, consommables et transfert ;
- n’attribue pas l’admission à l’infirmier ;
- limite le responsable hospitalisation à l’admission, au transfert, au départ physique et aux opérations de lit ;
- n’attribue aucune écriture clinique aux rôles hygiène et maintenance.

### Contrôleur hospitalisation

`HospitalizationController` protège explicitement :

| Endpoint | Permission |
|---|---|
| `POST /api/hospitalizations` | `HOSPITALIZATION_ADMIT` |
| `POST /api/hospitalizations/{id}/notes` | `HOSPITALIZATION_NOTE_WRITE` |
| `POST /api/hospitalizations/{id}/consents` | `HOSPITALIZATION_CONSENT_RECORD` |
| `POST /api/hospitalizations/{id}/daily-cares` | `HOSPITALIZATION_CARE_WRITE` |
| `POST /api/hospitalizations/{id}/medication-administrations` | `HOSPITALIZATION_MEDICATION_ADMINISTER` |
| `POST /api/hospitalizations/{id}/patient-consumptions` | `HOSPITALIZATION_CONSUMABLE_RECORD` |

Les comptes-rendus opératoires restent volontairement sous `CLINICAL_WRITE` en attendant le chantier bloc opératoire.

## Sémantique de sécurité

### Admission

`HOSPITALIZATION_ADMIT` autorise la création d’un séjour et l’affectation initiale d’un lit. Elle ne donne aucun droit d’écriture clinique pendant le séjour.

### Notes

`HOSPITALIZATION_NOTE_WRITE` autorise l’ajout de transmissions ou d’observations dans le séjour actif.

### Consentement

`HOSPITALIZATION_CONSENT_RECORD` autorise la traçabilité d’un consentement et de sa pièce signée. La permission ne signifie pas que son détenteur peut décider à la place du patient ni réaliser l’information médicale requise.

### Soins

`HOSPITALIZATION_CARE_WRITE` autorise la traçabilité des soins et actes courants.

### Médicaments

`HOSPITALIZATION_MEDICATION_ADMINISTER` correspond uniquement à l’administration effective. Elle n’est pas une permission de prescription et n’est pas confondue avec les permissions pharmacie.

### Consommables

`HOSPITALIZATION_CONSUMABLE_RECORD` trace l’usage d’un consommable pour le patient. Elle ne permet pas de gérer le stock.

## Matrice système livrée

| Rôle | Admission | Notes | Consentement | Soins | Administration médicament | Consommables |
|---|---:|---:|---:|---:|---:|---:|
| `ADMIN_CLINIQUE` | oui | oui | oui | oui | oui | oui |
| `MEDECIN` | oui | oui | oui | oui | non par défaut | non par défaut |
| `INFIRMIER` | non | oui | oui | oui | oui | oui |
| `RESPONSABLE_HOSPITALISATION` | oui | non | non | non | non | non |
| `AGENT_HYGIENE` | non | non | non | non | non | non |
| `TECHNICIEN_MAINTENANCE` | non | non | non | non | non | non |

Les rôles personnalisés peuvent recevoir les permissions nécessaires indépendamment.

## Critères d’acceptation

- [x] six permissions dédiées déclarées dans le catalogue ;
- [x] rôles système reconstruits au moindre privilège ;
- [x] `HOSPITALIZATION_MANAGE` retirée des rôles médecin, infirmier et responsable hospitalisation ;
- [x] les six endpoints d’écriture utilisent leur permission dédiée ;
- [x] tests de réflexion sur toutes les annotations ;
- [x] tests positifs et négatifs de la matrice système ;
- [x] test d’intégration séparant explicitement soin médecin et administration/consommables infirmier ;
- [x] visibilité frontend alignée sur chaque permission ;
- [x] test Angular du mapping onglet → permission sans fallback `HOSPITALIZATION_MANAGE` ;
- [x] continuité urgence → hospitalisation alignée sur `HOSPITALIZATION_ADMIT`, testée en refus et sans chargement des données d’admission si non autorisée ;
- [x] CI backend et frontend verte sur le head final de la PR #107 ;
- [x] CI `main` verte après fusion ;
- [x] documentation d’audit, conception technique et contrat API présents.

## État QA automatisée

- workflow **#1010** : échec intermédiaire ayant détecté une hypothèse de test devenue invalide ;
- correction sans élargissement des droits : médecin refusé pour médicament/consommable, infirmier autorisé ;
- workflow PR **#1011** : vert sur le head final de #107 ;
- fusion de #107 dans `main` au commit `4df94f43ee5943a55ae60bac22794b1a8ff746a4` ;
- workflow `main` **#1012** : vert après fusion.

Les anciens textes indiquant « QA finale bloquée » ou « PR Draft #107 en cours » sont obsolètes et ne doivent plus servir de référence.

## Compatibilité et déploiement

Aucune URL ni payload n’est modifié. En revanche, les rôles personnalisés qui dépendaient uniquement de `HOSPITALIZATION_MANAGE` doivent recevoir explicitement les nouvelles permissions avant déploiement.

Le bootstrap RBAC resynchronise le catalogue et les rôles système. Les JWT existants doivent être renouvelés après déploiement afin de refléter les nouvelles authorities.

Il ne faut pas traduire automatiquement `HOSPITALIZATION_MANAGE` vers l’ensemble des nouvelles permissions : cela recréerait la sur-autorisation supprimée par cet incrément.

## Validations externes encore requises

- [ ] validation RSSI/DPO de la matrice ;
- [ ] validation direction médicale des responsabilités médecin/infirmier ;
- [ ] validation responsable hospitalisation du droit d’admission ;
- [ ] inventaire des rôles personnalisés contenant `HOSPITALIZATION_MANAGE` ;
- [ ] remappage explicite et minimal des rôles personnalisés concernés ;
- [ ] recette positive et négative avec comptes représentatifs ;
- [ ] resynchronisation du catalogue sur l’environnement cible ;
- [ ] renouvellement des JWT/sessions après synchronisation.

## Hors périmètre

- prescription médicamenteuse ;
- validation à quatre yeux ;
- ABAC par unité, affectation ou relation de soin ;
- habilitations professionnelles et délégations datées ;
- clearance administrative/financière de sortie ;
- correction append-only des actes cliniques.

## Règle de non-duplication

Toute suite du chantier doit partir de `main` et traiter uniquement les éléments encore ouverts ci-dessus. Il est interdit de recréer les six permissions, de recopier le bootstrap RBAC de #107 ou de réintroduire `HOSPITALIZATION_MANAGE` comme fallback.