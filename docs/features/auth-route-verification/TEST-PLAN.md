# Plan de vérification

1. Reproduction routeur/HTTP sans session : patients et dashboard × réseau/401/403/500/502/503. Consigner observed true/redirect et activation réelle.
2. Routes réelles : guard et permission/role metadata des six modules, exceptions publiques ; récupération réussie soumise au RBAC, identité patient/pro distincte, refus RBAC.
3. Tests existants Angular auth/routes ; backend ciblé RBAC/IDOR/refresh/pharmacie/labo/urgence/facturation selon test isolé. Noter échecs, pas de test ignoré pour déclarer le résultat vert.
4. Rapport : scopes de preuve et gates P0 (session >60 min, 2 onglets >30 min, concurrence, coupure réseau, auth5xx, privé, Android, veille/réveil).

Les probes de caractérisation restent sous .ai-tmp ignoré, pour ne pas figer un défaut connu dans la suite de régression. Aucune donnée patient réelle, aucun secret de test ni capture de recette ajouté au dépôt.

## Résultats du 2026-10-04
633 tests Angular existants / 115 fichiers réussis ; 16 probes locaux / 1 fichier réussis, reproduisant le défaut ; 91 tests Maven ciblés réussis sans erreur/skip, profil H2 mémoire. Rapport détaillé et commandes dans `docs/ai/validation/QA-20261004-AUTH-RBAC-SESSION-REPORT.md`. 43 routes sensibles avec metadata présentes ; navigation sans session fail-open confirmée sur réseau/500/502/503. E2E navigateur, PostgreSQL et sessions longues non exécutés ; gates P0 restent ouvertes. Aucun code de production modifié, build production non requis.
