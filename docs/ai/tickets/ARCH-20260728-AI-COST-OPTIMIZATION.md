# ARCH-20260728 — Analyse et optimisation des coûts IA vocale

## Métadonnées

| Champ | Valeur |
|---|---|
| Mode | Architecture + Diagnostic + Project Manager |
| Date | 2026-07-28 |
| Epic | EPIC-0024 — Assistant vocal IA de consultation |
| Priorité | P0 avant tout nouveau test vocal prolongé |
| Statut | ANALYSE TERMINÉE — GO POC MESURÉ / NO-GO CONFIGURATION DIRECTE |
| Référence code | `main@54cd05cc` du 2026-07-28 20:17:22 + comparaison recette observée |
| Profil recommandé | Senior backend IA/observabilité + senior Angular/WebRTC |
| Reviewer | Tech Lead + médecin référent + QA clinique + DPO |
| Impact version de l'analyse | Aucun |

## Objectif

Expliquer pourquoi moins de quarante minutes cumulées de tests ont pu dépasser
3 USD, vérifier la proposition Gemini avec le code réellement présent à 20 h 20,
la comparer à la configuration observée en recette, puis définir une cible moins
coûteuse sans modifier le code applicatif pendant cette intervention.

## Périmètre audité

- configuration OpenAI Spring Boot ;
- création de session OpenAI Realtime ;
- pont WebRTC Angular et événements de transcription ;
- traitement clinique déclenché par chaque segment Realtime ;
- transcription classique, constantes vocales et TTS ;
- capture ambiante, découpage, upload et diarisation ;
- extraction de faits et projection clinique finale ;
- logs de recette disponibles et derniers travaux du 2026-07-28 ;
- tarifs officiels OpenAI au 2026-07-28.

## État réellement observé

### Recette prouvée par les logs disponibles

Les démarrages recette du 2026-07-28 à 03:33, 03:42 et 03:52 journalisent :

- brouillon : `gpt-4.1` ;
- transcription classique : `gpt-4o-transcribe` ;
- routage transcription/brouillon : OpenAI/OpenAI.

Les logs disponibles ne contiennent pas le modèle Realtime effectivement injecté
par l'environnement. Le code de cette période utilisait par défaut
`gpt-realtime-2.1`, avec repli `gpt-realtime-2.1-mini`, et
`gpt-4o-transcribe` pour la transcription Realtime.

### HEAD courant audité à 20 h 20

Le HEAD `54cd05cc`, postérieur aux logs ci-dessus, contient les valeurs par défaut :

| Capacité | Modèle courant par défaut |
|---|---|
| Chat/brouillon/extractions/constantes | `gpt-4.1` |
| Dictée classique | `gpt-4o-mini-transcribe` |
| Transcription Realtime | `gpt-4o-transcribe` |
| Diarisation ambiante | `gpt-4o-transcribe-diarize` |
| Session Realtime primaire | `gpt-realtime-2.1` |
| Session Realtime de repli | `gpt-realtime-2.1-mini` |
| Synthèse vocale | `gpt-4o-mini-tts` |

Les variables d'environnement de recette priment sur ces valeurs. Le log de recette
prouve donc encore `gpt-4o-transcribe` pour la dictée classique au dernier
démarrage observé, même si le défaut du code courant est désormais le modèle mini.

## Conclusions tirées du code courant

1. La session Realtime est configurée avec `create_response=false`,
   `output_modalities=["text"]`, sans bloc de sortie audio.
2. Le pont Angular courant n'émet aucun `response.create`. Le modèle Realtime ne
   construit donc pas lui-même les réponses cliniques ni leur audio.
3. En Consultation Realtime, Angular démarre néanmoins deux traitements audio
   distants en parallèle :
   - la transcription Realtime avec `gpt-4o-transcribe` ;
   - la capture ambiante continue, découpée toutes les 10 secondes puis envoyée
     à `gpt-4o-transcribe-diarize`.
