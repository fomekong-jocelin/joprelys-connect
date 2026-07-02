# STORY-0601 — Génération et stockage du PDF de consultation (Backend)

## 1. Objectif

Cette user story consiste à concevoir et implémenter la génération automatique et sécurisée d'un document PDF combinant le compte-rendu de consultation et l'ordonnance médicale lors de la clôture d'une visite par le médecin. Le PDF contiendra un QR code unique pointant vers la page publique de vérification.

## 2. Critères d'acceptation
- [x] À la clôture d'une visite active (`closeVisit`), le système génère un PDF officiel.
- [x] Le PDF intègre un QR code généré dynamiquement qui contient l'URL de vérification publique du document : `http://<domain>/verify/<document-uuid>`.
- [x] Le fichier PDF est enregistré de manière sécurisée dans un répertoire local du serveur (paramétré dans `application.yml`).
- [x] Une ligne de métadonnées est enregistrée dans la table `medical_documents`.
- [x] Le numéro de document unique (`DOC-YYYYMMDD-XXXXXX`) est généré séquentiellement.
- [x] Un endpoint sécurisé `GET /api/visits/{visitId}/document` permet aux praticiens habilités de télécharger le PDF.

## 3. Pilotage projet
| Champ | Valeur |
|---|---|
| Epic parent | EPIC-0006 (Génération PDF & Vérification par QR Code) |
| User story parent | STORY-0601 |
| Sprint cible | SPRINT-0003 |
| Priorité business | P0 |
| Complexité | M |
| Story points | 3 |
| Profil recommandé | Senior |
| Effort senior | 1.0j |
| Responsable | Antigravity |
| Reviewer obligatoire | Lead Developer |
| Risque fonctionnel | Moyen |
| Risque technique | Moyen |
| Dépendances | EPIC-0005 |

## 4. Contexte analysé
- [x] `AGENTS.md` lu
- [x] `SKILL.md` lu
- [x] `PROJECT-TRACKING.md` lu
- [x] `CHANGELOG.md` lu
- [x] `pom.xml` analysé

## 5. Hypothèses
- L'URL de base du serveur pour le QR code (ex: `http://localhost:8080` en dev, ou domaine de production) est paramétrée dans `application.yml`.
- Si la visite ne possède ni consultation ni constantes vitales, on peut quand même générer un PDF minimal de visite avec les données administratives.
- Si le répertoire de stockage configuré n'existe pas, le service le crée automatiquement au démarrage.

## 6. Risques et impacts
| Risque | Impact | Mitigation |
|---|---|---|
| Fuite de données si le dossier de stockage est exposé publiquement | Très Fort | Le répertoire de stockage est en dehors de la racine web publique et les fichiers ne sont servis que via un contrôleur Spring Security authentifié. |

## 7. Action plan
1. [x] Ajouter les dépendances `openpdf` et `zxing` dans `pom.xml` (déjà présentes).
2. [x] Créer la migration de base de données `V8__create_medical_documents_table.sql` (déjà présente).
3. [x] Créer l'entité `MedicalDocumentEntity.java` et son repository.
4. [x] Créer un générateur séquentiel pour le numéro de document.
5. [x] Implémentation de `PdfGeneratorService` et `QrCodeGeneratorService`.
6. [x] Intégrer l'appel de génération du document dans `VisitService.closeVisit`.
7. [x] Créer `DocumentController.java` avec les endpoints de téléchargement sécurisé et de vérification publique.
8. [x] Ajouter la route publique `/api/public/**` dans `SecurityConfig.java`.
9. [x] Écrire les tests unitaires et d'intégration.

## 8. Implémentation réalisée
L'ensemble de la story STORY-0601 a été implémenté :
1. **Entité & Repository JPA** : `MedicalDocumentEntity.java` et `MedicalDocumentRepository.java` ont été créés pour enregistrer les métadonnées sur le document (visit, doc number, file path, status, tenant-id organizationId).
2. **Génération séquentielle** : `DocumentNumberGenerator.java` a été écrit pour générer le numéro au format `DOC-YYYYMMDD-XXXXXX` en utilisant `JdbcTemplate` pour éviter d'être limité par le filtre du multi-tenant.
3. **Génération QR Code** : `QrCodeGeneratorService.java` utilise `zxing` pour générer le PNG en mémoire.
4. **Génération PDF** : `PdfGeneratorService.java` utilise `openpdf` (`PdfPTable` et `PdfPCell`) pour structurer un PDF A4 contenant les détails de visite, constantes vitales, consultation et prescription.
5. **Orchestration & Lecture** : `DocumentService.java` gère le cycle de vie, la création de dossiers locaux si besoin, l'écriture sur disque et la lecture du PDF. Il implémente également la méthode de vérification `verifyDocument` via `JdbcTemplate` afin de contourner l'absence de tenant (contexte anonyme public).
6. **Contrôleur REST** : `DocumentController.java` expose `/api/visits/{visitId}/document` pour le téléchargement sous contrôle RBAC, et `/api/public/documents/{id}/verify` pour l'authentification publique de métadonnées (sans divulguer symptômes ou diagnostics).
7. **Visite Clôturée** : L'appel a été inséré de façon `@Lazy` dans `VisitService.closeVisit()` pour assurer la génération automatique.
8. **Sécurité & Routage** : `SecurityConfig.java` autorise l'accès anonyme à `/api/public/**`.

## 9. Suivi d'exécution
- **Temps passé** : 1.0j
- **Activité** : Backend, Tests & Sécurité

## 10. Tests et vérifications
Les tests ont été implémentés dans `DocumentControllerTest.java` :
- `givenMedecin_whenCloseVisit_thenDocumentGeneratedAndSaved`
- `givenValidDocument_whenVerifyAnonymously_thenSuccess`
- `givenValidVisit_whenDownloadDocument_thenSuccess`
- `givenNoToken_whenDownloadDocument_thenUnauthorized`
- `givenWrongRole_whenDownloadDocument_thenForbidden`
- `givenCrossTenant_whenDownloadDocument_thenNotFound`

Tous les tests compilent et passent avec succès (`BUILD SUCCESS`, 6 tests exécutés).

## 11. Documentation
- [x] Spécification fonctionnelle créée : `docs/features/pdf/FUNCTIONAL-SPEC.md`
- [x] Spécification technique créée : `docs/features/pdf/TECHNICAL-DESIGN.md`

## 12. Reste à faire
Néant. La story est prête pour la validation finale par le lead dev.

## 13. Statut final
Statut : REVIEW

## 14. Impact version / SemVer
| Champ | Valeur |
|---|---|
| Changement livrable | Oui |
| Type de bump | MINOR |
| Justification | Ajout de la génération PDF, intégration QR code et portail public de vérification |
