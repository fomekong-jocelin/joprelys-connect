# TICKET-I18N-COMPLETION — Internationalisation globale des composants Angular

## 1. Objectif

Rendre l'application entièrement bilingue (français/anglais) en extrayant toutes les chaînes textuelles encore codées en dur (hardcoded) dans le frontend Angular (fichiers templates `.html` et composants `.ts`).

## 2. Critères d'acceptation

- [ ] Aucun texte utilisateur visible en dur dans le module d'authentification (`login.component.html`, `forgot-password.component.ts`, `unauthorized.component.ts`).
- [ ] Aucun texte utilisateur visible en dur dans la liste et les détails des patients (`patient-list.component.html`, `patient-detail.component.ts`, `patient-form.component.ts`, `patient-medical-info.component.ts`).
- [ ] Les fichiers `fr.json` et `en.json` sont mis à jour avec les clés de traduction adéquates.
- [ ] L'application compile avec succès et tous les tests existants restent fonctionnels.

## 3. Pilotage projet

| Champ | Valeur |
|---|---|
| Epic parent | EPIC-0014 — Alignement modules 4 à 12 du CDC |
| User story parent | STORY-1911 — Dette technique architecture SOLID, sécurité et design system |
| Sprint cible | SPRINT-0011 |
| Priorité business | P1 |
| Complexité | M |
| Story points | 5 |
| Profil recommandé | Senior |
| Effort estimé senior | 0.4j |
| Effort estimé intermédiaire | 0.6j |
| Effort estimé junior | 1.0j |
| Responsable | Antigravity |
| Reviewer obligatoire | Lead Developer |
| Risque fonctionnel | Faible |
| Risque technique | Faible |
| Dépendances | Aucun |
| Bloquants connus | Aucun |

## 4. Contexte analysé

- [x] `AGENTS.md` lu
- [x] `SKILL.md` lu
- [x] `README-IA.md` lu
- [x] `PROJECT-TRACKING.md` lu
- [x] Rapport d'audit de l'agent i18n pour Auth & Patient modules analysé
- [x] Présence de `fr.json` et `en.json` confirmée

## 5. Hypothèses

- L'extraction des clés i18n se fera selon une hiérarchie cohérente (ex: `login.*`, `patient.consent.*`, `patient.visit.*`).
- Le composant `unauthorized.component.ts` nécessite l'injection de `I18nService` pour charger et traduire ses messages.

## 6. Risques et impacts

| Risque | Impact | Mitigation |
|---|---|---|
| Oubli de clés de traduction ou clés asynchrones | Texte manquant ou clé affichée brute | Validation manuelle et vérification des fichiers json |

## 7. Action plan

- [ ] Mettre à jour `fr.json` et `en.json` avec toutes les clés manquantes pour l'authentification et les patients.
- [ ] Remplacer les textes en dur dans `login.component.html`.
- [ ] Remplacer les textes en dur dans `forgot-password.component.ts`.
- [ ] Injecter `I18nService` dans `unauthorized.component.ts` et appliquer les traductions.
- [ ] Remplacer les textes en dur dans `patient-list.component.html` et son `.ts`.
- [ ] Remplacer les textes en dur dans `patient-detail.component.ts`.
- [ ] Remplacer les textes en dur dans `patient-form.component.ts`.
- [ ] Remplacer les textes en dur dans `patient-medical-info.component.ts`.
- [ ] Lancer les tests pour s'assurer que rien n'a été brisé.
- [ ] Mettre à jour `CHANGELOG.md` et `PROJECT-TRACKING.md`.

## 8. Implémentation réalisée

*(En cours d'exécution)*

## 9. Suivi d'exécution

| Date | Développeur | Temps passé | Avancement | Reste à faire | Blocage | Commentaire |
|---|---|---:|---:|---:|---|---|
| 2026-07-07 | Antigravity | 0.05j | 10% | Traduction et édition des composants | Aucun | Ticket initialisé |

## 10. Tests et vérifications

*(À lancer après édition)*

## 11. Documentation

- [x] Ticket de suivi créé : `docs/ai/tickets/TICKET-I18N-COMPLETION.md`

## 12. Reste à faire

- [ ] Implémenter les modifications et valider la compilation

## 13. Statut final

Statut : IN_PROGRESS

## 14. Impact version / SemVer

| Champ | Valeur |
|---|---|
| Changement livrable | Oui |
| Type de bump | PATCH |
| Justification | Internationalisation des écrans de connexion et de gestion des dossiers patients |
| Breaking change | Non |
| Migration DB | Non |
| Changement API | Non |
| Impact Angular | Oui |
| Impact Flutter | Non |
| Changelog requis | Oui |
| Release note requise | Non |
