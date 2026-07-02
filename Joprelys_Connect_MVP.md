# Document MVP — Joprelys Connect

**Projet :** Joprelys Connect  
**Version :** 1.0  
**Date :** 2026-07-01  
**Objet :** MVP opérationnel pour une première clinique pilote  
**Durée cible :** 8 à 12 semaines  
**Objectif :** Prouver la valeur de Joprelys Connect avec un dossier patient, un historique de visites, des documents PDF vérifiables et un socle API sécurisé.

---

## 1. Résumé du MVP

Le MVP de **Joprelys Connect** doit être volontairement limité et solide.

Il ne doit pas chercher à connecter tout le système de santé dès le départ. Il doit démontrer qu’une clinique peut :

1. Créer un patient.
2. Lui attribuer un numéro DPU Joprelys.
3. Enregistrer une visite.
4. Saisir des constantes.
5. Saisir une consultation.
6. Créer une prescription simple.
7. Générer un document PDF médical propre.
8. Vérifier ce document par QR code.
9. Conserver l’historique.
10. Tracer les accès.

Le MVP doit être suffisamment professionnel pour servir de démonstration auprès d’une clinique, d’un laboratoire, d’une pharmacie ou d’une institution.

---

## 2. Objectif du MVP

### Objectif principal

Prouver qu’un patient peut avoir un dossier numérique unique, consultable dans une clinique pilote, avec génération de documents médicaux vérifiables.

### Objectifs secondaires

- Réduire la perte de documents médicaux.
- Donner une image moderne et crédible aux établissements.
- Montrer que chaque PDF peut être authentifié.
- Préparer l’intégration future avec AllôPharma, laboratoire et autres plateformes.
- Créer une base technique propre pour les versions suivantes.

---

## 3. Périmètre MVP

## 3.1 Inclus

Le MVP inclut :

- gestion d’une clinique pilote ;
- gestion des utilisateurs internes ;
- création patient ;
- numéro DPU Joprelys ;
- recherche patient ;
- création visite ;
- saisie constantes ;
- consultation médicale ;
- diagnostic ;
- conseils ;
- prescription simple ;
- génération PDF consultation ;
- QR code de vérification ;
- page publique de vérification ;
- historique patient ;
- audit logs simples ;
- API REST documentée ;
- interface web professionnelle minimale.

## 3.2 Exclu du MVP

Le MVP n’inclut pas encore :

- portail patient complet ;
- consentement patient avancé ;
- accès externe inter-hôpital complet ;
- laboratoire complet ;
- pharmacie complète ;
- paiement ;
- assurance ;
- application mobile ;
- HL7 FHIR complet ;
- hospitalisation complète ;
- imagerie ;
- IA médicale.

---

## 4. Acteurs MVP

### 4.1 Agent d’accueil

Responsabilités :

- créer un patient ;
- rechercher un patient ;
- ouvrir une visite ;
- imprimer ou partager la fiche patient.

### 4.2 Infirmier / agent de tri

Responsabilités :

- saisir les constantes ;
- ajouter une observation rapide.

### 4.3 Médecin

Responsabilités :

- consulter le patient ;
- voir les constantes ;
- saisir symptômes, examen clinique, diagnostic ;
- prescrire ;
- générer le document PDF.

### 4.4 Administrateur clinique

Responsabilités :

- gérer utilisateurs ;
- consulter les visites ;
- consulter les documents générés.

### 4.5 Administrateur Joprelys

Responsabilités :

- créer la clinique ;
- gérer les accès ;
- superviser les logs ;
- corriger incidents techniques.

### 4.6 Vérificateur externe

Responsabilités :

- scanner le QR code ;
- vérifier que le document est authentique ;
- voir uniquement les informations publiques du document.

---

## 5. Fonctionnalités MVP détaillées

## Module MVP-01 — Authentification

### Fonctionnalités

- Connexion email + mot de passe.
- Déconnexion.
- Mot de passe hashé.
- Sessions sécurisées.
- Rôles simples.

### Rôles MVP

- ADMIN_JOPRELYS
- ADMIN_CLINIQUE
- AGENT_ACCUEIL
- INFIRMIER
- MEDECIN

### Critères d’acceptation

