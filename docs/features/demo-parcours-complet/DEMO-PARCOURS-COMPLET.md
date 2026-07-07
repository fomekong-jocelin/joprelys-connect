# Guide de démo — Parcours utilisateur complet Joprelys Connect

> **Version :** 1.0.0  
> **Date :** 2026-07-07  
> **Statut :** Finalisé (SPRINT-0011)  
> **Ticket :** [TICKET-DEMO-PARCOURS-COMPLET](../../ai/tickets/TICKET-DEMO-PARCOURS-COMPLET.md)

---

## 1. Objectif de la démo

Ce document permet d’exécuter un **parcours utilisateur complet** de Joprelys Connect, de la création d’un patient jusqu’à la saisie des résultats d’examens de laboratoire, en passant par :

- l’admission / la visite ;
- la saisie des constantes vitales ;
- la consultation médicale ;
- la prescription et la finalisation de l’ordonnance ;
- la vérification et la délivrance en pharmacie ;
- la demande et la saisie des résultats d’analyses biologiques ;
- la consultation du dossier par le patient.

Chaque étape indique le **rôle connecté**, l’**écran Angular**, les **actions** et les **données de test** à utiliser.

---

## 2. Prérequis

### 2.1 Environnement

- Backend Spring Boot démarré (`./mvnw spring-boot:run` ou équivalent).
- Frontend Angular démarré (`npm start` avec `proxy.conf.json` actif).
- Base de données initialisée (migrations Flyway appliquées).
- Un compte `ADMIN_JOPRELYS` fonctionnel.

### 2.2 Comptes de démo à créer avant la démo

Se connecter en `ADMIN_JOPRELYS` et créer :

| Rôle | Email suggéré | Mot de passe | Organisation |
|---|---|---|---|
| `ADMIN_CLINIQUE` | `admin@clinique-demo.cm` | temporaire généré | Clinique Pilote Joprelys |
| `AGENT_ACCUEIL` | `accueil@clinique-demo.cm` | temporaire généré | Clinique Pilote Joprelys |
| `INFIRMIER` | `infirmier@clinique-demo.cm` | temporaire généré | Clinique Pilote Joprelys |
| `MEDECIN` | `medecin@clinique-demo.cm` | temporaire généré | Clinique Pilote Joprelys |
| `PHARMACIEN` | `pharmacien@clinique-demo.cm` | temporaire généré | Clinique Pilote Joprelys |
| `BIOLOGISTE` | `biologiste@clinique-demo.cm` | temporaire généré | Clinique Pilote Joprelys |

> **Astuce :** les rôles sensibles (`ADMIN_CLINIQUE`, `MEDECIN`, `PHARMACIEN`, `BIOLOGISTE`) nécessitent un OTP à la connexion. Prévoir l’accès à l’email ou utiliser la valeur renvoyée par le backend en environnement de démo.

### 2.3 Navigation générale

- URL de connexion staff : `http://localhost:4200/`
- URL portail patient : `http://localhost:4200/` puis bascule sur **“Espace Patient”**.
- URL de vérification publique : `http://localhost:4200/verify/{documentId}`.

---

## 3. Données de test

### 3.1 Patient de démo

| Champ | Valeur |
|---|---|
| Nom complet | **Amélie Nkomo** |
| Sexe | Féminin |
| Date de naissance | 1990-08-12 |
| Téléphone | +237 677 11 22 33 |
| Email | amelie.nkomo@demo.cm |
| Ville | Douala |
| Quartier | Akwa |
| Adresse | Rue des Palmiers, Immeuble B |
| Contact d’urgence | Paul Nkomo |
| Téléphone contact d’urgence | +237 699 44 55 66 |
| Groupe sanguin | O+ |
| Allergies | Pénicilline |
| Antécédents | Hypertension artérielle légère |

### 3.2 Parcours clinique

| Étape | Valeur |
|---|---|
| Motif de visite | Fièvre et fatigue intense depuis 48 h |
| Orientation | Médecine générale |
| Service | Médecine générale |
| Médecin responsable | Dr Médecin |

