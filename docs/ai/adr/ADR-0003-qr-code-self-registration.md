# ADR-0003 — Enregistrement patient autonome (Self-Registration) via QR Code

## Statut

Accepté

## Date

2026-07-07

## Contexte

Afin de fluidifier le processus d'accueil et d'admission dans les hôpitaux partenaires de Joprelys Connect, il est proposé de permettre aux patients de saisir leurs informations de base (identité, contact, urgence) de manière autonome. Le patient scanne un QR Code physique présent sur le comptoir d'admission, accède à un formulaire public sur son smartphone, remplit ses informations et les soumet. L'agent d'accueil reçoit cette demande dans l'application, la valide (ce qui pré-remplit le formulaire de création de patient officiel) ou la rejette.

Cette proposition soulève plusieurs problématiques d'architecture et de sécurité :
1. **Sécurité & Spam** : Un endpoint de soumission public et anonyme peut être ciblé par des robots, des spams ou des attaques par déni de service (DoS).
2. **Confidentialité & RGPD (Fuite de PII)** : Si le système permet de "retrouver" un patient existant ou venant d'un autre hôpital depuis le formulaire public en saisissant ses coordonnées, il y a un risque majeur de fuite de données si ces coordonnées sont divulguées publiquement.
3. **Qualité des données & Doublons** : Le patient peut commettre des erreurs ou saisir des variantes de son nom, ce qui peut générer des doublons dans le système DPU (Dossier Patient Unique).
4. **Disponibilité du service** : Le système d'admission classique à l'accueil doit rester pleinement fonctionnel pour les patients sans équipement mobile ou ne sachant pas écrire.

## Décision

Il est décidé d'implémenter la fonctionnalité selon l'architecture et les règles de sécurité suivantes :

1. **Création d'une zone tampon (Staging Area)** : Les formulaires soumis par le public ne créent jamais directement une entité `PatientEntity`. Ils sont persistés dans une table dédiée `patient_pre_registrations` sous la forme de l'entité `PatientPreRegistrationEntity`. Le statut initial est `AWAITING_VALIDATION`.
2. **Interdiction de divulgation sur le portail public** : Le formulaire mobile public (non authentifié) ne doit **jamais** afficher d'informations médicales ou personnelles provenant de patients existants. Si un patient indique qu'il a déjà un dossier et saisit ses coordonnées :
   - Le système effectue une vérification en arrière-plan (sans renvoyer de données personnelles vers le mobile).
   - Le système affiche uniquement un message générique au patient : *"Vos coordonnées ont été transmises à l'accueil pour validation. Veuillez vous présenter au comptoir."*
   - C'est l'interface de l'agent d'accueil (back-office authentifié) qui affiche l'alerte de réconciliation : *"Un pré-enregistrement correspond à un patient existant (Score de similarité : X%). Fusionner ?"*.
3. **Sécurisation de l'API publique** :
   - Rate limiting strict sur l'endpoint public de soumission.
   - **Captcha Médical Respectueux de la Vie Privée** : Pour empêcher les robots d'abuser du formulaire sans utiliser de solutions tierces (type Google reCAPTCHA qui posent des problèmes de conformité RGPD et de cookies), nous implémentons un captcha médical hébergé par le serveur backend. Le serveur génère une question médicale simple et accessible (ex: *"Quelle est la température normale moyenne du corps humain (en °C) ? Réf : 37"*, ou *"Combien de battements de cœur par minute représente un pouls au repos normal ? Réf : 60-100"*), le patient y répond sur son mobile, et le serveur valide la réponse de manière chiffrée.
   - Validation stricte des données (format téléphone, email, injection de scripts).
4. **Workflow de validation à l'accueil** :
   - L'agent d'accueil dispose d'un écran listant les pré-enregistrements en attente.
   - Lors de la sélection d'une ligne, l'agent vérifie l'identité physique du patient (carte d'identité).
   - L'agent peut compléter ou corriger les données saisies par le patient.
   - En cliquant sur "Valider", l'entité `PatientEntity` officielle est créée (ou mise à jour si réconciliation), le pré-enregistrement passe à `VALIDATED` et une fiche d'admission avec un QR code de visite est générée au format PDF pour impression.
5. **Nettoyage automatique** :
   - Un job planifié (`@Scheduled`) supprime toutes les demandes à l'état `AWAITING_VALIDATION` datant de plus de 24 heures afin d'éviter l'accumulation de données inutiles.
6. **Maintien de l'accueil traditionnel** :
   - La possibilité de créer manuellement un patient dans l'IHM d'admission actuelle reste active et prioritaire.

## Raisons

- **Protection des données (RGPD / HIPAA)** : L'affichage direct de données personnelles sur un terminal non authentifié constitue une fuite de données (PII Leak) critique. La validation par l'agent d'accueil fait office de point de contrôle de sécurité.
- **Intégrité de la base de données** : Séparer les brouillons publics de la base de données DPU officielle évite l'injection de données erronées, invalides ou malveillantes.
- **Inclusivité** : Tous les patients n'ont pas de smartphone ou de compétences numériques. L'accueil traditionnel physique reste le point d'ancrage central.

## Conséquences positives

- Amélioration significative de l'expérience patient et réduction du temps d'attente aux heures de pointe.
- Moins d'erreurs de saisie orthographique commises par le personnel d'accueil fatigué.
- Processus de réconciliation transparent et sécurisé s'appuyant sur l'infrastructure existante de détection de doublons.
- Génération d'une fiche d'admission imprimable standardisée.

## Conséquences négatives / risques

- Complexité technique accrue due à la gestion de la table de transition et de la réconciliation.
- Charge de stockage temporaire en base de données (mitigée par le job d'auto-nettoyage à 24h).
- Dépendance vis-à-vis d'une connexion internet pour le patient (Wi-Fi de l'hôpital ou réseau mobile).

## Alternatives rejetées

| Alternative | Raison du rejet |
|---|---|
| Enregistrement direct dans la table `patients` | Risque élevé de spam, de pollution de données et d'attaques par injection sans contrôle humain. |
| Afficher les coordonnées retrouvées sur le smartphone du patient pour confirmation | Risque d'usurpation d'identité. N'importe qui connaissant le nom et la date de naissance d'un patient pourrait consulter son adresse, son téléphone et son historique d'admission. |

## Impact planning

| Élément | Impact |
|---|---|
| Charge | Environ 5.5j (Est. Senior) / 8j (Est. Intermédiaire) pour l'implémentation complète |
| Risque | Moyen |
| Profils nécessaires | Backend Engineer (Senior) + Frontend UI Engineer (Intermédiaire) |
| Sprint impacté | SPRINT-0012 (Implémentation) |

## Références

- [docs/standards/ARCHITECTURE-SOLID-RESPONSIBILITY-STANDARDS.md](file:///C:/MES-APPLICATIONS/joprelys-connect/docs/standards/ARCHITECTURE-SOLID-RESPONSIBILITY-STANDARDS.md)
- [docs/standards/UI-RADIUS-AND-SHADOW-STANDARDS.md](file:///C:/MES-APPLICATIONS/joprelys-connect/docs/standards/UI-RADIUS-AND-SHADOW-STANDARDS.md)
- [TICKET-1302-similarity-service](file:///C:/MES-APPLICATIONS/joprelys-connect/docs/ai/tickets/STORY-1201-allergies-antecedents.md)
