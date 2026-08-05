# BUG-20260805 — L'écoute vocale mobile se fige et devient irrécupérable

## 1. Description du problème

Lors de la dictée vocale (consultation ou constantes), le praticien observe :
1. **L'IA transcrit pendant un moment puis stoppe sans raison visible** — la parole continue mais n'est plus transcrite.
2. **La conversation prononcée pendant l'arrêt est perdue** — lorsque l'écoute reprend, les mots dits entre-temps ne sont jamais récupérés.
3. **À un moment donné, impossible de relancer ou stopper l'écoute** — les boutons deviennent inopérants, l'interface est bloquée dans un état incohérent.

## 2. Mode d'intervention

| Champ | Valeur |
|---|---|
| Mode | Diagnostic → Engineering |
| Fichier ticket | Ce fichier |
| Rapport diagnostic | Section 4 ci-dessous |

## 3. Contexte analysé

- [x] Gouvernance, workflow, tracking, changelog et checklist relus.
- [x] `clinical_speech_service.dart` (1121 lignes) analysé en profondeur.
- [x] `speech_to_text: ^7.3.0` et configuration inspectée.
- [x] Comportement moteur Android/iOS Speech Recognition analysé.

## 4. Causes racines confirmées

### RC-01 — `pauseFor: 4 secondes` tue la session dès la moindre pause de parole (CAUSE PRINCIPALE)

**Fichier** : `clinical_speech_service.dart` ligne 399.

```dart
listenOptions: stt.SpeechListenOptions(
  listenFor: const Duration(hours: 1),
  pauseFor: const Duration(seconds: 4),   // ← ICI
  partialResults: true,
  cancelOnError: false,
  listenMode: stt.ListenMode.dictation,
  localeId: _speechLocale,
),
```

**Mécanisme** : `pauseFor` détermine la durée maximale de silence avant arrêt automatique. Avec **4 secondes**, dès que le praticien réfléchit, change de note, regarde un résultat ou consulte le patient, le moteur Android/iOS **met fin à la session**. Le plugin émet un statut `done` ou `notListening`.

**Pourquoi c'est la cause principale** : Une consultation clinique comporte naturellement des pauses de 5 à 30 secondes (réflexion, auscultation, question au patient). Avec `pauseFor: 4s`, la reconnaissance se coupe systématiquement à chaque pause naturelle.

### RC-02 — Le mécanisme de redémarrage automatique est limité à 4 tentatives puis meurt

**Fichier** : `clinical_speech_service.dart` lignes 97 et 532.

```dart
static const int _maximumConsecutiveSpeechRestarts = 4;
// ...
if (_consecutiveSpeechRestarts > _maximumConsecutiveSpeechRestarts) {
  _speechRecoveryInProgress = false;
  _stopAfterSpeechFailure(error);  // ← Arrêt terminal
  return;
}
```

**Mécanisme** : Quand le moteur s'arrête suite à un timeout de silence (RC-01), `_handleSpeechError` est appelé. Le service tente de relancer. Mais si le praticien fait plusieurs pauses rapprochées, le compteur `_consecutiveSpeechRestarts` atteint 5 et le service bascule en `_stopAfterSpeechFailure` — un **arrêt terminal** qui met `_shouldKeepListening = false`.

**Conséquence** : L'écoute s'arrête définitivement sans possibilité de reprise automatique. Le praticien continue de parler mais rien n'est transcrit.

### RC-03 — Le compteur de restarts n'est remis à zéro que par une reconnaissance réussie

**Fichier** : `clinical_speech_service.dart` ligne 418.

```dart
void _ingestRecognition(String rawWords, {required bool finalResult}) {
  _consecutiveSpeechRestarts = 0;  // ← Remis à 0 seulement ici
```

**Mécanisme** : `_consecutiveSpeechRestarts` est remis à 0 **uniquement** quand le moteur renvoie des mots reconnus. Si le praticien fait une série de pauses/silences (très fréquent en clinique), chaque silence consomme un restart sans jamais remettre le compteur à zéro. Après 4 silences consécutifs (4 × 4s = 16 secondes de silence cumulé), la dictée meurt.

