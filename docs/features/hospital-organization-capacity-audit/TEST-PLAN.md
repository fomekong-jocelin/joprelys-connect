# Plan de test fonctionnel cible — Organisation et hospitalisation

**Référence** : AUDIT-20260721 / EPIC-0027  
**Statut** : cible de recette, non exécutable tant que les stories correspondantes ne sont pas implémentées

## Stratégie

Chaque scénario doit être exécuté avec PostgreSQL réel pour les règles de concurrence, avec horloge contrôlée pour les expirations, et en E2E Angular pour les permissions et messages utilisateur. Les données de santé sont synthétiques. Les contrôles tenant, unité et relation de soin sont systématiques.

## ORG-001 — Création d'un établissement

- **Objectif** : créer une clinique minimale sans niveaux inutiles.
- **Préconditions** : groupe facultatif existant ; catalogue de types actif.
- **Profil utilisateur** : administrateur plateforme avec `FACILITY_CREATE`.
- **Étapes** : saisir code, nom, type, pays, timezone ; activer.
- **Résultat attendu** : établissement actif, tenant initialisé, aucun site/département créé automatiquement.
- **Contrôles de sécurité** : refus sans permission ; code unique insensible à la casse ; aucun secret retourné.
- **Cas d'erreur** : timezone invalide, doublon code, type inconnu.
- **Données à historiser** : créateur, valeurs initiales, date d'effet, activation.

## ORG-002 — Création d'un service

- **Objectif** : créer un service clinique directement sous l'établissement.
- **Préconditions** : établissement actif.
- **Profil utilisateur** : administrateur structure.
- **Étapes** : créer code `MED-GEN`, type `SERVICE`, nom, spécialités et responsables ; activer.
- **Résultat attendu** : service actif sans imposer pôle/département.
- **Contrôles de sécurité** : responsables appartenant au tenant et affectations valides.
- **Cas d'erreur** : code dupliqué, responsable inactif, parent fermé.
- **Données à historiser** : version, dates, responsables et spécialités datées.

## ORG-003 — Création d'une unité

- **Objectif** : créer une unité de soins rattachée fonctionnellement à un service.
- **Préconditions** : service actif.
- **Profil utilisateur** : administrateur structure + validation cadre.
- **Étapes** : créer unité, niveau de soins, capacité planifiée, date d'effet.
- **Résultat attendu** : unité active, sans espace créé implicitement.
- **Contrôles de sécurité** : parent du même tenant ; approbation requise selon politique.
- **Cas d'erreur** : cycle hiérarchique, date de fin antérieure, parent incompatible.
- **Données à historiser** : lien hiérarchique et paramètres de soins.

## LOC-001 — Création d'une salle

- **Objectif** : représenter une salle de consultation non hospitalière.
- **Préconditions** : établissement et éventuellement site actifs.
- **Profil utilisateur** : administrateur des espaces.
- **Étapes** : créer espace type `CONSULTATION_ROOM`, capacité 1, lien partagé avec deux services.
- **Résultat attendu** : salle réservable, sans chambre/lit.
- **Contrôles de sécurité** : rattachements même tenant ; capacité positive.
- **Cas d'erreur** : type inconnu, code dupliqué, parent fermé.
- **Données à historiser** : emplacement, type et affectations datées.

## LOC-002 — Création d'une chambre

- **Objectif** : créer une chambre avec règles de compatibilité.
- **Préconditions** : espace parent et unité actifs.
- **Profil utilisateur** : administrateur des espaces + cadre.
- **Étapes** : type `INPATIENT_ROOM`, catégorie, politique sexe/âge, niveau de soins, isolement, capacité.
- **Résultat attendu** : chambre active et affectée à l'unité.
- **Contrôles de sécurité** : approbateur tracé ; combinaison de règles valide.
- **Cas d'erreur** : capacité nulle, règles contradictoires, affectation inter-tenant.
- **Données à historiser** : configuration, décisions et dates d'effet.

## BED-001 — Création de plusieurs lits