- Un utilisateur sans compte ne peut pas se connecter.
- Un utilisateur désactivé ne peut pas se connecter.
- Le rôle détermine les menus visibles.
- Une connexion réussie est journalisée.
- Un échec de connexion est journalisé.

---

## Module MVP-02 — Gestion clinique pilote

### Fonctionnalités

- Créer la clinique.
- Modifier les informations de base.
- Associer des utilisateurs à la clinique.
- Afficher le nom et le logo sur les documents.

### Données

- nom ;
- téléphone ;
- email ;
- adresse ;
- ville ;
- logo ;
- responsable ;
- statut.

### Critères d’acceptation

- Le document PDF affiche le nom de la clinique.
- Le QR code renvoie vers une page mentionnant la clinique émettrice.
- Une clinique inactive ne peut pas générer de documents.

---

## Module MVP-03 — Création patient

### Fonctionnalités

- Créer un patient.
- Générer un numéro DPU.
- Générer un numéro patient local.
- Rechercher un patient.
- Voir le profil patient.

### Champs obligatoires

- nom complet ;
- sexe ;
- date de naissance ou âge approximatif ;
- téléphone ;
- ville / quartier ;
- contact d’urgence si disponible.

### Champs optionnels

- email ;
- adresse complète ;
- groupe sanguin ;
- allergies ;
- antécédents ;
- photo.

### Format recommandé des identifiants

```text
DPU-JOP-YYYYMMDD-000001
PAT-YYYYMMDD-000001
```

### Critères d’acceptation

- Un patient créé a toujours un DPU.
- Le système empêche deux DPU identiques.
- La recherche fonctionne par nom, téléphone ou DPU.
- Le profil affiche les visites et documents associés.

---

## Module MVP-04 — Création visite

### Fonctionnalités

- Ouvrir une visite.
- Renseigner le service.
- Renseigner le motif.
- Associer la visite à un médecin.
- Clôturer la visite.

### Statuts

```text
EN_COURS
TERMINEE
ANNULEE
```

### Critères d’acceptation

- Une visite est toujours liée à un patient.
- Une visite est toujours liée à la clinique.
- Une visite terminée apparaît dans l’historique.
- Une visite terminée ne peut pas être supprimée sans trace.

---

## Module MVP-05 — Constantes vitales

### Fonctionnalités

- Saisir température.
- Saisir poids.
- Saisir taille.
- Calculer l’IMC.
- Saisir pouls.
- Saisir tension artérielle.
- Saisir SpO2.
- Saisir fréquence respiratoire.
- Saisir glycémie.
- Ajouter une observation.

### Unités

| Donnée | Unité |
|---|---|
| Température | °C |
| Poids | kg |
| Taille | m |
| IMC | kg/m² |
| Pouls | bpm |
| Tension | mmHg |
| SpO2 | % |
| Fréquence respiratoire | cpm |
| Glycémie | g/L |

### Critères d’acceptation

- La taille ne peut pas être enregistrée comme `1.68 cm`.
- L’IMC est calculé automatiquement si poids et taille sont présents.
- Les constantes s’affichent dans la consultation.
- Les constantes s’affichent dans le PDF.

---

## Module MVP-06 — Consultation médicale

### Fonctionnalités

- Saisir symptômes.
- Saisir examen clinique.
- Saisir diagnostic.
- Saisir conclusion.
- Saisir conseils.
- Ajouter une prescription simple.
- Générer un document.

### Champs

- motif ;
- symptômes ;
- examen clinique ;
- diagnostic ;
- conseils ;
- retour / suivi recommandé.

### Critères d’acceptation

- Une consultation est liée à une visite.
- Une consultation est liée à un médecin.
- Une consultation peut générer un PDF.
- Une consultation terminée est visible dans l’historique patient.

---

## Module MVP-07 — Prescription simple

### Fonctionnalités

- Ajouter un médicament.
- Saisir dosage.
- Saisir posologie.
- Saisir durée.
- Saisir quantité.
- Ajouter instruction.

### Champs médicament

- nom ;
- dosage ;
- forme ;
- posologie ;
- durée ;
- quantité ;
- instructions.

### Critères d’acceptation

- Une prescription doit être liée à une consultation.
- Les médicaments apparaissent dans le PDF.
- Le texte généré ne doit pas répéter “jours jours”.
- L’ordonnance doit pouvoir recevoir un numéro dans une version suivante.

