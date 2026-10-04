# Conception — maquette avant développement

Ancrage : DESIGN.md, consultation.component.html, voice-assistant-panel.component.html et capture fournie. Identité existante : cyan #0b91b2, texte #0a1d3d, fond #f7fafc, surfaces blanches, typographies Montserrat/Inter, rayons 4–6px et séparateurs discrets.

La génération utilise la capture réelle comme référence visuelle. Implémentation acceptée : extraire `ConsultationEntryModeComponent` (sélection radio native, étapes, description du mode et CTA) et réutiliser les icônes UI et tokens existants. Le panneau vocal délègue les événements aux parcours existants ; aucune nouvelle intégration audio/API.

`ConsultationComponent.formWorkspaceReady` est un signal indépendant des valeurs et de dirty. Il devient vrai au chargement d'une consultation et consomme `formReadyChange` ; le panneau reçoit la même visibilité en input. Supprimer l'inférence de validation depuis les champs texte. La transition vers une capture realtime ne ferme le formulaire qu'après initialisation réussie, pour ne pas reproduire le vide si l'API échoue. Un indicateur local distingue rapport explicitement accepté et formulaire simplement ouvert. Les valeurs sont conservées pendant la reprise.

Validation : tests DOM des événements parent/enfant et des trois choix, suites consultation, i18n FR/EN et build. Recette visuelle navigateur séparée ; précédemment refusée, ne pas présenter les tests DOM comme preuve de fidélité visuelle. Angular seul ; backend maître, OWASP/auth et contrats inchangés, aucune nouvelle variable/configuration (12-Factor), aucune migration/Flutter/CI. SemVer : PATCH candidat, pas de préparation de release ni bump VERSION.
