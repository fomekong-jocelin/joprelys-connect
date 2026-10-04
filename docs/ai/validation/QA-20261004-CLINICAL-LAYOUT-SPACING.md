# Diagnostic et review — layout clinique

Preuves initiales : deux captures utilisateur du 2026-10-04 ; aucun contenu patient ajouté au dépôt.

- Admission : footer flex-wrap avec des hôtes app-ui-button flex-1, mais ButtonComponent transmet la même classe au bouton interne sans w-full ; bouton primaire étroit et fermetures dispersées selon le retour à la ligne.
- Consultation : le parent utilise space-y sur des hôtes Angular inline ; le bandeau ne bénéficie pas d'un espacement vertical fiable avec le workspace. La progression commence sans padding supérieur ; sa flex-row partage les étapes même sur une petite largeur.

Correction prévue : colonne d'actions pleine largeur et zones non rétrécissables ; conteneur flex-col/gap à la consultation ; grille de progression avec padding vertical. Autorisations, handlers et alertes inchangés.

Validation automatique : suite Angular complète 115 fichiers / 633 tests réussis ; build production réussi ; i18n shell 47 clés FR/EN ; diff sans erreurs. Utilitaires de layout présents dans le CSS compilé. Handlers et conditions de disponibilité conservés dans le diff ; aucune modification TS, API, validation ou autorisation. Logs locaux ignorés dans `.ai-tmp/clinical-layout-{tests,build}.log`.

Recette visuelle après modification : ouverte (accès navigateur précédemment refusé), ne pas assimiler tests DOM et preuve de rendu. Conformité de taille : template consultation existant 800 lignes (inchangé), dépasse la limite HTML de 300 ; bloque la review de conformité tant qu'extraction/ADR acceptée non traitée. Drawer 297 (alerte >200), entrée 136. Ne pas élargir ce correctif de présentation à une extraction clinique sans cadrage.
