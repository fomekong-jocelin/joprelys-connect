# Audit de conformité et de complétude — Cahier des charges Joprelys Connect V3.1

> **Référence auditée :** `JOPRELYS_CONNECT_CAHIER_DES_CHARGES_V3_1(1).docx` — version 3.1 du 11/07/2026  
> **Dépôt inspecté :** `fomekong-jocelin/joprelys-connect` — branche `main` — commit `817d971c` — version applicative `0.10.1`  
> **Date de l'audit :** 11/07/2026  
> **Statut :** audit initial de livraison, à transformer en backlog exécuté et preuves de recette.

## 1. Conclusion exécutive

Le dépôt contient un socle fonctionnel important et plusieurs modules avancés, mais **aucun des 43 modules ne peut encore être déclaré livré au sens strict de la V3.1** sans compléter les écarts fonctionnels, les exigences transverses et les preuves de recette.

- **43 modules** analysés, couvrant **197 exigences testables** : 16 P0, 17 P1 et 10 P2.
- **3 modules P0 présentent un écart bloquant immédiat** : `FND-02`, `PAT-01` et `CLN-09`.
- **19 modules sont avancés mais incomplets**, **11 partiels**, et **10 non démontrés** par des preuves de code, tests ou documentation suffisantes.
- Le principal chemin critique est le parcours **patient inconscient/non identifié** : le modèle patient impose des données d'identité obligatoires et l'API d'urgence exige un `patientId` existant.
- La sécurité d'authentification n'est pas industrialisée : OTP et révocation JWT en mémoire, OTP potentiellement exposé au frontend et écrit en console, sans OIDC/OAuth2, refresh token rotatif ni gestion distribuée des sessions.
- Le suivi projet est obsolète par rapport à `main` : il référence encore des stories financières en cours alors que certaines sont fusionnées, et ne reflète pas le RBAC livré.

### Décision de livraison

La livraison doit être organisée **par module et par preuve**, selon l'ordre suivant :

1. remettre à niveau le socle sécurité, tenant, référentiels et audit ;
2. livrer le parcours urgence inconscient/identité provisoire de bout en bout ;
3. compléter le parcours ambulatoire P0 et ses documents ;
4. compléter les domaines hospitaliers et Connect P1 ;
5. construire ou intégrer les modules P2 Back Office ;
6. exécuter les campagnes NFR, sécurité, PRA et UAT avant toute déclaration de livraison.

## 2. Méthode et niveaux de preuve

L'audit croise le cahier des charges avec :

- le code Spring Boot et Angular présent sur `main` ;
- les migrations Flyway ;
- les contrôleurs, services, modèles et tests identifiés ;
- `docs/ai/CHANGELOG.md`, `docs/ai/PROJECT-TRACKING.md` et les tickets existants ;
- l'historique des commits et des PR.

Les statuts signifient :

| Statut | Signification |
|---|---|
| AVANCÉ — INCOMPLET | Couverture fonctionnelle forte, mais critères V3.1 ou preuves de sortie manquants |
| PARTIEL AVANCÉ | Plusieurs flux opérationnels existent, sans couverture exhaustive du module |
| PARTIEL | Socle ou sous-flux présent, écarts structurants persistants |
| PARTIEL FAIBLE | Quelques éléments connexes existent, sans module cohérent livrable |
| NON DÉMONTRÉ | Aucune preuve probante retrouvée dans les sources inspectées ; confirmation finale par inventaire du code requise |
| BLOQUANT | Écart empêchant un parcours P0 ou une mise en production sûre |

## 3. Écarts confirmés directement dans le code

### 3.1 Identité provisoire et urgence

- `PatientEntity` rend obligatoires `full_name`, `gender`, `birth_date` et `city`, et initialise tout patient avec le statut `ACTIVE`.
- `CreateEmergencyRequest` impose un `patientId` non nul.
- `EmergencyEntity` impose une relation patient non nulle.
- Le dossier d'urgence ne contient pas les objets V3.1 nécessaires : identité provisoire, déclarations d'identité, tiers/accompagnant qualifié, incapacité à consentir, exception d'urgence, effets personnels, chaîne médico-légale et régularisation.

**Conséquence :** l'application ne peut pas prendre en charge conformément à la V3.1 un patient inconscient ou inconnu sans créer artificiellement une identité complète.

### 3.2 Authentification et sessions