4. Chaque segment Realtime non vide et suffisamment confiant est ensuite envoyé au
   backend. `AiConsultationService` appelle `gpt-4.1` une fois par segment.
5. L'appel `gpt-4.1` répète plus de 14 500 caractères d'instructions statiques,
   puis ajoute mémoire gouvernée, brouillon accepté et contexte clinique. Le coût
   dépend donc fortement du nombre de segments VAD, pas seulement des minutes.
6. Après chaque réponse analysée, une clarification ou un `assistantMessage` non
   vide peut déclencher un appel séparé à `gpt-4o-mini-tts`.
7. `OPENAI_MODEL` est aujourd'hui partagé par les conversations, les constantes,
   l'extraction structurée de faits et le planificateur de révision. La
   configuration proposée par Gemini avec un modèle distinct par capacité n'existe
   pas encore dans le code.
8. La projection clinique finale liée aux preuves est déterministe et n'appelle
   aucun modèle. Ajouter `gpt-5.6-terra` comme « brouillon final » créerait donc une
   nouvelle étape et ne constituerait pas un simple remplacement de configuration.
9. Le provider conserve seulement `total_tokens` dans la réponse Java, sans
   métrique persistée ni ventilation entrée/sortie/cachée. Côté Angular,
   l'événement officiel `input_audio_transcription.completed` peut contenir un
   objet `usage`, mais l'interface courante ne le lit pas. Les 3 USD ne peuvent
   donc pas être réconciliés exactement sans export Usage OpenAI ou nouvelle
   observabilité.
10. Les logs montrent des tentatives de diarisation ambiante rejetées en 429. Elles
    confirment l'activité du pipeline parallèle, mais un rejet 429 ne doit pas être
    compté comme une consommation réussie sans preuve issue du tableau Usage.

## Tarifs officiels retenus

| Modèle | Entrée | Sortie | Équivalent utile |
|---|---:|---:|---:|
| `gpt-realtime-2.1` audio | 32 USD/M | 64 USD/M | 0,0192 USD/min humaine ; 0,0768 USD/min IA |
| `gpt-realtime-2.1-mini` audio | 10 USD/M | 20 USD/M | 0,006 USD/min humaine ; 0,024 USD/min IA |
| `gpt-4o-transcribe` | — | — | 0,006 USD/min |
| `gpt-4o-mini-transcribe` | — | — | 0,003 USD/min |
| `gpt-4o-transcribe-diarize` | — | — | 0,006 USD/min |
| `gpt-4.1` texte | 2 USD/M | 8 USD/M | modèle partagé actuel |
| `gpt-4o-mini` texte | 0,15 USD/M | 0,60 USD/M | 92,5 % moins cher par token |
| `gpt-5.6-terra` texte court | 2,50 USD/M | 15 USD/M | 12k entrée + 2k sortie = 0,06 USD |
| `gpt-4o-mini-tts` | 0,60 USD/M texte | 12 USD/M audio | environ 0,014–0,015 USD/min audio |

Pour Realtime, le guide officiel retient environ 600 tokens/minute audio entrante
et 1 200 tokens/minute audio sortante. Une connexion ouverte n'est pas facturée
comme une heure forfaitaire : les réponses créées et la transcription d'entrée
constituent les postes facturables.

## Vérification de la proposition Gemini

| Affirmation | Verdict | Motif |
|---|---|---|
| Tarifs Realtime mini à 0,006/0,024 USD par minute | Correct | Conforme aux tarifs et à la conversion officiels |
| Tarifs Realtime 2.1 à 0,06/0,24 USD par minute | Incorrect | Les valeurs correctes sont 0,0192/0,0768 ; surestimation ×3,125 |
| Recette actuelle à environ 4,86 USD/h | Incorrect pour Joprelys | Le calcul suppose une génération audio Realtime continue absente du code courant et déjà neutralisée dans le pont clinique antérieur |
| Mini Realtime continu à environ 0,665 USD/h | Mathématiquement plausible | Valable pour un vrai flux speech-to-speech 40 min entrée/10 min sortie, pas pour l'architecture Joprelys actuelle |
| `gpt-4o-mini` pour les extractions | Direction correcte | Gain théorique de 92,5 %, sous réserve d'un benchmark clinique et d'un fallback |
| Terra 12k/2k à environ 0,06 USD | Correct | Mais le final courant est déterministe et gratuit en tokens |
| Realtime ON/OFF comme économie principale | Partiellement correct | Économise l'audio réellement envoyé et prévient les appels accidentels ; la connexion seule n'est pas le poste majeur et la capture ambiante parallèle doit aussi être traitée |
| Appliquer directement le YAML proposé | NO-GO | Les propriétés et ports séparés n'existent pas ; `OPENAI_MODEL=terra` ferait utiliser Terra aux appels fréquents au lieu d'un seul final |

