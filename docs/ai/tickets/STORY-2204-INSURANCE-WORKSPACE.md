# STORY-2204 — Poste assurance et progression des bordereaux

## Statut
IN_PROGRESS

## Priorité
P1

## Objectif
Fournir au gestionnaire assurance et à la DAF un workspace dédié permettant de suivre chaque bordereau depuis sa préparation jusqu'à son règlement, y compris les réceptions, acceptations, paiements partiels et rejets.

## Constat
Le parcours actuel est limité à `DRAFT → SENT → PAID`, impose un règlement intégral et n'expose ni montant accepté, ni montant réglé, ni écart, ni motif de rejet.

## Contrat cible
- DRAFT : bordereau préparé, encore modifiable.
- SENT : transmis à l'assureur.
- RECEIVED : accusé de réception enregistré.
- ACCEPTED : accepté par l'assureur, montant accepté connu.
- PARTIALLY_PAID : au moins un règlement reçu, solde restant.
- SETTLED : montant réclamé intégralement réglé.
- REJECTED : rejeté avec motif obligatoire.
- CANCELLED : annulé administrativement.
- PAID : valeur historique tolérée en lecture et présentée comme soldée.

## Critères d'acceptation
- [ ] Vue synthétique par statut et montants réclamés/acceptés/réglés/restants.
- [ ] Filtres par statut, convention et période.
- [ ] Transitions contrôlées côté backend.
- [ ] Référence assureur et motif de rejet persistés.
- [ ] Règlements partiels cumulés sans dépassement.
- [ ] Synchronisation des créances assurance sans double règlement.
- [ ] Isolation tenant et RBAC DAF/ADMIN_CLINIQUE/SECRETAIRE_COMPTABLE.
- [ ] Responsive, light/dark, FR/EN et navigation clavier.
- [ ] Tests backend, Angular, H2 et PostgreSQL 16 verts.

## Hors périmètre
- Télétransmission EDI vers les assureurs.
- Signature électronique.
- Lettrage comptable bancaire automatique.
- Abandon de créance ou remise automatique sur écart accepté.

## Validation

La maintenance E2E aligne les anciens scénarios financiers sur la nouvelle progression assurance, puis restaure le pipeline CI permanent avant la validation complète.
