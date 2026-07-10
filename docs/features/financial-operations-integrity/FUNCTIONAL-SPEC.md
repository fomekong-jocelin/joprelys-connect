# Spécification fonctionnelle - Intégrité financière et postes métier

## Source de vérité

Une facture représente le document de vente. Les créances représentent les montants exigibles par débiteur. Un paiement patient solde exclusivement la créance patient. Un règlement de bordereau solde exclusivement les créances assurance. La caisse ne comprend que les encaissements et sorties réellement manipulés par le caissier.

## États visibles attendus

| Situation | Facture | Créance patient | Créance assurance |
|---|---|---|---|
| Facture émise non réglée | À encaisser | Non réglée | Non réglée si tiers payant |
| Part patient réglée, assurance en attente | Part patient réglée - assurance à recouvrer | Réglée | Non réglée |
| Paiement patient partiel | Paiement patient partiel | Partiellement réglée | Non réglée si tiers payant |
| Bordereau assurance réglé | Soldée | Réglée | Réglée |

## Postes de travail cibles

### Caissier

Recherche patient ou facture, vérification des montants patient exigibles, encaissement, rendu de reçu, opérations de caisse et clôture.

### Recouvrement

Liste de travail par débiteur, échéance, ancienneté, montant restant, dernière action et action de relance documentée. Les lignes déjà réglées ne sont pas proposées à la relance.

### DAF

Vue de contrôle: caisse par session, encaissements par moyen, dépôts banque, créances patient/assurance, bordereaux à envoyer/régler et écarts de caisse.

## UX et accessibilité

- Un statut doit préciser le débiteur concerné; « Payée » seul est interdit sur une facture tiers payant non soldée.
- Les montants monétaires utilisent une colonne alignée à droite et une unité cohérente.
- Les identifiants techniques ne remplacent pas le nom du patient, de l'assureur ou du débiteur.
- Les relances et opérations sensibles nécessitent une confirmation et un retour d'état non basé uniquement sur la couleur.
- Les vues doivent rester utilisables en thème clair/sombre, FR/EN, et sur écran 1280px comme mobile.

## Validation DAF et règles métier détaillées

Pour le détail des règles d'écart de caisse, de balance âgée pour le recouvrement, et le plan de compte OHADA pour les imputations comptables, se référer au document de cadrage [DAF-VALIDATION.md](file:///C:/MES-APPLICATIONS/joprelys-connect/docs/features/financial-operations-integrity/DAF-VALIDATION.md).