- Authentification locale email/mot de passe et JWT uniquement ; aucun fournisseur OIDC/OAuth2 n'est démontré.
- OTP stockés dans une `ConcurrentHashMap`, imprimés en console et exposables au frontend par défaut.
- Révocation JWT stockée uniquement en mémoire, donc non partagée entre instances et perdue au redémarrage.
- Aucun modèle persistant de sessions, refresh tokens rotatifs, facteurs MFA ou fermeture distante n'est démontré.

### 3.3 RBAC

- La PR #24 a apporté un RBAC tenanté avancé : rôles, permissions, rôles personnalisés, multi-rôles, audit et protections.
- La V3.1 demande cependant une décision d'accès incluant rôle, tenant, service, relation de soins, consentement et durée. Le modèle actuel reste principalement RBAC + tenant.
- Les délégations temporaires et la revue périodique des accès ne sont pas présentes dans la migration V57.

## 4. Matrice module par module

| Module | Prio. CDC | Statut dépôt | Preuves principales | Manquements avant livraison | Exigences |
|---|---:|---|---|---|---|
| **FND-01 — Établissements, spécialités et configuration multi-tenant** | P0 | **PARTIEL AVANCÉ** | Organizations, activation/suspension, tenant context et clés API sont implémentés. | Services/sites/unités configurables, organization_settings versionnés, numérotation/modèles locaux et membership multi-établissements ne sont pas démontrés de bout en bout. | FR-FND-001, FR-FND-002, FR-FND-003, FR-FND-004 |
| **FND-02 — Identité, authentification et sessions** | P0 | **PARTIEL — BLOQUANT LIVRAISON** | Authentification locale JWT/BCrypt, OTP pour certains rôles, logout et audit de connexion existent. | OIDC/OAuth2, facteurs MFA persistants, verrouillage configurable, refresh tokens rotatifs, catalogue de sessions et révocation distribuée de toutes les sessions manquent. L’OTP peut être exposé au frontend et est imprimé en console; OTP et blacklist sont en mémoire. | FR-AUTH-001, FR-AUTH-002, FR-AUTH-003, FR-AUTH-004 |
| **FND-03 — Rôles, permissions et délégations** | P0 | **AVANCÉ — INCOMPLET** | RBAC administrable fusionné: rôles système/personnalisés, permissions, multi-rôles, audit, protections auto-élévation/dernier admin. | ABAC par service/relation de soins/consentement, délégations temporaires, double validation indépendante de l’élévation et revue périodique des accès ne sont pas couverts complètement. | FR-RBAC-001, FR-RBAC-002, FR-RBAC-003, FR-RBAC-004 |
| **FND-04 — Nomenclatures, tarifs et référentiels** | P0 | **PARTIEL** | Tarifs, conventions, catalogue médicaments et examens existent dans plusieurs domaines. | Référentiel générique versionné, publication avec dates d’effet, imports contrôlés, provenance/propriétaire et surcharge locale formalisée ne sont pas démontrés. | FR-REF-001, FR-REF-002, FR-REF-003, FR-REF-004 |
| **PAT-01 — Identité patient unique et dédoublonnage** | P0 | **PARTIEL — BLOQUANT URGENCES** | DPU, recherche patient, détection de doublons et rapprochement assisté existent. | Le modèle impose encore nom, sexe, date de naissance et ville; aucun profil URG-TEMP complet, source/confiance de chaque identité, aliases et workflow de fusion audité conforme V3.1 ne sont disponibles. | FR-PAT-001, FR-PAT-002, FR-PAT-003, FR-PAT-004, FR-PAT-005, FR-PAT-006, FR-PAT-007 |
| **PAT-02 — Portail patient, profil et représentants** | P1 | **PARTIEL AVANCÉ** | Portail patient, profil, documents, confidentialité, consentements et historique d’accès existent. | Représentants légaux avec preuves, période de validité, révocation et règles mineur/majeur protégé ne sont pas démontrés complètement. | FR-PORTAL-001, FR-PORTAL-002, FR-PORTAL-003, FR-PORTAL-004 |
| **CLN-01 — Accueil, rendez-vous, registre et visite** | P0 | **PARTIEL** | Ouverture/clôture de visite, file active, registre d’accueil et pré-enregistrement sont présents. | Rendez-vous complet, registre d’arrivée urgent indépendant du patient définitif, tiers accompagnant/transporteur et régularisation administrative différée restent à compléter. | FR-REC-001, FR-REC-002, FR-REC-003, FR-REC-004, FR-REC-005, FR-REC-006, FR-REC-007 |
| **CLN-02 — Triage, constantes et alertes cliniques** | P0 | **AVANCÉ — INCOMPLET** | Constantes vitales, IMC, douleur et tableau de triage urgence sont présents. | ABCDE structuré, score/algorithme de priorité versionné, alertes critiques acquittables, réévaluation horodatée et justification du surclassement/déclassement ne sont pas démontrés. | FR-TRI-001, FR-TRI-002, FR-TRI-003, FR-TRI-004 |
| **CLN-03 — Consultation, diagnostics et décisions** | P0 | **AVANCÉ — INCOMPLET** | Consultation, examen clinique, diagnostics, conclusion, conseils et corrections tracées existent. | Problèmes actifs structurés, diagnostics codifiés/versionnés, signature clinique forte et réévaluation obligatoire après résultat doivent être consolidés. | FR-CONS-001, FR-CONS-002, FR-CONS-003, FR-CONS-004 |
| **CLN-04 — Allergies, antécédents et traitements chroniques** | P0 | **AVANCÉ — INCOMPLET** | Allergies, antécédents, vaccinations, importance et soft delete audité existent. | Traitements chroniques structurés, réaction/sévérité/source de vérification, interaction avec prescriptions et réconciliation médicamenteuse ne sont pas démontrés. | FR-HIST-001, FR-HIST-002, FR-HIST-003, FR-HIST-004 |
| **CLN-05 — Prescriptions et ordonnances** | P0 | **AVANCÉ — INCOMPLET** | Ordonnances structurées, cycle DRAFT/ACTIVE/CANCELLED, PDF/QR/PIN, dispensation et stocks existent. | Signature forte, renouvellements, interactions/contre-indications, règles de substitution versionnées et réconciliation avec traitements chroniques restent à finaliser. | FR-PRES-001, FR-PRES-002, FR-PRES-003, FR-PRES-004 |
| **CLN-06 — Demandes d'examens et actes médico-techniques** | P0 | **PARTIEL** | Demandes d’analyses biologiques, numérotation, statuts et résultats sont implémentés. | Catalogue unifié de tous actes médico-techniques, prélèvement/chaîne de possession, autorisation financière non bloquante en urgence et boucle résultat-réévaluation obligatoire ne sont pas complets. | FR-EXAM-001, FR-EXAM-002, FR-EXAM-003, FR-EXAM-004 |
| **CLN-07 — Laboratoire, résultats et valeurs critiques** | P1 | **PARTIEL AVANCÉ** | Portail laboratoire, résultats structurés/PDF, import externe et historique sont présents. | Validation biologique multi-niveaux, contrôle qualité, valeurs de référence versionnées, notification critique avec accusé de réception et correction par nouvelle version restent à compléter. | FR-LAB-001, FR-LAB-002, FR-LAB-003, FR-LAB-004 |
| **CLN-08 — Imagerie et DICOMweb** | P2 | **NON DÉMONTRÉ** | Aucun module DICOM/DICOMweb/PACS probant n’a été retrouvé; seuls uploads d’images génériques apparaissent. | Demande d’imagerie, worklist, compte rendu radiologue, lien étude PACS, viewer DICOMweb et traçabilité d’accès sont à construire. | FR-IMG-001, FR-IMG-002, FR-IMG-003, FR-IMG-004 |
| **CLN-09 — Urgences, patient inconscient et réanimation** | P0 | **PARTIEL — BLOQUANT URGENCES** | Dossier d’urgence, triage, soins de réanimation horodatés, stabilisation/orientation et historique patient existent. | La création exige patientId; identité provisoire, incapacité/exception d’urgence, tiers qualifié, effets personnels, chaîne médico-légale, recherche d’identité, reprise de conscience et régularisation ne sont pas implémentés. | FR-URG-001, FR-URG-002, FR-URG-003, FR-URG-004, FR-URG-005, FR-URG-006, FR-URG-007, FR-URG-008, FR-URG-009, FR-URG-010, FR-URG-011 |
| **HOS-01 — Admission, hospitalisation et sortie** | P1 | **PARTIEL AVANCÉ** | Admission, séjour, notes, documents entrée/sortie, sortie contre avis, lit et facturation sont présents. | Admission depuis URG-TEMP, sortie médicale/administrative séparée, transfert externe formalisé, décès et boucle examens-réévaluation complète restent à consolider. | FR-HOSP-001, FR-HOSP-002, FR-HOSP-003, FR-HOSP-004, FR-HOSP-005, FR-HOSP-006 |
| **HOS-02 — Services, chambres et lits** | P1 | **AVANCÉ — INCOMPLET** | Services/chambres/lits, occupation, transfert, nettoyage et verrouillage optimiste sont présents. | Réservation, indisponibilité planifiée, historique complet d’affectation, nettoyage validé par rôle et KPI d’occupation doivent être achevés. | FR-BED-001, FR-BED-002, FR-BED-003, FR-BED-004 |
| **HOS-03 — Bloc opératoire et compte rendu opératoire** | P1 | **PARTIEL AVANCÉ** | CRO, implants, coefficients K et validation immuable existent. | Planning bloc, conflits de salle/équipe, checklists pré/per/post-op, incidents, participants horodatés et annulation/report complet ne sont pas démontrés. | FR-OR-001, FR-OR-002, FR-OR-003, FR-OR-004 |
| **HOS-04 — Anesthésie et consentements opératoires** | P1 | **PARTIEL** | Consentements chirurgie/anesthésie rattachés au séjour existent. | Consultation pré-anesthésique structurée, classification du risque, protocole anesthésique, incidents, feuille de réveil et critères de sortie SSPI sont à construire. | FR-ANES-001, FR-ANES-002, FR-ANES-003, FR-ANES-004 |
| **HOS-05 — Soins journaliers, administrations et suivi post-opératoire** | P1 | **PARTIEL AVANCÉ** | Soins journaliers, administrations médicamenteuses, consommables et transmissions sont présents. | Plan de soins ordonné, administrations planifiées/non réalisées avec motif, surveillance post-op structurée, alertes et feuille vérifiable complète restent à finaliser. | FR-CARE-001, FR-CARE-002, FR-CARE-003, FR-CARE-004 |
| **HOS-06 — Kinésithérapie et rééducation** | P2 | **NON DÉMONTRÉ** | Aucune preuve de workflow de kinésithérapie complet n’a été retrouvée. | Prescription, objectifs, plan de séances, bilans, présence, progression, clôture et documents sont à construire. | FR-PHYSIO-001, FR-PHYSIO-002, FR-PHYSIO-003, FR-PHYSIO-004 |
| **HOS-07 — Maternité et obstétrique** | P2 | **NON DÉMONTRÉ** | Aucune preuve de module maternité/obstétrique complet n’a été retrouvée. | Suivi grossesse, partogramme, accouchement, nouveau-né, suites de couches, alertes et documents sont à construire. | FR-OBST-001, FR-OBST-002, FR-OBST-003, FR-OBST-004 |
| **HOS-08 — Décès, morgue et remise du corps** | P1 | **NON DÉMONTRÉ** | Aucune preuve de circuit décès/morgue/remise du corps n’a été retrouvée. | Constat, certificats, identité, scellés/effets, transfert morgue, autorisation de remise, registre et audit sont à construire. | FR-DEATH-001, FR-DEATH-002, FR-DEATH-003, FR-DEATH-004 |
| **HOS-09 — Garde, passation et staff** | P1 | **NON DÉMONTRÉ** | Aucune preuve de registre de garde et passation opérationnelle n’a été retrouvée. | Ouverture/fermeture de garde, patients à surveiller, incidents, matériel critique, passation acceptée et rapport signé sont à construire. | FR-SHIFT-001, FR-SHIFT-002, FR-SHIFT-003, FR-SHIFT-004 |
| **DOC-01 — Documents, GED, signatures et vérification** | P0 | **AVANCÉ — INCOMPLET** | PDF, QR, hash, versionnement, révocation/annulation, recherche publique et GED de certains documents sont présents. | Stockage objet S3/MinIO chiffré, antivirus, signatures fortes, politiques de conservation, modèles versionnés et documents urgence V3.1 (fiche urgence, incapacité, tiers, effets, réanimation) manquent. | FR-DOC-001, FR-DOC-002, FR-DOC-003, FR-DOC-004, FR-DOC-005, FR-DOC-006, FR-DOC-007 |
| **CON-01 — DPU partagé et synthèse longitudinale** | P1 | **PARTIEL** | Synthèse patient et historique longitudinal existent au sein de la solution. | Agrégation multi-établissements avec provenance, niveau de confiance, sensibilité, publication et correction sans écrasement n’est pas complète. | FR-DPU-001, FR-DPU-002, FR-DPU-003, FR-DPU-004 |
| **CON-02 — Consentements et préférences de partage** | P1 | **PARTIEL AVANCÉ** | Consentements de partage et scopes existent. | Représentant vérifié, incapacité, base d’accès d’urgence, finalités granulaires, préférences, retrait prospectif et preuve versionnée doivent être complétés. | FR-CONSENT-001, FR-CONSENT-002, FR-CONSENT-003, FR-CONSENT-004, FR-CONSENT-005, FR-CONSENT-006, FR-CONSENT-007 |
| **CON-03 — Accès externe, temporaire et urgence** | P1 | **PARTIEL AVANCÉ** | Demande d’accès externe, validation patient, scopes et break-glass existent. | Durée/périmètre stricts, justification d’urgence, notifications, expiration/révocation systématique et revue post-accès doivent être vérifiés et complétés. | FR-ACCESS-001, FR-ACCESS-002, FR-ACCESS-003, FR-ACCESS-004 |
| **CON-04 — Audit et investigation** | P0 | **PARTIEL AVANCÉ** | Journaux d’audit backend, vues frontend et audit RBAC existent. | Journal append-only/anti-altération, corrélation, recherche d’investigation, export probatoire, alertes d’anomalie et politique de conservation ne sont pas démontrés. | FR-AUDIT-001, FR-AUDIT-002, FR-AUDIT-003, FR-AUDIT-004 |
| **CON-05 — Notifications et communications** | P1 | **PARTIEL FAIBLE** | Des notifications applicatives et badges sont mentionnés dans l’existant. | Canaux email/SMS/push, préférences, templates versionnés, retries, dead-letter, escalade critique et preuve de remise ne sont pas démontrés. | FR-NOTIF-001, FR-NOTIF-002, FR-NOTIF-003, FR-NOTIF-004 |
| **CON-06 — API, webhooks et partenaires** | P1 | **PARTIEL** | Clés API tenantées, API REST et quelques intégrations externes existent. | Versionnement explicite /api/v1, scopes contractuels, quotas, webhooks signés/rejouables, idempotence, portail développeur et gouvernance OpenAPI ne sont pas complets. | FR-API-001, FR-API-002, FR-API-003, FR-API-004 |
| **CON-07 — Interopérabilité FHIR** | P2 | **PARTIEL FAIBLE** | Un import FHIR DiagnosticReport simplifié existe. | CapabilityStatement, profils, mappings Patient/Encounter/Observation/Condition/MedicationRequest/Consent/DocumentReference, validation et versionnement FHIR R4 sont à construire. | FR-FHIR-001, FR-FHIR-002, FR-FHIR-003, FR-FHIR-004 |
| **FIN-01 — Catalogue, devis et facturation** | P0 | **AVANCÉ — INCOMPLET** | Devis, factures, remises, avoirs, créances, tiers-payant, coefficients K et PDF existent. | Catalogue tarifaire versionné générique, règles de date d’effet, validation métier finale et transfert fiable de créance lors du rapprochement URG-TEMP restent à compléter. | FR-BILL-001, FR-BILL-002, FR-BILL-003, FR-BILL-004, FR-BILL-005, FR-BILL-006, FR-BILL-007 |
| **FIN-02 — Caisse, paiements, clôture et remboursement** | P0 | **AVANCÉ — INCOMPLET** | Caisses, sessions, mouvements, reçus, encaissements, clôture, écarts et versements banque existent. | Remboursement complet avec double validation, annulation/contrepassation généralisée, indisponibilité réseau/idempotence et régularisation d’une urgence sans paiement initial doivent être complétés. | FR-CASH-001, FR-CASH-002, FR-CASH-003, FR-CASH-004, FR-CASH-005 |
| **FIN-03 — Assurances, conventions et tiers payant** | P1 | **AVANCÉ — INCOMPLET** | Conventions, ventilation patient/assurance, bordereaux, envoi, règlement et recouvrement existent. | Éligibilité/plafonds/exclusions versionnés, accords préalables, rejets/retours assureur, rapprochement détaillé et intégration contractuelle doivent être finalisés. | FR-INS-001, FR-INS-002, FR-INS-003, FR-INS-004 |
| **OPS-01 — Achats et fournisseurs** | P2 | **NON DÉMONTRÉ** | Aucune preuve de workflow achats/fournisseurs complet n’a été retrouvée. | Demande d’achat, approbation, consultation fournisseurs, comparatif, commande, réception, litige et rapprochement sont à construire. | FR-PROC-001, FR-PROC-002, FR-PROC-003, FR-PROC-004 |
| **OPS-02 — Stocks, pharmacie et traçabilité des lots** | P1 | **PARTIEL** | Stocks médicaments, décrémentation, alertes, consommables et implants sont présents. | Lots, péremption, FEFO, inventaires, ajustements à double validation, rappels de lot, transferts et traçabilité complète ne sont pas démontrés. | FR-STOCK-001, FR-STOCK-002, FR-STOCK-003, FR-STOCK-004 |
| **OPS-03 — Immobilisations et maintenance** | P2 | **NON DÉMONTRÉ** | Aucune preuve d’immobilisations et maintenance métier complète n’a été retrouvée. | Registre actifs, codification, affectation, amortissement, maintenance préventive/corrective, indisponibilité et sortie sont à construire. | FR-ASSET-001, FR-ASSET-002, FR-ASSET-003, FR-ASSET-004 |
| **FIN-04 — Comptabilité générale OHADA** | P2 | **PARTIEL FAIBLE** | Export Sage 100 avec schéma d’écritures OHADA et résolution d’écarts existe. | Plan comptable administrable, journaux/périodes, écritures équilibrées immuables, lettrage, clôture, contrepassation, balance, bilan et compte de résultat ne sont pas implémentés comme comptabilité générale. | FR-ACC-001, FR-ACC-002, FR-ACC-003, FR-ACC-004 |
| **FIN-05 — Budget et comptabilité analytique** | P2 | **NON DÉMONTRÉ** | Aucune preuve de budget/analytique complet n’a été retrouvée. | Axes analytiques, budgets, engagements, disponibilité, virements, révisions et comparatifs réel/budget sont à construire. | FR-BUD-001, FR-BUD-002, FR-BUD-003, FR-BUD-004 |
| **HR-01 — Ressources humaines, planning et paie** | P2 | **NON DÉMONTRÉ** | La gestion des comptes du personnel existe, mais pas un SIRH/paie. | Dossiers RH, contrats, planning, présence, congés, éléments variables, paie, confidentialité RH et validations réglementaires sont à construire ou à intégrer à un ERP. | FR-HR-001, FR-HR-002, FR-HR-003, FR-HR-004 |
| **OPS-04 — Restauration et régimes** | P2 | **NON DÉMONTRÉ** | Aucune preuve de restauration/régimes n’a été retrouvée. | Prescriptions alimentaires, allergies, menus, commandes, production, distribution et traçabilité sont à construire. | FR-MEAL-001, FR-MEAL-002, FR-MEAL-003, FR-MEAL-004 |
| **BI-01 — Statistiques et tableaux de bord médico-économiques** | P1 | **PARTIEL FAIBLE** | Plusieurs tableaux opérationnels existent (caisse, DAF, lits, urgences, patient). | Catalogue KPI gouverné, définitions versionnées, qualité des données, agrégations tenantées, exports, historisation et tableaux médico-économiques transverses ne sont pas complets. | FR-BI-001, FR-BI-002, FR-BI-003, FR-BI-004 |

