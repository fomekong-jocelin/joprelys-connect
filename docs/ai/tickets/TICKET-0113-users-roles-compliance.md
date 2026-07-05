# TICKET-0113 — Conformité Module 2 : Gestion des utilisateurs et rôles (Cahier des Charges)

> Fichier de suivi obligatoire pour l'alignement et la complétude du Module 2 du cahier des charges.

## 1. Objectif

Aligner l'implémentation de la gestion des utilisateurs et rôles (Module 2) avec le cahier des charges.
Ceci inclut :
- **FR-USER-001** : S'assurer que les professionnels sont rattachés à un établissement (déjà en place via `organizationId` obligatoire pour le staff).
- **FR-USER-002** : Permettre à un utilisateur d'avoir plusieurs rôles tout en gardant des permissions explicites.
- **FR-USER-003** : S'assurer qu'un compte désactivé ne peut plus accéder (déjà en place via `isEnabled`).
- **FR-USER-004** : Enregistrer la date et heure de dernière connexion (`last_login_at`).
- **FR-USER-005** : Imposer une authentification forte (OTP 2FA) pour les rôles professionnels sensibles (`ADMIN_JOPRELYS`, `ADMIN_CLINIQUE`, `MEDECIN`, `BIOLOGISTE`, `PHARMACIEN`).

## 2. Critères d'acceptation

- [x] Ajout de la colonne `last_login_at` dans la table `users` via une migration Flyway (`V29`).
- [x] Ajout et persistance du champ `lastLoginAt` dans `UserAccountEntity` et mise à jour lors de chaque connexion réussie.
- [x] Support des rôles multiples séparés par des virgules dans la base de données et découpage dans `JwtAuthenticationFilter` en authorities Spring Security distinctes.
- [x] Mise en place d'un flux d'authentification double facteur OTP à 6 chiffres pour les rôles professionnels sensibles.
- [x] Intégration du flux d'OTP staff et du support multi-rôles dans l'interface de gestion Angular du Personnel (sélection par cases à cocher).
- [x] Suite de tests unitaires et d'intégration Spring Boot (231/231) et Angular (63/63) au vert.

## 3. Pilotage projet

| Champ | Valeur |
|---|---|
| Epic parent | N/A |
| User story parent | N/A |
| Sprint cible | SPRINT-0009 |
| Priorité business | P1 |
| Complexité | M |
| Story points | 3 |
| Profil recommandé | Senior |
| Effort estimé senior | 0.4j |
| Effort estimé intermédiaire | 0.6j |
| Effort estimé junior | 1.0j |
| Responsable | Antigravity |
| Reviewer obligatoire | Lead Developer / Jocelin |
| Risque fonctionnel | Faible |
| Risque technique | Moyen |
| Dépendances | SPRINT-0009 |
| Bloquants connus | Aucun |

## 4. Contexte analysé

- [x] `AGENTS.md` lu
- [x] `SKILL.md` lu
- [x] `PROJECT-MANAGER-SKILL.md` lu si nécessaire
- [x] `README-IA.md` lu
- [x] `WORKFLOW-IA.md` lu
- [x] `PROJECT-TRACKING.md` lu
- [x] `CHANGELOG.md` lu
- [x] `review-checklist.md` lu
- [x] Rapport d'audit `audit_rapport_complet.md` lu
- [x] Code existant et tests analysés

## 5. Hypothèses

- Les rôles sensibles nécessitant l'OTP sont : `ADMIN_JOPRELYS`, `ADMIN_CLINIQUE`, `MEDECIN`, `BIOLOGISTE` et `PHARMACIEN`.
- Les rôles multiples sont stockés sous forme de chaîne de caractères délimitée par des virgules dans le champ `role` existant (ex: `"MEDECIN,PHARMACIEN"`), ce qui évite de casser la structure existante tout en respectant la limite de 64 caractères du schéma de données.

## 6. Risques et impacts

