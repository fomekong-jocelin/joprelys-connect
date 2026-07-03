# Comptes de Démo & Rôles Joprelys Connect

Ce document liste les comptes de test pré-initialisés et les procédures pour obtenir des accès sur les différents écrans de l'application.

---

## 1. Comptes pré-initialisés (Seeded)

Lors du démarrage de l'application backend, les comptes de test suivants sont automatiquement créés en base de données (se référer à [AdminUserSeeder.java](file:///C:/MES-APPLICATIONS/joprelys-connect/backend/src/main/java/com/joprelys/backend/auth/application/AdminUserSeeder.java)) :

| Rôle | Email | Mot de passe | Description |
|---|---|---|---|
| **ADMIN_JOPRELYS** | `admin@joprelys.local` | `Admin@12345` | Super-administrateur de Joprelys HealthTech. Gère les Cliniques Pilotes (organisations) et consulte le journal d'audit global. |
| **MEDECIN** | `medecin@joprelys.local` | `Admin@12345` | Praticien clinique de test rattaché à la *Clinique Joprelys*. Peut créer des visites, des prescriptions, et des demandes d'examens. **Utilisé également comme rôle de secours (fallback) pour accéder et tester le portail laboratoire.** |
| **PHARMACIEN** | `pharmacien@joprelys.local` | `Admin@12345` | Pharmacien de test rattaché à la *Clinique Joprelys*. Peut utiliser le portail pharmacie pour vérifier des ordonnances et saisir des dispensations de médicaments. |

---

## 2. Accès aux écrans Laboratoire (Biologiste)

Un rôle spécifique et dédié **BIOLOGISTE** ou **LABORATOIRE** n'est pas encore finalisé/tranché dans le périmètre actuel (post-MVP).

* **Comment tester les écrans laboratoire (`/clinic/lab-orders`) ?**
  Connectez-vous avec le compte **MEDECIN** (`medecin@joprelys.local` / `Admin@12345`) ou un compte **ADMIN_CLINIQUE**. Ces deux rôles disposent des habilitations requises pour accéder au tableau de bord des demandes d'examens biologiques, effectuer les saisies de résultats multi-analytes et téléverser le PDF de résultat.

---

## 3. Rôles dynamiques (Par invitation)

Pour tester les autres rôles du personnel de clinique, connectez-vous avec un compte **Administrateur Clinique** (`ADMIN_CLINIQUE`) sur son espace, puis invitez des membres via l'écran **Gestion du Personnel** (`/clinic/staff`).

### Comment obtenir un compte ADMIN_CLINIQUE ?
1. Connectez-vous en tant que **Super-administrateur** (`admin@joprelys.local`).
2. Allez sur l'écran **Cliniques Pilotes** (`/organizations`).
3. Créez un administrateur pour l'organisation (ou lors de la création d'une nouvelle clinique).
4. Définissez son adresse e-mail. Le mot de passe temporaire sera généré et s'affichera à l'écran.

### Rôles invitables depuis le portail Administrateur Clinique :
* **INFIRMIER** : Saisie des constantes vitales, historique patient.
* **AGENT_ACCUEIL** : Enregistrement de nouveaux patients et ouverture des visites.
* **MEDECIN** / **PHARMACIEN** (supplémentaires).

---

## 4. Accès au Portail Patient (`/patient/dashboard`)

Le portail patient n'utilise pas de mot de passe traditionnel, mais une authentification par **Code OTP** à deux étapes (simulée).

### Procédure de connexion pour un Patient :
1. Créez un patient en vous connectant avec un rôle **AGENT_ACCUEIL** ou **MEDECIN** sur la clinique.
2. Notez le **N° DPU** généré (ex: `DPU-20260703-0001`), le numéro de **téléphone**, et la **date de naissance** saisie.
3. Allez sur la page de connexion, basculez sur l'onglet **Patient**.
4. Remplissez le formulaire avec ces informations et cliquez sur **Recevoir un code OTP**.
5. Le code de connexion OTP (à 6 chiffres) est imprimé directement dans la console d'exécution du backend Spring Boot sous la forme :
   `[OTP PATIENT] Code de connexion pour DPU DPU-XXXXXX-XXXX : XXXXXX`
6. Saisissez ce code dans la deuxième étape pour accéder au tableau de bord patient.
