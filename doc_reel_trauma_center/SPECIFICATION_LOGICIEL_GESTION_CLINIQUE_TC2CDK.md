# Spécification logicielle — Système de gestion intégré clinique
## Cabinet Dr NOUPOUE — TC2CDK (Trauma Center / Centre Chirurgical de Douala-Kribi)

---

## 1. Analyse des documents fournis

### 1.1 Nature des documents

| Document | Type | Apport pour le logiciel |
|---|---|---|
| `MANUEL DE PROCEDURE TC2CDK.docx` | Manuel administratif, comptable et organisationnel | Organisation des services, circuit du patient, rôles, procédures financières, stocks, immobilisations, paie, comptabilité OHADA |
| `MOUTHE FACTURE CHIRURGIE RENE.docx` | Facture patient | Modèle de facturation, nomenclature des actes, tarifs, mode de règlement |
| `RAPPORT HOSPI ASSONGMO.docx` | Dossier d’hospitalisation + compte rendu opératoire | Contenu médical requis, suivi post-opératoire, ordonnance de sortie |
| `rapport de consultation... MAFFOCK NADEGE.docx` | Consultation + CRO + réanimation | Gestion des urgences, réanimation, équipe opératoire, protocoles médicamenteux |

### 1.2 Services identifiés

#### Services cliniques
1. **Accueil / Sécrétariat de direction**
2. **Consultations** : médecin chef, médecin général, kinésithérapeute
3. **Urgences / Réanimation** (choc, hypotension, remplissage vasculaire)
4. **Hospitalisation** (7 lits : chambres 1 et 2 lits)
5. **Bloc opératoire** (chirurgie ambulatoire et conventionnée)
6. **Anesthésie** (générale, intubation, anesthésiste)
7. **Imagerie médicale** : radiographie (os / poumon), échographie
8. **Biologie / Analyses médicales** : NFS, ionogramme, créatinine, CRP, VS, glycémie, GS/RH, etc.
9. **Kinésithérapie / Rééducation**
10. **Pharmacie / Dispensaire** (médicaments et consommables)
11. **Maternité / Césarienne / Accouchement** (fiche mère-enfant)
12. **Restauration** (menus pour patients)

#### Services administratifs et financiers
13. **Comptabilité / Facturation**
14. **Trésorerie / Caisse / Banque**
15. **Ressources humaines / Paie**
16. **Achats / Fournisseurs**
17. **Stocks et Immobilisations**
18. **Budget / Contrôle budgétaire**
19. **Statistiques médico-économiques**

### 1.3 Étapes de facturation observées

```
Accueil
  └── Estimation prix consultation
  └── Orientation vers comptable

Paiement consultation
  └── Emission reçu (souche comptable + patient)
  └── Fiche de consultation ouverte + cachet « PAYÉ »

Consultation
  └── Prise des paramètres par infirmier
  └── Consultation médecin
  └── Si examens paracliniques :
        └── Facturation examen (biologie / imagerie)
        └── Paiement
        └── Prélèvement / examen
        └── Rendez-vous de retour (48h pour résultats)

Post-consultation
  ├── Sortie simple
  ├── Refus d’hospitalisation → fiche sortie contre avis médical
  └── Hospitalisation
        └── Ordonnance médicale d’admission signée par le patient
        └── Modalités financières au comptable
        └── Si chirurgie :
              └── Fiche de soins
              └── Billet d’entrée
              └── Autorisation d’anesthésie + fiche anesthésie
              └── Consentement opération
              └── Si césarienne : fiche mère-enfant + protocole + déclaration naissance
              └── Repos médical
              └── Billet de sortie

Hospitalisation
  └── Suivi quotidien (paramètres, soins, médicaments, consommables)
  └── Facturation journalière séjour + actes + soins + médicaments
  └── Visites (y compris week-end majorées)

Sortie
  └── Clôture financière
  └── Edition facture globale
  └── Paiement (espèces / chèque / virement)
  └── Remise documents au patient (rapport médical, billet entrée, repos, examens, facture, billet sortie)
  └── Conservation dossier cabinet (photocopies)
```

---

## 2. Objectifs généraux du logiciel

