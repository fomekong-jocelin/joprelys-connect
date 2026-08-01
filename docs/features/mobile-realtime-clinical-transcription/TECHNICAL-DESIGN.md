# Transcription clinique mobile en temps réel

## Statut

- Ticket : #265
- Parent : #215
- Dépendance temporaire : PR #260
- Priorité : P0

## Problème

Le mobile enregistre actuellement un WAV PCM complet puis l'envoie au backend au clic sur **Terminer**. Une consultation longue dépasse la limite de 10 Mo, produit un HTTP 413 et retarde la transcription jusqu'à la fin de l'enregistrement.

## Décision

Le mobile réutilise le transport OpenAI Realtime déjà employé par Angular :

1. Flutter ouvre une session WebRTC via l'endpoint SDP Joprelys authentifié ;
2. la piste microphone est transmise en continu ;
3. le canal de données reçoit les événements de transcription ;
4. chaque tour final est persisté immédiatement dans la session Joprelys avec un identifiant idempotent ;
5. les deltas sont affichés localement mais ne deviennent jamais des données cliniques ;
6. **Terminer** clôture le tampon audio, attend le dernier tour final et ouvre la relecture ;
7. le compte rendu est généré seulement après validation du transcript ;
8. aucune proposition n'est appliquée au formulaire sans décision explicite du praticien.

## Propriété et indépendance fournisseur

Joprelys possède :

- le contrat client ;
- l'authentification ;
- la session de consultation ;
- la persistance et l'idempotence des tours ;
- la restauration après fermeture ;
- la relecture et les corrections ;
- l'extraction clinique ;
- les règles de sécurité et l'application au formulaire.

OpenAI est un adaptateur de transcription remplaçable. Le client mobile ne reçoit jamais de clé fournisseur.

## Flux

```text
Flutter microphone
        |
        v
WebRTC audio stream
        |
        v
OpenAI Realtime transcription
        |
        v
Data channel: delta/completed
        |
        +--> affichage immédiat des deltas
        |
        +--> POST idempotent de chaque tour final vers Joprelys
                    |
                    v
             transcript en attente
                    |
                    v
             relecture humaine
                    |
                    v
             extraction clinique
                    |
                    v
             validation explicite
```

## Contrat de persistance

Chaque tour final contient :

- `eventId` : identifiant idempotent du fournisseur ou identifiant généré par le client ;
- `itemId` : identifiant du tour audio lorsqu'il existe ;
- `transcript` : texte final non vide ;
- `confidence` : confiance disponible, jamais utilisée pour appliquer automatiquement une donnée ;
- `sequence` : ordre local monotone.

Le backend ignore les doublons et concatène les nouveaux tours dans l'ordre d'arrivée. Un transcript en cours reste récupérable via la session existante.

## Reconnexion

- reconnexion progressive bornée ;
- conservation des tours finaux non acquittés ;
- renvoi idempotent après reconnexion ;
- aucune suppression du transcript lors de la fermeture de la feuille ;
- message utilisateur lisible, jamais une représentation brute d'exception.

## Finalisation

Le clic sur **Terminer** :

1. envoie `input_audio_buffer.commit` lorsque nécessaire ;
2. attend le dernier événement `completed` pendant une durée bornée ;
3. ferme la piste et le peer connection ;
4. attend la persistance des tours en file ;
5. affiche le transcript consolidé à relire.

Il n'existe plus d'upload WAV final.

## Critères de sortie

- [ ] premier texte visible pendant que le médecin parle ;
- [ ] aucune requête audio finale volumineuse ;
- [ ] consultation longue sans HTTP 413 ;
- [ ] tours ordonnés, sans doublon ;
- [ ] fermeture/réouverture avec transcript restauré ;
- [ ] silence sans transcript ni proposition clinique ;
- [ ] reconnexion sans perte des tours déjà finalisés ;
- [ ] compte rendu séparé de la transcription ;
- [ ] tests backend et Flutter verts ;
- [ ] recette Android réelle validée avant fusion.
