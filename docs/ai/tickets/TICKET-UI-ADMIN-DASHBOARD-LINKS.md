# TICKET-UI-ADMIN-DASHBOARD-LINKS — Correction des liens inactifs du Tableau de Bord Admin

## 1. Objectif

Rendre actifs et fonctionnels les deux liens actuellement inactifs (boutons sans action) sur la section d'administration du tableau de bord de l'administrateur JOPRELYS (`ADMIN_JOPRELYS`) :
- **Configuration Interop** : Rediriger l'utilisateur vers la page de gestion des établissements (`/organizations`), qui permet d'activer/désactiver l'accès API et de générer/révoquer les clés d'API interopérabilité pour chaque clinique, pharmacie ou laboratoire.
- **Journal d'Audit Sécurisé** : Ouvrir un dialogue modal d'information expliquant que pour préserver le secret médical (ségrégation des responsabilités / règlementation FR-AUDIT-002), l'administrateur système n'a pas accès aux données de traçabilité clinique, et inviter à utiliser un profil habilité (`AUDITEUR` ou `ADMIN_CLINIQUE`).

## 2. Critères d'acceptation

- [x] Remplacer le bouton inactif "Configurer" sous "Configuration Interop" par un lien `routerLink="/organizations"`.
- [x] Remplacer le bouton inactif "Consulter" sous "Journal d'Audit Sécurisé" par un bouton déclenchant l'ouverture d'un dialogue modal d'information de sécurité.
- [x] Créer le dialogue modal dans `dashboard.component.html` avec un bouton "Compris" pour le fermer.
- [x] Déclarer l'état réactif et les méthodes de contrôle du modal dans `DashboardComponent`.
- [x] S'assurer que le build compile sans aucune erreur.
- [x] S'assurer que tous les tests unitaires existants continuent de passer.

## 3. Pilotage projet

| Champ | Valeur |
|---|---|
| Epic parent | EPIC-0014 — Alignement modules 4 à 12 du CDC |
| User story parent | STORY-1910 — Portails patient, pro, labo, pharmacie et vérification publique conformes CDC |
| Sprint cible | SPRINT-0011 |
| Priorité business | P1 |
| Complexité | XS |
| Story points | 1 |
| Profil recommandé | Junior |
| Effort estimé senior | 0.02j |
| Effort estimé intermédiaire | 0.05j |
| Effort estimé junior | 0.1j |
| Responsable | Antigravity |
| Reviewer obligatoire | Lead Developer |
| Risque fonctionnel | Faible |
| Risque technique | Faible |
| Dépendances | Aucune |
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
- [x] Image `pasted-image-11.png` analysée
- [x] Code source de `dashboard.component.html` et `dashboard.component.ts` analysé

## 5. Hypothèses

- L'administrateur général n'a pas vocation à lire les données cliniques, le journal d'audit général pour un administrateur n'étant pas encore implémenté ou volontairement bloqué pour des raisons de confidentialité médicale (isolation tenant). Un message éducatif et explicatif est la meilleure réponse.

## 6. Risques et impacts

| Risque | Impact | Mitigation |
|---|---|---|
| Aucun risque identifié | Très faible | NA |

## 7. Action plan

- [x] Modifier `dashboard.component.ts` pour introduire l'état réactif `showAuditSecurityModal` et les méthodes d'ouverture/fermeture.
- [x] Modifier `dashboard.component.html` pour lier les boutons aux actions (lien `routerLink` et clic handler) et intégrer la structure du modal de sécurité.
- [x] Valider le build Angular via `npm run build`.
- [x] Valider la suite de tests unitaires via `npm run test`.
- [x] Mettre à jour `CHANGELOG.md` et `PROJECT-TRACKING.md`.
- [x] Finaliser ce ticket avec le statut `DONE`.

## 8. Implémentation réalisée

1. **Configuration Interop** :
   - Remplacement de l'élément `<button>` générique inactif par un élément de lien `<a>` Angular doté de la directive `routerLink="/organizations"`. L'admin JOPRELYS est ainsi redirigé vers l'écran de gestion des établissements qui centralise l'accès API (`apiEnabled`) et les clés API de chaque organisation (Module 1 - TICKET-0112).
2. **Journal d'Audit Sécurisé** :
   - Liaison du bouton "Consulter" à la méthode `openAuditSecurityModal()` de `DashboardComponent`.
   - Création de la variable d'état réactive `showAuditSecurityModal` (signal) et des méthodes `openAuditSecurityModal()` et `closeAuditSecurityModal()`.
   - Ajout du composant modal d'information dans le template `dashboard.component.html`. Ce modal explique de façon pédagogique et sécurisée les restrictions d'accès de l'administrateur système général aux logs cliniques dans un souci de ségrégation des responsabilités (Module 14 - FR-AUDIT-002).
3. **Tests unitaires** :
   - Écriture d'un scénario de test pour vérifier la transition d'état et le contrôle du modal de sécurité d'audit dans `dashboard.component.spec.ts`.

## 9. Suivi d'exécution

| Date | Développeur | Temps passé | Avancement | Reste à faire | Blocage | Commentaire |
|---|---|---:|---:|---:|---|---|
| 2026-07-06 | Antigravity | 0.02j | 100% | Aucun | Aucun | Modification, build, tests et tracking complétés |

## 10. Tests et vérifications

### Commandes exécutées ou à exécuter

```bash
# Angular
npm run test
npm run build
```

### Résultats

- [x] Tests unitaires OK (80 tests passés avec succès, incluant le nouveau test d'état du modal)
- [x] Build de production OK (Compilation de production réussie avec succès)

## 11. Documentation

- [x] Changelog mis à jour (`docs/ai/CHANGELOG.md`)
- [x] Suivi projet mis à jour (`docs/ai/PROJECT-TRACKING.md`)

## 12. Reste à faire

Aucun.

## 13. Statut final

Statut : DONE

## 14. Impact version / SemVer

| Champ | Valeur |
|---|---|
| Changement livrable | Oui |
| Type de bump | PATCH |
| Justification | Correction de liens inactifs sur le tableau de bord admin et ajout d'un modal explicatif de sécurité |
| Breaking change | Non |
| Migration DB | Non |
| Changement API | Non |
| Impact Angular | Oui |
| Impact Flutter | Non |
| Changelog requis | Oui |
| Release note requise | Non |