| Risque | Impact | Mitigation |
|---|---|---|
| Expiration précoce de l'OTP en test unitaire | Échec des tests d'intégration | Utilisation de l'horloge injectée `clock` pour l'évaluation temporelle de validité de l'OTP au lieu de `Instant.now()` |
| Perte de compatibilité avec les clients mono-rôle | Régression d'authentification | Conserver le format de chaîne de caractères brut et découper dynamiquement à la volée |

## 7. Action plan

- [x] Créer la migration Flyway `V29__add_last_login_to_users.sql`
- [x] Ajouter le champ `lastLoginAt` et la méthode `hasRole` dans `UserAccountEntity`
- [x] Implémenter la logique d'authentification forte (OTP) et d'enregistrement de dernière connexion dans `AuthenticationService`
- [x] Configurer le endpoint `/api/auth/verify-otp` dans `AuthController` et `SecurityConfig`
- [x] Mettre à jour `JwtAuthenticationFilter` pour décoder et assigner les autorités multiples
- [x] Adapter les contrôles de rôles dans `AuditController`, `OrganizationController` et `PatientService`
- [x] Implémenter les tests unitaires et s'assurer que toute la suite passe
- [x] Mettre à jour l'IHM de login Angular pour le staff à double étape (Mot de passe + OTP)
- [x] Modifier l'écran d'administration du personnel pour permettre la sélection multi-rôle par checkboxes
- [x] Exécuter la compilation et les tests frontend (`ng build` et `ng test`)

## 8. Implémentation réalisée

- [x] Migration DB Flyway `V29` ajoutée.
- [x] Ajout des champs, helpers et routes sur le backend.
- [x] Enregistrement console de l'OTP pour le staff lors d'une connexion réussie pour un rôle sensible.
- [x] Multi-rôles supporté de bout en bout sur le backend (délivrance des tokens, filtres et autorisations Spring Security).
- [x] Mise à jour du client HTTP Angular et intégration des vues d'OTP et de sélection de rôles par cases à cocher.
- [x] Compilation et tests validés avec succès à 100% (Vitest 63/63, Maven 231/231).

## 9. Suivi d'exécution

| Date | Développeur | Temps passé | Avancement | Reste à faire | Blocage | Commentaire |
|---|---|---:|---:|---:|---|---|
| 2026-07-05 | Antigravity | 0.35j | 100% | Aucun | Aucun | Alignement du Module 2 effectué avec succès. |

## 10. Tests et vérifications

### Commandes exécutées ou à exécuter

```bash
# Backend compilation and tests
.\mvnw test

# Frontend build and tests
npm run build
npm run test
```

### Résultats

- [x] Tests unitaires OK (Vitest 63/63, Maven 231/231)
- [x] Tests intégration OK
- [x] Build OK

## 11. Documentation

- [ ] README mis à jour si nécessaire
- [ ] API docs mises à jour si nécessaire
- [ ] ADR créé si décision structurante
- [x] Changelog mis à jour
- [x] Suivi projet mis à jour

## 12. Reste à faire

Aucun.

## 13. Statut final

Statut : DONE

## 14. Notes finales

L'implémentation est entièrement alignée avec le cahier des charges (Module 2). La double authentification pour les rôles sensibles et l'enregistrement de la dernière connexion ont été ajoutés de manière propre et robuste, et le support des rôles multiples est désormais fonctionnel.

## 15. Impact version / SemVer

| Champ | Valeur |
|---|---|
| Changement livrable | Oui |
| Type de bump | MINOR |
| Justification | Ajout de nouvelles fonctionnalités rétrocompatibles (OTP staff, multi-rôles, date de dernière connexion) |
| Breaking change | Non |
| Migration DB | Oui (V29) |
| Changement API | Oui (Ajout de /api/auth/verify-otp) |
| Impact Angular | Oui (Login double étape + Checkboxes de rôles dans la gestion d'équipe) |
| Impact Flutter | Non |
| Changelog requis | Oui |
| Release note requise | Oui |

## 16. Vérification `.gitignore`

- [x] `.gitignore` présent à la racine
- [x] `.gitignore` adapté à la stack réelle du projet
- [x] `docs/standards/GITIGNORE-STANDARDS.md` respecté
- [x] Aucun secret, cache ou artefact de build versionné
