# Guide Développeur — API d'Interopérabilité HL7 FHIR (EPIC-0011)

Ce guide décrit l'utilisation des endpoints conformes au standard **HL7 FHIR R4** exposés par Joprelys Connect pour l'échange de données de santé avec des applications partenaires de confiance.

---

## 1. Concepts de base

L'API FHIR de Joprelys Connect permet d'interroger dynamiquement le dossier patient unique (DPU) sous forme de ressources standardisées HL7 FHIR R4.

### Sécurisation & Rôles
Les endpoints FHIR sont réservés aux acteurs de santé authentifiés possédant l'un des rôles suivants :
* `MEDECIN`
* `INFIRMIER`
* `BIOLOGISTE`

Toute requête doit inclure le jeton JWT dans l'en-tête de requête :
`Authorization: Bearer <JWT_TOKEN>`

### Multi-tenant & Consentement
L'isolation multi-tenant s'applique strictement :
* Un praticien de la **Clinique A** a accès immédiat aux patients enregistrés au sein de la Clinique A.
* Pour accéder à un patient enregistré au sein de la **Clinique B**, le praticien doit préalablement disposer d'un consentement actif enregistré par le patient, ou passer par une procédure d'autorisation d'accès d'urgence (Brise-Glace). En cas de défaut de consentement, une erreur `403 Forbidden` avec le message `CONSENT_REQUIRED` est renvoyée.

---

## 2. Référence des Endpoints

### 2.1 Récupérer un Patient

Permet d'extraire la ressource `Patient` correspondant à l'identifiant unique.

* **URL** : `GET /fhir/Patient/{id}`
* **Headers requis** :
  * `Authorization: Bearer <JWT_TOKEN>`
  * `Accept: application/json`
* **Exemple de réponse (200 OK)** :
```json
{
  "resourceType": "Patient",
  "id": "11efb91a-f3d1-4ba3-9c1e-58bf21743428",
  "identifier": [
    {
      "system": "urn:oid:1.3.6.1.4.1.59367.1.1",
      "value": "DPU-20260702-000001"
    }
  ],
  "name": [
    {
      "use": "official",
      "text": "Jean Dupont",
      "family": "Dupont",
      "given": [
        "Jean"
      ]
    }
  ],
  "gender": "male",
  "birthDate": "1980-01-01",
  "telecom": [
    {
      "system": "phone",
      "value": "+237600000001"
    }
  ]
}
```

---

### 2.2 Récupérer une Rencontre (Visite)

Permet d'obtenir les détails d'une visite médicale au format FHIR `Encounter`.

* **URL** : `GET /fhir/Encounter/{id}`
* **Headers requis** :
  * `Authorization: Bearer <JWT_TOKEN>`
  * `Accept: application/json`
* **Exemple de réponse (200 OK)** :
```json
{
  "resourceType": "Encounter",
  "id": "2a6639b3-5a5a-49c0-bbe6-ae56f0a9ee15",
  "identifier": [
    {
      "value": "VIS-20260702-000001"
    }
  ],
  "status": "in-progress",
  "class": {
    "system": "http://terminology.hl7.org/CodeSystem/v3-ActCode",
    "code": "AMB",
    "display": "ambulatory"
  },
  "subject": {
    "reference": "Patient/11efb91a-f3d1-4ba3-9c1e-58bf21743428"
  },
  "period": {
    "start": "2026-07-02T12:00:00Z",
    "end": null
  }
}
```

---

### 2.3 Récupérer les Constantes Vitales (Observations)

Retourne un `Bundle` FHIR de type `searchset` regroupant l'ensemble des constantes enregistrées pour un patient. Chaque constante (température, poids, tension, glycémie, etc.) est projetée en tant que ressource `Observation` distincte avec sa codification LOINC.

* **URL** : `GET /fhir/Observation?patient={patientId}`
* **Headers requis** :
  * `Authorization: Bearer <JWT_TOKEN>`
  * `Accept: application/json`
* **Exemple de réponse (200 OK)** :
```json
{
  "resourceType": "Bundle",
  "type": "searchset",
  "total": 2,
  "entry": [
    {
      "resource": {
        "resourceType": "Observation",
        "id": "7bfed74b-temp",
        "status": "final",
        "category": [
          {
            "coding": [
              {
                "system": "http://terminology.hl7.org/CodeSystem/observation-category",
                "code": "vital-signs",
                "display": "Vital Signs"
              }
            ],
            "text": "Vital Signs"
          }
        ],
        "code": {
          "coding": [
            {
              "system": "http://loinc.org",
              "code": "8310-5",
              "display": "Body temperature"
            }
          ],
          "text": "Body temperature"
        },
        "subject": {
          "reference": "Patient/11efb91a-f3d1-4ba3-9c1e-58bf21743428"
        },
        "encounter": {
          "reference": "Encounter/2a6639b3-5a5a-49c0-bbe6-ae56f0a9ee15"
        },
        "effectiveDateTime": "2026-07-02T12:05:00Z",
        "valueQuantity": {
          "value": 37.5,
          "unit": "°C",
          "system": "http://unitsofmeasure.org",
          "code": "Cel"
        }
      }
    }
  ]
}
```

---

## 3. Gestion de l'Audit et Traçabilité

Toute interrogation réussie d'une ressource via l'API FHIR engendre l'écriture immédiate d'un log d'audit dans la table `audit_logs` avec l'action `READ_FHIR_RESOURCE`, traçant :
1. L'acteur (praticien) ayant fait la demande.
2. L'organisation (clinique) associée.
3. Le patient ciblé.
4. L'adresse IP et l'agent utilisateur (User-Agent) de l'appelant.
