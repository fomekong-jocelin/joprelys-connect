# TICKET — API d'intégration labo externe pour téléversement (STORY-0902)

**Date** : 2026-07-03
**Mode** : Engineering
**Statut** : DONE
**Version impact** : MINOR

---

## Objectif

Implémenter l'API REST publique permettant à un laboratoire externe de téléverser des résultats d'analyses structurés ainsi que le fichier PDF officiel d'un patient. L'accès est sécurisé via une vérification de clé d'API (`X-API-KEY`).

---

## Critères d'acceptation

- [x] **Sécurité d'accès** :
  - L'endpoint `POST /api/public/lab-integration/upload` doit exiger le header `X-API-KEY`.
  - La valeur du header doit correspondre à la clé d'API configurée sous `joprelys.lab-integration.api-key` dans `application.yml` (par défaut `lab-partner-secret-token`).
  - En cas d'absence ou d'incohérence, renvoyer `401 Unauthorized`.
- [x] **Téléversement des Résultats** :
  - Récupérer la demande `LabOrderEntity` associée via son code unique `examRequestNumber`.
  - Si introuvable ou si le statut de la demande est `CANCELLED`, renvoyer une erreur appropriée.
  - Décoder la chaîne `pdfBase64` et sauvegarder le fichier physique PDF dans le dossier de stockage de documents (`joprelys.documents.storage-dir`).
  - Créer un enregistrement `LabResultEntity` pour chaque analyte présent dans la liste `results` de la requête HTTP.
  - Mettre à jour le statut de la demande `LabOrderEntity` à `VALIDATED`.
- [x] **Observabilité** :
  - Générer un log d'audit de succès via `AuditService` de type `UPLOAD_LAB_RESULTS`.

---

## Actions à réaliser

- [x] **Backend** :
  - [x] Déclarer les DTOs `LabResultUploadRequest` et `LabResultItem` dans `com.joprelys.backend.lab.api`.
  - [x] Créer le service `LabResultService` dans `com.joprelys.backend.lab.application` pour la logique métier et la gestion des fichiers.
  - [x] Créer le contrôleur REST `LabResultUploadController` sous `/api/public/lab-integration`.
  - [x] Ajouter des tests d'intégration dans `LabIntegrationControllerTest.java` pour valider l'authentification et les cas limites.
