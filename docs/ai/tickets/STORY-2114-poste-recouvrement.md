# STORY-2114 — Refaire le poste recouvrement: balance âgée, relances et aging

> Ticket d'ingénierie complété et validé au statut DONE.

## 1. Objectif

Permettre au chargé de recouvrement de suivre les créances impayées selon leur ancienneté (balance âgée), de consigner les actions de relance et d'afficher leur historique pour optimiser le recouvrement des factures.

## 2. Critères d'acceptation

- [x] Classification automatique des créances par tranches d'ancienneté (Sain 0-30j, À Relancer 31-60j, Urgent 61-90j, Contentieux >90j).
- [x] Enregistrement d'une action de relance (Appel, E-mail, Courrier, Visite) avec son statut et des notes.
- [x] Affichage de la timeline de l'historique des relances pour chaque créance.
- [x] Exclusion automatique des créances réglées ou soldées de la liste des relances actives.

## 3. Pilotage projet

| Champ | Valeur |
|---|---|
| Epic parent | EPIC-0018 - Intégrité financière et poste facturation/caisse |
| User story parent | STORY-2114 |
| Sprint cible | SPRINT-0013 |
| Priorité business | P1 |
| Complexité | M |
| Story points | 8 |
| Profil recommandé | Senior full-stack |
| Effort estimé senior | 2.0 j |
| Effort estimé intermédiaire | 2.6 j |
| Effort estimé junior | 4.0 j |
| Responsable | À assigner |
| Reviewer obligatoire | Lead Developer + DAF |
| Risque fonctionnel | Moyen |
| Risque technique | Moyen |
| Dépendances | STORY-2112 (modèle d'état validé) |
| Bloquants connus | Aucun |

## 4. Contexte analysé

- [x] `AGENTS.md` lu
- [x] `SKILL.md` lu
- [x] `PROJECT-MANAGER-SKILL.md` lu
- [x] `README-IA.md` lu
- [x] `WORKFLOW-IA.md` lu
- [x] `PROJECT-TRACKING.md` lu
- [x] `CHANGELOG.md` lu
- [x] `review-checklist.md` lu
- [x] Code existant analysé
- [x] Tests existants analysés
- [x] Contrats API analysés
- [x] Backend Maven uniquement vérifié
- [x] Backend `application.yml` / profils YAML vérifiés
- [x] Frontend Tailwind CSS v4 vérifié
- [x] Absence Angular Material vérifiée
- [x] Angular `proxy.conf.json` présent et référencé dans `angular.json`
- [x] Aucun appel API Angular avec URL backend hardcodée

## 5. Hypothèses

- L'ancienneté d'une créance se calcule par rapport à la date de création de la créance (`createdAt`).

## 6. Risques et impacts

| Risque | Impact | Mitigation |
|---|---|---|
| Perte de données d'historique de relance | Moyen | Table `receivable_reminders` dédiée avec indexation pour des requêtes rapides. |

## 7. Action plan

- [x] Créer le script de migration Flyway `V53__create_receivable_reminders_table.sql`
- [x] Implémenter l'entité `ReceivableReminderEntity` et son repository
- [x] Créer le service et le contrôleur pour enregistrer et lister les relances
- [x] Ajouter le calcul de la tranche d'ancienneté (Aging) sur les DTO de créance
- [x] Créer la modale Angular `BillingReminderModalComponent` de saisie d'action de relance
- [x] Modifier `BillingReceivablesComponent` pour filtrer et afficher les tranches d'ancienneté
- [x] Ajouter les tests unitaires et MockMvc
- [x] Mettre à jour `CHANGELOG.md` et `PROJECT-TRACKING.md`

## 8. Implémentation réalisée

- **Base de données & Persistance** : migration Flyway `V53` créant la table `receivable_reminders` et index de clé étrangère vers `receivables`.
- **Backend Spring Boot** : entité JPA `ReceivableReminderEntity`, enums `ReceivableReminderActionType` / `ReceivableReminderStatus`, et repository `ReceivableReminderRepository`.
- **Logique & Service** : `ReceivableReminderService` assurant la sécurité RBAC (DAF, ADMIN, SECRETAIRE seuls autorisés), création et historisation, et audit logs d'action `RECORD_REMINDER`.
- **API REST** : `ReceivableReminderController` avec endpoints `POST /api/receivables/{id}/reminders` et `GET /api/receivables/{id}/reminders`. DTO `ReceivableResponse` étendu avec le champ calculé en jours `agingSlice` (`0_30`, `31_60`, `61_90`, `90_PLUS`).
- **Tests** : tests d'intégration MockMvc complets écrits dans `ReceivableReminderControllerTest.java` (règles de sécurité DAF vs Caissier, création correcte, liste et rejets 404/403).
- **Interface Angular** : création de `BillingReminderModalComponent` affichant la timeline chronologique des relances précédentes et permettant d'en consigner une nouvelle. Intégration dans `BillingReceivablesComponent` avec filtre par tranche d'ancienneté (Aging) et mise à jour dynamique des données après enregistrement d'une relance.

## 9. Statut final

Statut : **DONE**
