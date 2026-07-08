# Cahier des charges complet — Joprelys Connect V2

**Projet :** Joprelys Connect  
**Version :** 2.0  
**Date :** 2026-07-08  
**Basée sur :** Version 1.0 (2026-07-01)  
**Produit :** Plateforme d'interopérabilité santé, dossier patient partagé et documents médicaux vérifiables  
**Éditeur envisagé :** Joprelys HealthTech  
**Slogan court recommandé :** Parce que chaque vie compte.  
**Slogan long recommandé :** Parce que chaque vie compte, nous protégeons l'essentiel.

---

## Historique des versions

| Version | Date | Auteur | Description |
|---|---|---|---|
| 1.0 | 2026-07-01 | Équipe Joprelys | Version initiale |
| 2.0 | 2026-07-08 | Antigravity | Enrichissement complet depuis les documents réels TC2CDK (Manuel procédures, rapports hospitalisation, facture chirurgie) et alignement rigoureux sur les spécifications logicielles cliniques (Ajout de 9 nouveaux modules et consolidation structurelle) |

---

## Sources de la V2

La version 2.0 est construite et validée à partir de documents et spécifications réels provenant du **Cabinet DR NOUPOUE — TRAUMACENTER/CLINIC** (TC2CDK) à Douala, Cameroun :

1. **Manuel de procédures administratives et comptables TC2CDK** (2016)
2. **Rapport d'hospitalisation ASSONGMO TEMGOUA Charly** (2025)
3. **Rapport de consultation et prise en charge MAFFOCK KOMGUEM NADÈGE** (2025)
4. **Facture chirurgie NDE RENE** (2023)
5. **Spécification logicielle du système de gestion intégré clinique TC2CDK** (2026)

---

## 1. Résumé exécutif

**Joprelys Connect** est une plateforme d'interopérabilité santé destinée à connecter les hôpitaux, cliniques, laboratoires, pharmacies, médecins, patients et applications santé autour d'un socle commun sécurisé.

Le produit résout un problème majeur de santé publique : le cloisonnement et la dispersion des dossiers patients.

> **Apport de la V2** : la V2 ancre Joprelys Connect dans la réalité opérationnelle et comptable des cliniques camerounaises, en intégrant les workflows cliniques, d'urgences, financiers, d'achats, de stocks, de ressources humaines et la comptabilité générale normée OHADA, en faisant de Joprelys Connect un véritable système de gestion intégré (ERP) et d'interopérabilité clinique.

---

## 2. Sources et références de cadrage

Ce cahier des charges s'appuie sur :

1. Des besoins exprimés de dossier patient complet et partagé.
2. Le document de démonstration (Fiche Patient Chantal Demo Sortie).
3. Le Plan Stratégique National de Santé Numérique 2026-2030 du MINSANTE Cameroun.
4. La loi camerounaise n°2024/017 relative à la protection des données à caractère personnel.
5. Les normes internationales d'interopérabilité (HL7 FHIR R4).
6. Le Manuel de procédures et les dossiers patients réels du Cabinet TC2CDK (Chirurgie, Anesthésie, Facturation).
7. Le système de normalisation comptable de l'Organisation pour l'Harmonisation en Afrique du Droit des Affaires (OHADA).

---

## 3. Vision produit

### 3.1 Vision générale

Joprelys Connect crée une **base médicale et de gestion vivante**, exposée à travers une API sécurisée et des interfaces adaptées. Les documents PDF avec QR code de vérification sont des sorties officielles immuables et auditables.

### 3.2 Phrase de positionnement

> Joprelys Connect connecte les acteurs de santé autour d'un dossier patient sécurisé, traçable, partageable et d'une gestion clinique intégrée.

### 3.3 Promesse produit

> Un patient ne perd plus son historique médical.  
> Un document médical peut être vérifié publiquement en ligne de manière sécurisée.  
> Un médecin accède instantanément à la synthèse clinique avec l'accord du patient.  
> La clinique pilote gère l'ensemble de ses activités cliniques, de stocks, de paie et sa facturation de façon transparente et conforme au droit OHADA.

---

## 4. Objectifs du projet

### 4.1 Objectif principal

