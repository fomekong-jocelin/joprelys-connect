# QA-DEMO-20260725 — Readiness démonstration client du 25 juillet 2026

## Métadonnées

- **GitHub** : #127
- **Baseline** : `main@81b7d20c4cf44800e436c86b964a38bc62929305`
- **Branche** : `qa/demo-20260725-readiness`
- **Type** : QA Review + stabilisation pré-démo
- **Priorité** : P0 jusqu'au 25/07/2026
- **Estimation** : 3 SP / 1 à 1,5 j senior QA/full-stack, hors défaut majeur découvert
- **Reviewer** : Tech Lead + référent métier
- **Statut** : **AUDIT CODE/DOC TERMINÉ — RÉPÉTITION HUMAINE RESTANTE**

## Objectif

Sécuriser une démonstration client répétable du parcours clinique déjà livré, sans ajouter de dette technique ni ouvrir un chantier architectural risqué juste avant la démonstration.

Le périmètre fonctionnel convenu est :

1. accueil et patient ;
2. ouverture de visite ;
3. constantes ;
4. consultation ;
5. urgence URG-TEMP et triage ;
6. rapprochement vers le DPU canonique ;
7. continuité vers l'hospitalisation ;
8. service/chambre/lit cohérents.

## Contraintes non négociables

- GitHub est l'unique source de vérité.
- Aucun développement direct sur PROD ou RECETTE.
- Aucun mock, bypass RBAC, alias legacy ou donnée hardcodée spécifiquement pour la démo.
- Aucun retour de `HOSPITALIZATION_MANAGE` supprimée par HOS-RBAC-001-D / Flyway V86.
- Aucune modification des migrations V1–V86 déjà fusionnées.
- Aucun gros chantier HOS-STAFF/HOS-DIS/ABAC avant la démo sauf anomalie P0 démontrée.
- Toute correction doit être minimale, documentée, testée et revue.

## État déjà prouvé

- STORY-2305 / #46 : PR #96 fusionnée ; continuité URG-TEMP → hospitalisation/documents/finance livrée.
- STORY-2306 / #47 : PR #97 fusionnée ; workspace urgence, rapprochement et continuité vers hospitalisation livrés.
- HOS-02 / #73 : typage des services livré et tests backend/frontend présents.
- HOS-RBAC-001-D / #121 : PR #122 fusionnée ; `HOSPITALIZATION_MANAGE` supprimée par V86.
- Documentation de gouvernance HOS-RBAC réalignée via #124.
- Guide canonique de démonstration existant : `docs/features/demo-parcours-complet/DEMO-PARCOURS-COMPLET.md` ; le runbook du 25/07 est un addendum daté et non un second guide concurrent.

## Action plan

### A — Gouvernance et documentation

- [x] Aligner l'EPIC GitHub #36 sur les stories #40/#42/#44/#45/#46/#47 déjà terminées techniquement.
- [x] Aligner STORY-2305 sur l'état post-fusion de #96.
- [x] Aligner STORY-2306 sur l'état post-fusion de #97.
- [x] Réconcilier l'EPIC GitHub #25 : les constats historiques sont conservés comme origine d'audit mais les P0 déjà livrés ne sont plus présentés comme absents.
- [x] Réconcilier #29 : #31/#33 terminées ; #34 qualifiée PARTIELLE après lecture du code Angular courant.
- [x] Mettre `PROJECT-TRACKING.md` à jour après #122/#124 et ajouter la contrainte de démo.
- [x] Rattacher le runbook du 25/07 au guide de démo canonique existant.

### B — Audit code / tests de la démo

- [x] Inventorier les classes, composants et tests du parcours normal.
- [x] Inventorier les classes, composants et tests du parcours URG-TEMP.
- [x] Vérifier le typage des services/chambres/lits.
- [x] Vérifier la matrice RBAC des acteurs de démonstration après V86.
- [x] Identifier les scénarios automatisés réellement manquants avant d'ajouter du code.

### C — Validation technique

- [x] Aucun nouveau test artificiel ajouté : les étapes critiques possèdent déjà des preuves significatives et aucune lacune de test imposant du code n'a été démontrée pendant cet audit documentaire.
- [x] Couverture Flyway V86 vérifiée : Maven strict/greenfield de la PR #122 et test PostgreSQL 16 dédié V85→V86 sont documentés dans HOS-RBAC-001-D.
- [ ] Exécuter les suites globales sur le futur SHA réellement destiné à la répétition/déploiement si la baseline évolue avant samedi.
- [ ] Si un correctif backend/frontend devient nécessaire après répétition, Maven/Angular deviennent bloquants dans la PR correspondante.