- **Objectif** : installer trois lits sans dépasser la capacité.
- **Préconditions** : chambre capacité 3 active.
- **Profil utilisateur** : gestionnaire structure.
- **Étapes** : créer A, B, C ; installer ; ouvrir ; marquer prêts.
- **Résultat attendu** : trois lits libres dérivés, capacité installée/ouverte=3.
- **Contrôles de sécurité** : code unique, transitions autorisées seulement.
- **Cas d'erreur** : quatrième lit, double code, ouverture avant installation.
- **Données à historiser** : chaque transition d'existence/ouverture/hygiène.

## ADM-001 — Admission programmée

- **Objectif** : convertir une demande planifiée en séjour.
- **Préconditions** : patient vérifié, demande approuvée, préadmission complète, lit compatible réservé.
- **Profil utilisateur** : admissions puis bed manager.
- **Étapes** : confirmer arrivée ; vérifier réservation ; affecter ; confirmer présence.
- **Résultat attendu** : réservation `CONSUMED`, séjour `IN_PROGRESS`, une affectation active.
- **Contrôles de sécurité** : séparation prescripteur/admissions ; idempotency key.
- **Cas d'erreur** : patient absent, réservation expirée, lit indisponible.
- **Données à historiser** : demande, décisions, checklist, réservation, affectation, acteurs.

## ADM-002 — Admission depuis les urgences

- **Objectif** : assurer un handoff sans rupture.
- **Préconditions** : urgence active, décision `ADMISSION`, unité aval et lit prêts.
- **Profil utilisateur** : urgentiste, cadre aval, bed manager.
- **Étapes** : demander aval ; accepter ; réserver ; confirmer départ urgences puis arrivée unité.
- **Résultat attendu** : séjour lié à l'urgence, présence aval active, urgence clôturée après arrivée.
- **Contrôles de sécurité** : seuls professionnels de la relation de soin ; corrélation unique.
- **Cas d'erreur** : refus aval, lit perdu, réseau coupé après départ.
- **Données à historiser** : décision, acceptation, jalons de transport, présence.

## BED-002 — Affectation d'un lit

- **Objectif** : affecter un lit libre compatible.
- **Préconditions** : séjour admis, lit installé/ouvert/prêt.
- **Profil utilisateur** : bed manager/cadre habilité.
- **Étapes** : sélectionner lit ; afficher compatibilité ; confirmer.
- **Résultat attendu** : affectation active et disponibilité `OCCUPIED` dérivée.
- **Contrôles de sécurité** : transaction, tenant, unité et relation de soin.
- **Cas d'erreur** : incompatibilité âge/sexe/isolement, lit fermé.
- **Données à historiser** : règles évaluées, décision, période et acteur.

## RES-001 — Réservation d'un lit

- **Objectif** : réserver un lit à l'avance.
- **Préconditions** : demande approuvée, lit compatible dans la plage.
- **Profil utilisateur** : bed manager.
- **Étapes** : renseigner début, fin prévue, expiration, priorité ; confirmer.
- **Résultat attendu** : réservation ferme visible ; lit non occupé avant arrivée.
- **Contrôles de sécurité** : période et tenant ; pas de surbooking par défaut.
- **Cas d'erreur** : expiration absente, période invalide, lit en maintenance.
- **Données à historiser** : création, prolongation, consommation/expiration/annulation.

## RES-002 — Tentative de double réservation

- **Objectif** : prouver l'exclusion temporelle.
- **Préconditions** : réservation ferme existante 10:00–14:00.
- **Profil utilisateur** : deux bed managers concurrents.
- **Étapes** : lancer simultanément deux réservations chevauchant la plage.
- **Résultat attendu** : une seule opération compatible réussit ; l'autre reçoit 409 avec alternatives.
- **Contrôles de sécurité** : contrainte PostgreSQL, pas seulement contrôle applicatif.
- **Cas d'erreur** : retry même idempotency key retourne le résultat initial.
- **Données à historiser** : succès, conflit, corrélation et critères.

## MOV-001 — Transfert entre chambres

