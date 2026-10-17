# Plan de recette de bout en bout — parcours patient

Référence : [AUDIT-REPORT.md](AUDIT-REPORT.md). Environnement : base dédiée, données fictives uniquement. Les scripts rejouables se trouvent dans [scenarios/](scenarios/). Chaque étape indique l'acteur, l'action et le **résultat attendu après correction**. Les écarts actuels sont signalés « (actuel : …) ».

## Prérequis

Établissement « Clinique Audit » : pôle Médecine ; services Médecine générale (MG), Médecine interne (MI), Urgences, Laboratoire ; unité de soins UH-MI-A (fille de MI) ; chambres CH-101 (lits 101-A et 101-B) → UH-MI-A et CH-102 (lit 102-A) → MI ; box de consultation. Comptes : accueil, IDE (UH-MI-A), Dr A (MG), Dr B (MI + UH-MI-A), Dr C (sans affectation), biologiste, pharmacien, caissier, gestionnaire des lits, secrétaire comptable, admin clinique. Grille tarifaire : CS, ROOM_STANDARD, Artemether-Lumefantrine, NFS et goutte épaisse.

## R1 — Configuration jusqu'à la première admission

| # | Acteur | Action | Attendu |
|---|---|---|---|
| 1 | Admin plateforme | Crée l'établissement et son administrateur | Mail avec mot de passe temporaire ; connexion OTP |
| 2 | Admin clinique | Crée la structure et les espaces ci-dessus | Refus explicites : service hors catalogue, lit dans un espace sans profil d'hébergement |
| 3 | Admin clinique | Ouvre « Préparation à la mise en service » | Liste des manques : tarifs, affectation espace → unité, médecin affecté par unité |
| 4 | Dr B | Admet avant l'affectation des espaces | 409 « L'unité n'utilise pas cet espace » |
| 5 | Admin clinique | Désactive UH-MI-A pendant qu'un patient y séjourne | 409 listant le séjour (actuel : 200) |

## R2 — Patient ambulatoire sans rendez-vous, examens et retour au médecin

| # | Acteur | Action | Attendu |
|---|---|---|---|
| 1 | Accueil | Crée « Mbarga Fictif Paul », puis « MBARGA Fictif Paul » (même date et téléphone) | 2e création : 409 `POTENTIAL_DUPLICATE` avec le candidat (actuel : créé) |
| 2 | Accueil | Ouvre une visite MG avec le pharmacien comme praticien | 400 (actuel : 200) |
| 3 | Accueil | Ouvre une visite MG, praticien Dr A | Visite `ATTENTE_CONSTANTES` dans la file |
| 4 | IDE | Saisit T 39,2 °C, FC 112 | `PRET_MEDECIN`, alertes fièvre et tachycardie |
| 5 | Dr A puis Dr B | Prise en charge | Dr A OK ; Dr B 409 |
| 6 | Dr A | Consultation, demande GE + NFS | 1 demande, 2 examens, file labo |
| 7 | Biologiste | PATCH global → VALIDATED | 409 (actuel : 200) |
| 8 | Biologiste | GE : résultat critique validé | Tâche « Résultat critique à voir » chez Dr A ; patient non notifié avant la règle Q-01 (actuel : patient notifié, même en brouillon) |
| 9 | Dr A | Acquitte le résultat ; demande une NFS de contrôle | Nouvelle demande distincte (actuel : fusion silencieuse) |
| 10 | Dr B | Clôture la visite de Dr A | 409 (actuel : 200, signataire = Dr B) |
| 11 | Dr A | Clôture | Récapitulatif « 1 examen en attente » ; consultation signée par Dr A |
| 12 | Facturation | Pré-calcule la facture | Consultation (CS), NFS, GE ; médicament = 1 boîte au tarif de la grille, seulement si délivré en interne (actuel : 124 × 2 500) |
| 13 | Caissier | Encaisse avant validation, puis après | Avant : 400 ; après : `PARTIALLY_PAID` |
| 14 | Pharmacien | Délivre sans revue, puis après revue | 409, puis 204 ; stock décrémenté (actuel : inchangé) |
| 15 | Secrétaire comptable | Émet un avoir et rembourse | Avoir accepté, mouvement de caisse lié (actuel : 403) |

## R3 — Rendez-vous

| # | Acteur | Action | Attendu |
|---|---|---|---|
| 1 | Dr A | Déclare des disponibilités | Créneaux visibles |
| 2 | Patient (portail) ou accueil | Réserve un créneau | `CONFIRMED` ; l'accueil peut réserver (actuel : impossible) |
| 3 | Accueil | Rendez-vous du jour → « Patient arrivé » | Visite créée, rendez-vous `ARRIVÉ` lié |
| 4 | Système / accueil | Patient absent à la fin de la journée | `NO_SHOW` |
| 5 | Dr A | Clôture la visite | Rendez-vous `COMPLETED` |

