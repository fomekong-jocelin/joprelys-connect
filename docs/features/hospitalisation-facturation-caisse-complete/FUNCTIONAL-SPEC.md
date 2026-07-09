# FUNCTIONAL-SPEC — Hospitalisation, Facturation et Caisse complètes

## 1. Résumé métier

Le périmètre vise à transformer les modules actuels d'hospitalisation, facturation et caisse en un flux opérationnel complet pour clinique : séjour hospitalier structuré, actes et soins traçables, facturation exhaustive, encaissements sécurisés, clôture de caisse et premières imputations OHADA.

Le socle existant reste conservé. Cette spécification ajoute les capacités manquantes du CDC V2.1 et du document TC2CDK.

## 1.1 État fonctionnel audité le 2026-07-09

Le module actuellement présent ne couvre pas encore l'objectif complet.

| Domaine | État | Commentaire |
|---|---|---|
| Factures patient | Partiel exploitable | Création facture, lignes, tiers payant simple, paiements et PDF existent. |
| Devis / proformas | Partiel | Backend et composant Angular existent, mais le parcours n'est pas encore intégré dans la page principale. |
| Validation / remises / avoirs | Partiel | Backend amorcé ; tests métier et intégration UX restent à compléter. |
| Créances | Partiel | Modèle et endpoints existent ; suivi opérationnel DAF encore incomplet. |
| Caisse | Non livré | Pas de session de caisse, reçu numéroté séparé, clôture journalière, écart ni mouvements caisse-banque. |
| Comptabilité OHADA | Non livré | Aucune génération d'écritures 411/706, 571/521/411 ou 58/57/52. |
| Expérience utilisateur | Insuffisante | Le parcours principal reste centré sur l'émission de facture et le règlement direct, pas sur un vrai flux caisse/facturation. |

Décision produit : ne pas déclarer le module facturation/caisse complet tant que les critères P0 suivants ne sont pas livrés : devis intégré, facture validée immuable testée, reçu/ticket de caisse, session caisse ouverte, clôture journalière et créances consultables par la DAF.

## 2. Objectifs

- [ ] Couvrir le séjour hospitalier complet : entrée, lit, soins, avis, bloc, suivi post-opératoire, sortie.
- [ ] Générer les documents requis : billet d'entrée, fiche de soins, CRO, consentements, billet de sortie, reçu, facture, état de caisse.
- [ ] Facturer les consultations, examens, soins, séjour, bloc, kiné, médicaments, consommables et restauration.
- [ ] Gérer les devis/proforma, remises, avoirs, créances patient et tiers payant.
- [ ] Mettre en place une vraie caisse : sessions, mouvements, clôture, écarts, transferts caisse-banque.
- [ ] Générer les écritures OHADA minimales liées aux factures et encaissements.

## 3. Utilisateurs / acteurs concernés

| Acteur | Besoin | Droits / limites |
|---|---|---|
| Médecin | Prescrire admission, soins, sortie, bloc et CRO | Décision clinique, pas de clôture de caisse |
| Infirmier / Major | Exécuter soins, administrer médicaments, suivre lits et consommables | Saisie clinique, pas de validation financière |
| Chirurgien / Anesthésiste | Produire CRO, fiche anesthésie, actes K et implants | Actes opératoires |
| Secrétaire comptable | Émettre devis/factures, suivre créances, préparer bordereaux | Facturation et suivi |
| Caissier | Encaisser, éditer reçus, clôturer sa session | Caisse affectée |
| DAF | Valider remises, avoirs, clôtures, dépenses et rapports | Supervision financière |
| Médecin Chef | Valider décisions sensibles : sortie, décès, dépenses > seuil | Supervision médicale |
| Patient | Recevoir documents, factures, reçus et état de ses créances | Lecture / règlement |

## 4. Périmètre

### Inclus

- Séjour hospitalier structuré avec billet d'entrée et sortie.
- Feuille de soins journalière et administration horodatée.
- Consommation de médicaments, consommables et restauration rattachée au patient.
- Avis consultatifs et suivi post-opératoire.
- Bloc opératoire : planning, équipe, CRO, anesthésie, implants.
- Devis/proforma, facture validée, paiements, reçus, tickets.
- Créances patient et assurance.
- Caisse recettes/dépenses, ouverture et clôture journalière.
- Imputations OHADA minimales.
- Refactor UI pour rester sous les limites de taille du dépôt.

### Exclus

- Paie complète.
- Achats/fournisseurs complets.
- États financiers réglementaires complets : bilan, compte de résultat, annexe.
- Plan 2D/3D de la clinique.
- Intégration bancaire automatisée.

## 5. Parcours utilisateur