- **Objectif** : déplacer un patient dans la même unité.
- **Préconditions** : séjour et présence actifs, destination prête.
- **Profil utilisateur** : cadre/infirmier délégué.
- **Étapes** : demander avec motif ; réserver destination ; confirmer départ et arrivée.
- **Résultat attendu** : une seule présence active ; ancien lit `TO_CLEAN` ; mouvement complet.
- **Contrôles de sécurité** : destination claimée ; action limitée à l'unité.
- **Cas d'erreur** : annulation avant départ, perte destination, arrivée non confirmée.
- **Données à historiser** : motif, jalons, acteurs, affectations source/destination.

## MOV-002 — Transfert entre services

- **Objectif** : transférer la responsabilité clinique et la localisation.
- **Préconditions** : demande médicale, service aval accepte, lit réservé.
- **Profil utilisateur** : médecins/cadres source et destination, transport.
- **Étapes** : demander ; accepter ; préparer ; départ ; arrivée ; handoff clinique.
- **Résultat attendu** : unité responsable changée à l'arrivée, données de continuité disponibles.
- **Contrôles de sécurité** : double validation ; accès source réévalué après handoff.
- **Cas d'erreur** : refus destination, transport retardé, patient instable.
- **Données à historiser** : validations, résumé de transfert, responsabilités et temps.

## MOV-003 — Changement de lit

- **Objectif** : déplacer vers un autre lit de la même chambre.
- **Préconditions** : destination compatible et libre.
- **Profil utilisateur** : infirmier habilité/cadre.
- **Étapes** : sélectionner motif ; confirmer mouvement physique.
- **Résultat attendu** : ancienne affectation close, nouvelle active, ancien lit à nettoyer selon politique.
- **Contrôles de sécurité** : verrou pessimiste/claim atomique.
- **Cas d'erreur** : double clic/retry, lit pris simultanément.
- **Données à historiser** : même niveau que tout mouvement.

## BED-003 — Fermeture d'un lit occupé

- **Objectif** : empêcher une fermeture silencieuse.
- **Préconditions** : lit avec affectation active.
- **Profil utilisateur** : cadre/maintenance.
- **Étapes** : demander fermeture immédiate avec motif.
- **Résultat attendu** : refus 409 et proposition de transfert ; exception vitale soumise à double validation.
- **Contrôles de sécurité** : maintenance seule ne déplace pas le patient.
- **Cas d'erreur** : fermeture planifiée après fin estimée autorisée avec alerte.
- **Données à historiser** : tentative, motif, plan de transfert/exception.

## DIS-001 — Sortie normale du patient

- **Objectif** : séparer trois validations de sortie.
- **Préconditions** : séjour actif, critères cliniques satisfaits.
- **Profil utilisateur** : médecin, admissions/caisse, infirmier.
- **Étapes** : valider sortie médicale ; préparer documents ; clearance ; confirmer départ physique.
- **Résultat attendu** : lit occupé jusqu'au départ, puis `TO_CLEAN`; séjour clos après règles.
- **Contrôles de sécurité** : permissions distinctes ; impossibilité de sauter une étape hors urgence auditée.
- **Cas d'erreur** : facture en litige, prescription manquante, patient reste physiquement.
- **Données à historiser** : trois décisions, documents, dettes autorisées, départ.

## TURN-001 — Nettoyage de la chambre

- **Objectif** : tracer la remise en état.
- **Préconditions** : départ physique confirmé, tâche `TO_CLEAN`.
- **Profil utilisateur** : équipe d'hygiène.
- **Étapes** : accepter ; démarrer ; exécuter protocole ; terminer.
- **Résultat attendu** : tâche `COMPLETED`, hygiène `VALIDATION_REQUIRED` ou `READY` selon politique.
- **Contrôles de sécurité** : équipe affectée ; horodatages cohérents.
- **Cas d'erreur** : incident, contamination, tâche interrompue.
- **Données à historiser** : protocole, opérateur, temps, incidents.

## TURN-002 — Remise à disposition du lit