## 5. Portes de sortie transverses non encore démontrées

Même lorsqu'un module est fonctionnellement avancé, il ne doit pas passer à `DONE/LIVRÉ` sans les preuves suivantes :

### 5.1 Données et intégrité

- isolation tenant testée sur chaque ressource et chaque chemin d'accès ;
- `organization_id` immuable et propagation vérifiée ;
- montants en décimal exact sur tous les DTO, calculs et exports ;
- verrouillage optimiste ou équivalent sur les objets concurrents ;
- idempotence sur création patient, visite, facture, paiement, document et appels externes ;
- corrections par version/avenant/avoir/contrepassation, sans écrasement silencieux.

### 5.2 Sécurité

- MFA de production, secrets hors logs, OIDC/OAuth2 ou stratégie locale documentée ;
- révocation de sessions distribuée et persistante ;
- BOLA/BFLA, rate limiting, upload sûr, antivirus, contrôle MIME/signature ;
- séparation des tâches sur droits, remboursements, avoirs, stocks et écritures ;
- audit append-only, corrélation, conservation et export probatoire.

### 5.3 API et interopérabilité

- versionnement explicite des API ;
- erreurs structurées avec correlation ID ;
- pagination bornée et tri stable ;
- `Idempotency-Key` pour opérations sensibles ;
- OpenAPI contractuel et tests de compatibilité ;
- webhooks signés, rejouables et observables ;
- profils FHIR R4 versionnés et validés.

