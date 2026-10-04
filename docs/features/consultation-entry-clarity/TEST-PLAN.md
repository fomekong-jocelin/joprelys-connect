# Vérification — entrée et formulaire de consultation

- Choisir chacun des trois modes ne démarre ni session ni microphone ; seul le CTA déclenche le parcours correspondant.
- Saisie manuelle : formulaire vierge visible sans faux message de validation IA, sauvegarde toujours bloquée si champs requis manquants.
- Validation explicite d'un compte rendu : formulaire et boutons visibles, contenu accepté conservé.
- Chargement d'une consultation existante : champs visibles même lorsque le formulaire est pristine.
- Reprise de capture : état parent synchronisé, valeurs non effacées ; rouvrir le formulaire conserve les valeurs.
- Dictée non supportée, initialisation en cours : action désactivée et explication lisible.
- Tests Angular exécutés : 115 fichiers / 619 tests réussis, dont 15 nouveaux tests de sélection et de workspace. Le navigateur et les services audio sont simulés pour les assertions DOM ; aucune capture microphone réelle attestée.
- Contrôle de traduction : 47 clés shell et 37 nouvelles clés consultation FR/EN ; 12 tokens de design existants contrôlés.
- Build Angular production réussi ; `git diff --check` réussi. Logs dans `.ai-tmp/consultation-entry-tests-full.log` et `.ai-tmp/consultation-entry-build.log` (ignorés).
- Recette navigateur desktop/mobile, FR/EN, light/dark et clavier : à distinguer des tests DOM ; accès navigateur précédemment refusé, aucune preuve visuelle d'implémentation disponible à ce stade.
