# STORY-2204 — Poste assurance et progression des bordereaux

## Statut
DONE

## Priorité
P1

## Objectif
Fournir au gestionnaire assurance et à la DAF un workspace dédié permettant de suivre chaque bordereau depuis sa préparation jusqu'à son règlement, y compris les réceptions, acceptations, paiements partiels et rejets.

## Contrat livré
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
- [x] Vue synthétique par statut et montants réclamés/acceptés/réglés/restants.
- [x] Filtres par statut, convention et période.
- [x] Transitions contrôlées côté backend.
- [x] Référence assureur et motif de rejet persistés.
- [x] Règlements partiels cumulés sans dépassement.
- [x] Synchronisation des créances assurance sans double règlement.
- [x] Isolation tenant et RBAC DAF/ADMIN_CLINIQUE/SECRETAIRE_COMPTABLE.
- [x] Responsive, light/dark, FR/EN et navigation clavier.
- [x] Tests backend, Angular, H2 et PostgreSQL 16 verts.

## Validation technique
- Maven `clean verify` : réussi.
- Migrations Flyway sur H2 : réussies.
- Migrations PostgreSQL 16 via Testcontainers : réussies.
- Tests backend des transitions et paiements partiels : réussis.
- Tests Angular : réussis.
- Build Angular de production : réussi.
- Pipeline permanent validé avant fusion.

## Livraison
- Pull Request : #19
- Commit de fusion : `83b4bbfeba31ccd8c9ebdb0903c3b9439b721cfb`
- Date de fusion : 2026-07-10
- Impact SemVer : MINOR

## Risques résiduels
- Validation métier DAF/Product des libellés et de la progression opérationnelle en environnement de recette.
- L'écart entre montant réclamé et montant accepté reste visible et n'est pas abandonné automatiquement.
- La télétransmission EDI et le lettrage bancaire automatique restent hors périmètre.

## Hors périmètre
- Télétransmission EDI vers les assureurs.
- Signature électronique.
- Lettrage comptable bancaire automatique.
- Abandon de créance ou remise automatique sur écart accepté.

## Reste à faire
Aucun correctif technique ouvert sur cette story. Le suivi post-fusion relève de la QA métier globale `STORY-2206`.
