# TICKET-UI-PHARMACY-LAB-HEADER-FIX — Alignement du header pharmacie et labo et résolution du double header

> Fichier obligatoire pour chaque ticket ou intervention IA.

## 1. Objectif

Résoudre le problème de double header dans le portail Laboratoire (logo dupliqué car la page affiche `<app-logo>` alors qu'elle est déjà enveloppée par `<app-shell>`), corriger le layout et le header de la page Pharmacie (qui n'était pas enveloppée dans `<app-shell>`, n'utilisait pas `<app-page-header>` et manquait de cohérence de navigation/rôles/traductions/session), et sécuriser la route de la pharmacie par un `roleGuard`.

## 2. Critères d'acceptation

- [ ] Envelopper `PharmacyPrescriptionVerifyPageComponent` avec `<app-shell>` dans son template.
- [ ] Remplacer les `<header>` customisés de la page Pharmacie et de la page Laboratoire par la directive standard `<app-page-header>`.
- [ ] Importer `PageHeaderComponent` et `AppShellComponent` (pour la pharmacie) dans les imports respectifs.
- [ ] Retirer l'usage direct de `<app-logo>` dans ces pages puisqu'elle est fournie de manière centralisée par la topbar de l'app-shell.
- [ ] Mettre à jour `app.routes.ts` pour sécuriser l'accès à `pharmacy/prescriptions` avec `roleGuard` réservé aux rôles `PHARMACIEN` et `ADMIN_JOPRELYS`.
- [ ] Corriger et adapter les tests unitaires pour qu'ils se compilent et passent avec succès (en particulier l'injection de `HttpClient` ou les dépendances de `AppShellComponent`).

## 3. Pilotage projet

| Champ | Valeur |
|---|---|
| Epic parent | EPIC-0009 & EPIC-0010 |
| User story parent | STORY-0904, STORY-1004, STORY-1005 |
| Sprint cible | SPRINT-0004 |
| Priorité business | P1 |
| Complexité | S |
| Story points | 2 |
| Profil recommandé | Intermédiaire |
| Effort estimé senior | 0.2j |
| Effort estimé intermédiaire | 0.3j |
| Effort estimé junior | 0.5j |
| Responsable | Antigravity |
| Reviewer obligatoire | Lead |
| Risque fonctionnel | Faible |
| Risque technique | Faible |
| Dépendances | Aucun |
| Bloquants connus | Aucun |

## 4. Contexte analysé

- [x] `AGENTS.md` lu
- [x] `SKILL.md` lu
- [x] `README-IA.md` lu
- [x] `WORKFLOW-IA.md` lu
- [x] `PROJECT-TRACKING.md` lu
- [x] `CHANGELOG.md` lu
- [x] `review-checklist.md` lu
- [x] Code existant analysé
- [x] Tests existants analysés
- [x] Contrats API analysés
- [x] Impacts sécurité analysés
- [x] Impacts données analysés
- [x] Impacts Angular analysés
- [x] Frontend Tailwind CSS v4 vérifié
- [x] Absence Angular Material vérifiée
- [x] Angular `proxy.conf.json` présent et référencé dans `angular.json`
- [x] Aucun appel API Angular avec URL backend hardcodée

## 5. Hypothèses

- L'utilisation de `<app-shell>` sur la page de vérification de prescription de la pharmacie est nécessaire pour assurer la cohérence visuelle, la traduction FR/EN, la déconnexion et l'affichage des informations utilisateur.
- La route `pharmacy/prescriptions` doit être protégée car elle contient des actions de dispensation réservées au pharmacien.

## 6. Risques et impacts

| Risque | Impact | Mitigation |
|---|---|---|
| Rupture de compilation des tests unitaires à cause de l'introduction de `AppShellComponent` dans la pharmacie | Moyen | Ajouter `provideHttpClient()` et `provideHttpClientTesting()` dans `pharmacy-portal.spec.ts` pour que l'injection transitive de `HttpClient` par `AuthApiService` fonctionne. |

## 7. Action plan

