# Ticket BUG-20260727-AI-VOICE-REALTIME-ENTANGLEMENT-DIAGNOSTIC — Diagnostic de la Dictée et du Realtime IA sur Constantes et Consultation

## 1. Description du problème

Lors de l'utilisation de la dictée et du Realtime IA dans le module de consultation et le module de constantes, l'utilisateur observe des dysfonctionnements majeurs :
- Entrelacement et chevauchement des transcriptions vocales ("ça s'entrelace").
- Écrasement brutal et intempestif des saisies manuelles du médecin dans le formulaire de consultation.
- Perte ou purge complète des ordonnances et demandes d'examens lors de l'application automatique des propositions IA.
- Conflits d'accès microphone et collisions WebRTC lorsque les contrôleurs de constantes et de consultation sont montés simultanément.
- Problèmes d'ergonomie et d'expérience utilisateur (absence de diff visuel, saut du curseur de saisie, commandes vocales incohérentes).

---

## 2. Analyse technique détaillée & Causes racines (Root Causes)

### BUG-RC-01 : Écrasement destructif des champs texte par `patchValue` sans diff ni fusion
- **Fichier** : `web/src/app/consultation/consultation.component.ts` (`applyAiDraft`)
- **Mécanisme** : La méthode `applyAiDraft` reçoit une proposition de brouillon IA et exécute `this.form.patchValue(acceptedDraft)`.
- **Impact** : Remplacement direct et aveugle de toute la valeur des contrôles `symptoms`, `clinicalExam`, `diagnosis`, `conclusion`, etc. Si le médecin était en train d'écrire ou avait rédigé un paragraphe, `patchValue` efface sa saisie, remet le curseur à zéro et détruit les notes cliniques non sauvegardées.

### BUG-RC-02 : Purge complète des FormArray `prescription` et `exams` à chaque proposition IA
- **Fichier** : `web/src/app/consultation/consultation.component.ts` (`applyAiPrescription`, `applyAiLabOrders`)
- **Mécanisme** : 
  - `this.prescriptionItems.clear();` vida tous les médicaments du formulaire avant de réinjecter la proposition IA.
  - `this.labExams.clear();` vide tous les examens de laboratoire du formulaire avant de réinjecter la liste IA.
- **Impact** : Si le praticien a ajouté manuellement une ligne de prescription ou ajusté un dosage, l'application d'un brouillon vocal supprime immédiatement ces lignes manuelles. Risque critique pour la sécurité des soins.

### BUG-RC-03 : Boucle de rétroaction réactive parasite (Form ValueChanges <-> Realtime Draft Sync)
- **Fichier** : `web/src/app/consultation/consultation.component.ts` (`ngOnInit`, `syncVoiceDraft`)
- **Mécanisme** :
  1. `this.form.valueChanges` s'abonne à chaque changement du formulaire Angular et déclenche `syncVoiceDraft()`.
  2. `syncVoiceDraft()` envoie le formulaire brut vers le composant `VoiceAssistantPanelComponent`.
  3. L'assistant IA génère une révision/proposition.
  4. L'application du brouillon déclenche `this.form.patchValue(...)`.
  5. `patchValue` réémet immédiatement un évènement `valueChanges`, ré-expédiant le brouillon incomplet vers l'IA.
- **Impact** : En mode Realtime streaming, la fréquence élevée des fragments audio ingérés génère des allers-retours réactifs en boucle. Les phrases partielles en cours de dictée viennent s'entremêler avec les versions précédentes du formulaire, produisant des doublons et des phrases tronquées ("ça s'entrelace").

