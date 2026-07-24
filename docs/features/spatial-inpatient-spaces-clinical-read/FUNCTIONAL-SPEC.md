# FUNCTIONAL-SPEC — Consultation clinique des espaces d'hébergement et lits

## 1. Objectif Fonctionnel

Permettre aux praticiens cliniques (médecins, infirmiers, sages-femmes, urgentistes) de consulter la liste des espaces d'hébergement actifs (chambres, box) et leur occupation de lits en temps réel depuis la page `/clinic/spatial`, sans leur donner accès à la modification de la structure physique de l'établissement.

## 2. Parcours Utilisateur

1. **Connexion Praticien** : Le médecin ou l'infirmier se connecte avec son compte professionnel.
2. **Consultation de la capacité spatiale** : Il clique sur « Occupation des lits » (`/clinic/spatial`).
3. **Sélection de l'espace** : La liste déroulante charge automatiquement la liste des espaces d'hébergement actifs de l'établissement.
4. **Visualisation** : En sélectionnant une chambre (ex. `Chambre 101`), la carte d'occupation et la liste des lits (`101-A`, `101-B`) s'affichent avec leurs états opérationnels (`Libre`, `Occupé`, `Nettoyage`, `Maintenance`).

## 3. Critères d'Acceptation

- [x] L'endpoint `/api/spatial/inpatient-spaces` est accessible aux utilisateurs possédant `HOSPITALIZATION_READ`.
- [x] La liste déroulante des espaces sur la page `/clinic/spatial` se remplit correctement pour un médecin sans erreur 403.
- [x] Les routes de modification `/api/spatial/configuration/*` restent strictement interdites aux médecins (403 Forbidden).
