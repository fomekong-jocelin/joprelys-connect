# TICKET-CLINIC-EXTERNAL-ACCESS-REQUEST — Page clinique de demande d'accès externe au DPU

## 1. Objectif

Créer une page frontend Angular côté clinique permettant à un professionnel de santé (MEDECIN, INFIRMIER, ADMIN_CLINIQUE) d'initier une demande d'accès externe au Dossier Patient Unique (DPU) d'un patient via son numéro DPU. La page doit permettre la saisie du motif, de la durée et des scopes granulaires de données demandées, conformément au Module 13 (Demande d'accès externe) du cahier des charges.

## 2. Critères d'acceptation

- [x] La page est accessible sur la route `/clinic/access-request`.
- [x] La page est protégée par `roleGuard` pour les rôles `MEDECIN`, `INFIRMIER`, `ADMIN_CLINIQUE`.
- [x] Le formulaire permet la saisie du numéro DPU du patient (recherche préalable possible).
- [x] Le formulaire permet la saisie du motif de la demande (textarea obligatoire).
- [x] Le formulaire permet le choix de la durée d'accès (15 min, 1h, 24h, 7 jours) via select.
- [x] Le formulaire permet la sélection granulaire des scopes (checkboxes) : Dossier médical, Ordonnances, Résultats de labo, Allergies & ATCD.
- [x] Tous les textes visibles sont internationalisés en `fr` et `en` via `I18nService`.
- [x] L'UI utilise les tokens du design system (`ui-card-subtle`, `ui-input`, `ui-label`, `ui-button`, variables CSS) avec arrondis sobres (4-6px).
- [x] Les états loading, success, error et empty sont gérés.
- [x] Le bouton de soumission appelle `POST /api/external-access/requests` via un service dédié.
- [x] Un lien vers cette page est ajouté dans le dashboard clinique.
- [x] Les tests unitaires Angular passent.
- [x] Le build Angular se compile sans erreur.

## 3. Pilotage projet

| Champ | Valeur |
|---|---|
| Epic parent | EPIC-0013 (Conformité Module Patient) |
| User story parent | STORY-1301 (Enregistrement de demande d'accès externe) |
| Sprint cible | SPRINT-0009 |
| Priorité business | P1 |
| Complexité | S |
| Story points | 2 |
| Profil recommandé | Intermédiaire |
| Effort estimé senior | 0.15j |
| Effort estimé intermédiaire | 0.25j |
| Effort estimé junior | 0.4j |
| Responsable | Antigravity |
| Reviewer obligatoire | Lead Developer |
| Risque fonctionnel | Faible |
| Risque technique | Faible |
| Dépendances | STORY-1301 (backend API existant) |
| Bloquants connus | Aucun |

## 4. Contexte analysé

- [x] `AGENTS.md` lu
- [x] `SKILL.md` lu
- [x] `WORKFLOW-IA.md` lu
- [x] `PROJECT-TRACKING.md` lu
- [x] `CHANGELOG.md` lu
- [x] `DESIGN.md` lu
- [x] `DESIGN-SYSTEM-STANDARDS.md` lu
- [x] `UI-RADIUS-AND-SHADOW-STANDARDS.md` lu
- [x] Code existant analysé (patterns Angular, i18n, composants partagés)
- [x] API backend existante analysée (`POST /api/external-access/requests`)

## 5. Hypothèses

- L'API backend `POST /api/external-access/requests` est opérationnelle (STORY-1301 DONE).
- Le patient cible existe dans le système avec un numéro DPU valide.

## 6. Risques et impacts

| Risque | Impact | Mitigation |
|---|---|---|
| Régression du dashboard clinique | Faible | Ajout minimal d'un lien, pas de suppression |
| Incohérence visuelle | Faible | Réutilisation stricte des composants et tokens existants |

## 7. Action plan

- [x] Créer le ticket `TICKET-CLINIC-EXTERNAL-ACCESS-REQUEST.md`
- [x] Ajouter les clés i18n FR/EN dans `I18nService`
- [x] Créer le service `ExternalAccessApiService`
- [x] Créer le composant `ClinicAccessRequestComponent` (template inline + TS)
- [x] Ajouter la route `/clinic/access-request` dans `app.routes.ts`
- [x] Ajouter le lien dans le dashboard clinique
- [x] Mettre à jour `PROJECT-TRACKING.md`
- [x] Mettre à jour `CHANGELOG.md`
- [x] Vérifier build et tests Angular

## 8. Implémentation réalisée

- Ajout des clés de traduction `clinic.accessRequest.*` en FR et EN dans `I18nService`.
- Création du service `ExternalAccessApiService` avec méthode `createRequest()` appelant `POST /api/external-access/requests`.
- Création du composant `ClinicAccessRequestComponent` standalone avec :
  - Formulaire de saisie du DPU, motif, durée et scopes granulaires.
  - Utilisation de `ui-card-subtle`, `ui-input`, `ui-label`, `ui-button`, variables CSS du design system.
  - Arrondis sobres (4-6px) via `var(--radius-brand-sm)` et `var(--radius-brand-md)`.
  - Gestion des états loading, success, error.
  - Internationalisation complète via `I18nService`.
- Ajout de la route `/clinic/access-request` dans `app.routes.ts` avec `roleGuard`.
- Ajout du lien "Demander un accès externe" dans le dashboard clinique pour les rôles habilités.

## 9. Suivi d'exécution

| Date | Développeur | Temps passé | Avancement | Reste à faire | Blocage | Commentaire |
|---|---|---:|---:|---:|---|---|
| 2026-07-05 | Antigravity | 0.15j | 100% | Aucun | Aucun | Implémentation complète, build et tests validés |

## 10. Tests et vérifications

```bash
# Build Angular
npm run build

# Tests unitaires
npm test -- --watch=false
```

- [x] Build OK
- [x] Tests unitaires passent

## 11. Documentation

- [x] Changelog mis à jour
- [x] Suivi projet mis à jour

## 12. Reste à faire

- Aucun.

## 13. Statut final

Statut : DONE

## 14. Impact version / SemVer

| Champ | Valeur |
|---|---|
| Changement livrable | Oui |
| Type de bump | PATCH |
| Justification | Ajout d'une page frontend manquante pour une fonctionnalité backend existante, sans changement d'API. |
| Breaking change | Non |
| Migration DB | Non |
| Changement API | Non |
| Impact Angular | Oui |
| Impact Flutter | Non |
| Changelog requis | Oui |
| Release note requise | Non |

## 15. Impact thème / i18n / branding

- [x] Impact Angular UI analysé
- [x] Thème centralisé vérifié
- [x] Thèmes light/dark vérifiés
- [x] Textes `fr` / `en` prévus
- [x] Composants/widgets réutilisables prévus
- [x] Aucun texte ou branding hardcodé prévu
