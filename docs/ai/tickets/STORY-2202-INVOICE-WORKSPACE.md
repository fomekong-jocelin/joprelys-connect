# STORY-2202 — Workspace Factures orienté tâche

## Mode

Product Design + Frontend Engineering + QA.

## Statut

QA — implémentation terminée et validation automatisée verte ; revue PR et QA visuelle navigateur restantes.

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
- [x] Extraire le template et les styles de l’historique.
- [x] Ajouter les états chargement, erreur et vide explicites.
- [x] Présenter les montants restant dus depuis `InvoiceSettlementSummary`.
- [x] Limiter chaque facture à une action financière principale contextuelle.
- [x] Rendre la sélection courante visible et accessible.
- [x] Déplacer le focus dans le panneau à l’ouverture et le restaurer à la fermeture.
- [x] Ajouter les traductions FR/EN.
- [x] Couvrir les états critiques et le focus par tests Angular.
- [x] Exécuter tests Angular, build de production et Maven strict.
- [x] Mettre à jour le suivi central et le changelog.

## Critères d’acceptation

- [x] Le chargement n’est jamais présenté comme une liste vide.
- [x] Une erreur de chargement propose une action de nouvelle tentative.
- [x] Chaque carte expose le total et les restes patient/assurance sans recalcul local.
- [x] Une seule action financière principale est présentée selon `collectionStatus`.
- [x] Les actions tiennent sur une ligne en desktop et restent utilisables sur mobile.
- [x] La facture ouverte est identifiable visuellement et par attribut accessible.
- [x] Le panneau reçoit le focus à l’ouverture, accepte Échap et restaure le focus à la fermeture.
- [x] Les textes visibles sont internationalisés en français et en anglais.
- [x] Les thèmes light/dark et les rayons du design system sont respectés dans l’implémentation.
- [x] Les tests Angular et le build de production sont verts.

## Estimation et réalisation

| Champ | Valeur |
|---|---|
| Priorité | P0 |
| Story points | 8 |
| Estimation senior | 2,5 j globale |
| Temps passé cumulé | 2,0 j, dont 1,2 j sur le lot de finalisation |
| Profil | Senior Angular + Product Design |
| Reviewer | Lead Developer + Product/DAF |
| Sprint | SPRINT-0014 |

## Résultats de validation

- tests Angular : ✅ ;
- build Angular de production : ✅ ;
- Maven `clean verify` : ✅ ;
- suite backend, H2 et PostgreSQL 16 : ✅ ;
- cas `PATIENT_DUE`, `PATIENT_PARTIALLY_PAID`, `INSURANCE_DUE`, `SETTLED` et `CANCELLED` : ✅ ;
- chargement, erreur/retry, vide, sélection et restauration du focus : ✅ ;
- diff du composant parent reconstruit depuis `main` pour ne conserver que les changements STORY-2202 ;
- aucun test désactivé ou contourné.

## Sécurité et régression

- aucun rôle ou endpoint modifié ;
- aucun calcul financier ajouté au frontend ;
- les soldes proviennent de `InvoiceSettlementSummary` ;
- les états terminaux `SETTLED` et `CANCELLED` restent non encaissables ;
- les données historiques sans synthèse utilisent un fallback de présentation prudent ;
- le téléchargement PDF et le parcours bordereau restent inchangés.

## Risques restants

- QA visuelle manuelle à effectuer en 360 px, 768 px et 1440 px, en thèmes light/dark ;
- validation Product/DAF de la hiérarchie des montants et des libellés ;
- test navigateur manuel du cycle complet de focus avec lecteur d’écran recommandé ;
- `BillingManagementPageComponent` reste un composant dense à décomposer dans un chantier ultérieur ;
- chevauchement fonctionnel à éviter avec STORY-2203 et STORY-2205.

## Impact version

MINOR — amélioration rétrocompatible du parcours et de l’accessibilité, sans évolution d’API.

## Reste à faire

- revue de la PR #15 ;
- QA visuelle light/dark et responsive ;
- validation Product/DAF ;
- fusion après approbation.
