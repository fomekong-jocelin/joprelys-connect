# EPIC-0027 — Organisation hospitalière, capacité et parcours patient

**Origine** : AUDIT-20260721  
**Statut** : PROPOSED / non engagé  
**Priorité globale** : Critique  
**SemVer cible** : ajout parallèle MINOR possible ; retrait des contrats legacy MAJOR  
**Effort total indicatif** : 103–143 jours-personnes, QA et documentation incluses

## Vision

Fournir à Joprelys un modèle flexible, historisé et sûr permettant à un cabinet, une clinique, un hôpital ou un réseau de gérer son organisation, ses espaces, ses capacités, ses personnels, ses ressources et le parcours réel d'un patient.

## Découpage EPIC → User Stories → Tasks → Subtasks

| Story | Objectif | SP | Effort | Profil | Reviewer | Tests attendus |
|---|---|---:|---:|---|---|---|
| HOS-BED-001 | invariants et concurrence lits | 12–14, réestimé | 9–12j | backend senior + DBA | cadre + DBA + QA | PostgreSQL concurrence/migration |
| HOS-BED-002 | capacité et cycles de remise en état | 8, à découper | 8–11j | full-stack senior | cadre + hygiène | API/E2E/KPI |
| HOS-RBAC-001 | permissions hospitalières contextuelles | 8, à découper | 8–12j | sécurité/backend | RSSI/DPO + métiers | matrice négative multi-tenant |
| HOS-ORG-001 | organisation flexible multi-structure | 13, obligatoire | 12–16j | architecte/full-stack | direction + DBA | hiérarchie/cycles/migration |
| HOS-LOC-001 | géographie et espaces génériques | 13, obligatoire | 12–16j | full-stack senior | cadre + logistique | type/localisation/partage |
| HOS-ADM-001 | demande, préadmission et réservation | 13, obligatoire | 12–16j | backend/frontend senior | médecin + admissions | workflow/concurrence/E2E |
| HOS-MOV-001 | présence et mouvements | 13, obligatoire | 10–14j | backend senior | cadres source/destination | transfert/handoff/retry |
| HOS-DIS-001 | sortie médicale/admin/physique | 8, à découper | 8–11j | full-stack senior | médecin + DAF + cadre | issues/turnover/facturation |
| HOS-STAFF-001 | affectations et habilitations datées | 13, obligatoire | 10–14j | backend + RH frontend | RH + cadre + RSSI | périodes/délégation/accès |
| HOS-RES-001 | ressources et équipements partagés | 13, obligatoire | 10–14j | full-stack/biomédical | biomédical + bloc | réservation/maintenance |
| HOS-PATH-001 | parcours transverse et work-items | 13, obligatoire | 10–14j | architecte/full-stack | métiers + DPO | saga/idempotence/E2E |
| HOS-KPI-001 | dashboards de capacité | 8, à découper | 7–10j | data/backend/frontend | direction + contrôle gestion | réconciliation/performance |
| HOS-INT-001 | interopérabilité et mode dégradé | 8, à découper | 7–10j | interop/SRE | RSSI + exploitation | contrats/reprise/charge |

Chaque story supérieure à 5 SP doit être découpée en tâches de 1 à 5 SP avant sprint. Aucun lot ne doit combiner migration DB, API, UI et recette complète dans une seule tâche.

### Incréments phase 0 engagés

| Task | Story | Objectif | SP | Effort senior | Statut | Reviewer | Tests attendus |
|---|---|---|---:|---:|---|---|---|
| HOS-BED-001-A | HOS-BED-001 | Interdire plusieurs affectations actives sur un même lit | 3 | 2j | QA H2 VERTE / POSTGRESQL REQUIS | Lead backend + DBA + cadre | H2 + MockMvc + PostgreSQL 16 |
| HOS-BED-001-B | HOS-BED-001 | Rattacher l'affectation au séjour, limiter une présence active par séjour et valider la période | 3 | 2j | QA H2 VERTE / POSTGRESQL REQUIS | Lead backend + DBA + cadre/DPO | H2 + parcours + PostgreSQL 16 |
| HOS-BED-001-C | HOS-BED-001 | Garantir le même établissement pour l'affectation, le séjour et le lit | 3 | 2j | QA H2 VERTE / POSTGRESQL REQUIS | Lead backend + DBA + RSSI/DPO + cadre | H2 + parcours + PostgreSQL 16 |
| HOS-BED-001-D | HOS-BED-001 | Interdire les chevauchements entre périodes historiques | 3–5 | 3–5j | PROPOSED / POSTGRESQL REQUIS | Lead backend + DBA + cadre | préflight + PostgreSQL exclusion/concurrence |
| HOS-BED-002-A | HOS-BED-002 | Corriger le compteur legacy des lits disponibles | 2 | 1j | QA TECHNIQUE VERTE | Lead full-stack + cadre | MockMvc + Angular ciblé + suites complètes |

