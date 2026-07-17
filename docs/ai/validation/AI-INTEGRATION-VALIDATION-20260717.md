# Validation intégration IA — 2026-07-17

## Périmètre vérifié

- routage OpenAI par défaut avec conservation du multi-provider ;
- transcription audio avec contexte médical français ;
- génération du brouillon clinique ;
- compatibilité des modèles OpenAI à raisonnement ne supportant pas une température personnalisée ;
- capture audio Angular renforcée ;
- validation manuelle du médecin avant application et sauvegarde.

## Couverture automatisée ajoutée

`OpenAiProviderTest` couvre désormais :

1. l'envoi du prompt médical dans la requête multipart de transcription ;
2. l'absence de `temperature` pour un modèle `gpt-5*` ;
3. la présence de `temperature=0.3` pour un modèle classique ;
4. le parsing du texte, de la locale et du nombre de tokens retournés.

## État de validation

- Configuration de production annoncée : `gpt-4o-transcribe` pour la transcription et `gpt-4.1` pour le brouillon.
- Correctifs backend et frontend présents sur `main`.
- Tests unitaires ciblés ajoutés sur `main`.
- La validation complète reste conditionnée par une exécution verte de `./mvnw clean verify -B -Dspring.profiles.active=test`, `npm test` et `npm run build` dans la CI.
- La recette clinique terrain et la validation DPO/fournisseur restent nécessaires avant généralisation.

## Critères de clôture

L'intégration peut être déclarée techniquement complète lorsque :

- la CI backend et frontend est verte sur le commit contenant les tests ciblés ;
- une recette avec des audios médicaux réels valide les dosages, unités, négations et noms de médicaments ;
- les éléments ambigus sont clairement signalés au médecin ;
- la conservation et la suppression des audios/transcriptions sont validées ;
- le DPO valide le traitement de données de santé par le fournisseur IA.
