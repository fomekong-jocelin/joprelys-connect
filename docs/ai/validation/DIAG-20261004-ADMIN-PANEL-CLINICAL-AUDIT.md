# Diagnostic et revue — panel administrateur et actes cliniques

Date : 2026-10-04. Source : [audit fourni](../../qa/QA-20261004-ADMIN-PANEL-CLINICAL-AUDIT.md). Ticket : [FIX-20261004](../tickets/FIX-20261004-ADMIN-PANEL-CLINICAL-AUDIT.md).
Périmètre : corrections locales Angular / Spring Boot / migrations ; aucune action sur recette.joprelys.com, aucun déploiement ni release. Le rapport source reste inchangé.

## Causes et corrections vérifiées

| Constat | Cause confirmée dans le code | Correction et preuve |
|---|---|---|
| FIX-01 profil indisponible | forkJoin échouait sur le contexte d'affectations ; backend exigeait un établissement | CatchError limité aux affectations, avertissement indépendant, réponse vide pour le compte plateforme sans établissement. Tests profil 403/500 et erreur API principale ; ProfileAssignmentsControllerTest. |
| FIX-02 audit inaccessible | CTA sans navigation, aucune route racine /audit-trail | Lien /clinic/rbac?tab=audit et alias racine, chargement de l'onglet une seule fois après résolution du tenant. Tests routes/RbacManagementComponent. Le journal affiché est celui des autorisations, distinct du journal patient. |
| FIX-03 services anonymes | Front, normalisation domaine et contrainte DB imposaient name null | Nom local optionnel, code catalogue obligatoire, priorité au nom local dans les projections organisation/profil/spatial ; fallback FR/EN préservé. Tests API et composant ; V113. |
| FIX-04 catalogues incomplets | Entrées manquantes dans les référentiels globaux | Toutes les disciplines citées ajoutées : urgences (spécialité), orthopédie/traumatologie, néonatologie, infectiologie, gastro/endoscopie, ophtalmologie, ORL, stomatologie, néphrologie, hémodialyse. Tests API des codes, 23 services / 20 spécialités ; V113. |
| FIX-05 espaces non hospitaliers | Néonatal/box urgences absents du catalogue de compatibilité | V113 ajoute ces compatibilités et hôpital de jour, dialyse, chimiothérapie, chirurgie ambulatoire. SpatialControllerTest crée réellement un profil puis un lit pour chacun des six types. |
| FIX-06 prescription/délivrance non isolées | CLINICAL_WRITE autorisait la prescription ; délivrance publique sans identité d'acteur | Six droits explicites, contrôles backend et UI, rôle soignant sans prescription/signature, administrateur sans nouveaux actes réservés, pharmacien avec revue/délivrance. Tests RBAC, refus infirmier/anonyme et pharmacie. |
| FIX-07 erreurs peu localisables | Bannière seule, aucune erreur liée au champ | InputComponent fournit bordure/message centralisés, label relié, aria-invalid/describedby ; blancs et email invalide arrêtent la soumission. Tests de formulaire et ARIA. |
| FIX-08 FR/EN et accents | Libellés uploader/type/organisation statiques, clés breadcrumbs absentes | Dictionnaires FR/EN, textes et rejets uploader, déconnexion/confidentialité accentués ; breadcrumbs réactifs lors du changement de langue. Tests uploader FR/EN et navigation/locale. |
| Gouvernance des unités, hors matrice | Affectations existantes sans responsabilités ciblées | MEDICAL_HEAD et NURSE_MANAGER via périodes/historique/tenant existants, qualification contrôlée, lien vers gestion staff. Test médecin/infirmier et refus du rôle de responsabilité incompatible. Aucun droit clinique accordé par l'affectation. |
| Signature/verrouillage, hors matrice | Clôture activait un brouillon sans permission ni complétude ; note pouvait être réécrite | Acteur médecin actif, heure serveur et hash du contenu ; note VALIDEE protégée ; droits de clôture et prescription distincts, verrous partagés, finalisation par service existant. Tests signature/immutabilité (même si visite/statut sont réouverts par un autre traitement), qualification et empreintes. |
| Validation pharmaceutique, hors matrice | Pas de revue enregistrée avant délivrance | Revue manuelle avec notes, acteur et date, droit PHARMACY_VALIDATE ; prérequis de délivrance côté serveur et UI. Validation/délivrance/annulation sérialisées sur l'ordonnance, doublons de lignes rejetés ; tests PharmacyControllerTest et parcours UI. |
| Redondance établissements/interop, hors matrice | Deux cartes vers /organizations | /interop distinct et autorisé, état API réel et lien vers la fiche de clés de l'établissement sélectionné. Test contrat de route ; recette navigateur ouverte. |
| Signature PNG, précision utilisateur | Upload signature pouvait conserver un fichier brut après décodage impossible | ImageIO décode PNG/JPEG, limite dimensions/pixels avant allocation et réencode PNG ; alpha conservé et métadonnées retirées. FileUploadUseCase préserve FILE_UPLOAD pour médecin/admin ; profil médecin contrôlé. Tests JPEG trompeur, transparence, image mince, format/header/dimensions invalides, profil persisté et ressource image dans PDF OpenPDF. |
| Ambiguïté du type d'établissement | Aucune explication des accès aux modules | Aide FR/EN par type : catégorie administrative, accès déterminés par rôles/autorisations ; aucune activation fictive de modules. |

