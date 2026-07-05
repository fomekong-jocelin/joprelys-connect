# Spécification Fonctionnelle — Interopérabilité HL7 FHIR (EPIC-0011)

## 1. Description et contexte
Afin de faire de **Joprelys Connect** un socle d'interopérabilité de santé moderne, il est nécessaire de supporter les standards internationaux, en particulier **HL7 FHIR R4**.
Cette fonctionnalité permettra à des applications partenaires (Mandacare, AllôPharma, assureurs, ministères) d'extraire de manière structurée les dossiers de consultation, sans devoir développer de connecteurs propriétaires.

## 2. Objectifs
- Exposer les dossiers patients et les rencontres cliniques sous forme de ressources FHIR standardisées.
- Faciliter l'intégration de nouveaux établissements et outils de santé partenaires.
- Permettre aux développeurs tiers de consommer l'API de façon normalisée.

## 3. Périmètre
### Inclus
- **Ressources FHIR R4 supportées** :
  - `Patient` (Identité et métadonnées du patient).
  - `Encounter` (Visites et rencontres médicales).
  - `Observation` (Constantes vitales et mesures physiques).
- **Format** : Rendu des requêtes HTTP au format JSON conforme aux schémas HL7 FHIR R4.
- **Droits d'accès** : Accès réservé aux applications de confiance (`API_CLIENT` / `PHARMACIEN` / `MEDECIN`) possédant un token d'autorisation valide.

### Exclu
- Support complet en écriture (POST/PUT/DELETE) pour toutes les ressources FHIR.
- Support des profils nationaux complexes hors du Cameroun.
- Stockage natif des données sous forme d'une base de données orientée FHIR (type HAPI FHIR). La base reste relationnelle, avec sérialisation/mapping dynamique à la volée.

## 4. Parcours utilisateur
1. Un système partenaire authentifié effectue un appel HTTP `GET /fhir/Patient/{id}`.
2. Joprelys Connect valide le token et les permissions du système.
3. Si autorisé, le système reçoit la ressource FHIR `Patient` correspondante.
4. Pour voir l'historique des visites, le système interroge `GET /fhir/Encounter?patient={patientId}`, retournant un `Bundle` de ressources `Encounter`.

## 5. Critères d'acceptation généraux (DoD)
- Les ressources FHIR générées valident contre les schémas officiels HL7 FHIR R4.
- La structure respecte le multi-tenant de la clinique (un client d'API externe ne peut lire que les données des organisations pour lesquelles il a été autorisé).
- Les accès via les endpoints FHIR font l'objet d'un log d'audit de sécurité standardisé (`READ_FHIR_RESOURCE`).
- Traduction i18n des libellés et des termes codés (ex. systèmes de codification des sexes et statuts de visite).