- Digitaliser le **circuit du patient** de l’accueil à la sortie.
- Garantir la **traçabilité médicale, financière et comptable**.
- Produire la **facturation** et la **comptabilité OHADA** en temps réel.
- Gérer les **stocks**, les **immobilisations**, les **achats** et le **budget**.
- Gérer le **personnel**, la **paie**, les **congés** et les **plannings**.
- Fournir des **tableaux de bord** et **statistiques** au Médecin Chef et à la DAF.
- Sécuriser les accès par **rôles et permissions**.
- Permettre le fonctionnement en **mode déconnecté partiel** et **backup quotidien**.

---

## 3. Architecture technique proposée

### 3.1 Modèle général

- **Type** : Application web / desktop hybride ou application web responsive.
- **Base de données** : PostgreSQL ou MySQL (recommandé PostgreSQL pour robustesse).
- **Backend** : API REST sécurisée (Node.js/Express, Python/FastAPI, Java/Spring Boot, ou .NET).
- **Frontend** : React / Vue.js / Angular ou application desktop Electron.
- **Impression** : Intégration imprimante thermique/A4 (factures, reçus, rapports).
- **Sauvegarde** : Backup automatique quotidien local + cloud.
- **Sécurité** : HTTPS, authentification JWT, chiffrement des données sensibles, logs d’audit.

### 3.2 Environnements

- Production (clinique)
- Test / Formation
- Backup / Archive

---

## 4. Modules fonctionnels détaillés

### 4.1 Module Accueil & Rendez-vous

**Utilisateurs** : Secrétaire de direction, standardiste.

**Fonctions** :
- Enregistrement rapide du patient (nom, prénom, âge, sexe, nationalité, adresse, téléphone, personne à contacter, religion, ethnie, CNI).
- Création / mise à jour du dossier patient unique.
- Demande d’audience pour visite non médicale.
- Gestion des rendez-vous (cahier de rendez-vous digital).
- Consultation des disponibilités médecins.
- Pointage / registre d’arrivée.
- Impression fiche de consultation.

**Documents générés** :
- Fiche de consultation
- Demande d’audience
- Billet d’entrée

---

### 4.2 Module Dossier Patient / DMP local

**Utilisateurs** : Médecins, infirmiers, major, secrétaire de direction.

**Fonctions** :
- Dossier patient unique avec historique complet.
- Antécédents médicaux, chirurgicaux, allergiques, traitements en cours.
- Constantes vitales (TA, pouls, température, SpO2, poids, taille).
- Consultations (motif, histoire maladie, examen clinique, diagnostic).
- Examens paracliniques demandés et résultats intégrés.
- Hospitalisations (rapports d’hospitalisation, feuilles de soins).
- Comptes rendus opératoires.
- Ordonnances (entrée, sortie, ambulatoire).
- Consignes post-opératoires.
- Certificats médicaux, repos médical, certificats de décès.
- Fiches spécifiques : mère-enfant, déclaration naissance.
- Gestion des visites (horaires, identité visiteur).

**Documents générés** :
- Rapport de consultation
- Rapport d’hospitalisation
- Compte rendu opératoire
- Ordonnance
- Certificat médical / repos médical
- Certificat de décès / permis d’inhumer
- Fiche mère-enfant

---

### 4.3 Module Consultations

**Utilisateurs** : Médecin chef, médecin général, kinésithérapeute, infirmier.

**Fonctions** :
- File d’attente des patients par service.
- Prise des paramètres par l’infirmier avant consultation.
- Rédaction de la consultation (motif, antécédents, examen, diagnostic).
- Demande d’examens complémentaires (biologie, imagerie, ECG, etc.).
- Orientation vers hospitalisation ou kinésithérapie.
- Gestion du refus d’hospitalisation (sortie contre avis médical).

**Intégrations** :
- Module facturation (actes et examens).
- Module imagerie / laboratoire.

---

### 4.4 Module Urgences & Réanimation

**Utilisateurs** : Médecin, infirmier, anesthésiste.

**Fonctions** :
- Tri patient urgent.
- Enregistrement rapide sans dossier complet initial.
- Fiche de réanimation : constantes, remplissage vasculaire, voies veineuses, antalgiques.
- Suivi hémodynamique en temps réel.
- Alertes si constantes critiques.
- Décision thérapeutique / demande avis consultatif.

