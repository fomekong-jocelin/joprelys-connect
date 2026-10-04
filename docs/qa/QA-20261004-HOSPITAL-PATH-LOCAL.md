# QA-20261004 — Parcours hospitalier local

## Verdict et limites

**Le parcours ne peut pas être déclaré entièrement normal ni validé.** Les tests Angular et le build passent, mais la revue identifie des défauts d'autorisation des lectures d'admission, de concurrence clinique, d'intégrité des administrations médicamenteuses et de gestion des erreurs financières. Aucun de ces constats n'est présenté comme une observation visuelle de l'application.

- Demande : code + parcours + recette sur `http://localhost:4201/dashboard`.
- Révision analysée : `5a63023a`, dépôt initialement propre.
- Mode : QA Review / Diagnostic, rattaché au ticket `QA-20261004-HOSPITAL-PATH-LOCAL`.
- Sources : code et tests du dépôt, spécifications hospitalières, DIAG-20260809 et TICKET-20261003-PATIENT-JOURNEY-AUDIT.
- Deux ouvertures Chrome ont été refusées par le contrôle d'accès navigateur, la seconde pour une préférence utilisateur enregistrée. Pas de connexion, capture ni recette interactive réalisée. Aucun contournement effectué.
- Tests backend isolés : profil `test`, H2 en mémoire ; ils ne certifient pas PostgreSQL ni l'état du serveur local.

## Parcours reconstitué dans le code

1. Pré-enregistrement ou recherche/création du patient, puis ouverture d'une visite.
2. File active : constantes, historique de mesures, alertes backend et prise en charge du praticien.
3. Consultation, prescriptions et demandes d'examens ; possibilité de remise en file ou reprise explicite.
4. Hospitalisation normale : sélection visite, unité/service, médecin affecté, espace et lit ; urgence : continuité depuis `emergencyId` avec récupération documentaire.
5. Admission transactionnelle : contrôles patient/unité/espace/médecin, claim atomique `FREE + OPEN + READY`, affectation active et audit.
6. Séjour : notes, soins, médicaments, consommables, consentements et compte rendu opératoire.
7. Décision médicale de sortie : le séjour reste actif et le lit occupé.
8. Confirmation du départ physique : clôture du séjour et de l'affectation, fiche PDF, état de nettoyage du lit.
9. Précalcul financier : séjour, soins facturables, consommations, CRO validés et prescriptions.

## Constats prioritaires

### HOS-QA-01 — P1 : droits d'admission incompatibles avec les API de lecture utilisées

**Preuve :** `patient-hospitalization.component.ts:160,325,349` et `emergency-hospitalization-continuation.component.ts` appellent `StaffApiService.list`, `HospitalOrganizationApiService.listUnits/listServiceCatalog` et `SpatialApiService.listSpaces/listBeds/listUnitSpaceAssignments`.

`spatial-api.service.ts:58,119` vise `/api/spatial/configuration/spaces` et `/api/spatial/configuration/beds`. Les controllers de configuration imposent `SPATIAL_CONFIGURATION_MANAGE`. Les profils standards `MEDECIN` et `RESPONSABLE_HOSPITALISATION` possèdent `HOSPITALIZATION_ADMIT` mais pas ce droit. Le responsable n'a pas non plus `USER_READ`, requis par `/api/staff` et ses affectations, ni `VISIT_READ`, utilisé pour les visites du patient.

**Impact :** un profil autorisé à admettre voit l'action, mais ses lectures nécessaires échouent en 403 selon les permissions effectives. Un compte administrateur peut masquer cette anomalie. Le transfert utilise les mêmes lectures de configuration.

**Reproduction attendue :** utiliser un médecin ou responsable avec les seules permissions standards, ouvrir l'admission normale/urgence puis sélectionner unité et lit. Vérifier les GET et la liste des choix. Reproduction navigateur non réalisée.

**Correction :** fournir des projections d'admission en lecture seule, autorisées par les permissions métier pertinentes, et adapter les clients. Ne pas accorder des droits de modification de structure/personnel pour débloquer le formulaire. Ajouter un test API de toutes les lectures nécessaires sous chaque profil.

