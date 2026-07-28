# EPIC-0024 — Assistant vocal IA de consultation

## 1. Objectif métier

Réduire la charge de saisie sans déléguer la décision clinique ni la validation
du dossier médical à un modèle.

## 2. Périmètre

### Inclus

- QR de visite active et scan mobile.
- Dictée segmentée et alternative texte.
- Brouillon interactif, corrections et validation médecin.
- FR/EN, light/dark, accessibilité et mode manuel.
- Feature flag, sécurité tenant et évaluation clinique.

### Exclus

- Diagnostic/prescription autonome.
- Streaming temps réel v1.
- Historique durable.
- Flutter v1.
- Production avant revue DPO/médicale.

## 3. User stories

| ID | Titre | P | SP | Statut | Dépendances |
|---|---|---:|---:|---|---|
| STORY-2500 | Cadrage clinique/conformité | P0 | 3 | IN_PROGRESS | médecin + DPO |
| STORY-2501 | Ports IA/adaptateur approuvé | P0 | 5 | IN_PROGRESS | 2500 |
| STORY-2502 | Sessions de brouillon | P0 | 8 | BLOCKED | 2500, 2501 |
| STORY-2503 | QR + scanner mobile | P0 | 5 | TODO | 2505 |
| STORY-2504 | Panneau vocal Angular | P0 | 8 | BLOCKED | 2502, 2503, refactor |
| STORY-2505 | QR visite backend | P0 | 3 | DONE | aucune |
| STORY-2506 | Docs/sécurité/observabilité | P0 | 5 | IN_PROGRESS | 2500 |
| STORY-2507 | E2E/évaluation/pilote | P0 | 5 | TODO | toutes |
| TASK-20260728-P0-VOICE-HOTFIX-B | Realtime conversationnel contrôlé, Dictée passive, correction et finalisation sans perte | P0 | 5 | IMPLEMENTED — RECETTE CLINIQUE REQUISE | 2502, 2504 |

## 4. Estimation globale

| Élément | Valeur |
|---|---|
| Total | 42 SP |
| Senior | 25–33 j |
| Intermédiaire | 34–45 j |
| Sprints | 3–4 indicatifs |
| Capacité | non confirmée ; ne pas engager SPRINT-0015 |

## 5. Definition of Ready

- [x] Documentation initiale.
- [x] Critères et tests attendus.
- [ ] DPO/fournisseur/rétention/résidence.
- [ ] Corpus anonymisé et seuils qualité.
- [ ] Capacité et assignations.

## 6. Contraintes

- Tailwind v4, aucun Angular Material.
- Proxy Angular et URLs `/api` relatifs.
- `DESIGN.md`, light/dark, FR/EN, rayons sobres.
- Refactor consultation/dashboard avant UI.
- Maven et `application.yml`.

## 7. Impact SemVer

MINOR (`0.11.0`) uniquement pour un incrément activable et validé. Le cadrage
et les fondations désactivées n’imposent pas de bump.

## 8. Risques

- Données de santé chez un sous-traitant.
- Transcription incorrecte/hallucination.
- Divulgation sonore par TTS.
- Latence/coût/quota.
- Composants Angular monolithiques.
- Capacité sprint non disponible.
- Perte d'un tour si la finalisation détruit une file non drainée.
- Divulgation ou réinjection audio si une vocalisation automatique subsiste.

## 9. Hotfix P0 du 2026-07-28

Le hotfix consomme 2 à 4 jours senior hors capacité fonctionnelle et bloque les
évolutions vocales non critiques jusqu'à la recette médecin. Les gates Angular,
i18n, build et Maven complet (816 tests) sont verts. Il ne modifie pas
l'estimation historique de l'epic ; il représente une charge corrective
distincte de 5 SP.
