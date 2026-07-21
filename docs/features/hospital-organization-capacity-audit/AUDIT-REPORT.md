# Audit de l'organisation hospitalière, des capacités et du parcours patient

**Référence** : AUDIT-20260721  
**Application** : Joprelys Connect  
**Version auditée** : `0.10.1`  
**Branche et révision** : `main` — `678c070e`  
**Date d'analyse** : 2026-07-21  
**Statut** : rapport d'audit terminé, recommandations à valider avant implémentation

## 1. Résumé exécutif

Joprelys n'implémente pas encore un référentiel hospitalier complet. Le modèle réellement en production dans le dépôt est essentiellement :

```text
Organization (établissement/tenant)
└── Ward (libellé « service » ou « département » selon l'écran)
    └── Room (en pratique chambre d'hospitalisation)
        └── Bed
            └── BedAssignment (occupation datée, partiellement historisée)
```

Les groupes, réseaux, sites, bâtiments, étages, zones, pôles, départements structurés, unités de soins, spécialités cataloguées et centres de responsabilité ne sont pas modélisés. Le champ libre `department` du personnel, le champ libre `service_name` d'une visite et l'entité `Ward` ne constituent pas ces référentiels.

L'application possède des briques cliniques réelles : accueil, visite, consultation, urgence, identité provisoire, laboratoire, pharmacie, hospitalisation, soins journaliers et compte rendu opératoire. Elles restent toutefois juxtaposées. Il n'existe ni moteur de parcours, ni position courante fiable du patient, ni file des actions attendues, ni modèle générique de ressources et locaux.

Les quatre risques immédiats sont :

1. l'absence de contrainte SQL empêchant deux affectations actives ou chevauchantes sur un même lit ;
2. la modification directe du statut d'un lit, qui peut désynchroniser lit, affectation et hospitalisation ;
3. la libération opérationnelle du lit dès la sortie médicale, sans sortie physique ni validation administrative distincte ;
4. le tableau Angular qui affiche `total - occupés` comme « lits libres » et inclut donc les lits en nettoyage et en maintenance.

La recommandation structurante est de séparer quatre axes aujourd'hui confondus :

- la structure juridique (`groupe`, `établissement`) ;
- la structure organisationnelle (`pôle`, `département`, `service`, `unité`) ;
- la structure géographique (`site`, `bâtiment`, `étage`, `zone`, `espace`) ;
- la capacité opérationnelle (`chambre`, `lit`, ouverture, disponibilité, réservation, occupation, nettoyage, maintenance).

Le modèle cible doit être facultatif par niveau. Une clinique pourra conserver `Établissement → Service → Espace`, tandis qu'un CHU pourra activer l'ensemble des niveaux. Le backlog proposé représente environ **102 à 140 jours-personnes**, hors migration de données exceptionnelle et homologations locales. Il ne doit pas être injecté dans le sprint courant sans arbitrage de capacité.

> **Suivi du 2026-07-21** — L'incrément HOS-BED-002-A ajoute un compteur backend `availableBedsCount` fondé uniquement sur `BedStatus.FREE` et fait afficher ce champ par Angular. GAP-007 est donc partiellement corrigé pour le modèle legacy ; la capacité ouverte, les statuts multi-axes et les invariants de HOS-BED-001/HOS-BED-002 restent à traiter.

> **Suivi intégrité du 2026-07-21** — HOS-BED-001-A ajoute avec V76 une contrainte portable garantissant au plus une affectation active par lit et un rollback 409 des collisions tardives. GAP-005 est partiellement réduit : la validation PostgreSQL 16, les chevauchements de périodes clôturées, la FK vers le séjour et l'idempotence restent ouverts.

> **Suivi séjour/période du 2026-07-21** — HOS-BED-001-B ajoute avec V77 une FK restrictive vers le séjour, l'unicité de l'affectation active par hospitalisation et la chronologie `released_at >= assigned_at`. GAP-005 est encore réduit sous H2 ; restent la validation PostgreSQL 16, les chevauchements entre périodes clôturées, la cohérence tenant composite et l'idempotence.

> **Suivi cohérence tenant du 2026-07-21** — HOS-BED-001-C ajoute avec V78 deux FK composites garantissant que l'affectation, le séjour et le lit relèvent du même établissement, ainsi qu'un refus applicatif sans fuite cross-tenant. GAP-005 est encore réduit sous H2 ; restent la validation PostgreSQL 16, les chevauchements entre périodes clôturées et l'idempotence complète.

## 2. Compréhension de l'existant et éléments réellement accessibles

### 2.1 Sources examinées

L'audit s'appuie sur les sources présentes dans le dépôt, notamment :

- 632 fichiers Java de production, 100 fichiers de test backend et 71 migrations Flyway accessibles ;
- 308 fichiers Angular sous `web/src/app`, dont 65 spécifications de composants/services ;
- les routes, menus, modèles, formulaires, services HTTP et traductions FR/EN Angular ;
- l'unique fichier applicatif Flutter `mobile/lib/main.dart` ; aucun parcours hospitalier mobile n'y est implémenté ;
- 222 tickets IA et 243 documents de fonctionnalités accessibles au moment de l'audit ;
- les cahiers des charges, standards, ADR, documents PM, configurations et scripts du dépôt ;
- les jeux de données créés dans les tests d'intégration et les migrations.

Le code de la révision auditée est traité comme preuve primaire. Les tickets ou documents anciens qui décrivent davantage que le code sont signalés comme intention, et non comme fonctionnalité disponible.

### 2.2 Matrice de maturité globale

| Domaine demandé | État | Preuve actuelle | Conclusion |
|---|---|---|---|
| Groupe/réseau de santé | Absent | aucune entité/table/API | plusieurs organisations ne forment pas un réseau administrable |
| Établissement | Partiel | `organizations`, CRUD plateforme | tenant plat, type libre, sans parent ni capacités structurées |
| Site, bâtiment, étage, zone | Absent | aucune entité/table/API/UI | la permission parle de bâtiments sans implémentation correspondante |
| Pôle, département, unité | Absent | aucun référentiel ; champs texte dispersés | impossible d'organiser un grand hôpital ou d'historiser un rattachement |
| Service | Partiel | `wards` + `HospitalServiceType` | concept mêlant service fonctionnel et conteneur spatial |
| Spécialité | Partiel/défectueux | `users.specialty` texte libre | pas de catalogue ni relation service-spécialité/professionnel-spécialité |
| Salle générique | Absent | `rooms` uniquement sous un `ward` hébergeant | une salle de consultation, de laboratoire ou d'attente n'est pas représentable |
| Chambre | Partiel | `rooms` : numéro, capacité, confort | absence de localisation, contraintes patient, statuts, historique et usage temporaire |
| Lit | Partiel/défectueux | `beds`, 4 statuts, version optimiste | pas de réservation ni axes installé/ouvert/disponible ; transitions non sécurisées |
| Bloc opératoire | Partiel | compte rendu opératoire et implants | aucune salle, table, programme, ressource ou SSPI |
| Urgences | Partiel avancé | admission, identité provisoire, triage, réanimation, stabilisation | pas d'unité/box/position, ni handoff atomique vers l'hospitalisation |
| Réanimation/USI | Absent comme organisation | valeur `INTENSIVE_CARE` de confort chambre | aucun niveau de soins, équipement, ratio ou capacité dédiée |
| Laboratoire | Partiel | ordres, statuts, résultats, cible externe | aucun site/laboratoire/paillasse/équipement ou traçabilité logistique d'échantillon |
| Imagerie | Absente | aucun module identifié | demandes, modalités, salles, planning et résultats absents |
| Pharmacie | Partiel | vérification/dispensation et stock | aucun local/dépôt ; dispensation non reliée automatiquement au stock interne |
| Équipements/ressources | Absent | aucune entité hospitalière | partage, maintenance, qualification et réservation impossibles |
| Personnel | Partiel | utilisateur, rôles RBAC, spécialité/département texte | aucun emploi ou rattachement fonctionnel daté, planning, garde ou habilitation clinique |
| Hospitalisation | Partiel/défectueux | admission directe, lit, notes, soins, transfert, sortie | demandes, préadmission, réservation, sorties séparées et cas limites manquent |
| Parcours patient | Fragmenté | visite, consultation, labo, facture, urgence, séjour | aucune orchestration ni prochaine étape consolidée |

