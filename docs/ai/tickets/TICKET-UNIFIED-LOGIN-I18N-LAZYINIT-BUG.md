# TICKET — Connexion Unifiée, Sélecteur de Langue Premium & Résolution LazyInitializationException

**Date** : 2026-07-02
**Mode** : Engineering / Bug / UI
**Statut** : DONE
**Version impact** : PATCH

---

## Problèmes résolus

1. **LazyInitializationException (Erreur 500)** :
   - Présent lors du téléchargement de documents médicaux (ordonnances) depuis le portail patient (`/api/patient/visits/{visitId}/document`) ainsi que l'interface clinique (`/api/visits/{visitId}/document`).
   - Cause : Hibernate tentait de charger la relation de visite et de patient (déclarée en `LAZY`) hors session dans le service d'audit lors de `loadDocumentFile()`, `revokeDocument()` et `cancelDocument()`.
   - Autre sous-problème : L'annotation `@Transactional(readOnly = true)` sur `loadDocumentFile()` interdisait l'écriture des journaux d'audit de succès (INSERT), causant un crash en écriture.

2. **Multiplicité des pages de connexion** :
   - Fatiguant de devoir saisir `/patient/login` et `/` séparément selon l'utilisateur.
   - Solution : Unification complète sous la route `/` avec un sélecteur "Personnel de santé / Patient" animé.

3. **Sélecteur de langue cassé sous Windows** :
   - Les émojis drapeaux 🇫🇷 et 🇬🇧 s'affichaient sous forme de texte brut ("FR FR" et "GB EN") sur Windows car l'OS n'intègre pas les drapeaux émojis nativement.
   - Solution : Refonte avec un sélecteur textuel statique élégant "FR | EN" respectant le design system, et réalignement du header "comme un pro" (Avatar à droite, nom/rôle à gauche, bouton déconnexion avec icône SVG).

---

## Actions réalisées

- [x] **MedicalDocumentRepository.java** : Ajout des requêtes JPQL avec `JOIN FETCH d.visit v JOIN FETCH v.patient` pour un chargement en une seule requête de la visite et du patient (`findByVisitIdWithVisitAndPatient` & `findByIdWithVisitAndPatient`).
- [x] **DocumentController.java** & **PatientPortalController.java** : Remplacement des appels de requêtes simples par les requêtes avec `JOIN FETCH`.
- [x] **DocumentService.java** : Suppression de `@Transactional(readOnly = true)` sur `loadDocumentFile()` pour permettre l'écriture de l'audit sans lever d'exception en écriture.
- [x] **I18nService.ts** : Ajout d'une méthode `setLocale(lang: AppLocale)` et d'une persistance locale automatique dans le `localStorage` sous la clé `joprelys_locale`.
- [x] **login.component.html** & **login.component.ts** : Refonte totale pour intégrer le sélecteur d'espace (Personnel/Patient) et le formulaire d'OTP patient en 2 étapes directement.
- [x] **app-shell.component.ts** : Remplacement du bouton toggle avec émoji par le sélecteur `FR | EN` statique et réalignement esthétique des éléments (avatar, nom/rôle, bouton déconnexion avec SVG).
- [x] **app.routes.ts** : Redirection de la route `/patient/login` vers `/` pour une entrée unique propre.

---

## Fichiers modifiés

- `backend/src/main/java/com/joprelys/backend/visit/infrastructure/persistence/MedicalDocumentRepository.java`
- `backend/src/main/java/com/joprelys/backend/visit/api/DocumentController.java`
- `backend/src/main/java/com/joprelys/backend/visit/application/DocumentService.java`
- `backend/src/main/java/com/joprelys/backend/patient/api/PatientPortalController.java`
- `web/src/app/core/i18n/i18n.service.ts`
- `web/src/app/auth/login.component.html`
- `web/src/app/auth/login.component.ts`
- `web/src/app/shared/layout/app-shell.component.ts`
- `web/src/app/app.routes.ts`

---

## Tests et validation

- Compilation backend réussie avec succès (`mvnw clean compile` OK).
- Build production frontend réussi avec succès (`npm run build` OK).
- Aucune régression détectée sur le flux.