### D — Runbook et répétition

- [x] Préparer le scénario principal normal à partir du guide canonique existant.
- [x] Préparer le scénario urgence/rapprochement/hospitalisation.
- [x] Documenter les acteurs et données à préparer sans secrets.
- [x] Définir les critères GO/NO-GO de la répétition générale.
- [x] Consigner honnêtement les limites non validées humainement.
- [ ] Réaliser la répétition générale sur l'environnement choisi pour la démo.
- [ ] Consigner le résultat GO/NO-GO après répétition.

## Preuves de code inventoriées

### Patient / accueil

- `PatientControllerTest` : création patient par `AGENT_ACCUEIL`, génération DPU/local ID et isolation tenant lors de la recherche.

### Visite / constantes

- `VisitControllerTest` : ouverture de visite, prévention du doublon actif, isolation tenant, saisie/lecture des constantes et calcul IMC, clôture réservée au profil habilité.

### Consultation

- `ConsultationControllerTest` : saisie médecin, upsert, validation des champs, refus `AGENT_ACCUEIL`, visite clôturée et cross-tenant.

### Hospitalisation / structure

- `HospitalizationAdmissionServiceTest` : lit configuré obligatoire, claim atomique d'un lit `FREE/OPEN/READY`, conflit si lit indisponible, continuité depuis une urgence sans visite préalable et conservation de `emergencyId`.
- `SpatialControllerTest` : service `HOSPITALIZATION` autorisant chambres/lits, service `ADMINISTRATIVE` refusant les chambres, capacité, disponibilité et isolation des opérations de configuration.
- `emergency-hospitalization-continuation.component.spec.ts` : seuls les services `allowsRooms=true` sont proposés, admission avec `HOSPITALIZATION_ADMIT`, aucun chargement/admission sans la permission dédiée.

### URG-TEMP / rapprochement

- `PatientReconciliationPageComponent` : file URG-TEMP, candidats/historique, décision explicite, conservation du parcours, reprise de l'`emergencyId`, navigation vers le DPU canonique et l'hospitalisation, clé d'idempotence sur rejeu.
- tests triage/dashboard/documents déjà présents dans le lot #97.

### Auth/session utile à la démo

- `auth-token.interceptor.ts` : refresh avant requête si access token expiré et retry après `401`.
- `AuthSessionRecoveryService` : un seul refresh concurrent, sauvegarde du nouveau token, purge/redirection si la session n'est plus récupérable.
- #34 reste ouverte pour l'UI complète « Sessions actives », mais cette UI n'est pas dans le scénario de démo.

## Matrice RBAC utile samedi

- `AGENT_ACCUEIL` : patient, visite, constantes, accueil ;
- `INFIRMIER` : patient/clinique/urgence, constantes, soins hospitaliers mais pas admission ;
- `MEDECIN` : clinique/urgence, consultation, admission, notes/consentement/soins/transfert/sortie médicale ;
- `RESPONSABLE_HOSPITALISATION` : admission, transfert, départ physique et opérations de lit ;
- `ADMIN_CLINIQUE` : administration du tenant et droits métier hors permissions plateforme exclues ;
- aucun rôle ne dépend de `HOSPITALIZATION_MANAGE`.

## Definition of Ready

- [x] baseline `main` connue ;
- [x] parcours de démonstration connu ;
- [x] dépendances #46/#47/#73/#121 fusionnées ou clôturées ;
- [x] aucune décision métier structurante requise pour démarrer l'audit ;
- [x] données de démonstration définies sans secrets.

## Definition of Done

- [x] documentation et tickets parents alignés avec le code actuel ;
- [x] aucun code existant dupliqué ;
- [x] aucun workaround de démonstration ;
- [x] preuves automatisées des parcours critiques inventoriées ;
- [x] runbook client prêt ;
- [x] risques techniques connus consignés ;
- [x] aucune action serveur effectuée ;
- [ ] répétition générale terminée ;
- [ ] GO/NO-GO client consigné.

## Impact version / SemVer

- Documentation/QA seule : aucun bump applicatif.
- Toute correction fonctionnelle découverte pendant la répétition sera ticketée séparément et évaluée selon `docs/release/SEMANTIC-VERSIONING.md`.