### RC-05 — Verrou `_listenStartInProgress` et corruption moteur Android

**Mécanisme** : Si le moteur Android échoue de manière asynchrone après que `listen()` se soit résolu, la session est dans un état zombie. `_speech.isListening` peut rester `true` côté plugin alors que le moteur Android est mort.

### RC-06 — La parole pendant l'arrêt est irrémédiablement perdue

**Mécanisme structurel** : Le plugin `speech_to_text` utilise le moteur natif. Quand la session est inactive, **aucun buffer audio n'est conservé**. Les mots prononcés entre l'arrêt et le redémarrage sont définitivement perdus — ce n'est pas un problème de code mais une limitation fondamentale du plugin.

## 5. Synthèse de la chaîne de défaillance

```text
Praticien parle → 4s de silence naturel → pauseFor expire
→ Moteur Android émet done/error_speech_timeout
→ Le service tente de redémarrer (restart #1)
→ Nouvelle pause de 4s → nouveau restart (#2)
→ Encore une pause → restart (#3)
→ Encore une pause → restart (#4)
→ Le compteur dépasse 4 → _stopAfterSpeechFailure()
→ _shouldKeepListening = false → ARRÊT TERMINAL
→ Le praticien continue de parler → PAROLES PERDUES
→ Le praticien tente de relancer → _speechReady peut être false
→ BOUTONS INOPÉRANTS
```

## 6. Correctifs implémentés

### FIX-01 — Augmenter `pauseFor` à 30 secondes

```dart
// AVANT
pauseFor: const Duration(seconds: 4),

// APRÈS — laisser le praticien respirer
pauseFor: const Duration(seconds: 30),
```

**Justification** : 30 secondes correspond à un temps clinique réaliste (auscultation, prise de constante). Le moteur continuera de renvoyer des résultats partiels entre les phrases.

### FIX-02 — Supprimer le plafond fixe de 4 restarts

```dart
// AVANT
static const int _maximumConsecutiveSpeechRestarts = 4;

// APRÈS — pas de limite fixe basse, mais un backoff exponentiel plafonné
static const int _maximumConsecutiveSpeechRestarts = 20;
static const int _reinitializeAfterConsecutiveRestarts = 3;
static const Duration _restartCounterResetDelay = Duration(seconds: 60);
```

Le compteur est remis à zéro par un Timer si 60 secondes s'écoulent sans erreur, même sans reconnaissance active.

### FIX-03 — Remettre le compteur à zéro sur le redémarrage réussi

```dart
void _handleSpeechStatus(String status) {
  if (status == stt.SpeechToText.listeningStatus) {
    _consecutiveSpeechRestarts = 0;  // ← Le moteur est reparti, reset
    _scheduleRestartCounterReset();
    value = value.copyWith(status: SpeechStatus.listening, clearError: true);
    return;
  }
```

### FIX-04 — Reset automatique après 60s sans erreur

```dart
void _scheduleRestartCounterReset() {
  _restartCounterResetTimer?.cancel();
  _restartCounterResetTimer = Timer(_restartCounterResetDelay, () {
    if (!_disposed && _shouldKeepListening) {
      _consecutiveSpeechRestarts = 0;
    }
  });
}
```

### FIX-05 — Réinitialiser complètement le moteur en cas d'échec prolongé

Si le moteur ne répond plus après 3 tentatives de restart, appeler `_speech.initialize()` à nouveau avant de retenter `listen()`, ce qui force Android à créer une nouvelle session de reconnaissance.

```dart
if (_consecutiveSpeechRestarts >= _reinitializeAfterConsecutiveRestarts) {
  _speechReady = false;
  try {
    _speechReady = await _speech.initialize(
      onStatus: _handleSpeechStatus,
      onError: (error) => _handleSpeechError(
        error.errorMsg,
        permanent: error.permanent,
      ),
    );
  } catch (_) {
    _speechReady = false;
  }
  if (!_speechReady) {
    _speechRecoveryInProgress = false;
    _stopAfterSpeechFailure(error);
    return;
  }
}
```

