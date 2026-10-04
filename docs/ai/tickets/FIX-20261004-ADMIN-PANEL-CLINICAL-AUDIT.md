# FIX-20261004 — Corrections ciblées du panel administrateur

## Cadre
Diagnostic + Engineering + QA ; READY_FOR_REVIEW pour les corrections locales. Source : docs/qa/QA-20261004-ADMIN-PANEL-CLINICAL-AUDIT.md. Demande explicite de corriger tous les constats, y compris ceux hors matrice FIX-01 à 08. Parent EPIC-0027 / RBAC / ADMINISTRATION. L'intégration qualifiée externe reste ouverte ; aucune conformité réglementaire complète ni recette déclarée terminée.

## EPIC → Stories → Tasks → Subtasks
| Story / tâche | Objectif et critères d'acceptation | Sous-tâches | Estimation senior | Profil / reviewer | Tests attendus |
|---|---|---|---|---|---|
| ADMIN-01 accès et profil | Profil exploitable sans affectation ; journal accessible et autorisé ; liens distincts | chargement indépendant, route audit, lien interop | 0.5j / 2 SP | Angular / QA | 403 affectations, erreur profil, navigation et permissions |
| ADMIN-02 organisation | Services nommables avec code catalogue obligatoire ; disciplines manquantes disponibles ; gouvernance d'unité traçable | modèle, migration additive, API/UI, rôles d'affectation | 1j / 3 SP | Spring/Angular / Tech Lead + praticien | nom libre/fallback, catalogue, isolation tenant |
| ADMIN-03 capacité | Néonatal/urgences compatibles ; ambulatoire configurable | migration référentiels, tests profil/lits | 0.5j / 2 SP | Spring / bed manager + QA | catalogues et création profil |
| ADMIN-04 autorisations | Prescription, signature, validation et dispensation isolées ; actes validés protégés | permissions, rôles, contrôles serveur et UI | 1.5–2j / 5 SP | Senior full-stack / sécurité + médecin + pharmacien | refus infirmier, droits dédiés, immutabilité et traçabilité |
| ADMIN-05 UX | Champs invalides identifiables ; explication types ; FR/EN, breadcrumbs et accents corrects | validation partagée, traductions, tests composants | 0.5j / 2 SP | Angular / QA UX | submit invalide, messages, dictionnaires |
| ADMIN-04-PNG complément utilisateur | Médecin actif peut téléverser une signature PNG/JPEG, stockée réellement en PNG et utilisée dans ses PDF ; fichier invalide refusé | normalisation contrôlée ImageIO, use case upload, aide FR/EN, preuve PNG/profil/PDF | 0.5j / 1 SP | Spring/Angular / Tech Lead + médecin | transparence, vrai PNG, JPEG, dimensions/format invalides, rôle, persistance profil et image PDF |

Pour chaque tâche ADMIN-01 à 05 et ADMIN-04-PNG, READY : audit, code, impacts et spécifications initiales lus ; critères et tests définis. DONE : critères implémentés, gates vérifiées, documentation et suivi actualisés ; review humaine et recette restent distinctes. Capacité nominative inconnue : aucune date/sprint engagé.

## Actions
- [x] Lire l'audit et la gouvernance ; identifier les changements utilisateur préexistants à préserver.
- [x] Écrire le découpage et la documentation initiale.
- [x] Corriger ADMIN-01.
- [x] Corriger ADMIN-02.
- [x] Corriger ADMIN-03.
- [x] Corriger ADMIN-04 côté application : permissions, revue pharmaceutique, acteur actif, scellement serveur et verrouillage.
- [x] Corriger ADMIN-05.
- [x] Exécuter tests Angular/Maven, build, i18n et revue du diff.
- [x] Finaliser preuves, checklist, changelog et suivi.
- [ ] Recette clinique multi-profils et PostgreSQL.
- [ ] Review Tech Lead / médecin / pharmacien / bed manager et attribution des droits aux rôles personnalisés.
- [ ] ADMIN-04-EXT : intégrer DSS avec certificats/services de confiance et vérification ordinale après configuration et validation des règles applicables.
- [x] ADMIN-04-PNG : JPEG/PNG normalisés en PNG réel ; aide FR/EN ; droits admin préservés ; preuves profil et image dans le PDF.