Ces incréments additifs ne valident pas l'ADR-0002 et n'engagent pas le reste de la phase 0. HOS-BED-002-A réduit GAP-007 ; HOS-BED-001-A/B/C réduisent GAP-005 pour les affectations actives, le rattachement au séjour, la chronologie simple et la cohérence tenant, sans couvrir les chevauchements entre périodes clôturées.

## Definition of Ready globale

- ADR-0002 acceptée ;
- workflow validé par les métiers nommés ;
- contrat API et modèle de données versionnés ;
- stratégie de migration/restauration écrite ;
- permissions et jeux de test identifiés ;
- capacité nominative et reviewer disponibles ;
- critères de performance et observabilité définis.

## Definition of Done globale

- documentation fonctionnelle, technique, API, data, tests et guide à jour ;
- migrations testées sur PostgreSQL et données anonymisées ;
- tests unitaires, intégration, concurrence, E2E et RBAC verts ;
- audit structuré des actions sensibles ;
- dashboards/alertes opérationnels pour les risques introduits ;
- validation médecin/cadre/admissions/RSSI selon story ;
- changelog, VERSION et release note mis à jour au moment de la livraison.

---

## HOS-BED-001 — Garantir l'unicité temporelle des réservations et occupations

### Titre

Empêcher toute double réservation ou occupation d'un lit.

### Contexte

Le claim atomique actuel protège un chemin applicatif, mais `bed_assignments` ne possède ni FK séjour ni contrainte d'unicité active/chevauchement.

### Problème actuel

Une écriture concurrente, un import ou un futur endpoint peut créer plusieurs faits incompatibles. Le statut du lit peut diverger de l'affectation.

### Besoin fonctionnel

La base et le domaine doivent garantir une seule réservation ferme compatible et une seule occupation sur une période.

### Règles métier

1. Les plages d'occupation d'un lit ne se chevauchent jamais.
2. Les réservations fermes qui se chevauchent sont refusées.
3. Une affectation référence un séjour du même tenant.
4. Les retries idempotents retournent le résultat initial.

### Critères d'acceptation

- Étant donné deux admissions concurrentes, lorsque le même lit est claimé, alors une seule réussit et l'autre reçoit 409.
- Étant donné un import avec périodes chevauchantes, lorsque la contrainte est activée, alors les lignes sont mises en quarantaine avant activation, jamais supprimées.
- Étant donné une affectation active, lorsque son séjour appartient à un autre tenant, alors la base refuse l'écriture.

### Cas particuliers

Corrections rétroactives, changement d'heure, réservation d'urgence prioritaire, retry après timeout.

### Permissions

Écriture par use cases admission/bed management uniquement ; aucune mutation SQL/API générique.

### Données et historique

Périodes, statuts, acteur, idempotency key, corrélation, conflits et corrections.

### Scénarios de test

`RES-002`, `BED-002`, `NET-001`, course 10 000 itérations sur PostgreSQL.

### Dépendances

Prototype GiST, Testcontainers PostgreSQL, script de réconciliation.

### Priorité

Critique.

### Estimation

Élevée : 12–14 SP réestimés ; A/B/C représentent 9 SP réalisés sous H2, D reste à arbitrer à 3–5 SP après prototype PostgreSQL ; 9–12j.

---

## HOS-BED-002 — Séparer capacité, disponibilité et remise en état

### Titre

Remplacer le statut unique du lit par des axes cohérents.

### Contexte

`FREE/OCCUPIED/CLEANING/MAINTENANCE` mélange occupation, hygiène et exploitation ; l'UI compte maintenance/nettoyage comme libres.

### Problème actuel

Les responsables ne connaissent ni les lits installés, ni ouverts, ni réellement prêts.