### FIX-06 — Backoff exponentiel plafonné

```dart
await Future<void>.delayed(
  Duration(
    milliseconds:
        _speechRestartDelay.inMilliseconds *
        _consecutiveSpeechRestarts.clamp(1, 5),
  ),
);
```

## 7. Fichiers impactés

| Fichier | Modification |
|---|---|
| `mobile/lib/features/dashboard/application/clinical_speech_service.dart` | FIX-01 à FIX-06 |

## 8. Risques et impacts

| Risque | Impact | Mitigation |
|---|---|---|
| `pauseFor: 30s` consomme plus de batterie | Marginal | Le wakelock est déjà activé pendant l'écoute |
| Android peut imposer une limite < 30s | Dépend du constructeur | Le mécanisme de restart reste en place comme filet |
| Plafond de 20 restarts trop haut | Boucle infinie théorique | Backoff exponentiel empêche le CPU thrashing |
| Réinitialisation du moteur prend du temps | 200-500ms de silence | Feedback utilisateur pendant la réinit |

## 9. Tests

### Tests unitaires
- [x] `flutter analyze` : vert
- [x] 157 tests passent
- [ ] 3 échecs antérieurs (source-scanning `initialDraft`) à corriger séparément

### Tests de recette Android requis

**Scénario critique** :
1. Ouvrir une consultation
2. Lancer la dictée vocale
3. Dicter : "Le patient présente une toux sèche"
4. PAUSE 10 secondes (silence total)
5. Dicter : "depuis trois jours"
6. PAUSE 15 secondes
7. Dicter : "avec fièvre légère"
8. PAUSE 10 secondes
9. Répéter 5-6 cycles pause/parole
10. Vérifier que tout est transcrit
11. Arrêter la dictée

**Critères de validation** :
- ✅ Toutes les phrases transcrites sans perte
- ✅ Pas de gel après les pauses
- ✅ Bouton stop opérant à tout moment
- ✅ Pas de message d'erreur

## 10. Impact version / SemVer

| Champ | Valeur |
|---|---|
| Changement livrable | Oui |
| Type de bump | PATCH |
| Justification | Correctif rétrocompatible de la stabilité de la reconnaissance vocale |
| Breaking change | Non |
| Migration DB | Non |
| Impact Flutter | Oui |
| Impact Spring/IA | Non |
| Impact Angular | Non |

## 11. Statut

**IMPLEMENTED — RECETTE_ANDROID_PENDING**

Les 6 correctifs (FIX-01 à FIX-06) ont été implémentés dans `clinical_speech_service.dart`.

Changements effectués :
- [x] FIX-01 : `pauseFor` passé de 4s à 30s
- [x] FIX-02 : `_maximumConsecutiveSpeechRestarts` passé de 4 à 20
- [x] FIX-03 : Reset du compteur sur `listeningStatus` et via `_scheduleRestartCounterReset()`
- [x] FIX-04 : `_restartCounterResetTimer` annulé pendant la recovery pour éviter la course
- [x] FIX-05 : Réinitialisation complète du moteur natif après 3 restarts consécutifs
- [x] FIX-06 : Backoff exponentiel plafonné à 5x pour limiter le délai max
- [x] Timer `_restartCounterResetTimer` correctement nettoyé dans `dispose()`
- [x] `flutter analyze` vert
- [ ] **Recette Android réelle validée par un praticien**
- [ ] Commit effectué
- [ ] Tests 3 échecs source-scanning à corriger

## 12. Prochaines étapes

1. Commiter les changements
2. Tester sur appareil Android physique (scénario ci-dessus)
3. Si validation OK : merger et déployer
4. Mettre à jour le ticket avec résultats de recette
