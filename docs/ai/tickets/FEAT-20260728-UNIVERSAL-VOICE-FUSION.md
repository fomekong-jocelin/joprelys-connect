# FEAT-20260728-UNIVERSAL-VOICE-FUSION — Évolution R1 : Micro Ambiant Universel Unifié (Note Clinique + Constantes)

## Métadonnées

| Champ | Valeur |
|---|---|
| Type | Architecture & Engineering — Évolution Majeure R1 |
| Priorité | P0 — Ergonomie Ambiante |
| Statut | DONE |
| Sprint | SPRINT-0014 |
| Date | 2026-07-28 |
| Profil recommandé | Senior Frontend Angular / Architecte Voice AI |

---

## 1. Vision Métier & Architecture Ambiante

Le médecin ne manipule **qu'un seul microphone universel** pendant la consultation. L'IA écoute l'échange médical de manière fluide et réalise l'extraction simultanée :
- De la **Note Clinique** (Symptômes, Examen, Diagnostic, Prescription, Examens de labo).
- Des **Constantes Vitales** (Température, Poids, Pouls, Tension systolique/diastolique, SpO2, Glycémie, Fréquence respiratoire, Échelle de douleur).

---

## 2. Plan d'Implémentation Technique

### Étape 1 : Unification du Draft IA (`AiConsultationDraft`)
- [x] Vérification de `AiConsultationDraft` : gestion intégrée de `symptoms`, `prescription`, `labOrders` et `vitals`.

### Étape 2 : Routage Unifié dans `consultation.component.ts`
- [x] Unification de la réception `applyAiDraft` dans `consultation.component.ts` : application simultanée du patch formulaire texte ET du staging de proposition de constantes `pendingVitalsProposal`.

### Étape 3 : Simplification du Template (`consultation.component.html`)
- [x] Confirmation de la présence d'un composant vocal unique maître `app-voice-assistant-panel` sur la page de consultation.

---

## 3. Critères d'acceptation (Definition of Done)

- [x] Un seul bouton micro universel contrôle la capture vocale sur la page de consultation.
- [x] Une dictée contenant à la fois des éléments cliniques et des constantes remplit simultanément la note et propose les constantes.
- [x] Tous les tests unitaires Angular (499/499) et le build de production sont 100% verts.
