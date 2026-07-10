# STORY-2201 — Conception technique du contrat d’état financier

## Décision d’architecture

Introduire `InvoiceFinancialStateService` comme source unique de calcul et de synchronisation des états de recouvrement.

Le service centralise :

- l’initialisation idempotente des créances patient/assurance ;
- le calcul du read model de règlement ;
- la traduction de l’état de recouvrement vers `InvoiceStatus` ;
- la reconstruction prudente d’une créance patient manquante à partir des paiements existants ;
- la synchronisation après validation, paiement patient, règlement assurance et avoir.

## Types introduits

### `InvoiceCollectionStatus`

Enum API stable :

- `NOT_YET_DUE`
- `PATIENT_DUE`
- `PATIENT_PARTIALLY_PAID`
- `INSURANCE_DUE`
- `SETTLED`
- `CANCELLED`

La sérialisation JSON reste une chaîne portant le nom de l’enum, donc le contrat HTTP existant reste compatible pour les valeurs déjà exposées.

### `InvoiceStatus.SETTLED`

Ajout du statut persistant terminal indiquant que toutes les obligations sont soldées.

## Flux

### Validation

1. contrôler que la facture est `PENDING` ;
2. enregistrer `VALIDATED`, l’auteur et l’horodatage ;
3. créer les créances manquantes ;
4. synchroniser le statut financier.

### Paiement patient

1. accepter uniquement `VALIDATED` ou `PARTIALLY_PAID` ;
2. contrôler le solde patient restant ;
3. persister paiement, mouvement et reçu ;
4. appliquer le montant à la créance patient ;
5. synchroniser le statut de facture depuis les créances.

### Règlement assurance

1. solder les créances assurance liées au bordereau ;
2. synchroniser chaque facture ;
3. obtenir `SETTLED` uniquement si la part patient est également soldée.

## Compatibilité des données historiques

Lorsqu’une créance manque :

- la créance patient est initialisée avec le cumul des paiements existants, plafonné à la part patient ;
- la créance assurance est initialisée à zéro, sauf pour une facture déjà `SETTLED` ;
- aucune créance existante n’est réécrite silencieusement.

## Sécurité et multi-tenant

- les créances créées héritent explicitement de `organizationId` de la facture ;
- les accès continuent de passer par les repositories soumis au tenant Hibernate ;
- aucun rôle ou endpoint n’est élargi ;
- les transitions s’exécutent dans les transactions des services appelants.

## Impact base de données

Aucune nouvelle colonne ni migration : `invoices.status` est déjà une chaîne suffisamment large pour la valeur `SETTLED`.

## Impact API

- `InvoiceResponse.status` peut désormais valoir `SETTLED` ;
- `InvoiceSettlementSummaryResponse.collectionStatus` devient un enum Java mais conserve la représentation JSON existante ;
- ajout possible de `CANCELLED` dans la synthèse.

## Risques de régression

- écrans TypeScript dont l’union de statuts n’inclut pas `SETTLED` ;
- tests historiques assimilant `PAID` à « facture totalement soldée » ;
- factures historiques validées sans créances ;
- bordereaux qui acceptaient à tort les factures `PENDING`.