- **Objectif** : ne rendre libre qu'un lit réellement prêt.
- **Préconditions** : nettoyage fini, inspection requise.
- **Profil utilisateur** : cadre/agent validateur distinct si exigé.
- **Étapes** : inspecter ; valider.
- **Résultat attendu** : `READY`, disponibilité dérivée `FREE` si aucun autre blocage.
- **Contrôles de sécurité** : un agent non habilité ne valide pas ; aucune réservation perdue.
- **Cas d'erreur** : inspection échouée retourne en nettoyage.
- **Données à historiser** : checklist, validateur, résultat.

## MAINT-001 — Mise en maintenance

- **Objectif** : planifier une indisponibilité sans conflit.
- **Préconditions** : lit libre, sans réservation chevauchante.
- **Profil utilisateur** : maintenance/biomédical.
- **Étapes** : ouvrir ordre, motif, période, impact ; démarrer.
- **Résultat attendu** : lit `MAINTENANCE/UNAVAILABLE`, alertes aux réservations futures.
- **Contrôles de sécurité** : rôle limité ; aucune donnée clinique inutile exposée.
- **Cas d'erreur** : lit occupé, réservation proche, ordre dupliqué.
- **Données à historiser** : ordre, panne, technicien, période.

## MAINT-002 — Réouverture

- **Objectif** : remettre un lit en service après contrôle.
- **Préconditions** : ordre terminé, qualification et nettoyage si requis.
- **Profil utilisateur** : maintenance puis cadre.
- **Étapes** : clôturer technique ; valider opérationnel ; valider hygiène.
- **Résultat attendu** : `OPEN + READY`, libre uniquement sans réservation/occupation.
- **Contrôles de sécurité** : séparation technique/soins configurable.
- **Cas d'erreur** : pièce non validée, contrôle hygiène absent.
- **Données à historiser** : rapports, validations, dates.

## EMR-001 — Admission d'un patient inconscient

- **Objectif** : prendre en charge sans identité confirmée.
- **Préconditions** : accès urgence, aucune identité certaine.
- **Profil utilisateur** : accueil urgence + soignant habilité.
- **Étapes** : créer identité provisoire ; enregistrer observations, tiers, capacité/base légale ; admettre.
- **Résultat attendu** : numéro temporaire unique, séjour lié, rapprochement ultérieur possible sans perte.
- **Contrôles de sécurité** : justification obligatoire, accès d'urgence audité.
- **Cas d'erreur** : idempotency retry, doublon possible, identité ultérieure contradictoire.
- **Données à historiser** : déclarations, sources, capacité, bases légales, liens canoniques.

## ADM-003 — Hospitalisation d'un mineur

- **Objectif** : gérer représentant légal et autorisations.
- **Préconditions** : âge sous seuil local ou date de naissance inconnue estimée mineure.
- **Profil utilisateur** : admissions + médecin.
- **Étapes** : identifier représentant, autorité, contacts, consentements ou exception urgence.
- **Résultat attendu** : checklist spécifique complète ou dérogation datée.
- **Contrôles de sécurité** : pièces protégées, accès restreint, pays/politique appliqués.
- **Cas d'erreur** : représentant absent, désaccord, mineur émancipé selon droit local.
- **Données à historiser** : relation, preuves, consentements, dérogations.

## CLIN-001 — Isolement

- **Objectif** : affecter une chambre compatible et bloquer les incompatibilités.
- **Préconditions** : décision médicale d'isolement, espace capable disponible.
- **Profil utilisateur** : médecin + cadre.
- **Étapes** : choisir type ; appliquer précautions ; affecter ; notifier équipe.
- **Résultat attendu** : règles chambre appliquées, capacité recalculée, tâche de désinfection future.
- **Contrôles de sécurité** : motif clinique visible au strict besoin ; audit.
- **Cas d'erreur** : aucune chambre compatible, isolement de cohorte, levée anticipée.
- **Données à historiser** : ordre, type, période, validations.

## CAP-001 — Saturation d'un service

