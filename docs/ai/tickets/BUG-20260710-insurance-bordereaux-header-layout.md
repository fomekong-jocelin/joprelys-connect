# BUG-20260710-BORDEREAUX-HEADER — En-tête de liste des bordereaux d'assurance comprimé

## Mode

Engineering — correctif UI Angular rétrocompatible.

**Statut :** QA — implémentation et automatisation terminées, contrôle visuel manuel restant.

## Problème

Dans l'onglet « Bordereaux Assurances », le filtre de statut hérite de la largeur globale `100%` de `.ui-select`. Dans l'en-tête horizontal de la liste, il comprime le titre « Liste des bordereaux générés » et provoque un retour sur deux lignes alors que les libellés du filtre sont courts.

## Objectif

Conserver le titre sur une ligne aux largeurs tablette/desktop et limiter le filtre à une largeur proportionnée à son contenu, tout en gardant une disposition empilée et exploitable sur mobile.

## Critères d'acceptation

- [x] Le titre « Liste des bordereaux générés » tient sur une ligne aux largeurs tablette/desktop.
- [x] Le filtre de statut ne prend plus toute la largeur aux largeurs tablette/desktop.
- [x] Sur mobile, l'en-tête peut s'empiler et le filtre reste pleine largeur.
- [x] Les thèmes light/dark, le focus visible, l'i18n existante et les rayons du design system sont préservés.
- [x] Aucun contrat API, modèle de données, règle financière ou permission n'est modifié.

## Actions

- [x] Analyser la règle globale `.ui-select` et le composant concerné.
- [x] Documenter le comportement attendu et la stratégie responsive.
- [x] Corriger les classes de layout de l'en-tête.
- [x] Ajouter un test Angular de non-régression.
- [x] Exécuter les tests ciblés et le build Angular.
- [ ] Vérifier le rendu navigateur en light/dark et aux largeurs desktop/mobile.
- [x] Mettre à jour le changelog et le suivi projet.

## Definition of Ready

- [x] Capture de reproduction disponible.
- [x] Cause technique identifiée.
- [x] Périmètre limité au composant de bordereaux.
- [x] Critères d'acceptation et tests attendus définis.

## Definition of Done

- [x] Critères d'acceptation validés par le code et les tests automatisés.
- [x] Test ciblé, suite Angular globale et build Angular réussis.
- [ ] QA visuelle light/dark et responsive réalisée ou blocage documenté.
- [x] Ticket, documentation, changelog et suivi mis à jour.

## Estimation et responsabilités

| Champ | Valeur |
|---|---|
| Priorité | P2 |
| Story points | 1 |
| Estimation senior | 0,1 j |
| Estimation intermédiaire | 0,15 j |
| Estimation junior | 0,25 j |
| Profil recommandé | Frontend Angular intermédiaire |
| Reviewer | Lead Frontend / DAF |
| Sprint | SPRINT-0014 |

## Tests attendus

- Test Vitest du composant vérifiant la disposition responsive et la largeur du filtre.
- Build Angular de production.
- QA navigateur sur l'URL `/clinic/billing`, onglet « Bordereaux Assurances ».

## Sécurité et régression

- Aucun flux de données, endpoint, permission ou donnée de santé modifié.
- Risque principal : débordement sur petit écran, couvert par la disposition empilée et le filtre pleine largeur sur mobile.
- Dette préexistante non étendue : le fichier du composant atteint `387` lignes et conserve un template inline avec plusieurs libellés non internationalisés. Il reste sous la limite dure de `500` lignes mais dépasse le seuil d'alerte de `300` lignes ; une extraction du template et une passe i18n doivent être planifiées dans STORY-2204.

## Impact version

- **PATCH** : correction UI rétrocompatible, sans changement de contrat.

## Reste à faire

- QA visuelle manuelle light/dark aux largeurs desktop et mobile. Le navigateur intégré n'était pas disponible dans la session d'exécution.

## Résultats de vérification

- Test ciblé : `1` test passé.
- Suite Angular : `122` tests passés dans `26` fichiers.
- Build Angular de production : réussi.
- Avertissement préexistant : budget CSS de `billing-estimates.component.css` dépassé de `109` octets, sans lien avec ce correctif.

## Checklist de review

| Contrôle | Statut | Justification |
|---|---|---|
| Changement minimal / SRP | ✅ | Classes de layout uniquement ; aucune logique métier ajoutée. |
| Limites de taille | ⚠️ | Composant à `387` lignes : sous la limite dure de `500`, au-dessus du seuil d'alerte de `300`, dette tracée. |
| Tailwind CSS v4 / design system | ✅ | Utilitaires Tailwind v4 et classes `.ui-*` existantes ; aucun token arbitraire ajouté. |
| Angular Material / Tailwind v3 | ✅ | Aucun ajout. |
| Thèmes light/dark | ✅ | Aucune couleur ni logique de thème modifiée. |
| i18n FR/EN | ✅ | Aucun nouveau texte visible ; dette i18n préexistante tracée. |
| Accessibilité | ✅ | Le focus et le contrôle natif `select` sont préservés ; aucune interaction modifiée. |
| API / DB / configuration | ➖ | Aucun impact. Proxy Angular existant et référencé. |
| OWASP / données sensibles | ➖ | Aucun flux, entrée, donnée sensible ou mécanisme d'autorisation modifié. |
| 12-Factor | ➖ | Aucune configuration ou dépendance d'environnement modifiée. |
| `.gitignore` | ✅ | Présent et adapté aux stacks Maven/Angular/Flutter ; aucun artefact généré versionné. |
| Tests | ✅ | Test ciblé, 122 tests globaux et build de production réussis. |
| Documentation / suivi / SemVer | ✅ | Spécifications, plan de test, changelog, epic, ticket et suivi mis à jour ; impact PATCH. |