---

### 4.5 Module Hospitalisation

**Utilisateurs** : Major, infirmiers, médecins, DAF, secrétaire comptable.

**Fonctions** :
- Plan des lits (7 lits, chambres 1/2 lits).
- Admission (ordonnance d’admission, billet d’entrée, consentements).
- Feuille de soins quotidienne.
- Administration des médicaments et soins (trace horodatée).
- Consommation de consommables liée au patient.
- Demande et suivi d’avis consultatifs.
- Sortie (billet de sortie, ordonnance de sortie, documents remis).
- Gestion des décès (certificats, permis d’inhumer, clôture financière).

**Documents générés** :
- Billet d’entrée
- Fiche de soins
- Consentement anesthésie / opération
- Feuille de surveillance
- Billet de sortie
- Ordonnance de sortie

---

### 4.6 Module Bloc Opératoire

**Utilisateurs** : Chirurgien, anesthésiste, infirmiers de bloc, major, DAF.

**Fonctions** :
- Programmation des interventions (urgente / programmée).
- Fiche intervention : indication, type d’intervention, type d’anesthésie.
- Équipe opératoire (chirurgien, assistants, anesthésiste, circulants).
- Instrumentation et matériel utilisé.
- Procédure opératoire détaillée (étapes, voie d’abord, gestes).
- Bilan per-opératoire (durée, perte sanguine, transfusion, complications).
- Consentement éclairé signé.
- Suivi du matériel implantable (plaques, vis, prothèses) avec traçabilité.

**Documents générés** :
- Compte rendu opératoire
- Fiche anesthésique
- Consentement éclairé
- Bon d’utilisation de matériel implantable

---

### 4.7 Module Imagerie & Laboratoire

**Utilisateurs** : Technicien imagerie, biologiste, médecin.

**Fonctions** :
- Réception des demandes d’examens depuis la consultation.
- Enregistrement des résultats (texte + fichier PDF/image).
- Validation des résultats par le technicien / biologiste.
- Notification au médecin prescripteur.
- Historique des examens par patient.
- Tarification par type d’examen.

**Examens supportés** :
- Radiographie os / poumon
- Échographie
- ECG
- NFS, ionogramme, créatinine, CRP, VS, glycémie, GS/RH, urée/créat, SRV, TP/TCK, hémocultures
- Anatomopathologie / bactériologie

---

### 4.8 Module Pharmacie & Stocks

**Utilisateurs** : Major, infirmiers, secrétaire comptable, DAF.

**Fonctions** :
- Fiche produit (médicaments, consommables, matériel).
- Gestion des dates d’expiration avec alertes.
- Mouvements de stock (entrées sur bon de livraison / facture, sorties sur demande signée).
- Stock minimum et alertes de réapprovisionnement.
- Inventaire physique mensuel et annuel.
- Consommation par service et par patient.
- Commandes internes et bons de sortie.
- Traçabilité des lots.

**Documents générés** :
- Bon de sortie
- Fiche de stock
- État mensuel de consommation
- Fiche d’inventaire

---

### 4.9 Module Facturation

**Utilisateurs** : Secrétaire comptable, caissier, DAF, Médecin Chef.

**Fonctions** :
- Nomenclature des actes médicaux avec tarifs.
- Tarifs personnalisables par convention / patient.
- Facturation des consultations, examens, soins, séjour, bloc, kiné, visites.
- Gestion des forfaits (chirurgie, accouchement, séjour journalier).
- Devis / proforma avant hospitalisation.
- Encaissements (espèces, chèque, virement, mobile money si applicable).
- Gestion des créances et dettes.
- Avoirs, remises, arrérages.
- Numérotation automatique des factures (ex: `00101/TC/03/2023`).
- Édition facture, reçu, ticket caisse.
- Clôture de caisse journalière.

**Lignes de facturation types** (d’après facture René) :
- Consultation chirurgien
- K Chirurgien (honoraires proportionnels)
- K Anesthésiste
- K Bloc
- Soins AMI
- Médicaments et consommables
- Visite week-end
- Séjour (coût journalier)

