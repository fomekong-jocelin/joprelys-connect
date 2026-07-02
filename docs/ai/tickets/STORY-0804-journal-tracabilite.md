# STORY-0804 — Journal de traçabilité des consultations du DPU

## 1. Objectif

Permettre au patient de consulter l'historique d'audit des accès à son Dossier Patient Unique (DPU) depuis son portail (onglet "Sécurité & Audit"). Le patient doit pouvoir suivre précisément qui a accédé à ses données de santé, quand, pour quel motif (notamment pour les accès d'urgence "Brise-Glace"), à partir de quelle IP et avec quel statut de succès ou refus.

## 2. Critères d'acceptation

### Côté Patient (Portail Patient)
- [ ] L'onglet "Sécurité & Audit" affiche une liste chronologique (décroissante) des accès au DPU.
- [ ] Chaque ligne d'audit affiche de manière lisible :
  * L'établissement de santé à l'origine de l'accès (ex: "Clinique Pilote A").
  * L'action réalisée (ex: consultation de fiche, impression d'ordonnance, accès d'urgence).
  * La date et l'heure précises de l'événement.
  * Le statut (Succès ou Refus).
  * Le motif / justification saisi (essentiel pour les accès Brise-Glace).
  * Les informations techniques optionnelles (Adresse IP, navigateur/User-Agent).
- [ ] Une mise en page claire et aérée respectant le design system centralisé (radius sobre, ombres subtiles, thèmes clair/sombre).

### Côté Backend
- [ ] Un endpoint sécurisé `GET /api/patient/audit-logs` est exposé.
- [ ] Cet endpoint est protégé par le rôle `PATIENT` et restreint strictement aux logs d'audit du patient connecté.
- [ ] L'identifiant de l'organisation (`actorOrganizationId`) est résolu en nom clair de clinique pour l'affichage utilisateur.

## 3. Pilotage projet

| Champ | Valeur |
|---|---|
| Epic parent | EPIC-0008 (Portail Patient & Consentement) |
| User story parent | STORY-0804 |
| Sprint cible | SPRINT-0003 (en cours) |
| Priorité business | P2 |
| Complexité | M |
| Story points | 3 |
| Profil recommandé | Intermédiaire |
| Effort estimé senior | 0.5j |
| Effort estimé intermédiaire | 1j |
| Effort estimé junior | 1.5j |
| Responsable | Antigravity |
| Reviewer obligatoire | Lead Developer |
| Risque fonctionnel | Faible |
| Risque technique | Faible |
| Dépendances | STORY-0801 (Espace patient), STORY-0803 (Brise-Glace) |

## 4. Action plan

### Phase 1 : Logique Métier & API (Backend)
- [ ] Injecter `AuditService` et `OrganizationRepository` dans `PatientPortalController`.
- [ ] Déclarer le record `PatientAuditLogDto`.
- [ ] Créer l'endpoint `GET /api/patient/audit-logs` filtré sur l'ID du patient connecté.
- [ ] Rédiger les tests d'intégration backend dans `PatientPortalControllerTest`.

### Phase 2 : Interface Utilisateur (Frontend)
- [ ] Ajouter la méthode `getAuditLogs()` dans `PatientPortalService`.
- [ ] Mettre à jour l'affichage de l'onglet "Sécurité & Audit" dans `PatientDashboardComponent`.
- [ ] Présenter les logs d'audit sous forme de liste/timeline soignée et lisible.
- [ ] Écrire les tests unitaires frontend dans `patient-portal.spec.ts`.

## 5. Statut final
Statut : **TODO**
