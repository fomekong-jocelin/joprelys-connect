# P0-20260726 — Isolation des sessions patient et professionnel

Issue : #169

## Décision d'architecture

Joprelys supporte explicitement deux identités simultanées dans deux onglets d'un même navigateur :

- une session professionnelle persistante : access JWT court + refresh cookie HttpOnly ;
- une session patient : JWT obtenu par OTP, non renouvelé avec le cookie professionnel.

Les deux contextes partagent l'origine web mais ne partagent jamais leur cycle de renouvellement ni leur contexte RBAC.

## Invariants de sécurité

- [x] Une connexion patient ne supprime pas le refresh cookie professionnel.
- [x] Un logout patient ne supprime pas le refresh cookie professionnel.
- [x] Un JWT patient expiré n'est jamais renouvelé via `/api/auth/refresh`.
- [x] Un 401 patient ne déclenche jamais un refresh professionnel.
- [x] Le keep-alive professionnel ignore toute session PATIENT.
- [x] Le BroadcastChannel professionnel ne remplace jamais l'identité d'un onglet patient.
- [x] Un onglet patient neuf ne restaure pas automatiquement l'identité professionnelle à partir du cookie partagé.
- [x] Une route patient sans session renvoie vers le contexte de login patient, sans refresh professionnel préalable.
- [x] Une session professionnelle ne donne jamais accès à une route patient.
- [x] Une session patient ne donne jamais accès à une route professionnelle.
- [x] Le cache RBAC et les états mémoire liés à l'identité sont purgés lors d'un changement d'identité dans l'onglet.
- [x] Un changement d'identité dans un onglet ne vide plus le `localStorage` partagé avec les autres onglets.
- [x] Le refresh cookie reste HttpOnly et son cycle de vie reste exclusivement backend.
- [x] Le logout patient révoque son JTI sans traiter le numéro DPU comme un `users.id`.

## Modèle cible

### Onglet professionnel

`JWT professionnel -> API -> 401/expiration proche -> refresh cookie professionnel -> nouveau JWT professionnel`

Coordination multi-onglet professionnel : Web Lock + BroadcastChannel professionnel.

### Onglet patient

`OTP patient -> JWT PATIENT -> API patient -> expiration/401 -> retour login patient`

Aucun appel à `/api/auth/refresh` n'est autorisé depuis ce contexte.

## Recette technique couverte par tests

- [x] token professionnel sain : aucun refresh prématuré ;
- [x] token professionnel expirant : refresh coordonné ;
- [x] token patient expirant : aucun refresh professionnel ;
- [x] 401 patient : aucun refresh professionnel et retour login patient ;
- [x] session patient valide au bootstrap : aucune restauration professionnelle ;
- [x] session patient expirée au bootstrap : purge locale de l'onglet, aucun refresh professionnel ;
- [x] route patient sans session : contexte login patient ;
- [x] route patient avec session professionnelle : refus de réutilisation du contexte professionnel ;
- [x] login patient : aucun effacement du cookie professionnel ;
- [x] logout patient : révocation du JTI patient, cookie professionnel préservé ;
- [x] logout professionnel : cookie professionnel expiré normalement ;
- [x] changement d'identité dans le même onglet : caches RBAC/mémoire purgés ;
- [x] préférences `localStorage` partagées préservées entre onglets.

## Recette navigateur à exécuter en recette

- [ ] Chrome desktop : médecin onglet A + patient onglet B pendant plus de 30 minutes ;
- [ ] Chrome desktop : refresh médecin pendant activité patient dans B ;
- [ ] Chrome desktop : logout patient B sans interruption du médecin A ;
- [ ] Chrome Android : mêmes scénarios multi-onglet ;
- [ ] coupure réseau puis retour pendant coexistence des deux identités.

La CI GitHub Actions peut être indisponible tant que le budget Actions du dépôt est bloqué. Ce document distingue donc les tests automatisés ajoutés au code de la recette navigateur d'intégration à effectuer sur l'environnement de recette.

## Definition of Done

- [x] Code frontend et backend séparant explicitement les deux contextes.
- [x] Tests unitaires de non-régression ajoutés.
- [x] Aucun `Clear-Site-Data` utilisé pour synchroniser les identités.
- [x] Aucun login/logout patient ne détruit le refresh professionnel.
- [x] Aucun refresh professionnel ne peut transformer silencieusement une session patient.
- [ ] Recette navigateur réelle validée après déploiement.