### 5.4 Exploitation et continuité

- stockage documentaire S3/MinIO chiffré au lieu d'un disque local comme unique stratégie ;
- sauvegardes automatiques, restauration testée, RPO ≤ 1 h et RTO ≤ 4 h ;
- logs structurés, métriques, traces, alertes et tableaux SLO ;
- performance P95 < 2 secondes sur parcours critiques ;
- disponibilité pilote ≥ 99,5 % ;
- gestion faible connectivité avec reprise sûre et statut de synchronisation.

### 5.5 UX, accessibilité et qualité

- UAT signée par rôle et module ;
- FR/EN, light/dark, responsive et clavier ;
- WCAG 2.2 AA vérifié par tests automatisés et revue manuelle ;
- aucun composant/classe > 500 lignes sans dérogation ;
- guide utilisateur, support et rollback disponibles.

## 6. Backlog de livraison recommandé

| Epic | Modules | Priorité | Résultat attendu | Estimation initiale |
|---|---|---:|---|---:|
| **EPIC-0021-A — Socle de livraison sécurisé** | FND-01 à FND-04, CON-04 | P0 | Tenant/configuration, auth/sessions industrialisées, RBAC/ABAC, référentiels et audit probatoire | 55–89 SP |
| **EPIC-0021-B — Identité et urgence inconscient** | PAT-01, CLN-01, CLN-02, CLN-09, DOC-01, FIN-01/02 | P0 | URG-TEMP, triage immédiat, accompagnant, incapacité, effets, régularisation, transfert des créances et documents | 55–89 SP |
| **EPIC-0021-C — Parcours ambulatoire complet** | PAT-02, CLN-03 à CLN-07 | P0/P1 | Consultation → prescription/examens → résultat → réévaluation → document → clôture | 55–89 SP |
| **EPIC-0021-D — Hospitalisation et bloc** | HOS-01 à HOS-05 | P1 | Admission, lit, soins, bloc, anesthésie, suivi, sortie clinique et administrative | 89–144 SP |
| **EPIC-0021-E — Extensions hospitalières** | HOS-06 à HOS-09, CLN-08 | P1/P2 | Kiné, maternité, décès/morgue, garde/passation et imagerie | 89–144 SP |
| **EPIC-0021-F — Connect et interopérabilité** | CON-01 à CON-07 | P1/P2 | DPU multi-établissements, consentement, accès externe, notifications, API/webhooks et FHIR | 89–144 SP |
| **EPIC-0021-G — Finance complète** | FIN-01 à FIN-05 | P0/P2 | Facturation/caisse/assurance consolidées, comptabilité OHADA et budget | 89–144 SP |
| **EPIC-0021-H — Opérations et administration** | OPS-01 à OPS-04, HR-01 | P1/P2 | Achats, stocks lots/FEFO, immobilisations, RH/paie et restauration | 144–233 SP |
| **EPIC-0021-I — BI, NFR et release** | BI-01 + exigences transverses | P0/P1 | KPI gouvernés, sécurité, performance, PRA, observabilité, UAT et release candidate | 55–89 SP |

