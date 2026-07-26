# Démarrage conditionnel du référentiel médicament — Spécification fonctionnelle

## Problème

L'activation du référentiel RxNorm ne doit jamais empêcher l'API Joprelys de démarrer.

## Règles

1. Si `JOPRELYS_MEDICATION_REFERENCE_ENABLED=true` et
   `JOPRELYS_RXNORM_ENABLED=true`, le port RxNorm est instancié avec sa configuration.
2. Si une des propriétés est désactivée, le port RxNorm n'est pas instancié.
3. Aucun appel RxNav n'est réalisé pendant le bootstrap.
4. Une indisponibilité RxNav pendant une consultation reste gérée par le comportement
   fail-open documenté du référentiel, sans affecter le démarrage.

## Critères d'acceptation

- contexte Spring vert avec RxNorm actif ;
- contexte Spring vert avec RxNorm inactif ;
- aucun redémarrage en boucle ;
- aucune modification du comportement clinique du garde ordonnance.