- **Objectif** : déclencher une alerte fiable.
- **Préconditions** : seuil configurable 95 %, lits ouverts connus.
- **Profil utilisateur** : bed manager/direction/cadre.
- **Étapes** : occuper jusqu'au seuil ; créer une demande supplémentaire.
- **Résultat attendu** : alerte, file d'attente, alternatives ; aucun lit maintenance compté disponible.
- **Contrôles de sécurité** : vue direction agrégée sans données patient superflues.
- **Cas d'erreur** : capacité ouverte modifiée en cours de calcul.
- **Données à historiser** : snapshot, seuil, alerte, acquittement.

## LOC-003 — Indisponibilité d'une chambre

- **Objectif** : fermer une chambre et propager l'effet aux lits.
- **Préconditions** : chambre active ; lits libres ou plan de transfert.
- **Profil utilisateur** : cadre/logistique.
- **Étapes** : déclarer fermeture, motif, période.
- **Résultat attendu** : nouvelles réservations bloquées ; lits indisponibles ; occupations existantes jamais effacées.
- **Contrôles de sécurité** : validation supplémentaire si patients présents.
- **Cas d'erreur** : fermeture rétroactive chevauchant des faits.
- **Données à historiser** : downtime, décisions et patients impactés via corrélations.

## ORG-004 — Archivage d'un service

- **Objectif** : préserver l'historique sans accepter de nouvelle activité.
- **Préconditions** : service fermé, aucun séjour actif, dépendances recensées.
- **Profil utilisateur** : direction/admin structure.
- **Étapes** : lancer analyse d'impact ; migrer les futures activités ; archiver.
- **Résultat attendu** : lecture historique possible, sélection nouvelle impossible.
- **Contrôles de sécurité** : hard delete refusé ; double validation.
- **Cas d'erreur** : séjour/réservation/affectation personnel actif.
- **Données à historiser** : décision, dépendances, mappings successeurs.

## AUD-001 — Consultation de l'historique

- **Objectif** : reconstituer qui était où et pourquoi.
- **Préconditions** : séjour avec transferts, nettoyage et correction.
- **Profil utilisateur** : auditeur autorisé/cadre sur son périmètre.
- **Étapes** : filtrer patient/lit/période ; ouvrir timeline ; exporter si autorisé.
- **Résultat attendu** : chronologie immuable, anciennes/nouvelles valeurs, acteurs et corrélations.
- **Contrôles de sécurité** : tenant/unité, motif d'accès, export tracé.
- **Cas d'erreur** : accès hors périmètre, données archivées, correction tardive.
- **Données à historiser** : la consultation/export elle-même.

## SEC-001 — Personnel multi-établissements et multi-services

- **Objectif** : vérifier les affectations temporelles et accès contextuels.
- **Préconditions** : professionnel employé dans A/B, affecté à A1 et temporairement A2.
- **Profil utilisateur** : RH/cadre/RSSI.
- **Étapes** : tester accès pendant/avant/après affectation et dans B.
- **Résultat attendu** : droits effectifs limités aux périodes, unités et rôles ; historique intact après retrait.
- **Contrôles de sécurité** : aucun droit hérité du champ profession seul.
- **Cas d'erreur** : chevauchement incompatible, délégation expirée, compte désactivé.
- **Données à historiser** : emploi, affectation, délégation, décisions d'accès.

## PATH-001 — Prochaine étape du patient

- **Objectif** : afficher position, responsable et work-items cohérents.
- **Préconditions** : patient en consultation avec labo demandé et paiement en attente configurable.
- **Profil utilisateur** : équipe de soins/accueil selon données.
- **Étapes** : terminer consultation ; consulter parcours ; réaliser prélèvement ; publier résultat.
- **Résultat attendu** : position et prochaine étape évoluent par événements, sans double saisie.
- **Contrôles de sécurité** : détails cliniques masqués pour l'accueil.
- **Cas d'erreur** : tâche dupliquée, résultat annulé, patient absent.
- **Données à historiser** : création/assignation/complétion de chaque work-item.

## PHARM-001 — Dispensation interne et stock

