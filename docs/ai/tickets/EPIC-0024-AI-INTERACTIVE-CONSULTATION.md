# EPIC-0024 — Consultation IA interactive et corrections contrôlées

- **Mode** : Project Manager + Engineering + Architecture + QA Review
- **Statut** : IN_PROGRESS — documentation initiale créée, développement à démarrer
- **Priorité** : P1
- **Branche** : `feat/ai-interactive-consultation`
- **Reviewer recommandé** : Tech Lead + médecin référent + QA + DPO

## Problème

Le pilote actuel couvre la dictée audio par bloc et la génération d’un brouillon, mais pas une véritable conversation médecin–assistant. La transcription est analysée immédiatement, la correction texte n’est pas historisée dans un fil visible, les clarifications ne sont pas structurées et le médecin ne peut pas accepter ou rejeter les modifications champ par champ.

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

- Backend : exposer des messages typés et horodatés dans la session.
- Frontend : extraire une façade API et un composant de fil conversationnel.
- Tests : ordre des messages, restauration de session, synchronisation entre appareils.

### STORY-2422 — Transcription éditable avant analyse — 5 SP

En tant que médecin, je veux corriger la transcription avant son analyse afin d’éviter qu’une erreur audio ne modifie le brouillon clinique.

- Séparer transcription et analyse.
- Ajouter un état `PENDING_REVIEW`.
- Permettre confirmer, modifier ou abandonner une transcription.

### STORY-2423 — Clarifications structurées — 5 SP

En tant que médecin, je veux répondre à une question de clarification liée à un champ précis afin que l’assistant corrige uniquement l’information concernée.

- DTO `ClarificationView` : id, field, question, status, options éventuelles.
- Réponse liée à l’identifiant de clarification.
- Une clarification résolue reste visible dans l’historique.

### STORY-2424 — Révisions et validation champ par champ — 8 SP

En tant que médecin, je veux comparer, accepter ou rejeter chaque modification afin de garder le contrôle sur le dossier.

- Proposition immuable : champ, ancienne valeur, nouvelle valeur, raison, niveau d’incertitude.
- Acceptation/rejet individuel ou global.
- Aucune modification du brouillon validé avant acceptation.
- Historique des révisions en session.

### STORY-2425 — Refactor UI, i18n et tests E2E — 5 SP

- Extraire le composant monolithique actuel.
- Aucun appel `HttpClient` direct dans les composants de présentation.
- Tailwind CSS v4, light/dark, textes FR/EN.
- Tests Angular des interactions critiques et parcours E2E du pilote.

## Critères d’acceptation globaux

- [ ] Le fil affiche tous les messages utilisateur et assistant dans l’ordre.
- [ ] Une transcription audio n’est jamais analysée avant confirmation explicite.
- [ ] Le médecin peut modifier le texte transcrit avant analyse.
- [ ] Une clarification possède un identifiant et un champ cible.
- [ ] Une réponse à une clarification est reliée à la bonne question.
- [ ] Les changements sont présentés sous forme de propositions avant/après.
- [ ] Le rejet d’une proposition conserve la valeur précédente.
- [ ] L’acceptation d’une proposition ne sauvegarde pas automatiquement la consultation.
- [ ] Le bouton final applique uniquement les valeurs acceptées au formulaire.
- [ ] La session reste isolée par visite, utilisateur et organisation.
- [ ] Aucun audio, transcript ou contenu clinique n’est écrit dans les logs.
- [ ] Les permissions actuelles `MEDECIN|ADMIN_CLINIQUE + CLINICAL_WRITE` sont conservées.
- [ ] Tests Maven, Angular et CI verts.

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
- [ ] Refactorer le backend en use cases et modèles de session explicites.
- [ ] Implémenter transcription en attente de validation.
- [ ] Implémenter conversation et clarifications structurées.
- [ ] Implémenter propositions et décisions champ par champ.
- [ ] Extraire la façade Angular et les composants réutilisables.
- [ ] Ajouter i18n FR/EN et états light/dark.
- [ ] Ajouter tests backend, frontend et E2E.
- [ ] Exécuter `./mvnw clean verify`, `npm test`, `npm run build`.
- [ ] Mettre à jour tracking, changelog, checklist et documentation finale.

## Estimation

| Profil | Charge indicative |
|---|---:|
| Senior full-stack | 8 à 10 jours |
| Intermédiaire encadré | 11 à 14 jours |
| Junior encadré | à redécouper, non recommandé seul |

## Risques

- Données de santé envoyées au fournisseur IA.
- Session en mémoire incompatible avec plusieurs instances backend.
- Risque de perte de contexte après redémarrage.
- Complexité UX si trop de propositions sont générées à chaque tour.
- Confusion entre brouillon IA, brouillon accepté et formulaire sauvegardé.

## Impact version

- **SemVer prévu** : MINOR, fonctionnalité rétrocompatible ajoutée.
- Aucun endpoint existant ne sera supprimé silencieusement ; les contrats actuels restent compatibles ou sont versionnés/additifs.
