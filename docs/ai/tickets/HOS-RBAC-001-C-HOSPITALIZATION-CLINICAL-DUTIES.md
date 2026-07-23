# HOS-RBAC-001-C — Séparation des tâches cliniques d’hospitalisation

## Statut

**IMPLÉMENTATION ET QA AUTOMATISÉE TERMINÉES — FUSIONNÉ DANS `main` VIA PR #107 — validations externes en attente**

Référence de fusion : `4df94f43ee5943a55ae60bac22794b1a8ff746a4`.

État de référence après consolidation CI : `main@078c3dc5f913f615910fad9f061085bc7acdcfec`. Le seul commit entre la fusion #107 et ce SHA est le correctif CI #111 ; aucun fichier RBAC/hospitalisation de HOS-RBAC-001-C n’a été modifié après la fusion.

## Contexte

L’audit `AUDIT-20260721` identifie GAP-016 : plusieurs opérations de nature différente restaient regroupées derrière la permission historique `HOSPITALIZATION_MANAGE`.

Avant cet incrément, la même permission permettait de :

- créer une hospitalisation ;
- ajouter une note ;
- enregistrer un consentement ;
- tracer un soin ;
- tracer une administration médicamenteuse ;
- tracer un consommable patient.

Cette agrégation ne respecte pas le principe du moindre privilège et empêche de distinguer les responsabilités médicales, infirmières et administratives.

## Objectif

Remplacer l’autorisation générique par six permissions orientées action, sans modifier les URL ni les payloads HTTP :

- `HOSPITALIZATION_ADMIT` ;
- `HOSPITALIZATION_NOTE_WRITE` ;
- `HOSPITALIZATION_CONSENT_RECORD` ;
- `HOSPITALIZATION_CARE_WRITE` ;
- `HOSPITALIZATION_MEDICATION_ADMINISTER` ;
- `HOSPITALIZATION_CONSUMABLE_RECORD`.

`HOSPITALIZATION_MANAGE` reste temporairement au catalogue pour permettre la migration des rôles personnalisés, mais ne protège plus les six endpoints de cet incrément.

## Sémantique de sécurité

### Admission

`HOSPITALIZATION_ADMIT` autorise la création d’un séjour et l’affectation initiale d’un lit. Elle ne donne aucun droit d’écriture clinique pendant le séjour.

### Notes

`HOSPITALIZATION_NOTE_WRITE` autorise l’ajout de transmissions/observations dans le séjour actif.

### Consentement

`HOSPITALIZATION_CONSENT_RECORD` autorise la **traçabilité** d’un consentement et de sa pièce signée. La permission ne signifie pas que son détenteur peut décider à la place du patient ni réaliser l’information médicale requise.

### Soins

`HOSPITALIZATION_CARE_WRITE` autorise la traçabilité des soins et actes courants.

### Médicaments

`HOSPITALIZATION_MEDICATION_ADMINISTER` correspond uniquement à l’**administration effective**. Elle n’est pas une permission de prescription et n’est pas confondue avec les permissions pharmacie.

### Consommables

`HOSPITALIZATION_CONSUMABLE_RECORD` trace l’usage d’un consommable pour le patient. Elle ne permet pas de gérer le stock.

## Matrice système cible et implémentée

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
- [x] CI backend et frontend verte sur le head final #107 ;
- [x] documentation d’audit, conception technique et contrat API mis à jour au moment de l’incrément.

## État QA automatisée

Le head final de la PR #107 est `b0137e2003b5fc46fc68f95e7df87b2874d9ec7b`.

Le workflow GitHub Actions **#1011** a réellement exécuté ce head final et s’est terminé avec succès :

- `Detect changed stacks` : succès ;
- `Backend — Maven Build & Tests` : succès ;
- étape `Build and verify (Maven strict)` : succès ;
- `Frontend Angular — Build & Tests` : succès ;
- tests Angular : succès ;
- build Angular production : succès.

La PR #107 a ensuite été fusionnée dans `main` au commit `4df94f43ee5943a55ae60bac22794b1a8ff746a4`.

Les anciennes mentions indiquant que la PR restait Draft ou que la QA finale était bloquée décrivaient un état transitoire antérieur au run #1011 ; elles ne décrivent plus l’état du dépôt.

## Vérification du code actuel

Au SHA `main@078c3dc5f913f615910fad9f061085bc7acdcfec` :

- `RbacCatalog` expose toujours les six permissions dédiées ;
- `MEDECIN` ne reçoit pas par défaut l’administration médicamenteuse ni les consommables ;
- `INFIRMIER` ne reçoit pas l’admission ;
- `RESPONSABLE_HOSPITALISATION` ne reçoit pas les écritures cliniques ;
- `HospitalizationController` protège les six commandes par leurs autorités dédiées ;
- le frontend `PatientHospitalizationComponent` mappe les onglets sur les mêmes permissions ;
- `HOSPITALIZATION_MANAGE` reste uniquement comme permission legacy de migration et n’est pas utilisé comme fallback sur les six commandes.

## Compatibilité et déploiement

Aucune URL ni payload n’est modifié. En revanche, les rôles personnalisés qui dépendaient uniquement de `HOSPITALIZATION_MANAGE` doivent recevoir explicitement les nouvelles permissions avant mise en production.

Le bootstrap RBAC resynchronise le catalogue et les rôles système. Les JWT existants doivent être renouvelés après déploiement afin de refléter les nouvelles authorities.

Il ne faut pas traduire automatiquement `HOSPITALIZATION_MANAGE` vers l’ensemble des nouvelles permissions : cela recréerait la sur-autorisation que cet incrément supprime.

## Validations externes encore requises

- [ ] validation RSSI/DPO de la matrice ;
- [ ] validation direction médicale des responsabilités médecin/infirmier ;
- [ ] validation responsable hospitalisation du droit d’admission ;
- [ ] revue et migration des rôles personnalisés ;
- [ ] recette multi-profils avec comptes représentatifs ;
- [ ] renouvellement des JWT/sessions après synchronisation du catalogue au déploiement.

## Réconciliation documentaire post-merge — 2026-07-23

- [x] vérifier le SHA actuel de `main` ;
- [x] vérifier que #107 est bien fusionnée ;
- [x] vérifier le run CI final #1011 ;
- [x] vérifier le catalogue RBAC actuel ;
- [x] vérifier les annotations `@PreAuthorize` actuelles ;
- [x] vérifier le mapping Angular actuel ;
- [x] confirmer qu’aucun changement RBAC/hospitalisation n’est intervenu entre #107 et `main@078c3dc5` ;
- [ ] obtenir les validations externes listées ci-dessus.

## Hors périmètre

- prescription médicamenteuse ;
- validation à quatre yeux ;
- ABAC par unité, affectation ou relation de soin ;
- habilitations professionnelles et délégations datées ;
- clearance administrative/financière de sortie ;
- correction append-only des actes cliniques.