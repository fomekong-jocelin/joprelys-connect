# Checklist engineering / QA

- [x] Analyse de l'existant et documentation initiale avant développement.
- [x] Décision prescription obligatoire explicitement acceptée par l'utilisateur.
- [x] Contrôleurs readonly avec contrats applicatifs dédiés, aucun repository direct.
- [x] Permissions métier et scope tenant explicites ; configuration et mutation protégées.
- [x] Validation prescription/patient/tenant/statut/expiration/nom côté serveur.
- [x] Verrou partagé pour transitions de visite et contrôle de révision consultation.
- [x] Pas de calcul financier réussi après panne d'une source clinique.
- [x] Erreur, chargement et absence de données distincts ; reprise et garde double soumission.
- [x] Extraction consent/CRO, composants applicatifs modifiés inférieurs à 500 lignes.
- [x] Libellés du périmètre dans les catalogues FR/EN, tokens centraux et thèmes existants ; arrondis sobres.
- [x] OWASP : authentification/autorisations conservées, projections sans coordonnées privées, erreurs médicamenteuses non révélatrices ; aucun nouveau secret ni bypass.
- [x] 12-Factor : état critique transactionnel en base, aucune URL backend codée en dur ; proxy Angular, YAML et Maven conservés.
- [x] .gitignore contrôlé : caches, logs, builds, secrets ignorés, gouvernance et proxy conservés.
- [x] Rupture de contrat documentée ; pas de bump ou release initié.
- [x] Validation finale automatisée : voir TEST-PLAN.md et rapport QA pour résultats exacts.
- [ ] Review humaine Tech Lead / clinique / QA.
- [ ] Recette navigateur multi-profils, FR/EN et light/dark (accès refusé dans la session).
- [ ] Validation du comportement des verrous sur PostgreSQL réel avant livraison.
