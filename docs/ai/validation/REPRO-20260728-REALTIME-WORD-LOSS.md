# Reproduction P0 — perte de mots Realtime

## Scénario A — voix assistant
1. Démarrer une consultation Realtime.
2. Prononcer une phrase générant une clarification ou un message assistant.
3. Continuer immédiatement à parler pendant la lecture vocale assistant.
4. Comparer les mots prononcés au transcript Realtime et au ledger ambient.

### Échec actuel attendu
Les mots prononcés pendant `assistantSpeaking` peuvent être absents du transcript Realtime, car le sender WebRTC est remplacé par `null`.

### Résultat requis
Tous les mots du médecin continuent d'être capturés ; la voix assistant ne coupe jamais le sender.

## Scénario B — backlog
1. Ralentir artificiellement les ACK de persistance.
2. Produire plus de 32 tours.
3. Continuer à parler lorsque l'UI affiche le rattrapage.

### Échec actuel attendu
`backlogPaused` entraîne un mute du sender ; aucun nouvel événement ASR n'est produit durant cette fenêtre.

### Résultat requis
Le backlog continue d'accepter les tours ; l'UI avertit sans couper le micro.

## Scénario C — constantes
1. Démarrer Realtime Constantes.
2. Prononcer plusieurs constantes pendant que le copilote restitue un message.
3. Vérifier que chaque phrase est persistée et proposée.

## Gate appareil réel
- Chrome mobile 360–430 px ;
- 3 minutes de parole continue ;
- parole pendant traitement et message assistant ;
- 0 mot perdu dans les segments de contrôle ;
- aucune donnée manuelle écrasée.