## Revue technique et sécurité

Deux migrations nouvelles V113/V114, migrations appliquées intactes. Les enregistrements historiques ne reçoivent pas de fausse signature. Aucune suppression de données. Les catalogues conservent les codes initiaux ; les noms null restent acceptés.
PrescriptionController délègue à un contrat applicatif ; consentement et portée tenant restent contrôlés côté serveur. La clôture utilise le même verrou de visite que l'édition, la pharmacie verrouille l'ordonnance avant mutation. Les nouveaux droits ne sont pas automatiquement copiés aux rôles personnalisés : attribution explicite requise avant livraison.
Le PIN conserve le parcours de présentation à une pharmacie externe ; il n'accorde pas le droit de délivrer. L'acteur est audité. Les notes de revue ne sont pas exposées dans la réponse publique. Aucun diagnostic automatique d'interactions/doses/contre-indications n'est revendiqué.
Maven, YAML, proxy Angular et appels relatifs inchangés ; aucune dépendance ajoutée. Tailwind v4 et tokens light/dark conservés, FR/EN centralisés. `.gitignore` contrôle les secrets, caches, builds et preuves locales. Aucun secret ni fichier clinique réel ajouté.
Les sections prescription/constantes/analyses sont extraites en panneaux de présentation : template consultation 214 lignes, chaque sous-template sous 300 ; métriques spatiales extraites sans changement de calcul. Aucun fichier de production modifié ne dépasse 500 lignes. Les alertes au-delà de 300 lignes des services/composants existants restent une dette tracée, sans refonte générale.
Les modifications utilisateur préexistantes du dossier auth/session sont préservées ; AUTH-01 fail-open reste un risque distinct, déjà documenté dans QA-20261004-AUTH-RBAC-SESSION-VERIFICATION.

## Preuves et limites

Maven exécuté via `backend/mvnw.cmd -o -Dmaven.repo.local=... test` avec le cache utilisateur existant ; aucun téléchargement requis. Suite complète finale (06:38, incluant signature PNG et upload administratif) : 922 tests, 0 échec, 0 erreur, 9 ignorés. Complément ciblé avant la suite complète finale, après ajout des cas de scellement/gouvernance/catalogues : 61 tests ciblés, 0 échec/erreur/ignoré. Les 61 cas cliniques et les 39 cas PNG/document ciblés recouvrent la suite complète ; ils ne doivent pas être additionnés comme cas uniques. Le test d'upload administratif ajouté ensuite est couvert par la suite complète finale.
Les neuf tests PostgreSQL Testcontainers sont ignorés par leur condition existante : Docker indisponible (`Could not find a valid Docker environment`). V113/V114 ont été appliquées dans les contextes H2 de la suite. Une migration PostgreSQL réelle reste à vérifier.
Preuves locales ignorées par Git : `.ai-tmp/admin-backend-full-tests.log`, `.ai-tmp/admin-backend-final-targeted-tests.log`, `.ai-tmp/admin-signature-png-tests.log`, `.ai-tmp/admin-frontend-tests.log`, `.ai-tmp/admin-frontend-build.log`, `.ai-tmp/admin-i18n-check.log` ; rapports Surefire dans backend/target.
Résultat Angular final et checks consolidés : voir [TEST-PLAN](../../features/admin-panel-clinical-audit-fixes/TEST-PLAN.md).

## Gates restantes

- Review Tech Lead / sécurité / médecin / pharmacien / bed manager ; recette navigateur multi-profils, FR/EN, light/dark et mobile.
- PostgreSQL : migration greenfield + upgrade V112 → V114, contraintes et transactions concurrentes sur moteur réel.
- Signature/horodatage qualifiés : DSS open source retenu pour l'intégration future, mais non intégré ; aucun fournisseur de confiance ni raccordement ordinal RPPS/ONMC configuré dans ce lot. L'empreinte et l'heure serveur ne constituent pas une signature qualifiée, un horodatage certifié ou une protection contre un administrateur DB privilégié. Ce constat réglementaire de l'audit reste ouvert, avec ADR et sous-tâche externe dans le ticket.
- Attribution explicite des droits aux rôles personnalisés, coordination mobile/partenaires et validation du parcours pharmacie authentifié avant livraison MAJOR candidate.

Statut : IMPLEMENTED / READY_FOR_REVIEW pour les corrections locales ; conformité réglementaire complète et readiness de recette non déclarées.
