# Rapport de vérification — accès et sessions

Date : 2026-10-04. Code de départ : `7a8ec4e6`, branche `agent/hospital-bed-assignment-hardening`. Conclusion : **recette NOT_READY**. P1 de navigation confirmé par exécution, aucune fuite de données démontrée. Le code local n'a pas été comparé au déploiement recette et aucun E2E navigateur n'est revendiqué. Aucun code de production corrigé dans cette revue.

## Résultats prioritaires

| ID | Priorité | Constat | Niveau de preuve |
|---|---|---|---|
| AUTH-01 | P1 | Sans session, une erreur réseau ou refresh 500/502/503 autorise patients/dashboard | Activation avec vrai routeur Angular et vrai service de récupération, HTTP simulé |
| AUTH-02 | P0 existant — gate de recette | Les 12 validations longues/appareil du ticket de résilience sont toujours non cochées | Documentation vérifiée, pas incident runtime P0 nouvellement reproduit |
| AUTH-03 | P2 fonctionnel | Les permissions de la page stocks pharmacie diffèrent de celles de son API | Décisions du vrai guard et contrats serveur/catalogue lus ; symptôme E2E restant |
| AUTH-04 | P2 | Un JWT patient expiré en session locale est accepté par roleGuard | Décision du guard exécutée ; l'intercepteur bloque ensuite les API |

Aucun autre P3 confirmé dans ce périmètre borné ; ceci n'est pas un audit exhaustif de sécurité ou de tous les workflows métier.

## AUTH-01 — fail-open à la récupération sans session

Localisation : `web/src/app/auth/role.guard.ts:95`, branche `catchError`, retour `of(true)` à la ligne 99. Le refus RBAC à la ligne 66 est fermé ; c'est la branche d'échec de récupération de session qui autorise la route.

```ts
catchError((error: HttpErrorResponse) => {
  if (error.status === 401 || error.status === 403) {
    return of(professionalLoginTree());
  }
  return of(true);
})
```

Reproduction : sessionStorage vide, vrai `roleGuard`, vrai `AuthSessionRecoveryService`, RouterTestingHarness avec une page protégée neutre et les métadonnées réelles patients/dashboard. `HttpTestingController` simule la réponse de `/api/auth/refresh`, credentials présents. Après navigation, vérifier URL, activation du composant, session toujours null et aucun appel à `ensureMyAccess`.

| Refresh simulé | `/patients` | `/dashboard` | Session après navigation |
|---|---|---|---|
| erreur réseau, status 0 | page protégée activée | page protégée activée | null |
| 500 | page protégée activée | page protégée activée | null |
| 502 | page protégée activée | page protégée activée | null |
| 503 | page protégée activée | page protégée activée | null |
| 401 | login avec returnUrl | login avec returnUrl | null |
| 403 | login avec returnUrl | login avec returnUrl | null |

Contrôle positif distinct : refresh réussi mais sans PATIENT_READ → `/unauthorized`, RBAC exécuté. Les essais prouvent une activation sans identité, pas l'affichage de dossiers réels : la page du harnais est un composant neutre, et aucune API clinique de recette n'a été appelée.

Impact : ouverture indue de la surface protégée et contournement du contrôle de navigation lors d'une panne auth. Ne pas qualifier cela de fuite de dossiers sans preuve d'une API autorisant les données.

Correction recommandée : refuser toute nouvelle activation sans identité vérifiée, y compris status 0/5xx/409/erreur inattendue ; conserver la distinction panne transitoire et session réellement expirée. Une panne ne doit pas purger la session/brouillons existants ni appeler expireSession par défaut. Retour false ou UrlTree vers un état login/indisponibilité avec reprise et returnUrl ; aucun `true` d'erreur. Ajouter des tests permanents du comportement sécurisé après correction. [OWASP recommande le refus par défaut](https://cheatsheetseries.owasp.org/cheatsheets/Authorization_Cheat_Sheet.html).

## AUTH-02 — gates P0 de session encore ouverts

`docs/ai/tickets/P0-20260726-AUTH-SESSION-CRITICAL-RESILIENCE.md:37` contient 12 cases non cochées : >60 min, deux onglets >30 min, refresh requêtes/onglets simultanés, reload simultané, ancien JWT après rotation, coupure réseau, auth500/503, replay réel, logout sans perte de brouillon, privé/Chrome Android et veille/réveil sur recette. Le ticket d'isolation patient/pro conserve également sa recette réelle ouverte.

La récupération partage les requêtes par shareReplay, coordonne via Web Locks quand disponibles, diffuse le refresh par BroadcastChannel, réessaie les conflits 409 et ne renouvelle pas une identité patient avec le cookie pro. Le keep-alive utilise 120 secondes de marge et ne purge pas sur erreur transitoire. Ces éléments sont lus dans le code et couverts partiellement par les tests existants ; cela ne prouve ni 60 minutes réelles ni le comportement inter-onglets/Android déployé.

Il est nécessaire de conserver ces gates avant une validation de production du parcours clinique. Ne pas confondre préserver une session pendant une panne et autoriser une nouvelle route sans session.

