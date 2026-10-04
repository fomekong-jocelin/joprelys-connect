# P0-20260726-AUTH-SESSION-CRITICAL-RESILIENCE

## Sévérité
P0 — continuité de session clinique.

## Symptôme
Déconnexions répétées avec message « session expirée » alors que l'utilisateur travaille normalement, notamment pendant des parcours longs comme une consultation vocale.

## Causes racines confirmées
1. JWT production de 15 minutes alors que le keep-alive utilisait une marge de 15 minutes : un token neuf était donc immédiatement éligible au refresh puis retesté chaque minute.
2. Chaque refresh créait une nouvelle session persistante et révoquait immédiatement la précédente ; les access tokens liés à la session précédente devenaient donc invalides avant leur propre expiration.
3. Le refresh token HttpOnly est partagé par les onglets du même origin tandis que l'access token est en sessionStorage par onglet. Deux onglets pouvaient donc faire tourner le même refresh token en concurrence.
4. La seconde utilisation d'un refresh token venant juste d'être tourné était interprétée comme une attaque de replay et révoquait toute la famille de sessions, même lorsque la cause était une course normale du navigateur.
5. La restauration de session au bootstrap Angular contournait le mécanisme de coordination et pouvait elle-même déclencher cette course.
6. Une panne réseau/5xx pendant restoreSession pouvait faire échouer l'initialisation Angular.
7. Le backend envoyait Clear-Site-Data sur la suppression du refresh cookie, pouvant purger largement le cache/cookies/storage du site.
8. Le frontend tentait également de supprimer tous les cookies accessibles via document.cookie alors que le refresh cookie est HttpOnly et ne peut pas être géré correctement par JavaScript.

## Correctifs
- keep-alive : refresh uniquement dans les 2 dernières minutes du JWT ;
- déduplication intra-onglet des refresh ;
- Web Locks pour sérialiser les refresh entre onglets quand disponible ;
- BroadcastChannel pour propager le nouvel access token aux autres onglets ;
- réutilisation d'un access token déjà renouvelé lorsqu'une requête ancienne reçoit un 401 ;
- endpoint refresh appelé avec credentials explicites ;
- HTTP 409 AUTH_REFRESH_CONCURRENT pour une course bénigne récente du même contexte client ;
- une course bénigne ne révoque plus la famille de sessions ;
- le replay réel hors fenêtre/contexte reste bloquant et révoque la famille ;
- un access token encore non expiré reste valide à travers une rotation normale tant que la chaîne de session aboutit à une session active ;
- logout/revocation explicite et replay réel continuent d'invalider toute la chaîne ;
- suppression de Clear-Site-Data ;
- suppression du nettoyage global document.cookie côté Angular ;
- restoreSession passe par le refresh coordonné ;
- panne réseau/409/5xx au bootstrap ne provoque plus de page blanche et ne purge pas la session locale ;
- seuls les refus authentiques 401/403 entraînent l'expiration locale de session.

## Recette critique
- [ ] session active > 60 min sans déconnexion ;
- [ ] deux onglets ouverts pendant > 30 min ;
- [ ] refresh simultané de deux requêtes ;
- [ ] refresh simultané de deux onglets ;
- [ ] reload simultané de deux onglets ;
- [ ] accès API avec ancien JWT pendant qu'un autre onglet vient de refresh ;
- [ ] perte réseau avant/pendant refresh puis retour ;
- [ ] backend auth en 500/503 au chargement puis retour ;
- [ ] vrai refresh token rejoué depuis un autre contexte : famille révoquée ;
- [ ] logout : token/cookie d'auth supprimés sans suppression des brouillons locaux ;
- [ ] Chrome desktop, Chrome Android et navigation privée ;
- [ ] recette.joprelys.com après veille/réveil du mobile.

## Critère de sortie
Une rotation normale, une course multi-onglet, une panne réseau transitoire ou un 5xx ne doivent jamais être présentés comme « session expirée ». La déconnexion automatique n'est autorisée que lorsque le serveur confirme réellement que la session n'est plus authentifiable (401/403 après tentative de récupération), ou lors d'un logout/révocation explicite.

## Vérification complémentaire — 2026-10-04
Revue `QA-20261004-AUTH-RBAC-SESSION-VERIFICATION` : tests existants Angular (633) et Maven ciblés (91) verts, rotation concurrente incluse. Les 12 validations de recette ci-dessus restent non démontrées et non cochées. Un P1 distinct est reproduit : roleGuard autorise une nouvelle route patients/dashboard sans session lorsque le refresh échoue en réseau/500/502/503. Préserver une session lors d'une panne ne doit pas autoriser une identité absente. Détails et limites : `docs/ai/validation/QA-20261004-AUTH-RBAC-SESSION-REPORT.md`. Aucun changement de production dans cette vérification ; recette NOT_READY.
