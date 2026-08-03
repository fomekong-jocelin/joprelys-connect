# BUG-20260802 — Répétitions vocales, anciens transcripts et pertes de mots

## 1. Objectif

Corriger de bout en bout l'assistant vocal Flutter de consultation et de saisie
des constantes afin qu'une parole ne soit capturée qu'une fois, que le micro
reste disponible pendant l'analyse progressive, qu'un transcript finalisé ne
réapparaisse pas après sauvegarde et que la reformulation clinique reste sûre.

## 2. Critères d'acceptation

- [x] Une seule opération `speech_to_text.listen()` peut démarrer à la fois.
- [x] Un callback provenant d'un ancien cycle d'écoute ne modifie pas le cycle courant.
- [x] Une fenêtre ASR rejouée deux à quatre fois n'est présente qu'une fois dans le transcript.
- [x] Un chevauchement récent avec une variation ASR mineure conserve les mots sans dupliquer la phrase.
- [x] Le microphone peut continuer à écouter pendant la persistance et l'analyse des passages finalisés.
- [x] Chaque passage finalisé est persisté dans le working set Spring avant l'appel IA.
- [x] Les retries progressifs sont idempotents par `eventId`.
- [x] Les corrections et suppressions locales sont reflétées dans le working set serveur.
- [x] La synthèse finale est reconstruite depuis le transcript serveur autoritaire.
- [x] « Tout supprimer » efface le brouillon local et écarte le working set serveur.
- [x] Une fermeture avant sauvegarde conserve un brouillon récupérable.
- [x] La sauvegarde Consultation ou Constantes réussie consomme le working set en best effort.
- [x] Une panne du nettoyage ne transforme pas une sauvegarde clinique réussie en erreur utilisateur.
- [x] La liste des intakes actifs exclut les éléments consommés ou écartés.
- [x] L'interface ne présente jamais simultanément un état d'écoute actif et une erreur terminale.
- [x] La reformulation organise et corrige la grammaire sans ajouter de fait, diagnostic ou synonyme médical.
- [ ] La recette Android réelle parole/silence/reprise/réseau est validée par un praticien.

## 3. Pilotage projet

| Champ | Valeur |
|---|---|
| Epic parent | EPIC-0028 |
| User stories parentes | MOB-2816 / MOB-2811 / MOB-2821 |
| Priorité business | P0 — sécurité et continuité clinique |
| Complexité | L |
| Story points | 5 |
| Profil recommandé | Senior Flutter + Senior Spring Boot + QA mobile clinique |
| Effort estimé senior | 1,5 à 2,5 jours, recette appareil incluse |
| Effort estimé intermédiaire | 3 à 4 jours encadrés |
| Effort estimé junior | Non recommandé seul |
| Responsable | Amp |
| Reviewer obligatoire | Tech Lead Flutter + Lead Backend + praticien référent |
| Risque fonctionnel | Moyen jusqu'à recette Android, auparavant fort |
| Risque technique | Moyen jusqu'aux gates exact-HEAD |
| Dépendances | `speech_to_text`, session IA, durable realtime intake, secure storage |
| Bloquants connus | Validation réelle du microphone Android hors tests automatisés |
| Branche | `fix/mobile-clinical-voice-progressive-sync` |
| Pull request | #271 |

## 4. Contexte analysé

- [x] Gouvernance, workflow, tracking, changelog et checklist relus.
- [x] `DESIGN.md`, standards Flutter/Spring, SOLID et configuration relus.
- [x] Capture utilisateur analysée : état « Paroles en cours » contradictoire avec une erreur terminale et transcript tronqué.
- [x] Chaîne Flutter capture → segments → brouillon local → analyse → application inspectée.
- [x] Chaîne Spring session → durable intake → analyse realtime → reconstruction inspectée.
- [x] Parcours Consultation et Constantes vérifiés séparément.
- [x] Tests existants et trous de couverture inspectés.
- [x] Aucun changement DB, secret, permission ou configuration requis.

## 5. Causes racines confirmées

Le rapport détaillé reste dans
`docs/ai/diagnostics/DIAG-20260802-MOBILE-CLINICAL-VOICE-END-TO-END.md`.

Causes retenues :

1. course entre la reprise sur statut `done/notListening` et la reprise après erreur ;
2. absence historique de verrou pendant l'appel asynchrone à `listen()` ;
3. déduplication initialement limitée au préfixe du transcript complet ;
4. ancien parcours mobile imposant l'arrêt du micro avant l'analyse backend ;
5. brouillon local supprimé seulement après application, donc restaurable après fermeture/rejet ;
6. session IA existante supprimée à l'ouverture sans résoudre le brouillon local ;
7. absence de persistance durable par passage avant analyse ;
8. interface annonçant une reformulation alors que le contrat IA l'interdisait ;
9. consommation du transcript non reliée à la sauvegarde clinique effective ;
10. parcours Constantes encore raccordé à l'ancien assistant bloquant.

