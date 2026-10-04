# QA-20261004 — Revue du parcours hospitalier local

## Cadre

- Mode : QA Review + Diagnostic ; aucun développement fonctionnel demandé.
- Statut : PARTIAL — revue code et tests terminés ; recette interactive bloquée par permission navigateur.
- Demande : vérifier le code, le parcours hospitalier et tester `http://localhost:4201/dashboard`.
- Rattachement : EPIC-0027, DIAG-20260809-PRACTITIONER-HOSPITAL-PATH-BED-ASSIGNMENT et TICKET-20261003-PATIENT-JOURNEY-AUDIT.
- Profil : senior Angular/Spring + QA ; reviewer : Tech Lead et référent clinique.
- Estimation de la revue bornée : 0,5 jour ; hors nouvel engagement de sprint.

## Critères d'acceptation

1. Reconstituer accueil/visite/constantes/consultation/admission/séjour/sortie depuis le code.
2. Exécuter les vérifications disponibles sans modifier les données du serveur local.
3. Rapporter les anomalies avec priorité, preuve, conséquence et action proposée.
4. Distinguer tests automatisés, observations navigateur et limites ; ne pas déclarer le parcours validé sans recette.

## Actions

- [x] Lire la gouvernance, les standards et les diagnostics existants.
- [x] Vérifier proxy Angular, dépendances et `.gitignore`.
- [x] Tenter l'ouverture de l'URL demandée dans Chrome.
- [x] Revoir les transitions métier et permissions côté Angular/Spring.
- [x] Exécuter tests Angular, i18n, build et tests Maven pertinents.
- [x] Rédiger le rapport QA et mettre à jour PROJECT-TRACKING.
- [ ] Recette interactive du dashboard et du parcours hospitalier.

## Blocage navigateur

Le 2026-10-04, `cua.createBrowserTab` sur l'URL demandée a été rejeté par la politique navigateur : permission refusée par l'utilisateur selon le contrôle d'accès. Aucun contournement (autre surface, HTTP direct ou automatisation parallèle) n'est effectué. La revue du dépôt et les tests isolés restent possibles.

## Impacts

- Code/API/DB/configuration : revue seulement ; aucune modification prévue.
- Sécurité : vérification AuthN/AuthZ, isolation établissement, intégrité lit/séjour, traçabilité.
- Régression : comparer code et tests actuels, sans supposer les anciennes preuves toujours valides.
- Planning : ne ferme aucun gap métier ni gate E2E existant.
- SemVer : aucun bump applicatif pour cette revue documentaire.

## Preuve et reste à faire

Rapport : `docs/qa/QA-20261004-HOSPITAL-PATH-LOCAL.md`.

- Angular : 112 fichiers, 593/593 tests verts ; build production et contrôle de 47 clés i18n shell réussis.
- Maven : 65/65 tests ciblés verts (8 classes), H2, zéro skip ; relance offline avec le cache Maven utilisateur existant après échec réseau/cache workspace incomplet.
- Constats : HOS-QA-01 à 04 (P1 : lectures d'admission/RBAC, concurrence clinique, lien prescription/administration, erreurs financières silencieuses), HOS-QA-05 à 07 (états erreur, double soumission, i18n/taille/tokens).
- Aucune modification de code applicatif ; changelog non modifié car aucun comportement/API/DB/UI/configuration/exploitation changé.
- Aucun nouveau planning de correction engagé ; les constats restent rattachés à EPIC-0027 et au parcours patient existant.

Recette Chrome à reprendre quand l'accès navigateur à localhost:4201 est autorisé.

## Suivi des corrections
Les constats HOS-QA-01 à 07 sont traités dans FIX-20261004-HOSPITAL-PATH. Les chiffres et descriptions ci-dessus sont les preuves historiques de la revue avant correction ; les nouvelles preuves figurent dans le rapport QA et TEST-PLAN du lot. La recette navigateur demeure ouverte.