### HOS-QA-02 — P1 : prise en charge et sauvegarde clinique non atomiques

**Preuve :** `VisitCareFlowService.java:35,61,93` lit par `findById`, vérifie le praticien puis sauvegarde ; `ConsultationService.java:54,70,79` lit la visite et compare `expectedUpdatedAt` en mémoire avant la sauvegarde. `VisitEntity` et `ConsultationEntity` n'ont pas de `@Version`. `VisitRepository.findByIdForUpdate` existe mais n'est pas utilisé par ces transitions.

**Impact :** deux transactions simultanées peuvent lire le même état et satisfaire toutes deux le contrôle. La deuxième prise en charge peut remplacer la première sans reprise explicite ; la comparaison du timestamp ne protège pas deux sauvegardes qui lisent le même timestamp avant leurs commits.

**Niveau de preuve :** défaut identifié dans le code ; course concurrente non reproduite sur PostgreSQL. Le test `onlyOnePractitionerHoldsTheConsultationUnlessExplicitTakeOver` effectue des requêtes successives, pas simultanées.

**Correction :** sérialiser les transitions et sauvegardes par verrou de visite commun, ou utiliser un versionnement atomique avec réponse 409. Couvrir aussi release/sauvegarde/reprise par un test à deux transactions avec barrières de synchronisation.

### HOS-QA-03 — P1 : administration médicamenteuse sans contrôle de la prescription référencée

**Preuve :** `HospitalizationCareService.java:88` contrôle le séjour actif puis copie `request.prescriptionItemId`, `medicationName` et `dose` sans résolution de la prescription. `CreateMedicationAdministrationRequest` rend la référence optionnelle. V47 ne crée pas de FK pour `prescription_item_id`. Le panneau Angular collecte uniquement médicament et dose en texte libre.

**Impact :** lorsqu'une référence est fournie, le service ne garantit ni son existence, ni son rattachement au patient/séjour, ni sa cohérence avec le médicament. Le catalogue RBAC décrit pourtant la permission comme une administration d'un médicament prescrit, sans droit de prescription.

**Correction :** valider côté serveur la prescription, son patient/établissement, son statut et la cohérence de l'administration. Si le métier autorise une administration sans prescription structurée, définir un chemin explicite, habilité et audité. Tests négatifs : référence inconnue, autre patient, autre tenant, prescription inactive.

### HOS-QA-04 — P1 : erreurs de précalcul financier silencieuses

**Preuve :** `InvoicePrecalculationService.java:166,187,251,274,288` intercepte `Exception` sans signaler l'échec, dans les soins, consommations, CRO et prescriptions.

**Impact :** des erreurs peuvent supprimer des postes ou laisser des postes partiellement ajoutés sans état « calcul incomplet ». Une erreur de persistance peut aussi invalider la transaction et échouer plus tard. Dans les deux cas, la tolérance actuelle ne garantit pas un résultat financier complet et inspectable.

**Correction :** distinguer absence normale de données d'une défaillance ; refuser ou signaler explicitement un précalcul incomplet et journaliser l'incident sans données sensibles. Tester les pannes de chaque source et empêcher une validation financière silencieuse.

### HOS-QA-05 — P2 : absence de lit confondue avec un échec de chargement

**Preuve :** `patient-hospitalization.component.ts:349` transforme toute erreur de `listBeds` en `freeBeds=[]` sans message. `loadHospitalizations`, `loadVisits`, `loadStaff`, consentements et CRO ont également des abonnements sans gestion d'erreur.

**Impact :** 403/panne réseau et absence réelle de disponibilité deviennent difficiles à distinguer. Si le chargement initial des séjours échoue, l'état vide peut proposer une nouvelle admission alors que le patient est déjà hospitalisé ; le backend reste le dernier contrôle.

**Correction :** séparer loading/empty/error, afficher un message traduit et une reprise, désactiver les actions tant que l'état requis n'est pas connu. Tester 403, 500 et erreur réseau.