### 2.3 Contradictions documentaires constatées

- La permission `SPATIAL_CONFIGURATION_MANAGE` annonce « bâtiments, services, chambres et lits », alors qu'aucun bâtiment n'existe.
- Les libellés FR nomment `Ward` « service », les libellés EN le nomment « department » ; le modèle ne distingue aucun des deux.
- Le ticket de typage des services contient encore des actions non cochées alors que la migration V74 et le code associé sont présents.
- Des tickets historiques déclarent le spatial ou l'hospitalisation « DONE », mais leurs critères couvrent uniquement le modèle réduit `Ward → Room → Bed` et non le périmètre hospitalier demandé ici.

### 2.4 Catalogue actuel des vingt objets audités

| Objet | Définition/relations et données actuelles | Écran/API | Création/modification/lecture/suppression | Limite constatée |
|---|---|---|---|---|
| Service | `Ward` : nom + type + organisation ; parent des chambres | Structure hospitalière ; `/api/spatial/configuration/wards` | config : `SPATIAL_CONFIGURATION_MANAGE` ; lecture occupation : `HOSPITALIZATION_READ` | cycle, responsables, spécialités, unité et multi-site absents |
| Département | aucune entité ; texte `users.department` | fiche personnel | `USER_MANAGE` modifie le texte | n'est ni référentiel ni rattachement historique |
| Pôle médical | absent | aucun | aucun | non représentable |
| Unité de soins | absente | aucun | aucun | capacité et responsabilité agrégées au service |
| Spécialité médicale | texte `users.specialty` | profil/personnel | utilisateur médecin pour certains champs, admin personnel pour la fiche | catalogue, multi-spécialité et lien service absents |
| Salle | `Room` sous un service autorisé ; numéro, capacité, confort | Structure/occupation | config : permission spatiale | ne représente que la chambre d'hébergement |
| Chambre | même `Room` | Structure/occupation | config ; lecture hospitalisation | aucune politique patient, statut ou fermeture |
| Lit | `Bed` : numéro, chambre, statut, version | Plan d'occupation | config pour CRUD ; `HOSPITALIZATION_MANAGE` pour statut | 4 statuts insuffisants et invariants incomplets |
| Bloc opératoire | `OperatingReport`/implant rattaché au séjour | onglet « Bloc & CRO » | `CLINICAL_WRITE/READ` | aucune salle, programme, table, équipe ou SSPI |
| Salle de consultation | absente ; consultation liée à visite | écran consultation | `CLINICAL_WRITE/READ` sur le dossier | aucun espace ou planning de salle |
| Salle d'attente | absente | aucun | aucun | aucune capacité/localisation/file spatiale |
| Unité d'urgence | `Emergency` clinique, pas organisationnelle | dashboard urgences | `EMERGENCY_*` | aucun box, lit urgence, zone ou unité spatiale |
| USI/réanimation | valeur de confort `INTENSIVE_CARE` seulement | formulaire chambre | config spatiale | ni unité ni niveau de soins structuré |
| Laboratoire | ordres/résultats, organisation cible facultative | file labo, dossier patient | `LAB_*` | pas de laboratoire physique, spécimen/localisation/équipement |
| Imagerie | absente | aucun | aucun | parcours totalement manquant |
| Pharmacie interne | stock organisationnel + dispensation | stocks et portail ordonnance | `PHARMACY_*`, `STOCK_*` | aucun local/dépôt ; stock/dispensation non atomiques |
| Ressource/équipement | absent | aucun | aucun | aucun partage, réservation ou maintenance |
| Affectation personnel | compte à une organisation, rôles, textes département/spécialité | gestion personnel/RBAC | `USER_*`, `RBAC_*` | aucun emploi/unité/période/garde/délégation |
| Mouvement/transfert | `BedAssignment` + transfert vers un lit | hospitalisation/plan lit | `HOSPITALIZATION_MANAGE` | pas de demande, jalons, motif, externe ou transport |
| Parcours patient | liens partiels patient-visite-urgence-séjour-labo-facture | admission, dossier, modules | permissions propres à chaque module | position, responsabilité et prochaine étape non consolidées |

Les suppressions existent uniquement sur la configuration service/chambre/lit et sont physiques lorsque les contrôles locaux les autorisent. Les faits d'hospitalisation n'exposent pas de suppression, mais certaines FK historiques utilisent `ON DELETE CASCADE`, notamment patient vers hospitalisation/laboratoire, ce qui impose une revue de conservation avant toute fonction de purge patient.

## 3. Schéma de l'organisation actuellement implémentée

### 3.1 Hiérarchie réelle

```text
Platform administrator
└── Organization [tenant]
    ├── UserAccount [exactement une organization_id]
    │   ├── role / user_roles [rôles applicatifs]
    │   ├── specialty [texte libre]
    │   └── department [texte libre]
    ├── Ward [service_type obligatoire]
    │   └── Room [une seule ward, capacité, comfort_level]
    │       └── Bed [FREE | OCCUPIED | CLEANING | MAINTENANCE]
    │           └── BedAssignment [hospitalization_id, début, fin]
    ├── Visit [service_name et orientation en texte]
    ├── Emergency [pas de box/unité]
    ├── Hospitalization [service/chambre/lit recopiés en texte]
    ├── LabOrder / LabResult [pas de laboratoire spatial]
    └── DrugStock / Dispensation [pas de pharmacie ou dépôt spatial]
```

### 3.2 Niveaux demandés comparés à l'existant

| Niveau | Implémenté | Cardinalité actuelle | Limite principale |
|---|---:|---|---|
| Groupe/réseau | Non | — | pas de consolidation multi-établissements |
| Établissement | Oui, partiel | tenant racine | pas de parent, code stable, dates d'effet ou périmètre réglementaire |
| Site | Non | — | établissement multi-site impossible |
| Bâtiment | Non | — | aucune localisation physique fiable |
| Étage | Non | — | aucune cartographie verticale |
| Pôle | Non | — | regroupement médico-économique impossible |
| Département | Non | — | seulement un texte sur l'utilisateur |
| Service (`Ward`) | Partiel | N par organisation | ne gère pas les rattachements multiples ni l'historique |
| Unité | Non | — | service multi-unités impossible |
| Salle/espace | Partiel | N chambres par ward | uniquement les wards autorisant les chambres |
| Chambre | Partiel | confondue avec `Room` | aucune extension clinique structurée |
| Lit | Partiel | N lits par room | statut monolithique et absence de réservation |

## 4. Analyse des services et départements

### 4.1 Définition et gestion actuelles

Le « service » est `WardEntity` : nom obligatoire, type fermé côté Java et organisation tenant. Les types sont `HOSPITALIZATION`, `EMERGENCY`, `OUTPATIENT`, `MEDICO_TECHNICAL`, `PHARMACY` et `ADMINISTRATIVE`. Seuls les deux premiers peuvent posséder des `Room`.

L'écran `/clinic/spatial/configuration` permet au détenteur de `SPATIAL_CONFIGURATION_MANAGE` de créer, modifier ou supprimer un service. La suppression est physique lorsqu'aucune chambre n'est rattachée. Les utilisateurs `HOSPITALIZATION_READ` voient les services et l'occupation ; les utilisateurs `HOSPITALIZATION_MANAGE` modifient les lits et transfèrent les patients.

| Donnée du service | État actuel |
|---|---|
| Obligatoire | `name`, `serviceType`, `organizationId` fourni par le contexte |
| Facultatif | aucune donnée métier |
| Relations | une organisation ; zéro à plusieurs chambres |
| Historisation | dates techniques de création/mise à jour uniquement |
| Cycle de vie | aucun : ni brouillon, suspension, fermeture, archivage |
| Responsable | absent |
| Spécialités | absentes |
| Codes/référentiels | absents |
| Multi-site/multi-bâtiment | absent |
| Centre de coût/responsabilité | absent |

