# HOS-RBAC-001-C — Séparation des responsabilités cliniques d’hospitalisation

## Contexte

L’audit `AUDIT-20260721` identifie GAP-016 : plusieurs écritures d’hospitalisation utilisent encore la permission historique `HOSPITALIZATION_MANAGE`. Cette permission permettait à un même profil de créer un séjour, écrire des notes, gérer un consentement, tracer un soin, une administration médicamenteuse et des consommables.

Les incréments HOS-RBAC-001-A/B ont déjà séparé la capacité des lits, le transfert, la décision médicale de sortie, le nettoyage et la maintenance. HOS-DIS-001-A a séparé la décision médicale du départ physique.

## Objectif

Appliquer le principe du moindre privilège et la séparation des tâches aux écritures cliniques restantes, sans confondre prescription médicale, administration infirmière et coordination administrative.

## Permissions dédiées

- `HOSPITALIZATION_ADMIT` — créer administrativement un séjour et affecter un lit après décision d’hospitalisation ;
- `HOSPITALIZATION_NOTE_WRITE` — ajouter une note au séjour ;
- `HOSPITALIZATION_CONSENT_MANAGE` — enregistrer un consentement clinique et sa preuve ;
- `HOSPITALIZATION_CARE_WRITE` — tracer un soin journalier ;
- `HOSPITALIZATION_MEDICATION_ADMINISTER` — tracer l’administration effective d’un médicament ;
- `HOSPITALIZATION_CONSUMABLE_WRITE` — tracer les consommables utilisés.

`HOSPITALIZATION_MANAGE` reste dans le catalogue uniquement comme permission historique pour identifier et migrer les rôles personnalisés. Les endpoints concernés ne l’acceptent plus.

## Matrice système cible

| Rôle | Admission | Notes | Consentement | Soins | Administration médicament | Consommables |
|---|---:|---:|---:|---:|---:|---:|
| `ADMIN_CLINIQUE` | Oui | Oui | Oui | Oui | Oui | Oui |
| `MEDECIN` | Oui | Oui | Oui | Oui | Non | Non |
| `INFIRMIER` | Non | Oui | Non | Oui | Oui | Oui |
| `RESPONSABLE_HOSPITALISATION` | Oui | Non | Non | Non | Non | Non |
| `AGENT_HYGIENE` | Non | Non | Non | Non | Non | Non |
| `TECHNICIEN_MAINTENANCE` | Non | Non | Non | Non | Non | Non |

## Décisions de sécurité

### Prescription ≠ administration

L’endpoint `/medication-administrations` persiste `administeredBy` et `administeredAt`. Il représente donc une administration réalisée et non une prescription. Le rôle `MEDECIN` ne reçoit pas automatiquement `HOSPITALIZATION_MEDICATION_ADMINISTER`; la prescription reste un acte clinique distinct.

### Consentement

L’endpoint actuel combine attestation du consentement et dépôt documentaire. Tant que ces deux opérations ne sont pas séparées, la permission système est attribuée au médecin et à l’administrateur clinique, pas au rôle infirmier par défaut.

### Admission

Le système ne possède pas encore une demande/ordonnance d’admission distincte de l’exécution administrative. `HOSPITALIZATION_ADMIT` est donc attribué au médecin et au responsable hospitalisation. HOS-ADM-001 devra séparer décision, préadmission et exécution.

### Notes

Les notes de séjour sont encore un flux générique ne distinguant pas note médicale et transmission infirmière. Les deux profils reçoivent temporairement `HOSPITALIZATION_NOTE_WRITE`; cette dette reste explicite jusqu’à la refonte clinique.

## Critères d’acceptation

1. Aucun des six endpoints d’écriture ne dépend de `HOSPITALIZATION_MANAGE`.
2. Chaque endpoint exige exactement sa permission dédiée.
3. Le médecin ne possède pas par défaut le droit d’administration médicamenteuse.
4. L’infirmier ne peut ni créer un séjour ni gérer un consentement clinique par défaut.
5. Le responsable hospitalisation ne peut pas écrire des soins, notes cliniques, administrations médicamenteuses ou consommables.
6. Les rôles hygiène et maintenance restent isolés des écritures cliniques.
7. `ADMIN_CLINIQUE` conserve l’ensemble des droits établissement via le catalogue complet.
8. Les tests de catalogue et d’annotations RBAC sont verts.
9. Les rôles personnalisés utilisant `HOSPITALIZATION_MANAGE` sont explicitement signalés comme nécessitant une revue avant déploiement.

## Compatibilité et déploiement

Il s’agit d’un durcissement volontaire de sécurité. Un rôle personnalisé qui ne possède que `HOSPITALIZATION_MANAGE` perdra l’accès aux six écritures après renouvellement du contexte d’autorisation. Aucun fan-out automatique vers les six nouvelles permissions n’est réalisé, car il recréerait le privilège excessif que cette story supprime.

Avant production :

1. inventorier les rôles personnalisés contenant `HOSPITALIZATION_MANAGE` ;
2. attribuer uniquement les nouvelles permissions réellement nécessaires ;
3. resynchroniser le catalogue ;
4. renouveler les JWT/sessions ;
5. réaliser une recette positive et négative par profil.

## Hors périmètre

- séparation note médicale / transmission infirmière ;
- ordre médical d’admission distinct de l’exécution administrative ;
- séparation capture documentaire / attestation clinique du consentement ;
- ABAC par unité, affectation ou relation de soin ;
- prescription médicamenteuse ;
- clearance administrative/financière de sortie.
