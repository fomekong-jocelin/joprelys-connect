# Tests
- API readonly : médecin/responsable/infirmier selon admission/transfert/lecture ; 401 anonyme, 403 profil non autorisé ; tenant étranger et mutation configuration refusés.
- Concurrence : deux transactions lancées simultanément, un seul succès take-charge sans takeover ; save concurrent avec même timestamp : un succès et un conflit ; libération/reprise cohérentes.
- Médicaments : lien nominal, inexistant, autre patient, autre organisation, médicament divergent, prescription draft/annulée/expirée ; null refusé (choix explicite prescription obligatoire).
- Finance : panne de chaque repository source propagée, listes vides normales, quantité libre non numérique ; nominal hospitalisation/CRO/implants.
- Angular : chargement échoué distinct de vide, retry, double admission avant réponse, disponibilités rafraîchies après 409, readonly API URLs, extraction panneaux, FR/EN, tokens et build.
- Suites Maven ciblées puis vérification complète selon impact ; suite Angular/i18n/build. Recette réelle reste bloquée par permission navigateur.

## Preuves du lot corrigé — 2026-10-04

| Vérification | Résultat | Limite |
|---|---|---|
| Suite Angular complète | 113 fichiers, 604/604 tests verts, sortie 0 | Vitest/DOM simulé ; pas d'E2E navigateur |
| Build Angular production | Réussi, sortie 0 | Compilation ; pas de recette visuelle |
| i18n shell | 47 clés FR/EN présentes | Contrôle shell seulement |
| i18n hospitalier complémentaire | 150 clés littérales utilisées par les 5 composants modifiés présentes en FR et EN | Contrôle statique, pas revue linguistique/visuelle |
| Maven ciblé | 86/86 tests, 11 classes, zéro échec/erreur/skip, BUILD SUCCESS, sortie 0 | H2 isolé, pas suite backend complète ni PostgreSQL réel |
| Diff et standards | git diff --check réussi ; composants modifiés : 466/73/140/119/316 lignes | PatientHospitalization et continuation urgence restent au-dessus du seuil d'alerte 300, sous le maximum 500 |

Commande Maven de validation :
```powershell
.\mvnw.cmd -o '-Dmaven.repo.local=C:/Users/Jocelin FOMEKONG/.m2/repository' '-Dtest=VisitControllerTest,VisitCareFlowControllerTest,HospitalizationAdmissionServiceTest,HospitalizationControllerTest,HospitalizationWorkflowControllerTest,HospitalizationControllerAuthorizationTest,ActiveBedAssignmentServiceTest,BedAssignmentRepositoryTest,RbacCatalogHospitalizationClinicalPermissionTest,ConsultationControllerTest,InvoicePrecalculationServiceTest' test
```
Commandes Angular : `npm.cmd test -- --watch=false`, `npm.cmd run build`, `npm.cmd run i18n:check` depuis `web`.

Les tests VisitCareFlowControllerTest.simultaneousTakeChargeAllowsExactlyOnePractitioner et ConsultationControllerTest.simultaneousConsultationSavesRejectTheSecondStaleRevision utilisent des requêtes parallèles avec synchronisation et attendent un succès et un 409. HospitalizationWorkflowControllerTest couvre droits métier/configuration, tenant étranger et prescription absente/inconnue/autre patient/brouillon/expirée/annulée/nom divergent. InvoicePrecalculationServiceTest couvre la panne de chacune des quatre sources. Les nouveaux tests UI couvrent la reprise après erreur, le double POST, les réponses lits obsolètes et la conservation des champs consent/CRO avec confirmation unique.

Les artefacts logs sont dans .ai-tmp (ignoré par Git) : fix-hospital-backend-final.log, fix-hospital-ui-complete.log, fix-hospital-build-complete.log, fix-hospital-i18n-final.log. Les rapports JUnit sont dans backend/target/surefire-reports (ignoré). Aucune donnée de la base applicative locale n'a été utilisée ou modifiée pour ces tests.

Recette réelle encore ouverte : navigation/session locale multi-profils, light/dark, FR/EN, contrôle des verrous PostgreSQL, review clinique et déploiement coordonné des clients. Les contrôles automatisés ne permettent pas de déclarer tout le parcours hospitalier normal.
