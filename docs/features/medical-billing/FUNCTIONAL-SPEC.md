# FUNCTIONAL-SPEC — Facturation Médicale (Actes K & Conventions)

## 1. Résumé métier

Le module de facturation médicale a pour but d'automatiser et de sécuriser la valorisation des actes cliniques et des frais de séjour de l'établissement (Trauma Center). Il applique les tarifs définis, calcule les coefficients K chirurgicaux et d'anesthésie, intègre les nuitées d'hospitalisation, gère le Tiers Payant (Assurances) et génère des factures certifiées et signées.

## 2. Objectifs

- [x] Saisie et valorisation des actes opératoires par coefficients K (Chirurgien, Anesthésiste, Bloc).
- [x] Prise en compte automatique des tarifs d'hébergement journalier selon la catégorie de chambre.
- [x] Intégration des ordonnances et médicaments dispensés dans la facturation globale.
- [x] Application du Tiers Payant (Conventions Assurances) avec répartition Patient (Ticket Modérateur) et Assurance.
- [x] Suivi des règlements (espèces, chèque, virement) et gestion des créances.
- [x] Génération de factures PDF numérotées et vérifiables.

## 3. Utilisateurs / acteurs concernés

| Acteur | Besoin | Droits / limites |
|---|---|---|
| **Secrétaire comptable** | Enregistrer les paiements, éditer les reçus et factures, gérer les remboursements. | Écriture / Facturation |
| **Médecin chef / DAF** | Définir la grille tarifaire (valeur de la lettre clé K, prix des chambres). | Administration financière |
| **Biologiste / Pharmacien** | Transmettre les actes d'examens et dispensations pour facturation. | Écriture indirecte |
| **Patient** | Consulter et régler sa facture. | Lecture seule |

## 4. Périmètre

### Inclus

- Grille de tarification de la clinique : valeur du point K (ex: 1 K = 1 000 FCFA), prix de la nuitée (VIP vs Standard), actes de soins standard (AMI).
- Calculateur de chirurgie : `Total Acte = (K_chirurgien + K_anesthésiste + K_bloc) * Valeur_K`.
- Répartition Tiers Payant : calcul automatique de la part assurance (ex: 80%) et de la part patient (20%).
- Gestion des règlements multiples et statuts de facture (`PENDING`, `PARTIALLY_PAID`, `PAID`).
- Modèle PDF de facture normalisé selon les standards administratifs de la clinique.

### Exclus

- Comptabilisation automatique dans le grand livre général OHADA (déportée au module Comptabilité Générale).
- Gestion des salaires et honoraires des médecins (RH / Paie).

## 5. Parcours utilisateur

1. **Clôture de Dossier** : Lors de la sortie d'un patient hospitalisé ou après une consultation/chirurgie, le soignant clôture le dossier clinique.
2. **Édition de Facture** : La secrétaire comptable accède à l'écran de facturation du patient. Le système pré-remplit les lignes de frais (consultation, actes K saisis au bloc, nuitées calculées d'après les mouvements de lits, médicaments).
3. **Application Assurance** : La secrétaire sélectionne l'assurance du patient. La facture est recalculée en deux parts.
4. **Paiement & Édition** : Le patient paie sa part. La secrétaire valide le règlement (espèces/chèque) et imprime la facture signée contenant un QR code de vérification.

## 6. Règles métier

| ID | Règle | Priorité | Source |
|---|---|---|---|
| **BR-BILL-001** | La facture doit comporter une numérotation séquentielle unique et inaltérable. | P0 | Manuel Procédure |
| **BR-BILL-002** | Le calcul de la part assurance s'applique uniquement si la convention est valide à la date de facturation. | P0 | CDC Module 17-septies |
| **BR-BILL-003** | Toute modification de tarification (valeur de K, tarif chambre) ne doit pas rétroagir sur les factures déjà closes. | P0 | Intégrité financière |
| **BR-BILL-004** | Une facture ne peut être clôturée au statut `PAID` que si la somme des règlements reçus est égale au montant total dû. | P1 | Contrôle interne |

## 7. Critères d’acceptation

- [ ] L'IHM de facturation présente la décomposition claire (Actes K, Soins AMI, Séjour, Pharmacie).
- [x] L'IHM distingue l'émission d'une nouvelle facture du règlement d'une facture existante.
- [x] Une visite déjà facturée ne permet pas de relancer l'émission d'une facture.
- [x] Une facture tiers-payant affiche séparément la part patient à régler et la part assurance à recouvrer.
- [x] Le règlement assurance s'effectue depuis le parcours `Bordereaux Assurances` : brouillon, envoi, puis règlement avec référence.
- [ ] L'édition du PDF de facture calcule la répartition Tiers Payant de façon transparente pour le patient.
- [ ] Une tentative de facturer un patient assuré applique le taux de la convention sélectionnée de façon dynamique.

## 8. Historique des mises à jour

| Date | Auteur | Changement |
|---|---|---|
| 2026-07-08 | Antigravity | Création initiale |
| 2026-07-10 | Codex | Navigation des onglets rendue utilisable sur mobile par défilement horizontal |
| 2026-07-10 | Codex | Audit UX et cadrage EPIC-0020 : séparation des tâches facturation, caisse, créances et assurances |
