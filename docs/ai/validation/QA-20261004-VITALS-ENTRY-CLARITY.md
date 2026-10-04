# Diagnostic et review — saisie des constantes

## Constat sourcé
Capture fournie le 2026-10-04 : patient répété dans l'assistant, deux libellés vocaux sans distinction du choix/démarrage, placeholder comprimé entre Dictée et Analyser, action d'enregistrement sous le défilement. Code : `enableRealtime()` appelé directement par le choix Temps réel ; grille `sm:grid-cols-[auto_1fr_auto]` indexée sur la fenêtre et non la largeur de la modale ; panneau entier en `overflow-y-auto` ; intitulé nominal du bouton final.

## Corrections
Radios Manuel/Dictée/Écoute continue et descriptions dédiées ; sélection sans microphone, CTA séparé. Saisie textuelle optionnelle en textarea pleine largeur. Champs visibles indépendamment des modes, propositions reportées explicitement et sauvegarde finale distincte. En-tête patient et footer hors du corps défilant ; labels reliés, état aria-invalid, navigation Tab/Échap et restauration de focus. Blocage de sauvegarde pendant activité de l'assistant, double permission microphone empêchée et libération des tracks accordés après réduction/destruction. Contrats et règles cliniques inchangés.

## Vérifications techniques
- Tests ciblés initiaux : 27 réussis (deux fichiers).
- Suite Angular complète après ajout de deux cas complémentaires : 115 fichiers / 633 tests réussis, dont 14 nouveaux tests pour ce lot.
- Contrôle FR/EN : 26 nouvelles clés et 2 libellés de report clarifiés ; 24 tokens visuels existants contrôlés ; shell i18n 47 clés réussi.
- Build de production final et `git diff --check` réussis après ajustement des cibles tactiles (44px minimum pour les nouvelles actions).
- Tests DOM et microphone simulé : ne constituent pas une preuve de rendu ni une recette clinique.

## Sécurité et régression
Pas d'API, permission, migration, dépendance, stockage, configuration, sanitizer ou HTML injecté ajouté. Les analyses ne déclenchent pas saveVitals ; le report ne sauvegarde pas les mesures. Les validations et valeurs précédentes restent couvertes. `.gitignore` conserve proxy et gouvernance ; logs/artifacts restent ignorés. Alertes de taille : assistant 339 lignes (>300, sous limite 500), modale 223 lignes, nouveau choix 65 lignes. Un éventuel découpage audio ultérieur est hors de cette correction bornée.

## Gate visuel
Statut : BLOQUÉ — accès navigateur précédemment refusé dans la session. Aucun accès alternatif ni aucune capture du nouveau rendu. Recette desktop/mobile, light/dark, FR/EN, zoom/clavier et microphone réel à effectuer. La capture fournie a servi au diagnostic ; aucune comparaison source/rendu ni validation visuelle n'est annoncée.