**Documents générés** :
- Facture
- Reçu
- Ticket de caisse
- Devis
- Cahier des créances
- État de caisse

---

### 4.10 Module Comptabilité

**Utilisateurs** : Secrétaire comptable, DAF, Médecin Chef.

**Fonctions** :
- Comptabilité générale OHADA (plan comptable normalisé).
- Journaux : recettes, dépenses, banque, caisse, opérations diverses.
- Imputation automatique des factures patients et factures fournisseurs.
- Gestion caisse recettes / caisse dépenses.
- Opérations caisse-banque (comptes 57, 52, 58).
- Rapprochement bancaire mensuel.
- Gestion des fournisseurs et clients.
- Écritures d’amortissements.
- Clôture d’exercice.
- Production des états financiers : bilan, compte de résultat, annexe.
- Export vers logiciel comptable externe (Excel, SAGE, etc.).

**Écritures types** (intégrées) :
- Acquisition immobilisation : Débit 21/22/23/24 — Crédit 481
- Règlement fournisseur investissement : Débit 481 — Crédit 52
- Charges de personnel : Débit 66 — Crédit 421/422/43/44
- Caisse-banque : comptes 57 / 52 / 58

---

### 4.11 Module Comptabilité Analytique

**Utilisateurs** : DAF, Médecin Chef.

**Fonctions** :
- Centres de coûts par service / activité.
- Calcul du coût des prestations.
- Écarts prévisions / réalisations.
- Bases pour fixation des tarifs de facturation.

---

### 4.12 Module Budget

**Utilisateurs** : DAF, Médecin Chef, comptable.

**Fonctions** :
- Élaboration annuelle du budget par chapitre / article / ligne budgétaire.
- Fiches d’engagement des dépenses.
- Contrôle de disponibilité budgétaire avant engagement.
- Suivi quotidien, mensuel, trimestriel.
- Tableaux de bord budgétaires.
- Écarts et alertes de dépassement.
- Rapports d’activité mensuels.

**Documents générés** :
- Fiche d’engagement des dépenses
- État d’exécution budgétaire
- Tableau de bord budgétaire

---

### 4.13 Module Achats & Fournisseurs

**Utilisateurs** : Secrétaire comptable, DAF, Médecin Chef, responsables de service.

**Fonctions** :
- Demande interne pré-numérotée.
- Consultation de 3 fournisseurs minimum.
- Tableau comparatif des offres.
- Bon de commande en 3 exemplaires.
- Workflow de validation (secrétaire comptable → DAF → Médecin Chef).
- Réception avec commission (DAF + responsable service + comptable).
- Procès-verbal de réception.
- Fichier fournisseurs.
- Suivi des commandes en cours.

**Documents générés** :
- Demande interne
- Tableau comparatif
- Bon de commande
- Bordereau de réception
- Procès-verbal de réception

---

### 4.14 Module Immobilisations

**Utilisateurs** : DAF, comptable.

**Fonctions** :
- Fiche d’immobilisation (nature, famille, affectation, date, valeur).
- Codification automatique : rubrique / nature / n° d’ordre / année.
- Génération numéro d’immatriculation unique + label TC2CDK.
- Fiche historique (réparations, affectations).
- Plan d’amortissement.
- Inventaire physique annuel.
- Sortie / mise au rebut.

**Documents générés** :
- Fiche d’immobilisation
- Fiche d’inventaire des immobilisations
- Tableau récapitulatif des immobilisations

---

### 4.15 Module Ressources Humaines & Paie

**Utilisateurs** : DAF, secrétaire comptable, Médecin Chef.

**Fonctions** :
- Dossier individuel du personnel.
- Contrats / lettres d’engagement (CDD/CDI).
- Visites médicales d’aptitude.
- Congés, permissions, absences.
- Plannings et roulement (gardes, week-ends).
- Notation du personnel.
- Conseil de discipline.
- Calcul de la paie (salaire de base, primes, indemnités, heures supplémentaires, avances, charges patronales).
- Édition bulletins de paie.
- Paiement par chèque / virement.

