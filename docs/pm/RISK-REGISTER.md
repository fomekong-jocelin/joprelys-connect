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
