# HOS-RBAC-001-C — Conception technique

## 1. But

Supprimer l’usage opérationnel de `HOSPITALIZATION_MANAGE` et appliquer une permission correspondant à chaque commande d’écriture actuellement exposée par `HospitalizationController`.

## 2. Principes

1. **Une permission par intention métier** : l’autorisation suit la commande, pas l’écran.
2. **Pas de fallback legacy** : le super-droit historique ne doit pas continuer à ouvrir toutes les actions.
3. **Nommage fidèle** : une administration médicamenteuse n’est pas une prescription.
4. **Compatibilité explicite** : préserver les actes déjà réalisables par le médecin avec des droits distincts et révocables, jamais avec le super-droit.
5. **Deny by default** : toute nouvelle zone de l’interface retourne `false` si elle n’est pas explicitement mappée.
6. **Backend source de vérité** : la visibilité Angular ne remplace jamais `@PreAuthorize`.
7. **Migration explicite des rôles personnalisés** : aucune expansion silencieuse du super-droit.

## 3. Catalogue

Constantes ajoutées dans `RbacCatalog` :

```text
HOSPITALIZATION_ADMIT
HOSPITALIZATION_NOTE_WRITE
HOSPITALIZATION_CONSENT_RECORD
HOSPITALIZATION_CARE_RECORD
HOSPITALIZATION_MEDICATION_ADMINISTER
HOSPITALIZATION_CONSUMABLE_RECORD
```

`HOSPITALIZATION_MANAGE` reste défini comme permission historique afin que l’administration RBAC puisse encore détecter les rôles personnalisés qui la possèdent. Il n’est plus attribué aux rôles métier médecin, infirmier ou responsable hospitalisation.

## 4. Matrice par défaut

```text
MEDECIN
  + ADMIT
  + NOTE_WRITE
  + CONSENT_RECORD
  + CARE_RECORD
  + MEDICATION_ADMINISTER
  + CONSUMABLE_RECORD
  + TRANSFER
  + DISCHARGE_DECIDE

INFIRMIER
  + NOTE_WRITE
  + CARE_RECORD
  + MEDICATION_ADMINISTER
  + CONSUMABLE_RECORD
  + TRANSFER

RESPONSABLE_HOSPITALISATION
  + ADMIT
  + TRANSFER
  + PHYSICAL_DEPARTURE_CONFIRM
  + BED_OPERATIONAL_STATUS_MANAGE
  + BED_CLEANING_MANAGE
  + BED_MAINTENANCE_MANAGE
```

Le médecin et l’infirmier peuvent tracer un soin, une administration ou un consommable qu’ils ont effectivement réalisé. Chaque permission reste indépendante et peut être retirée dans un rôle personnalisé. `MEDICATION_ADMINISTER` n’autorise aucune prescription.

`ADMIN_CLINIQUE` reçoit les permissions dédiées via le catalogue complet de l’établissement. Les rôles hygiène et maintenance ne reçoivent aucune écriture clinique.

## 5. Protection des endpoints

| Méthode | Permission |
|---|---|
| `admitPatient` | `HOSPITALIZATION_ADMIT` |
| `addNote` | `HOSPITALIZATION_NOTE_WRITE` |
| `addConsent` | `HOSPITALIZATION_CONSENT_RECORD` |
| `addDailyCare` | `HOSPITALIZATION_CARE_RECORD` |
| `addMedicationAdministration` | `HOSPITALIZATION_MEDICATION_ADMINISTER` |
| `addPatientConsumption` | `HOSPITALIZATION_CONSUMABLE_RECORD` |

Les commandes de sortie et de transfert conservent leurs permissions déjà séparées. Le CRO conserve `CLINICAL_WRITE`.

## 6. Stratégie frontend

Le template existant passe un booléen `canModify` aux panneaux enfants. Pour éviter une réécriture visuelle risquée, le composant parent conserve ce contrat mais calcule la valeur selon l’état :

```text
pas de séjour actif -> HOSPITALIZATION_ADMIT
notes               -> HOSPITALIZATION_NOTE_WRITE
consents             -> HOSPITALIZATION_CONSENT_RECORD
cares                -> HOSPITALIZATION_CARE_RECORD
meds                 -> HOSPITALIZATION_MEDICATION_ADMINISTER
consumptions         -> HOSPITALIZATION_CONSUMABLE_RECORD
cro                  -> CLINICAL_WRITE
inconnu              -> false
```

Les méthodes parent qui créent une admission, un consentement ou un CRO revérifient la permission avant l’appel HTTP. Les panneaux enfants restent protégés côté visibilité et le serveur refuse toute commande non autorisée.

## 7. Migration des rôles

Avant déploiement :

1. extraire les rôles personnalisés contenant `HOSPITALIZATION_MANAGE` ;
2. identifier les tâches réellement attendues pour chaque rôle ;
3. attribuer uniquement les nouvelles permissions nécessaires ;
4. retirer le super-droit historique ;
5. resynchroniser le catalogue ;
6. renouveler les JWT ;
7. exécuter une recette positive et négative par profil.

Le bootstrap remappe les rôles système, mais ne doit pas prendre de décision métier à la place de l’administrateur pour les rôles personnalisés.

## 8. Tests

### Tests de catalogue

- chaque permission est enregistrée ;
- droits positifs attendus par rôle ;
- droits négatifs sensibles par rôle ;
- compatibilité du parcours médecin démontrée avec les nouveaux droits ;
- aucune écriture clinique pour hygiène/maintenance ;
- aucun `HOSPITALIZATION_MANAGE` pour les trois rôles opérationnels historiques.

### Tests de contrôleur

La réflexion vérifie l’expression `@PreAuthorize` exacte de chaque méthode et recherche toute réintroduction de `HOSPITALIZATION_MANAGE` dans les endpoints hospitaliers.

### Tests Angular

Le test du composant parent remplace le template pour isoler la politique de permissions. Il vérifie chaque onglet et confirme qu’un profil note-only ne peut pas exposer l’administration médicamenteuse.

## 9. Limites conscientes

- absence d’ABAC par unité et relation de soin ;
- absence d’habilitations cliniques structurées ;
- notes non typées ;
- admission non séparée en ordre médical et placement opérationnel ;
- administration non liée obligatoirement à une prescription ;
- consommables non reliés atomiquement au stock ;
- consentement sans workflow de révocation/correction append-only.

Ces limites restent visibles dans GAP-016, GAP-017, GAP-026, GAP-031 et GAP-032.
