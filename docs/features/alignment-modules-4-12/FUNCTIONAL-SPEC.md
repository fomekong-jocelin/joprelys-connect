# FUNCTIONAL-SPEC — Alignement modules 4 à 12 du CDC

> Feature ID : `alignment-modules-4-12`  
> Epic : `EPIC-0014`  
> Ticket principal : `TICKET-0114`  
> Date : 2026-07-05  
> Version : 1.0

---

## 1. Objectif

Aligner le code backend/frontend de Joprelys Connect avec les exigences des modules 4 à 12 du Cahier des charges, sans compromis sur la qualité du code, la sécurité, ni le design system.

---

## 2. Périmètre

### Modules couverts

| Module | Nom | Exigences CDC principales |
|---|---|---|
| 4 | Dossier patient partagé | FR-DPU-001 à FR-DPU-005, synthèse médicale |
| 5 | Visites et consultations | FR-VISIT-001 à FR-VISIT-005, constantes vitales |
| 6 | Allergies et antécédents | FR-HIST-001 à FR-HIST-004 |
| 7 | Prescriptions et ordonnances | FR-PRESC-001 à FR-PRESC-005 |
| 8 | Examens médicaux | FR-EXAM-001 à FR-EXAM-005 |
| 9 | Résultats d’examens | FR-RESULT-001 à FR-RESULT-005 |
| 10 | Hospitalisation | FR-HOSP-001 à FR-HOSP-005 |
| 11 | Documents médicaux vérifiables | FR-DOC-001 à FR-DOC-006 |
| 12 | Consentement patient | FR-CONSENT-001 à FR-CONSENT-005 |

### Hors périmètre

- Modules 1, 2, 3 déjà conformes.
- Modules 13 à 16 traités partiellement (seuls les écarts liés au module 12 sont inclus).
- Nouvelles intégrations partenaires réelles (AllôPharma) — restent en mode simulé documenté.

---

## 3. Users & cas d’usage

| Acteur | Cas d’usage |
|---|---|
| Patient | Consulter sa synthèse, ses documents, ses ordonnances, ses résultats, gérer ses consentements, voir l’historique des accès, générer un QR code temporaire |
| Médecin | Créer une visite/consultation, prescrire, demander des examens, consulter la synthèse |
| Infirmier | Saisir les constantes vitales incluant la douleur |
| Laboratoire | Recevoir des demandes, changer le statut, saisir et valider des résultats |
| Pharmacie | Vérifier une ordonnance, délivrer partiellement/totalement |
| Établissement externe | Demander un accès temporaire au DPU |

---

## 4. Règles métier

### DPU et synthèse médicale (Module 4)

- La synthèse doit être consultable en moins de 3 secondes.
- Elle doit remonter les allergies actives, antécédents importants/en cours, traitements en cours, dernières visites, diagnostics, prescriptions et résultats critiques.
- Le patient peut consulter l’historique des accès à son dossier.

### Visites et consultations (Module 5)

- Une visite est liée à un patient, un établissement, un service, un praticien principal et un motif.
- Une visite terminée ne peut être modifiée sans trace de correction.
- Les constantes vitales doivent avoir des unités cohérentes.
- Le diagnostic actif est porté par l'unique champ `diagnosis` ; `conclusion` reste séparée. Les deux anciens niveaux diagnostiques ajoutés par V31 sont retirés par V109 conformément à ADR-0005.

### Allergies et antécédents (Module 6)

- Les allergies actives apparaissent dans la synthèse.
- Les antécédents importants sont remontés prioritairement.
- Aucune suppression physique sans traçabilité (soft-delete).
- Catégories : `MEDICAL`, `SURGICAL`, `FAMILY`, `OBSTETRICAL`, `ALLERGIC`, `SOCIAL`.

### Prescriptions (Module 7)

- Chaque ordonnance a un numéro unique, un statut de cycle de vie complet, et un PDF dédié avec QR code.
- Le médicament prescrit a : nom, dosage, forme, voie, fréquence, durée, quantité, instructions, substitution autorisée.
- L’ordonnance annulée reste visible comme annulée.

### Examens (Module 8)

- Types contrôlés : laboratoire, imagerie, cardiologie, ORL, ophtalmologie, autre.
- Statuts incluant le paiement : `REQUESTED`, `AWAITING_PAYMENT`, `PAID`, etc.
- Seul le labo cible peut modifier le statut d’une demande qui lui est destinée.

### Résultats (Module 9)

- Un résultat validé est immuable ; toute modification génère une nouvelle version.
- Le validateur est un utilisateur identifié.
- Le résultat a une conclusion et un document vérifiable.
- Export structuré et mapping FHIR disponibles.

### Hospitalisation (Module 10)

- Un séjour a un numéro, est lié à une visite, a un médecin responsable, une chambre/lit.
- Un lit occupé ne peut être attribué à un autre patient en cours.
- La fiche de sortie est un document vérifiable.

### Documents (Module 11)

- Chaque document a un numéro unique, un type, un hash, un QR code, une URL de vérification, une version.
- Une nouvelle version n’écrase pas l’ancienne.
- La vérification publique n’expose pas de données médicales sensibles.

### Consentements (Module 12)

- Types : ponctuel, temporaire (15 min, 1h, 24h, 7j), par établissement, par professionnel, limité, urgence.
- Statuts : demandé, accepté, refusé, expiré, révoqué.
- Canal de validation : OTP, app, agent habilité.
- Tout accès basé sur consentement est journalisé.

---

## 5. Interfaces utilisateur

### Portail patient

- Tableau de bord, profil, synthèse, visites, documents, ordonnances, résultats, demandes d’accès, historique, QR code temporaire, paramètres de confidentialité.

### Portail professionnel

- Recherche patient, synthèse, nouvelle visite, constantes, consultation, prescription, demande d’examen, documents générés, historique patient.

### Portail labo

- Dashboard, demandes reçues, détail demande, saisie résultat, validation résultat, envoi PDF, historique.

### Portail pharmacie

- Vérification ordonnance, détail, disponibilité médicaments, délivrance partielle/totale, historique.

### Vérification publique

- Saisie du numéro document, résultat de vérification, demande d’accès au dossier.

---

## 6. Critères d’acceptation globaux

- [ ] Chaque exigence FR-DPU, FR-VISIT, FR-HIST, FR-PRESC, FR-EXAM, FR-RESULT, FR-HOSP, FR-DOC, FR-CONSENT est implémentée ou explicitement dépriorisée avec ADR.
- [ ] Les tests backend passent (`./mvnw test`).
- [ ] Les tests frontend passent (`npm run test`, `npm run build`).
- [ ] Aucune régression sur les modules 1, 2, 3.
- [ ] La documentation technique est à jour.

---

## 7. Références

- `Cahier_des_charges_Joprelys_Connect_Complet.md`
- `docs/ai/tickets/TICKET-0114-ALIGNEMENT-MODULES-4-12-CDC.md`
- `docs/features/alignment-modules-4-12/TECHNICAL-DESIGN.md`