### Besoin fonctionnel

Gérer existence, ouverture, hygiène et usage dérivé, avec turnover après départ.

### Règles métier

1. `FREE` est une projection, jamais une commande.
2. Un lit n'est disponible que s'il est installé, ouvert et prêt.
3. Le départ physique crée une tâche de remise en état.
4. La maintenance ne peut démarrer sur un lit occupé.

### Critères d'acceptation

- Étant donné un lit en nettoyage, lorsque le dashboard calcule les lits libres, alors il n'est pas compté.
- Étant donné un lit occupé, lorsque la maintenance demande sa fermeture, alors la demande est refusée ou planifiée après transfert.
- Étant donné un nettoyage validé, lorsque aucun blocage n'existe, alors le lit devient disponible.

### Cas particuliers

Isolement, désinfection renforcée, lit temporaire, lit non facturable, fermeture planifiée.

### Permissions

Cadre : ouverture clinique ; hygiène : étapes nettoyage ; maintenance : downtime technique ; bed manager : lecture/affectation.

### Données et historique

Événements par axe, raisons, tâches, checklists, acteurs, temps de turnover.

### Scénarios de test

`BED-003`, `TURN-001/002`, `MAINT-001/002`, `KPI-001`.

### Dépendances

HOS-BED-001 et nomenclatures validées.

### Priorité

Critique.

### Estimation

Élevée : 8 SP à découper ; 8–11j.

---

## HOS-RBAC-001 — Contextualiser les permissions hospitalières

### Titre

Séparer les actions médicales, soignantes, administratives, hygiène et maintenance.

### Contexte

`HOSPITALIZATION_MANAGE` autorise admission, notes, soins, sortie, transfert et statut lit à plusieurs profils tenant-wide.

### Problème actuel

La séparation des tâches et le principe du moindre privilège ne sont pas assurés.

### Besoin fonctionnel

Créer des permissions par action, évaluées avec affectation d'unité, relation de soin, délégation et tenant.

### Règles métier

1. Une profession n'accorde aucun droit à elle seule.
2. La décision médicale, la clearance, le mouvement, le nettoyage et la maintenance sont distincts.
3. Une délégation expire automatiquement.
4. Toute décision d'accès sensible est auditée.

### Critères d'acceptation

- Étant donné un infirmier sans délégation, lorsque celui-ci tente une sortie médicale, alors l'API répond 403.
- Étant donné un professionnel affecté temporairement, lorsque la période expire, alors l'accès à l'unité est retiré sans effacer l'historique.
- Étant donné un administrateur sans relation de soin, lorsque le détail clinique est demandé, alors seules les données administratives autorisées sont visibles.

### Cas particuliers

Urgence vitale, garde transverse, petite clinique cumulant plusieurs fonctions, audit légal.

### Permissions

Catalogue à signer par RSSI/DPO et métiers ; politique deny-by-default.

### Données et historique

Rôle, permission, contexte, décision, règle appliquée, motif, délégation.

### Scénarios de test

`SEC-001`, matrices positives/négatives par endpoint et écran.

### Dépendances

HOS-STAFF-001 pour le contexte complet ; garde-fous minimaux livrables avant.

### Priorité

Critique.

### Estimation

Très élevée : 8 SP à découper ; 8–12j.

---

## HOS-ORG-001 — Introduire une organisation hospitalière flexible

### Titre

Modéliser groupes, établissements et unités organisationnelles facultatives.

### Contexte

L'organisation actuelle est un tenant plat et un `Ward` ambigu.

### Problème actuel

Réseaux, pôles, départements, unités et rattachements multiples sont impossibles.

### Besoin fonctionnel

Fournir un arbre d'unités typées sans imposer de profondeur et des relations transverses datées.

### Règles métier

1. Aucun niveau intermédiaire n'est obligatoire.
2. L'arbre ne contient aucun cycle.
3. Les relations et responsables sont datés.
4. Un service fermé conserve tout son historique.

### Critères d'acceptation

- Étant donné une petite clinique, lorsque le service est créé directement sous l'établissement, alors aucune entité factice n'est exigée.
- Étant donné un CHU, lorsque pôle, département, service et unité sont créés, alors la hiérarchie est navigable et historisée.
- Étant donné un service partagé, lorsque deux spécialités sont rattachées, alors elles coexistent sans duplication du service.

