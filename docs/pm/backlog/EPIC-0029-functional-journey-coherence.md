# EPIC-0029 — Cohérence fonctionnelle des parcours patient de bout en bout

- **Origine** : [AUDIT-20261017](../../ai/tickets/AUDIT-20261017-FUNCTIONAL-END-TO-END-JOURNEYS.md) — [rapport](../../features/functional-e2e-audit-20261017/AUDIT-REPORT.md)
- **Statut** : lots 1 à 3 READY_FOR_REVIEW ([STORY-20261017](../../ai/tickets/STORY-20261017-EPIC-0029-LOTS-1-3.md), décisions [ADR-20261017](../../architecture/adr/ADR-20261017-journey-coherence-decisions.md)) ; lots 4 et 5 à planifier.
- **Objectif** : que chaque transmission entre intervenants ait un responsable, un statut et une preuve, et qu'aucun épisode (visite, urgence, séjour, facture) ne reste bloqué ou ne disparaisse.
- **Estimation** : 117 SP, environ 3 à 4 sprints selon `docs/pm/ESTIMATION-GUIDE.md`. Les stories de 8 SP ou plus sont à découper avant READY.
- **Reviewers transverses** : médecin référent, cadre infirmier, pharmacien, DAF, DPO, QA senior.
- **SemVer** : MINOR minimum ; S02 et S16 sont des candidats MAJOR (contrats API).

## Définition de prêt (commune)

FUNCTIONAL-SPEC et TECHNICAL-DESIGN créés dans `docs/features/<story>/` ; questions métier associées tranchées ; scénario de recette rédigé dans le TEST-PLAN de l'audit.

## Définition de fini (commune)

Tests backend (`./mvnw test`) couvrant les critères ; tests Angular des écrans modifiés ; scénario API du TEST-PLAN rejoué vert ; i18n FR/EN ; documentation, CHANGELOG et PROJECT-TRACKING à jour.

---

## Lot 1 — Sécurité clinique immédiate (21 SP)

### S01 — Signature par le médecin en charge (FA-01) — 3 SP — P0
- **Règle** : `closeVisit` exige `actor == consultingPractitionerId`, sinon 409. La reprise explicite (`take-charge` avec `takeOver`) est la seule voie pour un autre médecin.
- **Critères d'acceptation** : Dr B sans reprise → 409 ; avec reprise → signature Dr B + audit `VISIT_CONSULTATION_TAKEN_OVER` ; l'auteur de la consultation n'est pas réécrit.
- **Tests** : `VisitControllerTest` (clôture par un tiers), `VisitCareFlowControllerTest`.
- **Profil** : intermédiaire. **Dépendances** : aucune.

### S02 — Séparer le statut d'ordonnance et le statut de délivrance (FA-02) — 5 SP — P0
- **Règle** : `prescriptions.status` ∈ {DRAFT, ACTIVE, CANCELLED, EXPIRED} ; nouveau `dispensation_status` ∈ {NONE, PARTIAL, FULL}. L'administration est éligible si l'ordonnance est ACTIVE et non expirée.
- **Migration** : `PARTIALLY_DISPENSED` / `FULLY_DISPENSED` → ACTIVE + `dispensation_status`.
- **Critères d'acceptation** : MED-05 = 1 ligne après délivrance ; la vérification pharmacie affiche les quantités cumulées ; une ordonnance entièrement délivrée n'est plus délivrable, mais reste administrable.
- **Tests** : `PharmacyServiceTest`, `HospitalizationMedicationPolicyTest`, test de migration.
- **Profil** : senior. **Dépendances** : Q-07.

