# Plan de tests — Historique et bordereau de clôture

## Backend automatisé

- historique personnel limité au caissier connecté ;
- session clôturée retournée avec totaux de rapprochement ;
- numéro `CLS-` déterministe ;
- PDF `application/pdf` avec signature `%PDF-` ;
- refus d’un autre caissier du même établissement ;
- refus d’un rôle clinique ;
- non-régression Maven, H2 et PostgreSQL 16.

## Frontend automatisé

- chargement et rendu de l’historique ;
- rafraîchissement automatique après clôture ;
- mise en évidence et ouverture du détail de la dernière session ;
- chargement différé des mouvements ;
- état d’erreur accessible avec retry ;
- compilation Angular de production.

## QA visuelle

- 360 px, 768 px et 1440 px ;
- thèmes light/dark ;
- 24 px entre la file d’encaissement et les cartes de session ;
- boutons non compressés et cibles tactiles >= 44 px ;
- tableau des mouvements scrollable sur mobile ;
- navigation clavier et focus visibles ;
- téléchargement et ouverture du bordereau PDF.