### 4.2 Cas métier demandés

| Cas | Comportement actuel | Écart et risque | Recommandation | Priorité |
|---|---|---|---|---|
| Service dans plusieurs bâtiments | impossible | duplication probable, capacité et reporting faux | service indépendant de la géographie + rattachements datés vers espaces | Haute |
| Service avec plusieurs unités | impossible | responsabilités et capacités agrégées sans finesse | `OrganizationalUnit` hiérarchique et type `CARE_UNIT` | Haute |
| Unité fonctionnelle ailleurs | impossible | activité et position géographique confondues | relation N–N datée unité-espace | Haute |
| Médecin multi-services | seulement champ texte unique | droits et responsabilité non fiables | affectations professionnelles N–N datées | Haute |
| Infirmier temporairement ailleurs | absent | pas de traçabilité de renfort | affectation temporaire avec motif, validateur et dates | Haute |
| Salle multi-services | impossible | plateaux partagés dupliqués ou invisibles | propriété géographique + réservations par unités utilisatrices | Haute |
| Plateau technique partagé | absent | conflits et sous-utilisation non mesurables | ressource réservable avec calendrier et règles de priorité | Haute |
| Établissement sans départements | implicitement possible | seulement parce que le niveau n'existe pas | rendre les niveaux optionnels explicitement | Moyenne |
| Petite clinique multi-spécialités | spécialités texte sur utilisateurs | aucune relation au service | service N–N spécialités, sans département obligatoire | Haute |
| Réseau multi-établissements | organisations isolées | pas de gouvernance de groupe ni consolidation | groupe facultatif + memberships multi-établissements | Haute |

### 4.3 Conclusion du domaine

`Ward` doit être renommé ou remplacé progressivement par un concept de service/unité organisationnelle. Il ne doit plus être le parent obligatoire de tout espace. Le type de service actuel est utile comme capacité fonctionnelle, mais insuffisant comme modèle organisationnel.

## 5. Analyse des unités, salles, chambres et lits

### 5.1 Espaces

`RoomEntity` possède seulement : `ward`, `roomNumber`, `capacity`, `comfortLevel`, `organizationId`, dates techniques. Le formulaire propose `STANDARD`, `VIP`, `INTENSIVE_CARE`, `ISOLATION`, mais la base accepte toute chaîne. Un « niveau de confort » porte donc à la fois une catégorie commerciale, un niveau de soins et une contrainte d'isolement : trois dimensions métier distinctes.

Ne sont pas représentés : bâtiment, étage, zone, type de salle, code de localisation, sexe/âge autorisé, niveau de soins, restrictions, équipements, périodes de fermeture, maintenance, désinfection, réservations, conflits, changement d'usage et historique.

La règle qui interdit toute `Room` à un service `MEDICO_TECHNICAL`, `PHARMACY`, `OUTPATIENT` ou `ADMINISTRATIVE` empêche précisément de représenter une salle d'imagerie, un laboratoire, une pharmacie, une consultation, une attente ou un bureau. Cela confirme que `Room` est une chambre d'hospitalisation mal nommée, non une salle générique.

### 5.2 Lits et capacité

Le lit possède un numéro, une chambre, un statut et une version optimiste. Le claim atomique `FREE → OCCUPIED` réduit correctement une course lors d'une admission ou d'un transfert. Le test d'intégration concurrent vérifie qu'un seul transfert gagne sur le même lit.

Cependant :

- aucune réservation n'existe ;
- aucune contrainte SQL n'interdit plusieurs `bed_assignments` actifs ou chevauchants ;
- `hospitalization_id` de `bed_assignments` n'est pas une clé étrangère ;
- un appel direct peut passer un lit à `OCCUPIED` sans affectation ;
- un appel direct peut passer un lit occupé à `MAINTENANCE` ou `CLEANING` sans contrôler l'hospitalisation ;
- passer un lit à `FREE` clôt l'affectation mais ne met pas à jour le séjour ;
- la capacité installée, ouverte et exploitable n'est pas distinguée ;
- le nombre affiché comme libre est `total - occupé`, et inclut nettoyage et maintenance ;
- aucune durée, cause, responsable ou preuve de remise en service n'est historisée.

### 5.3 Couverture par type d'espace

| Type demandé | État réel | Observation |
|---|---|---|
| Salle de consultation/soins/examen | Absent | une `Room` ne peut exister sous `OUTPATIENT` |
| Salle d'intervention/bloc | Absent | seuls le compte rendu et les implants existent |
| Salle de réveil/SSPI | Absent | aucun emplacement ou mouvement |
| Chambre | Partiel | `Room` sans contraintes cliniques ou cycle de vie |
| Box d'urgence | Absent | urgence sans localisation |
| Poste de soins | Absent | aucune ressource partagée |
| Laboratoire | Absent spatialement | workflow d'ordres présent séparément |
| Salle d'imagerie | Absent | aucun module d'imagerie |
| Salle d'attente | Absent | pas de capacité ou file localisée |
| Pharmacie/dépôt | Absent spatialement | stock unique par organisation |
| Zone d'isolement | Partiel nominal | `comfortLevel=ISOLATION`, sans règles |
| Morgue | Absent | décès seulement comme orientation d'urgence |
| Espace administratif | Absent | les services administratifs ne peuvent avoir de salle |

## 6. Analyse de l'hospitalisation

### 6.1 Parcours disponible

Le séjour actuel est créé directement en `EN_COURS`. La requête exige patient, service, chambre, lit, motif et médecin responsable. Elle peut référencer une visite et/ou une urgence. Depuis une urgence sans visite, le backend crée une visite et conserve le lien vers l'urgence. Le lit configuré est recherché par noms, puis claimé atomiquement. Une `BedAssignment` est créée.

Pendant le séjour, Joprelys gère notes, consentements chirurgicaux, soins journaliers, administrations de médicaments, consommables, compte rendu opératoire et implants. Un transfert de lit clôt l'affectation antérieure, passe l'ancien lit à `CLEANING`, occupe le nouveau et recopie les libellés dans le séjour.

La sortie exige diagnostic et consignes, permet « contre avis médical », génère un PDF, clôt l'affectation et met le lit à `CLEANING`. La sortie médicale, la sortie administrative et le départ physique ne sont pas distingués.

### 6.2 Fonctions attendues

| Fonction | État | Commentaire |
|---|---|---|
| Demande/décision médicale | Absente | admission directe seulement |
| Préadmission/admission administrative | Absentes | la préinscription patient est un autre concept |
| Affectation service/unité/chambre/lit | Partielle | pas d'unité ; noms recopiés ; lit réel claimé |
| Réservation anticipée/expiration | Absente | aucun modèle |
| Admission urgences | Partielle | lien explicite et identité provisoire présents |
| Programmée/directe | Absente comme type | aucune date planifiée ni workflow |
| Identité non confirmée/inconscient | Partielle avancée | identité provisoire, capacité et base légale urgence |
| Mineur/accompagnant | Absents du séjour | contact patient ne remplace pas le représentant légal |
| Isolement/observation | Absents comme parcours | libellé confort uniquement |
| Transfert interne | Partiel | transfert de lit uniquement, sans demande/validation/motif |
| Transfert inter-établissements/externe | Absent | aucune continuité ou handoff |
| Fermeture/nettoyage/maintenance | Partiel | statuts directs, sans événement ni planification |
| Sorties médicale/administrative/physique | Défectueux | une action unique libère l'occupation |
| Décès/évasion/transfert externe | Absents du séjour | orientation urgence `DEATH` non suffisante |
| Historique d'occupation | Partiel | affectations datées, sans contexte de transfert |

### 6.3 Risques d'hospitalisation