- **Objectif** : garantir l'atomicité dispensation-stock.
- **Préconditions** : ordonnance active, lot disponible, pharmacie interne sélectionnée.
- **Profil utilisateur** : pharmacien affecté.
- **Étapes** : vérifier ; sélectionner lots ; dispenser simultanément depuis deux postes.
- **Résultat attendu** : quantités dispensées et stock cohérents ; aucun stock négatif.
- **Contrôles de sécurité** : rate limit persistant, licence/habilitation, tenant.
- **Cas d'erreur** : lot expiré, stock perdu en course, retry réseau.
- **Données à historiser** : ordonnance, lots, mouvements, pharmacien et conflit.

## LAB-001 — Cycle d'une analyse

- **Objectif** : imposer la machine à états laboratoire.
- **Préconditions** : ordre payé ou dérogation urgence, laboratoire cible et analyseur disponibles.
- **Profil utilisateur** : préleveur/technicien/biologiste selon transition.
- **Étapes** : collecter ; réceptionner ; analyser ; rendre résultat ; valider.
- **Résultat attendu** : transitions séquentielles, spécimen et équipements tracés.
- **Contrôles de sécurité** : validation biologiste uniquement ; relation source/cible.
- **Cas d'erreur** : échantillon rejeté, résultat corrigé, statut sauté.
- **Données à historiser** : custody, statuts, analyseur, validation/version.

## RESRC-001 — Conflit de plateau partagé

- **Objectif** : empêcher deux usages incompatibles d'une salle/ressource.
- **Préconditions** : espace partagé, équipement non partageable.
- **Profil utilisateur** : planificateurs de deux services.
- **Étapes** : créer deux réservations simultanées chevauchantes.
- **Résultat attendu** : une seule confirmation ; alternative proposée.
- **Contrôles de sécurité** : unité utilisatrice autorisée ; exclusion DB.
- **Cas d'erreur** : maintenance créée après réservation, priorité urgente.
- **Données à historiser** : demandes, arbitrage, priorité et alertes.

## NET-001 — Reprise après coupure réseau

- **Objectif** : éviter les doublons en faible connectivité.
- **Préconditions** : clé d'idempotence, coupure simulée après commit backend.
- **Profil utilisateur** : admissions/bed manager.
- **Étapes** : soumettre admission ; couper réponse ; réessayer.
- **Résultat attendu** : même séjour/affectation retourné, aucune duplication.
- **Contrôles de sécurité** : clé liée à l'acteur/tenant/payload et expiration sûre.
- **Cas d'erreur** : même clé avec payload différent refusée.
- **Données à historiser** : requêtes, résultat initial, retries et corrélation.

## KPI-001 — Réconciliation de capacité

- **Objectif** : valider les indicateurs installés/ouverts/prêts/disponibles/occupés.
- **Préconditions** : 10 lits : 8 ouverts, 7 prêts, 1 réservé, 5 occupés, 1 maintenance.
- **Profil utilisateur** : direction/cadre/QA data.
- **Étapes** : charger dashboard ; comparer requêtes sources ; changer un état.
- **Résultat attendu** : installés 10, ouverts 8, prêts 7, occupés 5, disponibles 1 selon données non chevauchantes ; mise à jour attendue.
- **Contrôles de sécurité** : agrégats anonymisés selon profil.
- **Cas d'erreur** : événement en retard, timezone, affectation incohérente mise en quarantaine.
- **Données à historiser** : version formule, timestamp et source du snapshot.

## Critères globaux de recette

- zéro double affectation/réservation dans 10 000 courses concurrentes ciblées ;
- aucune transition directe contournant les use cases ;
- matrice RBAC/ABAC testée pour chaque profil et tenant ;
- aucune suppression destructive d'un fait clinique ;
- capacité réconciliée avec les requêtes sources ;
- accessibilité clavier, FR/EN, light/dark et viewport mobile/tablette/desktop ;
- tests de migration sur copie anonymisée et rapport de conflits signé ;
- `./mvnw clean verify`, tests Angular et E2E verts avant livraison.
