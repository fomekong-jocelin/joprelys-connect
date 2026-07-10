# STORY-2116 — Tests E2E globaux et Accessibilité

## 1. Objectif

Mettre en œuvre des tests de non-régression de bout en bout (E2E) couvrant le cycle financier nominal de l'établissement (de l'admission à l'exportation comptable Sage 100), et valider la conformité d'accessibilité (contraste, focus, structure HTML sémantique) sur tous les écrans d'intégrité financière.

## 2. Critères d'acceptation

- [x] Création et succès du test d'intégration E2E global backend `FullFinancialE2ETest.java` validant la cohérence logique de tout le cycle.
- [x] Tests de couverture de sécurité RBAC (vérifier que les rôles `AGENT_ACCUEIL`, `CAISSIER`, `DAF` sont bien cloisonnés pour chaque opération).
- [x] Validation de l'accessibilité sur le frontend Angular :
  - Structure HTML sémantique de tous les tableaux d'historique de caisse, créances, relances et pilotage DAF.
  - Ajout des `aria-label` sur les boutons iconographiques (ex: téléchargement de reçu, téléchargement d'export Sage).
  - Validation du focus visible sur tous les éléments interactifs.
- [x] Compilation et exécution réussie de la suite de tests globale du projet (JUnit backend et Vitest Angular).

## 3. Pilotage projet

| Champ | Valeur |
|---|---|
| Epic parent | EPIC-0018 - Intégrité financière et poste facturation/caisse |
| User story parent | STORY-2116 |
| Sprint cible | SPRINT-0013 |
| Priorité business | P0 |
| Complexité | S |
| Story points | 5 |
| Profil recommandé | Senior full-stack |
| Effort estimé senior | 0.5 j |
| Effort estimé intermédiaire | 0.6 j |
| Effort estimé junior | 1.0 j |
| Responsable | Codex |
| Reviewer obligatoire | Lead Developer + DAF |
| Risque fonctionnel | Faible |
| Risque technique | Faible |
| Dépendances | STORY-2115 (pilotage DAF) |
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
- [x] Spécifications fonctionnelles dans `STORY-2116-FUNCTIONAL-SPEC.md` rédigées
- [x] Spécifications techniques dans `STORY-2116-TECHNICAL-DESIGN.md` rédigées

## 5. Action plan

- [x] Implémenter le test d'intégration JUnit `FullFinancialE2ETest.java` pour valider l'ensemble du cycle financier backend de bout en bout.
- [x] Auditer et modifier les templates HTML des composants Angular financiers pour y ajouter des attributs `aria-label` et valider la structure sémantique (tables, inputs, focus).
- [x] Lancer la compilation de production et valider l'exécution réussie de tous les tests unitaires frontend (Vitest) et backend (JUnit).
- [x] Mettre à jour `CHANGELOG.md` et `PROJECT-TRACKING.md`.

## 6. Implémentation réalisée

- Test E2E backend `FullFinancialE2ETest.java` complété et exécuté avec succès :
  - Cycle financier nominal complet : admission, facturation, encaissement part patient, génération/envoi/paiement bordereau assurance, versement banque, clôture de caisse.
  - Tests RBAC cloisonnant `AGENT_ACCUEIL`, `CAISSIER` et `DAF` sur les opérations critiques.
  - Gestion multi-tenant via `TenantContext.setTenantId(...)` et nettoyage manuel de la base H2.
- Accessibilité frontend Angular sur les écrans financiers :
  - Ajout d'attributs `aria-label` sur tous les boutons iconographiques seuls (fermeture de modales, fermeture d'alertes, suppression de ligne de facture).
  - Ajout de `scope="col"` sur tous les en-têtes de tableaux (facturation, caisse, créances, bordereaux, pilotage DAF).
  - Ajout d'une règle CSS `:focus-visible` globale de fallback pour garantir un focus visible sur tous les éléments interactifs.
  - Internationalisation des libellés d'onglets précédemment codés en dur.
- Mise à jour des fichiers de traduction `fr.json` / `en.json` avec les clés `common.aria.*`, `billing.aria.*` et `billing.tab*`.
- Extension de `I18nService.t()` pour accepter une valeur par défaut, facilitant l'utilisation dans les composants sans `@Input translate`.

## 7. Statut final

Statut : **DONE**
