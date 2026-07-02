# STORY-0603 — Révocation et annulation de documents médicaux (Backend)

## 1. Objectif

Permettre aux acteurs habilités (Médecin, Admin Clinique) de révoquer ou d'annuler un document médical
généré. Une fois révoqué, le scan du QR code affiche clairement le statut `RÉVOQUÉ` ou `ANNULÉ` sur
la page publique de vérification, conformément aux exigences **FR-DOC-004** du cahier des charges.

## 2. Critères d'acceptation

- [x] `PATCH /api/documents/{id}/revoke` → passe le statut du document à `REVOQUE` ✅
- [x] `PATCH /api/documents/{id}/cancel` → passe le statut du document à `ANNULE`
- [x] Rôles autorisés : `MEDECIN`, `ADMIN_CLINIQUE` uniquement
- [x] Motif de révocation obligatoire (`reason` non vide, max 500 caractères)
- [x] Traçabilité complète : `revoked_at`, `revoked_by_user_id`, `revocation_reason` enregistrés
- [x] Double révocation d'un document déjà révoqué → `409 CONFLICT`
- [x] Accès sans token → `401 UNAUTHORIZED`
- [x] Rôle non habilité (PHARMACIEN, etc.) → `403 FORBIDDEN`
- [x] Document inexistant → `404 NOT FOUND`
- [x] La page publique `/verify/{id}` affiche le statut réel (`REVOQUE` / `ANNULE`) sans modification

## 3. Pilotage projet

| Champ | Valeur |
|---|---|
| Epic parent | EPIC-0006 (Génération PDF & Vérification par QR Code) |
| User story parent | STORY-0603 |
| Sprint cible | SPRINT-0004 |
| Priorité business | P1 |
| Complexité | S |
| Story points | 2 |
| Profil recommandé | Intermédiaire |
| Effort senior | 0.7j |
| Responsable | Antigravity |
| Reviewer obligatoire | Lead Developer |
| Dépendances | STORY-0601 (medical_documents en base) |

## 4. Contexte analysé

- [x] `AGENTS.md` lu
- [x] `SKILL.md` lu
- [x] `PROJECT-TRACKING.md` lu
- [x] `CHANGELOG.md` lu
- [x] `Cahier_des_charges_Joprelys_Connect_Complet.md` lu (Module 11, FR-DOC-004)
- [x] `MedicalDocumentEntity.java` analysé
- [x] `DocumentService.java` analysé
- [x] `DocumentController.java` analysé
- [x] `V8__create_medical_documents_table.sql` analysé

## 5. Différence sémantique REVOQUE vs ANNULE

| Statut | Signification | Cas d'usage |
|---|---|---|
| `REVOQUE` | Document invalide, mais référençable historiquement | Erreur de saisie médicale, document remplacé |
| `ANNULE` | Document considéré comme n'ayant jamais dû exister | Doublon système, bug de génération |

## 6. Risques et impacts

| Risque | Impact | Mitigation |
|---|---|---|
| Révocation d'un mauvais document | Moyen | Motif obligatoire + traçabilité acteur |
| Cross-tenant : Médecin A révoque doc de Clinique B | Fort | `@TenantId` sur `organization_id` + filtre Hibernate multi-tenant |

## 7. Action plan

1. [x] Migration Flyway `V9__add_revocation_to_medical_documents.sql`
2. [x] Mise à jour de `MedicalDocumentEntity` : colonnes + méthode `revoke()`
3. [x] Ajout `RevokeDocumentRequest.java` (DTO requête avec validation `@NotBlank`)
4. [x] Ajout `DocumentStatusResponse.java` (DTO réponse avec métadonnées d'audit)
5. [x] Mise à jour `DocumentService` : méthodes `revokeDocument()` + `cancelDocument()`
6. [x] Mise à jour `DocumentController` : endpoints `PATCH /revoke` + `PATCH /cancel`
7. [x] Écriture des 5 tests d'intégration dans `DocumentRevocationControllerTest.java`

## 8. Fichiers modifiés / créés

| Fichier | Action |
|---|---|
| `V9__add_revocation_to_medical_documents.sql` | Créé |
| `MedicalDocumentEntity.java` | Modifié (+3 colonnes, +méthode revoke()) |
| `RevokeDocumentRequest.java` | Créé |
| `DocumentStatusResponse.java` | Créé |
| `DocumentService.java` | Modifié (+revokeDocument, +cancelDocument) |
| `DocumentController.java` | Modifié (+PATCH revoke, +PATCH cancel) |
| `DocumentRevocationControllerTest.java` | Créé (5 tests) |

## 9. Tests

| Test | Résultat attendu |
|---|---|
| `givenMedecin_whenRevokeValidDocument_thenStatusIsRevoque` | 200 + statut REVOQUE |
| `givenAdminClinique_whenCancelValidDocument_thenStatusIsAnnule` | 200 + statut ANNULE |
| `givenAlreadyRevokedDocument_whenRevokeAgain_thenConflict` | 409 CONFLICT |
| `givenPharmacien_whenRevokeDocument_thenForbidden` | 403 FORBIDDEN |
| `givenNoToken_whenRevokeDocument_thenUnauthorized` | 401 UNAUTHORIZED |

## 10. Sécurité / OWASP

- RBAC strict (`@PreAuthorize`) — OWASP API4 / Broken Object Level Authorization
- Motif obligatoire → traçabilité complète (OWASP A09 — Security Logging)
- Multi-tenant Hibernate : un médecin ne peut révoquer que les documents de sa clinique
- Pas d'exposition de données médicales dans la réponse de révocation

## 11. Impact version / SemVer

| Champ | Valeur |
|---|---|
| Changement livrable | Oui |
| Type de bump | MINOR |
| Justification | Ajout des endpoints de révocation/annulation de documents + migration V9 |
| Release cible | v0.5.0 |

## 12. Statut final

Statut : **REVIEW** — 5/5 tests au vert (BUILD SUCCESS, 21.16s)

## 13. Reste à faire

- [x] 5 tests exécutés et validés
- [x] `PROJECT-TRACKING.md` mis à jour
- [x] `CHANGELOG.md` mis à jour
- [ ] Validation visuelle du statut révoqué sur la page `/verify/:id` (frontend STORY-0602)
- [ ] Cadrer EPIC-0008 (Portail Patient & Consentement) en backlog
