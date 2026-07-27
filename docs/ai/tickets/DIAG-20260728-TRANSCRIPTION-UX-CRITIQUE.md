# DIAG-20260728-TRANSCRIPTION-UX-CRITIQUE

## Métadonnées

| Champ              | Valeur                                                 |
|--------------------|--------------------------------------------------------|
| **Type**           | Diagnostic UX critique                                 |
| **Priorité**       | P0 — Bloquant utilisabilité                            |
| **Statut**         | DIAGNOSTIC                                             |
| **Sprint**         | SPRINT-0014                                            |
| **Date**           | 2026-07-28                                             |
| **Impact version** | Aucun (analyse seule, pas de modification de code)      |

---

## Résumé exécutif

L'analyse complète de la fonctionnalité de dictée/transcription temps réel (Constantes et Consultation) révèle **un système techniquement robuste mais avec des défauts d'ergonomie critiques qui le rendent inutilisable en conditions cliniques réelles**. Le médecin fait face à une charge cognitive excessive, des flux d'information entrelacés et confus, et des feedback insuffisants sur l'état de ses dictées.

---

## Fichiers analysés

| Fichier | Rôle | Lignes |
|---------|------|--------|
| `voice-assistant-panel.component.ts/.html` | Panel IA consultation (dictée + realtime) | 475 + 158 |
| `smart-vitals-assistant.component.ts/.html` | Assistant constantes dédié | 327 + 209 |
| `realtime-voice-controller.component.ts/.html` | Contrôleur WebRTC consultation | 421 + 84 |
| `realtime-vitals-controller.component.ts` | Contrôleur WebRTC constantes | 495 |
| `realtime-clinical-turn-coordinator.service.ts` | Coordonnateur tours cliniques | 389 |
| `ai-assistant-input.component.ts` | UI microphone/dictée | 237 |
| `ai-draft-merge.service.ts` | Service merge brouillon | 230 |
| `consultation.component.ts/.html` | Formulaire consultation principal | 599 + 370 |
| `realtime-voice-bridge.service.ts` | Bridge WebRTC (transport) | ~500 |
| `dashboard.component.html` | Dashboard clinique (constantes) | 1181 |

---

## 🔴 BUGS CRITIQUES — Bloquants

### BUG-01 : Deux assistants vocaux indépendants sur le même écran (Conflit contextuel)

**Sévérité** : 🔴 CRITIQUE — Source principale de l'entrelacement

**Constat** :
- Le formulaire de consultation (`consultation.component.html` L113) intègre `<app-voice-assistant-panel>` pour les notes cliniques.
- Le dashboard (`dashboard.component.html` L451) intègre `<app-smart-vitals-assistant>` pour les constantes.
- Ces deux composants ont **chacun leur propre pipeline de transcription indépendant** avec des microphones, des files d'attente et des états séparés.

**Problème d'entrelacement** :
1. Le médecin parle naturellement : « *Patient fébrile, température 38.5, poids 72 kg, il se plaint de douleurs thoraciques depuis hier* »
2. Selon le composant actif, cette phrase atterrit dans UN SEUL des deux pipelines :
   - Si `voice-assistant-panel` est actif → les constantes (38.5, 72kg) sont traitées comme du texte clinique libre, pas comme des valeurs structurées
   - Si `smart-vitals-assistant` est actif → les symptômes cliniques (douleurs thoraciques) sont perdus/ignorés
3. **Le médecin ne sait pas quel pipeline est actif** car les deux se ressemblent visuellement
4. **Impossible de dicter un flux naturel mixte** (constantes + notes cliniques mélangées)