**Documents générés** :
- Bulletin de paie
- Livre de paie
- Contrat de travail
- Attestation de travail
- Fiche de renseignements

---

### 4.16 Module Plannings & Garde

**Utilisateurs** : Major, DAF.

**Fonctions** :
- Planning du personnel médical et paramédical.
- Tours de garde.
- Congés et permissions.
- Alertes de sous-effectif.
- Pointage / registre du personnel.

---

### 4.17 Module Restauration (optionnel)

**Utilisateurs** : Cuisinier, major, DAF.

**Fonctions** :
- Menus par repas.
- Préférences / régimes patients.
- Commande de repas par patient hospitalisé.
- Suivi des coûts de restauration.

---

### 4.18 Module Statistiques & Tableaux de bord

**Utilisateurs** : Médecin Chef, DAF, Médecin général.

**Fonctions** :
- Nombre de consultations par spécialité / médecin / période.
- Nombre d’hospitalisations, durée moyenne de séjour.
- Taux d’occupation des lits.
- Nombre d’interventions par type.
- Recettes par activité.
- Dépenses par nature et par ligne budgétaire.
- Évolution des stocks.
- Créances clients / dettes fournisseurs.
- Rapports mensuels / annuels d’activité.

---

### 4.19 Module Administration & Sécurité

**Utilisateurs** : Administrateur système, Médecin Chef.

**Fonctions** :
- Gestion des utilisateurs et rôles.
- Permissions granulaires par module et par action.
- Logs d’audit (qui fait quoi, quand).
- Authentification forte (mot de passe + 2FA recommandé).
- Sauvegarde automatique.
- Paramétrage de la structure (nom, adresse, compte bancaire, tarifs, etc.).
- Gestion des mises à jour du manuel de procédures.

---

## 5. Rôles et permissions

| Rôle | Accès principaux |
|---|---|
| **Médecin Chef** | Tout (validation finale, budgets > 30 000 FCFA, signatures financières, rapports) |
| **DAF** | RH, paie, budget, stocks, immobilisations, achats, validation factures, tableaux de bord |
| **Médecin général** | Consultations, hospitalisations, dossiers patients, ordonnances |
| **Chirurgien / Spécialiste** | Bloc, consultations, hospitalisations |
| **Anesthésiste** | Fiches anesthésiques, bloc, réanimation |
| **Kinésithérapeute** | Consultations kiné, séances, dossiers patients |
| **Major** | Hospitalisation, soins, plannings, stocks, dossiers patients |
| **Infirmier** | Dossiers patients, soins, paramètres, prise des constantes, registre patient |
| **Secrétaire comptable** | Facturation, encaissements, comptabilité, stocks, achats, paie |
| **Secrétaire de direction** | Accueil, rendez-vous, dossiers patients, plannings |
| **Caissier** | Encaissements, clôture de caisse |
| **Technicien imagerie / Biologiste** | Saisie résultats examens |
| **Pharmacien / Magasinier** | Gestion stocks et dispensation |
| **Cuisinier** | Restauration |

---

## 6. Flux métier clés à implémenter

### 6.1 Consultation simple

```
Accueil → Paiement consultation → Consultation → Ordonnance / Sortie
                 ↓
        Examens paracliniques → Paiement examens → Résultats → Retour consultation
```

### 6.2 Hospitalisation chirurgicale

```
Urgence / Consultation → Admission → Validation financière → Programmation bloc
   ↓                         ↓
Réanimation (si choc)    Consentements + fiches pré-op
   ↓                         ↓
Stabilisation            Intervention chirurgicale
   ↓                         ↓
Suivi hospitalier        Soins / Médicaments / Consommables
   ↓                         ↓
Sortie médicale          Facturation finale → Paiement → Documents sortie
```

### 6.3 Achat fournisseur

```
Demande interne (service) → Validation DAF/Médecin Chef → Consultation 3 fournisseurs
   ↓
Bon de commande → Livraison → Réception commission → Mise à jour stock
   ↓
Facture fournisseur → Ordre de paiement → Paiement → Écriture comptable
```

### 6.4 Paie mensuelle

```
Collecte éléments variables → Calcul paie → Contrôle DAF → Signature Médecin Chef
   ↓
Préparation chèques/virements → Distribution → Comptabilisation
```

