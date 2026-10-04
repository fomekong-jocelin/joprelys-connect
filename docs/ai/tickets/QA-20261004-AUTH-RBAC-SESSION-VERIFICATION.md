# QA-20261004-AUTH-RBAC-SESSION-VERIFICATION

Mode : Diagnostic / QA Review. Statut : DIAGNOSTIC_VERIFIED — P1 confirmé, recette NOT_READY. Demande : vérifier le fail-open du roleGuard, les routes hospitalisations/labo/urgences/facturation/pharmacie/portail patient et les preuves de session.

## Actions et acceptation
- [x] Lire le guard, récupération/intercepteur, routes et tickets P0 session/isolation.
- [x] Reproduire navigation sans session + refresh 503 avec routeur Angular et HTTP simulé ; étendre réseau/500/502 et contrôles 401/403.
- [x] Vérifier les métadonnées des routes, droits effectifs, contextes patient/professionnel et surfaces API publiques/protégées.
- [x] Exécuter les tests existants pertinents côté Angular et Maven selon configuration de test isolée.
- [x] Produire rapport P0/P1/P2/P3 avec références de lignes, preuves, limites et recommandations ; mettre à jour suivi/checklist.
- [ ] Recette interactive multi-profils et sessions longues sur recette ; aucun navigateur/capture de recette prétendu sans preuve.

Acceptation du diagnostic : distinguer défaut reproduit et fuite de données non démontrée, lecture et tests exécutés, local et déployé, API protégées et exceptions publiques intentionnelles. Aucun correctif de production ni commit/push/release demandé dans cette vérification.

## Périmètre et impacts
Audit borné du code local et tests automatisés, pas recette exhaustive des dix parcours. Front TypeScript/Angular 22 et backend Java/Spring, aucune règle métier/API/DB/configuration modifiée. OWASP : refus par défaut, contrôle serveur et isolement patient. 12-Factor : paramètres/secrets inchangés. Documentation fonctionnelle/technique initiale dans docs/features/auth-route-verification ; rapport dans docs/ai/validation. Profil senior full-stack/QA sécurité, 0.5–1j indicatif pour diagnostic, hors sprint sans capacité engagée. Reviewer Tech Lead + QA + praticien. SemVer : aucun bump pour audit ; un correctif de guard serait PATCH candidat. Les recettes longues sont des gates existants, non fermés par cette revue.

## Preuves et suite
Rapport : `docs/ai/validation/QA-20261004-AUTH-RBAC-SESSION-REPORT.md`. 43 routes sensibles déclarées derrière roleGuard, mais activation sans session reproduite pour patients/dashboard sur 0/500/502/503 ; 401/403 redirigent. Deux P2 relevés : décalage permissions stock route/API et expiration patient non vérifiée au guard. Aucun accès à des dossiers réels démontré.

633 tests Angular existants, 16 probes de caractérisation ignorés et 91 tests Maven ciblés verts (H2 test, zéro skip). Un vert de caractérisation reproduit le défaut, il ne signifie pas sécurité validée. Premiers essais du probe corrigés par configuration TS locale ignorée ; pas d'erreur de production masquée. Diff/docs contrôlés, artefacts locaux vérifiés ignorés ; aucun build production requis car aucune modification de production.

Restent correctifs AUTH-01/03/04, review et recette P0 longue/multi-profils/mobile ; diagnostic terminé sans déclarer Ready l'environnement. Aucun commit/push/release demandé.
