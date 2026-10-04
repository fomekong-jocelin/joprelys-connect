# UX-20261004 — Clarifier la saisie des constantes

Mode : Diagnostic / Engineering / QA. Rattaché au parcours constantes → consultation et au précédent lot de clarification de consultation. Demande : corriger l'ambiguïté de la modale montrée dans la capture utilisateur.

Statut : READY_FOR_REVIEW — 633 tests complets, build final et i18n verts ; recette visuelle/microphone réel ouverte.

## Acceptation et périmètre
- Saisie manuelle identifiable comme tâche principale ; dictée ponctuelle et écoute continue décrites séparément.
- Choisir un mode ne démarre pas le microphone ; action explicite et état affiché.
- Texte optionnel dans un bloc pleine largeur, avec label et action explicites ; pas de placeholder tronqué entre deux boutons.
- Proposition IA relue puis reportée au formulaire explicitement ; aucune sauvegarde automatique. Distinction claire report / enregistrement.
- En-tête patient et actions de sauvegarde hors du corps défilant ; unités et labels associés aux champs.
- Traductions FR/EN, tokens light/dark, radios natives et arrondis sobres ; conserver mesures précédentes et validations.

## Actions
- [x] Lire le code modale/assistant, les tests existants, la capture et DESIGN.md.
- [x] Documenter le diagnostic : Temps réel active l'écoute lors du choix ; grille basée sur largeur écran comprime le texte dans une modale étroite ; actions au bas du contenu défilant ; patient répété.
- [x] Implémenter choix sans démarrage, CTA explicite et texte optionnel séparé.
- [x] Corriger disposition et libellés de modale, labels et navigation clavier.
- [x] Tester sélection/capture/validation/sauvegarde/erreurs et i18n : 14 nouveaux tests, suite complète 633/633 verte ; 26 nouvelles clés FR/EN, 2 libellés clarifiés et 24 tokens contrôlés.
- [x] Consigner le build final après ajustement des cibles tactiles : `npm run build` réussi ; `git diff --check` réussi.
- [x] Mettre à jour suivi, changelog, checklist et preuves.
- [ ] Recette navigateur et microphone réel (accès précédemment refusé).

## Impacts / risques
Angular uniquement. Pas d'API/DB/Flutter/backend/CI ni dépendance nouvelle ; validation métier serveur inchangée, mesures non inventées. OWASP : interpolation Angular, permissions inchangées, aucun secret ou contenu HTML ajouté. 12-Factor : aucune configuration/env créée. PATCH candidat, pas de bump/release. Estimation bornée 0.5–1j senior frontend indicative ; hors sprint, capacité non engagée. Reviewer frontend + utilisateur/référent clinique. Risques : états audio et valeurs antérieures, à couvrir par tests ; ne pas prétendre à une validation de rendu sans recette navigateur.

## Preuves et reste à faire
Diagnostic / review : `docs/ai/validation/QA-20261004-VITALS-ENTRY-CLARITY.md`. Logs locaux ignorés : `.ai-tmp/vitals-entry-tests-full.log`, `.ai-tmp/vitals-entry-build.log`. Helper de contrôle i18n/tokens : `.ai-tmp/check-vitals-entry.py`. Pas de lint script configuré ; aucun test Maven/Flutter nécessaire pour ce lot frontend. Alerte assistant 339 lignes (sous 500), modale 223, choix 65 ; refactor audio massif non requis ici. Restent recette visuelle/microphone réel et review.

## Versionnement local
Demande utilisateur du 2026-10-04 : « commit ». Périmètre : corrections consultation et constantes, tests, traductions et documentation associée. Commit local uniquement ; aucun push, tag ou release demandé. Captures, logs et artefacts de build restent ignorés. Le versionnement ne clôture pas la recette ni la review.