## AUTH-03 — permissions stocks incohérentes

- `web/src/app/app.routes.ts:210` : stocks autorisés avec l'une de `STOCK_READ`, `STOCK_MANAGE`.
- `web/src/app/pharmacy/pharmacy-api.service.ts:19` : appels à `/api/pharmacy/stocks`.
- `backend/src/main/java/com/joprelys/backend/prescription/api/DrugStockController.java:18` : toutes les méthodes exigent `PHARMACY_STOCK_MANAGE`.
- `backend/src/main/java/com/joprelys/backend/auth/rbac/RbacCatalog.java:211` : pharmacien standard possède les trois permissions ; ligne 213, gestionnaire stock possède seulement STOCK_READ/STOCK_MANAGE.

Probe du vrai guard : permission PHARMACY_STOCK_MANAGE seule → unauthorized ; STOCK_READ seule → true. Les rôles standards pharmacien/admin peuvent masquer le décalage car ils cumulent les permissions. Risque : profil personnalisé légitime bloqué à la route, ou gestionnaire stock admis dans un écran dont l'API devrait refuser les lectures. Aucun contournement serveur démontré.

Recommandation : aligner contrat de page/menu/API avec la responsabilité métier attendue. Ne pas élargir silencieusement l'API pour faire passer la navigation ; vérifier avec pharmacien/gestionnaire stock la séparation lecture/écriture. Recette des rôles réels et custom restante.

## AUTH-04 — identité patient expirée acceptée par le guard

`web/src/app/auth/role.guard.ts:34` renvoie true pour une session patient et une route patient sans vérifier isExpired. Le probe enregistre une session expirée depuis 2000, constate `storage.isExpired() === true` puis `roleGuard(patient/dashboard) === true`, sans refresh professionnel. Le loginGuard à `web/src/app/auth/login.guard.ts:13` se base également sur la présence d'une session, pas son expiration.

Limite : `web/src/app/auth/auth-token.interceptor.ts:49` refuse un JWT patient expiré et expire ce contexte avant un appel protégé ; aucune réponse clinique autorisée n'est démontrée. Recommandation : vérifier l'expiration dans le parcours patient/login et rediriger vers le login patient, sans utiliser le cookie professionnel ; confirmer le comportement retour arrière et éventuel affichage de données en mémoire lors de la recette.

## Routes et API des domaines demandés

43 déclarations de pages sensibles : vrai roleGuard et métadonnées permission/rôle contrôlés par probe. Les parcours patient sont réservés PATIENT ; les pages professionnelles utilisent des permissions effectives et ne se contentent pas d'un rôle JWT. Dashboard admet un rôle interne effectif. Cette déclaration correcte ne supprime pas AUTH-01.

