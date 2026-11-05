# UX-20261105 — Rendre l'écran « Organisation hospitalière » compréhensible

Mode : Engineering (UX). Rattaché à HOS-ORG-001-A (`docs/features/hospital-organization-referentials`). Demande utilisateur du 2026-11-05, capture de `/clinic/hospital-organization` : « très complexe à comprendre, moi-même qui l'ai développé ne trouve pas sa fonction ; simplifier, rendre intuitif, créer une hiérarchie visuelle ».

Statut : READY_FOR_REVIEW — web 775 tests verts (27 sur ce dossier), build de production OK ; recette Maestro écrite, non exécutée.

## Diagnostic
1. **La hiérarchie est invisible.** Le composant calcule une profondeur (`depth`) mais ne l'affiche pas : un pôle et ses services apparaissent comme des cartes identiques, à plat. La spec (§ 7) demandait pourtant une indentation progressive.
2. **Les référentiels dominent la page.** Les catalogues « Types de services » et « Spécialités médicales » (lecture seule, ~60 cartes) occupent les deux tiers de l'écran et ressemblent à des services configurés, alors que l'état vide dit « Aucune unité configurée ». Les spécialités ne servent pas à cet écran (elles servent aux affectations du personnel).
3. **Le but de l'écran n'est dit nulle part.** Sous-titre technique (« sans confondre organisation et localisation »), carte d'introduction qui répète le titre, surtitres décoratifs.
4. **Bloc « responsables » isolé** entre l'arbre et les catalogues, sans lien visible avec le reste.
5. **Formulaire dans le désordre** : niveau, code technique obligatoire à inventer, rattachement, type, nom. Aucun texte n'explique les niveaux.
6. **Actions bruyantes** : trois boutons par ligne, dont un « Désactiver » rouge partout ; « Ajouter un niveau » ne dit pas ce qui sera créé.
7. **Erreur invisible** : un échec d'enregistrement s'affiche derrière la fenêtre de saisie.

## Acceptation
- Le sous-titre dit à quoi sert l'écran et ce que les services alimentent (personnel, chambres, hospitalisations).
- L'arbre montre l'imbrication pôle › département › service › unité de soins (indentation + filet), sur mobile comme sur desktop.
- Une légende courte explique les niveaux : seul le service est indispensable.
- Chaque ligne propose au plus deux actions nommées (« Ajouter un service », « Ajouter une unité de soins », « Modifier ») ; désactiver se fait depuis la fenêtre de modification ; une unité inactive propose « Réactiver ».
- Les référentiels sont repliés dans une section « Référentiels standard » en bas de page.
- Formulaire : niveau expliqué (choix avec description), puis type de service, nom, rattachement, code interne proposé automatiquement (modifiable, validé en ligne avec la règle du serveur).
- Les erreurs d'enregistrement s'affichent dans la fenêtre ; la saisie n'est jamais perdue.
- États : chargement (squelette), vide (une action principale), erreur, succès. FR/EN, thèmes clair/sombre, arrondis ≤ 8 px, focus visible.

## Actions
- [x] Lire composant, spec, traductions, contraintes serveur (code `[A-Z][A-Z0-9_]{2,63}`, unicité par établissement, règles de rattachement RF-06).
- [x] Diagnostic documenté ci-dessus.
- [x] Extraire les règles de hiérarchie et la proposition de code dans des fonctions pures testées (`hospital-organization-hierarchy.ts`, `org-unit-code.ts`).
- [x] Extraire la fenêtre de saisie dans un composant dédié (`org-unit-editor.component`) ; page 216 lignes, fenêtre 202.
- [x] Réécrire le gabarit : arbre imbriqué, légende, actions, référentiels repliés, état vide, squelette.
- [x] Traductions FR/EN : clés ajoutées et réécrites, clés mortes supprimées, clés `hospitalOrg.*` regroupées dans le fichier de la fonctionnalité.
- [x] Tests : 27 tests sur le dossier (arbre, actions, erreurs dans la fenêtre, validation, code proposé), suite web 775/775, `ng build` OK.
- [x] Contrôle visuel desktop clair sur données fictives : hiérarchie lisible ; chevrons de légende corrigés (rotation appliquée deux fois).
- [ ] Recette Maestro `.maestro/web/hospital-organization.yaml` : écrite, non exécutée (backend et MailDev arrêtés, mot de passe à fournir par l'utilisateur).
- [ ] Contrôle visuel mobile 375 px et thème sombre (à faire pendant la recette Maestro).
- [x] Mettre à jour spec fonctionnelle (§ 7.1), design technique (§ 12), suivi, changelog.

## Choses retirées
- Carte d'introduction qui répétait le titre, et les surtitres : le titre et le sous-titre portent le sens.
- Grilles de cartes des référentiels : remplacées par une section repliée avec des listes simples (sans les codes techniques).
- Bouton rouge « Désactiver » sur chaque ligne : déplacé dans la fenêtre de modification.

## Non fait
- Pas de piège du focus clavier dans la fenêtre (comme avant) : focus initial et retour au déclencheur ajoutés, Échap ferme.

## Impacts / risques
Angular uniquement : aucun changement d'API, de base ou de règle serveur. Le serveur reste l'autorité (RF-14) : la validation du code et des rattachements côté écran ne fait que prévenir l'utilisateur. La proposition de code ne connaît que les unités chargées : un code déjà pris par une unité inactive non affichée reste refusé par le serveur (message affiché dans la fenêtre). Ajout d'une variante `.ui-button-ghost` au design system (`styles.css`). Version : PATCH candidat, pas de bump ni de release. Estimation : 0,5–1 j frontend ; hors sprint, capacité non engagée.
