# HOS-RBAC-001-C — Séparation des tâches cliniques d’hospitalisation

## Statut

**QA AUTOMATISÉE EN COURS — validations externes en attente**

## Contexte

L’audit `AUDIT-20260721` identifie GAP-016 : plusieurs opérations de nature différente restent regroupées derrière la permission historique `HOSPITALIZATION_MANAGE`.

Avant cet incrément, la même permission permet de :

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

`HOSPITALIZATION_MANAGE` reste temporairement au catalogue pour permettre la migration des rôles personnalisés, mais ne doit plus protéger ces endpoints.

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

## Matrice système cible

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
- [x] visibilité frontend alignée sur chaque permission ;
- [x] continuité urgence → hospitalisation alignée sur `HOSPITALIZATION_ADMIT` et testée en refus ;
- [ ] CI backend et frontend verte sur le head final ;
- [x] documentation d’audit, conception technique et contrat API mis à jour.

## Compatibilité et déploiement

Aucune URL ni payload n’est modifié. En revanche, les rôles personnalisés qui dépendaient uniquement de `HOSPITALIZATION_MANAGE` doivent recevoir explicitement les nouvelles permissions avant mise en production.

Le bootstrap RBAC resynchronise le catalogue et les rôles système. Les JWT existants doivent être renouvelés après déploiement afin de refléter les nouvelles authorities.

Il ne faut pas traduire automatiquement `HOSPITALIZATION_MANAGE` vers l’ensemble des nouvelles permissions : cela recréerait la sur-autorisation que cet incrément supprime.

## Validations externes encore requises

- validation RSSI/DPO de la matrice ;
- validation direction médicale des responsabilités médecin/infirmier ;
- validation responsable hospitalisation du droit d’admission ;
- revue et migration des rôles personnalisés ;
- recette multi-profils avec comptes représentatifs ;
- renouvellement des JWT/sessions après synchronisation du catalogue.

## Hors périmètre

- prescription médicamenteuse ;
- validation à quatre yeux ;
- ABAC par unité, affectation ou relation de soin ;
- habilitations professionnelles et délégations datées ;
- clearance administrative/financière de sortie ;
- correction append-only des actes cliniques.
