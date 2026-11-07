# FUNCTIONAL-SPEC — Joprelys ID et liaison patient ↔ DPU (EPIC ECO-20261107)

## 1. Besoin

Un patient qui utilise AllôPharma veut retrouver dans son carnet les informations de son dossier patient unique (DPU) tenu par une clinique Joprelys. Les deux applications restent autonomes : chacune garde sa propre connexion. **Joprelys ID** ne sert qu'à relier les deux, avec le consentement explicite du patient.

## 2. Acteurs

| Acteur | Rôle |
|---|---|
| Patient (ou tuteur d'un enfant / adulte dépendant) | Relie son profil AllôPharma à son DPU, consent, révoque |
| AllôPharma (mobile + serveur) | Demande la liaison, conserve le lien sans copie d'identité |
| Joprelys (HIS) | Propriétaire du DPU, enregistre la liaison et le consentement |
| Joprelys ID (Keycloak) | Authentifie le patient pour la liaison et affiche l'écran de consentement OAuth |

## 3. Parcours (environnement de test)

1. Dans AllôPharma, le patient ouvre « Relier mon dossier Joprelys », lit ce qui sera partagé et pour quel profil, puis accepte.
2. L'application ouvre Joprelys ID dans le navigateur du téléphone : le patient se connecte, puis valide l'écran de consentement « Relier votre profil AllôPharma à votre dossier patient Joprelys (DPU) ».
3. AllôPharma reçoit un jeton à durée courte, le transmet une seule fois à son serveur, qui demande la liaison à Joprelys. Le jeton n'est pas conservé.
4. Joprelys crée la liaison (`PatientLink`) et le consentement (`Consent`) et renvoie leurs identifiants. AllôPharma affiche « Dossier relié ».
5. Révocation :
   - depuis AllôPharma : le patient arrête le partage ; AllôPharma le signale à Joprelys et supprime sa copie ;
   - depuis le portail patient Joprelys : Joprelys envoie l'événement `consent.revoked` ; AllôPharma supprime sa copie.

## 4. Règles métier

- Aucune donnée d'identité (nom, téléphone, date de naissance) n'est copiée d'un système à l'autre par la liaison.
- Le consentement est explicite, limité à des scopes, borné dans le temps et révocable à tout moment, des deux côtés.
- Après révocation, le récepteur purge sa copie en cache.
- Un tuteur ne peut relier que les profils dont il a la charge.
- En test, seuls des comptes et DPU fictifs (`DPU-TEST-…`) sont utilisés.

## 4 bis. Portail patient : « Applications reliées » (S7)

Page `/patient/linked-apps`, accessible depuis « Confidentialité » du portail patient (connexion patient existante : DPU, téléphone, date de naissance, code).

- Liste des applications **actuellement** reliées au dossier : nom et logo de l'application, données partagées (ordonnances, résultats d'analyses…), date de liaison, date de fin de validité.
- Action « Révoquer l'accès » sur chaque application, avec confirmation explicite qui rappelle les conséquences (l'application ne lit plus aucune donnée et supprime sa copie ; une nouvelle liaison reste possible depuis l'application).
- Après révocation : l'application disparaît de la liste, un message confirme ; Joprelys notifie l'application (`consent.revoked`).
- États : chargement (squelette), erreur avec « Réessayer », vide (« Aucune application n'accède à votre dossier »), échec de révocation (l'application reste listée, message clair).
- Une liaison appartenant à un autre patient est introuvable (404), jamais révélée.
- FR/EN, thème clair et sombre, navigation clavier et focus visible, cibles ≥ 44 px.

## 5. Hors périmètre de l'EPIC

- Preuve d'identité qui rattache un compte Joprelys ID à un vrai DPU (story ultérieure).
- Lecture des ordonnances, résultats et vaccinations (flux suivants).
- Mise en production (VPS, DPA, AIPD).

## 6. Avancement

| Story | Statut |
|---|---|
| S1 Joprelys ID en dev (Keycloak) | Livrée |
| S2 Dépôt `eco-contracts` v1 | Livrée |
| S3 API éco Joprelys : liaison et consentement | Livrée (seule la relation « soi-même » est acceptée) |
| S4 Webhooks signés | Livrée |
| S5 Réception AllôPharma | Livrée (dépôt AllôPharma) |
| S6 Écran mobile AllôPharma | Livrée, testée sur appareil |
| S7 Portail « Applications reliées » | Livrée |
| S8 Bout en bout, revue sécurité, clôture | À faire |