### HOS-QA-06 — P2 : formulaire d'admission sans garde de soumission en cours

**Preuve :** `patient-hospitalization.component.ts:257` n'a pas d'état `submitting`, contrairement à `savePhysicalDeparture` et au parcours urgence. Le bouton HTML est désactivé uniquement si le lit n'est pas sélectionné.

**Impact :** un double clic déclenche deux POST. Le claim atomique du même lit protège son occupation, mais la seconde réponse 409 peut être affichée après la réussite de la première et masquer le résultat.

**Correction :** garde UI avec désactivation pendant la requête et rafraîchissement guidé des disponibilités en cas de 409 ; tester deux soumissions avant la première réponse.

### HOS-QA-07 — P2 / gate standards : i18n, taille et arrondis

- `patient-hospitalization.component.ts` fait **589 lignes**, au-delà du maximum obligatoire de 500 ; le template fait 545 lignes et contient des sous-parcours consentements/CRO à extraire.
- Le template contient des textes français directs (`Type`, `Anesthésie`, `Signature du patient présente`, `Enregistrer`, etc.) ; les erreurs `alert` et la confirmation de validation CRO sont aussi françaises en dur (`.ts:447,457,499,569,574`). Le contrôle i18n exécuté ne couvre que 47 clés du shell.
- Le template utilise `rounded-xl` sur les modales (`.html:399` et suivantes), tandis que les tokens applicatifs standards sont limités à 8 px. Aucune exception applicable à ces modales n'a été relevée dans DESIGN.md ; rendu et valeur calculée restent à vérifier en recette.

**Correction :** extraire les panneaux/formulaires, déplacer tous les textes en FR/EN, utiliser les composants/tokens centraux et vérifier light/dark, mobile et clavier. Aucune conclusion visuelle sans capture.

## Point métier à arbitrer

La liste d'admission propose toutes les visites (`.html:416`), et `HospitalizationAdmissionService.java:257` vérifie leur patient mais pas leur état. Le code permet donc de sélectionner une visite terminée. Définir si ce rattachement historique est permis ou si une visite active doit être exigée ; ajouter ensuite le contrôle backend et les tests correspondants. Ce point est une incohérence potentielle à valider, pas une règle métier inventée par la revue.

## Risques structurels déjà documentés

DIAG-20260809 reste ouvert : demande/préadmission/réservation, acceptation par l'unité, handoff, compatibilité clinique du lit, absence de lit et contexte clinique au placement. Ces capacités ne sont pas validées par le succès des tests existants. Les évolutions structurantes restent rattachées à HOS-ADM → HOS-MOV → HOS-PATH, avec cadrage métier avant développement.

## Vérifications

| Vérification | Résultat de cette session | Limite |
|---|---|---|
| Angular `npm.cmd test -- --watch=false` | 112 fichiers, **593/593 tests verts**, sortie 0 | Tests principalement isolés/mockés ; pas d'E2E |
| Angular `npm.cmd run build` | Build production réussi, sortie 0 | Pas de preuve visuelle |
| `npm.cmd run i18n:check` | 47 clés shell présentes en FR/EN, sortie 0 | Ne certifie pas les textes hospitaliers |
| Maven ciblé | **65/65 tests verts**, 8 classes, zéro échec/erreur/skip, `BUILD SUCCESS`, sortie 0 | Profil test/H2, pas PostgreSQL |
| `npm run lint` | Non exécuté | Aucun script lint dans package.json |
| Proxy, configuration, `.gitignore` | Maven/pom, YAML, proxy référencé, API relatives, Tailwind v4 et exclusions vérifiés | Pas d'audit global des secrets/dépendances |
| Chrome localhost:4201 | Deux tentatives refusées | Permission enregistrée bloque l'accès ; aucune recette |

Le premier lancement Maven échoue sur l'accès réseau au parent ; le cache workspace est incomplet. La relance hors réseau avec le cache Maven utilisateur existant atteint les tests sans modifier le pom ni télécharger de dépendance. Les journaux locaux sont ignorés dans `.ai-tmp/qa-20261004-*`.

