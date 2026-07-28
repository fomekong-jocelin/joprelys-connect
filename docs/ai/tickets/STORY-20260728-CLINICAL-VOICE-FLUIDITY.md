# STORY-20260728-CLINICAL-VOICE-FLUIDITY — Refonte UX Expérience Médecin : Dictée Fluide, Ondes Animées & Injection 1-Clic

## Métadonnées

| Champ | Valeur |
|---|---|
| Type | Feature UX / Ergonomie Clinique |
| Priorité | P0 — Expérience Utilisateur |
| Statut | DONE |
| Sprint | SPRINT-0014 |
| Date | 2026-07-28 |
| Profil recommandé | Senior Frontend Angular / UX Health Specialist |

---

## 1. Vision Métier & Alignement Médecin

En consultation, le médecin doit consacrer 100 % de son attention visuelle et cognitive au patient. L'assistant IA ne doit pas être un outil d'administration complexe avec des clics multiples, mais un copilote invisible et fluide.

### Principes directeurs UX Médecin :
1. **Écoute vivante & réactive (Ondes/Vagues)** : L'animation du microphone réassure immédiatement le médecin par des ondes fluides et pulsatiles reflétant la capture vocale en temps réel (`VoiceWaveVisualizerComponent`).
2. **Zéro clic répété (Tout valider en 1 clic)** : Suppression du piège de validation "champ par champ". Bouton global `Tout valider (X modifications en 1 clic)` pour injecter d'un coup l'ensemble des révisions dans le dossier.
3. **Élimination du jargon technique** : Masquage dans un accordéon pliable discret (`<details>`) de la "Note clinique sourcée / SHA-256".

---

## 2. Découpage technique (Tasks & Subtasks)

### Task 1 : Visualiseur audio à vagues / ondes fluides (`VoiceWaveVisualizerComponent`)
- [x] Création du visualiseur d'ondes sonores fluides et organiques réagissant dynamiquement au signal `audioLevel()`.
- [x] Intégration dans le contrôleur Realtime avec gradient et pulsabilité réactive.

### Task 2 : Bouton "Tout valider (1 clic)" (`AiProposalPanelComponent`)
- [x] Ajout des méthodes `allPendingRevisions()` et `acceptAllPending()`.
- [x] Bouton principal `Tout valider (X en 1 clic)` au sommet du panneau pour vider l'ensemble des révisions accumulées d'un coup.

### Task 3 : Masquage du bloc d'audit technique ("Note clinique sourcée")
- [x] Insertion de `<app-linked-evidence-note-panel>` dans un accordéon pliable discret `<details>` intitulé *"Traçabilité & Preuves cliniques (Optionnel)"*.

---

## 3. Critères d'acceptation (Definition of Done)

- [x] L'enregistrement affiche une animation d'ondes sonores fluides et naturelles pendant l'écoute.
- [x] Le médecin peut appliquer TOUTES les propositions générées en UN SEUL clic.
- [x] Le bloc technique "Note clinique sourcée" ne pollue plus la vue principale du formulaire.
- [x] Le build production et les tests unitaires Angular sont 100% verts (499/499 tests).
