# Relecture clinique — AUDIT-20261104

Date : 2026-11-04. Relecture technique et clinique faite par Claude Code à la demande du porteur du projet. Portée : décisions D-01 à D-07 d'[ADR-20261104](../../architecture/adr/ADR-20261104-audit-security-clinical-hardening.md), messages envoyés au patient, affichage des interprétations de biologie (portail patient, dossier côté personnel, mobile).

> **Statut** : cette relecture ne remplace pas la validation formelle d'un médecin référent et du pharmacien responsable. Les questions Q-R1 à Q-R6 attendent leur décision. Aucune posologie, interaction ou valeur seuil n'a été inventée : les paramètres cliniques restent configurables et signalés « à valider ».

## 1. Avis sur les décisions de l'ADR

| Décision | Avis | Point d'attention |
|---|---|---|
| D-01 — actes réservés au médecin | **Conforme** pour la décision de sortie et le constat de décès : le certificat de décès est un acte médical. | **Consultation** : dans beaucoup de formations sanitaires de premier niveau, des infirmiers et des sages-femmes font des consultations curatives et prénatales selon des protocoles nationaux. Réserver la consultation au rôle `MEDECIN` bloque ce fonctionnement. L'application n'a pas de rôle sage-femme aujourd'hui. Voir Q-R1. |
| D-02 — ordonnance du praticien en charge | **Conforme.** La reprise explicite d'un patient par un confrère existe (`startConsultation?takeOver=true`). La garde et la relève restent donc possibles, et elles sont tracées. | Le pharmacien peut refuser une délivrance (validation pharmaceutique) sans annuler l'ordonnance : ce verrou de sécurité est conservé. |
| D-03 — patient limité au validé | **Conforme.** Seuls les consultations signées et les résultats validés (dernière version) sont visibles. | Faut-il **différer** un résultat critique au patient jusqu'à ce que le médecin l'ait vu ? C'est une décision de gouvernance clinique (Q-R3). Avec A4, le prescripteur reçoit le résultat au même moment que le patient. |
| D-04 — aucun soin après le décès | **Conforme** pour les médicaments, la délivrance et l'administration. La tolérance de 5 minutes dans le futur couvre les décalages d'horloge. | **Prélèvements post-mortem** (médico-légaux, surveillance des maladies à potentiel épidémique) : le refus de toute demande d'examen après le décès les bloque aussi. Voir Q-R2. |
| D-05 — examen et visite du même patient | Conforme. | — |
| D-06 / D-07 | Hors champ clinique. | — |

## 2. Constats corrigés pendant la relecture

