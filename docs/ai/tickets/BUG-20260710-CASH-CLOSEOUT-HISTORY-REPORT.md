# BUG-20260710 — Historique et bordereau de clôture de caisse

## Mode

Product Design + Backend Engineering + Frontend Engineering + QA.

## Statut

IN_PROGRESS — correctif P0 sur `fix/cash-register-history-closeout-report`.

## Constat

Après la fusion de STORY-2203 :

- la carte de file d’encaissement et la grille de session apparaissent visuellement collées ;
- la clôture masque immédiatement la session sans présenter son récapitulatif ;
- le caissier ne voit aucun historique de ses sessions clôturées ;
- aucun bordereau PDF de clôture n’est généré.

## Objectif

Rendre la clôture exploitable et auditable sans élargir le périmètre assurance : espacement conforme au design system, historique personnel tenanté, récapitulatif post-clôture et bordereau PDF téléchargeable.

## Règles métier

1. Un caissier consulte uniquement les sessions qu’il a ouvertes dans son établissement.
2. DAF et ADMIN_CLINIQUE peuvent consulter et télécharger le bordereau d’une session de leur établissement.
3. Le bordereau est disponible uniquement pour une session `CLOSED`.
4. Le numéro est déterministe : `CLS-yyyyMMdd-XXXXXXXX`, sans nouvelle séquence ni migration.
5. Les totaux sont recalculés par `CashSessionReconciliationCalculator`, jamais par Angular.
6. Le PDF contient l’établissement, la caisse, le caissier, les horaires, les totaux par moyen, le montant déclaré, l’écart, la justification, les mouvements et les zones de signature.

## Plan d’action

- [x] Auditer l’écran, les endpoints, les sessions et les reçus existants.
- [x] Créer une branche corrective dédiée.
- [x] Documenter les règles et le périmètre.
- [ ] Ajouter le read model d’historique de session.
- [ ] Exposer l’historique personnel et le bordereau PDF sécurisé.
- [ ] Ajouter les tests RBAC, tenant, totaux et PDF.
- [ ] Ajouter le composant Angular d’historique et le récapitulatif post-clôture.
- [ ] Corriger l’espacement des cartes.
- [ ] Ajouter les traductions FR/EN et tests Angular.
- [ ] Exécuter Maven strict, PostgreSQL 16, tests Angular et build de production.
- [ ] Mettre à jour le suivi central et le changelog.

## Critères d’acceptation

- [ ] Les cartes de premier niveau ont un espacement vertical de 24 px.
- [ ] Une clôture réussie rafraîchit immédiatement l’historique et met la session en évidence.
- [ ] Le caissier voit ses vingt dernières sessions, triées de la plus récente à la plus ancienne.
- [ ] Les totaux espèces, chèques, virements, dépenses, banque et écart sont visibles.
- [ ] Le détail des mouvements est consultable.
- [ ] Le PDF de clôture est téléchargeable et commence par une signature PDF valide.
- [ ] Un autre caissier ne peut pas télécharger le bordereau d’une session qui ne lui appartient pas.
- [ ] Les contrôles tenant, DAF/Admin et non-régression sont couverts.
- [ ] Le parcours est responsive, light/dark et accessible au clavier.

## Hors périmètre

- règlement assurance et STORY-2204 ;
- signature électronique ;
- impression thermique ESC/POS ;
- migration des montants historiques `Double` ;
- pagination serveur au-delà des vingt dernières sessions du caissier.

## Estimation

| Champ | Valeur |
|---|---|
| Priorité | P0 |
| Story points | 5 |
| Estimation senior | 1,5 j |
| Reviewer | Lead Developer + DAF |
| Sprint | SPRINT-0014 |

## Impact version

MINOR — ajout rétrocompatible d’endpoints de lecture/PDF et d’un parcours d’historique.