## Estimation comparable — consultation de 60 minutes

Hypothèses communes : 40 minutes de parole humaine, 10 minutes cumulées de voix
Joprelys, 10 minutes de silence/examen. Pour le texte courant, un segment est borné
à 4–6k tokens d'entrée et 250–600 tokens de sortie, soit environ
0,010–0,0168 USD par appel `gpt-4.1`. Ce sont des hypothèses de budget, pas une
mesure issue du compte OpenAI.

Coûts fixes approximatifs du HEAD courant :

- transcription Realtime : 40 × 0,006 = 0,24 USD ;
- diarisation ambiante continue : 60 × 0,006 = 0,36 USD ;
- TTS de 10 minutes : environ 0,144 USD ;
- référence vocale médecin de 3 secondes répétée avec chaque chunk : jusqu'à
  environ 0,108 USD additionnel si cet audio est facturé à chaque requête.

| Segments analysés par `gpt-4.1` | HEAD courant estimé | Cible mini + Terra, sans double diarisation | Cible mini + Terra, diarisation conservée |
|---:|---:|---:|---:|
| 20 | 0,94–1,19 USD | environ 0,34 USD | jusqu'à environ 0,82 USD |
| 60 | 1,34–1,86 USD | environ 0,37 USD | jusqu'à environ 0,87 USD |
| 120 | 1,94–2,87 USD | environ 0,41 USD | jusqu'à environ 0,94 USD |
| 180 | 2,54–3,88 USD | environ 0,46 USD | jusqu'à environ 1,02 USD |

Ainsi, plus de 3 USD sur moins de quarante minutes cumulées est techniquement
plausible si le VAD a produit beaucoup de petits segments : chacun répète le grand
prompt `gpt-4.1`, tandis que Realtime et la diarisation ambiante traitent le même
microphone. La cause exacte reste à confirmer par l'export Usage par modèle.

## Décision recommandée

### GO

- instrumenter les coûts réels par capacité et modèle avant les prochains tests ;
- évaluer `gpt-4o-mini-transcribe` avec repli `gpt-4o-transcribe` sur un corpus
  clinique anonymisé ;
- router les extractions fréquentes vers `gpt-4o-mini`, avec fallback contrôlé
  pour ambiguïtés, médicaments, négations, nombres, schéma invalide ou confiance
  insuffisante ;
- réduire le nombre d'appels texte par regroupement/batching ou déclenchement
  explicite de l'assistant ;
- vocaliser seulement les clarifications utiles ou les réponses demandées ;
- supprimer la double transcription distante seulement après décision
  d'architecture sur la preuve ambiante et la récupération.

### NO-GO

- remplacer seulement `OPENAI_MODEL` par `gpt-5.6-terra` ;
- laisser croire que le passage du modèle de session Realtime 2.1 vers mini
  divisera le coût actuel alors qu'aucune réponse Realtime n'est créée ;
- supprimer la diarisation ou les garde-fous cliniques uniquement pour réduire la
  facture ;
- mettre Terra en source de vérité clinique ou remplacer la projection
  déterministe liée aux preuves sans évaluation médicale.

## Découpage macro proposé — non engagé

### EPIC → Stories → Tasks

