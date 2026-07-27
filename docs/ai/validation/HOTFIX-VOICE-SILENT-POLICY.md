# Hotfix — politique voix clinique silencieuse

## Pourquoi
Le micro Realtime était automatiquement retiré du sender lorsque le copilote parlait. Un médecin qui continuait sa consultation pendant cette restitution pouvait donc prononcer des mots jamais envoyés au moteur Realtime.

## Changement
Le provider global `RealtimeVoiceBridgeService` est remplacé, pour la consultation, par `ClinicalRealtimeVoiceBridgeService` qui refuse toute restitution vocale automatique (`speakApproved` retourne `false`).

Les clarifications restent visibles. Le médecin conserve la priorité audio permanente.

## Limites
- ce hotfix cible immédiatement la perte pendant la voix assistant ;
- la politique de backlog et le contrôleur Constantes local restent à durcir dans la suite de #215 ;
- une validation mobile physique reste obligatoire avant de déclarer le P0 clos.
