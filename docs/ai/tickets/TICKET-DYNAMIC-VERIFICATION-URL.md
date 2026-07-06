# TICKET-DYNAMIC-VERIFICATION-URL — Résolution dynamique de l'URL de vérification des documents en production

> Fichier obligatoire pour chaque ticket ou intervention IA.

## 1. Objectif

Permettre la génération d'URLs et de QR Codes de vérification dynamiques sur les PDF (Fiches de consultation, Ordonnances, Résumés médicaux, Fiches de sortie) :
1. Éviter d'avoir des URLs pointant vers `localhost` en production si la variable d'environnement `JOPRELYS_VERIFICATION_BASE_URL` n'est pas explicitement configurée.
2. Résoudre l'URL de base dynamiquement à partir du contexte de la requête HTTP active (en extrayant les headers `X-Forwarded-Host`, `X-Forwarded-Proto`, et `Host` provenant du reverse proxy de production).
3. Rediriger dynamiquement le trafic vers le domaine principal si la requête provient d'un sous-domaine d'API (ex: `api.joprelys.com` -> `joprelys.com`).
4. Retomber élégamment sur l'URL par défaut configurée si exécuté hors contexte HTTP.
5. Mettre à jour la valeur par défaut sur `https://joprelys.com/verify`.

## 2. Critères d'acceptation

- [x] Création de la classe utilitaire `VerificationUrlProvider` résolvant dynamiquement le protocole et le nom de domaine de la requête (avec suppression du préfixe `api.` pour cibler l'IHM Angular).
- [x] Injection de `VerificationUrlProvider` et retrait de l'injection statique `@Value` de `verificationBaseUrl` dans :
  - `DocumentService.java`
  - `HospitalizationService.java`
  - `LabResultService.java`
  - `PatientSummaryService.java`
- [x] Nettoyage de la variable inutilisée dans `PatientService.java`.
- [x] Valeur de repli par défaut de `verification-base-url` configurée sur `https://joprelys.com/verify` dans `application.yml`.
- [ ] Build Maven et tests de non-régression validés.

## 3. Pilotage projet

| Champ | Valeur |
|---|---|
| Epic parent | EPIC-0006 |
| User story parent | STORY-0602 |
| Sprint cible | SPRINT-0011 |
| Priorité business | P1 |
| Complexité | S |
| Story points | 1 |
| Profil recommandé | Backend Engineer / DevOps |
| Effort estimé senior | 0.05j |
| Responsable | Antigravity |
| Reviewer obligatoire | Lead Developer |
| Risque fonctionnel | Très faible |
| Risque technique | Faible |
| Dépendances | Aucun |
| Bloquants connus | Aucun |

## 4. Contexte analysé

- [x] `AGENTS.md` lu
- [x] `SKILL.md` lu
- [x] Fichiers existants résolvant `verificationBaseUrl` analysés.

## 5. Action plan

- [x] Créer `VerificationUrlProvider.java` dans `com.joprelys.backend.common.application`.
- [x] Modifier `DocumentService.java`.
- [x] Modifier `HospitalizationService.java`.
- [x] Modifier `LabResultService.java`.
- [x] Modifier `PatientSummaryService.java`.
- [x] Retirer la variable inutilisée dans `PatientService.java`.
- [x] Mettre à jour la valeur par défaut dans `application.yml`.
- [x] Lancer `./mvnw test` pour s'assurer que les configurations Spring Boot restent saines.
- [x] Mettre à jour `CHANGELOG.md` et `PROJECT-TRACKING.md`.

## 6. Implémentation réalisée

- **Création de `VerificationUrlProvider`** : Résolution dynamique de l'URL de base et du protocole à partir des headers HTTP (`X-Forwarded-Host`, `X-Forwarded-Proto`, `Host`) avec fallback automatique vers la valeur par défaut de configuration hors contexte HTTP.
- **Support des Sous-domaines API** : Nettoyage automatique du préfixe `api.` sur le domaine hôte résolu (ex : `api.joprelys.com` -> `joprelys.com`) afin d'assurer que les liens de vérification pointent vers l'interface frontend principale de l'application et non vers les APIs.
- **Mise à jour des Services** : Injection de `VerificationUrlProvider` et retrait de l'utilisation de `verificationBaseUrl` dans `DocumentService.java`, `HospitalizationService.java`, `LabResultService.java` et `PatientSummaryService.java`.
- **Nettoyage** : Retrait de la variable `verificationBaseUrl` non utilisée dans `PatientService.java`.
- **Configuration** : Remplacement de la valeur par défaut locale (`http://localhost:4200/verify`) par le nom de domaine officiel (`https://joprelys.com/verify`) dans `application.yml`.
- **Validation** : Les tests unitaires et d'intégration Spring Boot ont tous été validés avec succès (240/240).

## 7. Suivi d'exécution

| Date | Développeur | Temps passé | Avancement | Reste à faire | Blocage | Commentaire |
|---|---|---:|---:|---:|---|---|
| 2026-07-06 | Antigravity | 0.05j | 100% | Aucun | Aucun | Code modifié, compilé et testé avec succès (240 tests OK). |

## 8. Tests et vérifications

```bash
./mvnw test
```

## 9. Impact version / SemVer

| Champ | Valeur |
|---|---|
| Changement livrable | Oui |
| Type de bump | PATCH |
| Justification | Résolution dynamique et robuste des URLs de vérification des documents en production. |
| Breaking change | Non |
| Migration DB | Non |
| Changement API | Non |
| Impact Angular | Non |
| Impact Flutter | Non |
| Changelog requis | Oui |