---

## Module MVP-08 — Génération PDF

### Objectif

Générer un document propre à partir des données structurées.

### Document MVP

Type recommandé :

**Fiche patient — Synthèse de consultation**

### Contenu

- logo clinique ;
- nom clinique ;
- contacts clinique ;
- numéro DPU ;
- numéro patient local ;
- numéro document ;
- date ;
- identité patient ;
- contact d’urgence ;
- allergies ;
- antécédents ;
- visite ;
- constantes ;
- symptômes ;
- examen clinique ;
- diagnostic ;
- conseils ;
- médicaments prescrits ;
- signature patient si disponible ;
- signature médecin ;
- QR code ;
- mention de confidentialité.

### Numéro document

```text
DOC-CONS-YYYYMMDD-000001
```

### Mention obligatoire

```text
Document médical confidentiel généré par Joprelys Connect.
La vérification de ce document est possible via le QR code.
L’accès au dossier patient complet nécessite l’accord du patient
ou une autorisation professionnelle encadrée.
```

### Critères d’acceptation

- Le PDF tient idéalement sur une ou deux pages.
- Le PDF contient un QR code.
- Le PDF contient un numéro document unique.
- Le PDF contient le DPU.
- Le PDF est stocké.
- Le PDF est lié au dossier patient.
- Le PDF est vérifiable en ligne.

---

## Module MVP-09 — QR code et vérification publique

### Fonctionnalités

- Générer une URL de vérification.
- Générer un QR code.
- Afficher une page publique de vérification.
- Vérifier le statut du document.

### Page publique

La page publique affiche :

- document authentique ou non ;
- numéro document ;
- type document ;
- clinique émettrice ;
- date d’émission ;
- statut : valide, annulé, remplacé ;
- nom du médecin ou service si applicable.

La page publique ne doit pas afficher :

- diagnostic ;
- médicaments ;
- allergies ;
- antécédents ;
- résultats ;
- données sensibles.

### Critères d’acceptation

- Le QR code ouvre une page accessible publiquement.
- Le numéro document est affiché.
- Le statut est affiché.
- Aucune donnée médicale sensible n’est affichée.
- Un document annulé apparaît comme annulé.

---

## Module MVP-10 — Historique patient

### Fonctionnalités

- Voir toutes les visites d’un patient.
- Voir tous les documents générés.
- Télécharger un document autorisé.
- Voir les constantes et diagnostics des visites passées selon rôle.

### Critères d’acceptation

- Le profil patient affiche les visites par date.
- Chaque visite affiche son statut.
- Chaque document est associé à une visite.
- Le médecin peut voir l’historique du patient dans la clinique pilote.

---

## Module MVP-11 — Audit logs simples

### Événements à tracer au MVP

- connexion ;
- échec connexion ;
- création patient ;
- modification patient ;
- création visite ;
- saisie consultation ;
- génération PDF ;
- téléchargement PDF ;
- scan QR code ;
- annulation document.

### Critères d’acceptation

- Chaque événement sensible crée un log.
- Un admin peut consulter les logs.
- Les logs affichent date, utilisateur, action et ressource.
- Les logs ne peuvent pas être modifiés depuis l’interface métier.

---

## 6. Écrans MVP à concevoir

### Portail professionnel

1. Connexion.
2. Tableau de bord.
3. Liste patients.
4. Recherche patient.
5. Création patient.
6. Profil patient.
7. Nouvelle visite.
8. Saisie constantes.
9. Consultation médicale.
10. Prescription simple.
11. Aperçu document.
12. Historique documents.
13. Vérification document.
14. Administration utilisateurs.
15. Logs simples.

### Page publique

1. Vérification document valide.
2. Vérification document invalide.
3. Document annulé.
4. Document remplacé.

---

## 7. API MVP

### Auth

```http
POST /api/v1/auth/login
POST /api/v1/auth/logout
GET /api/v1/auth/me
```

### Patients

```http
POST /api/v1/patients
GET /api/v1/patients
GET /api/v1/patients/{patientId}
PUT /api/v1/patients/{patientId}
GET /api/v1/patients/search?q=
```

### Visites

```http
POST /api/v1/visits
GET /api/v1/patients/{patientId}/visits
GET /api/v1/visits/{visitId}
PATCH /api/v1/visits/{visitId}/close
```

