# Résumé implémentation

Le provider global Consultation utilise désormais `ClinicalRealtimeVoiceBridgeService`. Sa politique refuse toute restitution vocale automatique pendant la capture clinique continue afin d'empêcher l'état `assistantSpeaking` de provoquer le retrait du microphone du sender Realtime.