**Fichiers impactés** :
- [`voice-assistant-panel.component.ts`](file:///C:/MES-APPLICATIONS/joprelys-connect/web/src/app/consultation/voice-assistant-panel.component.ts) (L311-318, `finishRealtimeTranscription`)
- [`smart-vitals-assistant.component.ts`](file:///C:/MES-APPLICATIONS/joprelys-connect/web/src/app/consultation/smart-vitals-assistant.component.ts) (L157-169, `handleProposal`)
- [`consultation.component.ts`](file:///C:/MES-APPLICATIONS/joprelys-connect/web/src/app/consultation/consultation.component.ts) (L169, `applyAiDraft` — traite la consultation)

---

### BUG-02 : Instance singleton `RealtimeVoiceBridgeService` vs instance locale — Conflit de microphone

**Sévérité** : 🔴 CRITIQUE — Risque de conflit microphone

**Constat** :
- `RealtimeVoiceBridgeService` est déclaré `providedIn: 'root'` (singleton) dans [`realtime-voice-bridge.service.ts` L52](file:///C:/MES-APPLICATIONS/joprelys-connect/web/src/app/consultation/realtime-voice-bridge.service.ts#L52)
- **MAIS** `RealtimeVitalsControllerComponent` le redéclare dans ses `providers: [RealtimeVoiceBridgeService]` dans [`realtime-vitals-controller.component.ts` L42](file:///C:/MES-APPLICATIONS/joprelys-connect/web/src/app/consultation/realtime-vitals-controller.component.ts#L42)

**Conséquence** :
- Quand le mode Realtime Consultation ET le mode Realtime Constantes sont actifs simultanément :
  - Le `RealtimeVoiceControllerComponent` (consultation) utilise l'instance singleton du bridge
  - Le `RealtimeVitalsControllerComponent` (constantes) crée SA PROPRE instance locale du bridge
  - **Deux connexions WebRTC concurrentes** sur le même microphone
  - Le navigateur peut refuser la deuxième capture audio OU les deux flux audio interfèrent
  - Les transcriptions arrivent dans des pipelines différents sans synchronisation

**Impact utilisateur** : Le médecin active le realtime consultation puis essaie les constantes → le micro « bugue », les transcriptions « s'entrelacent » ou disparaissent.

---

### BUG-03 : Aucun historique visible des transcriptions en mode Realtime

**Sévérité** : 🔴 CRITIQUE — Anxiété utilisateur, perte de confiance

**Constat** :
- En mode Realtime consultation ([`realtime-voice-controller.component.html` L28-29](file:///C:/MES-APPLICATIONS/joprelys-connect/web/src/app/consultation/realtime-voice-controller.component.html#L28-L29)), seule la **dernière phrase** est affichée :
  ```html
  <p class="mt-1 line-clamp-3 text-sm leading-5">"{​{ lastTranscript() }}"</p>
  ```
- `line-clamp-3` tronque à 3 lignes maximum
- **Aucun historique scrollable** de toutes les phrases dictées
- Quand la phrase suivante arrive, la précédente disparaît **sans confirmation visuelle** qu'elle a été traitée

**Impact utilisateur** : Le médecin ne sait jamais si ses 10 phrases précédentes ont été captées. Il voit juste la dernière. Cela crée une anxiété constante et le pousse à répéter ou à ne pas faire confiance au système.

---

### BUG-04 : Constantes sauvegardées directement en base sans validation explicite

**Sévérité** : 🔴 CRITIQUE — Risque clinique

**Constat** :
- Dans [`consultation.component.ts` L243-267](file:///C:/MES-APPLICATIONS/joprelys-connect/web/src/app/consultation/consultation.component.ts#L243-L267), la méthode `applyAiVitals()` fait un `saveVitals()` HTTP POST **immédiatement** :
  ```typescript
  private applyAiVitals(raw: string): void {
    // ... parse ...
    this.visitApi.saveVitals(this.visitId, vitals).subscribe({...});
  }
  ```
- Contrairement aux champs textuels (symptoms, clinicalExam...) qui font un `patchValue()` réversible sur le formulaire, **les constantes sont persistées directement en base**
- Si l'IA interprète mal « 38.5 » comme tension systolique au lieu de température, la valeur erronée est déjà en base

**Note** : Le `smart-vitals-assistant` gère correctement ceci avec un staging (`proposalApplied`, bouton "Appliquer"), mais le flux via `voice-assistant-panel → applyAiDraft` bypasse cette validation.

---

## 🟠 BUGS MAJEURS — Dégradent fortement l'expérience

### BUG-05 : Le `voice-assistant-panel` exclut les constantes du merge safe

**Sévérité** : 🟠 MAJEUR — Les constantes ne passent PAS par le filtre de sécurité

**Constat** :
- Dans [`ai-draft-merge.service.ts` L141-162](file:///C:/MES-APPLICATIONS/joprelys-connect/web/src/app/consultation/ai-draft-merge.service.ts#L141-L162), `vitalsProposal()` parse les constantes mais les **exclut du `textPatch`**
- Le merge service retourne `vitalsProposal` dans le plan MAIS ne l'inclut jamais dans `safeDraft`
- Dans [`voice-assistant-panel.component.ts` L298-303](file:///C:/MES-APPLICATIONS/joprelys-connect/web/src/app/consultation/voice-assistant-panel.component.ts#L298-L303), un avertissement est affiché :
  ```typescript
  if (plan.vitalsProposal) {
    warnings.push('Les constantes détectées ne sont pas enregistrées depuis le brouillon...');
  }
  ```
- **Mais le warning est une simple chaîne `errorMessage` qui disparaît dès la prochaine action** — aucun mécanisme de rappel ni de navigation vers le bloc constantes

**Impact** : Le médecin dicte des constantes dans le panel consultation, voit un message flash qu'il rate souvent, et croit que les constantes ont été appliquées alors qu'elles ne l'ont pas été.

---

### BUG-06 : Polling agressif en mode dictée — interférence avec le formulaire

**Sévérité** : 🟠 MAJEUR

**Constat** :
- [`voice-assistant-panel.component.ts` L89-93](file:///C:/MES-APPLICATIONS/joprelys-connect/web/src/app/consultation/voice-assistant-panel.component.ts#L89-L93) : un polling `interval(4000)` rafraîchit la session :
  ```typescript
  this.pollingSubscription = interval(4000).subscribe(() => {
    if (!this.conversationMode() && !this.recording() && !this.busy()) {
      this.refreshSession(true);
    }
  });
  ```
- Ce refresh appelle `this.session.set(response)` qui reconstruit l'objet session entier
- Cela peut provoquer des re-renders du template pendant que le médecin interagit avec le formulaire
- En mode dictée, le polling tourne toutes les 4 secondes MÊME quand aucune transcription n'est en cours

**Impact** : Micro-saccades UI, perte de focus input potentielle.

---

### BUG-07 : Le `smart-vitals-assistant` est un floating overlay qui masque les boutons de sauvegarde

**Sévérité** : 🟠 MAJEUR — Sur mobile/tablette

**Constat** :
- [`smart-vitals-assistant.component.html` L1-6](file:///C:/MES-APPLICATIONS/joprelys-connect/web/src/app/consultation/smart-vitals-assistant.component.html#L1-L6) :
  ```html
  <section class="z-[80]"
    [ngClass]="embedded
      ? 'relative mb-4 block w-full'
      : 'fixed inset-x-0 bottom-0 sm:left-1/2 sm:right-auto sm:bottom-3 sm:w-[560px] sm:-translate-x-1/2'">
  ```
- En mode NON-embedded (`embedded=false`), le composant est **`fixed inset-x-0 bottom-0`** = pleine largeur en bas
- `z-[80]` place le panneau au-dessus de tout
- **Sur mobile**, quand le panneau est expanded, il couvre tout le bas de l'écran, masquant les boutons « Enregistrer » et « Fermer » du formulaire principal

---

### BUG-08 : Pas de feedback "phrase en file d'attente" visible en Realtime

**Sévérité** : 🟠 MAJEUR

**Constat** :
- Le `RealtimeClinicalTurnCoordinator` maintient une double file (`intakeQueue` + `analysisQueue`) — [`realtime-clinical-turn-coordinator.service.ts` L52-53](file:///C:/MES-APPLICATIONS/joprelys-connect/web/src/app/consultation/realtime-clinical-turn-coordinator.service.ts#L52-L53)
- Quand la file contient > 8 éléments (LOW_WATER_MARK), le backpressure est activé
- En mode Realtime, le compteur `queuedCount()` est bien affiché ([`realtime-voice-controller.component.html` L11-15](file:///C:/MES-APPLICATIONS/joprelys-connect/web/src/app/consultation/realtime-voice-controller.component.html#L11-L15))
- **MAIS** : le badge « N en attente » est dans le header compact et se fond dans l'UI — pas d'animation ni d'alerte visuelle
- Le médecin voit juste un petit chiffre gris « 14 en attente » sans comprendre ce que ça signifie ni pourquoi le système semble « lent »

---

## 🟡 PROBLÈMES ERGONOMIQUES — Réduisent la productivité

### ERG-01 : Trop de modes/options — surcharge cognitive

Le médecin doit choisir entre :
1. **Mode Realtime** vs **Mode Dictée** (dans `voice-assistant-panel`)
2. **Mode Realtime** vs **Mode Dictée** (dans `smart-vitals-assistant` — séparément !)
3. Pour chaque mode, comprendre la différence entre « écoute continue » et « appuyer pour parler »
4. Décider dans QUEL assistant parler (consultation vs constantes)

→ 4 niveaux de décision avant même de commencer à dicter. Un médecin en consultation n'a **pas le temps** pour ça.

### ERG-02 : Pas de confirmation visuelle post-application

- Quand `applyCurrentDraft()` est appelé, le formulaire est patchValue'd silencieusement
- Le `successMessage` n'est visible que si on scrolle vers le bon endroit
- **Aucun highlight visuel** des champs modifiés par l'IA (pas de bordure verte, pas d'animation)

### ERG-03 : Le "transcript review" est un obstacle au flux

- Quand la confiance ASR < 0.35 ([`realtime-voice-controller.component.ts` L32](file:///C:/MES-APPLICATIONS/joprelys-connect/web/src/app/consultation/realtime-voice-controller.component.ts#L32)), le système bloque le pipeline et demande une revue humaine
- Pendant cette revue, **tout le pipeline est bloqué** (analysisQueue ne drain plus — [`realtime-clinical-turn-coordinator.service.ts` L303-308](file:///C:/MES-APPLICATIONS/joprelys-connect/web/src/app/consultation/realtime-clinical-turn-coordinator.service.ts#L303-L308))
- Le médecin doit quitter son patient des yeux pour approuver/corriger un texte sur l'écran
- Les phrases suivantes s'accumulent dans la file d'attente → backpressure → mute automatique

### ERG-04 : Pas d'historique accessible des phrases analysées

- En mode dictée classique, une seule `pendingTranscript` est visible. Le transcript précédent est écrasé dès qu'un nouveau message arrive ([`voice-assistant-panel.component.ts` L394-410](file:///C:/MES-APPLICATIONS/joprelys-connect/web/src/app/consultation/voice-assistant-panel.component.ts#L394-L410))
- Pas de panneau « historique de conversation » visible en permanence

### ERG-05 : Waveform cosmétique mais pas informative

- La barre de waveform est une animation sinusoïdale basée sur le niveau audio moyen
- Elle ne montre PAS si le système « comprend » la parole
- Le médecin ne peut pas distinguer entre « le micro capte du bruit » et « la parole est bien détectée »

### ERG-06 : `RealtimeVitalsControllerComponent` à 495 lignes

- Dépasse le seuil de 300 lignes de la règle de qualité
- Mélange logique de transport WebRTC, gestion de file, UI et analyse — violation SOLID

---

## Matrice de risque clinique

| Bug | Probabilité | Impact Patient | Impact UX | Score |
|-----|:-----------:|:--------------:|:---------:|:-----:|
| BUG-01 Deux assistants indépendants | Élevée | Moyen | Critique | 🔴 |
| BUG-02 Conflit singleton/local bridge | Élevée | Moyen | Critique | 🔴 |
| BUG-03 Pas d'historique transcription | Élevée | Faible | Critique | 🔴 |
| BUG-04 Constantes sauvées sans validation | Moyenne | Élevé | Critique | 🔴 |
| BUG-05 Constantes ignorées dans merge | Élevée | Moyen | Majeur | 🟠 |
| BUG-06 Polling 4s interfère UI | Moyenne | Faible | Majeur | 🟠 |
| BUG-07 Overlay masque boutons mobile | Élevée | Faible | Majeur | 🟠 |
| BUG-08 File d'attente invisible | Moyenne | Faible | Majeur | 🟠 |

---

## Recommandations de résolution (à découper en tickets)

### R1 — Fusionner les deux assistants en UN SEUL (BUG-01, BUG-02, ERG-01)
Un seul point d'entrée vocal intelligent qui route automatiquement les constantes vers le pipeline vitals et le reste vers la note clinique. Le médecin ne devrait **jamais** avoir à choisir dans quel assistant parler.

### R2 — Panneau d'historique scrollable des transcriptions (BUG-03, ERG-04)
Afficher TOUTES les phrases captées dans un feed chronologique avec :
- ✅ Phrase traitée avec succès
- ⏳ Phrase en cours d'analyse
- ⚠️ Phrase nécessitant revue
- ❌ Phrase échouée

### R3 — Staging systématique des constantes (BUG-04, BUG-05)
Aligner le flux `voice-assistant-panel → applyAiDraft` sur le pattern du `smart-vitals-assistant` : proposer les constantes sans les sauvegarder immédiatement, forcer la validation explicite.

### R4 — Highlight visuel des champs modifiés par l'IA (ERG-02)
Après `patchValue`, ajouter une bordure verte temporaire (3s) et un badge « IA » sur les champs modifiés.

### R5 — Revue non-bloquante du transcript (ERG-03)
Permettre au pipeline de continuer à traiter les phrases suivantes pendant que le médecin revoit une phrase à faible confiance, au lieu de tout bloquer.

### R6 — Refactoring du `RealtimeVitalsControllerComponent` (ERG-06)
Extraire la logique de pipeline dans un service dédié (`RealtimeVitalsTurnCoordinator`), similaire au `RealtimeClinicalTurnCoordinator`.

---

## Actions terminées

- [x] Lecture des fichiers de gouvernance obligatoires
- [x] Analyse complète du pipeline transcription (WebRTC → formulaire)
- [x] Analyse UX/UI des templates HTML
- [x] Vérification des providers Angular et scoping DI
- [x] Identification des conflits d'instances de services
- [x] Analyse du merge service et des risques de perte de données
- [x] Rédaction du rapport diagnostic

## Reste à faire

- [ ] Présenter ce diagnostic au médecin utilisateur pour validation
- [ ] Découper les recommandations R1-R6 en tickets engineering
- [ ] Estimer chaque ticket par complexité et risque
- [ ] Prioriser dans le sprint backlog
- [ ] Tests cliniques de validation sur 5 tours conversationnels
