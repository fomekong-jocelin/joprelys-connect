# FUNCTIONAL-SPEC — Workspace Factures orienté tâche

## Problème

L’historique actuel présente les informations financières sous forme d’une ligne dense et juxtapose toutes les actions. Le chargement est indistinguable d’une absence de facture et l’ouverture du détail n’est pas suffisamment visible ni accessible au clavier.

## Utilisateur principal

Secrétaire comptable ou agent de facturation chargé de consulter une facture, d’encaisser la part patient ou d’orienter la créance vers le suivi assurance.

## Source de vérité

L’écran consomme `InvoiceSettlementSummary` produit par le backend. Il n’effectue aucun calcul de solde financier.

## Parcours cible

1. Après sélection du patient, l’historique affiche un état de chargement explicite.
2. En cas d’échec, un message contextualisé permet de réessayer.
3. Chaque facture affiche le numéro, la date, le statut de recouvrement, le total et les restes patient/assurance.
4. Une seule action financière principale est proposée :
   - `PATIENT_DUE` ou `PATIENT_PARTIALLY_PAID` → `Encaisser` ;
   - `INSURANCE_DUE` → `Suivre l’assurance` ;
   - `SETTLED`, `CANCELLED` ou `NOT_YET_DUE` → aucune action d’encaissement.
5. Les actions secondaires `Détail` et `PDF` restent disponibles selon le contexte.
6. L’ouverture du détail sélectionne visuellement la facture et place le focus dans le panneau.
7. La fermeture par bouton, clic sur le voile ou touche Échap restaure le focus sur la facture ouverte.

## États d’interface

| État | Rendu attendu |
|---|---|
| Chargement | indicateur animé et libellé accessible, sans message « aucune facture » |
| Erreur | alerte avec bouton `Réessayer` |
| Vide | message explicite indiquant qu’aucune facture n’existe pour ce patient |
| Données | liste sémantique de cartes de facture |
| Sélection | bordure/fond sobres et `aria-current` sur la carte active |

## Hiérarchie visuelle

- Niveau 1 : numéro de facture et statut de recouvrement.
- Niveau 2 : total, reste patient et reste assurance.
- Niveau 3 : date d’émission et indication métier éventuelle.
- Actions : une action financière principale maximum, puis `Détail` et `PDF` en secondaires.

## Responsive

- Mobile : montants empilés en grille, actions sur plusieurs lignes si nécessaire, cibles tactiles d’au moins 44 px.
- Tablette : carte en deux zones, informations puis actions.
- Desktop : actions sur une ligne et largeur stable sans compression des libellés.

## Accessibilité

- liste et éléments sémantiques ;
- états `role=status` ou `role=alert` ;
- boutons nommés et focus visible ;
- panneau `role=dialog`, `aria-modal`, titre relié par `aria-labelledby` ;
- focus déplacé à l’ouverture et restauré à la fermeture ;
- fermeture par Échap.

## Internationalisation et thèmes

Tous les textes visibles utilisent les ressources FR/EN. Les couleurs, rayons, surfaces et ombres proviennent du design system et restent compatibles light/dark.

## Critères d’acceptation

- les soldes affichés viennent du read model backend ;
- aucun bouton d’encaissement n’est proposé pour une facture soldée ou annulée ;
- le chargement, l’erreur et le vide sont distincts ;
- la facture ouverte est clairement sélectionnée ;
- l’ouverture et la fermeture du détail sont utilisables au clavier ;
- aucun débordement horizontal des actions sur les largeurs supportées.