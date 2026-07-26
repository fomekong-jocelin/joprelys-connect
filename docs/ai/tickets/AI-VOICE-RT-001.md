# AI-VOICE-RT-001 — Copilote clinique vocal Realtime gouverné

## Statut

- **Epic** : IA clinique / Consultation assistée
- **Type** : Feature sécurité + UX clinique
- **Priorité** : P0
- **Statut** : IMPLEMENTED — validation finale PR en cours
- **PR** : #153
- **Branche** : `agent/realtime-clinical-copilot-v3`

## Objectif

Transformer le copilote vocal de consultation en expérience temps réel naturelle, avec WebRTC, détection sémantique de fin de tour et interruption vocale, sans donner au fournisseur IA le contrôle des décisions ou de la persistance clinique.

Le modèle Realtime est une couche de transport/transcription/vocalisation. Le moteur métier Joprelys reste l'autorité pour parser, contrôler, proposer et faire valider les changements.

## Périmètre inclus

- WebRTC continu pour la consultation.
- Création de session Realtime côté backend ; aucune clé OpenAI durable dans le navigateur.
- Semantic VAD configurable.
- Barge-in : interruption immédiate d'une réponse vocale lorsque le professionnel reprend la parole.
- `create_response=false` : aucune réponse clinique autonome du modèle Realtime.
- Transcription Realtime FR/EN avec vocabulaire médical.
- Routage de toute transcription vers `AiConsultationService`.
- Vocalisation uniquement d'un message produit/approuvé par le backend Joprelys.
- `ClinicalContextAssembler` minimal et lecture seule : âge/sexe, allergies, antécédents, groupe sanguin, constantes, motif/service, traitements actifs.
- Exclusion volontaire des PII administratives non nécessaires : nom, téléphone, adresse, identifiants DPU/globaux.
- Chargement du contexte à chaque tour pour éviter un contexte clinique périmé.
- Mode fail-closed si le contexte clinique de sécurité n'est pas disponible.
- Dispatcher d'actions cliniques provider-agnostique : note clinique, prescription, examens, constantes, clarification.
- Rejet des actions concurrentes sur un même champ dans un même tour.
- Garde médicament déterministe limité aux faits démontrables sans référentiel pharmacologique : allergie exacte au nom du médicament et doublon exact d'un traitement actif.
- Internationalisation FR/EN.
- Interface mobile-first et fallback vers la dictée contrôlée/classique.

## Hors périmètre

- Référentiel pharmacologique complet, interactions médicamenteuses et classes thérapeutiques.
- Substitution thérapeutique automatique.
- Diagnostic ou prescription autonome.
- Persistance automatique d'une proposition IA.
- Diarisation médecin/patient en mode ambiant.
- Stockage distribué Redis des sessions IA.
- Connexion directe à des dispositifs médicaux.

## Architecture de gouvernance

```text
Microphone
   ↓ WebRTC
OpenAI Realtime
   ↓ transcript uniquement
AiConsultationService
   ↓
Parser strict
   ↓
Grounding anti-hallucination
   ↓
MedicationSafetyGuard
   ↓
ClinicalToolDispatcher
   ↓
Révision / clarification Joprelys
   ↓
Validation explicite du professionnel
   ↓
Formulaire / persistance métier existante
```

Le flux vocal inverse utilise uniquement le texte déjà produit par Joprelys :

```text
assistantMessage validé côté backend
   ↓
response.create hors conversation
   ↓
Audio Realtime
```

## Critères d'acceptation

