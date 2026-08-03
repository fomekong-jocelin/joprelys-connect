# Hotfix 2026-08-03 — Dictée clinique progressive et durable

## Résumé

Ce hotfix remplace, pour les parcours mobiles Consultation et Constantes, le cycle bloquant
« arrêter le micro → analyser → appliquer » par un pipeline progressif :

1. `speech_to_text` finalise un passage local ;
2. le passage est enregistré dans le working set durable Spring avant tout appel IA ;
3. l'analyse progressive met à jour un aperçu clinique sans arrêter le microphone ;
4. les corrections et suppressions sont synchronisées sur le même passage ;
5. la synthèse finale est reconstruite depuis le working set serveur autoritaire ;
6. le transcript n'est consommé qu'après la sauvegarde réussie du formulaire clinique.

## Invariants fonctionnels

- Une panne IA ou réseau ne transforme jamais une écoute active en erreur terminale.
- Une parole finalisée est persistée avant analyse.
- Un retry conserve le même `eventId` et reste idempotent.
- Une correction remplace le passage durable correspondant ; elle ne crée pas un doublon.
- « Tout supprimer » écarte le working set serveur et efface le brouillon local.
- Une fermeture avant sauvegarde conserve un brouillon récupérable.
- Une sauvegarde Consultation ou Constantes réussie consomme le working set en best effort.
- Une panne du nettoyage ne fait jamais croire qu'une donnée clinique déjà sauvegardée a échoué.
- Le formulaire SOAP de consultation est remplacé uniquement après validation humaine de la nouvelle dictée.
- Les constantes existantes restent le brouillon de départ et seules les valeurs confirmées sont appliquées.

## Flux mobile / API

| Étape | Mobile | Endpoint Spring |
|---|---|---|
| Restaurer | `ClinicalVoiceProgressiveCoordinator.start()` | `GET /api/ai/consultations/{visitId}/realtime-intake` |
| Persister un passage | `ingestSegment()` | `POST /api/ai/consultations/{visitId}/realtime-intake` |
| Aperçu progressif | `analyzeProgressiveSegment()` | `POST /api/ai/consultations/{visitId}/messages/realtime` |
| Corriger | `correctSegment()` | `POST /api/ai/consultations/{visitId}/realtime-intake/{id}/correction` |
| Supprimer un passage | `discardSegment()` | `DELETE /api/ai/consultations/{visitId}/realtime-intake/{id}` |
| Supprimer tout | `discardAll()` | `DELETE /api/ai/consultations/{visitId}/realtime-intake` |
| Synthèse finale | `rebuild()` | `POST /api/ai/consultations/{visitId}/capture/rebuild` |
| Consommer après save | `ConsultationApi` / `VitalsApi` | `POST /api/ai/consultations/{visitId}/realtime-intake/consume` |

## Reformulation clinique contrôlée

Le contrat IA autorise désormais l'organisation, la ponctuation et une correction grammaticale
minimale pour produire des phrases cliniques professionnelles. Les garde-fous restent stricts :

- aucune invention, complétion ou interprétation médicale ;
- aucun synonyme médical nouveau ;
- conservation exacte des négations, incertitudes, temporalités, nombres, unités,
  médicaments, doses, voies, fréquences et durées ;
- preuves exactes obligatoires depuis le transcript courant ;
- rejet déterministe des tokens cliniques non sourcés par `AiClinicalFactualityGuard` ;
- validation explicite du praticien avant application.

## Cycle de vie des brouillons

| Situation | Brouillon local | Working set serveur |
|---|---|---|
| Écoute / analyse progressive | Conservé | Actif |
| Fermeture avant application | Conservé | Actif |
| Application au formulaire sans sauvegarde | Nettoyé localement après synchronisation | Actif et récupérable |
| Sauvegarde clinique réussie | Déjà appliqué | Consommé en best effort |
| Sauvegarde clinique échouée | Formulaire non confirmé | Reste actif |
| « Tout supprimer » | Effacé | Écarté avec audit |

## Tests ajoutés

### Flutter

- `clinical_voice_capture_api_test.dart`
  - restauration du working set actif ;
  - persistance avant analyse progressive ;
  - reconstruction finale ;
  - consommation explicite.
- `clinical_voice_progressive_flow_test.dart`
  - ordre ACK → IA ;
  - absence d'erreur terminale pendant l'écoute ;
  - restauration locale avant coordination serveur ;
  - raccordement Consultation et Constantes ;
  - reconstruction finale depuis le working set ;
  - consommation après réponse de sauvegarde.
- `consultation_api_voice_cleanup_test.dart`
  - nettoyage après save uniquement ;
  - save clinique prioritaire sur une panne de nettoyage.
- `vitals_api_voice_cleanup_test.dart`
  - mêmes invariants pour les constantes.

### Spring Boot

- `AiClinicalReformulationContractTest`
  - reformulation grammaticale sûre ;
  - suppression d'un enrichissement diagnostique non sourcé ;
  - présence des règles de fidélité et de preuves exactes.

## Sécurité et conformité

- Authentification, RBAC, consentement et frontières tenant inchangés.
- Aucune donnée clinique ajoutée aux logs.
- Aucun nouveau secret, stockage, schéma SQL ou délai de rétention.
- Les intakes existants conservent leur audit et passent à `CONSUMED` ou `DISCARDED`.
- L'IA reste une aide à la structuration ; elle n'applique aucune décision clinique seule.

## Validation attendue

- `dart format --output=none --set-exit-if-changed lib test`
- `flutter analyze --no-pub`
- `flutter test --no-pub`
- `./mvnw clean verify -B --no-transfer-progress -Dspring.profiles.active=test`
- recette Android réelle : parole longue, silences, reprise automatique, analyse lente,
  mode avion/reconnexion, correction, suppression, fermeture/réouverture, sauvegarde Consultation
  et sauvegarde Constantes.

## Version

Correctif rétrocompatible candidat **PATCH**. Aucun breaking change API et aucune migration DB.
