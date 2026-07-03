# SPRINT-0006 — Allergies, Antécédents, Hospitalisations & QA

## 1. Informations générales

| Champ | Valeur |
|---|---|
| Sprint | SPRINT-0006 |
| Période | du 2026-07-06 au 2026-07-20 |
| Objectif | Implémenter les modules cliniques d'Allergies/Antécédents et de gestion des Hospitalisations, tout en renforçant les procédures d'audit qualité (TICKET-0002) |
| Responsable | Lead Developer / Scrum Master |

## 2. Capacité

| Développeur | Profil | Jours ouvrés | Absences | Réunions/support | Capacité planifiable |
|---|---|---:|---:|---:|---:|
| Lead Developer | Senior | 10 | 0 | 2.5 | 7.5j |
| Gemini (Antigravity) | Senior | 10 | 0 | 2.5 | 7.5j |

**Capacité planifiable totale : 15.0j**

## 3. Charge sélectionnée

| Ticket | Titre | Type | SP | Profil recommandé | Est. Senior | Est. Intermédiaire | Est. Junior | Assigné | Reviewer | Statut |
|---|---|---|---:|---|---:|---:|---:|---|---|---|
| [STORY-1201](../../features/story-1201/FUNCTIONAL-SPEC.md) | Module Allergies & Antécédents Médicaux | User Story | 5 | Intermédiaire | 0.8j | 1.2j | 2.0j | Gemini | Lead Developer | DONE |
| [STORY-1202](../../features/story-1202/FUNCTIONAL-SPEC.md) | Module Hospitalisations, lits et notes journalières | User Story | 8 | Senior | 1.8j | 2.5j | 4.0j | Gemini | Lead Developer | DONE |
| [TICKET-0002](../../ai/tickets/TICKET-0002-governance.md) | Application de la checklist de review aux futures PR | Task | 2 | Intermédiaire | 0.5j | 0.75j | 1j | Lead Developer | Gemini | DONE |

## 4. Synthèse capacité

| Élément | Valeur |
|---|---:|
| Capacité planifiable totale | 15.0j |
| Charge engagée | 3.1j (Est. Senior) |
| Charge terminée | 3.1j (Est. Senior) |
| Taux de livraison | 100.0 % |
| Marge restante (Marge de sécurité/Bugs) | 11.9j |
| Taux de charge | 20.7 % |

> [!NOTE]
> Le taux de charge est volontairement bas pour ce sprint d'été afin de permettre au Lead Developer de mener l'audit qualité complet du code existant, de valider la couverture de tests et d'assurer une phase de stabilisation pilote pour la clinique.

## 5. Risques sprint

| Risque | Impact | Mitigation |
|---|---|---|
| Complexité du modèle de données d'hospitalisation (lits concurrents) | Moyen | Implémenter un verrouillage optimiste JPA (@Version) sur l'affectation des lits |
| Saisie incomplète des antécédents médicaux par les praticiens | Faible | Rendre l'interface utilisateur intuitive avec des catégories pré-remplies (médical, chirurgical, familial) |

## 6. Definition of Success

- [ ] Un médecin peut ajouter une allergie (substance, réaction, gravité) ou un antécédent sur le profil DPU d'un patient.
- [ ] Les allergies actives s'affichent en évidence (badge rouge/orange) sur la synthèse clinique du patient.
- [ ] Un patient peut être admis en hospitalisation (choix du lit/chambre), faire l'objet de notes d'observations quotidiennes, puis être libéré avec une fiche de sortie PDF générée.
- [ ] La checklist de revue de code est appliquée et validée systématiquement sur les PRs.

## Impact version / SemVer

| Champ | Valeur |
|---|---|
| Impact version | MINOR |
| Justification | Ajout des modules complémentaires d'Allergies, d'Antécédents et d'Hospitalisations. |
| Breaking change | Non |
| Release cible | v0.8.0 |