### Cas particuliers

Unité fonctionnelle déportée, service transversal, centre de coût distinct, fusion/fermeture.

### Permissions

`ORG_STRUCTURE_READ/WRITE/APPROVE/ARCHIVE`, séparées par établissement.

### Données et historique

Codes, types, parents, relations, spécialités, responsables, statuts et dates.

### Scénarios de test

`ORG-001/002/003/004`, cycles et multi-tenant.

### Dépendances

ADR-0002, catalogue métier, stratégie tenant groupe.

### Priorité

Haute.

### Estimation

Très élevée : 13 SP, tâches schéma (3), API (5), UI (5), migration (5), QA/docs (3) ; 12–16j.

---

## HOS-LOC-001 — Créer le référentiel géographique et les espaces

### Titre

Gérer sites, bâtiments, étages, zones et salles génériques.

### Contexte

`Room` représente uniquement une chambre sous service hébergeant.

### Problème actuel

Consultations, attentes, laboratoires, imagerie, pharmacie, bloc et espaces partagés ne sont pas représentables.

### Besoin fonctionnel

Créer un arbre géographique facultatif et des espaces typés reliés aux unités par affectations datées.

### Règles métier

1. Un espace possède un emplacement unique mais plusieurs unités utilisatrices possibles.
2. Une chambre est une extension d'espace.
3. Les caractéristiques cliniques sont séparées de la catégorie hôtelière.
4. Les changements d'usage sont datés.

### Critères d'acceptation

- Étant donné un cabinet, lorsque sa salle est créée directement, alors site/bâtiment/étage ne sont pas requis.
- Étant donné un plateau partagé, lorsque deux services l'utilisent, alors une seule salle et deux liens datés existent.
- Étant donné une chambre d'isolement, lorsque ses politiques sont configurées, alors le moteur d'affectation les évalue.

### Cas particuliers

Adresse multi-site, salle mobile, espace hors service, morgue, dépôt.

### Permissions

Lecture opérationnelle large ; écriture logistique ; caractéristiques cliniques validées par cadre.

### Données et historique

Arbre, type, capacité, politiques, fermetures, affectations et changements d'usage.

### Scénarios de test

`LOC-001/002/003`, partage et archivage.

### Dépendances

HOS-ORG-001, ADR-0002.

### Priorité

Haute.

### Estimation

Très élevée : 13 SP ; 12–16j.

---

## HOS-ADM-001 — Gérer demandes, préadmissions et réservations

### Titre

Introduire le parcours d'admission avant l'occupation.

### Contexte

Le séjour est actuellement créé directement `EN_COURS` avec un lit.

### Problème actuel

Admissions programmées, décisions, documents, no-show et anticipation de capacité sont absents.

### Besoin fonctionnel

Gérer demande médicale, approbation, préadmission, recherche de compatibilité, réservation expirante et arrivée.

### Règles métier

1. Une réservation possède une expiration.
2. L'urgence peut différer l'administratif avec motif et échéance.
3. Le lit est occupé uniquement à l'arrivée confirmée.
4. L'affectation valide les contraintes patient/espace.

### Critères d'acceptation

- Étant donné une admission planifiée, lorsque le patient arrive avant expiration, alors la réservation est consommée atomiquement.
- Étant donné un no-show, lorsque l'expiration survient, alors le lit redevient disponible et l'événement est tracé.
- Étant donné une chambre incompatible, lorsque l'affectation est tentée, alors l'API explique les incompatibilités sans exposer d'autres patients.

### Cas particuliers

Direct, urgence, identité provisoire, mineur, isolement, accompagnant, paiement différé.

### Permissions

Médecin demande/décide ; admissions complète ; bed manager réserve/affecte.

### Données et historique

Demandes, décisions, checklists, compatibilité, réservation, arrivée, exceptions.

### Scénarios de test

`ADM-001/002/003`, `RES-001/002`, `EMR-001`, `CLIN-001`.

### Dépendances

HOS-BED-001/002, HOS-ORG-001, HOS-LOC-001, permissions minimales.

### Priorité

Haute.

### Estimation

Très élevée : 13 SP ; 12–16j.

---

## HOS-MOV-001 — Tracer la présence et les transferts

### Titre

Connaître la position et la responsabilité du patient à chaque instant.

### Contexte

