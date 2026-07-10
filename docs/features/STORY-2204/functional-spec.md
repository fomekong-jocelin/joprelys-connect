# STORY-2204 — Spécification fonctionnelle du poste assurance

## Utilisateurs
- `SECRETAIRE_COMPTABLE` : prépare, envoie et enregistre la réception d'un bordereau.
- `DAF` : accepte, rejette et enregistre les règlements.
- `ADMIN_CLINIQUE` : supervision complète.

## Parcours nominal
1. Générer un bordereau pour une convention et une période.
2. Vérifier les factures et le montant réclamé.
3. Marquer le bordereau comme envoyé.
4. Enregistrer l'accusé de réception et la référence assureur.
5. Enregistrer l'acceptation et le montant accepté, ou le rejet avec motif.
6. Enregistrer un ou plusieurs règlements.
7. Passer automatiquement à `PARTIALLY_PAID` puis `SETTLED` lorsque le montant réclamé est intégralement réglé.

## Règles
- Les transitions sont séquentielles et contrôlées côté backend.
- Le montant accepté ne peut être négatif ni supérieur au montant réclamé.
- Un règlement est strictement positif et ne peut dépasser le reste à régler.
- Un bordereau rejeté ou annulé ne peut plus être réglé.
- La valeur historique `PAID` est affichée comme soldée mais aucune nouvelle écriture ne la produit.
- Les écarts `réclamé - accepté` sont visibles mais ne sont pas automatiquement abandonnés.
- Les créances assurance sont créditées uniquement du montant réellement encaissé.

## Vue synthétique
Chaque carte ou ligne expose :
- numéro et convention ;
- période ;
- statut métier traduit ;
- montant réclamé ;
- montant accepté ;
- montant réglé ;
- reste à régler ;
- écart contesté ;
- prochaine action autorisée.

## Filtres
- statut ;
- convention ;
- période de création ;
- recherche par numéro ou référence assureur.

## États UX
- chargement ;
- erreur avec nouvelle tentative ;
- vide ;
- succès ou erreur d'action ;
- confirmation pour les transitions terminales.
