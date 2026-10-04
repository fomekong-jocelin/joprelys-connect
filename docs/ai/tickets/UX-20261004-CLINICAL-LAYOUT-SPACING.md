# UX-20261004-CLINICAL-LAYOUT-SPACING

Mode : Diagnostic / Engineering / QA. Statut : READY_FOR_REVIEW — contrôles automatiques verts ; recette visuelle et dette de template ouvertes.

## Besoin et acceptation
Retour utilisateur sur deux captures : footer d'admission avec boutons dispersés ; étapes de consultation accolées au bandeau patient. Actions d'admission alignées en pleine largeur, consultation en premier, fermeture secondaire séparée. Espacement explicite entre patient, étapes et choix ; étapes empilées sur petit écran et distribuées sur trois colonnes à partir de sm. Conserver libellés FR/EN, événements, droits et alertes cliniques.

## Actions
- [x] Lire standards, captures, templates et composant Button partagé.
- [x] Créer spécification, conception et diagnostic avant code.
- [x] Corriger footer et espacement de consultation avec utilitaires Tailwind v4 existants.
- [x] Exécuter tests Angular existants et build production ; contrôler le diff.
- [x] Mettre à jour suivi, changelog et checklist.
- [ ] Recette visuelle desktop/mobile FR/EN light/dark (accès navigateur précédemment refusé).

## Impacts et préparation
Angular présentation uniquement ; aucun changement API, backend, DB, Flutter, configuration, CI/CD, permissions ni validation métier. OWASP et 12-Factor : aucun nouvel input, HTML dynamique, secret, stockage ou variable d'environnement. Risque borné : footer plus haut ; le corps doit rester défilable et les zones fixes non rétrécissables. Pas de nouvelle architecture/ADR. PATCH candidat, pas de bump ou livraison. Estimation indicative 0.2j frontend, hors sprint sans engagement de capacité ; reviewer frontend + utilisateur. READY : captures et acceptation ci-dessus, conception présente. DONE : vérifications consignées et recette/review visuelle acceptées.

Tests : réutiliser les scénarios de consultation/drawer ; pas de test miroir de classes CSS pour un ajustement de présentation. Le build doit générer les utilitaires de layout, et la recette doit vérifier le rendu et les labels anglais longs.

## Versionnement local
Demande utilisateur du 2026-10-04 : « commit », puis « continue ». Périmètre : trois templates de layout et documentation associée. Commit local uniquement ; aucun push, tag ou release demandé. Les contrôles automatiques précédents restent valides, aucun code modifié depuis leur exécution. Recette visuelle et review de conformité restent ouvertes.

## Résultats et reste à faire
633 tests / 115 fichiers Angular verts ; build production réussi, i18n shell 47 clés FR/EN et diff sans erreurs. Présence des utilitaires de colonne/gap/grille/largeur/cible 44px vérifiée dans le CSS compilé. Logs ignorés : `.ai-tmp/clinical-layout-tests.log` et `.ai-tmp/clinical-layout-build.log`. Aucun lint script configuré. Pas de test de classes ajouté ; comportement couvert par la suite existante.

Dette existante : template consultation 800 lignes, nombre inchangé par ce correctif d'une classe du conteneur ; dépasse la limite HTML de 300 du standard SOLID. Review de conformité bloquée tant qu'une extraction ou exception ADR acceptée ne traite pas cette dette ; ne pas prétendre à une conformité globale. Drawer 297 lignes (alerte >200, sous limite 300), entrée 136. Extraction à traiter séparément pour éviter un refactor clinique dans ce correctif de spacing. Restent review et recette visuelle.
