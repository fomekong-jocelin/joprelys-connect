# FUNCTIONAL-SPEC — Cohérence des parcours, lots 1 à 3 (EPIC-0029)

Décisions : [ADR-20261017](../../architecture/adr/ADR-20261017-journey-coherence-decisions.md). Origine : [audit du 2026-10-17](../functional-e2e-audit-20261017/AUDIT-REPORT.md).

## Lot 1 — Sécurité clinique

| Règle | Qui | Comportement |
|---|---|---|
| Signature de visite | Médecin | Seul le praticien qui a le patient en charge clôture et signe. Sinon : « Patient en consultation chez Dr X. Reprenez la prise en charge depuis la file avant de clôturer. » (FA-01) |
| Délivrance et administration | Pharmacien, IDE | Une ordonnance délivrée (partiellement ou totalement) reste administrable. La pharmacie voit « Partiellement / Entièrement délivrée ». (FA-02) |
| Administration rapprochée | IDE | Un double clic n'enregistre qu'une administration. Une nouvelle dose dans la fenêtre de sécurité est refusée avec l'heure de la précédente ; l'IDE peut la forcer avec un motif, visible dans l'historique (« Administration forcée »). (FA-03) |
| Laboratoire | Biologiste | Les demandes détaillées avancent examen par examen. Le statut global ne sert qu'à annuler, avec motif ; les examens validés sont conservés. (FA-09) |
| Examen de contrôle | Médecin | Un examen redemandé après le début de la demande crée une nouvelle demande. (FA-10) |
| Résultats et portail patient | Biologiste, patient | Le validateur est l'utilisateur connecté. Le patient ne voit et n'est notifié que des résultats validés. (FA-11, partiel) |
| Constantes d'urgence | Accueil, IDE | Seules les constantes d'une urgence en cours sont reprises, à leur heure réelle. (FA-28) |

## Lot 2 — Continuité des épisodes

| Règle | Qui | Comportement |
|---|---|---|
| Orientation aux urgences | Médecin | « Décider l'orientation ». Hospitalisation, bloc et consultation : le patient reste au tableau des urgences, avec « En attente d'aval : … depuis 2 h 10 », et ses soins restent traçables. L'admission (ou la visite de consultation) clôt l'urgence et reprend ses constantes. Sortie et transfert externe clôturent immédiatement. (FA-06) |
| Décès | Médecin | « Déclarer le décès » sur le dossier, ou l'orientation « Décès constaté » aux urgences. Effets : patient décédé, épisodes clos, rendez-vous annulés, nouvelles visites, urgences et réservations refusées, portail fermé. Le séjour reçoit une sortie « décès » et le lit est libéré au départ du corps (statut « Décédé »). Une déclaration erronée est annulable avec motif. (FA-07) |
| Patient parti | Accueil | « Patient parti / annuler la visite », avec un motif, tant qu'aucun médecin n'a pris le patient en charge. Les examens non commencés et les factures en attente sont annulés. (FA-45) |
| Fin d'hospitalisation | Gestionnaire des lits | Le départ physique clôt la visite : le patient peut revenir à J7. Une visite liée à un séjour en cours ne se clôture pas manuellement. (FA-04, FA-05) |
| Décision de sortie | Médecin | Annulable avec motif tant que le patient est présent. (FA-32) |
| Désactivation d'unité | Admin | Refusée tant qu'elle porte des séjours, du personnel ou des espaces actifs ; les compteurs sont affichés. (FA-29) |

## Lot 3 — Facturation

| Règle | Qui | Comportement |
|---|---|---|
| Tarifs | Facturation | Aucun prix inventé. Les lignes sans tarif sont signalées « à tarifer » et bloquent la validation, avec la liste des clés. « Recalculer la facture » après paramétrage. (FA-13) |
| Clés tarifaires | Admin, DAF | `CS`, `ROOM_<NIVEAU>`, `K`, `EXAM:<NOM>` et le nom du médicament, rappelés dans l'onglet Tarifs. |
| Médicaments | — | Facturés sur les quantités délivrées par la pharmacie de l'établissement ; jamais depuis l'ordonnance. (FA-14, FA-15) |
| Examens | — | Les examens internes non annulés sont facturés. La consultation n'est facturée que si une consultation existe. (FA-16, FA-17) |
| Plusieurs factures | Facturation | Facture intermédiaire puis complémentaire : seules les charges non encore facturées sont reprises, et les jours de séjour déjà facturés sont déduits. S'il ne reste rien à facturer : 409. (FA-18) |
| Paiement avant prélèvement | Admin, DAF | Paramètre d'établissement. Une fois activé, les examens ambulatoires attendent le paiement de la part patient (hors urgences et hospitalisation). (D-13) |
| Avoirs | Secrétaire comptable, DAF | Avoir motivé sur une facture validée, plafonné au montant de la facture. Au-delà du seuil (50 000 FCFA par défaut, paramétrable), il attend l'approbation d'une autre personne habilitée (DAF). (FA-19) |
| Remboursement | Caissier | Remboursement d'un avoir actif dans la session de caisse, borné par l'avoir et par les sommes encaissées. |
| Acomptes | Caissier | Encaissement avant facturation, puis imputation sur une facture validée, sans double comptage en caisse. (D-16) |