- [x] La clé OpenAI n'est jamais exposée au navigateur.
- [x] Une session Realtime consultation requiert `CLINICAL_WRITE` et une visite/session clinique active.
- [x] Le transport utilise WebRTC et `semantic_vad`.
- [x] `create_response` est désactivé pour empêcher toute réponse clinique autonome Realtime.
- [x] Une reprise de parole annule la réponse audio en cours et vide le buffer de sortie.
- [x] Un transcript Realtime passe par le même pipeline clinique contrôlé que la saisie texte.
- [x] Une prescription non explicitement dictée reste bloquée par le garde anti-hallucination.
- [x] Un doublon exact de traitement actif provoque une clarification avant proposition.
- [x] Une allergie contenant exactement le nom du médicament proposé provoque une clarification avant proposition.
- [x] Une réponse explicite à une clarification prescription déjà ouverte n'entre pas dans une boucle infinie de confirmation.
- [x] Deux actions contradictoires sur un même champ dans le même tour sont rejetées avant création de révision.
- [x] Le contexte patient est lecture seule et ne peut pas devenir une nouvelle donnée clinique sans énoncé/confirmation du professionnel.
- [x] Les données administratives non nécessaires ne sont pas envoyées au modèle.
- [x] L'indisponibilité du contexte clinique provoque un échec explicite et non un mode dégradé silencieux.
- [x] Les réponses sont localisées FR/EN.
- [x] Le fallback dictée contrôlée reste disponible si WebRTC est indisponible.
- [x] Une transcription contrôlée en attente reste visible lors d'un changement de mode.

## Scénarios de recette

### RT-01 — Conversation naturelle
1. Démarrer le copilote.
2. Dicter : « Patient de 42 ans, céphalées depuis trois jours, pas de fièvre. »
3. Vérifier que la négation est conservée et qu'aucun traitement n'est proposé.

### RT-02 — Barge-in
1. Laisser Joprelys commencer une réponse vocale.
2. Dire : « Non, attends, je voulais dire 37,8. »
3. Vérifier que l'audio Joprelys s'arrête et que le nouveau tour est traité.

### RT-03 — Prescription spontanée interdite
1. Dicter un symptôme sans médicament.
2. Terminer le tour.
3. Vérifier qu'aucun changement `prescription` et aucune clarification médicament ne sont créés.

### RT-04 — Doublon exact
1. Patient avec Amlodipine dans les traitements actifs.
2. Dicter explicitement une nouvelle prescription Amlodipine.
3. Vérifier qu'une clarification de confirmation est posée avant création de la proposition.

### RT-05 — Allergie exacte
1. Allergie documentée « Amoxicilline ».
2. Dicter explicitement « Amoxicilline 500 mg ».
3. Vérifier que l'ordonnance est retenue jusqu'à confirmation du médecin.

### RT-06 — Contexte indisponible
1. Simuler l'indisponibilité du contexte clinique.
2. Soumettre un tour clinique.
3. Vérifier `503 AI_CLINICAL_CONTEXT_UNAVAILABLE` et absence de révision.

### RT-07 — Internationalisation
1. Passer Joprelys en anglais.
2. Démarrer une session.
3. Vérifier transcription, message initial, sécurité et UI en anglais.

## Risques et garde-fous

| Risque | Garde-fou |
|---|---|
| Hallucination thérapeutique | Prompt + grounding déterministe + validation humaine |
| Realtime contourne le backend | `create_response=false`, transcript routé vers API clinique |
| Contexte patient transformé en ordre | Contexte marqué lecture seule + prompt + tests |
| Données PII excessives | Assembleur minimal, tests d'exclusion |
| Interaction médicamenteuse non détectée | Hors périmètre déclaré ; futur référentiel pharmacologique autoritatif |
| Réponse vocale paraphrasée | Texte affiché reste autorité ; instruction Realtime de lecture fidèle |
| Session mono-instance | Limitation connue ; migration Redis à traiter séparément |

## Definition of Done

- [x] Architecture WebRTC implémentée.
- [x] Garde-fous de prescription conservés.
- [x] Contexte clinique minimal implémenté et testé.
- [x] Dispatcher d'actions métier explicite implémenté et testé.
- [x] Garde médicament exact-match implémenté et testé.
- [x] FR/EN implémenté.
- [x] Mobile-first implémenté.
- [x] Tests unitaires et intégration ajoutés.
- [ ] CI globale verte sur le HEAD final de la PR #153.
- [ ] Recette clinique avec vrais échantillons audio et bruit ambiant.
- [ ] Validation DPO / politique de rétention fournisseur avant production.

## Preuves attendues

- Run CI final Maven strict.
- Run CI final tests Angular.
- Run CI final build Angular production.
- Captures/vidéo de la recette mobile Realtime.
- Rapport de recette RT-01 à RT-07.
