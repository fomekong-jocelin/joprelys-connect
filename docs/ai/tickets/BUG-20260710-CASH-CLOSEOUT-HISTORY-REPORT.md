# BUG-20260710 — Historique et bordereau de clôture de caisse

## Mode

Product Design + Backend Engineering + Frontend Engineering + QA.

## Statut

QA — implémentation, refonte de l’espace de travail et documentation terminées sur `fix/cash-register-history-closeout-report` ; validation visuelle utilisateur restante.

## Objectif

Rendre la clôture exploitable et auditable sans élargir le périmètre assurance : espacement conforme au design system, historique personnel tenanté, récapitulatif post-clôture et bordereau PDF téléchargeable.

## Correctifs livrés

- espacement vertical rétabli entre la file d’encaissement et les cartes de session via un hôte Angular de type bloc ;
- état « caisse clôturée » refondu en espace de travail pleine largeur au lieu d’un petit formulaire centré ;
- organisation responsive en deux zones sur desktop et une colonne sur mobile/tablette ;
- extraction du template du composant caisse vers un fichier HTML dédié afin de séparer la logique du design ;
- historique personnel borné aux vingt dernières sessions du caissier connecté ;
- historique repliable avec filtres de période 7 jours, 30 jours, 90 jours ou toutes les sessions ;
- récapitulatif post-clôture rafraîchi automatiquement et mis en évidence ;
- totaux espèces, chèques, virements, dépenses, versements banque, attendu, déclaré et écart fournis par le backend ;
- détail des mouvements chargé à la demande ;
- bordereau PDF numéroté `CLS-yyyyMMdd-XXXXXXXX` avec établissement, caisse, caissier, rapprochement, mouvements et signatures ;
- numéro de bordereau attribué uniquement aux sessions `CLOSED` ;
- contrôle explicite du propriétaire de session et accès de supervision DAF/Admin ;
- formatage PDF sans état mutable partagé entre requêtes concurrentes ;
- traductions FR/EN et états chargement, erreur, vide et succès.

## Règles métier

1. Un caissier consulte uniquement les sessions qu’il a ouvertes dans son établissement.
2. DAF et ADMIN_CLINIQUE peuvent consulter et télécharger le bordereau d’une session de leur établissement.
3. Le bordereau est disponible uniquement pour une session `CLOSED`.
4. Le numéro est déterministe : `CLS-yyyyMMdd-XXXXXXXX`, sans nouvelle séquence ni migration.
5. Les totaux sont produits par `CashSessionReconciliationCalculator`, jamais par Angular.
6. Le PDF contient l’établissement, la caisse, le caissier, les horaires, les totaux par moyen, le montant déclaré, l’écart, la justification, les mouvements et les zones de signature.
7. L’historique peut être réduit et filtré localement par période sans recalcul financier côté frontend.

## Plan d’action

- [x] Auditer l’écran, les endpoints, les sessions et les reçus existants.
- [x] Créer une branche corrective dédiée.
- [x] Documenter les règles et le périmètre.
- [x] Ajouter le read model d’historique de session.
- [x] Exposer l’historique personnel et le bordereau PDF sécurisé.
- [x] Ajouter les tests RBAC, tenant, totaux et PDF.
- [x] Ajouter le composant Angular d’historique et le récapitulatif post-clôture.
- [x] Intégrer l’historique au composant caisse partagé.
- [x] Revoir la mise en page de l’état caisse clôturée en pleine largeur.
- [x] Ajouter le repli et les filtres de période de l’historique.
- [x] Corriger l’espacement des cartes.
- [x] Ajouter les traductions FR/EN et tests Angular.
- [x] Exécuter Maven strict, PostgreSQL 16, tests Angular et build de production.
- [x] Mettre à jour le suivi central et le changelog.

## Critères d’acceptation

- [x] Les cartes de premier niveau utilisent un espacement vertical de 24 px.
- [x] L’état caisse clôturée utilise la largeur disponible sans grand vide latéral artificiel.
- [x] L’historique peut être replié et filtré sur 7, 30, 90 jours ou toutes les périodes.
- [x] Une clôture réussie rafraîchit immédiatement l’historique et met la session en évidence.
- [x] Le caissier voit ses vingt dernières sessions, triées de la plus récente à la plus ancienne.
- [x] Les totaux espèces, chèques, virements, dépenses, banque et écart sont visibles.
- [x] Le détail des mouvements est consultable.
- [x] Le PDF de clôture est téléchargeable et commence par une signature PDF valide.
- [x] Un autre caissier ne peut pas télécharger le bordereau d’une session qui ne lui appartient pas.
- [x] Les contrôles tenant, DAF/Admin et non-régression sont couverts.
- [ ] Le parcours est validé manuellement à 360 px, 768 px et 1440 px, en thèmes light/dark.

## Résultats de validation

- CI complète après refonte du workspace : ✅ ;
- tests Angular : ✅ ;
- build Angular de production : ✅ ;
- Maven `clean verify` : ✅ ;
- migrations H2 et PostgreSQL 16 via Testcontainers : ✅ ;
- test d’historique personnel et des totaux : ✅ ;
- tests de repli et de filtre par période : ✅ ;
- test du PDF `%PDF-` et de son en-tête de téléchargement : ✅ ;
- refus d’un autre caissier et d’un rôle clinique : ✅ ;
- aucun test désactivé ou contourné.

## Hors périmètre

- règlement assurance et STORY-2204 ;
- signature électronique ;
- impression thermique ESC/POS ;
- migration des montants historiques `Double` ;
- pagination serveur au-delà des vingt dernières sessions du caissier.

## Estimation et réalisation

| Champ | Valeur |
|---|---|
| Priorité | P0 |
| Story points | 5 |
| Estimation senior | 1,5 j |
| Temps passé cumulé | 1,5 j |
| Reviewer | Lead Developer + DAF |
| Sprint | SPRINT-0014 |

## Risques résiduels

- QA visuelle réelle sur mobile, tablette et desktop ;
- ouverture et vérification humaine du rendu PDF ;
- volumétrie supérieure à vingt sessions à traiter ultérieurement par pagination ;
- migration séparée des montants de caisse historiques encore en `Double`.

## Impact version

MINOR — ajout rétrocompatible d’endpoints de lecture/PDF, d’un parcours d’historique filtrable et d’une refonte du workspace caisse.

## Reste à faire

- QA visuelle light/dark et responsive ;
- vérification humaine du PDF ;
- revue et fusion de la PR #17 après approbation.