Mettre en place une plateforme API sécurisée permettant de créer, conserver, partager et vérifier les données médicales d'un patient entre plusieurs acteurs de santé, tout en fournissant les modules d'administration clinique, financière et RH requis pour le fonctionnement quotidien d'un établissement hospitalier pilote.

### 4.2 Objectifs fonctionnels

Joprelys Connect doit permettre de :

1. Créer un dossier patient unique (DPU).
2. Gérer l'identité patient et limiter les doublons.
3. Enregistrer les visites, consultations, constantes et antécédents.
4. Gérer les diagnostics et prescriptions.
5. Gérer les hospitalisations, les mouvements de lits et les billets d'entrée/sortie.
6. Saisir et archiver les comptes rendus opératoires (CRO) et les fiches d'anesthésie.
7. Gérer le suivi post-opératoire journalier (J1 à Jn).
8. Gérer les urgences critiques et la fiche de réanimation initiale.
9. Intégrer la kinésithérapie et les prescriptions de rééducation.
10. Gérer le registre de garde infirmier et la passation de service.
11. Gérer les listes de préparation (obstétricale, césarienne).
12. Gérer la facturation médicale par coefficients K, séjour journalier et pharmacie.
13. Gérer les devis proforma, les encaissements, les créances et les clôtures de caisse.
14. Gérer la comptabilité générale OHADA (journaux, immatriculations, bilans).
15. Gérer le budget annuel, les fiches d'engagement et le contrôle de disponibilité.
16. Gérer les demandes internes d'achats, le comparatif fournisseur et les réceptions.
17. Gérer les immobilisations (codification, étiquetage, amortissements).
18. Gérer les ressources humaines (recrutement, contrats, notation, paie mensuelle).
19. Gérer la restauration (menus, préférences, commandes repas).
20. Générer des documents PDF signés et vérifiables par QR code.
21. Fournir des statistiques médico-économiques.

---

## 5. Périmètre du produit

### 5.1 Inclus dans le périmètre

- **Gestion Clinique** : Identité unique, DMP, consultations, constantes, prescriptions, hospitalisations, bloc opératoire, anesthésie, kinésithérapie, maternité/obstétrique, urgences et réanimation, registre de garde, réunions staff.
- **Gestion Financière & Comptable** : Facturation avec coefficients K, devis, créances, caisse recettes/dépenses, budget, comptabilité OHADA, rapprochement bancaire, achats et fournisseurs, immobilisations.
- **Gestion Logistique & RH** : Pharmacie clinique, gestion des stocks, ressources humaines, contrats, notation, paie, restauration.
- **Sécurité et Interopérabilité** : Audit logs, permissions RBAC granulaires, consentements (accès et opératoires), mapping et endpoints FHIR R4, documents PDF sécurisés par hash et QR code.

### 5.2 Hors périmètre initial

- Dossier médical national officiel.
- Connexion immédiate à l'ensemble du réseau hospitalier national.
- Intégration DHIS2 complète.
- Stockage d'imagerie DICOM lourde (seule la conservation des rapports textuels et des fichiers de synthèse PDF/JPEG est requise).
- Intégrations bancaires ou passerelles de paiement automatisées (les transactions sont enregistrées manuellement sur déclaration).

---

## 6. Principes fondamentaux

### 6.1 Le numéro patient ne donne jamais accès directement au dossier

### 6.2 Le patient contrôle l'accès à ses données

### 6.3 Les professionnels n'accèdent qu'aux informations nécessaires (RBAC)

### 6.4 Toute action sensible est journalisée

### 6.5 Le PDF est un document de sortie

### 6.6 Compatibilité progressive avec HL7 FHIR

### 6.7 Le circuit patient est structuré de l'accueil à la sortie