### 3.3 Constantes vitales

| Constante | Valeur |
|---|---|
| Température | 38.7 °C |
| Poids | 68 kg |
| Taille | 165 cm |
| Pouls | 92 bpm |
| Tension systolique | 132 mmHg |
| Tension diastolique | 84 mmHg |
| SpO2 | 97 % |
| Glycémie | 0.92 g/L |
| Fréquence respiratoire | 20 /min |
| Douleur | 3 /10 |

### 3.4 Consultation et prescription

| Champ | Valeur |
|---|---|
| Symptômes | Fièvre à 38.7 °C, céphalées, frissons, fatigue |
| Examen clinique | Conjonctives roses, auscultation pulmonaire normale, abdomen souple |
| Hypothèse diagnostique | Syndrome palustre |
| Diagnostic | Paludisme simple confirmé par TDR |
| Conclusion | Patient stable, traitement antipaludique prescrit |
| Conseils | Repos, hydratation abondante, revenir en urgence si vomissements |
| Suivi | Contrôle NFS à J+7 |

**Ordonnance :**

| Médicament | Dosage | Forme | Voie | Fréquence | Durée | Quantité | Substitution |
|---|---|---|---|---|---|---|---|
| Artemether-Luméfantrine | 20/120 mg | Comprimé | Oral | 2 fois/jour | 3 jours | 6 | Oui |
| Paracétamol | 500 mg | Comprimé | Oral | 3 fois/jour | 3 jours | 9 | Oui |

### 3.5 Examens biologiques

| Examen | Priorité | Motif |
|---|---|---|
| Numération formule sanguine (NFS) | NORMALE | Bilan paludisme |
| Paludisme (TDR / Goutte épaisse) | NORMALE | Confirmation diagnostic |
| Créatininémie | NORMALE | Bilan rénal avant traitement |

### 3.6 Résultats labo

| Analyse | Valeur | Unité | Référence | Interprétation |
|---|---|---|---|---|
| Hémoglobine | 11.2 | g/dL | 12-16 | LOW |
| Plasmodium falciparum | Positif | - | Négatif | HIGH |
| Créatinine | 8.5 | mg/L | 6-12 | NORMAL |

---

## 4. Scénario détaillé

### Étape 0 — Préparation de la structure et des comptes

**Rôle :** `ADMIN_JOPRELYS`

1. Se connecter à `http://localhost:4200/` avec `admin@joprelys.local`.
2. Créer l’organisation **“Clinique Pilote Joprelys”** via *Cliniques pilotes* (`/organizations`).
3. Créer l’`ADMIN_CLINIQUE` (`admin@clinique-demo.cm`).
4. Se déconnecter.

**Rôle :** `ADMIN_CLINIQUE`

5. Se connecter avec `admin@clinique-demo.cm`.
6. Aller dans *Équipe clinique* (`/clinic/staff`).
7. Inviter successivement les comptes : `AGENT_ACCUEIL`, `INFIRMIER`, `MEDECIN`, `PHARMACIEN`, `BIOLOGISTE`.
8. Noter les mots de passe temporaires affichés.
9. Se déconnecter.

---

### Étape 1 — Création du patient

**Rôle :** `AGENT_ACCUEIL`

1. Se connecter avec `accueil@clinique-demo.cm`.
2. Le tableau de bord s’affiche (`/dashboard`).
3. Cliquer sur **“Patients”** dans le menu latéral (`/patients`).
4. Cliquer sur **“Nouveau patient”**.
5. Renseigner les données de test de la patiente **Amélie Nkomo** (section 3.1).
6. Cliquer sur **“Enregistrer”**.
7. Le dossier patient s’ouvre automatiquement : `/patients/{id}/profile`.
8. **Noter** le numéro DPU affiché (ex. `DPU-JOP-20260707-000103`) et le numéro local.

> **Point de valeur :** le numéro DPU est généré automatiquement et reste l’identifiant unique du patient dans tout l’écosystème Joprelys.

---

### Étape 2 — Admission / ouverture de la visite