| Risque | Protection actuelle | Résiduel |
|---|---|---|
| Double affectation simultanée | claim atomique du statut du lit | critique : aucun verrou temporel SQL sur les affectations/réservations |
| Occupation non clôturée | clôture lors transfert/sortie | haute : changement manuel de statut et absence d'invariant DB |
| Libération prématurée | aucune séparation de sorties | critique |
| Unité/chambre incompatible | service avec chambres seulement | haute : aucune règle sexe, âge, isolement, niveau de soins |
| Perte de trace de transfert | affectation précédente conservée | moyenne/haute : pas de motif, acteur, décision, origine/destination explicites |
| Incohérence séjour/lit | noms dupliqués + statuts mutables | critique |
| Réservation sans expiration | fonction absente | haute dès introduction si non corrigée |

## 7. Analyse du parcours patient

### 7.1 Parcours actuel reconstitué

```text
Accueil / admission unifiée
├── Parcours normal → patient → visite EN_COURS → consultation
└── Urgence → patient existant/nouveau/provisoire → triage/réanimation
    └── stabilisation/orientation → navigation UI possible vers hospitalisation

Consultation
├── ordonnance → pharmacie publique/interne non distinguée
├── demande laboratoire → file labo → résultat
└── documents/facturation

Hospitalisation
└── admission directe → soins/transfert → sortie unique → lit CLEANING
```

La visite stocke une orientation, un service et un praticien sous forme de texte/UUID, avec les seuls statuts `EN_COURS`, `TERMINEE`, `ANNULEE`. La stabilisation d'urgence clôt le dossier actif en enregistrant une orientation, mais l'hospitalisation subséquente est une action distincte de l'interface : le handoff n'est pas atomique.

### 7.2 Ruptures et doubles saisies

- le nom du service est saisi dans la visite puis resaisi dans l'hospitalisation ;
- le séjour recopie service, chambre et lit alors qu'une affectation existe ;
- les responsables de soin sont parfois des UUID, parfois des noms texte (`performed_by`, `administered_by`, `consumed_by`) ;
- urgence, visite, hospitalisation, laboratoire, facturation et pharmacie ont chacun leur état, sans état de parcours consolidé ;
- le paiement n'est pas une précondition explicite configurable du passage clinique ;
- une urgence orientée `ADMISSION` peut être stabilisée sans qu'un lit soit attribué ;
- la pharmacie enregistre une dispensation sans décrémenter le stock interne via `DrugStockService` ;
- le laboratoire autorise la modification directe vers n'importe quel statut, sans machine à états.

### 7.3 Questions opérationnelles

| Question | Réponse actuelle fiable ? | Motif |
|---|---:|---|
| Où se trouve le patient ? | Non | seulement lit courant d'un séjour ; aucune présence générale |
| Quel service le prend en charge ? | Partiel | texte de visite/séjour, non référentiel |
| Quel professionnel en est responsable ? | Partiel | un praticien principal/responsable, non validé par affectation |
| Quelle est la prochaine étape ? | Non | navigation UI locale, aucun workflow persistant |
| Quelles actions sont en attente ? | Partiel | files propres au labo/facturation, aucune liste transverse |
| Examens demandés/résultats ? | Oui, pour le laboratoire | imagerie absente |
| Soins réalisés ? | Partiel | hospitalisation/urgence, acteurs parfois texte |
| Transferts ? | Partiel | lits seulement |
| Factures en attente ? | Oui dans le module financier | non agrégé au critère de sortie |
| Le patient peut-il sortir ? | Non de manière consolidée | aucun readiness checklist |
| Le lit peut-il être libéré ? | Défectueux | sortie unique et statut direct |

## 8. Analyse des rôles et responsabilités

### 8.1 Modèle actuel

Un utilisateur appartient à une seule organisation. Les rôles applicatifs sont stockés dans le RBAC et encore recopiés dans une chaîne `users.role`. `specialty` et `department` sont des textes uniques. Il n'existe aucune entité de profession, fonction, poste, contrat, service principal/secondaire, unité, garde, astreinte, remplacement, délégation ou habilitation clinique.

La désactivation porte sur tout le compte, pas sur une affectation. Retirer un rôle est audité dans le RBAC, mais retirer une personne d'un service n'est pas possible car le rattachement service n'existe pas.

### 8.2 Permissions actuelles du périmètre

| Opération | Permission | Profils standard notables |
|---|---|---|
| CRUD établissements | `ORGANIZATION_MANAGE` | administrateurs plateforme |
| CRUD service/chambre/lit | `SPATIAL_CONFIGURATION_MANAGE` | administrateur clinique et plateforme |
| Voir occupation/séjours | `HOSPITALIZATION_READ` | médecin, infirmier, responsable hospitalisation |
| Admission, transfert, statut lit, soins, sortie | `HOSPITALIZATION_MANAGE` | médecin, infirmier, responsable hospitalisation |
| Urgence | `EMERGENCY_READ/WRITE/STABILIZE` | granularité partielle |
| Laboratoire | `LAB_*` | biologiste, médecins selon action |
| Pharmacie/stock | `PHARMACY_*`, `STOCK_*` | pharmacien |
| Personnel | `USER_READ/MANAGE` | administrateurs selon rôle |

Le permissionnement est tenant-wide. Il ne tient compte ni du site, ni du service, ni de l'unité, ni de la relation de soin, ni d'une délégation temporelle. Un infirmier standard possédant `HOSPITALIZATION_MANAGE` peut techniquement déclencher la même sortie qu'un médecin et changer un lit en maintenance ; le backend ne distingue pas la décision médicale, le départ administratif, le bionettoyage et la maintenance biomédicale.

### 8.3 Cible de responsabilité

Il faut séparer :

- identité de la personne ;
- profession réglementée et spécialité ;
- fonction/poste organisationnel ;
- affectation datée à un établissement/service/unité ;
- habilitation clinique ;
- rôle applicatif et permission ;
- délégation temporaire ;
- relation de soin au patient.

## 9. Analyse du modèle de données

### 9.1 Forces existantes

- `organization_id` et `@TenantId` sont largement utilisés ;
- le lit possède une version optimiste et un claim atomique ;
- les affectations de lit conservent début et fin ;
- l'urgence dispose d'un historique de triage et d'un parcours d'identité provisoire plus rigoureux ;
- le RBAC est structuré en rôles, permissions et affectations ;
- les résultats de laboratoire sont versionnés et peuvent référencer un résultat parent.

### 9.2 Faiblesses structurantes

| Type | Constat |
|---|---|
| Relations rigides | chambre rattachée à un seul service ; utilisateur à un seul établissement |
| Relations manquantes | unité-espace, service-spécialité, personnel-unité, équipement-espace, réservation-lit |
| Concepts mélangés | `comfort_level` = confort + soins intensifs + isolement ; `Ward` = service + localisation |
| Données dupliquées | noms service/chambre/lit dans le séjour ; service texte dans la visite |
| Historisation | aucun rattachement organisationnel/spatial daté ; statut direct sans journal dédié |
| Suppressions | cascades `Ward → Room → Bed` et suppression physique conditionnelle |
| États | chaînes ou enums incomplètes ; V68 supprime plusieurs contraintes de statut |
| Intégrité | pas de FK `bed_assignments.hospitalization_id`, plusieurs `organization_id` sans FK |
| Concurrence | claim ponctuel mais pas d'exclusion temporelle DB |
| Tenant | pas de contraintes composites garantissant que parents/enfants appartiennent au même tenant |
| Reporting | aucune table d'événement de capacité ou snapshot de disponibilité |
| Temps | mélange de `TIMESTAMP` et `TIMESTAMP WITH TIME ZONE` |
| Migration | V74 échoue volontairement si une ward historique sans chambre reste non typée |

Le modèle cible détaillé est dans [DATA-MODEL.md](DATA-MODEL.md).

## 10. Liste détaillée des écarts

