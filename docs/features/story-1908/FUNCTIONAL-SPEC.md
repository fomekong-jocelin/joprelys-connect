# Spécifications Fonctionnelles : Documents médicaux vérifiables conformes CDC (STORY-1908)

## 1. Problème métier
Le cahier des charges (CDC) de Joprelys Connect exige des garanties d'intégrité, de traçabilité, et de vérifiabilité publique pour tous les documents médicaux produits par la plateforme :
- **Numéro unique** : Chaque document généré doit avoir un numéro unique et non devinable.
- **Hash d’intégrité** : Calcul automatique d'un hash cryptographique SHA-256 sur le fichier PDF généré ou téléversé.
- **QR Code et URL de vérification** : Chaque document doit comporter un QR code permettant de vérifier son authenticité en redirigeant vers une URL publique de vérification.
- **Types de documents** : Les documents doivent être typés selon les 12 catégories définies par le CDC.
- **Versionnement et immutabilité** : Une nouvelle version d'un document (ex: suite à une correction de consultation ou de résultats) n'écrase pas l'ancienne mais la remplace (la version précédente passe au statut `REMPLACE` et la nouvelle pointe vers elle via `previous_document_id`).
- **Confidentialité de la vérification** : La vérification publique ne doit jamais divulguer de données de santé sensibles de manière anonyme. Elle affiche uniquement le statut, le numéro, le type, l'établissement, la date, le signataire (auteur ou service), et la mention légale obligatoire.
- **Demande d'accès** : La page de vérification publique doit permettre à un professionnel de santé externe de demander l'accès au dossier médical complet du patient associé.

## 2. Acteurs & Rôles
- **Médecin / Biologiste / Praticien** : Génère ou signe des documents médicaux (comptes-rendus, ordonnances, fiches de sortie, résultats).
- **Patient** : Consulte ses documents sur son espace personnel, vérifie l'historique des accès.
- **Tiers vérificateur (Pharmacie, Laboratoire, Etablissement externe, Autorités)** : Scanne le QR code sur le document PDF papier ou numérique et accède à la page de vérification pour confirmer que le document n'a pas été falsifié, révoqué ou remplacé.

## 3. Les 12 Types de Documents du CDC
Les documents enregistrés dans le système doivent appartenir à l'une de ces catégories :
1. `FICHE_ACCUEIL` : Fiche d’accueil patient.
2. `FICHE_PATIENT` : Fiche d’identification patient.
3. `COMPTE_RENDU_CONSULTATION` : Compte-rendu clinique de consultation.
4. `ORDONNANCE` : Prescription médicamenteuse.
5. `DEMANDE_EXAMEN` : Demande d’examens biologiques ou cliniques.
6. `RESULTAT_LABORATOIRE` : Résultats d'analyses médicales.
7. `COMPTE_RENDU_IMAGERIE` : Compte-rendu de radiologie, échographie, etc.
8. `CERTIFICAT_MEDICAL` : Certificat d’aptitude ou d’arrêt de travail.
9. `FICHE_HOSPITALISATION` : Fiche de séjour/d'admission en hospitalisation.
10. `FICHE_SORTIE` : Fiche officielle de sortie d'hospitalisation.
11. `RESUME_MEDICAL` : Synthèse médicale structurée.
12. `CONSENTEMENT_SIGNE` : Formulaire de consentement signé par le patient.

## 4. Critères d'acceptation
- **Champs de la table** : `MedicalDocumentEntity` contient `document_type`, `hash`, `qr_code_url`, `verification_url`, `author_user_id`, `version`, `previous_document_id`, et supporte le statut `REMPLACE`.
- **Calcul du Hash** : Au moment de l'enregistrement de n'importe quel PDF médical (synthèse de consultation, ordonnance, fiche de sortie, ou téléversement de résultat labo), son hash SHA-256 est calculé et stocké.
- **Unique Sequence** : Remplacement du compteur de document basé sur un simple `COUNT(*)` par une séquence de base de données dédiée pour garantir l'absence de doublons en environnement distribué.
- **Versionnement automatique** : En cas de nouvelle génération de document de même type pour la même visite (ex: génération d'une nouvelle ordonnance ou synthèse), la version précédente passe au statut `REMPLACE`, la nouvelle prend la version supérieure et référence la précédente.
- **Vérification Publique conforme** : L'API publique de vérification anonyme retourne les champs requis ainsi que la mention légale : *"Ce document ne donne pas accès au dossier médical complet"*.
- **Demande d'accès depuis la page de vérification** : La page frontend de vérification comporte un bouton/lien visible permettant de solliciter un accès temporaire au dossier.
