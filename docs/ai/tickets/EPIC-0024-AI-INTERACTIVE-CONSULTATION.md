# EPIC-0024 — Consultation IA interactive et corrections contrôlées

- **Mode** : Project Manager + Engineering + Architecture + QA Review
- **Statut** : IN_PROGRESS — STORY-2421 et STORY-2422 implémentées et validées par la CI
- **Priorité** : P1
- **Branche** : `feat/ai-interactive-consultation`
- **Reviewer recommandé** : Tech Lead + médecin référent + QA + DPO

## Problème

Le pilote initial couvrait la dictée audio par bloc et la génération d’un brouillon, mais pas une véritable conversation médecin–assistant. La transcription était analysée immédiatement, la correction texte n’était pas historisée dans un fil visible, les clarifications n’étaient pas structurées et le médecin ne pouvait pas accepter ou rejeter les modifications champ par champ.

## Résultat attendu

Fournir une conversation multi-tour visible permettant au médecin de :

1. dicter un segment audio ;
2. relire et corriger la transcription avant analyse ;
3. envoyer un message texte ou une correction ;
4. voir les échanges médecin/assistant dans un historique ;
5. répondre à une clarification explicitement liée à un champ ;
6. comparer ancienne et nouvelle valeur ;
7. accepter ou rejeter chaque proposition ;
8. appliquer uniquement les propositions validées au formulaire clinique.

## Découpage

### STORY-2421 — Conversation clinique multi-tour visible — 5 SP

En tant que médecin, je veux voir l’historique de mes messages et des réponses de l’assistant afin de comprendre comment le brouillon évolue.

- [x] Exposer des messages utilisateur/assistant typés et horodatés dans la session.
- [x] Séparer le fil clinique visible des prompts techniques et réponses JSON du fournisseur.
- [x] Ajouter un composant Angular dédié au fil conversationnel.
- [x] Afficher les échanges texte et les transcriptions audio confirmées dans l’ordre.
- [x] Restaurer le fil lors du polling et de la reprise de session sur un autre appareil avec le même compte.
- [x] Ajouter les tests backend multi-tour et le test Angular d’affichage.
- [ ] Réaliser la recette visuelle authentifiée sur ordinateur et téléphone.
- **État** : DONE techniquement, recette visuelle restante.

### STORY-2422 — Transcription éditable avant analyse — 5 SP

En tant que médecin, je veux corriger la transcription avant son analyse afin d’éviter qu’une erreur audio ne modifie le brouillon clinique.

- [x] Séparer transcription et analyse.
- [x] Ajouter un état `PENDING_REVIEW`.
- [x] Permettre confirmer, modifier ou abandonner une transcription.
- [x] Conserver l’ancien endpoint audio pour compatibilité descendante.
- [x] Extraire les appels HTTP Angular dans une façade dédiée.
- [x] Ajouter les tests backend et frontend ciblés.
- [x] Valider Maven, les tests Angular et le build Angular de production dans la CI.
- [ ] Réaliser la recette visuelle authentifiée.

### STORY-2423 — Clarifications structurées — 5 SP

En tant que médecin, je veux répondre à une question de clarification liée à un champ précis afin que l’assistant corrige uniquement l’information concernée.

- DTO `ClarificationView` : id, field, question, status, options éventuelles.
- Réponse liée à l’identifiant de clarification.
- Une clarification résolue reste visible dans l’historique.
- **État** : TODO, validation médecin référent requise.

### STORY-2424 — Révisions et validation champ par champ — 8 SP

En tant que médecin, je veux comparer, accepter ou rejeter chaque modification afin de garder le contrôle sur le dossier.

- Proposition immuable : champ, ancienne valeur, nouvelle valeur, raison, niveau d’incertitude.
- Acceptation/rejet individuel ou global.
- Aucune modification du brouillon validé avant acceptation.
- Historique des révisions en session.
- **État** : TODO.

### STORY-2425 — Refactor UI, i18n et tests E2E — 5 SP

- [x] Extraire la façade API et supprimer les appels `HttpClient` directs du panneau.
- [x] Extraire le fil conversationnel dans un composant réutilisable.
- [ ] Extraire l’éditeur de transcription et les propositions dans des composants réutilisables.
- [x] Compléter les clés i18n FR/EN du parcours livré.
- [x] Ajouter un test Angular du fil conversationnel.
- [ ] Ajouter les tests E2E du pilote.

## Contrats ajoutés

### STORY-2421

Les réponses de session et de message exposent désormais `conversation` :

- `id` ;
- `role` : `USER` ou `ASSISTANT` ;
- `content` ;
- `source` : `TEXT`, `AUDIO`, `AI` ou `SYSTEM` ;
- `createdAt` ;
- `needsClarification`.

