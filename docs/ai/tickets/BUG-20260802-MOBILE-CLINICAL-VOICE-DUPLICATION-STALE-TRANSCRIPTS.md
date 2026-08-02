# BUG-20260802 — Répétitions vocales, anciens transcripts et pertes de mots

## 1. Objectif

Corriger de bout en bout l'assistant vocal Flutter de consultation et de saisie
des constantes afin qu'une parole ne soit capturée qu'une fois, qu'un transcript
finalisé ne réapparaisse pas et qu'une reprise du moteur ne perde pas les mots à
la frontière de deux fenêtres de reconnaissance.

## 2. Critères d'acceptation

- [ ] Une seule opération `speech_to_text.listen()` peut démarrer à la fois.
- [ ] Un callback provenant d'un ancien cycle d'écoute ne modifie pas le cycle courant.
- [ ] Une fenêtre ASR rejouée deux à quatre fois n'est présente qu'une fois dans le transcript.
- [ ] Un chevauchement récent avec une variation ASR mineure conserve tous les mots sans dupliquer la phrase.
- [ ] Seul un transcript serveur `PENDING_REVIEW` peut être restauré par Flutter ; `ANALYZED` et `NONE` démarrent vides.
- [ ] « Tout supprimer » reste effectif après fermeture/réouverture et ne peut pas être écrasé par `dispose()`.
- [ ] Le backend n'expose plus le texte analysé comme brouillon restaurable et utilise `eventId` pour rendre un retry realtime idempotent.
- [ ] La liste des intakes de constantes exclut les éléments consommés ou écartés.
- [ ] L'interface ne présente jamais simultanément un état d'écoute actif et une erreur terminale.

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
| Risque fonctionnel | Fort |
| Risque technique | Fort |
| Dépendances | `speech_to_text`, session IA consultation, secure storage |
| Bloquants connus | Validation réelle du microphone Android hors tests unitaires |

## 4. Contexte analysé

- [x] Gouvernance, workflow, tracking, changelog et checklist relus.
- [x] `DESIGN.md`, standards Flutter/Spring, SOLID et configuration relus.
- [x] Capture utilisateur analysée : état « Paroles en cours » contradictoire avec une erreur terminale et transcript tronqué.
- [x] Chaîne Flutter capture → hypothèses → brouillon local → analyse → application inspectée.
- [x] Chaîne Spring session → pending transcript → analyse realtime → restitution inspectée.
- [x] Tests existants et trous de couverture inspectés.
- [x] Aucun changement DB, secret, permission ou configuration requis.

## 5. Rapport diagnostic

Le rapport détaillé et les preuves sont consignés dans
`docs/ai/diagnostics/DIAG-20260802-MOBILE-CLINICAL-VOICE-END-TO-END.md`.

Causes racines retenues :

1. course entre la reprise sur statut `done/notListening` et la reprise après erreur ;
2. absence de verrou pendant l'appel asynchrone à `listen()` ;
3. déduplication limitée au préfixe du transcript complet, insuffisante pour les fenêtres Android rejouées ;
4. restauration Flutter du dernier transcript serveur même lorsqu'il est `ANALYZED` ;
5. marqueur local `explicitlyCleared` ignoré et potentiellement écrasé à la destruction du service ;
6. `eventId` realtime reçu par le backend mais non utilisé ;
7. endpoint Vitals listant aussi les intakes déjà consommés/écartés.

## 6. Risques et impacts

| Risque | Impact | Mitigation |
|---|---|---|
| Déduplication trop agressive | Suppression d'une répétition réellement dictée | Limiter la fusion au suffixe récent et exiger plusieurs tokens avec forte similarité |
| Reprise audio bloquée | Arrêt de capture après timeout Android | Conserver le backoff existant avec un verrou et un identifiant de cycle |
| Perte d'un brouillon utile | Donnée clinique non revue | Restaurer intégralement `PENDING_REVIEW`, ne nettoyer que les états analysés/effacés |
| Retry IA exécuté plusieurs fois | Révisions dupliquées | Cache idempotent borné au cycle de vie de la session par `eventId` |
| Régression de contrat | Clients existants | `eventId` reste optionnel ; comportement inchangé quand il est absent |

## 7. Actions

- [x] Documenter le diagnostic, les invariants et le plan de test.
- [ ] Corriger la coordination et la déduplication Flutter.
- [ ] Corriger la restauration et l'effacement Flutter.
- [ ] Rendre l'analyse realtime backend idempotente et nettoyer le transcript analysé.
- [ ] Limiter le listing Vitals au working set actif.
- [ ] Ajouter les tests Flutter et Spring ciblés.
- [ ] Exécuter les tests ciblés, l'analyse Dart et les vérifications Maven possibles.
- [ ] Mettre à jour changelog et suivi avec les résultats réels.

## 8. Sécurité / conformité

- AuthN/RBAC inchangés ; aucune donnée clinique n'est loguée par le correctif.
- Le backend reste maître de l'analyse et des décisions cliniques.
- L'effacement porte uniquement sur le brouillon vocal de la visite courante.
- Aucun secret, nouveau stockage ou changement de rétention n'est introduit.

## 9. Impact version / SemVer

| Champ | Valeur |
|---|---|
| Changement livrable | Oui |
| Type de bump | PATCH |
| Justification | Correctif rétrocompatible de capture, reprise et restauration |
| Breaking change | Non |
| Migration DB | Non |
| Changement API | Comportement idempotent/filtré, schéma inchangé |
| Impact Flutter | Oui |
| Impact Angular | Non |
| Release note requise | Oui au prochain lot mobile |

## 10. Statut

Statut : IN_PROGRESS