> Les estimations sont des ordres de grandeur de cadrage. Chaque epic doit être redécoupé en stories ≤ 8 SP après atelier métier et inspection détaillée du code.

## 7. Ordre d'exécution immédiat

### Vague 0 — Baseline et décision de release

1. mettre à jour le tracking, la version matrix et le backlog réel ;
2. établir une matrice exigences V3.1 → code → tests → preuve UAT ;
3. figer les règles de sortie de module ;
4. créer une branche et une PR par module ou lot cohérent, jamais une PR couvrant plusieurs domaines non liés.

### Vague 1 — P0 bloquants

1. `FND-02` : sessions/MFA/revocation/secrets ;
2. `PAT-01 + CLN-01 + CLN-09` : patient provisoire et urgence ;
3. `DOC-01 + FIN-01/02 + HOS-01` : documents, régularisation financière et admission liés à URG-TEMP ;
4. `FND-04` : référentiels et tarifs versionnés ;
5. parcours ambulatoire P0 E2E et zéro fuite inter-tenant.

### Vague 2 — P1

- laboratoire complet, hospitalisation, bloc/anesthésie, soins, assurance, Connect et KPI opérationnels.

### Vague 3 — P2

- imagerie, kiné, maternité, achats, immobilisations, comptabilité complète, budget, RH/paie et restauration.