| Référence | Domaine | Fonction actuelle | Écart identifié | Conséquence | Risque | Recommandation | Priorité | Complexité |
|---|---|---|---|---|---|---|---|---|
| GAP-001 | Modèle/organisation | organisation tenant plate | aucun groupe/site/bâtiment | réseaux et multi-sites non représentables | reporting et exploitation faux | hiérarchies juridique et géographique séparées | Haute | Élevée |
| GAP-002 | Métier | `Ward` nommé service/département | service, département et localisation confondus | responsabilités ambiguës | erreurs d'affectation | unités organisationnelles typées | Haute | Élevée |
| GAP-003 | Fonctionnel | type de service fermé | salles interdites aux consultations, labo, pharmacie, administratif | locaux réels impossibles | contournements/doublons | espace générique indépendant du service | Haute | Élevée |
| GAP-004 | Données | service/chambre/lit recopiés en texte | duplication avec spatial | renommages et historique incohérents | dossier erroné | FK stables + libellés snapshot explicites | Haute | Moyenne |
| GAP-005 | Intégrité | claim atomique du lit | aucune exclusion SQL des périodes | double affectation possible par import/code concurrent | sécurité des soins | contrainte d'exclusion temporelle | Critique | Élevée |
| GAP-006 | Cycle de vie | statut lit modifiable directement | occupation et affectation peuvent diverger | lit faux libre/occupé | admission dangereuse | commandes métier et invariants transactionnels | Critique | Moyenne |
| GAP-007 | Ergonomie/reporting | libre = total − occupé | maintenance/nettoyage comptés libres | décisions de capacité erronées | saturation cachée | calculer `status=FREE` puis capacité ouverte | Critique | Faible |
| GAP-008 | Hospitalisation | sortie unique | médical, administratif et physique confondus | libération prématurée | patient/lit introuvable | workflow de sortie à trois validations | Critique | Élevée |
| GAP-009 | Hospitalisation | admission directe EN_COURS | pas de demande, décision, planification, préadmission | lit non anticipé | annulations/conflits | demande + réservation + admission | Haute | Élevée |
| GAP-010 | Capacité | 4 statuts lit | installé/ouvert/fermé/bloqué/réservé manquent | capacité réelle inconnue | pilotage faux | axes de statut séparés et disponibilité dérivée | Haute | Élevée |
| GAP-011 | Traçabilité | `BedAssignment` début/fin | motif, acteur, transfert et correction absents | audit incomplet | litige/erreur | événement de mouvement immuable | Haute | Moyenne |
| GAP-012 | Données | hard delete spatial conditionnel | pas d'archivage/date d'effet | perte de contexte et clés historiques fragiles | audit | archivage et validité temporelle | Haute | Moyenne |
| GAP-013 | Données | `comfortLevel` libre | dimensions incompatibles mélangées | règles impossibles | chambre inadaptée | catégories, niveau de soins et isolement séparés | Haute | Moyenne |
| GAP-014 | Fonctionnel | aucune contrainte patient-chambre | sexe, âge, isolement, accompagnant absents | affectation inadéquate | sécurité/dignité | moteur de compatibilité backend | Haute | Élevée |
| GAP-015 | Personnel | département/spécialité texte uniques | multi-affectation et historique absents | droits/charge faux | accès indu | référentiels et affectations datées | Haute | Très élevée |
| GAP-016 | Droits | `HOSPITALIZATION_MANAGE` global | mêmes droits pour décision, soins, lit, sortie | séparation des tâches absente | sécurité/abus | permissions par action + contexte d'unité | Critique | Élevée |
| GAP-017 | Confidentialité | accès tenant-wide au séjour | aucune relation de soin ou unité autorisée | accès excessif | confidentialité | ABAC : tenant + affectation + relation de soin | Haute | Très élevée |
| GAP-018 | Parcours | modules juxtaposés | aucune position/prochaine action transverse | ruptures, délais | perte de patient | épisode, présence et work-items | Haute | Très élevée |
| GAP-019 | Urgences | stabilisation puis navigation | handoff hospitalisation non atomique | urgence close sans lit | rupture de soins | orientation demandée/acceptée et handoff transactionnel | Haute | Élevée |
| GAP-020 | Urgences | dossier sans localisation | aucun box/zone/brancard | position inconnue | retard de soins | présence et ressources urgences | Haute | Élevée |
| GAP-021 | Bloc | CRO/implants | bloc, salle, table, programme, SSPI absents | conflits et traçabilité incomplète | sécurité opératoire | module ressources opératoires | Haute | Très élevée |
| GAP-022 | Réanimation | libellé de confort | aucune unité/capacité de soins critiques | niveau de soins non garanti | sécurité clinique | unité/capabilité/équipement critiques | Haute | Élevée |
| GAP-023 | Laboratoire | ordres/résultats | aucun site, analyseur, prélèvement routé ou emplacement | chaîne pré-analytique faible | résultat retardé/mal routé | lab facility/specimen/resource | Haute | Très élevée |
| GAP-024 | Laboratoire | statut librement remplaçable | transitions et préconditions absentes | résultat validé hors séquence | qualité | machine à états backend | Haute | Moyenne |
| GAP-025 | Imagerie | aucun module | demande, modalité, planning, compte rendu absents | parcours incomplet | perte de chance/double saisie | epic imagerie distinct | Haute | Très élevée |
| GAP-026 | Pharmacie | dispensation + stock séparés | dispensation ne décrémente pas le stock interne | stock théorique faux | rupture/erreur | transaction stock-dispensation selon pharmacie cible | Haute | Élevée |
| GAP-027 | Sécurité | lockout pharmacie en mémoire | non partagé et perdu au redémarrage | brute force distribué | accès prescription | compteur persistant/distribué, rate limit | Haute | Moyenne |
| GAP-028 | Ressources | aucune entité équipement | partage, réservation, panne absents | conflits/indisponibilité invisible | soins retardés | ressource, affectation, réservation, maintenance | Haute | Très élevée |
| GAP-029 | Multi-tenant | IDs tenant sans FK composite | parent/enfant inter-tenant possible au niveau DB | fuite/corruption | confidentialité | FK composites et contraintes de cohérence | Critique | Élevée |
| GAP-030 | Audit | log générique avec raison texte | pas de before/after ni corrélation métier systématique | reconstitution difficile | médico-légal | événements immuables structurés | Haute | Élevée |
| GAP-031 | Acteurs | noms texte sur certains soins | auteur non résolu et modifiable | imputabilité faible | traçabilité | `performedByUserId` + snapshot signé | Haute | Moyenne |
| GAP-032 | Responsable | praticien de séjour non validé | existence/tenant/rôle/affectation non contrôlés dans l'admission | mauvais responsable | sécurité | validation d'habilitation backend | Haute | Moyenne |
| GAP-033 | Interopérabilité | API et FHIR partiels | aucun identifiant Location/Organization/Encounter cible | échanges ambigus | intégration | mapping FHIR Location/Organization/Encounter/PractitionerRole | Moyenne | Élevée |
| GAP-034 | Performance | occupation chargée service par service | aucune vue/snapshot capacité | tableaux réseau coûteux | latence | projections d'événements et index temporels | Moyenne | Élevée |
| GAP-035 | Résilience | web connecté uniquement | aucune stratégie faible débit/offline du domaine hospitalier | indisponibilité en contexte réseau instable | continuité | mode dégradé ciblé et reprise idempotente | Haute | Très élevée |
| GAP-036 | Architecture | services 385/459 lignes | responsabilités admissions, PDF, lit et mapping regroupées | régression | maintenance | use cases séparés, événements domaine | Moyenne | Moyenne |
| GAP-037 | Configuration | V74 requiert correction manuelle | wards vides bloquent la migration | déploiement impossible | exploitation | script de préflight et table de mapping | Haute | Faible |
| GAP-038 | Temps | timestamps avec/sans fuseau | comparaisons temporelles ambiguës | chevauchements/reporting faux | intégrité | `timestamptz`/`Instant` uniformes | Haute | Moyenne |
| GAP-039 | Reporting | aucun dénominateur « lits ouverts » | taux d'occupation basé sur lits configurés | KPI non comparable | décision direction | capacité installée/ouverte/opérationnelle | Haute | Élevée |
| GAP-040 | Réglementaire | règles pays non paramétrées | conservation, représentant légal, décès à valider | non-conformité locale possible | juridique | politiques par pays validées DPO/juriste | Haute | Élevée |

