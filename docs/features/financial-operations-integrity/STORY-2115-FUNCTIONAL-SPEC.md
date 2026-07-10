# Spécifications Fonctionnelles — STORY-2115 (Pilotage DAF & Exports OHADA)

## 1. Description du besoin

Le Direction Administrative et Financière (DAF) a besoin de :
1. **Superviser l'activité de caisse** : Visualiser l'historique complet de toutes les sessions de caisse ouvertes et clôturées dans la clinique.
2. **Gérer les écarts de caisse** : Permettre de consulter le motif d'écart saisi par les caissiers à la clôture et de valider le traitement de l'écart.
3. **Générer des écritures comptables conformes aux normes OHADA** : Automatiser ou exporter sous format CSV les écritures comptables imputées selon les comptes cibles pour intégration dans un outil de comptabilité générale.

## 2. Cas d'utilisation (Use Cases)

### UC-DAF-1 : Tableau de bord des sessions de caisse
- **Acteur** : DAF / Administrateur.
- **Description** : L'acteur accède à la liste de toutes les sessions de caisse.
- **Informations affichées** :
  - Identifiant unique de la session
  - Nom du caissier
  - Code et nom de la caisse
  - Date et heure d'ouverture / de clôture
  - Solde d'ouverture
  - Total des encaissements (par mode de règlement)
  - Total des dépenses espèces / dépôts banque
  - Solde théorique attendu
  - Solde physique déclaré par le caissier
  - Écart de caisse (Solde déclaré - Solde théorique)
  - Motif de l'écart (justification obligatoire saisie par le caissier)
  - Statut de traitement de l'écart (`PENDING`, `RESOLVED`)

### UC-DAF-2 : Résolution des écarts de caisse
- **Acteur** : DAF.
- **Description** : Pour toute session clôturée présentant un écart de caisse non nul, le DAF peut marquer cet écart comme résolu après investigation en y saisissant une note de résolution.

### UC-DAF-3 : Export comptable OHADA
- **Acteur** : DAF.
- **Description** : L'acteur sélectionne une plage de dates (date de début et date de fin) et clique sur "Exporter les écritures comptables".
- **Format du fichier** : Un fichier CSV contenant les colonnes suivantes :
  - `Date` (Format `YYYY-MM-DD`)
  - `Journal` (`VENTES`, `CAISSE`, `BANQUE`)
  - `Compte` (Numéro de compte OHADA)
  - `Libellé Compte`
  - `Débit` (Montant)
  - `Crédit` (Montant)
  - `Référence` (Identifiant de facture, reçu ou session)
  - `Libellé Écriture` (Description intelligible de l'écriture)

---

## 3. Schéma d'imputations comptables OHADA

Pour chaque événement financier clôturé, le système génère les écritures suivantes :

| Événement | Compte Débité | Libellé Débit | Compte Crédité | Libellé Crédit | Type de journal |
|---|---|---|---|---|---|
| **Facturation Patient** | `411100` | Client Patient | `706100` | Prestations médicales | Ventes |
| **Facturation Assurance** | `411200` | Client Assurance | `706100` | Prestations médicales | Ventes |
| **Encaissement Patient** | `571100` | Caisse Principale | `411100` | Client Patient | Caisse |
| **Versement Banque (Espèces)** | `585000` | Virements internes | `571100` | Caisse Principale | Caisse |
| **Validation Versement (Banque)** | `521100` | Banque | `585000` | Virements internes | Banque |
| **Règlement Assurance (Bordereau)** | `521100` | Banque | `411200` | Client Assurance | Banque |
| **Déficit de Caisse (Clôture)** | `656000` | Pertes sur écarts de caisse | `571100` | Caisse Principale | Caisse |
| **Excédent de Caisse (Clôture)** | `571100` | Caisse Principale | `756000` | Gains sur écarts de caisse | Caisse |

---

## 4. Règles de gestion

1. **Rôle requis** : Seuls les utilisateurs disposant du rôle `DAF` ou `ADMIN_CLINIQUE` ont accès aux pages de pilotage et aux exports. Un accès par un caissier ou secrétaire doit renvoyer une erreur 403 Forbidden.
2. **Génération à la volée** : Pour conserver une architecture simple et robuste, les écritures comptables peuvent être projetées/générées dynamiquement à la volée à partir des tables opérationnelles (`invoices`, `payments`, `cash_movements`, `cash_sessions`, `insurance_bordereaux`) lors de l'export, évitant ainsi des duplications complexes et des risques de désynchronisation.
3. **Calcul des écarts de caisse** :
   - Écart négatif (Déficit) : $\text{Declared} < \text{Expected}$. Débit du compte de charge exceptionnelle `656000` / Crédit du compte de caisse `571100`.
   - Écart positif (Excédent) : $\text{Declared} > \text{Expected}$. Débit du compte de caisse `571100` / Crédit du compte de produit exceptionnel `756000`.
