# TEST-PLAN — Assistant vocal IA de consultation

## 1. Objectif

Prouver que l’assistant accélère la saisie sans contourner la sécurité, le
tenant, la validation médicale ni la confidentialité.

## 2. Scénarios fonctionnels

| ID | Scénario | Résultat attendu |
|---|---|---|
| TC-001 | QR d’une visite active du tenant | bon patient confirmé |
| TC-002 | QR d’un autre tenant | 404, aucune donnée révélée |
| TC-003 | Visite clôturée/annulée | 409 |
| TC-004 | Dictée symptômes + examen | brouillon, aucune persistance |
| TC-005 | « Non, corrige ici… » | champ ciblé modifié |
| TC-006 | Champ provider inconnu | sortie rejetée |
| TC-007 | Prompt injection dictée | aucune action/secret |
| TC-008 | Provider indisponible | 503, mode manuel disponible |
| TC-009 | Session expirée | redémarrage explicite |
| TC-010 | Édition manuelle après application | valeur manuelle conservée |
| TC-011 | Dictée passive | aucune lecture vocale |
| TC-012 | Permission refusée | alternative texte/manuelle |

## 3. Tests sécurité

- 401 sans JWT ; 403 sans permission ; 404 inter-tenant.
- Limites 413/415/429.
- Clé provider absente avec feature active.
- Aucun contenu clinique dans les logs capturés.
- Sorties provider malformées, trop longues, nulles ou contradictoires.
- Sessions concurrentes et expiration.

## 4. Évaluation clinique

Corpus anonymisé : français et accents locaux, bruit, négations, correction,
antécédent versus symptôme, hypothèse versus diagnostic, contradictions.

Mesures : exactitude par champ, taux de correction, omissions critiques,
hallucinations, latence p95 et abandon.

## 5. Commandes

| Niveau | Commande |
|---|---|
| Backend ciblé | `./mvnw test -Dtest=VisitControllerTest,*Ai*Test` |
| Backend complet | `./mvnw clean verify` |
| Angular | `npm run test` |
| Angular build | `npm run build` |
| i18n | `npm run i18n:check` |

## 6. Matrice UI

Android Chrome, iOS Safari, desktop Chrome/Firefox/Edge, light/dark, FR/EN,
clavier/lecteur d’écran, permissions accordées/refusées/révoquées.

## 7. Résultats

| Date | Commande | Résultat | Commentaire |
|---|---|---|---|
| 2026-07-17 | compilation Maven offline avec cache local | SUCCESS | état amorcé uniquement |
| 2026-07-17 | Maven avec dépôt sandbox par défaut | BLOCKED | réseau Maven refusé |
| 2026-07-17 | `mvn -o -Dmaven.repo.local=… -Dtest=VisitControllerTest test` | SUCCESS | 16 tests, 0 échec ; QR décodé et cas 401/403/404/409 couverts |
| 2026-07-28 | tests Angular ciblés surface vocale | SUCCESS | 7 fichiers, 46 tests |
| 2026-07-28 | `npm run test -- --no-watch` | SUCCESS | 101 fichiers, 507 tests |
| 2026-07-28 | `npm run i18n:check` | SUCCESS | 49 clés shell présentes en FR/EN |
| 2026-07-28 | `npm run build` | SUCCESS | bundle initial 542,11 kB ; avertissement NG8113 préexistant |
| 2026-07-28 | comparaison visuelle automatisée | BLOCKED | adresse locale refusée par la politique du navigateur ; recette authentifiée manuelle requise |
| 2026-07-28 | tests Angular ciblés hotfix vocal B | SUCCESS | 28 fichiers, 136 tests ; conversation Realtime, Dictée passive, correction, barge-in, backlog et finalisation |
| 2026-07-28 | `npm test -- --no-watch` après hotfix vocal B | SUCCESS | 102 fichiers, 517 tests |
| 2026-07-28 | `npm run i18n:check` après hotfix vocal B | SUCCESS | 49 clés shell présentes en FR/EN |
| 2026-07-28 | `npm run build` après hotfix vocal B | SUCCESS | bundle initial 525,77 kB ; aucun avertissement Angular |
| 2026-07-28 | `mvn -o -Dtest=OpenAiRealtimeCallServiceTest test` | SUCCESS | 6 tests, session texte uniquement et absence de sortie audio |
| 2026-07-28 | `mvn verify` | ENVIRONMENT | 816 tests exécutés, 0 échec fonctionnel ; 4 erreurs JUnit de nettoyage `FileStorageServiceTest` dues à `AccessDeniedException` sur le répertoire temporaire Windows hors sandbox |
| 2026-07-28 | `mvn -DargLine=-Djava.io.tmpdir=… -Dtest=FileStorageServiceTest test` | SUCCESS | 4 tests verts avec le répertoire temporaire placé dans le workspace |
| 2026-07-28 | `mvn -DargLine=-Djava.io.tmpdir=… verify` | SUCCESS | 816 tests, 0 échec, 0 erreur, 8 ignorés ; JAR Spring Boot produit |

