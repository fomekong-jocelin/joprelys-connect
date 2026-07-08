# TICKET-CDC-V2-ENRICHISSEMENT

## Informations

| Champ              | Valeur                                               |
|--------------------|------------------------------------------------------|
| ID                 | TICKET-CDC-V2-ENRICHISSEMENT                         |
| Type               | Gouvernance / Documentation                          |
| Priorité           | P0                                                   |
| Sprint             | SPRINT-0011                                          |
| Assigné            | Antigravity                                          |
| Reviewer           | Lead Developer                                       |
| Statut             | **DONE**                                             |
| Estimation         | 1.0j (Senior)                                        |
| Temps passé        | 0.8j                                                 |
| Dernière MAJ       | 2026-07-08                                           |
| Mode d'intervention| Engineering / Documentation                          |


## Contexte

L'utilisateur a fourni 4 documents réels issus du cabinet TC2CDK (Trauma Center Chirurgical de Douala – Kribi) du Dr NOUPOUE :

1. **MANUEL DE PROCEDURE TC2CDK** — Manuel complet des procédures administratives, financières, comptables et du circuit patient.
2. **RAPPORT HOSPI ASSONGMO** — Rapport d'hospitalisation pour fracture de l'humérus avec compte rendu opératoire complet.
3. **RAPPORT MAFFOCK PERITONITE** — Rapport de consultation et prise en charge chirurgicale pour péritonite appendiculaire.
4. **MOUTHE FACTURE CHIRURGIE RENE** — Facture de chirurgie structurée avec coefficients K.

L'objectif est d'enrichir le cahier des charges existant (V1) avec tous les éléments absents identifiés dans ces documents.

## Actions à réaliser

- [x] Lire et analyser les 4 documents TC2CDK
- [x] Analyser le CDC V1 existant
- [x] Identifier les lacunes
- [x] Créer ce ticket de gouvernance
- [x] Rédiger le CDC V2 enrichi → fichier `Cahier_des_charges_Joprelys_Connect_V2.md` créé
- [x] Mettre à jour PROJECT-TRACKING.md
- [x] Mettre à jour CHANGELOG.md


## Lacunes identifiées dans le CDC V1

### Domaine clinique
- Manque de structure complète du rapport d'hospitalisation (histoire de la maladie, signes fonctionnels, signes généraux, signes physiques)
- Absence du compte rendu opératoire (type d'intervention, anesthésie, équipe, procédure pas à pas, matériel spécifique)
- Absence de la conduite à tenir post-opératoire (protocoles thérapeutiques structurés)
- Absence du suivi journalier post-opératoire (J1, J2... Jn)
- Absence de la notion de réanimation initiale et de stabilisation hémodynamique
- Manque des hypothèses diagnostiques différentielles documentées
- Absence du consentement d'anesthésie et d'opération (distinct du consentement d'accès)
- Absence de la gestion des spécialités chirurgicales multiples
- Absence de la kinésithérapie dans le parcours de soins

### Domaine facturation et finances
- Absence totale d'un module facturation (frais de consultation, K chirurgien, K anesthésiste, K bloc, hébergement/nuit, médicaments)
- Absence de la tarification par coefficient K
- Absence de la gestion des créances patients
- Absence de la gestion du séjour hospitalier par journée
- Absence des modes de règlement (espèces, chèque, virement bancaire)
- Absence de facture numérotée, signée

### Domaine administratif et circuit patient
- Accueil non documenté dans le circuit numérique (fiche de consultation papier → numérique)
- Absence de la fiche de sortie contre avis médical
- Absence des certificats (médical, décès, genre de mort, permis d'inhumer)
- Absence de la gestion des décès (protocole, certificats, dossier famille)
- Absence de la réunion staff matinale et du registre infirmier de garde
- Absence du registre de garde (passation de service)
- Manque de gestion des listes de préparation (ex : liste grossesse/accouchement)
- Manque des heures de visites des patients hospitalisés

### Domaine personnel et organisation
- Absence d'un module RH (dossier personnel, contrat, congés, paie)
- Absence de la gestion des médecins vacataires
- Absence de la kinésithérapie comme spécialité du personnel

### Domaine documents PDF
- Structure du rapport d'hospitalisation non conforme aux documents réels
- Absence du compte rendu opératoire comme type de document PDF
- Absence de la fiche de décès
- Absence du billet d'entrée / billet de sortie

## Critères d'acceptation

- Le CDC V2 couvre tous les modules identifiés ci-dessus
- Chaque module nouveau contient : objectif, données, exigences fonctionnelles
- Le CDC V2 est cohérent avec la V1 (pas de régression)
- Le CDC V2 reste réaliste et pilotable

## Reste à faire

- Rédaction du CDC V2
- Mise à jour PROJECT-TRACKING.md
- Mise à jour CHANGELOG.md