## 8. Definition of Done obligatoire par module

Un module ne peut être déclaré livré que lorsque :

- toutes ses exigences P0/P1 prévues dans la vague sont reliées à un test ou une preuve UAT ;
- le workflow nominal et les cas d'erreur sont implémentés côté backend ;
- les transitions d'état sont contrôlées et auditées ;
- l'isolation tenant et le RBAC/ABAC sont testés positivement et négativement ;
- les migrations H2 et PostgreSQL 16 passent ;
- les tests unitaires, intégration, Angular et E2E sont verts ;
- l'écran est FR/EN, light/dark, responsive, clavier et accessible ;
- les PDF/documents sont versionnés et vérifiables lorsque requis ;
- la documentation fonctionnelle, technique, API, data, test et utilisateur est à jour ;
- le métier habilité signe la recette ;
- le changelog, la version matrix, le plan de rollback et le suivi projet sont mis à jour ;
- aucun risque critique n'est ouvert.

## 9. Scénarios E2E obligatoires avant release

- création patient, détection de doublon, rapprochement et conservation de provenance ;
- patient inconscient sans identité : arrivée → triage → réanimation → identité URG-TEMP → admission/bloc → reprise de conscience → régularisation → rapprochement ;
- ambulatoire : accueil → caisse/prise en charge → triage → consultation → ordonnance → clôture ;
- examens : demande → autorisation → prélèvement/réalisation → résultat validé → alerte critique → réévaluation médicale ;
- hospitalisation : admission → lit → soins/administrations → examens → bloc → suivi → sortie médicale puis administrative ;
- accès externe : demande → consentement/refus → accès borné → révocation/expiration → audit ;
- finance : devis → facture → paiement patient → bordereau assurance → règlement → caisse → banque → export ;
- stocks : réception lot → dispensation/consommation → inventaire → ajustement approuvé → rappel lot ;
- attaque cross-tenant et BOLA/BFLA sur chaque domaine ;
- rejeu réseau/idempotence sur paiement, création patient, document et webhook ;
- restauration d'une sauvegarde avec mesure RPO/RTO.