2026-10-04 — Précision utilisateur : solution gratuite/open source et image de signature du médecin stockée en PNG. OpenPDF existant + ImageIO retenus pour le circuit visuel ; DSS retenu pour une future intégration cryptographique, sans déclarer cette intégration ou un certificat qualifié disponibles. Charge locale portée à 15 SP / 4.5–5 jours senior indicatifs, hors sprint. DoR/DoD et reviewers de ADMIN-04-PNG : règles et formats ci-dessus documentés, tests définis ; fini après preuves PNG/profil/PDF et docs/suivi actualisés.

## Preuves

647 tests Angular dans 118 fichiers, build production et i18n:check verts. 56 nouvelles clés de base présentes en FR/EN, sans conflit de surcharge dans les dictionnaires de feature. Maven complet : 922 tests, 0 échec/erreur, 9 PostgreSQL ignorés faute de Docker ; compléments ciblés : 61 cas cliniques et 39 cas PNG/document verts, avec recouvrement de la suite complète. V113/V114 appliquées sous H2. Diff sans erreurs de whitespace, encodage UTF-8 contrôlé, aucun secret/artefact suivi.
[Diagnostic et revue](../validation/DIAG-20261004-ADMIN-PANEL-CLINICAL-AUDIT.md), [plan et résultats](../../features/admin-panel-clinical-audit-fixes/TEST-PLAN.md), [ADR](../../architecture/adr/ADR-20261004-clinical-act-authorities-and-sealing.md).

## Sous-tâche externe ADMIN-04-EXT

Objectif : preuve cryptographique du contenu et du temps avec DSS, avec qualification professionnelle vérifiée selon le parcours accepté. Critères : identité/consentement du signataire vérifiés, preuve du contenu et du temps vérifiable indépendamment de la DB, conservation et révocation définies, erreur du service de confiance refusant la validation sans faux succès. Sous-tâches : configurer certificats et services de confiance avec DSS, définir ADR/API/modèle, intégrer l'adaptateur et les preuves, exécuter tests altération/indisponibilité/révocation et revue métier. Le choix du logiciel gratuit/open source est résolu ; les certificats et services de confiance ne sont pas fournis par le dépôt.
Profil : architecte sécurité + senior backend + référent métier ; reviewer : Tech Lead, médecin, conformité. Estimation : à cadrer après choix du fournisseur (hors 15 SP du correctif local). DoR : fournisseur, environnement de test, habilitations et critères réglementaires acceptés. DoD : preuves indépendantes, tests et recette avec référents acceptés. Dépendances externes absentes du dépôt : cette intégration n'est pas simulée.

## Impacts / risques
Backend maître, migrations nouvelles uniquement, aucun secret ni URL d'environnement introduit. Angular Tailwind v4, thèmes/tokens et proxy conservés. Flutter : consommateurs prescriptions à vérifier lors de livraison coordonnée. CI inchangée. OWASP : autorisations et tenant vérifiés serveur, échappement Angular conservé. 12-Factor : configuration et stockage durable existants. SemVer : ajout catalogues/champs MINOR ; remplacement de CLINICAL_WRITE pour prescrire/signature = rupture d'autorisation, MAJOR candidat ; aucune release préparée.

## Versionnement local demandé — 2026-10-04

- [x] Préparer le commit des corrections administrateur, signature PNG, tests, migrations et documentation, avec mention BREAKING CHANGE.
- [x] Définir l'exclusion des changements préexistants QA AUTH/RBAC/session, y compris leurs sections des documents de suivi partagés.
- [x] Réutiliser les preuves finales 647 tests Angular / 922 Maven (9 PostgreSQL ignorés), build et i18n ; aucune modification de code depuis ces vérifications.
Périmètre : commit local seulement, sans push, tag, release ni déploiement. Le statut READY_FOR_REVIEW et les gates restantes ne changent pas.
- [x] Versionner localement les travaux après levée des restrictions d'écriture et confirmation utilisateur. Les tentatives précédentes étaient bloquées par `.git` en lecture seule ; les travaux QA AUTH/RBAC/session restent exclus du périmètre.