### Constantes

```http
POST /api/v1/visits/{visitId}/vitals
GET /api/v1/visits/{visitId}/vitals
```

### Consultations

```http
POST /api/v1/consultations
GET /api/v1/consultations/{consultationId}
PUT /api/v1/consultations/{consultationId}
```

### Prescriptions

```http
POST /api/v1/consultations/{consultationId}/prescriptions
GET /api/v1/consultations/{consultationId}/prescriptions
```

### Documents

```http
POST /api/v1/consultations/{consultationId}/documents
GET /api/v1/documents/{documentId}
GET /api/v1/documents/{documentId}/download
GET /api/v1/public/documents/{documentNumber}/verify
POST /api/v1/documents/{documentId}/cancel
```

### Audit

```http
GET /api/v1/audit
GET /api/v1/audit/patients/{patientId}
```

---

## 8. Modèle de données MVP

### Tables minimales

- `organizations`
- `users`
- `patients`
- `visits`
- `vital_signs`
- `consultations`
- `prescriptions`
- `prescription_items`
- `documents`
- `audit_logs`

### `organizations`

```sql
id
name
type
phone
email
address
city
logo_url
status
created_at
updated_at
```

### `users`

```sql
id
organization_id
full_name
email
phone
password_hash
role
status
last_login_at
created_at
updated_at
```

### `patients`

```sql
id
organization_id
global_patient_number
local_patient_number
full_name
gender
birth_date
phone
city
district
address
emergency_contact_name
emergency_contact_phone
allergies
medical_history
status
created_at
updated_at
```

### `visits`

```sql
id
visit_number
patient_id
organization_id
service
reason
status
arrival_at
closed_at
created_by
created_at
updated_at
```

### `vital_signs`

```sql
id
visit_id
temperature_c
weight_kg
height_m
bmi
blood_pressure
pulse_bpm
spo2_percent
respiratory_rate_cpm
glycemia_gl
pain_score
notes
created_by
created_at
```

### `consultations`

```sql
id
visit_id
patient_id
practitioner_id
symptoms
clinical_exam
diagnosis
conclusion
advice
status
created_at
updated_at
```

### `prescriptions`

```sql
id
consultation_id
patient_id
practitioner_id
status
created_at
updated_at
```

### `prescription_items`

```sql
id
prescription_id
medicine_name
dosage
form
frequency
duration
quantity
instructions
created_at
```

### `documents`

```sql
id
document_number
patient_id
visit_id
consultation_id
organization_id
author_user_id
document_type
title
file_url
hash
qr_code_url
verification_url
status
version
created_at
cancelled_at
```

### `audit_logs`

```sql
id
actor_user_id
actor_role
organization_id
patient_id
resource_type
resource_id
action
status
ip_address
user_agent
created_at
```

---

## 9. Sécurité MVP non négociable

Le MVP doit obligatoirement inclure :

1. HTTPS en production.
2. Mots de passe hashés.
3. Rôles et permissions.
4. Accès PDF journalisé.
5. QR public sans données médicales sensibles.
6. Tokens sécurisés.
7. Sauvegarde base de données.
8. Sauvegarde documents.
9. Variables d’environnement pour secrets.
10. Séparation environnement test / production.
11. Validation des données en entrée.
12. Protection contre l’accès horizontal : un utilisateur ne doit pas pouvoir changer un ID pour voir un autre dossier non autorisé.

---

## 10. Sprints proposés

### Sprint 0 — Préparation

Durée : 1 semaine

Livrables :

- dépôt Git ;
- architecture projet ;
- base de données ;
- CI/CD simple ;
- environnement dev ;
- conventions ;
- maquettes validées.

### Sprint 1 — Authentification et clinique

Durée : 1 à 2 semaines

Livrables :

- connexion ;
- rôles ;
- clinique ;
- utilisateurs ;
- layout interface.

### Sprint 2 — Patient

Durée : 1 à 2 semaines

Livrables :

- création patient ;
- DPU ;
- recherche patient ;
- profil patient.

### Sprint 3 — Visite et constantes

Durée : 1 à 2 semaines

Livrables :

- création visite ;
- saisie constantes ;
- IMC ;
- historique visites.

### Sprint 4 — Consultation et prescription