## 10. Risques majeurs

| Risque | Niveau | Traitement |
|---|---|---|
| Soins urgents bloqués par identité obligatoire | **Critique** | Implémenter URG-TEMP et admission minimale avant toute autre évolution urgence. |
| OTP/secrets visibles et états de sécurité en mémoire | **Critique** | Désactiver l’exposition, supprimer les logs, persister sessions/révocations et intégrer MFA de production. |
| Confusion entre fonctionnalité présente et module livré | **Élevé** | Utiliser la matrice exigences-preuves et la DoD par module. |
| Modules Back Office annoncés mais non démontrés | **Élevé** | Décider build vs intégration ERP et contractualiser les interfaces. |
| Stockage local des documents | **Élevé** | Migrer vers stockage objet chiffré, sauvegardé et scanné. |
| Tracking et documentation désynchronisés | **Moyen** | Rendre la mise à jour bloquante dans la PR et la CI. |
| Règles médicales/comptables non validées localement | **Élevé** | Recette par médecin chef, DAF/expert-comptable, DPO et RSSI. |

## 11. Prochaine action recommandée

Démarrer par une story verticale **`URG-TEMP — patient inconscient de l'arrivée à la régularisation`**. Elle doit inclure migration, modèle patient provisoire, déclarant/accompagnant, urgence, incapacité, effets personnels, documents, admission, facturation différée, rapprochement, audit, UI et tests E2E. Ce parcours constitue la meilleure preuve que le socle patient, clinique, financier, documentaire et sécurité fonctionne réellement de bout en bout.