La suite complète, les tests des adaptateurs IA, Angular et E2E restent à
exécuter lorsque les stories correspondantes seront implémentées.

## 8. Surface d’écoute vocale unifiée

| ID | Scénario | Résultat attendu |
|---|---|---|
| UI-VOICE-01 | Consultation Realtime connectée | carte Soft UI partagée active |
| UI-VOICE-02 | Consultation Dictée en enregistrement | même carte et arrêt de la dictée |
| UI-VOICE-03 | Constantes Realtime connectées | même carte avec conseil Constantes |
| UI-VOICE-04 | Constantes Dictée en enregistrement | même carte et arrêt de la dictée |
| UI-VOICE-05 | Realtime en reconnexion ou bloqué | même carte, état accessible et sans perte des contrôles |
| UI-VOICE-06 | Historique Consultation | zone séparée, fixe, scrollable et auto-scroll |
| UI-VOICE-07 | Thèmes light/dark | tokens centraux, contraste et hiérarchie préservés |
| UI-VOICE-08 | FR/EN | aucun libellé visible non traduit |
| UI-VOICE-09 | Mobile 360 px | aucun débordement et action d’arrêt ≥ 44 px |
| UI-VOICE-10 | Mouvement réduit | pulsations non essentielles désactivées |

### Vérifications ciblées

- test unitaire du composant partagé : rendu, état actif/inactif et événement
  `stop` ;
- tests d’intégration des quatre consommateurs ;
- contrôle `npm run i18n:check` ;
- suite `npm run test -- --no-watch` ;
- build `npm run build` ;
- comparaison visuelle avec
  `docs/ai/mockups/exact_soft_voice_card_1785238037119.jpg`.

La vérification fonctionnelle et structurelle de la surface unifiée est verte.
La matrice visuelle interactive UI-VOICE-07/UI-VOICE-09 et la comparaison
pixel à pixel restent à signer sur l’application authentifiée ; voir
`design-qa.md`.

## 9. Non-régression P0 — correction et finalisation Realtime

| ID | Scénario | Résultat attendu |
|---|---|---|
| RT-SAFE-01 | Session Realtime créée | sortie texte uniquement, aucune sortie audio |
| RT-SAFE-02 | Clarification ou message IA reçu en Realtime | affichage + canal TTS backend unique ; aucun prompt lu |
| RT-SAFE-02B | Réponse IA en Dictée | affichage uniquement, aucune vocalisation |
| RT-SAFE-02C | Médecin reprend la parole pendant le TTS | lecture interrompue, sender jamais muté |
| RT-SAFE-03 | Transcript non vide sans `logprobs` | intake durable puis éditeur visible, texte intact |
| RT-SAFE-04 | Correction du transcript en Realtime | texte corrigé envoyé à l'analyse, revue ensuite libérée |
| RT-SAFE-04B | Transcript jugé fiable mais incorrect | action « Corriger » disponible sur la dernière phrase ; texte édité envoyé comme correction conversationnelle |
| RT-SAFE-05 | Nouveau tour pendant revue/révision | capture et persistance continuent dans l'ordre |
| RT-SAFE-06 | Backlog supérieur au seuil haut | avertissement visible, sender non muté |
| RT-SAFE-07 | Échec durable non transitoire | sender muté fail-closed, file non supprimée |
| RT-SAFE-08 | « Terminer » pendant ACK lent | nouveaux tours stoppés, ACK attendu, aucune destruction |
| RT-SAFE-09 | « Terminer » pendant revue | éditeur conservé ; sortie après décision et pipeline idle |
| RT-SAFE-10 | Fin sans revue ni révision | brouillon fusionné puis passage en Dictée |
| RT-SAFE-11 | Vingt tours rapides | intake/analyse ordonnés, aucune duplication/perte |
| RT-SAFE-12 | Dictée Constantes corrigée | réanalyse du texte corrigé, application encore explicite |
| RT-SAFE-13 | Parole pendant traitement IA | transcript conservé ; aucun mute `assistantSpeaking` |
| RT-SAFE-14 | Suppression de session explicite | seule l'action de suppression dédiée efface la session |

La recette réelle complète les tests automatiques sur Chrome desktop et
Android, en FR/EN et light/dark. Elle doit inclure une correction de nombre ou
de négation, une parole pendant traitement et un clic « Terminer » immédiatement
après une phrase.
