# EPIC-0030 — Remédiation de l'audit complet du 2026-11-04 (reste à faire)

- **Origine** : [AUDIT-REPORT](../../features/audit-20261104-full/AUDIT-REPORT.md). Les correctifs courts (effort S/M) sont livrés sur la branche `fix/audit-20261104-security-clinical-ux` ; cet epic porte ce qui demande une décision métier, un changement de contrat ou plus de 2 jours.
- **SemVer** : MINOR minimum ; A2, A3 et B1 sont des candidats MAJOR (contrats API web/mobile).
- **Définition de prêt** : règle clinique validée par un médecin référent (et le pharmacien pour le circuit du médicament), contrat API écrit dans `docs/features/<id>/API-CONTRACT.md`, maquette pour toute UI.
- **Définition de fini** : règle appliquée côté serveur, tests unitaires et d'intégration, écran web (et mobile si concerné) FR/EN clair/sombre, recette navigateur, documentation et CHANGELOG à jour.

Estimation en story points (SP) selon `docs/pm/ESTIMATION-GUIDE.md`. Profil recommandé entre parenthèses.

## Lot A — Sécurité du patient (P0/P1) — 63 SP

| ID | Constat | Objectif et critères d'acceptation | SP | Profil |
|---|---|---|---|---|
| A1 | CLIN-01 | Contrôle des **allergies actives** et des **doublons thérapeutiques** à la signature de l'ordonnance et à la validation pharmaceutique (réutiliser `MedicationReferenceDuplicateDetector`). Alerte bloquante 409, forçage possible uniquement avec motif tracé dans l'audit. Écran d'alerte web. | 8 | Senior back + front |
| A2 | CLIN-02 | **Posologie structurée** : dose > 0 + unité, voie, fréquence en listes fermées, durée en jours, « si besoin » avec maximum par 24 h ; signature refusée si incomplète ; expiration = début + durée ; poids obligatoire sous un âge seuil (**à valider par un professionnel de santé**). À découper : modèle + migration / API / web / mobile. | 13 | Senior |
| A3 | CLIN-03, CLIN-06 | **Prescriptions d'hospitalisation** : plusieurs ordonnances par séjour, arrêt ligne par ligne (date, auteur, motif), éligibilité à l'administration limitée au séjour en cours + reprise explicite du traitement personnel ; motif obligatoire à l'annulation d'une ordonnance (contrat web). | 13 | Senior |
| A4 | FA-11 / S15 | **Corbeille de résultats du prescripteur** : tâche « Résultat à voir », escalade des valeurs critiques, accusé de prise en compte. Message « attention immédiate » au patient réservé aux résultats critiques validés. | 8 | Intermédiaire |
| A5 | CLIN-05, SEC-14 | Laboratoire : statut `DRAFT` par défaut à la saisie, **permission de validation biologique distincte**, heure de prélèvement obligatoire, au moins un résultat pour valider, motif obligatoire pour corriger un résultat validé (avis au prescripteur) ; intégration partenaire avec clé par établissement — elle ne résout pas l'établissement de la demande en requête anonyme et est donc probablement inopérante aujourd'hui (constat du 2026-11-04) ; statut du `DiagnosticReport` FHIR respecté au lieu de VALIDATED forcé. | 5 | Intermédiaire |
| A6 | CLIN-10, CLIN-07 | Constantes : bornes physiques (et non physiologiques) avec confirmation des valeurs extrêmes, **unité de glycémie** explicite (g/L ou mmol/L) convertie côté serveur, seuils pédiatriques et score d'alerte (**à valider par un professionnel de santé**) ; **heure du décès saisie** (et non l'heure du clic) pour l'orientation « Décès » des urgences (CLIN-07). Le triage en liste fermée et les bornes physiques des constantes initiales sont livrés. | 5 | Intermédiaire |
| A7 | CLIN-09 | Administration : dose structurée comparée à la dose prescrite, statuts « administré / non administré (motif) / suspendu », auteur par identifiant. L'heure bornée (pas de futur, pas après le décès) est livrée. | 8 | Intermédiaire |
| A8 | CLIN-14 | Allergies : motif obligatoire à la suppression ; sévérité « non renseignée » par défaut au lieu de « MEDIUM » posé par le système. | 3 | Junior encadré |

## Lot B — Sécurité applicative (P1) — 47 SP