## R4 — Hospitalisation, concurrence sur un lit, transfert, sortie

| # | Acteur | Action | Attendu |
|---|---|---|---|
| 1 | Dr B | Demande d'hospitalisation UH-MI-A pour Claire et Jean | 2 demandes `EN_ATTENTE_LIT` dans la file du gestionnaire |
| 2 | Gestionnaire des lits ×2 | Réservent 101-A simultanément | Une réservation ; l'autre 409 ; la demande perdante reste en file (actuel : aucune file) |
| 3 | IDE | « Patient arrivé dans l'unité » | Séjour `EN_COURS`, lit occupé |
| 4 | Gestionnaire des lits | Met le lit occupé en maintenance | 409 |
| 5 | IDE | Demande un transfert vers CH-102 (MI) avec motif | Tâche d'acceptation pour MI ; à l'acceptation, choix du nouveau responsable (actuel : transfert immédiat sans motif ni changement de responsable) |
| 6 | Dr B | Prescrit la Ceftriaxone 1 g/24 h | Plan d'administration généré |
| 7 | IDE | Administre, puis re-clique immédiatement | 1 administration ; la 2e → 409 avec l'heure (actuel : 2 lignes) |
| 8 | Pharmacien | Délivre 3 flacons au service | Le traitement reste administrable (actuel : disparu) |
| 9 | Dr B | Décide la sortie, puis annule (aggravation) | Annulation acceptée avec motif (actuel : 409) |
| 10 | Dr B / gestionnaire des lits | Décision puis départ physique | Visite `TERMINEE` ; lit `NETTOYAGE` (actuel : visite `EN_COURS`) |
| 11 | Accueil | Visite de contrôle à J7 | 201 (actuel : 400) |
| 12 | Agent d'hygiène | Nettoyage terminé | Lit `PRÊT` ; l'IDE est refusé |

## R5 — Urgence avec patient inconnu puis rapprochement

| # | Acteur | Action | Attendu |
|---|---|---|---|
| 1 | IDE | Urgence provisoire P1 (double clic) | Une seule urgence `URG-TEMP-…` |
| 2 | IDE | Gestes de réanimation | Tracés avec auteur |
| 3 | Dr A | Décide l'orientation hospitalisation MI | `ORIENTATION_DECIDEE` ; reste dans le tableau « en attente de lit » ; soins toujours traçables (actuel : disparaît, 409) |
| 4 | Gestionnaire des lits | Réserve un lit ; IDE confirme l'arrivée | Séjour créé ; urgence `SORTIE_URGENCES` dans la même transaction |
| 5 | Accueil / admin | Rapproche l'identité définitive | Alias conservé ; factures et séjour rattachés au dossier canonique |
| 6 | Facturation | Facture sur identité provisoire | `REGULARIZATION_PENDING`, sans blocage des soins |

## R6 — Décès

| # | Acteur | Action | Attendu |
|---|---|---|---|
| 1 | Dr A | Constate le décès (date, heure, circonstances) | Patient `DÉCÉDÉ` ; urgence, visite et séjour clos ; rendez-vous annulés |
| 2 | Accueil | Tente d'ouvrir une visite | 409 (actuel : 201) |

## R7 — Fin de parcours anormale

| # | Acteur | Action | Attendu |
|---|---|---|---|
| 1 | Accueil | Patient parti avant les constantes | « Patient parti » avec motif ; visite `ANNULEE` ; la visite suivante est possible |
| 2 | Accueil | Patient qui refuse un examen | Examen `CANCELLED` (motif : refus) par le médecin ; pas de facturation |
| 3 | Urgence | Clôture d'une urgence, puis visite programmée 3 jours plus tard | Aucune constante recopiée (actuel : TA 190/110 recopiée avec alerte critique) |

## R8 — Droits et séparation des tâches

| # | Acteur | Action | Attendu |
|---|---|---|---|
| 1 | Admin clinique | Décision de sortie médicale | 403 (actuel : 200) — selon Q-04 |
| 2 | Accueil | Saisie des constantes vitales | 403 — selon Q-05 (actuel : 200) |
| 3 | Médecin | Accès aux tâches et alertes | 200 (actuel : 403) |

## Non couverts, à ajouter avant la recette finale

Mineur avec représentant légal (Q-09) ; portail patient (résultats, consentements, accès externe Connect) avec OTP envoyé par e-mail ; imagerie et bloc si retenus (Q-10) ; clôture de caisse avec écart ; bordereaux d'assurance ; parcours complets dans le navigateur FR/EN, clair/sombre, à 390 px.