Le transfert actuel change seulement l'affectation de lit et trois libellés du séjour.

### Problème actuel

Pas de demande, acceptation, transport, handoff, transfert externe ni position hors lit.

### Besoin fonctionnel

Introduire présences et mouvements avec jalons, origine/destination et responsabilité.

### Règles métier

1. Un mouvement possède motif et jalons.
2. La destination accepte avant départ sauf urgence documentée.
3. La responsabilité change au handoff défini.
4. Les événements sont immuables.

### Critères d'acceptation

- Étant donné un transfert accepté, lorsque l'arrivée est confirmée, alors une seule présence est active et l'ancien lit part en turnover.
- Étant donné une perte réseau après départ, lorsque la commande est rejouée, alors aucun mouvement n'est dupliqué.
- Étant donné un transfert externe, lorsque le handoff est clos, alors le destinataire et les documents transmis sont tracés.

### Cas particuliers

Patient en transit, destination perdue, transfert annulé, morgue, bloc/SSPI.

### Permissions

Source demande, destination accepte, transport jalonne, cadre supervise.

### Données et historique

Origine, destination, motifs, responsables, jalons, documents et corrélation.

### Scénarios de test

`MOV-001/002/003`, `ADM-002`, `NET-001`.

### Dépendances

HOS-ADM-001, espaces et affectations personnel.

### Priorité

Haute.

### Estimation

Très élevée : 13 SP ; 10–14j.

---

## HOS-DIS-001 — Séparer les étapes de sortie

### Titre

Découpler décision médicale, clearance administrative, départ physique et lit prêt.

### Contexte

La sortie actuelle ferme immédiatement l'affectation et met le lit à nettoyer.

### Problème actuel

Un patient encore présent peut perdre son lit ; les factures et documents ne participent pas à une readiness consolidée.

### Besoin fonctionnel

Créer un processus de sortie multi-étapes avec issues explicites et turnover.

### Règles métier

1. La sortie médicale ne clôt pas l'occupation.
2. La dette peut être autorisée selon politique, jamais masquée.
3. Le départ physique déclenche le turnover.
4. Chaque issue a ses preuves et permissions.

### Critères d'acceptation

- Étant donné une sortie médicale, lorsque le patient reste dans la chambre, alors le lit reste occupé.
- Étant donné un départ confirmé, lorsque le nettoyage n'est pas validé, alors le lit reste indisponible.
- Étant donné un décès, lorsque l'issue est enregistrée, alors les règles morgue/documents sont déclenchées sans utiliser une sortie normale.

### Cas particuliers

Contre avis, décès, évasion, transfert externe, facture en litige, accompagnant.

### Permissions

Médecin, admissions/caisse, infirmier/cadre et hygiène ont des actions distinctes.

### Données et historique

Décisions, diagnostics, documents, clearance, issue, départ, turnover.

### Scénarios de test

`DIS-001`, `TURN-001/002`, variantes décès/CAM/évasion/transfert.

### Dépendances

HOS-BED-002, HOS-MOV-001, facturation.

### Priorité

Critique.

### Estimation

Élevée : 8 SP ; 8–11j.

---

## HOS-STAFF-001 — Historiser affectations et habilitations du personnel

### Titre

Gérer profession, emplois, services, unités, gardes et délégations.

### Contexte

L'utilisateur actuel possède une organisation et des textes uniques spécialité/département.

### Problème actuel

Impossible de représenter multi-établissement, multi-service, remplacement ou responsabilité datée.

### Besoin fonctionnel

Séparer personne, profil professionnel, emploi, affectation, habilitation, planning, délégation et compte.

### Règles métier

1. Une affectation a une période et un statut.
2. Le retrait clôt la période sans supprimer l'historique.
3. Une spécialité vérifiée a une source et une validité.
4. Les rôles applicatifs restent séparés.

### Critères d'acceptation

- Étant donné un médecin dans deux établissements, lorsque ses affectations sont consultées, alors une identité et deux emplois datés existent.
- Étant donné un remplacement expiré, lorsque l'accès est retesté, alors les droits temporaires ont disparu.
- Étant donné un responsable médical et administratif différents, lorsque la fiche service s'affiche, alors les responsabilités sont correctement datées.

### Cas particuliers

Cumul de fonctions, vacataire, suspension d'exercice, garde transverse, délégation urgente.