| ID | Gravité | Constat | Correction |
|---|---|---|---|
| RC-01 | P0 | **Dossier patient côté personnel** : la couleur des résultats reposait sur des codes français (`ELEVE`, `CRITIQUE`, `BAS`) que le système ne produit jamais. Un résultat **critique** s'affichait donc en vert « normal », avec son code brut. | Helper partagé `web/src/app/shared/lab/lab-interpretation.ts`. Critique en rouge, hors valeurs de référence en orange, non interprété en neutre ; libellé traduit. |
| RC-02 | P0 | Une interprétation **absente était enregistrée « NORMAL »**, dans l'entité comme dans l'import FHIR. | Enum `LabInterpretation` : une interprétation absente devient `NOT_INTERPRETED`, jamais normale. Les résultats enregistrés avant ce correctif ne permettent pas de distinguer un vrai « normal » d'une valeur absente. |
| RC-03 | P1 | **Portail patient** : toute valeur inconnue (`ABNORMAL`, texte libre d'un partenaire) s'affichait « Normal », en vert. | Même helper. Une valeur inconnue s'affiche « Non interprété », en neutre. |
| RC-04 | P1 | La notification graduée ne reconnaissait que `ABNORMAL`. Or le formulaire de saisie envoie `LOW` ou `HIGH` : le patient recevait le message standard. | `LOW`, `HIGH` et `ABNORMAL` déclenchent le message « hors valeurs de référence ». |
| RC-05 | P1 | La saisie labo **présélectionnait « Normal »**. | Aucune présélection : l'interprétation est obligatoire et choisie explicitement. Options « Hors valeurs de référence » et « Non interprété » ajoutées. |
| RC-06 | P1 | L'import FHIR lisait le texte libre de l'interprétation (par exemple « ELEVE ») et le stockait tel quel. | Les codes HL7 v3 `ObservationInterpretation` sont lus : N, L/LU, H/HU, A, LL/HH/AA. Un code non listé reste non interprété. L'API JSON partenaire refuse désormais une interprétation inconnue (400) : **changement de contrat** pour les partenaires. |

Tests : `LabInterpretationTest` (3), `FhirDiagnosticReportParserTest` (+1), `LabIntegrationControllerTest` (+1 et assertion `HIGH`), `lab-interpretation.spec.ts` (2), `lab-orders-page.spec.ts` (+1).

## 3. Messages envoyés au patient

Les notifications sont aujourd'hui **internes à l'application** : la passerelle SMS/e-mail est simulée.

| Cas | Message après relecture | Justification |
|---|---|---|
| Valeur critique validée | « Vos résultats d'analyses pour la demande … sont disponibles dans votre espace patient. Une valeur doit être revue rapidement avec un médecin : contactez votre médecin ou l'établissement. En cas de malaise ou de symptômes inquiétants, rendez-vous sans attendre aux urgences. » | Consigne d'action claire, sans valeur ni diagnostic. Une fois la corbeille du prescripteur (A4) intégrée, ajouter : « Le médecin prescripteur en a été informé. » Cette phrase n'est vraie qu'à cette condition. |
| Valeur hors valeurs de référence | « … Certaines valeurs sont en dehors des valeurs de référence du laboratoire. Une valeur hors de ces limites n'est pas forcément anormale pour vous : parlez-en avec votre médecin. » | Les valeurs de référence sont calculées sur une population en bonne santé ; une partie des personnes en bonne santé en sort. Le message réduit l'inquiétude sans banaliser. |
| Résultats normaux ou non interprétés | « Vos résultats d'analyses pour la demande … sont disponibles dans votre espace patient. » | Information neutre. |

**Exigence pour un futur canal SMS ou WhatsApp** : envoyer un texte neutre (« Vous avez un nouveau message de votre établissement dans votre espace Joprelys »), sans mention de résultat ni d'urgence. Les détails restent dans l'espace authentifié, car le téléphone est souvent partagé et le message s'affiche sur l'écran verrouillé.

## 4. Recommandations non appliquées

- **Saisie labo** : le statut proposé par défaut est « Validé ». La boîte de confirmation demande un geste volontaire, mais le brouillon devrait être le défaut quand la permission de validation biologique distincte existera (A5).
- **Mobile** : les codes d'interprétation s'affichent bruts (`HIGH`, `NOT_INTERPRETED`). Ce n'est pas trompeur (le critique est en rouge, le reste en neutre), mais il faut les traduire (C7).
- **Import FHIR** : un analyte absent devient « Inconnu » et une valeur absente « N/A ». À traiter avec A5.

## 5. Questions au médecin référent

| ID | Question | Valeur actuelle |
|---|---|---|
| Q-R1 | Les infirmiers et les sages-femmes doivent-ils pouvoir rédiger une consultation, et prescrire selon un protocole (liste fermée de médicaments par établissement) ? | Non : consultation réservée au rôle `MEDECIN`. |
| Q-R2 | Autoriser une demande d'examen **post-mortem** par un médecin, avec un type dédié et un motif obligatoire ? | Toute demande d'examen après le décès est refusée. |
| Q-R3 | Différer l'affichage d'un résultat critique au patient jusqu'à l'accusé de lecture du prescripteur, ou pendant un délai fixé ? | Affichage immédiat, avec consigne d'action. |
| Q-R4 | Délai d'escalade d'un résultat critique non pris en compte (A4), et destinataires de l'escalade. | Défaut proposé par A4 : 30 minutes. |
| Q-R5 | Âge en dessous duquel le poids est obligatoire pour prescrire (A2). | Défaut prudent : 18 ans. |
| Q-R6 | Faut-il que le pharmacien soit alerté quand le médecin force une alerte allergie ou doublon (A1) ? | Le forçage est tracé dans l'audit ; la validation pharmaceutique refait le contrôle. |

Restent aussi ouvertes : FA-27 / Q-05 (constantes saisies par l'accueil) et FA-38 (unité de dispensation).
