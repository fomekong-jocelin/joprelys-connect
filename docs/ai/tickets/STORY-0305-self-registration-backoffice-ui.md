# STORY-0305 — Tableau de Bord d'Accueil & Validation Back-office

## 1. Objectif

Créer l'interface back-office Angular pour l'agent d'accueil authentifié afin de lister, réviser, fusionner, valider ou rejeter les demandes de pré-enregistrement faites par QR Code, et de générer automatiquement la fiche d'admission physique.

## 2. Critères d'acceptation

- [x] **Badge indicateur** : Affichage d'un badge rouge indiquant le nombre de pré-enregistrements en attente sur l'onglet de navigation "Admissions" dans la Sidebar.
- [x] **Tableau de gestion** :
  - Liste paginée des demandes `AWAITING_VALIDATION`.
  - Filtre par statut (En attente, Validé, Rejeté).
- [x] **Tiroir de détails (Drawer)** :
  - Ouverture des détails d'une demande sélectionnée.
  - Affichage comparatif side-by-side si le patient existe ou présente une forte similarité en base (score calculé par le backend).
- [x] **Réconciliation assistée** :
  - Option "Valider comme nouveau patient".
  - Option "Fusionner avec le dossier existant de [Nom Patient]" (met à jour le patient existant avec les nouvelles coordonnées validées).
- [x] **Génération & Impression** :
  - Après validation réussie, ouverture automatique ou téléchargement du PDF de la fiche d'admission générée contenant les mentions légales et le QR Code de visite.
- [x] **Traductions (i18n)** : Localisation FR/EN complète.
- [x] **Tests** : Suite de tests unitaires Vitest.

## 3. Pilotage projet

| Champ | Valeur |
|---|---|
| Epic parent | EPIC-0003 — Dossier Patient Unique (DPU) & Recherche |
| Sprint cible | SPRINT-0012 |
| Priorité business | P1 |
| Complexité | M |
| Story points | 3 |
| Profil recommandé | Frontend UI Engineer (Intermédiaire) |
| Effort estimé senior | 1.5j |
| Effort estimé intermédiaire | 2.5j |
| Effort estimé junior | 4.0j |
| Responsable | Antigravity |
| Reviewer obligatoire | Lead Developer |
| Risque fonctionnel | Moyen |
| Risque technique | Moyen |
| Dépendances | STORY-0303, STORY-0304 |
| Bloquants connus | Aucun |

## 4. Definition of Ready

- Conception de l'IHM et règles de fusion validées dans `TECHNICAL-DESIGN.md`.
- Endpoints de validation et de listing backend prêts.

## 5. Definition of Done

- Code livré et build Angular passant sans erreur (`npm run build`).
- Tests unitaires Vitest au vert.
- Intégration i18n FR/EN complète.

## 6. Action plan

- [x] Créer le composant `PreRegistrationsListComponent` et son template HTML.
- [x] Ajouter la route back-office `/clinic/admissions/pre-registrations` sécurisée par guard de rôle.
- [x] Mettre à jour `AppShellNavComponent` pour ajouter le badge dynamique au menu "Admissions".
- [x] Mettre en place la liste des demandes dans le composant d'admissions.
- [x] Intégrer le tiroir de détails avec l'alerte de doublons potentiels et le panneau de fusion side-by-side.
- [x] Connecter le bouton de téléchargement du PDF de la fiche d'admission après l'action de validation.
- [x] Ajouter les chaînes i18n dans `fr.json` et `en.json`.
- [x] Écrire les tests unitaires dans `pre-registrations-list.component.spec.ts`.

## 7. Statut final

Statut : DONE
