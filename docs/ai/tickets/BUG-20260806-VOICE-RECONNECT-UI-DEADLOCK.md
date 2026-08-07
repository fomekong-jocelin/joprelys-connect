# BUG-20260806 — Blocage de l’assistant vocal après reconnexions

## Incident

Après plusieurs échecs de connexion WebSocket, l’assistant vocal reste dans un état de traitement sans issue visible. Le bouton de fermeture est désactivé et la synchronisation distante peut bloquer la fermeture de la feuille.

## Causes

- l’état `processing` utilisé pour la connexion/reconnexion désactive le bouton de fermeture ;
- `prepareForClose()` refuse toute fermeture pendant `processing` ;
- la fermeture attend une synchronisation réseau distante alors que le transport peut précisément être indisponible ;
- le coordinateur de synchronisation peut effacer le message de reconnexion sans sortir de l’état `processing` ;
- le cycle complet de cinq reconnexions peut dépasser une minute à cause des timeouts cumulés.

## Correctif attendu

- fermeture toujours disponible et immédiate ;
- annulation explicite des connexions et timers de reconnexion ;
- sauvegarde locale prioritaire, sans dépendance au réseau ;
- possibilité d’annuler une reconnexion depuis le bouton micro ;
- message de transport conservé pendant la reconnexion ;
- timeouts de connexion bornés et nettoyage non bloquant ;
- tests Flutter de non-régression.
