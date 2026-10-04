# UX-20261004 — Clarifier l'entrée en consultation

Mode : conception UI/UX puis Diagnostic / Engineering / QA ; rattaché au parcours consultation et aux travaux cliniques existants. Demande initiale : générer une image avant implémentation ; maquette ensuite acceptée et correction de l'écran vide ajoutée par l'utilisateur.

Statut : READY_FOR_REVIEW — code et tests automatisés verts ; recette navigateur / design QA encore ouverte.

## Objectif et acceptation
Le médecin comprend les trois modes disponibles, voit clairement ce qui va démarrer et garde le contexte patient/alertes visible. Préserver la marque, la navigation et le design system. Une maquette desktop acceptée puis implémentation Angular ; corriger le formulaire annoncé mais masqué.

## Actions
- [x] Examiner directement la capture, DESIGN.md et les écrans Angular existants.
- [x] Définir une hiérarchie : contexte patient, étapes du parcours, choix explicites, une action principale contextualisée.
- [x] Générer et examiner la proposition visuelle.
- [x] Recueillir les ajustements de l'utilisateur avant implémentation : proposition acceptée.
- [x] Diagnostiquer la capture supplémentaire : état FORM_READY de l'assistant indépendant de la visibilité parent fondée sur `form.dirty` ; output `formReadyChange` non relié.
- [x] Implémenter le choix explicite et son action de démarrage dans un composant dédié.
- [x] Synchroniser l'ouverture du formulaire pour saisie manuelle, brouillon chargé et validation explicite du compte rendu.
- [x] Ajouter les tests de parcours, les traductions FR/EN et vérifier le build.
- [x] Mettre à jour diagnostic, checklist, changelog et suivi.
- [ ] Recette visuelle dans le navigateur et microphone réel, responsive/light/dark/FR/EN/clavier.

## Périmètre d'engineering accepté
Correction bornée Angular, 0.5–1 jour senior frontend indicatif, hors sprint et sans engagement de date. Deux sous-tâches : entrée conforme à la maquette ; disparition du formulaire et libellé trompeur. Reviewer : utilisateur / référent clinique + frontend. Critères : aucune capture ne démarre à la sélection ; les trois modes ont un CTA explicite ; les champs et les actions de sauvegarde apparaissent après ouverture manuelle ou validation du rapport et pour une consultation chargée ; les saisies conservées ne sont pas effacées à la reprise. Tests réels du DOM et des événements, sans sauvegarde clinique automatique. PATCH candidat, aucun contrat API/DB/backend/mobile/CI modifié.

## Proposition
Titre « Démarrer la consultation ». Modes « Saisie manuelle », « Dictée » et « Conversation avec le patient », décrits sans jargon. Distinguer sélection du mode et démarrage effectif ; afficher l'état « Microphone arrêté » et un bouton explicite. Le parcours assisté expose capture, relecture et formulaire clinique, sans annoncer un résultat déjà validé. Alertes patient lisibles, couleurs/tokens centraux et rayons sobres.

## Risques et validation
Une image est une proposition, pas une preuve de fonctionnement, d'accessibilité ou de validation clinique. Le code Angular est implémenté ; aucun bump/release. Accès navigateur précédemment refusé : aucune capture d'implémentation ni comparaison visuelle attestée ; `design-qa.md` indique `final result: blocked`. Ne pas déclarer le ticket DONE avant recette/review.

## Preuves de tests
- Suite complète `npm test -- --watch=false` : 115 fichiers, 619 tests réussis (15 nouveaux tests DOM/parcours).
- `npm run build` : build production réussi.
- `npm run i18n:check` : 47 clés shell FR/EN ; contrôle complémentaire des 37 nouvelles clés consultation et des 12 tokens existants : réussi.
- `git diff --check` : réussi ; aucun script lint configuré dans `web/package.json`.
- Logs locaux ignorés : `.ai-tmp/consultation-entry-tests-full.log`, `.ai-tmp/consultation-entry-build.log` ; helper de contrôle i18n/tokens `.ai-tmp/check-consultation-entry.py`.
- Sécurité/12-Factor : aucun endpoint, permission, sanitizer, secret, stockage ou configuration ajouté ; interpolation Angular et validations/sauvegarde explicites conservées. Backend/DB/Flutter non modifiés, tests Maven/Flutter non requis pour ce lot.
- Alerte de taille existante : consultation 470 lignes, panneau vocal 441 lignes ; restent sous 500, nouveau composant de sélection 38 lignes. Aucun refactor métier massif dans cette correction bornée.

## Preuve visuelle
Une image générée depuis la capture et présentée dans la conversation. Examen visuel : trois modes identifiables, sélection distincte du démarrage, bouton contextualisé, état du microphone et alertes patient visibles. La validation responsive, light/dark, FR/EN et clavier sera nécessaire lors de l'implémentation.

Artefact local : `C:/Users/Jocelin FOMEKONG/.codex/generated_images/01a10452-2660-7ab3-835e-6a6316484fb3/exec-0645e321-75bd-458b-ba80-e3c3196858ec.png`. L'image n'est pas ajoutée au dépôt ; les données de la capture servent uniquement de contexte visuel.

## Versionnement local
Demande utilisateur du 2026-10-04 : « commit ». Périmètre commun avec UX-20261004-VITALS-ENTRY-CLARITY : corrections UI, tests, traductions et documentation associée. Validation intégrée finale : 633 tests Angular réussis, build production et i18n verts. Aucun push, tag ou release demandé ; review et recette visuelle/microphone réel restent ouvertes.