### BUG-RC-04 : Collision des instances WebRTC & Conflit d'accès Microphone
- **Fichiers** : `web/src/app/consultation/realtime-vitals-controller.component.ts` et `web/src/app/consultation/realtime-voice-controller.component.ts`
- **Mécanisme** : `RealtimeVitalsControllerComponent` déclare `providers: [RealtimeVoiceBridgeService]`, créant une instance isolée du service WebRTC. De son côté, `RealtimeVoiceControllerComponent` utilise l'instance singleton `RealtimeVoiceBridgeService`.
- **Impact** : Lorsque les constantes et la consultation sont ouvertes simultanément (ou lors de l'ouverture du tiroir/modale des constantes dans la consultation), deux connexions WebRTC et deux capteurs microphones `getUserMedia` tournent en parallèle. Les flux audio se mélangent, provoquant des échos, des blocages de canal WebRTC et la duplication des transcriptions.

### BUG-RC-05 : Désynchronisation temporelle entre Ingestion WebRTC / Audio streaming et Polling HTTP (4s)
- **Fichiers** : `web/src/app/consultation/voice-assistant-panel.component.ts` (`interval(4000)`) et `realtime-voice-controller.component.ts` (`drainTranscriptQueue`)
- **Mécanisme** : Le contrôleur Realtime ingère les tours de parole en streaming continu via WebSocket/WebRTC et `/api/realtime/.../ingest`. En parallèle, `VoiceAssistantPanelComponent` effectue un polling HTTP toutes les 4 secondes via `refreshSession()`.
- **Impact** : Le polling 4s récupère des états de brouillon du serveur qui n'ont pas encore intégré les derniers fragments du flux temps réel, écrasant l'état local plus récent du streaming et causant des désynchronisations chronologiques.

### BUG-RC-06 : Injection impérative du composant vocal par manipulation directe du DOM
- **Fichier** : `web/src/app/consultation/consultation.component.ts` (`mountVoiceAssistant`, `scheduleVoiceAssistantMount`)
- **Mécanisme** : Le composant `VoiceAssistantPanelComponent` est instancié dynamiquement avec `document.querySelector('app-consultation form')` et `target.insertBefore` dans un timer en boucle (jusqu'à 40 essais).
- **Impact** : Anti-pattern Angular violent qui outrepasse la détection de changements, provoque des fuites mémoire lors de la navigation, et casse la propagation réactive des `@Input()` et `@Output()`.

### BUG-RC-07 : Persistance directe des constantes en BDD sans validation du formulaire parent
- **Fichier** : `web/src/app/consultation/consultation.component.ts` (`applyAiVitals`)
- **Mécanisme** : `this.visitApi.saveVitals(this.visitId, vitals)` est appelé directement lors du traitement d'une proposition d'IA pour sauvegarder en BDD.
- **Impact** : Les constantes sont écrites en base de données immédiatement à la volée, indépendamment de l'action de sauvegarde globale de la consultation par le médecin. Si le médecin annule la consultation, les constantes dictées ont déjà été modifiées en BDD.

---

## 3. Rapport d'Ergonomie et de Défauts UX / UI (Non User-Friendly)

1. **Absence de Prévisualisation / Diff Clinique** : Le médecin n'a aucun visuel montrant la différence entre ce qu'il a saisi lui-même et ce que l'IA propose d'ajouter.
2. **Saut de Curseur (Caret Jumps)** : Lorsque l'IA met à jour un champ pendant que le médecin frappe au clavier, le focus et le curseur sont réinitialisés, rendant la saisie mixte (clavier + voix) impossible.
3. **Double Contrôle Vocal Déroutant** : La présence de deux panneaux vocaux distincts (Panneau vocal Consultation + Panneau assistant Constantes) avec chacun leur propre bouton d'enregistrement, leurs barres d'onde audio et leurs statuts crée une confusion totale pour le praticien.
4. **Masquage / Blocage asymétrique** : Les composants d'entrée de l'assistant IA bloquent la saisie texte pendant le Realtime, mais les champs du formulaire principal restent éditables. Le praticien croit pouvoir taper dans le formulaire, mais son texte est écrasé 2 secondes plus tard par la dictée.
5. **Absence de validation ligne par ligne** : Impossible de rejeter un médicament proposé par l'IA sans rejeter toute l'ordonnance ou repasser par une modification manuelle.

---

## 4. Recommandations de Réparation (Plan d'Action Technique)

1. **Remplacer `patchValue` par un moteur de fusion intelligente (Merge Engine)** :
   - Préserver le texte déjà saisi par l'utilisateur.
   - Proposer un mode append/concaténation ou un système de diff visuel avec acceptation/refus par champ.
2. **Conserver les FormArray `prescription` et `exams` existants** :
   - Fusionner les nouveaux médicaments proposés avec la liste existante au lieu d'exécuter `.clear()`.
3. **Unifier la gestion du Microphone et de WebRTC** :
   - Centraliser la capture audio dans un service unique partagé (`AudioCaptureManagerService`) interdisant l'ouverture simultanée de deux flux micro.
4. **Supprimer le montage DOM impératif (`mountVoiceAssistant`)** :
   - Intégrer `<app-voice-assistant-panel>` directement dans le template HTML Angular de `consultation.component.html`.
5. **Remplacer le Polling HTTP 4s par des notifications réactives en WebSocket / Event Source** :
   - Synchroniser le brouillon uniquement sur les évènements de fin de tour de parole (`assistantTurnCompleted`).
6. **Mettre en place un composant UI de Review Diff avant application** :
   - Permettre au médecin de cocher/décocher chaque élément (symptômes, diagnostic, ordonnance, constantes) avant insertion dans le formulaire.

---

## 5. Fichiers impactés (Lecture / Référence)

- `web/src/app/consultation/consultation.component.ts`
- `web/src/app/consultation/consultation.component.html`
- `web/src/app/consultation/voice-assistant-panel.component.ts`
- `web/src/app/consultation/voice-assistant-panel.component.html`
- `web/src/app/consultation/realtime-voice-controller.component.ts`
- `web/src/app/consultation/realtime-vitals-controller.component.ts`
- `web/src/app/consultation/realtime-voice-bridge.service.ts`
- `web/src/app/consultation/smart-vitals-assistant.component.ts`
- `web/src/app/consultation/smart-vitals-assistant.component.html`