### S03 — Contrôle des administrations (FA-03) — 5 SP — P0
- **Règle** : clé d'idempotence client ; 409 si une administration de la même ligne d'ordonnance date de moins de l'intervalle minimal (paramètre provisoire : 1 h tant que Q-11 n'est pas tranchée) ; forçage avec motif obligatoire, tracé.
- **Critères d'acceptation** : un double clic produit 1 ligne ; une 2e administration immédiate → 409 avec l'heure de la précédente ; un forçage avec motif → 201 + audit.
- **Profil** : senior. **Dépendances** : Q-11 (plan d'administration complet : story ultérieure).

### S04 — Transitions de laboratoire verrouillées (FA-09, FA-08 labo) — 3 SP — P0
- **Règle** : `PATCH /lab-orders/{id}/status` limité à CANCELLED avec motif ; progression uniquement par item ; validateur = utilisateur connecté.
- **Critères d'acceptation** : OP-16 et OP-17 → 409 ; audit d'upload avec `userId`.
- **Profil** : intermédiaire.

### S05 — Propagation des constantes d'urgence (FA-28) — 5 SP — P0
- **Règle** : recopie uniquement si la visite est créée depuis une urgence **active** (paramètre explicite `emergencyId`) ; horodatage et auteur d'origine conservés.
- **Critères d'acceptation** : URG-12 → aucune constante ; une visite issue d'une urgence active → mesures avec `recordedAt` d'origine.
- **Profil** : intermédiaire.

## Lot 2 — Continuité des épisodes (26 SP)

### S06 — Fin de visite cohérente (FA-04, FA-05, FA-45) — 5 SP — P1
- **Règles** : le départ physique clôt la visite liée ; clôture de visite refusée si un séjour est actif ; action accueil « Patient parti / annuler » avant toute donnée clinique, motif obligatoire, annulation en cascade des demandes non commencées et des factures `PENDING`.
- **Critères d'acceptation** : DIS-09 `TERMINEE` ; DIS-10 201 ; HOS-07 409 ; l'annulation par l'accueil est tracée.
- **Profil** : intermédiaire.

### S07 — Orientation d'urgence et attente d'aval (FA-06, FA-08) — 8 SP — P0 (à découper : back / front)
- **Statuts** : `EN_COURS` → `ORIENTATION_DECIDEE` (destination structurée) → `SORTIE_URGENCES` (déclenchée par l'admission, le départ ou le transfert).
- **Critères d'acceptation** : URG-07 et URG-08 verts ; admission depuis l'urgence = clôture atomique ; tableau « en attente de lit » avec durée ; `orientedBy` et `closedBy` renseignés.
- **Dépendances** : S18 pour la réservation.

### S08 — Décès (FA-07) — 5 SP — P0
- **Règles** : événement de décès par un médecin ; patient `DECEASED` ; clôture des visites et séjours ; rendez-vous annulés ; nouvelles visites refusées.
- **Critères d'acceptation** : URG-10 et URG-11 verts. **Dépendances** : Q-03.

### S09 — Annulation de la décision de sortie (FA-32) — 3 SP — P1
- **Critères d'acceptation** : annulation avec motif avant le départ physique ; historique conservé ; après le départ → 409.

### S10 — Désactivation d'unité protégée (FA-29) — 5 SP — P1
- **Critères d'acceptation** : CFG-34 → 409 listant les séjours, visites et affectations en cours.

## Lot 3 — Facturation fiable (18 SP)

### S11 — Tarifs obligatoires et quantités structurées (FA-13, FA-14, FA-15) — 8 SP — P0 (à découper)
- **Règles** : aucune valeur par défaut ; ligne « à tarifer » bloquant la validation ; quantité structurée sur la ligne d'ordonnance ; médicaments facturés sur la délivrance interne (Q-07).
- **Critères d'acceptation** : BIL-03 = 1 boîte ; sans grille, la validation est refusée avec la liste des tarifs manquants.

### S12 — Facturation des examens et garde de paiement paramétrable (FA-16, FA-17) — 5 SP — P1
- **Critères d'acceptation** : BIL-02 vert ; avec le paramètre « paiement avant prélèvement », la demande est `AWAITING_PAYMENT` jusqu'au paiement (hors urgence et séjour) ; consultation facturée seulement si une consultation est signée.

### S13 — Corrections financières et remboursement (FA-19) — 5 SP — P1
- **Critères d'acceptation** : la secrétaire comptable émet un avoir (BIL-10 vert) ; validation DAF au-delà d'un seuil ; un remboursement crée un mouvement de caisse lié et un reçu.

## Lot 4 — Transmissions (21 SP)

### S14 — Tâches et alertes du personnel (FA-36) — 8 SP — P1 (à découper)
- **Modèle** : tâche {type, priorité, destinataire utilisateur ou rôle@unité, objet lié, statut, prise en charge par, échéance}.
- **Critères d'acceptation** : badge dans le menu ; prise en charge exclusive ; historique.

### S15 — Corbeille de résultats et politique de publication (FA-11, FA-12) — 5 SP — P0
- **Critères d'acceptation** : le brouillon n'est ni visible ni notifié au patient ; tâche « Résultat à voir » chez le prescripteur ; résultat critique escaladé ; accusé de prise en compte. **Dépendances** : S14, Q-01.

### S16 — Visite rattachée à une unité (FA-24, FA-25, FA-31) — 8 SP — P1 (MAJOR candidat)
- **Critères d'acceptation** : OP-04 et OP-06 → 400 ; `serviceUnitId` obligatoire ; règle d'héritage service → unités unique ; migration des libellés existants vers les unités (rapport de réconciliation).

## Lot 5 — Évolutions de parcours (31 SP, dont S18 à découper)

- **S17 — Rendez-vous → arrivée → visite (FA-23)** — 8 SP — P1 : rendez-vous du jour, arrivée, `NO_SHOW`, réservation par l'accueil. Dépend de Q-08.
- **S18 — Demande d'hospitalisation et réservation de lit (FA-34)** — 13 SP — P1 : **à découper** (demande, réservation, arrivée dans l'unité).
- **S19 — Transfert accepté et changement de responsable (FA-30)** — 5 SP — P1.
- **S20 — Doublons à la création et signalement par l'accueil (FA-26)** — 3 SP — P1.
- **S21 — Recentrage du rôle ADMIN_CLINIQUE (FA-37)** — 2 SP + communication aux clients — P1. Dépend de Q-04.

## Hors epic (à planifier séparément)

FA-38 stock et délivrance (GAP-026), FA-39 durcissement de la vérification publique d'ordonnance, FA-40 imagerie, FA-41 bloc, FA-42 mineurs, FA-21 mobile money, FA-22 assurance avancée, FA-43 erreurs 400, FA-44 catalogue.