### Permissions

RH gère l'emploi ; cadre l'affectation ; direction la responsabilité ; RSSI le compte/rôle.

### Données et historique

Identité, qualifications, vérifications, périodes, décideurs, changements.

### Scénarios de test

`SEC-001`, chevauchements, révocation et délégation.

### Dépendances

HOS-ORG-001 ; alimente HOS-RBAC-001.

### Priorité

Haute.

### Estimation

Très élevée : 13 SP ; 10–14j.

---

## HOS-RES-001 — Planifier espaces, ressources et équipements partagés

### Titre

Gérer la disponibilité des salles et équipements médicaux.

### Contexte

Aucun équipement, plateau ou ressource partagée n'est modélisé ; le bloc n'a que des comptes rendus.

### Problème actuel

Conflits, pannes, maintenance et taux d'utilisation sont invisibles.

### Besoin fonctionnel

Créer ressources, emplacements datés, réservations, downtimes et ordres de maintenance.

### Règles métier

1. Une ressource non partageable n'a pas de réservations fermes chevauchantes.
2. Une panne alerte les réservations impactées.
3. Une remise en service exige les validations configurées.
4. Le bloc/SSPI réutilise ce socle.

### Critères d'acceptation

- Étant donné deux services, lorsque la même salle est réservée à la même heure, alors une seule confirmation est possible.
- Étant donné un équipement en panne, lorsque sa période commence, alors il n'est plus proposé et les réservations sont alertées.
- Étant donné une opération, lorsque salle, équipe ou équipement critique manque, alors le démarrage est bloqué selon politique.

### Cas particuliers

Priorité urgence, équipement mobile, maintenance préventive, remplacement équivalent.

### Permissions

Planificateur réserve ; biomédical maintient ; cadre arbitre ; clinique voit la disponibilité utile.

### Données et historique

Inventaire, emplacement, réservation, panne, maintenance, validations.

### Scénarios de test

`RESRC-001`, `MAINT-001/002`, parcours bloc/SSPI.

### Dépendances

HOS-LOC-001, HOS-STAFF-001 pour équipes.

### Priorité

Haute.

### Estimation

Très élevée : 13 SP ; 10–14j.

---

## HOS-PATH-001 — Orchestrer le parcours patient transverse

### Titre

Afficher position, responsable, prochaine étape et actions en attente.

### Contexte

Visite, urgence, labo, pharmacie, facturation et séjour ont des statuts séparés.

### Problème actuel

Les utilisateurs doivent reconstruire le parcours et les handoffs sont fragiles.

### Besoin fonctionnel

Introduire épisode, encounter, présence et work-items, sans déplacer la logique métier critique vers le frontend.

### Règles métier

1. Chaque work-item a propriétaire, échéance, statut et idempotency key.
2. Les modules publient des événements ; l'orchestrateur ne réécrit pas leurs faits.
3. Les détails visibles dépendent du profil.
4. Un handoff reste ouvert jusqu'à acceptation/arrivée.

### Critères d'acceptation

- Étant donné un patient avec laboratoire en attente, lorsque le parcours est ouvert, alors position, responsable et prélèvement attendu sont visibles aux bons profils.
- Étant donné une urgence orientée vers admission, lorsque aucun aval n'accepte, alors l'urgence reste marquée « attente d'aval ».
- Étant donné une tâche rejouée après coupure, lorsque la même clé est reçue, alors elle n'est pas dupliquée.

### Cas particuliers

Étapes parallèles, annulation, retour en arrière, résultat corrigé, transfert externe.

### Permissions

Vue filtrée par fonction : accueil administratif, clinique complète selon relation de soin, direction agrégée.

### Données et historique

Épisodes, présences, responsabilités, dépendances, SLA, outcomes et corrélations.

### Scénarios de test

`PATH-001`, `ADM-002`, `NET-001`, tests de masquage.

### Dépendances

HOS-ORG/LOC/STAFF/MOV et contrats d'événements des modules.

### Priorité

Haute.

### Estimation

Très élevée : 13 SP ; 10–14j.

---

## HOS-KPI-001 — Piloter la capacité et la saturation

### Titre

Fournir des indicateurs réconciliables de capacité.

### Contexte

Le tableau actuel ne connaît que total et occupé et calcule mal les libres.

### Problème actuel

