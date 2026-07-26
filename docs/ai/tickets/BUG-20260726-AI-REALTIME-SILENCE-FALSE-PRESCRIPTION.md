# BUG-20260726-AI-REALTIME-SILENCE-FALSE-PRESCRIPTION

## Qualification

- **Mode** : Diagnostic + Engineering + QA Review
- **Epic** : EPIC-0024 / AI_VOICE_CONSULTATION
- **Priorité** : P0 — sécurité clinique
- **Statut** : IMPLEMENTED — QA ciblée verte, recette clinique requise
- **Stack** : Angular / WebRTC / Spring Boot / OpenAI transcription
- **Profil recommandé** : senior full-stack + reviewer clinique
- **Reviewer** : Tech Lead + médecin référent + QA
- **Estimation** : 2 à 4 jours avec recette clinique
- **Impact SemVer prévu** : PATCH rétrocompatible

## Problème

Lorsque la création du canal OpenAI Realtime échoue, le frontend réessaie à chaque
rafraîchissement de session. Le mode audio classique prend le relais, peut redémarrer
automatiquement le microphone après la voix de l'assistant et transmet un conteneur
audio même lorsqu'aucune parole n'a été prononcée. La transcription peut alors
halluciner une prescription qui est présentée au médecin comme une dictée explicite.

## Causes confirmées sur `main`

- [x] Le délai d'ouverture du data channel reste armé quand le POST SDP échoue.
- [x] Un changement d'objet session déclenche une nouvelle tentative Realtime.
- [x] Le polling renouvelle l'objet session toutes les quatre secondes.
- [x] Le mode classique redémarre le microphone après la synthèse vocale.
- [x] Le contrôle audio vérifie la taille du Blob, pas la présence de parole.
- [x] Le mode conversationnel appelle l'ancien endpoint audio qui transcrit puis
  analyse immédiatement sans relecture.
- [x] Le garde ordonnance fait confiance au texte transcrit et ne peut pas prouver
  l'origine acoustique d'un nom de médicament.

## Critères d'acceptation

- [x] Un échec du POST Realtime ne produit aucun
  `AI_REALTIME_CHANNEL_TIMEOUT` tardif/non géré.
- [x] Une exception Realtime inattendue retourne un `503` générique et sûr au lieu
  d'un `500` non qualifié.
- [x] Une seule tentative Realtime est faite par visite jusqu'à une action explicite
  de reconnexion.
- [x] Le micro classique ne démarre jamais automatiquement.
- [x] Une capture sans activité vocale suffisante n'est pas envoyée au backend.
- [x] Toute dictée classique, y compris en mode conversationnel dégradé, produit une
  transcription `PENDING_REVIEW`.
- [x] Aucune analyse clinique n'est lancée avant confirmation explicite du texte.
- [x] Une transcription dont la confiance fournisseur est sous le seuil configuré
  est rejetée sans message clinique, révision ni prescription.
- [x] Les libellés de sécurité sont disponibles en français et en anglais.
- [ ] Les tests Angular et Maven ciblés passent : Angular vert ; Maven non exécutable
  dans l'environnement local faute de parent Spring Boot 4.1.0 en cache.
- [x] Les builds complets pertinents passent ou les limites sont documentées.

## Plan d'action

- [x] Lire la gouvernance, les standards et les tickets IA existants.
- [x] Vérifier le nouveau `main` et confirmer la présence des causes.
- [x] Créer la documentation fonctionnelle, technique et le plan de test.
- [x] Corriger le cycle de vie WebRTC et le circuit de reconnexion.
- [x] Rendre la dictée classique manuelle et contrôlée.
- [x] Ajouter la détection locale de parole et le seuil de confiance backend.
- [x] Ajouter les tests de non-régression.
- [x] Exécuter les validations Angular et consigner le blocage Maven local.
- [x] Mettre à jour le changelog, le suivi projet et la checklist QA.

## Definition of Done

- [x] Aucun P0/P1 ouvert dans le périmètre corrigé au niveau du code.
- [x] Preuves de tests consignées.
- [x] Documentation alignée avec le comportement livré.
- [ ] Recette réelle silence, voix faible, bruit et médicament planifiée avec un
  médecin référent.

## Reste à faire

Exécuter la suite Maven dans un environnement disposant des dépendances, puis réaliser
la recette clinique réelle silence/voix faible/bruit. L'ancien endpoint
`POST /messages/audio`, conservé pour les clients historiques, doit rester suivi
jusqu'à migration complète vers le parcours en deux temps. Les trois classes
signalées entre 300 et 500 lignes dans la review restent une dette de découpage non
bloquante.
