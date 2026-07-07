# TKT-WEBP-SUPPORT-FIX-2 — Résolution de la disparition des images WebP après sauvegarde (Windows & Concurrence de flux)

**Mode** : Engineering — Bug Fix  
**Date** : 2026-07-07  
**Statut** : ✅ DONE  
**Profil** : Full-stack  
**Impact version** : PATCH (Correction de bug)

---

## Problématique

Lorsqu'un utilisateur chargeait une image au format WebP, celle-ci s'affichait temporairement dans la prévisualisation, mais disparaissait définitivement lors de la validation/sauvegarde et du rafraîchissement de la page.

## Cause racine

1. **Accès concurrent aux flux sous Windows** : Dans `FileStorageService.storeFile()`, un premier `InputStream` était ouvert pour l'analyse d'image via `ImageIO.read()`. Si cette lecture renvoyait `null` (comportement par défaut pour le WebP sous un JDK standard sans plugin), le code tentait d'ouvrir un second stream `file.getInputStream()` pour copier le fichier brut. Sous Windows, cette double ouverture simultanée provoquait un verrouillage de fichier temporaire Tomcat et levait une `IOException`, faisant échouer l'upload de l'image de manière silencieuse pour l'utilisateur, ce qui empêchait l'enregistrement de l'image en base de données.
2. **Incohérence d'extension & Type MIME** : Si un WebP était lu avec succès par le JDK mais devait être redimensionné, il était sauvegardé au format PNG (le JDK standard ne sachant pas écrire de WebP), tout en conservant l'extension `.webp` du nom d'origine. Cette incohérence entre l'extension de fichier et son contenu binaire provoquait le rejet de l'image par certains navigateurs stricts.

## Résolution apportée

1. **Chargement des octets en mémoire** : Modification de `FileStorageService.storeFile` pour charger les données du fichier en mémoire via `file.getBytes()` dès le début de la méthode. Cela évite d'ouvrir de multiples flux simultanés sur le même `MultipartFile` et résout le problème de verrouillage de fichier sous Windows.
2. **Gestion adaptative de l'extension et du format** :
   * Si une image WebP est redimensionnée, elle est convertie en PNG et l'extension du fichier final est dynamiquement forcée à `.png` (ex: `uuid.png`) afin de garantir la cohérence absolue avec son type MIME.
   * Si l'image WebP n'a pas besoin de redimensionnement ou si elle ne peut pas être décodée, elle est enregistrée brute sous son extension `.webp` d'origine.
3. **Tests unitaires dédiés** : Création du fichier de test [FileStorageServiceTest.java](file:///C:/MES-APPLICATIONS/joprelys-connect/backend/src/test/java/com/joprelys/backend/file/FileStorageServiceTest.java) pour valider ces comportements d'upload pour les formats JPEG, PNG et WEBP, ainsi que la validation stricte d'en-tête (Magic Numbers).

---

## Fichiers modifiés

* [FileStorageService.java](file:///C:/MES-APPLICATIONS/joprelys-connect/backend/src/main/java/com/joprelys/backend/file/FileStorageService.java)
* [FileStorageServiceTest.java](file:///C:/MES-APPLICATIONS/joprelys-connect/backend/src/test/java/com/joprelys/backend/file/FileStorageServiceTest.java)

---

## Tests / Vérifications

* Exécution réussie des 246 tests Maven (242 tests existants + 4 nouveaux tests unitaires pour `FileStorageService`).