Durée : 1 à 2 semaines

Livrables :

- consultation médicale ;
- diagnostic ;
- conseils ;
- prescription simple.

### Sprint 5 — PDF et QR code

Durée : 1 à 2 semaines

Livrables :

- modèle PDF ;
- génération document ;
- hash ;
- QR code ;
- stockage fichier.

### Sprint 6 — Vérification publique et logs

Durée : 1 à 2 semaines

Livrables :

- page vérification ;
- statut document ;
- audit logs ;
- annulation document.

### Sprint 7 — Stabilisation pilote

Durée : 1 à 2 semaines

Livrables :

- tests ;
- corrections ;
- documentation utilisateur ;
- documentation API ;
- démonstration pilote.

---

## 11. Critères de réussite du MVP

Le MVP est réussi si :

- une clinique peut créer un patient en moins de 2 minutes ;
- une consultation peut être enregistrée sans blocage ;
- un PDF propre peut être généré ;
- le QR code vérifie le document ;
- le document ne révèle pas tout le dossier publiquement ;
- l’historique du patient est visible ;
- les accès importants sont journalisés ;
- la démonstration est compréhensible par un médecin ou responsable de clinique.

---

## 12. Tests MVP

### Tests fonctionnels

- création patient ;
- recherche patient ;
- création visite ;
- saisie constantes ;
- consultation ;
- prescription ;
- génération PDF ;
- vérification QR ;
- annulation document ;
- consultation historique.

### Tests sécurité

- accès sans connexion ;
- accès avec mauvais rôle ;
- tentative de consulter un patient d’une autre clinique ;
- tentative de télécharger un document sans droit ;
- scan QR code ;
- QR code ne montre pas de données médicales sensibles.

### Tests qualité

- PDF lisible ;
- unités correctes ;
- pas de répétition de mots ;
- accents corrects ;
- mise en page propre ;
- logo affiché ;
- numéro document visible ;
- QR code lisible.

---

## 13. Démonstration cible

Scénario de démonstration :

1. L’agent d’accueil crée Chantal Demo.
2. Le système génère `DPU-JOP-20260701-000103`.
3. L’agent ouvre une visite.
4. L’infirmier saisit les constantes.
5. Le médecin saisit la consultation.
6. Le médecin ajoute une prescription.
7. Le système génère une fiche patient PDF.
8. Le PDF contient un QR code.
9. Un téléphone scanne le QR.
10. La page affiche : document authentique, clinique, date, statut valide.
11. Le médecin ouvre l’historique et voit la visite conservée.

---

## 14. Livrables MVP

### Livrables produit

- maquettes écrans MVP ;
- parcours utilisateur ;
- modèle PDF ;
- matrice des rôles ;
- guide utilisateur clinique ;
- guide démonstration.

### Livrables techniques

- backend API ;
- base de données ;
- portail web ;
- génération PDF ;
- QR code ;
- stockage documents ;
- audit logs ;
- documentation Swagger ;
- scripts de déploiement ;
- sauvegarde.

### Livrables qualité

- plan de test ;
- rapport de test ;
- liste des anomalies ;
- checklist sécurité ;
- changelog MVP.

---

## 15. Décisions importantes pour ne pas se disperser

### À faire maintenant

- DPU.
- Historique patient.
- Visite.
- Constantes.
- Consultation.
- Prescription simple.
- PDF vérifiable.
- QR code.
- Logs.

### À faire après le MVP

- Portail patient.
- Consentement avancé.
- Accès inter-hôpital.
- Laboratoire.
- Pharmacie.
- AllôPharma.
- FHIR.

### À éviter au MVP

- vouloir tout connecter ;
- faire une application mobile trop tôt ;
- afficher trop de données dans le QR public ;
- faire du FHIR complet dès le départ ;
- gérer hospitalisation complète immédiatement ;
- développer une IA médicale.

---

## 16. Conclusion MVP

Le MVP de Joprelys Connect doit prouver une chose simple :

> Une clinique peut créer un dossier patient, garder l’historique de ses visites, générer un document médical professionnel et permettre sa vérification en ligne sans exposer les données sensibles.

C’est cette preuve qui permettra ensuite de construire les modules avancés : consentement patient, accès externe, laboratoire, pharmacie, AllôPharma et interopérabilité FHIR.

---
