# Vérification des corrections

## Scénarios
- Profil : affectations 403/500 sans perte du profil ; erreur de l'API principale conservée ; compte plateforme sans établissement → contexte vide autorisé.
- Navigation : /audit-trail reconnu, onglet audit ouvert et autorisé ; liens établissements/interop distincts ; fiche API de l'établissement sélectionné.
- Organisation : nom local sauvegardé/affiché dans toutes les projections ; nom vide → catalogue localisé ; catalogue toujours obligatoire ; noms trop courts/longs et tenant incohérent refusés.
- Référentiels : toutes les disciplines citées disponibles ; néonatal/urgences et ambulatoire compatibles avec profil hospitalier ; migrations PostgreSQL et H2.
- Gouvernance : responsabilités historisées disponibles, qualification professionnelle contrôlée, aucun droit clinique dérivé automatiquement d'une affectation.
- RBAC : infirmier CLINICAL_WRITE sans prescription/signature ; médecin autorisé ; administrateur sans signature automatique ; pharmacien validation/délivrance, sans signature médicale.
- Signature : acteur actif, empreinte et heure persistées ; modification après signature refusée ; clôture ne contourne ni permission ni complétude ; écriture/signature concurrentes sérialisées.
- Pharmacie : anonyme/infirmier refusés ; revue obligatoire ; statut actif, PIN et expiration ; quantités, historique, doublons de lignes ; validation et délivrance auditées avec acteur.
- UX : champs vides/blancs et email invalide signalés au champ avec aria-invalid/aria-describedby ; formulaire valide et non bloqué ; i18n uploader, erreurs et breadcrumbs FR/EN.

## Gates

Complément PNG : JPEG au nom trompeur devient PNG réel ; PNG transparent préserve alpha ; image longue conserve ratio et dimensions non nulles ; en-tête seul/format non pris en charge/dimensions excessives refusés sans fichier brut ; API upload autorisée par FILE_UPLOAD pour médecin/admin ; association au profil soignant refusée ; signaturePath enregistrée puis lue dans le profil ; image présente dans les ressources du PDF de consultation. Photos/logos historiques non modifiés.
Suite Angular, build production, i18n:check, diff et revue encodage. Maven via wrapper/cache local hors ligne ; suite backend complète et contrôles PostgreSQL si Docker disponible. Les tests H2 ne constituent pas une recette PostgreSQL.
Recette navigateur médecin/infirmier/pharmacien/admin, FR/EN light/dark et mobile : à distinguer des tests automatisés. Validation clinique et signature qualifiée externe restent des gates humaines/infrastructure.

## Résultats — 2026-10-04

| Gate | Résultat |
|---|---|
| `npm test -- --watch=false` | 118 fichiers, 647 tests réussis |
| `npm run build` | Build de production réussi, aucun warning de template inutilisé après extraction |
| `npm run i18n:check` | 47 clés shell FR/EN contrôlées |
| Contrôle des nouvelles traductions | 56 clés ajoutées en FR/EN, aucune vide/manquante ni surcharge contradictoire dans les features |
| Maven wrapper hors ligne, cache utilisateur explicite, `test` | 922 tests, 0 échec/erreur, 9 ignorés |
| Complément Maven ciblé avant la suite complète finale | 61 tests, 0 échec/erreur/ignoré : consultation, prescription, gouvernance staff, catalogues, spatial, politique de signature et hash |
| Complément ciblé PNG/document avant la suite complète finale | 39 tests verts avec recouvrement ; le cas administratif ajouté ensuite est couvert par la suite complète finale |
| Flyway H2 | V113/V114 appliquées dans les contextes d'intégration |
| `git diff --check`, UTF-8, `.gitignore` | Réussis ; secrets, caches, builds et logs locaux non suivis |
| PostgreSQL Testcontainers | Non exécuté : Docker indisponible ; neuf tests ignorés par la condition existante |
| Recette navigateur/clinique et fournisseur qualifié | Ouvertes, non déduites des tests unitaires/H2 |

Les 61 cas ciblés recouvrent partiellement les 922 cas de la suite complète. Logs ignorés dans `.ai-tmp/admin-*.log` ; rapport consolidé dans docs/ai/validation/DIAG-20261004-ADMIN-PANEL-CLINICAL-AUDIT.md. Le wrapper utilise le cache `C:/Users/Jocelin FOMEKONG/.m2/repository` ; aucun changement de configuration Maven du projet.
