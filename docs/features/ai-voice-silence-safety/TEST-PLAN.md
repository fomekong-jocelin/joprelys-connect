# Sécurité silence et repli vocal IA — Plan de test

## Tests Angular

- échec HTTP SDP : aucun timeout data channel tardif ;
- rafraîchissements successifs de session : une seule tentative Realtime ;
- fin de synthèse vocale : aucun démarrage automatique du micro ;
- capture sans trame vocale : aucun appel `transcribeAudio` ;
- capture avec activité vocale : création d'une transcription `PENDING_REVIEW` ;
- mode conversationnel dégradé : aucun appel à `sendAudio` ;
- clarification en attente : capture classique bloquée ;
- libellés FR/EN présents.

## Tests Spring Boot

- dépôt d'une transcription Realtime en `PENDING_REVIEW` sans appel au modèle ;
- requête OpenAI avec `include[]=logprobs` et seuil VAD configuré ;
- calcul de confiance depuis les logprobs ;
- rejet `422 AI_TRANSCRIPTION_LOW_CONFIDENCE` sous le seuil ;
- acceptation lorsque la confiance est absente mais parcours de relecture actif ;
- aucune révision ni conversation créée lors du rejet ;
- configuration YAML et valeurs par défaut validées.

## Commandes

```text
cd backend
./mvnw test -Dtest=OpenAiProviderTest,AiConsultationServiceTest,OpenAiRealtimeCallServiceTest
./mvnw clean verify

cd web
npm test -- --watch=false
npm run build
```

## Résultats du 26 juillet 2026

- tests Angular ciblés : 15/15 verts ;
- build Angular production : vert ;
- contrôle i18n : 49 clés shell FR/EN présentes ;
- suite Angular complète : 364 tests verts et 23 échecs hors périmètre liés à
  l'environnement Node 25 (`localStorage.clear` et initialisation TestBed), hors
  fichiers modifiés ;
- tests Maven ciblés : non démarrés, parent Spring Boot 4.1.0 absent du cache Maven
  et accès réseau indisponible.

## Recette clinique manuelle

1. Activer le copilote dans une pièce calme et rester silencieux.
2. Vérifier l'absence de message médecin et de proposition.
3. Dicter une phrase clinique sans médicament et confirmer la transcription.
4. Dicter volontairement un médicament avec dose, corriger le texte puis confirmer.
5. Simuler un échec Realtime et vérifier une seule tentative.
6. Rejouer avec voix faible, bruit modéré, casque et haut-parleurs.

## Critères de sortie

- tests automatisés verts ;
- aucune prescription issue du silence ;
- validation médicale de la recette ;
- aucune donnée clinique sensible dans les logs.
