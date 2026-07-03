# FUNCTIONAL-SPEC — Portail Patient (EPIC-0008)

## 1. Résumé métier

Permettre aux patients du réseau Joprelys Connect d'accéder à leur espace personnel sécurisé afin de consulter leur dossier médical (DPU), de voir l'historique de leurs consultations, et de télécharger en toute autonomie leurs ordonnances PDF, tout en gérant le consentement d'accès de leurs données de santé pour les établissements de soins.

## 2. Objectifs

- Offrir un accès direct et autonome au patient à ses données de santé de manière sécurisée.
- Réduire la charge administrative des cliniques pour la transmission d'ordonnances.
- Donner le contrôle au patient sur le partage de ses données médicales (consentement).

## 3. Utilisateurs / acteurs concernés

| Acteur | Besoin | Droits / limites |
|---|---|---|
| **Patient** | Se connecter, voir son profil, son historique et télécharger ses ordonnances. Gérer son consentement. | Rôle `PATIENT` uniquement. Accès limité à son propre DPU uniquement. |
| **Médecin / Infirmier** | Consulter le DPU d'un patient si le consentement est valide. | Rôle `MEDECIN` / `INFIRMIER`. Interdit d'accès si le patient a révoqué son consentement (sauf procédure d'urgence). |

## 4. Périmètre

### Inclus

- Connexion sécurisée du patient (Simulé par couple E-mail / N° DPU + Code OTP de validation imprimé en console).
- Tableau de bord patient avec rappel des informations personnelles, allergies et antécédents.
- Historique chronologique des visites clôturées avec nom de la clinique, médecin, date de visite, et lien de téléchargement du PDF de consultation.
- Écran de gestion du consentement : liste des cliniques du réseau avec commutateur Activer / Désactiver l'accès à son DPU.

### Exclus

- Modification directe des antécédents ou allergies par le patient (réservé aux praticiens cliniques).
- Prise de rendez-vous en ligne ou téléconsultation.

## 5. Parcours utilisateur (Connexion & Consultation)

1. Le patient accède à la page `/patient/login`.
2. Il saisit son **N° DPU** et son **E-mail**.
3. Le système vérifie la correspondance et génère un code OTP à 6 chiffres (simulé en console backend).
4. Le patient saisit l'OTP et accède à son espace `/patient/dashboard`.
5. Il y voit sa fiche d'identité (nom, âge, allergies) et la liste de ses ordonnances.
6. Il peut cliquer sur "Télécharger PDF" pour chaque consultation finalisée.

## 6. Règles métier

| ID | Règle | Priorité | Source |
|---|---|---|---|
| BR-PAT-001 | Un patient ne peut accéder qu'aux données rattachées à son propre identifiant de DPU (isolation stricte). | P0 | Spécification de sécurité |
| BR-PAT-002 | L'OTP de connexion a une validité de 5 minutes. | P1 | Sécurité |
| BR-PAT-003 | Par défaut, la clinique pilote ayant créé le dossier du patient dispose du consentement d'accès. | P0 | RGPD |
| BR-PAT-004 | Une clinique révoquée par le patient ne peut plus lire son historique médical (sauf via le bouton d'accès d'urgence "Brise-Glace" tracé en audit log). | P0 | Consentement |

## 7. Critères d’acceptation

- [ ] Route `/patient/login` accessible publiquement.
- [ ] Route `/patient/dashboard` protégée par rôle `PATIENT`.
- [ ] Le patient connecté voit son profil DPU : nom complet, N° DPU, date de naissance, téléphone, allergies connues, antécédents.
- [ ] Le patient voit la liste chronologique de ses visites clôturées avec date, établissement, médecin et un bouton "Télécharger Ordonnance".
- [ ] Un patient ne peut en aucun cas modifier ses données de profil.
- [ ] La modale de consentement permet de désactiver ou d'activer l'accès au DPU pour chaque clinique.

## 8. Cas limites / erreurs attendues

| Cas | Comportement attendu |
|---|---|
| Tentative d'accès à un autre DPU via l'URL | Retourner `403 Forbidden` (Vérification du Token JWT par rapport à l'identifiant demandé). |
| Code OTP erroné ou expiré | Message d'erreur et invitation à régénérer le code. |
| Clinique révoquée tente de charger le DPU d'un patient | Le backend lève une exception `403 FORBIDDEN` avec le code `CONSENT_REQUIRED`. |
| Procédure d'urgence Brise-Glace (Break-Glass) | Un médecin ou infirmier peut forcer l'accès temporaire (15 minutes) au DPU en saisissant une justification médicale obligatoire, ce qui génère un audit log critique de type `EMERGENCY_ACCESS` et affiche un bandeau d'alerte rouge sur le dossier. |

## 9. Textes / i18n

| Clé | Français | English |
|---|---|---|
| `patient.login.title` | Espace Patient Sécurisé | Secure Patient Portal |
| `patient.login.dpu` | Numéro DPU | UPR Number |
| `patient.login.email` | Adresse E-mail | Email Address |
| `patient.login.submit` | Recevoir le code de sécurité | Request Security Code |
| `patient.login.otp` | Code de sécurité (OTP) | Security Code (OTP) |
| `patient.login.verify` | Se connecter | Log In |
| `patient.dashboard.title` | Mon Espace Santé | My Health Portal |
| `patient.dashboard.consent` | Consentements d'accès | Access Consents |
| `patient.consent.revoke` | Révoquer l'accès | Revoke Access |
| `patient.consent.grant` | Accorder l'accès | Grant Access |

## 10. Impacts UI / branding

| Point | Impact |
|---|---|
| Nom de l’app | Non |
| Logo | Oui (Affiché sur la page de login patient) |
| Thème light/dark | Oui |
| Composants réutilisables | Oui (Boutons, cartes standard) |

## 10.1 Amélioration UI premium du dashboard patient (2026-07-03)

Objectif complémentaire : rendre `/patient/dashboard` plus premium et plus confortable à lire sans changer le périmètre fonctionnel.

Critères UX complémentaires :

- Le patient doit identifier immédiatement son nom, son DPU, ses informations clés et les sections disponibles.
- Le DPU doit rester lisible même s'il est long.
- L'historique des consultations doit être plus compact et hiérarchisé : établissement, date, médecin, diagnostic, mesures, détails cliniques, document.
- Les informations médicales ne doivent pas s'appuyer sur des emojis décoratifs.
- Les couleurs doivent rester sobres et limitées aux accents utiles : action principale, alerte/allergie, information médicale.
- Le layout desktop doit mieux utiliser la largeur disponible tout en conservant une pile claire sur mobile.

## 11. Hypothèses et questions ouvertes

- Pour la version locale/démo, le code OTP généré sera affiché dans les logs du backend (stdout) sous la forme `[OTP PATIENT] Code de connexion pour DPU XXX : 123456`.

## 12. Historique des mises à jour

| Date | Auteur | Changement |
|---|---|---|
| 2026-07-02 | Antigravity | Création initiale de la spec fonctionnelle |
| 2026-07-03 | Codex | Ajout des critères UX premium du dashboard patient |
