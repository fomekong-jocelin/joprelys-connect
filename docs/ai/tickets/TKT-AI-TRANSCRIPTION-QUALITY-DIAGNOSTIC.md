# TKT-AI-TRANSCRIPTION-QUALITY-DIAGNOSTIC — Diagnostic qualité transcription IA consultation

- **Mode** : Diagnostic + Engineering
- **Date** : 2026-07-17
- **Statut** : Correctifs et tests ciblés ajoutés ; validation CI et recette clinique restantes

## Symptôme

La transcription de la consultation (assistant vocal IA) présentait une qualité insuffisante.
Configuration initiale observée : `OPENAI_MODEL=gpt-4o-mini`, `OPENAI_TRANSCRIBE_MODEL=whisper-1`.

## Causes identifiées

1. **Modèle de transcription ancien** : `whisper-1` était moins adapté au français accentué et au vocabulaire médical que `gpt-4o-transcribe` / `gpt-4o-mini-transcribe`.
2. **Absence de prompt de transcription** : le fournisseur OpenAI ne recevait aucun contexte médical français.
3. **Modèle de structuration trop léger** : `gpt-4o-mini` était utilisé pour produire le brouillon clinique alors que le défaut projet est `gpt-4.1`.
4. **Capture audio perfectible** : absence de contraintes explicites de réduction de bruit, mono, gain automatique et débit audio.
5. **Facteurs terrain** : bruit ambiant, distance du micro, deux locuteurs et absence de diarisation.

## Correctifs appliqués

- [x] Prompt médical français transmis lors de la transcription, configurable via `OPENAI_TRANSCRIBE_PROMPT`.
- [x] Capture micro Angular renforcée : `channelCount: 1`, `echoCancellation`, `noiseSuppression`, `autoGainControl`, `audioBitsPerSecond: 128000`.
- [x] `temperature` non envoyée aux modèles `gpt-5*`, `o1`, `o3` et `o4`.
- [x] Prompt système renforcé pour conserver fidèlement nom, dosage, unité, fréquence, durée et voie d'administration ; ambiguïtés marquées `[À CONFIRMER]`.
- [x] Production configurée avec `OPENAI_TRANSCRIBE_MODEL=gpt-4o-transcribe` et `OPENAI_MODEL=gpt-4.1`.

## Tests automatisés ajoutés

`OpenAiProviderTest` vérifie désormais :

- [x] l'envoi du prompt médical dans la requête de transcription ;
- [x] le parsing du texte et de la locale retournés ;
- [x] l'absence de `temperature` pour un modèle `gpt-5*` ;
- [x] la présence de `temperature=0.3` pour un modèle classique ;
- [x] le parsing du nombre total de tokens.

## Validation restante

- [ ] Obtenir une exécution verte de `./mvnw clean verify -B -Dspring.profiles.active=test` dans la CI.
- [ ] Obtenir une exécution verte de `npm test` et `npm run build` dans la CI.
- [ ] Confirmer le déploiement du backend contenant le prompt, les règles de fidélité et la garde `temperature`.
- [ ] Confirmer le déploiement du frontend contenant les contraintes de capture audio.
- [ ] Réaliser une recette médicale avec des audios réels : noms de médicaments proches, dosages ambigus, unités, négations, accents et bruit ambiant.
- [ ] Valider la conservation/suppression des audios et transcriptions avec le DPO et le fournisseur.

## Conclusion

Le correctif technique et sa couverture ciblée sont présents sur `main`. Le ticket ne doit être considéré comme complètement clôturé qu'après CI verte, recette clinique documentée et validation DPO/fournisseur.
