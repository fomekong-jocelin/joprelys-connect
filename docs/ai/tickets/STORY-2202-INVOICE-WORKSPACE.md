# STORY-2202 — Workspace Factures orienté tâche

## Mode

Product Design + Frontend Engineering + QA.

## Statut

IN_PROGRESS — lot de finalisation du workspace factures après stabilisation du contrat financier STORY-2201.

## Objectif

Permettre à la secrétaire comptable de comprendre immédiatement si elle doit consulter, encaisser ou suivre l’assurance, sans confondre chargement, absence de données et erreur.

## Périmètre

- historique des factures du patient ;
- hiérarchie visuelle des montants et du statut de recouvrement ;
- action principale contextuelle par facture ;
- états chargement, erreur et vide ;
- sélection visible de la facture ouverte ;
- ouverture du panneau de détail avec focus clavier ;
- fermeture avec restauration du focus sur la facture sélectionnée ;
- responsive mobile/tablette/desktop, thèmes light/dark et i18n FR/EN.

## Hors périmètre

- poste caisse et file d’encaissement (`STORY-2203`) ;
- parcours complet des bordereaux (`STORY-2204`) ;
- modification d’un endpoint ou d’un calcul financier ;
- paiement partiel d’un bordereau assurance ;
- refonte globale de la page de facturation.

## Plan d’action

- [x] Auditer le lot 1 et les critères restants.
- [x] Définir le périmètre UX et les états attendus.
- [x] Documenter la spécification, la conception et le plan de tests.
- [ ] Extraire le template et les styles de l’historique.
- [ ] Ajouter les états chargement, erreur et vide explicites.
- [ ] Présenter les montants restant dus depuis `InvoiceSettlementSummary`.
- [ ] Limiter chaque facture à une action financière principale contextuelle.
- [ ] Rendre la sélection courante visible et accessible.
- [ ] Déplacer le focus dans le panneau à l’ouverture et le restaurer à la fermeture.
- [ ] Ajouter les traductions FR/EN.
- [ ] Couvrir les états critiques et le focus par tests Angular.
- [ ] Exécuter tests Angular, build de production et Maven strict.
- [ ] Mettre à jour le suivi central et le changelog.

## Critères d’acceptation

- [ ] Le chargement n’est jamais présenté comme une liste vide.
- [ ] Une erreur de chargement propose une action de nouvelle tentative.
- [ ] Chaque carte expose le total et les restes patient/assurance sans recalcul local.
- [ ] Une seule action financière principale est présentée selon `collectionStatus`.
- [ ] Les actions tiennent sur une ligne en desktop et restent utilisables sur mobile.
- [ ] La facture ouverte est identifiable visuellement et par attribut accessible.
- [ ] Le panneau reçoit le focus à l’ouverture, accepte Échap et restaure le focus à la fermeture.
- [ ] Les textes visibles sont internationalisés en français et en anglais.
- [ ] Les thèmes light/dark et les rayons du design system sont respectés.
- [ ] Les tests Angular et le build de production sont verts.

## Estimation

| Champ | Valeur |
|---|---|
| Priorité | P0 |
| Story points | 8 |
| Estimation senior | 2,5 j globale ; lot restant estimé à 1,2 j |
| Profil | Senior Angular + Product Design |
| Reviewer | Lead Developer + Product/DAF |
| Sprint | SPRINT-0014 |

## Risques

- densité excessive si les trois montants et toutes les actions sont affichés sans hiérarchie ;
- régression clavier lors de l’ouverture/fermeture du panneau ;
- états historiques sans synthèse de règlement ;
- chevauchement fonctionnel à éviter avec STORY-2203 et STORY-2205.

## Impact version

MINOR — amélioration rétrocompatible du parcours et de l’accessibilité, sans évolution d’API.