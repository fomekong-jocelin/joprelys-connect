# STORY-2301 — Identité provisoire URG-TEMP

## Objectif fonctionnel

Permettre à un professionnel habilité de créer immédiatement un dossier patient provisoire lorsqu'une personne arrive inconsciente, non identifiée ou avec une identité non fiable, sans inventer de nom, de sexe, de date de naissance, de ville ou de téléphone.

Cette story fournit uniquement la fondation d'identité. L'ouverture atomique du dossier d'urgence et du triage relève de STORY-2302.

## États d'identité

| État | Sens | Transitions autorisées |
|---|---|---|
| `PROVISIONAL_URGENCY` | Identité inconnue ou non fiable créée en urgence | `DECLARED`, `VERIFIED`, `MERGED` |
| `DECLARED` | Une identité a été déclarée, mais pas encore vérifiée | `VERIFIED`, `MERGED` |
| `VERIFIED` | Identité administrativement vérifiée | `MERGED` |
| `MERGED` | Dossier source rapproché d'un dossier patient canonique | aucune |

Un patient existant est conservé en `VERIFIED` lors de la migration V58.

## Numérotation

Le dossier provisoire reçoit :

- un numéro métier visible `URG-TEMP-YYYYMMDD-XXXXXX` ;
- un numéro DPU technique unique ;
- un numéro patient local technique unique.

Le numéro URG-TEMP ne doit jamais être remplacé ni réattribué. Après régularisation, il reste un alias de recherche audité.

## Données observables

La création accepte uniquement les informations disponibles et non présentées comme des vérités administratives :

- sexe apparent ;
- tranche d'âge estimée ;
- description physique ;
- date/heure et lieu de découverte ou d'arrivée ;
- déclarations d'identité optionnelles avec source et niveau de confiance.

## Déclarations d'identité

Chaque déclaration conserve :

- le champ concerné ;
- la valeur déclarée ;
- la source (`PATIENT`, `ACCOMPANYING_PERSON`, `WITNESS`, `TRANSPORTER`, etc.) ;
- les détails de source ;
- le niveau de confiance ;
- le statut de vérification ;
- l'auteur et l'horodatage.

Une déclaration initiale ne peut pas être enregistrée comme `VERIFIED`.

## Contrat API

### Création

`POST /api/patients/provisional`

Permissions : `PATIENT_WRITE` ou rôle historique autorisé (`AGENT_ACCUEIL`, `INFIRMIER`, `MEDECIN`, `ADMIN_CLINIQUE`).

Exemple minimal :

```json
{}
```

Exemple avec observations :

```json
{
  "apparentGender": "MASCULIN",
  "estimatedAgeRange": "35-45",
  "physicalDescription": "Cicatrice au front",
  "foundAt": "2026-07-11T10:30:00Z",
  "foundLocation": "Bonamoussadi",
  "confidenceLevel": "LOW",
  "identityDeclarations": [
    {
      "fieldName": "fullName",
      "value": "Nom déclaré non vérifié",
      "sourceType": "ACCOMPANYING_PERSON",
      "sourceDetails": "Personne ayant amené le patient",
      "confidenceLevel": "LOW"
    }
  ]
}
```

La réponse contient le dossier provisoire et les déclarations enregistrées.

### Recherche

`GET /api/patients?q=<URG-TEMP>` recherche le numéro provisoire uniquement dans le tenant courant.

Les recherches globales inter-établissements excluent les identités `PROVISIONAL_URGENCY` afin d'éviter toute exposition d'un dossier non vérifié.

## Modèle de données

Migration : `V58__provisional_emergency_patient_identity.sql`.

Évolutions principales :

- identité administrative nullable pour les patients provisoires ;
- nouveaux champs d'état et d'observation sur `patients` ;
- table `patient_identity_declarations` ;
- table `patient_identity_status_history` ;
- index tenant/statut et unicité du numéro provisoire.

## Sécurité et audit

- création tenantée à partir du compte authentifié ;
- aucune organisation fournie par le client ;
- dossier provisoire exclu de la recherche globale ;
- événement d'audit `CREATE_PROVISIONAL_EMERGENCY_PATIENT` ;
- historique initial du statut d'identité ;
- aucun faux renseignement administratif requis.

## Tests

La campagne automatisée couvre :

- création minimale sans identité fictive ;
- création avec observations et déclarations sourcées ;
- refus d'un niveau de confiance `VERIFIED` à la création ;
- recherche limitée au tenant propriétaire ;
- génération concurrente de numéros distincts ;
- compatibilité H2 ;
- migration complète PostgreSQL 16 via Testcontainers ;
- non-régression du parcours patient vérifié.

## Hors périmètre

- création atomique patient + urgence + triage ;
- incapacité à consentir et base légale d'urgence ;
- accompagnant, témoin, transporteur et effets personnels ;
- régularisation de l'identité ;
- rapprochement avec un DPU ;
- intégration hospitalisation, documents et finance différée ;
- workspace Angular complet.

Ces éléments sont couverts par les stories #42, #44, #45, #46 et #47.

## Rollback

La migration V58 est additive, à l'exception de l'assouplissement des contraintes NOT NULL. Avant rollback :

1. vérifier qu'aucun patient `PROVISIONAL_URGENCY` n'existe ;
2. régulariser ou archiver tout dossier provisoire ;
3. rétablir les valeurs obligatoires avant de remettre les contraintes NOT NULL ;
4. sauvegarder les tables de déclarations et d'historique ;
5. supprimer les nouvelles colonnes/tables uniquement après validation métier et données.
