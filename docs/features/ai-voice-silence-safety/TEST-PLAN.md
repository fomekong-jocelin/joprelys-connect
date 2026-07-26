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
- modèle principal par défaut `gpt-realtime-2.1` ;
- modèle de repli par défaut `gpt-realtime-2.1-mini` ;
- absence de dépendance Reactive Streams non utilisée dans le POM ;
- payload de session toujours conforme au schéma Realtime avec réponses autonomes
  désactivées.

## Commandes

```text
cd backend
./mvnw test -Dtest=OpenAiProviderTest,AiConsultationServiceTest,OpenAiRealtimeCallServiceTest
./mvnw dependency:tree -Dincludes=org.reactivestreams:reactive-streams
./mvnw clean verify

cd web
npm test -- --watch=false
npm run build
```

## Résultats du 26 juillet 2026

- tests Angular ciblés : 15/15 verts ;
- build Angular production : vert ;
- contrôle i18n : 49 clés shell FR/EN présentes ;
- suite Angular complète : 81 fichiers et 388/388 tests verts ;
- arbre Maven `org.reactivestreams:reactive-streams` : vide ;
- tests Maven ciblés : 20/20 verts ;
- suite Maven complète : 637/637 tests verts, zéro échec, erreur ou test ignoré ;
- cycle Maven `verify -DskipTests` : succès, JAR Spring Boot généré.

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
