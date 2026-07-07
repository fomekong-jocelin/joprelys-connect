# STORY-0304 — Formulaire Mobile Public de Pré-enregistrement (Self-Registration)

## 1. Objectif

Créer l'interface utilisateur Angular publique et responsive (mobile-first) accessible par scan de QR code sur le comptoir. L'interface doit permettre de guider le patient dans la saisie de ses coordonnées et intégrer la validation par captcha médical localisé.

## 2. Critères d'acceptation

- [x] **Accessibilité publique** : Route publique Angular `/public/register?orgId={uuid}` accessible sans être connecté.
- [x] **Design System & Thème** :
  - Respect de `docs/standards/UI-RADIUS-AND-SHADOW-STANDARDS.md` (coins sobres, ombres douces).
  - Utilisation exclusive de Tailwind CSS v4 CSS-first avec support light/dark.
  - Absence stricte d'Angular Material.
- [x] **Sélection du profil** : Choix initial clair entre "Nouvelle admission" ou "Déjà venu / Autre hôpital".
- [x] **Formulaire de saisie** :
  - Champs d'identité (nom, prénom, date de naissance, genre, groupe sanguin).
  - Coordonnées de contact (téléphone optionnel, email optionnel, adresse).
  - Personne à contacter en cas d'urgence (nom, téléphone, lien de parenté).
- [x] **Captcha Médical** :
  - Récupération de la question de captcha médical auprès du backend.
  - Saisie de la réponse par l'utilisateur et validation au moment de la soumission.
- [x] **Traductions (i18n)** : Toutes les chaînes de l'interface doivent être localisées en français (`fr`) et anglais (`en`) sans texte codé en dur.
- [x] **Tests** : Suite de tests unitaires Vitest.

## 3. Pilotage projet

| Champ | Valeur |
|---|---|
| Epic parent | EPIC-0003 — Dossier Patient Unique (DPU) & Recherche |
| Sprint cible | SPRINT-0012 |
| Priorité business | P1 |
| Complexité | S |
| Story points | 3 |
| Profil recommandé | Frontend UI Engineer (Intermédiaire) |
| Effort estimé senior | 1.0j |
| Effort estimé intermédiaire | 1.5j |
| Effort estimé junior | 2.5j |
| Responsable | Antigravity |
| Reviewer obligatoire | Lead Developer |
| Risque fonctionnel | Moyen |
| Risque technique | Faible |
| Dépendances | STORY-0303 |
| Bloquants connus | Aucun |

## 4. Definition of Ready

- Endpoints de captcha et de pré-enregistrement backend documentés ou mockés.
- DESIGN.md respecté.

## 5. Definition of Done

- Code livré et build Angular passant sans erreur (`npm run build`).
- Tests unitaires Vitest au vert (`npm run test`).
- Intégration i18n FR/EN complète.

## 6. Action plan

- [x] Créer la route publique dans `app.routes.ts`.
- [x] Créer le composant `PatientSelfRegistrationComponent` et séparer le template HTML.
- [x] Mettre en place le service API Angular `PatientSelfRegistrationApiService` pour communiquer avec les endpoints publics.
- [x] Implémenter le formulaire avec validation réactive Angular.
- [x] Intégrer la carte dynamique de captcha médical (affichage de la question reçue de l'API, validation locale de base).
- [x] Ajouter les traductions correspondantes dans `fr.json` et `en.json`.
- [x] Écrire les tests unitaires dans `patient-self-registration.component.spec.ts`.

## 7. Statut final

Statut : DONE
