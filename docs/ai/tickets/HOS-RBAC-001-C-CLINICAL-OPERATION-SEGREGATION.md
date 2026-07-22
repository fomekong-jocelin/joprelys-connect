# HOS-RBAC-001-C — Séparation des écritures hospitalières

## Contexte

Le contrôleur d’hospitalisation utilisait encore `HOSPITALIZATION_MANAGE` pour six opérations hétérogènes : admission, notes, consentements, soins, administration médicamenteuse et consommables. Cette agrégation empêchait une séparation fiable des tâches entre médecin, infirmier et responsable hospitalisation.

## Objectif

Remplacer l’autorisation historique par des permissions métier explicites et testables, sans confondre prescription médicale et administration infirmière.

## Permissions

| Permission | Opération |
|---|---|
| `HOSPITALIZATION_ADMIT` | créer un séjour et affecter un lit |
| `HOSPITALIZATION_NOTE_WRITE` | ajouter une observation ou transmission |
| `HOSPITALIZATION_CONSENT_MANAGE` | enregistrer un consentement et sa preuve |
| `HOSPITALIZATION_CARE_WRITE` | tracer un soin ou acte infirmier |
| `HOSPITALIZATION_MEDICATION_ADMINISTER` | tracer l’administration effective d’un médicament |
| `HOSPITALIZATION_CONSUMABLE_MANAGE` | tracer les consommables utilisés |

Aucune permission `PRESCRIBE` n’est créée dans cet incrément : l’API actuelle ne possède pas de commande de prescription hospitalière distincte.

## Matrice des rôles système

| Rôle | Admission | Notes | Consentement | Soins | Administration médicament | Consommables |
|---|---:|---:|---:|---:|---:|---:|
| `ADMIN_CLINIQUE` | Oui | Oui | Oui | Oui | Oui | Oui |
| `MEDECIN` | Oui | Oui | Oui | Non | Non | Non |
| `INFIRMIER` | Non | Oui | Non | Oui | Oui | Oui |
| `RESPONSABLE_HOSPITALISATION` | Oui | Non | Non | Non | Non | Non |
| `AGENT_HYGIENE` | Non | Non | Non | Non | Non | Non |
| `TECHNICIEN_MAINTENANCE` | Non | Non | Non | Non | Non | Non |

Les utilisateurs cumulant plusieurs rôles reçoivent l’union des permissions.

## Critères d’acceptation

- aucun endpoint d’écriture concerné n’utilise encore `HOSPITALIZATION_MANAGE` ;
- les six permissions sont créées de façon idempotente au démarrage ;
- le bootstrap retire la permission historique aux trois rôles migrés ;
- le médecin ne reçoit ni soins infirmiers, ni administration, ni consommables ;
- l’infirmier ne reçoit ni admission, ni consentement, ni décision médicale de sortie ;
- le responsable hospitalisation ne reçoit que l’admission dans ce sous-domaine ;
- les annotations Spring Security et la matrice sont couvertes par tests ;
- les rôles personnalisés peuvent attribuer les nouvelles permissions depuis le catalogue persistant ;
- la prescription hospitalière reste explicitement hors périmètre tant que son domaine n’est pas modélisé.

## Risques et déploiement

- les JWT existants doivent être renouvelés après déploiement ;
- les rôles personnalisés utilisant `HOSPITALIZATION_MANAGE` doivent être revus avant suppression définitive de cette permission ;
- l’interface Angular doit être alignée sur les permissions spécialisées avant passage de la PR en état fusionnable ;
- une recette négative par profil est requise.

## Impact audit

GAP-016 reste `PARTIAL` : la séparation des écritures du séjour est renforcée, mais l’ABAC par unité/relation de soin, la clearance administrative et la prescription hospitalière restent ouverts.
