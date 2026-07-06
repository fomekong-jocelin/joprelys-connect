# TICKET-UI-OTP-AUTOFILL — Pré-remplissage automatique des codes OTP sur le front-end pour la production/dev

## 1. Objectif
Permettre la configuration de l'exposition des codes OTP au front-end depuis le fichier `application.yml` du backend (propriété `expose-otp-to-frontend`). Lorsque cette option est active, les codes OTP générés pour la connexion du personnel et du patient sont retournés dans les réponses HTTP respectives afin de pré-remplir automatiquement les formulaires de saisie OTP sur l'interface Angular. Cela simplifie la recette et l'utilisation de l'application en l'absence de serveurs de messagerie (SMS/Email) configurés.

## 2. Critères d'acceptation
- [x] Ajout de la propriété `joprelys.security.expose-otp-to-frontend` (défaut `true`) dans `application.yml`.
- [x] Injection de la propriété et exposition du code OTP dans `LoginResponse` lors de la connexion du personnel (staff) nécessitant un OTP.
- [x] Modification de la route `/api/public/patient/auth/otp` pour retourner une réponse de type `PatientOtpResponse` contenant le code OTP si l'exposition est active.
- [x] Mise à jour du modèle frontend `LoginResponse` dans `auth.models.ts` pour inclure la propriété optionnelle `otpCode`.
- [x] Mise à jour de `PatientPortalService` pour que la méthode `requestOtp` retourne un objet `{ otpCode?: string }`.
- [x] Pré-remplissage automatique des inputs OTP dans les formulaires de connexion staff et patient (dans `login.component.ts` et `patient-login.component.ts`) lorsque le code est retourné par l'API.

## 3. Pilotage projet

| Champ | Valeur |
|---|---|
| Epic parent | |
| User story parent | |
| Sprint cible | SPRINT-0011 |
| Priorité business | P1 |
| Complexité | S |
| Story points | 2 |
| Profil recommandé | Intermédiaire / Senior |
| Effort estimé senior | 2h |
| Effort estimé intermédiaire | 4h |
| Effort estimé junior | 8h |
| Responsable | Antigravity |
| Reviewer obligatoire | User |
| Risque fonctionnel | Faible |
| Risque technique | Faible |
| Dépendances | Aucun |
| Bloquants connus | Aucun |

## 4. Contexte analysé
- [x] `AGENTS.md` lu
- [x] `SKILL.md` lu
- [x] `README-IA.md` lu
- [x] `WORKFLOW-IA.md` lu
- [x] `PROJECT-TRACKING.md` lu
- [x] `CHANGELOG.md` lu
- [x] `review-checklist.md` lu
- [x] Code existant analysé (AuthenticationService, PatientAuthService, LoginComponent, PatientLoginComponent)

## 5. Hypothèses
- La propriété de configuration `joprelys.security.expose-otp-to-frontend` est activée par défaut pour faciliter le déploiement et la phase de test initiale sans infrastructure de mail/SMS.
- Si désactivée, l'application fonctionne normalement mais les codes ne sont plus envoyés à l'interface (ils restent imprimés dans la console du backend).

## 6. Risques et impacts

| Risque | Impact | Mitigation |
|---|---|---|
| Fuite d'OTP en production réelle | Risque de contournement du 2FA par interception | L'exposition de l'OTP doit être explicitement désactivée en production via la variable d'environnement `JOPRELYS_EXPOSE_OTP=false`. |

## 7. Action plan
- [x] Ajouter le paramètre de configuration dans le fichier `application.yml`.
- [x] Modifier `LoginResponse` dans le backend pour inclure `otpCode`.
- [x] Mettre à jour `AuthenticationService` pour retourner le code.
- [x] Modifier `PatientAuthService` et `PatientAuthController` pour retourner le code dans `PatientOtpResponse`.
- [x] Mettre à jour `auth.models.ts` et `patient-portal.service.ts` côté Angular.
- [x] Mettre à jour `login.component.ts` et `patient-login.component.ts` pour pré-remplir l'input.
- [x] Exécuter le build complet de l'application et les tests pour s'assurer de l'absence de régression.
- [x] Mettre à jour `PROJECT-TRACKING.md` et `CHANGELOG.md`.

## 8. Implémentation réalisée
- **Backend** :
  - Ajout de `joprelys.security.expose-otp-to-frontend: ${JOPRELYS_EXPOSE_OTP:true}` dans `application.yml`.
  - Modification du record `LoginResponse` avec un nouveau champ `otpCode` et ajout d'un constructeur alternatif à 7 arguments pour la rétrocompatibilité des tests existants.
  - Mise à jour de `AuthenticationService` avec injection du paramètre `@Value` et retour du code pour le personnel ayant des rôles sensibles.
  - Modification de `PatientAuthService.generateAndSendOtp` pour renvoyer le code généré sous forme de chaîne de caractères.
  - Modification de `PatientAuthController.requestOtp` pour renvoyer un record `PatientOtpResponse(String otpCode)` contenant le code uniquement si `exposeOtpToFrontend` est actif.
- **Frontend** :
  - Ajout de `readonly otpCode?: string;` dans l'interface `LoginResponse` de `auth.models.ts`.
  - Modification de la signature de `requestOtp` dans `PatientPortalService` pour typer le retour en `Observable<{ otpCode?: string }>`.
  - Mise à jour de `LoginComponent` pour intercepter `otpCode` et le pré-remplir dans `staffOtpCode` (pour le staff) ou `otpCode` (pour le patient).
  - Mise à jour de `PatientLoginComponent` pour récupérer `otpCode` et le pré-remplir dans le signal local `otpCode`.

## 9. Suivi d'exécution

| Date | Développeur | Temps passé | Avancement | Reste à faire | Blocage | Commentaire |
|---|---|---:|---:|---:|---|---|
| 2026-07-06 | Antigravity | 0.15j | 100% | Aucun | Aucun | Implémentation complète et build validé |

## 10. Tests et vérifications

### Commandes exécutées ou à exécuter

```bash
# Backend unit tests
./mvnw test # OK

# Frontend build & unit tests
npm run build # OK
npm run test # OK
```

### Résultats

- [x] Tests unitaires OK
- [x] Build OK
- [x] Analyse statique OK

## 11. Documentation

- [x] Changelog mis à jour
- [x] Suivi projet mis à jour

## 12. Reste à faire
Aucun.

## 13. Statut final
Statut : DONE

## 14. Notes finales
L'exposition des codes OTP est active par défaut pour les tests et la production temporaire, et peut être facilement désactivée via la variable d'environnement `JOPRELYS_EXPOSE_OTP=false`.

## 15. Impact version / SemVer

| Champ | Valeur |
|---|---|
| Changement livrable | Oui |
| Type de bump | PATCH |
| Justification | Ajout d'une fonctionnalité de configuration pour le contournement OTP en phase de recette/production et auto-remplissage des formulaires associés. |
| Breaking change | Non |
| Migration DB | Non |
| Changement API | Oui (Ajout de propriétés de réponse non bloquantes) |
| Impact Angular | Oui |
| Impact Flutter | Non |
| Changelog requis | Oui |
| Release note requise | Non |

## 16. Impact thème / i18n / branding
- [x] Impact Angular UI analysé
- [x] Aucun texte ou branding hardcodé prévu