| ID | Constat | Objectif et critères d'acceptation | SP | Profil |
|---|---|---|---|---|
| B1 | SEC-02 | Bris de glace **rattaché à l'utilisateur** (et non à l'établissement), réservé aux patients présents (épisode actif) ou soumis à seconde validation ; limite de fréquence ; alerte au DPO de l'établissement propriétaire ; file de revue a posteriori ; ne plus renvoyer `patientId` dans la demande d'accès externe. | 8 | Senior sécurité |
| B2 | SEC-08 | Signature, cachet et numéro d'ordre : table de métadonnées de fichiers (propriétaire, type), contrôle de propriété, signatures non servies publiquement, numéro d'ordre saisi par l'administrateur. | 5 | Intermédiaire |
| B3 | SEC-10 | DTO administratif (accueil, caisse) séparé du DTO clinique (allergies, antécédents : `CLINICAL_READ`) ; pagination de `GET /api/patients` ; audit des listes. | 5 | Intermédiaire |
| B4 | SEC-12 | IA : consentement ou opposition du patient par visite vérifié côté serveur, caviardage des identifiants dans les transcriptions, DPA et rétention zéro chez le fournisseur. | 5 | Senior + DPO |
| B5 | MOB-01, SEC-19 | Ticket WebSocket à usage unique (≈ 30 s) au lieu du jeton dans l'URL ; permission vérifiée au handshake. | 3 | Intermédiaire |
| B6 | UX-02 | QR code patient : jeton court signé par le backend, rendu local (plus d'appel à `api.qrserver.com`). | 3 | Intermédiaire |
| B7 | UX-14 | Double visa caisse au-delà du seuil : second approbateur authentifié côté serveur, seuil paramétrable ; horodatage labo et finalisation de consultation transactionnels côté serveur ; codes d'erreur typés. | 5 | Senior |
| B8 | SEC-05 (reste) | Défis OTP persistés (HMAC du code, tentatives, expiration) ou Redis/Bucket4j, pour un fonctionnement multi-instance. | 5 | Intermédiaire |
| B9 | SEC-01 (reste) | Identifiant opaque (128 bits) dans le QR d'ordonnance au lieu du numéro séquentiel, PIN haché, réponse anonyme réduite (initiales) et détail complet réservé à `PHARMACY_PRESCRIPTION_READ`. PIN 6 chiffres `SecureRandom` et verrouillage persistant atomique livrés (V121). | 5 | Senior sécurité |
| B10 | SEC-15, 17, 18, 21, CLIN-16 | Dette sécurité : captcha et anti-spam des demandes de démo, type et contenu des consentements d'hospitalisation vérifiés + `no-store`, `validate-on-migrate: true` (après contrôle des checksums), springdoc 3.x, OWASP dependency-check / CycloneDX en CI, tarifs non modifiables par l'accueil, déduplication des lignes manuelles. | 3 | Intermédiaire |

## Lot C — Parcours et interface (P1/P2) — 63 SP

| ID | Constat | Objectif et critères d'acceptation | SP | Profil |
|---|---|---|---|---|
| C1 | UX-07, FA-35 | Navigation **par parcours** (Accueil/Admissions, Soins, Plateau technique, Pharmacie, Finance, Administration), une seule entrée Caisse, entrées « File active » et « Patients hospitalisés de mon unité » (endpoint `GET /api/hospitalizations?unitId=&status=`), icônes distinctes, cartes de tableau de bord pour accueil, caisse et urgences. | 8 | Intermédiaire front + UX |
| C2 | UX-03, MOB-10 | Garde « modifications non enregistrées » (consultation, hospitalisation, saisie labo) et brouillon automatique ; verrou mobile en surcouche sans perte de saisie. | 5 | Intermédiaire |
| C3 | UX-09, MOB-15 | i18n restante : ≈ 600 chaînes web et ≈ 350 mobile, dictionnaire landing parallèle, clés dupliquées. | 13 | Junior encadré |
| C4 | UX-15, MOB-17 | Découpage des 28 composants web et 8 fichiers mobiles > 500 lignes ; suppression du code mort mobile (≈ 3 200 lignes) ; palette Tailwind brute → tokens. | 13 | Intermédiaire |
| C5 | UX-12, MOB-16 | Accessibilité : labels associés (145), `Semantics(identifier:)` mobiles (≈ 105), cibles ≥ 48 dp, drawer mobile en dialogue avec piège de focus, lien d'évitement. | 8 | Intermédiaire |
| C6 | MOB-08, MOB-09 | Mobile : signature release (keystore via secrets CI), `--obfuscate --split-debug-info`, verrou obligatoire après inactivité, repli code appareil. | 8 | Intermédiaire mobile |
| C7 | MOB-11, MOB-12 | Mobile : messages d'erreur localisés, bandeau hors ligne, recherche patients paginée (≥ 2 caractères). | 8 | Intermédiaire mobile |

## Lot D — Cohérence des parcours (reprise d'EPIC-0029 lots 4-5)

S14 (tâches du personnel), S16 (visite rattachée à une unité), S17 (rendez-vous → arrivée), S18 (demande d'hospitalisation et réservation de lit), S19 (transfert), S20 / FA-26 (doublons à la création, recherche indépendante de l'ordre des noms), S21 (recentrage d'ADMIN_CLINIQUE au niveau RBAC ; les actes médicaux sont déjà bloqués côté serveur par ADR-20261104 D-01), FA-12 (récapitulatif de clôture avec examens en attente), FA-27 (constantes saisies par l'accueil : Q-05), **FA-38** (décrément du stock à la délivrance : décider d'abord l'**unité de dispensation**, boîte ou unité, avec le pharmacien). Estimations conservées dans [EPIC-0029](EPIC-0029-functional-journey-coherence.md).

## Capacité

173 SP hors lot D : à planifier sur plusieurs sprints selon `docs/pm/CAPACITY-PLANNING.md`. Ordre recommandé : A1, A4, B1, A2 puis A3 (risque patient), en parallèle de C1 et C6 (avant toute diffusion d'APK).