Le contenu technique utilisé pour le contexte du modèle reste interne et n’est jamais retourné au frontend.

### STORY-2422

- `POST /api/ai/consultations/{visitId}/transcriptions/audio`
  - transcrit uniquement ;
  - ne lance jamais le modèle de structuration ;
  - retourne `PENDING_REVIEW`.
- `POST /api/ai/consultations/{visitId}/transcriptions/analyze`
  - reçoit le texte relu/corrigé ;
  - lance explicitement l’analyse clinique.
- `DELETE /api/ai/consultations/{visitId}/transcriptions/pending`
  - abandonne le texte en attente sans appel au modèle de chat.
- `POST /messages/audio` reste disponible temporairement pour les clients historiques.

## Optimisation CI

- [x] Cache Maven ciblé par `backend/pom.xml` et le Maven Wrapper.
- [x] Cache npm ciblé par `web/package-lock.json`.
- [x] Cache incrémental Angular partagé entre les pipelines.
- [x] Installation npm en mode préférentiellement hors ligne, sans audit ni collecte de financement dans la CI.
- [x] Suppression de la progression Maven inutile dans les logs.
- [x] Annulation automatique des anciennes exécutions d’une même PR lorsqu’un nouveau commit arrive.
- [x] Ignorer les workflows déclenchés uniquement par des documents Markdown hors PR comportant du code.

## Critères d’acceptation globaux

- [x] Le fil affiche les messages utilisateur et assistant dans l’ordre.
- [x] Le fil visible ne contient aucun prompt technique, brouillon sérialisé ou JSON brut du fournisseur.
- [x] Une transcription audio n’est jamais analysée avant confirmation explicite avec le nouveau parcours.
- [x] Le médecin peut modifier le texte transcrit avant analyse.
- [ ] Une clarification possède un identifiant et un champ cible.
- [ ] Une réponse à une clarification est reliée à la bonne question.
- [ ] Les changements sont présentés sous forme de propositions avant/après.
- [ ] Le rejet d’une proposition conserve la valeur précédente.
- [ ] L’acceptation d’une proposition ne sauvegarde pas automatiquement la consultation.
- [ ] Le bouton final applique uniquement les valeurs acceptées au formulaire.
- [x] La session reste isolée par visite, utilisateur et organisation.
- [x] Aucun audio, transcript ou contenu clinique n’est écrit dans les logs.
- [x] Les permissions actuelles `MEDECIN|ADMIN_CLINIQUE + CLINICAL_WRITE` sont conservées.
- [x] Tests Maven, Angular et build de production verts sur la branche.

## Definition of Ready

- [x] Besoin et limites documentés.
- [x] Stories découpées.
- [x] Critères d’acceptation définis.
- [x] Documentation fonctionnelle et technique initiale créée.
- [x] Stratégie API et tests documentée.
- [ ] Validation métier du comportement de clarification par un médecin référent.

## Plan d’action

- [x] Lire les règles obligatoires du dépôt.
- [x] Analyser le code, la PR #57 et les tests actuels.
- [x] Créer la branche dédiée.
- [x] Créer le cadrage Documentation First.
- [x] Implémenter transcription en attente de validation.
- [x] Extraire la façade Angular.
- [x] Implémenter le fil conversationnel visible.
- [x] Ajouter les tests unitaires STORY-2421 et STORY-2422.
- [x] Valider `./mvnw clean verify`, `npm test`, `npm run build` via CI.
- [x] Optimiser les caches et la consommation de runners CI.
- [ ] Implémenter les clarifications structurées.
- [ ] Implémenter les propositions et décisions champ par champ.
- [ ] Extraire les derniers sous-composants Angular.
- [ ] Ajouter E2E et recette clinique.
- [ ] Mettre à jour changelog et checklist après recette fonctionnelle.

## Estimation

| Profil | Charge indicative |
|---|---:|
| Senior full-stack | 8 à 10 jours pour l’epic complète |
| STORY-2421 + STORY-2422 | implémentées, recette clinique restante |
| Intermédiaire encadré | 11 à 14 jours pour l’epic complète |
| Junior encadré | à redécouper, non recommandé seul |

## Risques

- Données de santé envoyées au fournisseur IA.
- Session en mémoire incompatible avec plusieurs instances backend.
- Risque de perte de contexte après redémarrage.
- Le endpoint historique conserve l’analyse immédiate pour compatibilité : les nouveaux clients doivent obligatoirement utiliser le parcours en deux étapes.
- Complexité UX si trop de propositions sont générées à chaque tour.
- Confusion entre transcription, brouillon IA, brouillon accepté et formulaire sauvegardé.

## Impact version

- **SemVer prévu** : MINOR, fonctionnalité rétrocompatible ajoutée.
- Aucun endpoint existant n’est supprimé ; les nouveaux contrats sont additifs.