**Rôle :** `AGENT_ACCUEIL` (ou `INFIRMIER`, `MEDECIN`)

1. Sur la fiche patient, cliquer sur **“Ouvrir une visite”**.
2. Renseigner :
   - Motif : *Fièvre et fatigue intense depuis 48 h*
   - Orientation : *Médecine générale*
   - Service : *Médecine générale*
   - Praticien responsable : *Dr Médecin*
   - Date/heure d’arrivée : maintenant
3. Cliquer sur **“Valider l’admission”**.
4. La visite apparaît dans la file d’attente du dashboard (`/dashboard`).

---

### Étape 3 — Saisie des constantes vitales

**Rôle :** `INFIRMIER`

1. Se connecter avec `infirmier@clinique-demo.cm`.
2. Aller sur `/dashboard` : la file d’attente affiche **Amélie Nkomo**.
3. Cliquer sur la ligne de la visite, puis sur **“Saisir les constantes”**.
4. Renseigner les constantes vitales (section 3.3).
5. Observer le **IMC calculé automatiquement** (≈ 25.0).
6. Cliquer sur **“Enregistrer les constantes”**.
7. Se déconnecter.

> **Point de valeur :** les constantes sont rattachées à la visite et accessibles en lecture seule lors de la consultation.

---

### Étape 4 — Consultation médicale

**Rôle :** `MEDECIN`

1. Se connecter avec `medecin@clinique-demo.cm`.
2. Sur le dashboard, cliquer sur **“Démarrer la consultation”** pour la visite d’Amélie Nkomo.
3. L’écran `/clinic/consultation/{visitId}` s’ouvre.
4. Renseigner les champs cliniques (section 3.4).

#### 4.1 Prescription médicale

1. Dans la section **“Prescription Médicale”**, cliquer sur **“Ajouter un médicament”**.
2. Saisir les deux lignes de la prescription (section 3.4).
3. Cliquer sur **“Finaliser l’ordonnance”**.
4. Un numéro d’ordonnance et un **code PIN** sont générés. **Les noter** (ex. `ORD-20260707-000042` / `8F2A`).
5. Télécharger le **PDF de l’ordonnance** pour montrer le QR code de vérification.

#### 4.2 Demande d’examens biologiques

1. Dans la section **“Demande d’Examens Biologiques”**, cliquer sur les suggestions : **NFS**, **Paludisme**, **Créatininémie**.
2. Priorité : *NORMALE*.
3. Motif : *Bilan paludisme*.

#### 4.3 Clôture de la visite

1. Cliquer sur **“Valider et clôturer la visite”**.
2. La consultation, l’ordonnance et la demande d’examens sont enregistrées.
3. Un **document médical** est généré automatiquement.
4. Retour au dashboard.
5. Se déconnecter.

> **Point de valeur :** la clôture de visite finalise automatiquement les ordonnances encore au statut `DRAFT` et génère les documents PDF.

---

### Étape 5 — Délivrance en pharmacie

**Rôle :** `PHARMACIEN`

1. Se connecter avec `pharmacien@clinique-demo.cm`.
2. Le menu latéral propose **“Vérification ordonnances”** (`/pharmacy/prescriptions`).
3. Saisir :
   - Numéro d’ordonnance : `ORD-20260707-000042`
   - Code PIN : `8F2A`
4. Cliquer sur **“Vérifier l’ordonnance”**.
5. Le détail de l’ordonnance s’affiche : patient, médecin, médicaments, substitution autorisée.

#### 5.1 Délivrance

1. Dans le panneau **“Disponibilité et quantités servies”** :
   - Nom de la pharmacie : *Pharmacie du Centre*
   - Licence pharmacien : *PH-12345*
   - Pour chaque médicament, cocher **Disponible**, renseigner la quantité servie et éventuellement un substituant.
2. Cliquer sur **“Enregistrer la délivrance”**.
3. Le statut de l’ordonnance passe à `FULLY_DISPENSED` (ou `PARTIALLY_DISPENSED` si une ligne n’est pas disponible).
4. Se déconnecter.