- [ ] Créer le ticket (cette étape)
- [ ] Modifier la configuration des routes dans [app.routes.ts](file:///C:/MES-APPLICATIONS/joprelys-connect/web/src/app/app.routes.ts) pour ajouter le `roleGuard` sur la route pharmacie.
- [ ] Corriger le layout et remplacer le header par `app-page-header` dans [lab-orders-page.component.ts](file:///C:/MES-APPLICATIONS/joprelys-connect/web/src/app/clinic/lab/lab-orders-page.component.ts).
- [ ] Envelopper de `app-shell` et utiliser `app-page-header` dans [pharmacy-prescription-verify-page.component.ts](file:///C:/MES-APPLICATIONS/joprelys-connect/web/src/app/pharmacy/pharmacy-prescription-verify-page.component.ts).
- [ ] Mettre à jour les tests dans [pharmacy-portal.spec.ts](file:///C:/MES-APPLICATIONS/joprelys-connect/web/src/app/pharmacy/pharmacy-portal.spec.ts).
- [ ] Lancer les tests Angular avec `npm test` pour s'assurer que tout passe.
- [ ] Mettre à jour `CHANGELOG.md` et `PROJECT-TRACKING.md`.

## 8. Implémentation réalisée

- [x] Remplacement du double header dans le portail laboratoire par `app-page-header`.
- [x] Ajout de l'enveloppe `app-shell` dans `PharmacyPrescriptionVerifyPageComponent`.
- [x] Utilisation du composant `app-page-header` dans `PharmacyPrescriptionVerifyPageComponent`.
- [x] Sécurisation de la route `pharmacy/prescriptions` avec `roleGuard` réservé aux pharmaciens et administrateurs.
- [x] Nettoyage des imports `RouterLink` inutilisés.
- [x] Ajout des dépendances `HttpClient` et `HttpClientTesting` dans les tests unitaires de la pharmacie.

## 9. Suivi d'exécution

| Date | Développeur | Temps passé | Avancement | Reste à faire | Blocage | Commentaire |
|---|---|---:|---:|---:|---|---|
| 2026-07-03 | Antigravity | 0.15j | 100% | Aucun | Aucun | Ticket résolu et validé par tests |

## 10. Tests et vérifications

### Commandes exécutées ou à exécuter

```bash
# Angular
npm test -- --watch=false
```

### Résultats

- [x] Tests unitaires OK (57/57 passés)
- [x] Build OK (compilation réussie par le compilateur Angular)

## 11. Documentation

- [x] Changelog mis à jour
- [x] Suivi projet mis à jour

## 12. Reste à faire

Aucun.

## 13. Statut final

Statut : DONE

## 14. Notes finales

Le header de la pharmacie et du laboratoire est maintenant parfaitement aligné avec la charte graphique et n'affiche plus de double logo. L'accès à la pharmacie est désormais protégé par rôle.

## 15. Impact version / SemVer

| Champ | Valeur |
|---|---|
| Changement livrable | Oui |
| Type de bump | PATCH |
| Justification | Correction visuelle du double header et sécurisation par guard (rétrocompatible) |
| Breaking change | Non |
| Migration DB | Non |
| Changement API | Non |
| Impact Angular | Oui |
| Impact Flutter | Non |
| Changelog requis | Oui |
| Release note requise | Non |

## 15.1 Impact thème / i18n / branding

- [x] Impact Angular UI analysé
- [x] Thème centralisé vérifié
- [x] Thèmes light/dark vérifiés
- [x] Textes `fr` / `en` prévus
- [x] Composants/widgets réutilisables prévus
- [x] Configuration app/branding vérifiée
- [x] Aucun texte ou branding hardcodé prévu

## Documentation First

- [x] Documentation feature existante consultée
- [x] Documentation à jour avant passage à DONE

## Design System / UI

- [x] `DESIGN.md` lu
- [x] Tailwind CSS v4 vérifié côté Angular
- [x] Light/dark vérifiés
- [x] i18n FR/EN prévue
- [x] Contraste/focus/accessibilité vérifiés