## 11. Risques métier et opérationnels

| Risque | Scénario | Impact | Mesure immédiate avant refonte | Validation requise |
|---|---|---|---|---|
| Affectation concurrente | deux admissions sur le même lit | deux patients annoncés au même emplacement | surveiller incohérences, limiter imports/écritures directes | cadre infirmier + DBA |
| Faux lit libre | lit maintenance affiché libre | admission retardée ou impossible | corriger le calcul UI/API en P0 | responsable hospitalisation |
| Sortie prématurée | sortie médicale avant départ physique | lit réattribué trop tôt | procédure manuelle de confirmation, ne pas passer CLEANING avant départ | médecin + admissions + cadre |
| Maintenance sur lit occupé | statut forcé par permission large | perte de localisation | interdire l'action hors commande métier | biomédical + cadre |
| Accès excessif | infirmier tenant-wide | consultation non nécessaire | revue des rôles et journalisation renforcée | DPO/RSSI |
| Service/chambre incompatible | absence de contraintes pédiatrie/isolement | atteinte à la sécurité/dignité | contrôle humain obligatoire | médecin + cadre |
| Handoff urgence perdu | urgence stabilisée sans séjour | patient sans responsable aval | registre manuel des orientations non acceptées | urgentiste + bed manager |
| Stock pharmacie faux | dispensation non liée au stock | rupture inattendue | rapprochement quotidien stock/dispensations | pharmacien |

Les règles juridiques de conservation, consentement, mineurs, décès, transferts et accès d'urgence doivent être validées pays par pays. Ce rapport ne constitue pas un avis réglementaire.

## 12. Modèle organisationnel cible

### 12.1 Principes

1. Aucun niveau facultatif ne doit être artificiellement créé.
2. L'organisation et la géographie sont deux arbres distincts.
3. Les liens entre unités, espaces, personnels et spécialités sont datés.
4. Un code stable interne remplace les jointures par libellé.
5. Les suppressions métier deviennent archivages ou fins de validité.
6. La capacité se calcule à partir des lits installés, ouverts, prêts, réservés et occupés, pas à partir d'un compteur manuel.

### 12.2 Profils de configuration

```text
Cabinet :       Établissement → Espace de consultation
Petite clinique:Établissement → Service → Chambre → Lit
Hôpital :       Établissement → Site/Bâtiment → Service/Unité ↔ Espaces → Lit
CHU/réseau :    Groupe → Établissements → Sites → Bâtiments
                Pôles → Départements → Services → Unités ↔ Espaces → Lits
```

Les flèches `↔` matérialisent un rattachement daté et potentiellement multiple : une unité utilise plusieurs espaces et un espace partagé est réservable par plusieurs unités.

### 12.3 Typologies cibles

- `OrganizationUnitType` : `POLE`, `MEDICAL_DEPARTMENT`, `ADMIN_DEPARTMENT`, `SERVICE`, `CARE_UNIT`, `FUNCTIONAL_UNIT`, `RESPONSIBILITY_CENTER`, `TRANSVERSAL_SERVICE`.
- `LocationType` : `SITE`, `BUILDING`, `FLOOR`, `ZONE`, `SPACE`.
- `SpaceType` : consultation, soins, examen, intervention, bloc, SSPI, chambre, box urgence, laboratoire, imagerie, attente, pharmacie, dépôt, isolement, morgue, administratif.
- `Capability` : hospitalisation, urgence, réanimation, ambulatoire, laboratoire, imagerie, chirurgie, pharmacie, stérilisation, etc.

## 13. Modèle de données cible

Le détail des entités, cardinalités, statuts, contraintes, dates d'effet, index et règles de migration figure dans [DATA-MODEL.md](DATA-MODEL.md).

Les agrégats majeurs sont :

- `OrganizationGroup`, `HealthcareFacility`, `OrganizationMembership` ;
- `OrganizationalUnit`, `Specialty`, `UnitSpecialty`, `UnitRelationship` ;
- `LocationNode`, `Space`, `InpatientRoom`, `Bed`, `UnitSpaceAssignment` ;
- `Resource`, `Equipment`, `ResourceLocationAssignment`, `ResourceDowntime`, `ResourceReservation` ;
- `ProfessionalProfile`, `Employment`, `UnitAssignment`, `ClinicalPrivilege`, `DutyPeriod`, `Delegation` ;
- `CareEpisode`, `Encounter`, `PatientPresence`, `PatientMovement`, `WorkItem` ;
- `AdmissionRequest`, `Preadmission`, `HospitalStay`, `BedReservation`, `BedAssignment`, `DischargeProcess`, `TurnaroundTask`.

Les contraintes critiques seront portées par PostgreSQL : FK tenant cohérentes, index uniques partiels, exclusions de plages temporelles, version optimiste et journal d'événements append-only.

## 14. Workflows cibles

### 14.1 Admission programmée

```text
Demande médicale → validation clinique → préadmission administrative
→ recherche de compatibilité → réservation avec expiration
→ confirmation d'arrivée → affectation physique → séjour EN_COURS
```

Une réservation expirée ne devient jamais une occupation. Le claim final du lit est transactionnel.

### 14.2 Admission depuis les urgences

```text
Urgence active → décision d'orientation → demande d'aval
→ acceptation par unité/bed manager → lit ou zone d'attente sécurisé
→ transfert physique confirmé → hospitalisation active
→ urgence clôturée avec référence du handoff
```

La stabilisation clinique n'est pas synonyme de transfert réalisé.

### 14.3 Transfert interne

```text
Demande + motif → validation clinique/organisationnelle
→ réservation destination → préparation source/destination
→ départ physique → clôture présence source
→ arrivée confirmée → ouverture présence destination
→ ancien lit TO_CLEAN
```

La transaction garantit une seule présence active, tout en permettant un statut `IN_TRANSIT` court et audité.

### 14.4 Sortie

```text
Sortie médicale validée
→ ordonnance/documents/plan de suivi
→ validation administrative ou dette explicitement autorisée
→ départ physique confirmé
→ affectation clôturée
→ lit TO_CLEAN → CLEANING → READY/FREE
→ séjour clôturé
```

Les issues `DECEASED`, `TRANSFERRED_EXTERNAL`, `LEFT_AGAINST_MEDICAL_ADVICE` et `ABSCONDED` possèdent leurs propres préconditions et traces.

### 14.5 Maintenance et fermeture

Une fermeture planifiée bloque les nouvelles réservations mais ne déplace jamais silencieusement un patient. Un lit occupé ne peut être fermé qu'après plan de transfert validé ou situation exceptionnelle explicitement auditée.

## 15. Cycles de vie et statuts

### 15.1 Service/unité

| Transition | Acteur | Préconditions | Effets/trace | Retour arrière |
|---|---|---|---|---|
| DRAFT → ACTIVE | admin structure | code, type, établissement et responsable valides | date d'effet, audit, notification responsable | suspension seulement |
| ACTIVE → SUSPENDED | direction autorisée | motif et période | bloque nouvelles activités, conserve existantes | réactivation autorisée |
| ACTIVE/SUSPENDED → CLOSED | direction | aucun épisode non traité ou plan de migration | date de fin, alertes, archivage liens | réouverture par décision auditée |
| CLOSED → ARCHIVED | records admin | délais et dépendances vérifiés | lecture seule | exceptionnel |

### 15.2 Espace/chambre

Le cycle administratif est `DRAFT → ACTIVE → TEMPORARILY_CLOSED → CLOSED → ARCHIVED`. L'usage courant est calculé à partir des réservations, présences, tâches de nettoyage et indisponibilités ; il ne doit pas écraser le cycle administratif.

### 15.3 Lit : axes indépendants