| Story / Task | Objectif | Estimation | Profil | Reviewer |
|---|---|---:|---|---|
| COST-01 — Observabilité | Ventiler appels, modèles, tokens entrée/sortie/cachés, `usage` des événements ASR, minutes TTS, segments VAD et coût estimé | 3 SP / 1–2 j | Senior backend/SRE | Tech Lead + DPO |
| COST-02 — Benchmark clinique | Comparer transcribe/mini et extraction 4.1/mini sur corpus anonymisé avec seuils de non-régression | 3 SP / 2–3 j | Senior IA + QA clinique | Médecin référent |
| COST-03 — Routage par capacité | Introduire des ports/configurations distincts transcription, extraction, conversation, révision et éventuel final | 5 SP / 3–5 j | Senior backend | Architecte + sécurité |
| COST-04 — Cycle audio | Regrouper les tours, empêcher les pipelines distants inutiles et limiter le TTS sans perte durable | 3 SP / 2–3 j | Senior Angular/WebRTC | QA clinique |
| COST-05 — Garde budgétaire et rollout | Budget par consultation, alertes, feature flags, canary et rollback | 3 SP / 2–4 j | Backend/SRE + QA | Product + DPO |

Charge candidate : 17 SP, environ 10 à 17 jours senior hors constitution du
corpus, validation médicale et recette terrain. Ne pas engager dans SPRINT-0014
sans arbitrage de capacité.

## Critères d'acceptation avant implémentation

- [ ] Export OpenAI Usage obtenu par modèle et par jour de test.
- [ ] Nombre réel de segments, appels texte et minutes audio mesuré par consultation.
- [ ] Coût cible et budget maximal par consultation validés par Product.
- [ ] Corpus anonymisé couvrant médicaments, doses, nombres, unités, négations,
  accents et bruit validé par le médecin/DPO.
- [ ] Non-régression clinique mini versus modèle de référence mesurée.
- [ ] Contrat de fallback et fail-closed documenté.
- [ ] Choix explicite sur la conservation ou le remplacement de la diarisation
  ambiante parallèle.
- [ ] ADR accepté avant changement de routage ou de source de brouillon final.

## Actions réalisées

- [x] Lire la gouvernance, les standards, l'ADR et les tickets associés.
- [x] Auditer le HEAD de 20 h 17 et les derniers travaux, pas seulement la recette
  de 4 h.
- [x] Comparer le HEAD aux logs de recette disponibles.
- [x] Tracer chaque appel externe OpenAI depuis Angular jusqu'au backend.
- [x] Vérifier les tarifs dans la documentation officielle OpenAI.
- [x] Recalculer les scénarios Gemini.
- [x] Identifier les limites de la configuration actuelle.
- [x] Proposer un découpage, des profils, reviewers et tests attendus.
- [x] Ne modifier aucun fichier applicatif ni aucune configuration.

## Tests et vérifications

- `git status`, `git log` et `git show` : HEAD et derniers travaux confirmés sur
  `54cd05cc` à 20:17:22.
- Tests Angular ciblés :
  `realtime-voice-bridge.service.spec.ts`,
  `clinical-voice-playback.service.spec.ts` et
  `realtime-voice-controller.component.spec.ts` : 3 fichiers, 27 tests verts.
- Relance Maven ciblée non obtenue : le wrapper Windows échoue avant Maven, puis
  Maven local ne peut résoudre des dépendances Spring Boot/Testcontainers à cause
  de l'accès réseau interdit et du cache incomplet.
- Rapports Surefire présents et horodatés 20:17:35 sur ce HEAD :
  `OpenAiRealtimeCallServiceTest` 6/6 et `OpenAiProviderTest` 7/7, sans échec.
  Ils sont cités comme preuve existante, pas comme une nouvelle exécution.
- `git diff --check` : aucune erreur de whitespace documentaire.

## Reste à faire

Obtenir l'export Usage OpenAI, puis décider si COST-01 est engagé. Sans cette
mesure, la recommandation reste un budget d'architecture et non une facture
reconstituée au centime.
