# TICKET — Résolution de l'erreur 401 Unauthorized lors du téléchargement du PDF de décharge d'hospitalisation

**Date** : 2026-07-05
**Mode** : Diagnostic / Bug / Engineering
**Statut** : DONE
**Version impact** : PATCH

---

## Problème résolu

**Erreur 401 Unauthorized** (Whitelabel Error Page) lors du clic sur le bouton de téléchargement de la fiche de sortie d'hospitalisation (`/api/hospitalizations/{id}/pdf`).
* **Cause** : Le composant frontend `PatientHospitalizationComponent` utilisait une balise `<a>` avec un attribut `[href]` pointant directement vers l'URL du backend. Étant donné que cette navigation de fichier s'effectue directement par le navigateur (hors du client HTTP Angular), aucun en-tête `Authorization: Bearer <token>` n'était inclus. Spring Security rejetait donc la demande avec une erreur `401 Unauthorized`.
* **Solution** : 
  1. Remplacement du lien de téléchargement direct `<a>` par un `<button>` déclenchant une méthode Angular de téléchargement par flux.
  2. Implémentation de la méthode `downloadDischargePdf` dans `PatientApiService` effectuant une requête GET avec l'en-tête JWT (géré par l'intercepteur Angular `AuthTokenInterceptor`) et retournant un `Blob`.
  3. Gestion du téléchargement du Blob à la volée en créant un élément `a` temporaire avec `URL.createObjectURL(blob)` et en déclenchant un clic programmatique avant de libérer l'URL.

---

## Actions réalisées

- [x] **PatientApiService.ts** : Ajout de la méthode `downloadDischargePdf(id)` pour charger le fichier sous forme de blob binaire avec le HttpClient d'Angular.
- [x] **PatientHospitalizationComponent.ts** : 
  * Remplacement de l'ancre `<a>` par un `<button>` appelant `downloadDischargePdf(hosp)`.
  * Remplacement de la méthode utilitaire `getDownloadUrl` par la méthode `downloadDischargePdf` gérant la création de l'URL du blob, le déclenchement du téléchargement et le nettoyage mémoire (`URL.revokeObjectURL`).

---

## Fichiers modifiés

- `web/src/app/patient/patient-api.service.ts`
- `web/src/app/patient/patient-hospitalization.component.ts`

---

## Tests et validation

* **Compilation frontend** : Compilation réussie sans erreur.
* **Tests unitaires et d'intégration** : Exécution de `npm test` sur le frontend et `.\mvnw.cmd test` sur le backend. **Tous les tests (62/62 frontend et 180/180 backend) ont été validés avec succès.**
