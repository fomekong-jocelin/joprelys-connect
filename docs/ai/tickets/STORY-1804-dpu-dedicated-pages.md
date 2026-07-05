# STORY-1804 — Découpage du Dossier Patient Unique (DPU) en Vues Dédiées

## 1. Objectif

Réorganiser la fiche patient (`patient-detail.component.ts`) pour éviter de concentrer toutes les informations médicales (Identité, Consultations, Hospitalisations, Examens, Audits, Révocations) sur un seul et unique écran surchargé, en introduisant un routage par sous-pages dédiées (routes enfants).

## 2. Critères d'acceptation

- [ ] La route `/patients/:id` est découpée en sous-routes enfants de navigation :
  - `/patients/:id/profile` (Fiche d'identité, contact et antécédents/allergies).
  - `/patients/:id/consultations` (Historique des consultations et prescriptions).
  - `/patients/:id/hospitalizations` (Suivi des hospitalisations et notes d'évolution).
  - `/patients/:id/lab-orders` (Demandes d'examens et graphique des résultats biologiques).
  - `/patients/:id/audit-trail` (Historique des accès et sécurité).
- [ ] Une barre d'onglets de navigation horizontale (Sub-navigation) est ajoutée en haut du détail du patient pour basculer facilement entre ces sections par routage d'URL, remplaçant la gestion d'états interne complexe.
- [ ] Les composants de formulaires et de détails respectent scrupuleusement la charte graphique Material 3 avec des rayons d'arrondis de 4px à 8px maximum.
- [ ] Le code du composant monolithique d'origine (actuellement volumineux) est découpé en composants spécialisés réutilisables ou enfants pour respecter la limite de 500 lignes.
- [ ] Tous les libellés et textes de navigation respectent l'internationalisation FR/EN.

## 3. Pilotage projet

| Champ | Valeur |
|---|---|
| Epic parent | EPIC-0012 — Refonte UI/UX Premium |
| User story parent | N/A |
| Sprint cible | SPRINT-0009 |
| Priorité business | P1 |
| Complexité | L |
| Story points | 5 |
| Profil recommandé | Senior / Lead Developer |
| Effort estimé senior | 0.6j |
| Effort estimé intermédiaire | 1.0j |
| Effort estimé junior | 1.7j |
| Responsable | Frontend Agent |
| Reviewer obligatoire | Antigravity |
| Risque fonctionnel | Moyen |
| Risque technique | Moyen |
| Dépendances | STORY-1802, STORY-1803 |
| Bloquants connus | Aucun |

## 4. Action plan

- [ ] Mettre à jour `app.routes.ts` pour introduire les routes enfants sous `/patients/:id`.
- [ ] Refactoriser `patient-detail.component.ts` en extrayant les onglets actuels dans des composants autonomes associés à chaque route enfant.
- [ ] Mettre en place la navigation par onglets reliée au Router Angular.
- [ ] S'assurer que le chargement des données patient (résolu par resolver ou service) reste fluide et sans doublon d'appels API.
- [ ] Valider le support du thème light/dark et le multilinguisme.
- [ ] Exécuter les tests unitaires frontend existants et les adapter aux nouvelles routes.
