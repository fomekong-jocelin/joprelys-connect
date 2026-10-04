# Vérification des accès et de la continuité de session

Vérifier routes sensibles hospitalisation, laboratoire, urgences, facturation, pharmacie et portail patient ; vérifier parcours publics verify/forgot-password/public-register. Un utilisateur sans identité vérifiée ne doit pas obtenir une nouvelle autorisation à cause d'une panne auth. Une panne transitoire ne doit pas être présentée comme expiration confirmée et ne doit pas effacer les brouillons cliniques.

Diagnostic : reproduire Angular sans session sur patients/dashboard, refresh 503 puis comportement du guard/routeur. Vérifier également 401/403, réseau et 500/502, session restaurée et permissions, frontières patient/pro. Mettre en évidence les preuves automatisées et l'absence de preuve d'E2E, sessions longues, navigateur privé/Android/veille et données d'un autre patient sur l'environnement déployé.

Acceptation : rapport factuel et priorisé avec recommandations. Ce lot ne modifie pas le guard ni les règles d'accès de production.