> **Point de valeur :** la pharmacie n’a accès qu’aux seules données nécessaires à la délivrance. Le dossier médical complet reste protégé.

---

### Étape 6 — Saisie des résultats d’examens

**Rôle :** `BIOLOGISTE`

1. Se connecter avec `biologiste@clinique-demo.cm`.
2. Le menu latéral propose **“Demandes d’analyses”** (`/clinic/lab-orders`).
3. La demande d’Amélie Nkomo apparaît avec le statut `REQUESTED`.
4. Cliquer sur la demande pour afficher le détail.
5. Modifier le statut : `SAMPLE_COLLECTED` puis `IN_PROGRESS`.
6. Dans la section **“Saisie résultat”**, renseigner :
   - Clé API laboratoire (fournie par la clinique)
   - Validateur : *Dr Biologiste*
   - Statut : `VALIDATED`
   - Date de prélèvement et de validation
   - Conclusion : *Paludisme confirmé, fonction rénale conservée*
7. Ajouter les lignes de résultats (section 3.6).
8. Uploader éventuellement le **PDF du résultat**.
9. Cliquer sur **“Valider les résultats”**.
10. Le statut de la demande passe à `VALIDATED`.
11. Se déconnecter.

> **Point de valeur :** les résultats validés sont immédiatement consultables par le médecin et le patient dans le DPU.

---

### Étape 7 — Consultation par le patient

**Rôle :** `PATIENT`

1. Sur la page de connexion `/`, basculer en mode **“Espace Patient”**.
2. Saisir :
   - Numéro DPU : `DPU-JOP-20260707-000103`
   - Téléphone : `+237 677 11 22 33`
   - Date de naissance : `1990-08-12`
3. Cliquer sur **“Recevoir le code de sécurité”**.
4. Saisir l’OTP reçu, puis **“Se connecter”**.
5. Le portail patient s’ouvre (`/patient/dashboard`).
6. Naviguer dans :
   - **Ma Synthèse Médicale** (`/patient/summary`)
   - **Mes Ordonnances** (`/patient/prescriptions`) → télécharger le PDF
   - **Mes Analyses & Résultats** (`/patient/results`) → consulter les résultats du biologiste
   - **Mon QR Code** (`/patient/qr-code`) → montrer le QR code du DPU
   - **Sécurité & Accès** (`/patient/audit`) → montrer l’historique des accès

> **Point de valeur :** le patient garde le contrôle de son dossier et peut consulter l’historique des accès (traçabilité).

---

### Étape 8 — Vérification publique d’un document (bonus)

1. Prendre l’URL ou le QR code d’un document PDF (ordonnance ou compte-rendu).
2. Ouvrir un navigateur en navigation privée.
3. Accéder à `http://localhost:4200/verify/{documentId}`.
4. La page affiche les informations vérifiables du document : numéro, statut, émetteur, patient.

> **Point de valeur :** tout document médical généré par Joprelys est vérifiable publiquement sans révéler le dossier complet.

---

## 5. Tableau récapitulatif des rôles et actions

| # | Rôle | Écran / Route | Action principale | Données clés à noter |
|---|---|---|---|---|
| 0 | `ADMIN_JOPRELYS` | `/organizations` | Créer clinique et admin clinique | - |
| 0 | `ADMIN_CLINIQUE` | `/clinic/staff` | Inviter le staff | Mots de passe temporaires |
| 1 | `AGENT_ACCUEIL` | `/patients` | Créer le patient | Numéro DPU |
| 2 | `AGENT_ACCUEIL` | `/patients/:id` | Ouvrir la visite | Numéro de visite |
| 3 | `INFIRMIER` | `/dashboard` | Saisir les constantes vitales | IMC auto |
| 4 | `MEDECIN` | `/clinic/consultation/:visitId` | Consulter, prescrire, demander examens | N° ordonnance + PIN |
| 5 | `PHARMACIEN` | `/pharmacy/prescriptions` | Vérifier et délivrer | Statut dispensation |
| 6 | `BIOLOGISTE` | `/clinic/lab-orders` | Saisir et valider résultats | Statut `VALIDATED` |
| 7 | `PATIENT` | `/patient/*` | Consulter son DPU | Synthèse, ordonnances, résultats |

