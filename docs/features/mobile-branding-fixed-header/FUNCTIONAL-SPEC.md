# FUNCTIONAL SPEC — Branding adaptatif et headers mobiles fixes

## Problème utilisateur

Sur Android sombre, la connexion affiche le logo dans une grande carte blanche,
le pictogramme de l’accueil professionnel manque de contraste et la barre
d’application disparaît pendant le défilement. Dans la consultation SOAP, le
motif occupe inutilement la zone fixe et réduit l’espace de saisie.

## Utilisateurs concernés

- professionnels se connectant à Joprelys Connect mobile ;
- professionnels consultant la file patients ;
- médecins ouvrant une note SOAP depuis le dashboard.

## Parcours attendu

### Connexion

1. Les préférences thème/langue restent accessibles avant authentification.
2. Le logo transparent et `Connect` apparaissent directement sur le fond.
3. Le rendu choisit automatiquement une apparence lisible en light/dark.
4. Le formulaire et son comportement restent inchangés.

### Dashboard professionnel

1. Une app bar compacte présente la marque, la langue, le thème et le profil.
2. Cette app bar reste visible quand la file patients défile.
3. Le geste de pull-to-refresh reste disponible dans le contenu.
4. Le pictogramme de marque reste identifiable dans les deux thèmes.

### Consultation SOAP

1. Le drag handle, le titre, le patient et les actions restent fixes.
2. Le motif de consultation se trouve au début du contenu défilant.
3. L’enregistrement, les erreurs et l’assistant vocal gardent leurs contrats.

## Périmètre inclus

- rendu light/dark des logos Flutter ;
- app bar professionnelle fixe ;
- densité des contrôles de toolbar ;
- réduction du header fixe SOAP ;
- tests widget et goldens.

## Hors périmètre

- correction de l’erreur réseau SOAP visible sur la capture ;
- modification de la file patients, du payload SOAP ou du backend ;
- changement de permissions, session, biométrie ou routes ;
- refonte complète des cartes patient.

## Accessibilité

- cibles tactiles d’au moins 44 px ;
- nom accessible `Joprelys Connect` pour le logo ;
- tooltips conservés pour thème, langue et profil ;
- contraste vérifié structurellement en light/dark ;
- aucune information portée uniquement par la couleur.

## Internationalisation

Aucun nouveau texte utilisateur n’est requis. Les libellés existants restent
issus des catalogues ARB français et anglais.

## Critères d’acceptation

- absence de rectangle blanc derrière le wordmark ;
- logo et pictogramme lisibles en light/dark ;
- toolbar fixe sur un viewport 360–393 px ;
- contrôles accessibles sans collision ;
- header SOAP compact et motif défilant ;
- aucune régression fonctionnelle auth/dashboard/SOAP.

## Cas limites

- thème `system` résolu en clair ou sombre ;
- nom professionnel long ;
- locale anglaise ;
- texte système agrandi ;
- clavier ouvert sur le login ou la note SOAP ;
- file vide, en chargement ou en erreur.