## 6. Solution livrée dans la PR #271

### Flutter

- `ClinicalVoiceCaptureApi` encapsule le working set durable, les corrections,
  suppressions, analyses progressives, reconstructions et consommations.
- `ClinicalVoiceProgressiveCoordinator` sérialise les mutations réseau et impose
  l'ordre persistance → analyse.
- `ClinicalVoiceProgressiveAssistantSheet` conserve le microphone actif pendant
  les analyses non bloquantes et affiche un aperçu structuré progressif.
- Consultation et Constantes utilisent désormais ce même pipeline.
- Le transcript reste récupérable après application au formulaire et n'est consommé
  qu'après la sauvegarde réussie de la consultation ou des constantes.

### Spring Boot / IA

- Le pipeline réutilise les endpoints durables et idempotents déjà présents :
  realtime intake, correction, discard, consume, messages realtime et rebuild.
- `AiClinicalFidelityContract` autorise une reformulation grammaticale contrôlée,
  sans enrichissement médical.
- `AiClinicalCapturePrompt` demande une rédaction clinique professionnelle tout en
  conservant les preuves exactes et les attributs à risque.
- `AiClinicalFactualityGuard` reste la barrière déterministe contre les faits non sourcés.

La conception détaillée est documentée dans
`docs/features/mobile-clinical-voice-assistant/PROGRESSIVE-SYNC-HOTFIX-20260803.md`.

## 7. Tests ajoutés

- [x] API mobile durable : list, ingest, analyse progressive, rebuild et consume.
- [x] Invariant persistance avant analyse.
- [x] Absence d'erreur terminale pendant une écoute active.
- [x] Raccordement progressif Consultation et Constantes.
- [x] Nettoyage après sauvegarde Consultation seulement.
- [x] Nettoyage après sauvegarde Constantes seulement.
- [x] Sauvegarde clinique prioritaire sur une panne de nettoyage.
- [x] Reformulation grammaticale sûre et rejet d'un diagnostic inventé.
- [x] Formatage Dart normalisé avec Flutter 3.44.6.
- [ ] `flutter analyze --no-pub` vert sur le HEAD final.
- [ ] `flutter test --no-pub` vert sur le HEAD final.
- [ ] `mvnw clean verify` vert sur le HEAD final.
- [ ] Recette Android réelle validée.

## 8. Risques et impacts

| Risque | Impact | Mitigation |
|---|---|---|
| Déduplication trop agressive | Suppression d'une répétition réellement dictée | Fusion bornée au suffixe récent avec forte similarité |
| Panne réseau pendant écoute | Aperçu IA retardé | Brouillon local conservé, intake retry-safe, aucune erreur terminale pendant écoute |
| Correction après analyse progressive | Aperçu devenu obsolète | Reconstruction autoritaire depuis le working set actif |
| Fermeture avant sauvegarde | Perte d'une dictée utile | Working set serveur conservé jusqu'au save clinique |
| Nettoyage indisponible après save | Ancien brouillon encore actif | Cleanup best effort, retry au save suivant ou suppression explicite |
| Reformulation trop libre | Altération clinique | Evidence exacte, garde factuel, pas de synonymes/diagnostics nouveaux, validation humaine |

## 9. Sécurité / conformité

- AuthN, RBAC, consentement et frontières tenant inchangés.
- Aucune donnée clinique n'est ajoutée aux logs.
- Le backend reste maître de l'analyse et de la reconstruction.
- Aucune proposition n'est appliquée sans validation explicite du praticien.
- Aucun secret, nouveau stockage, changement de rétention ou migration DB.
- L'effacement et la consommation conservent l'audit des intakes.

## 10. Impact version / SemVer

| Champ | Valeur |
|---|---|
| Changement livrable | Oui |
| Type de bump | PATCH |
| Justification | Correctif rétrocompatible de capture, continuité, restauration et reformulation |
| Breaking change | Non |
| Migration DB | Non |
| Changement API | Aucun nouveau schéma public ; endpoints existants réutilisés |
| Impact Flutter | Oui |
| Impact Spring/IA | Oui, prompts et contrat de fidélité |
| Impact Angular | Non |
| Release note requise | Oui au prochain lot mobile |

## 11. Statut

Statut : IMPLEMENTED — GATES_EXACT_HEAD_PENDING

La PR #271 contient le correctif complet. La clôture `DONE` exige encore les gates
Maven/Flutter exact-HEAD et la recette Android réelle par un praticien.