Commande Maven réussie, depuis `backend` :

```powershell
.\mvnw.cmd -o '-Dmaven.repo.local=C:/Users/Jocelin FOMEKONG/.m2/repository' '-Dtest=VisitControllerTest,VisitCareFlowControllerTest,HospitalizationAdmissionServiceTest,HospitalizationControllerTest,HospitalizationControllerAuthorizationTest,ActiveBedAssignmentServiceTest,BedAssignmentRepositoryTest,RbacCatalogHospitalizationClinicalPermissionTest' test
```

| Classe backend | Tests réussis |
|---|---:|
| RbacCatalogHospitalizationClinicalPermissionTest | 6 |
| HospitalizationControllerAuthorizationTest | 8 |
| HospitalizationControllerTest | 7 |
| HospitalizationAdmissionServiceTest | 7 |
| ActiveBedAssignmentServiceTest | 3 |
| BedAssignmentRepositoryTest | 9 |
| VisitCareFlowControllerTest | 7 |
| VisitControllerTest | 18 |

Les logs d'accès refusé et de contrainte d'unicité présents pendant cette suite correspondent aux tests négatifs réussis ; ils ne sont pas des incidents du serveur local.

## Checklist QA et suite

- [x] Gouvernance et diagnostics existants consultés ; critères d'acceptation explicites.
- [x] Revue croisée des contrôles front/backend et permissions ; aucune permission affaiblie.
- [x] Claim atomique du lit et filtre OPEN/READY présents.
- [x] Workflow médical/départ physique identifié et tests associés exécutés dans la sélection Maven.
- [x] Tests Angular, build et i18n shell terminés.
- [x] Résultat final Maven consigné : 65/65, build réussi.
- [ ] Course de prise en charge/sauvegarde testée sous PostgreSQL.
- [ ] Recette authentifiée multi-profils, FR/EN, light/dark, desktop/mobile.
- [ ] Validation clinique et arbitrage des gaps EPIC-0027.
- [ ] Corrections HOS-QA-01 à 07 à découper avec tests d'acceptation avant développement.

## Impacts

Revue documentaire seulement : aucune modification de code applicatif, contrat, schéma, configuration ou donnée du serveur local ; aucun bump SemVer ni release. La revue ne ferme pas les tickets métier ni les gates E2E. Les estimations des corrections et l'engagement de sprint restent à établir séparément.

## Résolution des constats — FIX-20261004

La revue initiale ci-dessus reste la preuve avant correction au commit 5a63023a. Le lot de correction est implémenté localement, non publié, et prêt pour review.

| Constat | Résolution implémentée | Preuve après correction |
|---|---|---|
| HOS-QA-01 | Projections placement, lits, praticiens et visites sous permissions métier, tenant courant | HospitalizationWorkflowControllerTest : médecins/responsable et transfert infirmier, configuration refusée, tenant étranger refusé |
| HOS-QA-02 | Verrou de visite commun aux transitions, sauvegarde, close/cancel/correct | Deux tests concurrents MockMvc : un succès et un 409 |
| HOS-QA-03 | Prescription validée obligatoire acceptée par l'utilisateur, contrôlée côté serveur ; UI propose lignes admissibles | Scénarios nominaux et négatifs patient/tenant/statut/expiration/nom, annulation après chargement |
| HOS-QA-04 | Suppression des catch Exception silencieux des quatre sources ; erreur source propagée | 4 tests de panne + calculs nominaux hospitalisation/CRO/implants |
| HOS-QA-05 | États loading/error/empty, reprise ; lecture échouée distincte d'absence | Tests Angular chargement/reprise ; état visites indépendant ; erreurs consent/CRO/historique médicaments visibles |
| HOS-QA-06 | Garde admission/transfert et autres panneaux ; disponibilités actualisées après conflit | Tests double soumission et réponse lits obsolète |
| HOS-QA-07 | Consent/CRO extraits, confirmation partagée, catalogues FR/EN, rayon central | 150 clés vérifiées FR/EN, maximum 466 lignes dans les composants modifiés, build réussi |

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