| Axe | Valeurs |
|---|---|
| Existence | `PLANNED`, `INSTALLED`, `DECOMMISSIONED`, `ARCHIVED` |
| Ouverture | `OPEN`, `CLOSED`, `BLOCKED`, `MAINTENANCE` |
| Hygiène | `READY`, `TO_CLEAN`, `CLEANING`, `DISINFECTION`, `VALIDATION_REQUIRED` |
| Usage dérivé | `FREE`, `RESERVED`, `OCCUPIED`, `UNAVAILABLE` |

`FREE` n'est vrai que si le lit est installé, ouvert, prêt, sans réservation ferme ni occupation active. Les changements critiques exigent raison, acteur, timestamp, source et corrélation.

### 15.4 Hospitalisation

```text
REQUESTED → CLINICALLY_APPROVED → PLANNED → PREADMITTED → ADMITTED → IN_PROGRESS
→ MEDICALLY_DISCHARGED → ADMINISTRATIVELY_CLEARED → PHYSICALLY_DEPARTED → CLOSED
```

Branches : `CANCELLED`, `TRANSFERRED_EXTERNAL`, `DECEASED`, `LEFT_AGAINST_MEDICAL_ADVICE`, `ABSCONDED`. Une correction ne réécrit pas l'historique : elle ajoute un événement de rectification.

### 15.5 Réservation

`HELD → CONFIRMED → CONSUMED`, avec branches `EXPIRED`, `CANCELLED`, `NO_SHOW`. Toute réservation possède début, expiration, priorité, propriétaire, motif et règle de surbooking ; par défaut, surbooking interdit.

## 16. Règles métier recommandées

1. Un lit ne peut avoir qu'une occupation active et aucune période d'occupation ne peut se chevaucher.
2. Deux réservations fermes ne peuvent se chevaucher sur le même lit ; les holds expirent automatiquement.
3. Une affectation possède une date de début ; sa fin est strictement postérieure à son début.
4. Un lit n'est disponible que s'il est installé, ouvert, prêt, non bloqué, non réservé et non occupé.
5. Un lit fermé, en maintenance ou à nettoyer ne peut être réservé ni occupé.
6. Un statut `OCCUPIED` est dérivé d'une affectation active ; aucun endpoint générique ne le force.
7. Toute mutation de disponibilité passe par un use case backend transactionnel.
8. Une sortie médicale ne libère ni le patient ni le lit.
9. Le départ physique clôt l'affectation et déclenche le bionettoyage.
10. Le lit ne redevient disponible qu'après validation du nettoyage requise par la politique locale.
11. Tout transfert clôt la présence source et ouvre la destination sans chevauchement interdit.
12. Une destination doit être compatible avec âge, sexe, isolement, niveau de soins, équipement et politique locale.
13. Une chambre peut être affectée temporairement à une autre unité avec dates, motif et approbation.
14. La capacité ouverte peut être inférieure à la capacité installée et doit être historisée.
15. Un service fermé n'accepte plus de nouveaux épisodes, mais son historique demeure consultable.
16. Une suppression ne détruit jamais une occupation, un mouvement, une décision clinique ou un audit.
17. Le médecin responsable doit être actif, habilité, dans le bon tenant et affecté ou délégué au périmètre.
18. Profession, fonction et rôle applicatif sont des concepts séparés.
19. Toute affectation de personnel a un début, une fin facultative, une portée et un décideur.
20. Une permission clinique s'évalue avec le tenant, l'unité, l'affectation temporelle et la relation de soin.
21. Une urgence orientée vers l'hospitalisation reste en attente d'aval jusqu'à confirmation du handoff.
22. Les événements décès, départ non autorisé, contre avis et transfert externe sont distincts.
23. Toute opération sensible conserve acteur, rôle effectif, date, ancienne/nouvelle valeur, raison et corrélation.
24. Tous les temps métier sont enregistrés en UTC avec fuseau d'affichage explicite.
25. Les identifiants stables, jamais les noms, portent les relations ; un snapshot de libellé peut servir au document signé.
26. Une dispensation interne décrémente le lot de stock dans la même transaction, avec contrôle de concurrence.
27. Les transitions laboratoire suivent une machine à états et la validation exige un biologiste habilité.
28. Les files de tâches sont idempotentes et reprennent après coupure réseau sans dupliquer une admission ou un mouvement.

## 17. Écrans recommandés

| Écran | Utilisateurs | Informations/filtres | Actions, alertes et permissions | Liens |
|---|---|---|---|---|
| Organigramme | direction/admin | établissement, type, statut, dates | créer/lier/archiver selon `ORG_STRUCTURE_*` | unités, personnel, capacité |
| Services | direction/cadres | type, spécialités, responsables, statut | activer/suspendre/fermer ; alertes dépendances | fiche service |
| Fiche service | mêmes | unités, sites, capacités, équipes, KPI | affectations datées, audit | espaces, personnel |
| Unités | cadres/bed managers | service, niveau de soins, capacité | créer/fermer/renforcer | plan de lits |
| Cartographie | logistique/admin | sites, bâtiments, étages, zones | déplacer via affectation datée, jamais par réécriture | salles |
| Salles/fiche salle | cadres/logistique | type, équipements, calendrier, statut | réserver, fermer, changer usage | maintenance |
| Chambres/fiche | cadres | compatibilités, lits, hygiène | bloquer, isoler, affecter temporairement | plan de lits |
| Tableau/plan de lits | bed manager/cadres | axe installé-ouvert-prêt-usage | réserver, affecter, transférer ; alerte conflit | séjour/patient |
| Vue occupation | direction/cadres | unité/site/type/statut | drill-down, export autorisé | historique |
| Demandes d'hospitalisation | médecins/admissions | priorité, date, diagnostic, unité | approuver/refuser/planifier | préadmission |
| Admissions en attente | admissions/bed manager | readiness, paiement configurable, identité | confirmer arrivée/assigner | plan de lits |
| Réservations | bed manager | début, expiration, priorité | prolonger/annuler/convertir | patient/lit |
| Transferts internes | cadres/transport | source, destination, motif, SLA | accepter/démarrer/confirmer | mouvements |
| Lits à nettoyer | hygiène/cadres | depuis, protocole, priorité | démarrer/terminer/valider | chambre |
| Maintenance | technique/biomédical | panne, équipement, ETA | ouvrir/affecter/clôturer | ressources |
| Historique d'occupation | auditeurs autorisés | lit/patient/période/mouvement | lecture/export contrôlé | audit |
| Tableau capacité | direction | installé/ouvert/prêt/occupé/réservé | seuils et prévisions | alertes |
| Saturation | direction/bed manager | attente, taux, durée, alternatives | plan d'escalade | transferts externes |
| Affectation personnel | RH/cadres | profession, unité, dates, garde | affecter/remplacer/déléguer | planning |
| Ressources/équipements | biomédical/logistique | type, emplacement, disponibilité | réserver/maintenir/déplacer | salles |
| Parcours patient | équipe de soins | position, responsable, prochaine étape, tâches | accepter/terminer/escalader | tous modules |
| Bloc opératoire | bloc | programme, salle, équipe, matériel, SSPI | planifier/check-in/check-out | séjour |
| Labo/imagerie | professionnels dédiés | file, prélèvement/modalité, SLA | transitions contrôlées | parcours patient |

Chaque écran doit être FR/EN, light/dark, responsive, compatible faible débit, avec composants partagés et rayons sobres conformément à `DESIGN.md`.

## 18. Tableaux de bord et indicateurs