1. Le médecin décide l'hospitalisation et déclenche une ordonnance d'admission.
2. Le major affecte un lit libre, imprime le billet d'entrée et rattache les consentements nécessaires.
3. Les infirmiers saisissent les soins journaliers, administrations de médicaments, consommables et constantes.
4. Si une chirurgie est nécessaire, le bloc planifie l'intervention, documente l'équipe, l'anesthésie, les actes K, implants et CRO.
5. La secrétaire comptable produit un devis/proforma puis la facture à partir des actes source.
6. Le caissier encaisse la part patient et imprime un reçu/ticket de caisse.
7. La DAF suit les créances patient/assurance et clôture la caisse journalière.
8. À la sortie, le médecin valide le billet de sortie et les documents finaux sont remis au patient.

## 6. Règles métier

| ID | Règle | Priorité | Source |
|---|---|---|---|
| BR-HFC-001 | Une hospitalisation active doit être liée à un patient, une visite, un médecin responsable et un lit libre. | P0 | CDC V2.1 FR-SPACE-001 |
| BR-HFC-002 | Les soins journaliers et administrations médicamenteuses doivent être horodatés et attribués à un utilisateur. | P0 | TC2CDK 4.5 |
| BR-HFC-003 | Les actes opératoires doivent produire un CRO non modifiable après validation médicale. | P0 | TC2CDK 4.6 |
| BR-HFC-004 | Une facture validée ne peut plus être modifiée ; toute correction passe par avoir ou annulation contrôlée. | P0 | Contrôle interne |
| BR-HFC-005 | Un encaissement doit être rattaché à une session de caisse ouverte. | P0 | TC2CDK 4.9 |
| BR-HFC-006 | La clôture de caisse compare solde théorique, solde déclaré et écart justifié. | P0 | TC2CDK 4.9 |
| BR-HFC-007 | Les factures et encaissements génèrent des écritures OHADA minimales. | P1 | CDC FR-COMPTA-001 |
| BR-HFC-008 | Les dépenses de caisse supérieures au seuil autorisé nécessitent double visa DAF + Médecin Chef. | P1 | CDC FR-COMPTA-004 |
| BR-HFC-009 | Les bordereaux assurance mensuels listent les factures tiers payant non réglées. | P1 | CDC FR-INS-002 |

## 7. Critères d'acceptation

- [ ] L'utilisateur peut consulter un dossier de séjour hospitalier complet, non limité à des notes libres.
- [ ] Une facture peut être créée depuis les actes source et validée comme immuable.
- [ ] Un reçu de paiement est généré pour chaque encaissement.
- [ ] La caisse journalière peut être ouverte, clôturée et auditée.
- [ ] Les créances patient et assurance sont visibles séparément.
- [ ] Les écritures OHADA minimales sont générées et consultables.
- [ ] Les rôles caissier, secrétaire comptable, DAF et médecin chef sont pris en compte dans les autorisations.
- [ ] Les composants Angular impactés sont découpés et i18n FR/EN.

## 8. Cas limites / erreurs attendues

| Cas | Comportement attendu |
|---|---|
| Lit pris entre deux admissions | Erreur 409 et rechargement des lits disponibles |
| Facture déjà validée | Modification directe refusée ; proposer avoir/annulation |
| Paiement sans session ouverte | Erreur 409 avec message d'ouverture de caisse |
| Montant encaissé supérieur au reste dû | Erreur 400 |
| Clôture avec écart | Clôture possible seulement avec justification selon seuil |
| Assurance expirée | Tiers payant refusé ou recalculé en part patient |
| Dépense de caisse > 100 000 FCFA | Double visa obligatoire |

## 9. Textes / i18n

| Clé | Français | English |
|---|---|---|
| `hospitalization.stayRecord` | Dossier de séjour | Stay record |
| `hospitalization.dailyCare` | Feuille de soins journalière | Daily care sheet |
| `hospitalization.operatingReport` | Compte rendu opératoire | Operative report |
| `billing.estimate` | Devis / Proforma | Estimate / Proforma |
| `billing.creditNote` | Avoir | Credit note |
| `cashRegister.session` | Session de caisse | Cash register session |
| `cashRegister.close` | Clôturer la caisse | Close cash register |
| `accounting.entries` | Écritures comptables | Accounting entries |

## 10. Impacts UI / branding

| Point | Impact |
|---|---|
| Nom de l'app | Non |
| Logo | Non |
| Thème light/dark | Oui |
| Composants réutilisables | Oui : onglets de séjour, tableaux financiers, modales de clôture |
| Accessibilité | Oui : états financiers et alertes ne doivent pas dépendre uniquement de la couleur |

## 11. Hypothèses et questions ouvertes

- Confirmer les rôles exacts à ajouter : `CAISSIER`, `SECRETAIRE_COMPTABLE`, `DAF`, `MEDECIN_CHEF`.
- Confirmer le format final de numérotation des factures et reçus.
- Confirmer les règles de validation des remises et avoirs.
- Confirmer les comptes OHADA exacts par type de prestation.
- Confirmer si les documents signés manuscrits passent par la GED ou par un flux dédié.

## 12. Historique des mises à jour

| Date | Auteur | Changement |
|---|---|---|
| 2026-07-08 | Codex | Création du cadrage initial |