La direction ne peut distinguer capacité installée, ouverte, prête ou réservée.

### Besoin fonctionnel

Créer projections, tableaux, alertes et définitions versionnées.

### Règles métier

1. Chaque KPI possède formule, source, fuseau et version.
2. Les dénominateurs historiques utilisent les périodes d'ouverture.
3. Les agrégats direction minimisent les données patient.
4. Une donnée incohérente est signalée, pas silencieusement ignorée.

### Critères d'acceptation

- Étant donné le jeu `KPI-001`, lorsque le dashboard s'affiche, alors chaque compteur se réconcilie aux sources.
- Étant donné un seuil dépassé, lorsque l'événement est projeté, alors l'alerte est créée et acquittable.
- Étant donné une correction tardive, lorsque le rapport est recalculé, alors la version et la date de recalcul sont visibles.

### Cas particuliers

Fermeture partielle, timezone, événement retardé, réseau multi-établissements.

### Permissions

Direction agrégée ; cadre unité ; bed manager temps réel ; export contrôlé.

### Données et historique

Snapshots, définitions, seuils, alertes, acquittements, corrections.

### Scénarios de test

`CAP-001`, `KPI-001`, charge et réconciliation SQL.

### Dépendances

HOS-BED-002, HOS-MOV-001 et modèles fiables.

### Priorité

Haute.

### Estimation

Élevée : 8 SP ; 7–10j.

---

## HOS-INT-001 — Interopérabilité et continuité en réseau instable

### Titre

Versionner les contrats et rendre les commandes critiques rejouables.

### Contexte

Le contexte africain peut connaître une connectivité variable ; les nouvelles entités doivent aussi être échangeables.

### Problème actuel

Pas de stratégie hospitalière de mode dégradé ni d'identifiants structurés pour organisation/localisation/encounter.

### Besoin fonctionnel

Mapper les référentiels aux standards d'échange pertinents, versionner les API, rendre les commandes critiques idempotentes et prévoir une file locale limitée.

### Règles métier

1. Aucune donnée clinique critique n'est confiée au cache navigateur sans chiffrement/politique validée.
2. Admission, réservation et mouvement sont idempotents.
3. Les conflits sont résolus par le backend maître.
4. Le mode dégradé expose clairement la fraîcheur des données.

### Critères d'acceptation

- Étant donné une coupure après commit, lorsque la commande est rejouée, alors le même résultat est retourné.
- Étant donné une donnée d'occupation ancienne, lorsque l'UI la montre, alors son âge et son caractère non confirmable sont visibles.
- Étant donné une exportation, lorsque Location/Organization/Encounter sont produits, alors les identifiants et historiques restent stables.

### Cas particuliers

Conflit offline, horloge appareil fausse, établissement sans connexion prolongée, transfert inter-établissements.

### Permissions

APIs à scopes minimaux ; synchronisation et exports audités.

### Données et historique

Versions de contrat, idempotency keys, offsets, conflits, décisions de reprise.

### Scénarios de test

`NET-001`, contract tests, chaos réseau, reprise après redémarrage.

### Dépendances

Contrats des stories précédentes, validation DPO/RSSI et exploitation.

### Priorité

Haute pour l'idempotence, moyenne pour l'ensemble des mappings.

### Estimation

Élevée : 8 SP ; 7–10j.

## Ordre de réalisation conseillé

1. HOS-BED-001, correction P0 du compteur de lits et garde-fous HOS-RBAC-001.
2. ADR-0002 puis HOS-ORG-001 et HOS-LOC-001.
3. HOS-BED-002 et migration des états.
4. HOS-ADM-001, HOS-MOV-001, HOS-DIS-001.
5. HOS-STAFF-001 puis contextualisation RBAC complète.
6. HOS-RES-001, avec sous-epics bloc/labo/imagerie/pharmacie si nécessaire.
7. HOS-PATH-001, HOS-KPI-001 et HOS-INT-001.

## Capacité et engagement

Le sprint courant est déjà engagé et aucune capacité nominative additionnelle n'est fournie. EPIC-0027 reste donc hors sprint. À 16–20 jours-personnes planifiables par sprint, l'ordre de grandeur est 6–9 sprints, sous réserve des validations métier et de la migration. Une phase 0 de 12–16 jours-personnes doit être arbitrée séparément comme réduction de risque critique.
