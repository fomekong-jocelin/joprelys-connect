# Sécurité silence et repli vocal IA — Spécification fonctionnelle

## Problème

Le copilote ne doit jamais transformer une absence de parole, un bruit de fond ou la
voix de l'assistant en donnée clinique attribuée au médecin. Une indisponibilité
Realtime ne doit pas dégrader ce principe.

## Utilisateurs concernés

- médecin utilisant le copilote vocal ;
- patient dont le dossier est ouvert ;
- équipe QA et exploitation supervisant le fournisseur IA.

## Périmètre inclus

- établissement et échec du canal Realtime ;
- repli vers la dictée classique ;
- déclenchement et arrêt de l'enregistrement ;
- détection d'activité vocale locale ;
- relecture obligatoire de la transcription classique ;
- rejet des transcriptions à faible confiance.
- maintien de la configuration Realtime sur les modèles OpenAI officiels non dépréciés.

## Hors périmètre

- diarisation médecin/patient ;
- moteur complet d'interactions médicamenteuses ;
- changement de fournisseur IA ;
- migration vers une session de transcription seule ou vers `gpt-realtime-whisper` ;
- persistance des audios.

## Règles métier

1. Une panne Realtime ne peut déclencher qu'une tentative automatique par visite.
2. Le micro classique démarre uniquement après une action explicite du médecin.
3. Une capture sans activité vocale détectable n'est pas transmise.
4. Une transcription classique n'est jamais analysée avant relecture et confirmation.
5. Une transcription sous le seuil de confiance configuré est rejetée.
6. Une prescription reste une proposition et nécessite toujours les validations
   existantes ; aucune persistance automatique n'est ajoutée.
7. Une clarification en attente se répond par le contrôle explicite prévu ; elle ne
   déclenche pas une capture audio implicite.
8. Les modèles Realtime par défaut doivent correspondre aux identifiants officiels
   courants ; une valeur d'environnement explicite reste prioritaire.

## Parcours attendu

### Realtime disponible

Le canal se connecte une fois et suit le parcours gouverné existant.

### Realtime indisponible

L'interface affiche le repli classique. Le médecin déclenche manuellement la dictée,
la termine, relit le texte, le corrige si nécessaire puis lance explicitement
l'analyse.

### Silence

La capture est arrêtée sans appel backend et un message localisé indique qu'aucune
parole n'a été détectée.

## Critères d'acceptation

- aucun appel clinique après une capture silencieuse ;
- aucune prescription proposée après une capture silencieuse ;
- aucune reconnexion en boucle ;
- aucune analyse de dictée classique sans confirmation ;
- comportement FR/EN conservé.

## Cas limites

- navigateur sans `AudioContext` : la capture reste possible, mais la relecture
  obligatoire demeure le garde-fou principal ;
- voix très faible : le médecin reçoit un message et peut recommencer plus près du
  microphone ;
- coupure réseau après connexion : retour fail-closed au mode classique, sans boucle.
