# TKT-WEBP-SUPPORT-FIX — Support du format WEBP pour l'upload d'images

**Mode** : Engineering — Bug Fix  
**Date** : 2026-07-07  
**Statut** : ✅ DONE  
**Profil** : Full-stack  
**Impact version** : PATCH (Correction de bug)

---

## Problématique

Depuis la mise en place de la conversion des images au format WebP (par exemple par le navigateur ou lors de l'envoi), les images s'affichent correctement en local dans le composant de prévisualisation (grâce au FileReader), mais disparaissent au rechargement (refresh) de la page.

## Cause racine

Le composant frontend génère immédiatement une prévisualisation locale de l'image sélectionnée. Cependant, lors de la soumission, l'appel API `/api/files/upload` échouait avec une erreur `400 Bad Request` car le service de stockage du backend (`FileStorageService`) rejetait le format d'image WebP dans sa validation stricte des Magic Numbers (`validateImageHeader`), ne tolérant que le PNG et le JPEG.

## Résolution apportée

1. **Backend - Validation d'en-tête (`FileStorageService.java`) :**
   * Augmentation du tampon d'en-tête de validation de 8 à 12 octets.
   * Ajout de la validation de la signature magique WebP (vérification de la présence de `"RIFF"` aux octets 0-3 et de `"WEBP"` aux octets 8-11).
   * Tolérance explicite pour les fichiers WebP réels.
2. **Backend - Détection d'extension (`FileStorageService.java`) :**
   * Prise en compte de l'extension `.webp` lors de l'upload et fallback propre vers le stockage direct (sans altération) si la bibliothèque ImageIO standard du JDK n'intègre pas de codec WebP de redimensionnement.
3. **Backend - Affichage des fichiers (`FileController.java`) :**
   * Ajout du support de retour pour le MediaType `image/webp` lors de la visualisation via `/api/public/files/view?path=...`.
4. **Frontend - Composant Drag-and-Drop (`file-drag-drop.component.ts`) :**
   * Ajout de `image/webp` dans la liste des formats acceptés par défaut dans l'input `accept`.

---

## Fichiers modifiés

* [FileStorageService.java](file:///C:/MES-APPLICATIONS/joprelys-connect/backend/src/main/java/com/joprelys/backend/file/FileStorageService.java)
* [FileController.java](file:///C:/MES-APPLICATIONS/joprelys-connect/backend/src/main/java/com/joprelys/backend/file/FileController.java)
* [file-drag-drop.component.ts](file:///C:/MES-APPLICATIONS/joprelys-connect/web/src/app/shared/ui/file-drag-drop.component.ts)

---

## Tests / Vérifications

* **Backend :** Exécution réussie des 242 tests Maven.
* **Frontend :** Exécution réussie des 101 tests Vitest Angular.
* **Build :** OK.