---

## 6. Flux de statuts observables

### Ordonnance

```
DRAFT ──[finaliser]──► ACTIVE ──[dispenser]──► FULLY_DISPENSED
                          │
                          └─[partiel]────────► PARTIALLY_DISPENSED
```

### Visite

```
EN_COURS ──[clôturer]──► TERMINEE
```

### Demande d’examen

```
REQUESTED ──[prélèvement]──► SAMPLE_COLLECTED ──[analyse]──► IN_PROGRESS
      │                                                       │
      │                                                       ▼
      └───────────────────────────────────────────────[validation]──► VALIDATED
```

---

## 7. Checklist avant la démo

- [ ] Backend et frontend démarrés.
- [ ] Base de données propre ou scénario de reset connu.
- [ ] Organisation et comptes de démo créés.
- [ ] OTP accessibles (boîtes mail ou logs backend).
- [ ] Navigateur en français pour la démo (ou anglais si audience internationale).
- [ ] Thème clair par défaut (toggle accessible en topbar).
- [ ] PDF viewers fonctionnels pour les téléchargements.
- [ ] Connexion internet coupée si la démo est 100 % locale (éviter les appels externes).

---

## 8. Points de vigilance et sécurité

- **Consentement DPU** : si la clinique n’a pas encore d’accès patient actif, un écran de consentement peut bloquer l’affichage du dossier. En démo, utiliser la **procédure d’urgence (break-glass)** avec justification si nécessaire.
- **Rôles sensibles** : les comptes `ADMIN_CLINIQUE`, `MEDECIN`, `PHARMACIEN`, `BIOLOGISTE` déclenchent un OTP.
- **Pharmacie** : l’API publique de pharmacie (`/api/public/pharmacy/prescriptions/*`) ne donne accès qu’aux données de délivrance, jamais au dossier complet.
- **Multi-tenant** : chaque action est rattachée à l’`organizationId` de la clinique connectée.
- **Audit** : toutes les actions sensibles sont tracées et consultables dans *Sécurité & Accès* du portail patient.

---

## 9. Annexes

### 9.1 Endpoints API clés

| Ressource | Endpoint | Méthode |
|---|---|---|
| Créer patient | `/api/patients` | POST |
| Créer visite | `/api/visits` | POST |
| Constantes vitales | `/api/visits/{id}/vitals` | POST |
| Consultation | `/api/visits/{id}/consultation` | POST |
| Prescription | `/api/consultations/{id}/prescription` | POST |
| Finaliser ordonnance | `/api/prescriptions/{id}/finalize` | POST |
| Vérifier ordonnance (public) | `/api/public/pharmacy/prescriptions/verify` | POST |
| Dispenser (public) | `/api/public/pharmacy/prescriptions/dispense` | POST |
| Demande d’examen | `/api/lab-orders` | POST |
| Upload résultats (public) | `/api/public/lab-integration/upload` | POST |

### 9.2 Fichiers sources de référence

- Routage : `web/src/app/app.routes.ts`
- Dashboard / file d’attente : `web/src/app/clinic/dashboard.component.ts`
- Patients : `web/src/app/patient/patient-list.component.ts`, `patient-detail.component.ts`
- Consultation : `web/src/app/consultation/consultation.component.ts`
- Pharmacie : `web/src/app/pharmacy/pharmacy-prescription-verify-page.component.ts`
- Laboratoire : `web/src/app/clinic/lab/lab-orders-page.component.ts`
- Portail patient : `web/src/app/patient/portal/*`

---

## 10. Reste à faire / améliorations futures

- [ ] Ajouter des captures d’écran pour chaque étape.
- [ ] Créer une vidéo de démonstration de 5 min.
- [ ] Préparer un scénario de démo en anglais.
- [ ] Automatiser le reset de l’environnement de démo via un script SQL/Seed.
