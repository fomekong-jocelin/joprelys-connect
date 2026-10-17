# ADR — Décisions métier pour la cohérence des parcours (EPIC-0029, lots 1 à 3)

Date : 2026-10-17. Statut : accepté — l'utilisateur a délégué les décisions des sections 1, 2 et 3 du plan de correction (« comme c'est fait dans les meilleurs centres hospitaliers ou les meilleures applications »). Une revue clinique, financière et juridique reste requise avant mise en production.

Référence : [AUDIT-20261017](../../features/functional-e2e-audit-20261017/AUDIT-REPORT.md), [EPIC-0029](../../pm/backlog/EPIC-0029-functional-journey-coherence.md).

## Principes retenus

Ce sont les pratiques des DPI de référence (Epic, Oracle Health/Cerner, DPI hospitaliers européens) et des standards de sécurité des soins (ISMP, recommandations sur les « 5 B » de l'administration, tableaux de suivi des urgences) :

1. **Un acte clinique est signé par son auteur.** Un tiers doit reprendre explicitement la prise en charge.
2. **La prescription, la délivrance et l'administration sont trois états distincts.** Le dossier d'administration (MAR) se lit depuis la prescription active, jamais depuis l'état de délivrance.
3. **Le patient reste sous la responsabilité d'un service tant que l'aval ne l'a pas accepté** (« boarding » aux urgences).
4. **Chaque charge facturable a une source unique**, facturée une seule fois. Rien n'est facturé à un prix inventé.

## Décisions — sécurité clinique (section 1)

| ID | Décision |
|---|---|
| D-01 | La clôture d'une visite exige que l'utilisateur soit le praticien qui a le patient en charge. Sinon 409, avec invitation à reprendre la prise en charge (reprise tracée existante). Une visite sans prise en charge est prise en charge par le clôtureur dans la même transaction. |
| D-02 | `prescriptions.status` ∈ {DRAFT, ACTIVE, CANCELLED, EXPIRED} ; nouvelle colonne `dispensation_status` ∈ {NONE, PARTIAL, FULL}. Une ordonnance ACTIVE reste administrable quelle que soit sa délivrance. Les valeurs historiques `PARTIALLY_DISPENSED` / `FULLY_DISPENSED` sont migrées. |
| D-03 | Administration : la clé d'idempotence client rend un double clic sans effet. Une administration de la même ligne dans la **fenêtre de sécurité** est refusée (409). La fenêtre vaut l'intervalle structuré de la ligne (`intervalHours`) moins 1 heure (règle usuelle de fenêtre ± 1 h), ou **1 heure** si l'intervalle est inconnu (minimum 30 min). L'intervalle est déduit des formes non ambiguës de la fréquence ou de la posologie (« toutes les 8 h », « /24h », « 3 fois par jour »). Un forçage est possible avec un motif obligatoire, tracé (`MEDICATION_ADMINISTRATION_OVERRIDE`). |
| D-04 | Laboratoire : la progression se fait uniquement examen par examen, avec les transitions existantes. Le changement de statut global sert uniquement à l'annulation, avec motif obligatoire. Les examens validés ne sont pas annulés. Le validateur d'un résultat est l'utilisateur connecté quand l'appel est authentifié. |
| D-05 | Les constantes d'une urgence ne sont reprises dans une visite que si l'urgence est **encore active** (ni clôturée ni rattachée à une autre visite). Elles conservent l'horodatage de la mesure d'origine. |

## Décisions — continuité des épisodes (section 2)

| ID | Décision |
|---|---|
| D-06 | Urgences : modèle « décision d'orientation / sortie des urgences ». `ADMISSION`, `OR_DIRECT` et `CONSULTATION` laissent l'urgence **active** en attente d'aval (boarding, soins toujours traçables, durée affichée). La création de l'hospitalisation, ou de la visite de consultation, depuis l'urgence la clôture dans la même transaction et reprend ses constantes. `DISCHARGE` et `TRANSFER` clôturent immédiatement. `DEATH` déclenche la déclaration de décès (D-07). L'auteur de la décision et de la clôture est enregistré. L'orientation peut être révisée tant que l'urgence est active. |
| D-07 | Décès : déclaré par un médecin (`PATIENT_DEATH_DECLARE`), avec date et heure, lieu et circonstances. Effets : patient `DECEASED` ; urgence active clôturée ; visites actives clôturées sans signer les brouillons d'autrui ; rendez-vous futurs annulés (motif « décès ») ; décision de sortie « décès » sur le séjour actif — le lit est libéré au départ du corps, confirmé par le circuit de départ physique, statut final `DECEDE`. Une nouvelle visite, urgence ou réservation est refusée. Une déclaration erronée est révocable par un médecin, avec motif, sans rouvrir les épisodes clôturés. |
| D-08 | Fin de visite : le départ physique clôture la visite liée au séjour. La clôture manuelle d'une visite liée à un séjour actif est refusée. L'accueil (`VISIT_CREATE`) peut **abandonner** une visite tant qu'aucune consultation n'existe. Motifs : `LEFT_WITHOUT_BEING_SEEN`, `REGISTRATION_ERROR`, `PATIENT_REFUSED`. Effets en cascade : demandes d'examens sans prélèvement annulées, factures `PENDING` annulées, audit. |
| D-09 | La décision médicale de sortie est annulable par un médecin, avec motif, tant que le départ physique n'est pas confirmé. Les valeurs annulées sont conservées dans l'audit. |
| D-10 | La désactivation d'une unité est refusée tant qu'elle porte un séjour actif, une affectation de personnel active ou une affectation d'espace active. Le message donne les compteurs. |

## Décisions — facturation (section 3)

| ID | Décision |
|---|---|
| D-11 | Aucun prix par défaut. Une ligne sans tarif est créée à 0 avec `tariff_missing = true`. Une facture qui contient une telle ligne ne peut pas être validée ; le message liste les tarifs manquants. Une facture `PENDING` peut être **recalculée** après paramétrage. |
| D-12 | Médicaments : facturés sur ce que l'établissement a **réellement fourni**. En ambulatoire, ce sont les délivrances de la pharmacie de l'établissement, sur les quantités entières délivrées. En hospitalisation, ce sont ces mêmes délivrances, plus les consommables déjà tracés. Une ordonnance servie à l'extérieur n'est jamais facturée. L'administration n'est pas facturée en plus de la délivrance. |
| D-13 | Les examens réalisés par le laboratoire de l'établissement sont facturés (`LAB_EXAM`, `IMAGING_EXAM`) au tarif de l'examen ; les examens annulés ne le sont pas. Paramètre d'établissement `labPaymentBeforeSampling` (désactivé par défaut). Activé, il place les demandes ambulatoires en `AWAITING_PAYMENT`, hors urgence et hors hospitalisation. Le paiement intégral de la part patient de la facture qui porte ces examens les libère (`PAID`). |
| D-14 | La consultation n'est facturée que si une consultation existe sur la visite. |
| D-15 | Plusieurs factures par épisode : chaque ligne porte sa **source** (`source_type`, `source_id`). Une source déjà portée par une facture non annulée n'est pas refacturée. Les jours de séjour déjà facturés sont déduits : facture intermédiaire puis facture finale. |
| D-16 | Acomptes : encaissés en caisse (session ouverte) sur un patient, éventuellement liés à un épisode. Ils sont imputés ensuite sur une facture validée (paiement de mode `DEPOSIT`, non recompté en caisse). |
| D-17 | Avoirs : émis par la facturation (`BILLING_CREDIT_NOTE_ISSUE` : secrétaire comptable, DAF, admin). Au-delà du seuil d'établissement (50 000 XAF par défaut), l'avoir reste `PENDING_APPROVAL` jusqu'à l'approbation par la DAF (`BILLING_CREDIT_NOTE_APPROVE`). Le remboursement d'un avoir actif est fait par le caissier, dans sa session : mouvement de caisse sortant lié, borné par le montant de l'avoir et par le montant encaissé. |
| D-18 | Le caissier reçoit `PATIENT_READ` (lecture administrative seule) pour retrouver un patient et encaisser son acompte avant toute facture (D-16). Aucun accès clinique n'est ajouté. |

## Conséquences

- Contrats additifs, sauf la valeur `status` des ordonnances délivrées (D-02), qui n'expose plus `*_DISPENSED` : bump **MAJOR** candidat. Aucune version n'est préparée.
- Migrations Flyway additives, avec reprise des données (D-02).
- Les rôles personnalisés existants n'obtiennent pas automatiquement les nouvelles permissions ; les rôles système sont mis à jour.
- Différés à d'autres lots (EPIC-0029, lots 4 et 5) : corbeille de résultats et notifications du personnel (D-04 ne publie rien au prescripteur), plan d'administration complet, demande de lit et réservation, rendez-vous → arrivée.