---

## 7. Données essentielles à modéliser

### Entités principales

- `Patient` : identité, contacts, assurance, personne à contacter, antécédents.
- `DossierMedical` : consultations, hospitalisations, examens, CRO, ordonnances.
- `Consultation` : date, motif, médecin, diagnostic, actes.
- `Hospitalisation` : dates entrée/sortie, lit, médecin responsable.
- `Intervention` : indication, type, équipe, durée, complications.
- `ActeMedical` : code, libellé, tarif, famille.
- `Produit` : médicament / consommable / matériel, stock, prix, dates péremption.
- `MouvementStock` : entrée / sortie, quantité, valeur, document source.
- `Facture` : numéro, patient, lignes, total, paiements, statut.
- `Paiement` : mode, montant, date, reçu.
- `Fournisseur` : identité, contacts, historique.
- `Commande` : bon de commande, lignes, statut.
- `Immobilisation` : code, nature, valeur, amortissement.
- `Employe` : identité, contrat, salaire, congés.
- `BulletinPaie` : éléments de paie, net à payer.
- `Budget` : chapitres, articles, crédits, réalisations.
- `Utilisateur` : login, rôle, permissions.

---

## 8. Impressions et éditions obligatoires

- Fiche de consultation
- Reçu de paiement
- Facture patient
- Devis / Proforma
- Billet d’entrée / Billet de sortie
- Fiche de soins
- Ordonnance
- Rapport d’hospitalisation
- Compte rendu opératoire
- Consentement éclairé
- Certificat médical / Repos médical
- Certificat de décès / Permis d’inhumer
- Bon de commande
- Bordereau de réception
- Bon de sortie stock
- Fiche d’immobilisation
- Bulletin de paie
- Ordre de paiement
- État de caisse
- Journal comptable
- Bilan / Compte de résultat
- Tableau de bord budgétaire
- Fiche d’engagement des dépenses

---

## 9. Contraintes réglementaires et comptables

- **OHADA** : plan comptable, principes comptables (séparation des exercices, continuité d’exploitation, prudence, image fidèle).
- **Droits du patient** : consentement, confidentialité, accès au dossier.
- **Sécurité des données de santé** : traçabilité, accès restreint, sauvegarde.
- **Fiscalité camerounaise** : TVA si applicable, retenues à la source, CNPS, impôts.
- **Numérotation** : factures, bons, commandes séquentiels et non modifiables.

---

## 10. Plan de déploiement suggéré

### Phase 1 — Fondation (mois 1-2)
- Paramétrage structure, utilisateurs, rôles.
- Dossier patient + accueil + rendez-vous.
- Consultations + paramètres vitaux.
- Nomenclature et tarifs.

### Phase 2 — Facturation & Trésorerie (mois 2-3)
- Facturation patient.
- Encaissements et caisse.
- Comptabilité générale OHADA.

### Phase 3 — Hospitalisation & Bloc (mois 3-4)
- Admission / sortie.
- Feuille de soins.
- Bloc opératoire + CRO.

### Phase 4 — Paraclinique & Pharmacie (mois 4-5)
- Imagerie / laboratoire.
- Pharmacie / stocks.

### Phase 5 — Administration & Pilotage (mois 5-6)
- Achats / immobilisations.
- RH / paie.
- Budget / tableaux de bord.
- Statistiques et rapports.

### Phase 6 — Formation & Support
- Formation utilisateurs par rôle.
- Documentation.
- Support et maintenance.

---

## 11. Indicateurs de succès

- 100 % des patients enregistrés dans le système dès l’accueil.
- Facturation et encaissement immédiats après chaque acte.
- Stocks mis à jour en temps réel.
- Clôture de caisse journalière en moins de 15 minutes.
- Comptabilité à jour en permanence.
- Bilan et compte de résultat disponibles sous 4 mois après clôture.
- Réduction des erreurs de facturation et des pertes de documents.

---

*Document généré à partir des documents fournis : Manuel de procédures TC2CDK, facture chirurgie René, rapport d’hospitalisation Assongmo, rapport de consultation Maffock Nadege.*
