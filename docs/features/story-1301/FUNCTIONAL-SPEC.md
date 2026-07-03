# FUNCTIONAL-SPEC — Demande d'accès externe (backend) - STORY-1301

## 1. Résumé métier

Lorsqu'un patient change d'établissement de santé ou consulte un spécialiste externe au sein du réseau Joprelys Connect, ce nouveau praticien doit pouvoir accéder temporairement à son dossier médical (DPU). Pour ce faire, il doit formuler une demande d'accès numérique en saisissant le numéro DPU du patient. Cette demande spécifie le motif et la durée de consultation souhaitée. Elle reste dans l'attente de l'approbation explicite du patient.

## 2. Objectifs

- Permettre aux professionnels de santé d'un établissement tiers d'initier une demande d'accès temporaire à un DPU.
- Saisir et historiser les métadonnées de la demande (motif, durée demandée en heures).
- Garantir le respect de la confidentialité en initiant la demande à l'état "EN_ATTENTE" sans divulgation d'information médicale à ce stade.

## 3. Utilisateurs / acteurs concernés

| Acteur | Besoin | Droits / limites |
|---|---|---|
| Médecin / Infirmier externe | Demander l'accès au DPU d'un patient externe pour assurer la continuité des soins | Peut créer une demande d'accès via le numéro DPU. Ne peut pas consulter le DPU tant que la demande n'est pas approuvée. |

## 4. Périmètre

### Inclus

- Enregistrement de la demande d'accès externe avec les informations : ID du patient, ID de l'organisation requérante, ID de l'utilisateur requérant, motif (texte libre), durée demandée (en heures), statut initial (`EN_ATTENTE`).
- Validation stricte du motif (non vide, minimum 10 caractères) et de la durée (entre 1 et 168 heures).
- Traçabilité complète via l'Audit Log lors de la soumission de la demande.

### Exclus

- Approbation ou rejet de la demande (couvert par STORY-1302).
- Mécanisme de blocage et de filtrage effectif des API (couvert par STORY-1303).

## 5. Parcours utilisateur

1. Le praticien externe se connecte sur son espace Joprelys Connect.
2. Il saisit le numéro DPU d'un patient externe (qui n'appartient pas à son établissement).
3. L'application affiche un formulaire de demande d'accès externe au lieu de la fiche DPU directe.
4. Le praticien saisit le motif de la consultation (ex: "Suivi cardiologique post-hospitalisation") et choisit une durée de validité (ex: 24 heures).
5. Il clique sur "Soumettre la demande".
6. La demande est enregistrée et notifiée au patient. Le praticien voit un message confirmant la soumission et lui indiquant d'attendre la validation du patient.

## 6. Règles métier

| ID | Règle | Priorité | Source |
|---|---|---|---|
| BR-1301-01 | Le DPU du patient ciblé doit être valide et exister dans le système. | P0 | Spécification |
| BR-1301-02 | Le motif de la demande est obligatoire et doit contenir entre 10 et 500 caractères. | P0 | Spécification |
| BR-1301-03 | La durée d'accès demandée doit être un entier compris entre 1 heure et 168 heures (7 jours maximum). | P0 | Spécification |
| BR-1301-04 | Le statut initial de toute nouvelle demande d'accès est obligatoirement `EN_ATTENTE`. | P0 | Spécification |
| BR-1301-05 | L'organisation requérante doit être différente de l'organisation propriétaire du DPU (demande externe uniquement). | P1 | Spécification |

## 7. Critères d’acceptation

- [ ] L'API backend rejette toute demande dont le motif est inférieur à 10 caractères ou vide (HTTP 400 Bad Request).
- [ ] L'API backend rejette toute demande dont la durée est hors limites (HTTP 400).
- [ ] Une demande d'accès externe créée avec succès retourne le code HTTP 201 Created et le statut de la demande est `EN_ATTENTE`.
- [ ] L'action génère un log d'audit avec l'action `REQUEST_EXTERNAL_ACCESS` dans la table des audits.

## 8. Cas limites / erreurs attendues

| Cas | Comportement attendu |
|---|---|
| DPU inexistant | HTTP 404 Not Found avec message d'erreur structuré. |
| Demande d'accès pour un patient de son propre établissement | HTTP 400 Bad Request (l'accès local est déjà couvert par le RBAC standard, pas besoin de demande externe). |
| Demande déjà active existante | HTTP 409 Conflict si une demande `EN_ATTENTE` ou `APPROUVEE` (non expirée) existe déjà pour le même couple organisation requérante / patient. |

## 9. Textes / i18n

N/A pour cette story backend pure.

## 10. Impacts UI / branding

N/A pour cette story backend pure.

## 11. Hypothèses et questions ouvertes

- **Notifications** : Les notifications au patient (STORY-1501) seront déclenchées par un événement d'application (Spring ApplicationEvent) publié lors de la création d'une demande d'accès.

## 12. Historique des mises à jour

| Date | Auteur | Changement |
|---|---|---|
| 2026-07-04 | Antigravity | Création initiale de la spécification fonctionnelle |