| Domaine | Routes représentatives | Contrôles serveur lus et preuves exécutées |
|---|---|---|
| Hospitalisations | patients/:id/hospitalizations, clinic/spatial | HOSPITALIZATION_READ et permissions dédiées admission/soins ; 8 tests annotations contrôleur + 7 spatial, pas E2E hospitalier |
| Laboratoire | clinic/lab-orders, patients/:id/lab-orders | LAB_QUEUE_READ / LAB_ORDER_READ / LAB_ORDER_WRITE ; 9 tests contrôleur, 7 workflow dont gate paiement |
| Urgences | clinic/emergencies | EMERGENCY_READ / WRITE / STABILIZE ; 4 tests contrôleur |
| Facturation | clinic/billing, clinic/cashier, clinic/billing/invoice/:invoiceId | Politique composite page et permissions BILLING/CASH dédiées API ; 2 tests InvoiceController, couverture partielle du domaine |
| Pharmacie | pharmacy/prescriptions, pharmacy/stocks | Prescriptions page PHARMACY_PRESCRIPTION_READ ; stock API PHARMACY_STOCK_MANAGE, AUTH-03 ; 10 tests endpoint public avec PIN/dispensation |
| Portail patient | dashboard/profil/résultats/ordonnances/documents/QR/consentements et autres patient/* | PATIENT côté routes, PATIENT_PORTAL_ACCESS serveur ; 24 tests portail + 4 IDOR + 11 scopes/consentements |

`SecurityConfig.java:42` exige authenticated par défaut et active la sécurité méthode. Tests MockMvc IDOR réussis : API `/api/patient/me` sans bearer → 401 ; patient A et document B → 403 ; propre profil → 200 ; document inconnu → 404. Des comparaisons de propriétaire existent également pour résultats PDF et documents par ID dans PatientPortalController. Il faut encore vérifier tous les points d'entrée, organisations et rôles sur recette.

### Exceptions publiques intentionnelles

Verify (et variantes), forgot-password, public/register sont sans roleGuard. Les deux entrées login utilisent loginGuard ; unauthorized est une page sans dossier clinique. Les probes ne découvrent pas de page métier chargée hors de la liste publique sans guard.

Nuance serveur : `/api/public/**` est permitAll (`SecurityConfig.java:39`). Le principe « toutes les API sensibles exigent un JWT » ne peut donc pas être affirmé globalement :
- document verify/recherche/QR sont anonymes par conception (`DocumentController.java:77`) ; les limites du contenu renvoyé et document révoqué/inconnu restent à recetter ;
- pharmacie verify/dispense/history sont publics mais contrôlés par numéro+PIN et verrouillage d'essais dans PharmacyService ; tests PIN correct/incorrect et dispensation existants exécutés, pas audit exhaustif de brute force ;
- lab-integration upload/FHIR contrôle X-API-KEY dans le contrôleur et refuse clé vide/config vide ; lecture de code seulement pour cette exception, pas test exécuté dans ce lot.

Les contrôles serveur restent indispensables même avec un guard corrigé. [La documentation Angular précise que les guards client ne constituent pas à eux seuls le contrôle d'accès](https://angular.dev/guide/routing/route-guards).

## Vérifications exécutées et reproductibilité

1. Suite Angular existante : `npm test -- --watch=false --exclude=src/.ai-tmp/**` → **115 fichiers, 633 tests verts**. Elle ne couvrait pas le fail-open reproduit ; un vert n'est pas une recette Ready.
2. Probe local ignoré : `npm test -- --watch=false --ts-config=.ai-tmp/tsconfig.audit.json --include=src/.ai-tmp/auth-route-audit.spec.ts` → **1 fichier, 16 tests de caractérisation verts**. 12 cas routeur/HTTP, 1 restauration réussie/RBAC refusé, 2 vérifications routes publiques/protégées, 1 cas groupé stock/expiration patient. Vert signifie que le défaut observé est reproduit, pas corrigé. Les premiers essais ont nécessité une configuration TS locale explicite pour compiler un fichier dans un dossier caché ; aucune configuration du projet versionnée n'a été modifiée.
3. Maven : `mvnw.cmd -Dtest=HospitalizationControllerAuthorizationTest,SpatialControllerAuthorizationTest,LabOrderControllerTest,LabOrderItemWorkflowTest,EmergencyControllerTest,InvoiceControllerTest,PatientPortalControllerTest,PatientIdorSecurityTest,GranularScopesSecurityTest,RefreshTokenSecurityTest,AuthSessionRotationConcurrencyTest,AuthControllerLogoutIsolationTest,PharmacyControllerTest test` → **91 tests, zéro échec/erreur/skip, BUILD SUCCESS**. Base test H2 mémoire, migrations jusqu'à V112 ; pas base PostgreSQL réelle ni suite Maven entière.

La rotation concurrente dispose d'un test exécuté ; cela ne remplace pas deux onglets réels ni un replay depuis un autre appareil. Les tests hospital/spatial vérifient des annotations, ils ne prouvent pas un ensemble complet de refus HTTP par profil. Les tests restantes utilisent les contextes/fixtures du dépôt, pas des comptes de recette.

Artefacts locaux ignorés : `.ai-tmp/auth-route-audit.log`, `.ai-tmp/auth-route-existing-tests.log`, `.ai-tmp/auth-rbac-backend-tests.log`, probe `web/src/.ai-tmp/auth-route-audit.spec.ts` et config `web/.ai-tmp/tsconfig.audit.json`. Pas de test permanent imposant le comportement défaillant. Pas de build production relancé : aucun code/dépendance/configuration de production modifié par cette revue.

## Recette interactive priorisée — ouverte

| Lot | Vérification attendue | État |
|---|---|---|
| Pro/auth | login → dashboard → logout → retour arrière ; expirations et returnUrl | Tests partiels, navigateur non exécuté |
| RBAC | médecin, infirmier, accueil, biologiste, pharmacien, admin et rôles custom ; URL directe et changement de rôle | Métadonnées/guard testés, comptes réels non testés |
| Patient | login OTP, profil, ordonnances, résultats, documents, QR, consentements ; patient A/B | API IDOR ciblées testées, parcours clics non exécuté |
| Clinique | dossier → consultation → hospitalisation → laboratoire | Couverture automatique existante, E2E non exécuté |
| Labo | commande → paiement → examen individuel → résultats/PDF | Gate/workflow tests ciblés, E2E non exécuté |
| Pharmacie | vérifier PIN/ordonnance puis stock avec droits dédiés | Tests API publics, mismatch droits à corriger/recetter |
| Public | inscription, verify, inconnu/invalide/révoqué | Routes lues, parcours complet non exécuté |
| Résilience | JWT, concurrence 2 onglets, 401/403/5xx/réseau/reconnexion, sessions longues | P1 reproduit, gate P0 ouverte |
| Mobile | 360/390px, clavier, formulaires/tableaux/menus, veille/réveil | Non exécuté |
| Sécurité serveur | sans JWT, expiré/révoqué, autre patient/org, rôle modifié, exceptions publiques | Sous-ensemble H2/MockMvc validé, matrice complète restante |

Reviewer : Tech Lead + QA sécurité + praticien. Audit borné hors sprint ; aucune capacité/date de recette engagée. Corriger AUTH-01 en priorité, aligner AUTH-03, renforcer AUTH-04 et fournir les preuves P0 avant statut Ready. Aucun bump/version/commit/push/release dans ce diagnostic.
