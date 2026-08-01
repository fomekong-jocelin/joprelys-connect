# RISK REGISTER — Registre des risques

| ID | Risque | Probabilité | Impact | Niveau | Propriétaire | Mitigation | Statut | Dernière MAJ |
|---|---|---|---|---|---|---|---|---|
| RISK-001 | Tickets macro non découpés | Moyen | Fort | Élevé | Chef de projet | Activer PM skill et découper | Ouvert | 2026-07-01 |
| RISK-002 | Absence de tests | Moyen | Fort | Élevé | Tech Lead | DoD obligatoire | Ouvert | 2026-07-01 |
| RISK-003 | États facture/créance contradictoires | Fort | Fort | Critique | Lead Developer + DAF | Synchroniser les règlements, exposer un état ventilé patient/assurance et couvrir le parcours E2E | Ouvert | 2026-07-09 |
| RISK-004 | Poste hospitalisation monolithique et règles cliniques non formalisées | Fort | Fort | Critique | Médecin Chef + Cadre infirmier + Lead Developer | Valider les parcours par rôle, découper le composant et couvrir les transitions critiques par tests E2E/RBAC | Ouvert | 2026-07-10 |
| RISK-005 | Workspace facturation/caisse trop chargé et états financiers ambigus | Fort | Fort | Critique | Lead Developer + DAF + Product Design | Livrer EPIC-0020 par étapes : contrat d'état, workspace factures, caisse, assurance, puis QA E2E | Ouvert | 2026-07-10 |
| RISK-006 | Données de santé transmises à un fournisseur IA sans validation de sous-traitance, résidence et rétention | Moyen | Fort | Critique | DPO + RSSI + Tech Lead | Maintenir la feature désactivée ; valider fournisseur/contrat/rétention/résidence ; ne conserver ni audio ni contenu clinique dans les logs | Ouvert | 2026-07-17 |
| RISK-007 | Assistant vocal ajouté à des écrans Angular déjà monolithiques | Fort | Moyen | Élevé | Lead Frontend | Découper consultation et dashboard avant le panneau IA ; respecter la limite 500 lignes et couvrir light/dark, FR/EN et accessibilité | Ouvert | 2026-07-17 |
| RISK-008 | Autorisations incohérentes entre rôles, permissions, menus, routes et APIs | Faible | Fort | Élevé | RSSI + Tech Lead | Politiques exactes centralisées, inférences supprimées, catalogue médecin corrigé, 720 tests automatisés verts et garde statique ; signer la matrice et la recette E2E multi-rôles | En surveillance | 2026-07-18 |
| RISK-009 | Lit, affectation et hospitalisation peuvent être désynchronisés ; le calcul UI historique comptait nettoyage/maintenance comme libres | Fort | Fort | Critique | Cadre infirmier + Responsable hospitalisation + Tech Lead | Compteur corrigé par HOS-BED-002-A ; V76–V78 interdisent sous H2 les doublons actifs, orphelins, périodes inversées et incohérences tenant affectation/séjour/lit ; restent validation PostgreSQL, commande de statut générique, chevauchements clôturés et sortie physique | En réduction | 2026-07-21 |
| RISK-010 | Sortie médicale et départ physique confondus, entraînant une libération prématurée du lit | Fort | Fort | Critique | Médecin chef + Admissions + Cadre infirmier | Séparer décision médicale, clearance administrative, départ physique et turnover ; procédure manuelle transitoire | Ouvert | 2026-07-21 |
| RISK-011 | Modèle `Organization → Ward → Room → Bed` insuffisant pour multi-site, unités et espaces partagés | Fort | Fort | Critique | Product Owner + Architecte + DBA | Valider ADR-0002, migration progressive API v2 et réconciliation sans suppression ; ne pas étendre le legacy par champs ad hoc | Ouvert | 2026-07-21 |
| RISK-012 | Branche mobile cumulative non reviewable : IDs de tickets réutilisés, migration du diagnostic unique non exécutée en PostgreSQL, gates cross-stack absents et composants au-dessus de 500 lignes | Fort | Fort | Critique | Product Owner + Tech Lead Flutter + Lead Backend | Maintenir le gel ; reviewer V109/ADR-0005, réaligner le backlog, découper les composants en dette et obtenir Maven + CI APK + recette Android exact-HEAD | En réduction | 2026-08-01 |

## Niveaux

| Probabilité | Signification |
|---|---|
| Faible | Peu probable |
| Moyen | Possible |
| Fort | Probable ou déjà observé |

| Impact | Signification |
|---|---|
| Faible | Peu d'effet sur livraison |
| Moyen | Retard ou dette modérée |
| Fort | Blocage, régression ou incident |
