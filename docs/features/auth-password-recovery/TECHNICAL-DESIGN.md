# Spécification Technique — Récupération de Mot de Passe Simplifiée (STORY-0103)

## 1. Architecture Backend

Le module d'authentification existant sera étendu pour intégrer le flux de récupération de mot de passe sous un contrôleur public (accessible sans authentification JWT).

### 1.1 Modèle de données & Persistance
Pour le MVP, la persistance des codes OTP se fera en mémoire via une `ConcurrentHashMap` dans la classe de service, similaire au mécanisme de `PatientAuthService`. Cela évite d'encombrer la base de données avec des codes temporaires à cycle de vie très court.

### 1.2 Endpoints API

#### A. Demande de réinitialisation
* **URL** : `POST /api/public/auth/password-recovery/request`
* **Accès** : Public (non authentifié)
* **Entrée** : `PasswordRecoveryRequest`
```json
{
  "email": "medecin@joprelys.local"
}
```
* **Sortie** : `200 OK` (Corps vide)
* **Comportement** :
  * Normalisation de l'e-mail (mise en minuscules et trim).
  * Recherche de l'utilisateur actif (`UserAccountEntity`).
  * Si trouvé et actif :
    * Génération d'un code OTP aléatoire à 6 chiffres.
    * Stockage dans la Map avec une date de création (`Instant.now()`) et un compteur de tentatives à `0`.
    * Log de l'OTP dans la console : `[PASSWORD RECOVERY] Code de réinitialisation pour l'e-mail {email} : {code}`.
  * Dans tous les cas (utilisateur trouvé ou non), renvoi d'un statut `200 OK` sans message d'erreur pour éviter l'énumération de comptes.

#### B. Validation & Changement de mot de passe
* **URL** : `POST /api/public/auth/password-recovery/reset`
* **Accès** : Public (non authentifié)
* **Entrée** : `PasswordResetRequest`
```json
{
  "email": "medecin@joprelys.local",
  "otpCode": "123456",
  "newPassword": "nouveauMotDePasseSecurise123"
}
```
* **Sortie** : `200 OK` (Corps vide)
* **Comportement** :
  * Validation des champs (Bean Validation : email, otpCode obligatoire, newPassword ≥ 8 caractères).
  * Normalisation de l'e-mail.
  * Récupération des données OTP en mémoire.
  * Si aucune demande ou OTP expiré (plus de 300 secondes) : `400 Bad Request` ("Aucune demande de réinitialisation active ou code expiré").
  * Si le code OTP est incorrect :
    * Incrémentation des tentatives.
    * Si tentatives >= 3 : suppression de l'OTP en mémoire, `400 Bad Request` ("Trop de tentatives incorrectes, veuillez recommencer").
    * Sinon : `400 Bad Request` ("Code de réinitialisation incorrect").
  * Si le code OTP est correct :
    * Recherche de l'utilisateur.
    * Hashage du nouveau mot de passe avec `PasswordEncoder` (BCrypt).
    * Mise à jour du `passwordHash` et sauvegarde.
    * Suppression de l'OTP en mémoire.

---

## 2. Architecture Frontend

### 2.1 Composant Angular
Un nouveau composant standalone `ForgotPasswordComponent` sera créé.
* **Route** : `/forgot-password` (redirection ou lien depuis `/` via la page de connexion).
* **Étapes de l'interface** :
  * **Étape 1** : Saisie de l'e-mail uniquement. Bouton "Envoyer le code".
  * **Étape 2** : Saisie du code de sécurité et du nouveau mot de passe (avec confirmation). Bouton "Modifier le mot de passe".
  * **Étape 3** : Message de succès et bouton de redirection vers la page de connexion `/`.

### 2.2 API Service
Le service existant `AuthApiService` sera enrichi des deux méthodes HTTP :
* `requestPasswordRecovery(email: string): Observable<void>`
* `resetPassword(request: PasswordResetRequest): Observable<void>`

---

## 3. Plan de Tests (Backend & Frontend)

### 3.1 Tests Backend (`PasswordRecoveryControllerTest`)
* `givenActiveUser_whenRequestRecovery_thenSuccessAndCodeLogged`
* `givenInactiveUser_whenRequestRecovery_thenSuccessButNoCodeLogged`
* `givenUnknownEmail_whenRequestRecovery_thenSuccessButNoCodeLogged`
* `givenValidOtp_whenResetPassword_thenPasswordUpdatedInDb`
* `givenExpiredOtp_whenResetPassword_thenBadRequest`
* `givenInvalidOtp_whenResetPassword_thenBadRequestAndAttemptsIncremented`
* `givenTooManyFailedAttempts_whenResetPassword_thenOtpDeleted`
* `givenShortPassword_whenResetPassword_thenValidationBadRequest`

### 3.2 Tests Frontend (`forgot-password.component.spec.ts`)
* Devra tester l'enchaînement des étapes, la validation des champs (format email, longueur mot de passe) et la redirection vers la page de connexion.
