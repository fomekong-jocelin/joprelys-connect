# TICKET-0702 — Écran de visualisation et filtrage des logs d'audit (frontend)

> Fichier obligatoire pour chaque ticket ou intervention IA.

## 1. Objectif

Implémenter l'intégration de la visualisation des logs d'audit côté frontend Angular dans le dossier patient unique. Cela permet aux professionnels de santé et administrateurs habilités de suivre la traçabilité des accès aux données de santé.

## 2. Critères d'acceptation

- [x] Création du service d'API Angular `AuditApiService` pour consommer les routes backend `/api/audit/patients/{id}`.
- [x] Ajout de la section collapsible "Journal d'Audit & Sécurité" (Section 5) au bas de `PatientDetailComponent`.
- [x] Affichage sous forme de timeline moderne et soignée des événements d'audit.
- [x] Signalétique visuelle de statut (vert pour SUCCESS, rouge pour DENIED) et affichage de la date formatée, de l'IP et de la raison.
- [x] Contrôle d'autorisation côté client : affichage conditionnel basé sur `canViewAudit()` (MEDECIN, ADMIN_CLINIQUE, AUDITEUR).
- [x] Ajout et traduction des 5 clés de dictionnaire FR/EN nécessaires dans `I18nService`.
- [x] Écriture de 2 tests unitaires frontend Vitest validant le comportement du composant.

## 3. Pilotage projet

| Champ | Valeur |
|---|---|
| Epic parent | EPIC-0007 — Traçabilité & Audit Logs |
| User story parent | STORY-0702 |
| Sprint cible | SPRINT-0003 |
| Priorité business | P2 |
| Complexité | S |
| Story points | 2 |
| Profil recommandé | Intermédiaire |
| Effort estimé senior | 0.5j |
| Effort estimé intermédiaire | 0.7j |
| Effort estimé junior | 1.1j |
| Responsable | Antigravity |
| Reviewer obligatoire | Lead Developer |
| Risque fonctionnel | Faible |
| Risque technique | Faible |
| Dépendances | STORY-0701 (Backend) |
| Bloquants connus | Aucun |

## 4. Contexte analysé

- [x] `AGENTS.md` lu
- [x] `SKILL.md` lu
- [x] `PROJECT-MANAGER-SKILL.md` lu si nécessaire
- [x] `README-IA.md` lu
- [x] `WORKFLOW-IA.md` lu
- [x] `PROJECT-TRACKING.md` lu
- [x] `CHANGELOG.md` lu
- [x] `review-checklist.md` lu
- [x] Code existant analysé
- [x] Tests existants analysés
- [x] Frontend Tailwind CSS v4 vérifié (sobriété des arrondis et respect du design system)
- [x] Absence Angular Material vérifiée
- [x] Aucun appel API Angular avec URL backend hardcodée (appels relatifs `/api/audit/...`)

## 5. Hypothèses

- L'affichage se fait sous forme de liste d'événements ordonnée de manière chronologique inversée (logs les plus récents en premier), chargée de façon paresseuse (lazy-loading) à l'ouverture de la section.

## 6. Risques et impacts

| Risque | Impact | Mitigation |
|---|---|---|
| Fuite de données d'audit sur l'interface | Moyen | Contrôle strict basé sur le rôle de la session de l'utilisateur actif. |

## 7. Action plan

- [x] Créer `audit.models.ts` et `audit-api.service.ts`.
- [x] Ajouter les clés de dictionnaire FR/EN dans `i18n.service.ts`.
- [x] Modifier `patient-detail.component.ts` pour injecter le service, charger les données paresseusement, et intégrer la timeline HTML/Tailwind.
- [x] Mettre à jour `patient-detail.component.spec.ts` avec les mocks et les tests unitaires.
- [x] Lancer le build Angular dev et s'assurer que tous les tests Vitest sont au vert.
- [x] Mettre à jour `CHANGELOG.md` et `PROJECT-TRACKING.md`.

## 8. Implémentation réalisée

- Interface de données `AuditLog` dans `web/src/app/audit/audit.models.ts`.
- Service HTTP Angular `AuditApiService` dans `web/src/app/audit/audit-api.service.ts`.
- Clés de dictionnaire i18n dans `I18nService` (`patients.auditLogsTitle`, `patients.auditLogsLoading`, `patients.auditLogsEmpty`, `patients.auditLogsAction`, `patients.auditLogsIp`, `patients.auditLogsUser`).
- Intégration de la timeline d'audit (Section 5) en bas de `PatientDetailComponent` avec chargement asynchrone paresseux au clic et computed properties pour la sécurité.
- Ajout de la résolution de `actorName` depuis le backend via `UserAccountRepository` et affichage en timeline côté frontend sous `Utilisateur : <nom>`.
- Ajout des tests unitaires Vitest Angular dans `patient-detail.component.spec.ts`.

## 9. Suivi d'exécution

| Date | Développeur | Temps passé | Avancement | Reste à faire | Blocage | Commentaire |
|---|---|---:|---:|---:|---|---|
| 2026-07-02 | Antigravity | 0.5j | 100% | Aucun | Aucun | Implémentation frontend, intégration i18n, design et tests unitaires terminés |
| 2026-07-02 | Antigravity | 0.1j | 100% | Aucun | Aucun | Ajout de la résolution du nom de l'acteur en backend et affichage dans la timeline |

## 10. Tests et vérifications

### Commandes exécutées ou à exécuter

```bash
# Angular Unit Tests
npx ng test --no-watch
```

### Résultats

- [x] Tests unitaires OK (27/27 tests au vert dans l'application)
- [x] Build OK (génération bundle Angular réussite)

## 11. Documentation

- [x] Documentation technique mise à jour : `docs/features/audit/TECHNICAL-DESIGN.md`
- [x] Changelog mis à jour : `docs/ai/CHANGELOG.md`
- [x] Suivi projet mis à jour : `docs/ai/PROJECT-TRACKING.md`

## 12. Reste à faire

- Aucun (la story frontend est entièrement terminée).

## 13. Statut final

Statut : DONE

## 14. Impact version / SemVer

| Champ | Valeur |
|---|---|
| Changement livrable | Oui |
| Type de bump | MINOR |
| Justification | Ajout de l'écran frontend de traçabilité des logs d'audit. |
| Breaking change | Non |
| Migration DB | Non |
| Changement API | Non |
| Impact Angular | Oui (nouveaux composants/services) |
| Impact Flutter | Non |
| Changelog requis | Oui |
| Release note requise | Non |

## 15. Impact thème / i18n / branding

- [x] Impact Angular UI analysé
- [x] Thème centralisé vérifié (light/dark adaptabilité)
- [x] Textes `fr` / `en` prévus (100% i18n)
- [x] Aucun texte ou branding hardcodé