Le système suit pas à pas le circuit physique du patient :
1. **Accueil** (identité, pièce, motif).
2. **Paiement consultation** (reçu, ouverture de la fiche de consultation).
3. **Tri & Paramètres** (constantes vitales par l'infirmier).
4. **Consultation** (médecin / spécialiste).
5. **Orientation** (ambulatoire, examens paracliniques, hospitalisation, sortie contre avis).
6. **Hospitalisation** (admission, accord financier, consentement chirurgical, bloc, soins post-op, suivi journalier J1-Jn).
7. **Sortie** (décision médicale, ordonnance de sortie, billet de sortie, facture finale, remise des documents).

### 6.8 Le backend est maître des calculs financiers

Les tarifs, calculs de coefficients K, soldes de créances et calculs de paie sont traités côté serveur pour garantir l'intégrité des données et la conformité OHADA.

---

## 7. Acteurs et utilisateurs

- **Patient** : Propriétaire du dossier, consulte sa synthèse, ses ordonnances, ses factures et gère ses consentements.
- **Agent d'accueil / Secrétaire de direction** : Enregistre, vérifie les doublons probables, oriente, gère le cahier de rendez-vous, gère les audiences non médicales et les heures de visite des proches.
- **Secrétaire comptable / Caissier** : Reçoit les paiements, émet les reçus, gère la facturation patient, le cahier des créances, la comptabilité générale, les amortissements, les commandes fournisseurs et la paie.
- **Infirmier** : Prend les constantes, administre les soins prescrits, enregistre les médicaments consommés, saisit le rapport de garde.
- **Major** : Planifie les lits et hospitalisations, gère le planning des tours de garde, vérifie le stock pharmacie et valide le registre de garde et le matériel d'urgence.
- **Médecin / Spécialiste / Chirurgien** : Réalise les consultations, prescrit, pose les diagnostics, réalise les interventions chirurgicales et rédige les comptes rendus opératoires (CRO).
- **Anesthésiste** : Évalue le patient, réalise la fiche pré-anesthésique et anesthésique, administre le protocole d'anesthésie et valide le consentement.
- **Kinésithérapeute** : Réalise les séances de rééducation et suit l'évolution physique des patients.
- **Biologiste / Technicien d'imagerie** : Reçoit les demandes d'examens et saisit les résultats validés.
- **Directeur Administratif et Financier (DAF) / Médecin Chef** : Dirigent l'établissement, gèrent les ressources humaines, valident les budgets et engagements supérieurs à 30 000 FCFA, signent les états financiers.

---

## 8. Modules fonctionnels détaillés

## Module 1 — Gestion des établissements et spécialités

*(identique à V1 + ajouts)*

Un établissement (clinique pilote, trauma center) peut déclarer ses spécialités médicales :
- Chirurgie (générale, viscérale, orthopédique, traumatologique, esthétique/plastique, neurochirurgie, urologie).
- Kinésithérapie et rééducation physique.
- Gynécologie-obstétrique et maternité.
- Imagerie médicale et explorations fonctionnelles (ECG, échographie, radiographie).
- Analyses biologiques.

---

## Module 2 — Gestion des utilisateurs et rôles

*(identique à V1 + ajouts des rôles cliniques et administratifs réels)*

---

## Module 3 — Identité patient unique

*(identique à V1 + enrichissements)*

Le système gère les données d'identité complètes :
- Numéro DPU Joprelys global et numéro patient local.
- Prénom, nom, genre, date de naissance et âge calculé automatiquement.
- Nationalité, religion (optionnelle pour respect des croyances/rites en cas de décès), ethnie, situation matrimoniale.
- Coordonnées (téléphone, email, ville, quartier, adresse).
- Contact d'urgence (nom, téléphone).
- Groupe sanguin et rhésus.
- Type de pièce d'identité (CNI, Passeport, etc.) et numéro de document.

---

## Module 4 — Dossier patient partagé

*(identique à V1 + nouvelles sections d'urgences, chirurgie, kiné et facturation)*

---

## Module 4-bis — Urgences & Réanimation **(V2 — nouveau)**

### Objectif

Gérer la prise en charge rapide des patients admis en urgence critique (accidents de la route AVP, traumatismes fermés, états de choc septiques ou hémorragiques), en s'inspirant des dossiers réels de traumatologie et de péritonite du TC2CDK.

### Données — Fiche d'urgence et réanimation

| Champ | Description |
|---|---|
| emergency_id | Identifiant |
| patient_id | Patient (création simplifiée si inconscient) |
| arrival_at | Date et heure de l'arrivée |
| arrival_mode | Mode d'arrivée (ambulance, non médicalisé, sapeurs-pompiers) |
| trauma_type | Traumatisme fermé, ouvert, plaie, brûlure, abdomen aigu |
| triage_level | Niveau d'urgence (rouge, orange, vert) |
| hemodynamic_status | État hémodynamique (choc, hypotension, stable) |
| initial_bp | Tension artérielle initiale (ex : 70/40 mmHg) |
| initial_hr | Fréquence cardiaque (ex : 145 bpm, tachycardie) |
| initial_temp | Température |
| signs_clinical | Signes généraux (sueurs, pâleur, polypnée, altération de l'état général) |
| resuscitation_protocol | Protocole de réanimation initiale |
| vascular_access | Voies veineuses périphériques (nombre, calibre, site) |
| fluids_infused | Remplissage vasculaire (cristalloïdes, Ringer, NaCl, macromolécules) |
| emergency_meds | Antalgiques, antiémétiques, antibiotiques empiriques IV |
| stabilization_at | Date et heure de stabilisation des constantes |
| advisory_opinion_request | Demande d'avis consultatif de spécialiste |
| orientation_decision | Orientation (bloc opératoire direct, hospitalisation, sortie) |

### Exigences

- **FR-EMERG-001** : le système doit permettre de créer un dossier patient d'urgence minimal en un clic.
- **FR-EMERG-002** : la fiche de réanimation doit permettre de tracer par horodatage chaque bolus de remplissage et chaque administration de médicament d'urgence.
- **FR-EMERG-003** : un tableau de surveillance des constantes critiques doit être visible sur l'écran d'urgence.
- **FR-EMERG-004** : le passage de l'état d'urgence à l'état stabilisé doit être documenté avec les constantes de sortie d'urgence.

---

## Module 5 — Visites et consultations

*(identique à V1)*

---

## Module 5-bis — Circuit patient complet

*(identique à la section 6.7 avec intégration du registre d'accueil)*

### Le registre d'accueil

Chaque contact physique ou administratif à l'accueil donne lieu à une inscription :
- Visiteur de patient hospitalisé (identifié par pièce d'identité et enregistré selon les plages horaires autorisées : 06h-08h, 12h-14h, 18h-20h).
- Demande d'audience non médicale pour le médecin chef (remplissage d'un formulaire d'audience numérique, validation par le médecin chef, planification de rendez-vous ou admission).
- Patient venant pour consultation ou soins.

---

## Module 6 — Allergies et antécédents

*(identique à V1)*

---

## Module 7 — Prescriptions et ordonnances

*(identique à V1 + types V2 : Ambulatoire, d'Admission et de Sortie)*

---

## Module 8 — Examens médicaux

*(identique à V1)*

---

## Module 9 — Résultats d'examens

*(identique à V1)*

---

## Module 10 — Hospitalisation

*(identique à V1 + V2 : Billets d'entrée/sortie, statut DECEDE, responsables médicaux)*

---

## Module 10-bis — Compte rendu opératoire (CRO)

*(identique à V2 initial)*

---

## Module 10-ter — Suivi post-opératoire et soins journaliers

*(identique à V2 initial)*

---

## Module 10-quater — Consentement d'anesthésie et d'opération

*(identique à V2 initial)*

---

## Module 10-quinquies — Kinésithérapie

*(identique à V2 initial)*

---

## Module 11 — Documents médicaux vérifiables

*(identique à V1 + liste complète des 25 documents de la V2)*

---

## Module 12 — Consentement patient (d'accès)

*(identique à V1)*

---

## Module 13 — Demande d'accès externe

*(identique à V1)*

---

## Module 14 — Historique des accès et audit

*(identique à V1)*

---

## Module 15 — Notifications

*(identique à V1)*

---

## Module 16 — API Joprelys Connect

*(identique à V2 initial)*

---

## Module 17 — Facturation médicale

*(identique à V2 initial avec coefficients K)*

---

## Module 17-bis — Achats & Gestion fournisseurs **(V2 — nouveau)**

### Objectif

Gérer l'acquisition des biens, consommables médicaux et services de l'établissement hospitalier, conformément aux règles du contrôle interne et du manuel TC2CDK.

### Flux d'achat

```
Expression du besoin (Service) 
  → Demande d'achat interne pré-numérotée (2 exemplaires : DAF/Médecin chef + Souche)
  → Vérification des disponibilités en caisse par la secrétaire comptable
  → Approbation DAF / Autorisation Médecin Chef
  → Consultation d'au moins 3 fournisseurs
  → Tableau comparatif des prix, délais et modalités (Excel/Système)
  → Établissement du Bon de Commande (3 exemplaires)
  → Réception par Commission (DAF + Responsable service + Comptable)
  → Signature Bordereau de Réception + Procès-verbal de réception
  → Mise à jour des stocks / fiches d'immobilisations
```

### Données — Demande d'achat interne

- Numéro de demande (séquentiel), date, service utilisateur demandeur.
- Liste des articles, quantités demandées, signatures du responsable de service, visa DAF et signature du Médecin Chef.

### Données — Bon de commande

- Numéro (séquentiel), date, fournisseur retenu.
- Liste des articles, quantités, prix unitaires, montant total, lieu de livraison.
- Souche archivée, deux exemplaires transmis au fournisseur (dont un retourné avec la facture).

### Exigences

- **FR-PURCH-001** : toute commande d'achat doit être précédée d'une demande interne approuvée.
- **FR-PURCH-002** : la comparaison de 3 offres fournisseurs doit être documentée dans le système avant validation de l'achat.
- **FR-PURCH-003** : un workflow d'autorisation selon les montants doit être configuré (ex : Médecin Chef requis si investissement supérieur à 30 000 FCFA).
- **FR-PURCH-004** : le PV de réception doit être co-signé par les membres de la commission avant prise en charge en stock ou immobilisation.

---

## Module 17-ter — Gestion des stocks matériels & consommables **(V2 — nouveau)**

### Objectif

Gérer les entrées, sorties, inventaires et consommations de médicaments et de fournitures consommables de la clinique (manuel TC2CDK).

### Données

- **Produit** : Code, désignation, famille, unité, stock minimum, stock d'alerte, date de péremption, numéro de lot.
- **Mouvement** : Date, type (Entrée, Sortie, Ajustement), quantité, prix unitaire d'achat, valeur totale, numéro de bon de livraison / bon de sortie associé.
- **Bon de sortie** : Numéro, date, service bénéficiaire, articles, quantités, signatures (responsable, DAF, Médecin Chef).

### Exigences

- **FR-STOCK-001** : toute entrée en stock doit être adossée à une facture fournisseur ou un bon de livraison réceptionné.
- **FR-STOCK-002** : toute sortie de stock doit être justifiée par un bon de sortie approuvé.
- **FR-STOCK-003** : des alertes automatiques doivent signaler les produits approchant de leur stock d'alerte ou de leur date de péremption (ex : alerte à 15 jours).
- **FR-STOCK-004** : le système doit éditer une fiche d'inventaire physique mensuelle pour rapprochement théorique/réel.
- **FR-STOCK-005** : toute différence d'inventaire doit donner lieu à une note d'ajustement explicative.

---

## Module 17-quater — Gestion des immobilisations **(V2 — nouveau)**

### Objectif

Assurer l'enregistrement, la codification, l'amortissement et le suivi de maintenance du matériel de la clinique (matériel médical, mobilier, matériel informatique, transport).

### Données — Fiche d'immobilisation

- Numéro d'immatriculation unique (Codification : Code Rubrique / Code Nature / N° d'ordre / Année d'acquisition) et label d'identification.
- Désignation, famille, affectation physique.
- Date d'entrée, valeur d'acquisition historique, source de financement.
- Tableau d'amortissement (durée, dotations annuelles, valeur nette comptable).
- Fiche historique de maintenance : date, objet de la réparation, référence de facture, coût, technicien (interne / sous-traitant partenaire), visa de certification des travaux par le Médecin Chef.

### Exigences

- **FR-ASSET-001** : chaque immobilisation acquise doit générer une fiche d'immobilisation et un numéro d'immatriculation unique.
- **FR-ASSET-002** : le plan d'amortissement linéaire ou dégressif doit être calculé automatiquement par le système selon la catégorie du bien.
- **FR-ASSET-003** : tout signalement de panne, accident d'utilisation ou réparation doit être consigné dans la fiche historique du matériel.
- **FR-ASSET-004** : un inventaire annuel des immobilisations doit être produit au 31 décembre.

---

## Module 17-quinquies — Comptabilité générale OHADA **(V2 — nouveau)**

### Objectif

Traduire les actes médicaux, d'hospitalisation, d'achats, de paie et de trésorerie en écritures comptables conformes au plan comptable normalisé OHADA.

### Journaux comptables requis

- **Journal des Recettes** (facturation patient, encaissements).
- **Journal des Dépenses** (achats, factures fournisseurs).
- **Journal de Banque** (mouvements du compte bancaire).
- **Journal de Caisse** (mouvements de la caisse recettes et de la caisse dépenses).
- **Journal des Opérations Diverses (OD)** (paie, amortissements).

### Écritures types à implémenter

1. **Facturation Patient** :
   - Débit `411` (Clients) / Crédit `706` (Prestations de services).
2. **Encaissement Patient** :
   - Débit `571` (Caisse) ou `521` (Banque) / Crédit `411` (Clients).
3. **Acquisition d'immobilisation** :
   - Débit `21/22/23/24` (Immobilisations) / Crédit `481` (Fournisseurs d'investissements).
4. **Charges de personnel (Paie)** :
   - Débit `66` (Charges de personnel) / Crédit `421` (Avances et acomptes), `422` (Rémunérations dues), `43` (Organismes sociaux - CNPS), `44` (État - impôts sur salaire).
5. **Virement de fonds caisse-banque** :
   - Débit `58` (Virement de fonds) / Crédit `57` (Caisse).
   - Débit `52` (Banque) / Crédit `58` (Virement de fonds).

### Exigences

- **FR-COMPTA-001** : le système doit générer les imputations comptables automatiquement à la validation de chaque facture ou écriture de paie.
- **FR-COMPTA-002** : les états financiers réglementaires (Bilan, Compte de résultat, Annexe) doivent être éditables au plus tard 4 mois après la clôture de l'exercice.
- **FR-COMPTA-003** : un état de rapprochement bancaire mensuel doit être produit par le système.
- **FR-COMPTA-004** : le montant maximum de la caisse dépenses est fixé à 100 000 FCFA. Tout retrait nécessite le double visa de la DAF et du Médecin Chef.

---

## Module 17-sexies — Comptabilité analytique & Budget

### Objectif

Élaborer le budget annuel par lignes budgétaires et effectuer le contrôle de disponibilité avant tout engagement de dépense.

### Fiche d'engagement des dépenses par ligne budgétaire

Le système compare à chaque saisie de demande d'achat :
- Le crédit initial alloué à la ligne budgétaire.
- Les dépenses cumulées à ce jour.
- Le crédit encore disponible.
- L'engagement proposé.
Si le crédit est insuffisant, l'engagement est bloqué et nécessite une dérogation.

---

## Module 18 — Gestion des décès

*(identique à V2 initial)*

---

## Module 19 — Registre de garde et passation de service

*(identique à V2 initial)*

---

## Module 19-bis — Ressources Humaines & Paie **(V2 — nouveau)**

### Objectif

Gérer les dossiers des employés, le recrutement, le planning des roulements de garde et le calcul mensuel de la paie du personnel hospitalier (médecins, vacataires, infirmiers, administratifs, cuisinier, gardien).

### Dossier du personnel

- Pièces d'identité, curriculum vitae, diplômes validés, certificats d'aptitude médicale.
- Contrat de travail (CDD/CDI) ou lettre d'engagement signée.
- Historique disciplinaire (notes de service, sanctions, conseil de discipline).
- Fiche de notation annuelle.
- Historique des congés, absences et permissions exceptionnelles.

### Calcul de la paie mensuelle

Le système calcule le net à payer pour chaque bulletin de paie :
- **Éléments de gains** : Salaire de base, indemnités de logement, primes d'ancienneté, heures supplémentaires, indemnités de garde.
- **Éléments de retenues** : Retenues CNPS (cotisations sociales), impôts sur salaire (IRPP, CAC), avances et acomptes perçus.

### Exigences

- **FR-HR-001** : chaque employé doit avoir un dossier individuel numérique unique.
- **FR-HR-002** : le calcul de la paie doit appliquer les formules légales de fiscalité sur salaire en vigueur au Cameroun.
- **FR-HR-003** : le bulletin de paie détaillé doit être éditable en PDF et archivé dans le dossier de l'employé.
- **FR-HR-004** : les ordres de paiement des salaires par virement bancaire ou chèque doivent être générés en fin de mois.

---

## Module 20 — Liste de préparation obstétricale

*(identique à V2 initial)*

---

## Module 20-bis — Restauration **(V2 — nouveau)**

### Objectif

Gérer la préparation et la distribution des repas pour les patients hospitalisés en fonction des prescriptions médicales.

### Données

- **Menu** : Désignation, repas (petit-déjeuner, déjeuner, dîner), coût standard.
- **Commande repas** : Patient hospitalisé, date, repas, régime alimentaire prescrit par le médecin (sans sel, diabétique, liquide, normal), visa de l'infirmier major.

### Exigences

- **FR-REST-001** : le cuisinier doit pouvoir consulter l'état des repas commandés pour la journée.
- **FR-REST-002** : le coût de la restauration doit être affecté à la facture du patient ou pris en charge selon sa convention.

---

## Module 21 — Statistiques & Tableaux de bord médico-économiques **(V2 — nouveau)**

### Objectif

Fournir des rapports et graphiques d'activité pour le pilotage stratégique de l'établissement par le Médecin Chef et la DAF.

### Indicateurs clés

- **Indicateurs cliniques** : Nombre de consultations par médecin/spécialité, taux d'occupation des lits d'hospitalisation, durée moyenne de séjour (DMS), taux de succès opératoires, nombre de naissances / décès.
- **Indicateurs financiers** : Chiffre d'affaires mensuel facturé, encaissements réels par mode de paiement, balance des créances patients non soldées, état d'exécution du budget (prévisions vs réalisations), coût des stocks et consommables par service.

---

## 9. Modèle de données recommandé (enrichi)

Le modèle de données s'organise autour des 32 tables de la base de données :
1. **Établissements & Sécurité** : `organizations`, `organization_specialties`, `users`, `roles`, `permissions`, `audit_logs`, `api_clients`, `api_keys`.
2. **Dossier Clinique** : `patients`, `patient_identifiers`, `patient_duplicate_candidates`, `visits`, `consultations`, `vital_signs`, `allergies`, `medical_histories`, `diagnoses`, `prescriptions`, `prescription_items`, `exam_requests`, `exam_results`, `hospitalizations`, `or_reports`, `daily_followups`, `surgical_consents`, `physio_prescriptions`, `physio_sessions`, `obstetric_prep_lists`, `reception_logs`, `discharge_against_advice`.
3. **Urgences & Réanimation** : `emergencies`, `resuscitation_protocols`.
4. **Facturation & Finance** : `invoices`, `invoice_lines`, `k_tariffs`, `estimates`, `receivables`, `payment_records`.
5. **Achats & Stocks** : `purchase_requests`, `purchase_orders`, `suppliers`, `inventory_items`, `inventory_transactions`.
6. **Immobilisations** : `assets`, `asset_maintenance_records`.
7. **RH & Paie** : `employees`, `employee_contracts`, `payrolls`, `attendance_logs`, `leave_requests`.
8. **Restauration** : `meals`, `meal_orders`.
9. **Garde & Staff** : `shift_reports`, `staff_meetings`.

---

## 10. Impressions et éditions obligatoires

Le système doit permettre l'édition PDF propre de tous les documents listés dans la spécification TC2CDK (25 types de documents cliniques, administratifs, financiers et comptables).

---

## 11. Contraintes réglementaires et de sécurité

- **Immutabilité** : Les factures validées, les écritures comptables et les journaux ne doivent pas être modifiables.
- **Protection des données de santé** : Hébergement sécurisé, chiffrement, gestion fine des accès.
- **Conformité OHADA** : Séquentialité des écritures, non-compensation, rigueur d'imputation.

---

## 12. Plan de déploiement et de pilotage

Le déploiement s'articule sur 6 phases progressives pour assurer la transition numérique de l'établissement pilote :
- **Mois 1-2** : Fondation technique, accueil, DMP.
- **Mois 2-3** : Facturation caisse, nomenclature K et comptabilité OHADA.
- **Mois 3-4** : Hospitalisation, bloc opératoire, CRO et urgences.
- **Mois 4-5** : Pharmacie, laboratoires et kinésithérapie.
- **Mois 5-6** : Achats, immobilisations, RH, paie et budget.
- **Mois 6** : Formation des équipes, double saisie et démarrage réel.

---

*Ce cahier des charges V2 complet et enrichi a été produit le 2026-07-08 par Antigravity.*