| Indicateur | Définition/formule | Données | Fréquence/profils | Seuil possible |
|---|---|---|---|---|
| Lits installés | `count(bed existence=INSTALLED)` | lit + dates d'effet | quotidien, direction | variation non planifiée |
| Lits ouverts | installés avec ouverture `OPEN` | états d'ouverture | temps réel, direction/cadres | < capacité planifiée |
| Lits prêts | ouverts et hygiène `READY` | lit + turnover | temps réel, bed manager | < 2 par unité configurable |
| Lits disponibles | prêts sans réservation/occupation | réservations + occupations | temps réel | 0 = saturation |
| Lits occupés | occupations actives à l'instant T | bed assignments | temps réel | selon unité |
| Taux d'occupation | occupés / lits ouverts × 100 | mêmes données | horaire/jour | >85 %, >95 % critique, à valider |
| Taux de disponibilité | disponibles / lits ouverts × 100 | états | temps réel | <10 % |
| Patients en attente | demandes acceptées sans affectation | demandes | temps réel | >0 au-delà SLA |
| Délai attribution | médiane(`assignedAt-requestedAt`) | demande/affectation | quotidien | par priorité |
| Durée moyenne séjour | somme durées séjours clos / sorties | séjours | mensuel, direction | comparaison service/case-mix |
| Rotation lit | sorties / lit ouvert sur période | séjours/capacité | mensuel | dérive vs cible |
| Délai remise en état | `readyAt-physicalDepartureAt` | turnover | temps réel/jour | > protocole local |
| Lits à nettoyer | count turnover non clos | turnover | temps réel | âge >30/60 min configurable |
| Lits maintenance | count ouverture=MAINTENANCE | downtime | temps réel | > x % unité |
| Sorties prévues/réalisées | nombre par date et statut | discharge | quotidien | retard > SLA |
| Sorties retardées | médicalement sorties sans départ après SLA | discharge | horaire | toute occurrence prolongée |
| Admissions non réalisées | `NO_SHOW/CANCELLED` / planifiées | réservation/admission | hebdo | tendance |
| Transferts | count et délai par type | mouvements | quotidien | attente > SLA |
| Saturation | minutes au-dessus du seuil d'occupation | snapshots | quotidien | >60 min configurable |
| Utilisation salles | temps occupé / temps ouvert | réservations/présences | quotidien/mensuel | >90 ou <30 % à analyser |
| Bloc opératoire | temps incision/occupation / temps ouvert | programme/événements | quotidien | retard/débordement |
| Conflits réservation | tentatives refusées pour chevauchement | audit de concurrence | quotidien, ops/IT | >0 récurrent |
| Charge équipe | patients pondérés / ETP présent | présence/roster/acuité | temps réel, cadre | seuil validé localement |
| Labo SLA | résultats dans délai / résultats | ordres/étapes | quotidien | par priorité/examen |

Les seuils donnés sont des exemples de configuration, pas des normes. Ils doivent être validés par la direction médicale et le cadre infirmier pour chaque type d'unité.

## 19. Scénarios de test

Le plan détaillé, au format demandé avec préconditions, profils, étapes, sécurité, erreurs et données historisées, se trouve dans [TEST-PLAN.md](TEST-PLAN.md). Il contient 34 scénarios couvrant les 28 cas minimaux demandés, plus concurrence, RBAC contextuel, reprise réseau, handoff urgence et cohérence des KPI.

Les suites prioritaires sont :

- contraintes PostgreSQL de non-chevauchement ;
- transactions admission/transfert/sortie/nettoyage ;
- tests E2E multi-rôles ;
- migration et réconciliation des données `wards/rooms/beds` ;
- tests de charge sur capacité et réservation ;
- tests de confidentialité par site/unité/relation de soin.

## 20. Backlog des améliorations

Le backlog détaillé est [EPIC-0027 — Organisation hospitalière, capacité et parcours patient](../../pm/backlog/EPIC-0027-hospital-organization-capacity-patient-flow.md). Il respecte `EPIC → User Stories → Tasks → Subtasks` et contient critères d'acceptation, DoR, DoD, reviewers, tests, dépendances et estimations.

Ordre recommandé :

1. invariants de lit, calcul de disponibilité et séparation des permissions ;
2. modèle organisationnel/géographique et migration compatible ;
3. demandes, réservations, admissions, mouvements et sorties ;
4. personnel, ressources, bloc, laboratoire/imagerie/pharmacie ;
5. parcours transverse, dashboards, interopérabilité et résilience.

## 21. Feuille de route de mise en œuvre

| Phase | Valeur métier | Priorité | Dépendances | Complexité | Profils | Effort indicatif | Critères de sortie |
|---|---|---|---|---|---|---:|---|
| 0 — Guardrails critiques | évite faux libres, désynchronisations et droits excessifs | Critique | DBA/RSSI/cadre | Élevée | senior backend, DBA, Angular, QA | 12–16 j | invariants DB, API sans statut direct, KPI corrigé, RBAC signé |
| 1 — Référentiels | représente petites structures et réseaux | Haute | ADR + migration | Très élevée | architecte, backend, frontend, DBA | 24–32 j | organisation et géographie flexibles, legacy migré |
| 2 — Hospitalisation | sécurise réservation, mouvements et sortie | Critique/Haute | phases 0–1 | Très élevée | backend, Angular, métier, QA | 28–38 j | parcours E2E et concurrence validés |
| 3 — Personnel/ressources | sait qui travaille où et avec quoi | Haute | référentiels | Très élevée | RH/cadre/biomédical/dev | 20–28 j | affectations datées, ressources réservables |
| 4 — Pilotage/interop | capacité fiable et parcours visible | Haute/Moyenne | événements consolidés | Élevée | data, backend, frontend, interop | 18–26 j | KPI réconciliés, alertes, mappings standards |

**Total réestimé** : 103–143 jours-personnes, documentation et QA incluses, après découpage des invariants de lit. Avec une capacité planifiable de 16 à 20 jours-personnes par sprint, l'ordre de grandeur reste 6 à 9 sprints. La capacité réelle, les absences et la disponibilité des reviewers métier ne sont pas connues : aucune date d'engagement ne doit être déduite de cette fourchette.

Chaque phase exige : spécification mise à jour, migration réversible ou plan de restauration, tests unitaires/intégration/E2E, revue sécurité, observabilité, guide utilisateur et validation métier. Les travaux imagerie et bloc peuvent devenir des epics dédiés après les fondations.

## 22. Conclusion et recommandations prioritaires

Joprelys dispose d'une base clinique exploitable, en particulier pour l'identité provisoire et la continuité urgence-hospitalisation, mais son modèle spatial est encore celui d'une petite structure mono-site. L'étendre par ajout de champs à `Ward`, `Room` et `Bed` amplifierait les confusions actuelles.

Les décisions prioritaires sont :

1. corriger immédiatement le calcul de lits libres et bloquer les transitions de lit incohérentes ;
2. imposer l'intégrité temporelle des réservations/occupations en base ;
3. séparer décision médicale, clearance administrative, départ physique et remise en état ;
4. adopter l'ADR proposé séparant organisation, géographie et capacité ;
5. remplacer les champs texte de rattachement par des liens datés, en conservant des snapshots pour les documents signés ;
6. introduire un suivi transverse de présence, responsabilité et prochaine étape ;
7. faire signer les workflows par un médecin responsable, un cadre infirmier, les admissions, le biomédical, le laboratoire, la pharmacie, le DPO/RSSI et la direction financière.

L'audit ne valide pas l'ouverture clinique ou réglementaire de ces fonctionnalités. Une phase d'ateliers terrain et une recette sur données anonymisées sont obligatoires avant mise en production.

## Annexes — preuves principales du dépôt

- Schéma spatial : `backend/src/main/resources/db/migration/V44__create_spatial_tables.sql`, V69, V74.
- Domaine spatial : `WardEntity`, `RoomEntity`, `BedEntity`, `BedAssignmentEntity`, `SpatialService`.
- Admission/sortie : `HospitalizationAdmissionService`, `HospitalizationService`, `HospitalizationEntity`.
- Urgences : `EmergencyEntity`, triage V64/V65, identité provisoire V58, continuité V75.
- Personnel/RBAC : `UserAccountEntity`, `StaffService`, `RbacCatalog`, V41, V57.
- Parcours : `VisitEntity`, `VisitService`, `LabOrderService`, `PharmacyService`.
- UI : routes Angular, `spatial-management-page`, `spatial-configuration-page`, `patient-hospitalization`, `emergency-dashboard`, admission unifiée.
- Tests : `SpatialControllerTest`, `HospitalizationAdmissionServiceTest`, `HospitalizationControllerTest`, tests urgences et composants Angular associés.
