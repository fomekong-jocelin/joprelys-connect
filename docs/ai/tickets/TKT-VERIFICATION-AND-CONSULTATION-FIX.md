# TKT-VERIFICATION-AND-CONSULTATION-FIX — Résolution du double verify d'authentification et de l'erreur 500 consultation

**Mode** : Engineering — Bug Fix  
**Date** : 2026-07-07  
**Statut** : ✅ DONE  
**Profil** : Full-stack  
**Impact version** : PATCH (Correction de bugs)

---

## 1. Problématiques & Causes racines

### Bug 1 : Lien de vérification publique vers une page blanche
* **Symptôme** : Les liens et QR codes d'authentification des fiches de sortie d'hospitalisation et des résultats de laboratoires menaient à une page blanche.
* **Cause racine** : 
  Dans `VerificationUrlProvider.java`, la méthode `getDynamicVerificationBaseUrl()` retourne déjà une URL contenant `/verify`.
  Toutefois, lors de la génération de l'URL du document, les services appelaient `verificationUrlProvider.getVerificationUrl("verify/" + doc.getId())`.
  Le chemin final retourné était donc du type `[scheme]://[domain]/verify/verify/[docId]`.
  Côté frontend Angular, aucune route n'était configurée pour `/verify/verify/:documentId`, provoquant le routage vers une page blanche.
  Par ailleurs, pour la fiche d'admission / synthèse de patient (`patient-summary/[patientId]`), la route à double segment `/verify/patient-summary/:id` n'était pas reconnue par la route simple `/verify/:documentId`, entraînant aussi une page blanche.

### Bug 2 : Impossible d'enregistrer une consultation (Erreur 500)
* **Symptôme** : Les médecins ne pouvaient pas enregistrer de consultation (HTTP 500).
* **Cause racine** :
  Dans `ConsultationService.java`, le numéro de document de la consultation était généré en utilisant :
  `long count = consultationRepository.count() + 1;`
  Dans un environnement multi-tenant, le filtre JPA Hibernate restreint automatiquement l'appel à `.count()` au tenant courant (la clinique de l'utilisateur connecté).
  Par conséquent, deux cliniques différentes commençaient leur compteur au même nombre et généraient des numéros de documents identiques (ex: `DOC-CONS-20260707-000001`).
  Puisque la colonne `document_number` possède une contrainte d'unicité globale (`UNIQUE`) en base de données, l'insertion échouait avec une exception de violation d'unicité, levant une erreur 500.

---

## 2. Résolution apportée

### Bug 1 (Authentification) :
1. **Nettoyage automatique du chemin** : Modification de la méthode `getVerificationUrl` dans `VerificationUrlProvider.java` pour détecter et supprimer le préfixe double `verify/` ou `/` s'il est déjà fourni dans l'argument.
2. **Routes Angular de repli** : Ajout des routes `/verify/patient-summary/:documentId` et `/verify/hospitalization/:documentId` dans `web/src/app/app.routes.ts` pointant vers `VerificationComponent` afin de prévenir toute page blanche sur d'anciens QR codes ou formats d'URLs spécifiques.

### Bug 2 (Consultation 500) :
1. **Comptage global natif** : Ajout de la méthode `countGlobally()` avec une requête SQL native (`nativeQuery = true`) dans `ConsultationRepository.java` pour bypasser le filtre de tenant d'Hibernate.
2. **Génération unique** : Remplacement de `consultationRepository.count()` par `consultationRepository.countGlobally()` dans `ConsultationService.generateDocumentNumber()` pour assurer des numéros de consultations uniques à l'échelle de la base de données.

---

## 3. Fichiers modifiés

* [VerificationUrlProvider.java](file:///C:/MES-APPLICATIONS/joprelys-connect/backend/src/main/java/com/joprelys/backend/common/application/VerificationUrlProvider.java)
* [ConsultationRepository.java](file:///C:/MES-APPLICATIONS/joprelys-connect/backend/src/main/java/com/joprelys/backend/consultation/infrastructure/persistence/ConsultationRepository.java)
* [ConsultationService.java](file:///C:/MES-APPLICATIONS/joprelys-connect/backend/src/main/java/com/joprelys/backend/consultation/application/ConsultationService.java)
* [app.routes.ts](file:///C:/MES-APPLICATIONS/joprelys-connect/web/src/app/app.routes.ts)
