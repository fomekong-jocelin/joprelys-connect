# Button label layout — Functional specification

## Problème métier

Une action doit rester immédiatement compréhensible. Lorsqu'un libellé de bouton se coupe sur deux lignes, la barre d'actions perd sa hiérarchie visuelle, les boutons n'ont plus la même hauteur et l'utilisateur peut confondre plusieurs actions adjacentes.

## Utilisateurs concernés

- Personnel clinique et administratif ;
- patients utilisant le portail web ;
- administrateurs de plateforme et de clinique.

## Objectif

Garantir une présentation stable et lisible de toutes les actions standard du web Angular, quel que soit le thème, la langue et la largeur de l'écran.

## Règles fonctionnelles

1. Un libellé de bouton standard reste sur une seule ligne.
2. Une icône et son libellé restent sur la même ligne.
3. Quand plusieurs actions ne tiennent plus horizontalement, les boutons reviennent à la ligne comme des unités complètes ou s'empilent selon le layout de la page.
4. Une action ne doit pas être tronquée silencieusement pour contourner un problème de mise en page.
5. Les actions critiques conservent leur variante visuelle et leur état disabled.
6. Les textes français et anglais suivent la même règle.
7. Le correctif est transversal : aucune page ne doit maintenir sa propre règle concurrente.

## Périmètre inclus

- Boutons utilisant la classe design system `.ui-button` ;
- boutons primaires, secondaires et danger ;
- boutons texte seul, icône + texte et icône seule ;
- en-têtes de page, modales, cartes, formulaires et groupes d'actions.

## Périmètre exclu

- Liens textuels `.ui-link` ;
- badges et tags ;
- changement des libellés métier ;
- refonte graphique des écrans.

## Parcours attendu

Sur desktop, les actions restent alignées horizontalement tant que l'espace le permet. Sur tablette et mobile, le conteneur peut faire revenir les boutons à la ligne ou les empiler, mais le texte interne de chaque bouton reste intact.

## Critères d'acceptation

- Aucun libellé `.ui-button` ne se répartit sur deux lignes aux largeurs 320, 375, 768 et 1440 px.
- Les icônes restent centrées avec le texte.
- Aucun changement de couleur, rayon, focus ou état disabled.
- Aucun défilement horizontal de page provoqué par le correctif.
- Thèmes light et dark validés.
- Français et anglais validés.

## Cas limites

Un libellé plus large que la largeur physique de l'écran révèle un problème de wording ou de composition de page. Il doit être corrigé à la source ; la règle globale de non-coupure ne doit pas être retirée.
