# BUG-20260728 — Perte de mots Realtime persistante après #216

## Statut
P0 clinique — reproduction confirmée en recette après déploiement de `main` contenant #216.

## Constat utilisateur
Le médecin continue de perdre des mots pendant la dictée / le Realtime malgré la tranche A de #215.

## Cause racine confirmée dans le code
La tranche A a sécurisé les tours **déjà émis par le bridge** grâce à une queue durable. Elle ne protège pas les mots prononcés pendant les fenêtres où le sender WebRTC est volontairement coupé.

Le contrôleur Consultation appelle encore :

```text
bridge.setMuted(manualMuted || backlogPaused || assistantSpeaking)
```

Le bridge applique ce mute via `RTCRtpSender.replaceTrack(null)`. Pendant `assistantSpeaking` ou `backlogPaused`, OpenAI ne reçoit donc plus l'audio Realtime. La capture ambient conserve des chunks séparés de 10 secondes et les envoie au backend, mais elle ne garantit pas la réinjection immédiate de ces mots dans le transcript Realtime / brouillon visible.

Le contrôleur Constantes coupe également le sender pendant `assistantSpeaking`.

## Pourquoi les tests #216 ont donné un faux sentiment de sécurité
Les tests vérifient surtout que 20/21 événements `transcript$` déjà reçus sont persistés en ordre. Ils ne simulent pas la parole réelle pendant une période où `replaceTrack(null)` empêche précisément OpenAI de produire ces événements.

## Correctif exigé
- [ ] Une voix IA ne doit jamais couper automatiquement le micro du médecin.
- [ ] Le backlog ne doit jamais couper automatiquement le micro ; il devient un avertissement et une file de traitement.
- [ ] Seules deux causes peuvent couper le sender : pause explicite du médecin ou impossibilité de persistance durable fail-closed.
- [ ] Désactiver la lecture vocale automatique des réponses non critiques pendant une consultation ; privilégier un message visuel compact.
- [ ] Ajouter des tests qui simulent `assistantSpeaking=true` puis injectent de nouveaux tours et vérifient leur persistance/analyse.
- [ ] Ajouter un test de backlog au-dessus du high-water mark vérifiant que le sender reste actif.
- [ ] Ajouter les mêmes protections au Realtime Constantes.
- [ ] Vérifier sur mobile Chrome réel 360–430 px avec parole continue pendant traitement.

## Gate
Aucune affirmation « zéro perte » avant test physique documenté. La CI seule ne prouve pas la capture micro réelle.
