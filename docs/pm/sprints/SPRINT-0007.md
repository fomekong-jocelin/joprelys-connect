# SPRINT-0007 — Demandes d'Accès Externes & Notifications

## 1. Informations générales

| Champ | Valeur |
|---|---|
| Sprint | SPRINT-0007 |
| Période | du 2026-07-20 au 2026-08-03 |
| Objectif | Implémenter le module de demande d'accès externe temporaire (Module 13) et le socle du système de notifications (Module 15) pour les patients |
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
| [STORY-1301](../../features/story-1301/FUNCTIONAL-SPEC.md) | Enregistrement de demande d'accès externe (backend) | User Story | 5 | Intermédiaire | 0.8j | 1.2j | 2.0j | Gemini | Lead Developer | READY |
| [STORY-1302](../../features/story-1302/FUNCTIONAL-SPEC.md) | Validation de demande d'accès externe (portail patient) | User Story | 3 | Intermédiaire | 0.5j | 0.8j | 1.3j | Gemini | Lead Developer | READY |
| [STORY-1303](../../features/story-1303/FUNCTIONAL-SPEC.md) | Contrôle d'accès & Expiration des droits externes | User Story | 5 | Senior | 1.0j | 1.5j | 2.5j | Lead Developer | Gemini | READY |
| [STORY-1501](../../features/story-1501/FUNCTIONAL-SPEC.md) | Socle et service d'envoi de notifications (backend) | User Story | 3 | Intermédiaire | 0.6j | 0.9j | 1.5j | Gemini | Lead Developer | READY |
| [STORY-1502](../../features/story-1502/FUNCTIONAL-SPEC.md) | Centre de notifications sur le portail patient (IHM) | User Story | 3 | Junior/Intermédiaire | 0.4j | 0.6j | 1.0j | Lead Developer | Gemini | READY |

## 4. Synthèse capacité

| Élément | Valeur |
|---|---:|
| Capacité planifiable totale | 15.0j |
| Charge engagée | 3.3j (Est. Senior) |
| Marge restante (Marge de sécurité/Bugs) | 11.7j |
| Taux de charge | 22.0 % |

> [!NOTE]
> Le taux de charge de 22% est configuré pour sécuriser le déploiement pilote initial de la version v0.8.0 et traiter les retours utilisateurs directs de la clinique pilote tout en avançant sur des briques transverses de sécurité.

## 5. Risques sprint

| Risque | Impact | Mitigation |
|---|---|---|
| Expiration asynchrone des droits d'accès | Moyen | Mettre en place un planificateur Spring `@Scheduled` ou vérifier la validité de la date d'accès à chaque requête au niveau du filtre de sécurité. |
| Inondation de notifications (spam) | Faible | Limiter les types d'événements déclencheurs de notifications et regrouper les alertes similaires. |

## 6. Definition of Success

- [ ] Un établissement externe peut soumettre une demande d'accès temporaire de consultation d'un DPU (motif, durée en heures).
- [ ] Le patient reçoit une notification sur son portail et peut approuver/refuser la demande en un clic.
- [ ] Un médecin externe ne peut pas consulter le dossier sans consentement préalable approuvé et non expiré.
- [ ] Les accès expirés sont bloqués immédiatement.
- [ ] Le journal d'audit trace toutes les demandes d'accès externe (émises, approuvées, expirées, refusées).

## Impact version / SemVer

| Champ | Valeur |
|---|---|
| Impact version | MINOR |
| Justification | Ajout des modules de demande d'accès externe et du centre de notifications. |
| Breaking change | Non |
| Release cible | v0.9.0 |
