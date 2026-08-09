# DIAG-20260809 — Parcours praticien : urgence/admission jusqu’à l’attribution du lit

## Mode d’intervention

Diagnostic fonctionnel et métier, avec revue UX/accessibilité limitée par l’impossibilité de capturer l’application locale dans le navigateur.

## Statut

CORRECTIFS BORNÉS IMPLÉMENTÉS — cohérence des lits, récupération documentaire et validation du praticien responsable ; workflow d’admission structurant encore à construire.

## Demande métier

Évaluer, du point de vue d’un praticien, le parcours d’un patient depuis son arrivée/urgence jusqu’à l’attribution d’un lit d’hospitalisation et identifier ce qui manque pour une admission sûre, lisible et opérable.

## Périmètre observé

- urgence, triage, réévaluation et stabilisation ;
- liaison urgence → dossier patient → hospitalisation ;
- sélection unité/service → espace → lit → praticien responsable ;
- création du séjour et occupation du lit ;
- continuité documentaire immédiate après admission.

## Parcours réellement présent

1. Création ou ouverture du patient, y compris identité provisoire d’urgence.
2. Saisie de l’arrivée, du triage, des constantes initiales, des actes de réanimation et des éléments médico-légaux.
3. Stabilisation avec orientation `ADMISSION` ou `OR_DIRECT`.
4. Navigation vers le dossier patient avec `emergencyId` conservé dans l’URL.
5. Chargement des unités actives, espaces d’hébergement affectés et lits disponibles.
6. Sélection du service/unité, de l’espace, du lit, du praticien responsable et du motif.
7. Création transactionnelle du séjour, création/réutilisation de la visite, claim du lit et affectation active.
8. Tentative de génération du lot documentaire d’urgence après la création du séjour.

## Constats prioritaires praticien

| ID | Priorité | Manque / risque | Preuve | Conséquence clinique ou opérationnelle |
|---|---|---|---|---|
| GAP-PAT-01 | P0 | Pas de demande d’admission, préadmission ou réservation distincte : le bouton final crée directement le séjour et passe le lit à `OCCUPIED`. | `HospitalizationAdmissionService.admitPatient`, `claimConfiguredBed`; EPIC-0027/HOS-ADM-001 | Le lit est consommé avant confirmation d’arrivée ; impossible de distinguer décision médicale, lit réservé, patient en route et patient installé. |
| GAP-PAT-02 | P0 | Pas d’acceptation formelle par l’unité receveuse ni de handoff structuré. | `EmergencyHospitalizationContinuationComponent` ne collecte que unité/espace/lit/praticien/motif ; EPIC-0027/HOS-MOV-001 | Aucun responsable receveur, délai, consigne de transfert ou preuve de transmission n’est visible. |
| GAP-PAT-03 | P0 | Compatibilité clinique du lit absente. | Le filtrage front porte sur `available`, `OPEN` et `READY`; aucun contrôle âge, sexe, isolement, niveau de soins, équipement, infection ou cohorte. | Risque d’affecter un lit techniquement libre mais cliniquement inadapté. |
| GAP-PAT-04 | P0 | Praticien responsable filtré par rôle seulement et non validé par habilitation/affectation à l’unité. | Correctif appliqué : le frontend ne propose que `MEDECIN` et `HospitalizationAdmissionService` vérifie tenant, compte actif et affectation datée à l’unité. | Le cas est maintenant refusé côté backend ; la validation métier des rôles d’affectation reste à confirmer avec les métiers. |
| GAP-PAT-05 | P0 | Contexte clinique insuffisant au moment du choix du lit. | Le panneau reçoit `patientId`, `emergencyId`, statut d’identité et numéro provisoire ; il n’affiche pas triage, constantes, diagnostic, allergies, isolement ou niveau de soins. | Le choix spatial est découplé des informations nécessaires à une décision de placement sûre. |
| GAP-PAT-06 | P1 | Aucun scénario « aucun lit » : pas de liste d’attente, overflow, transfert, escalade, délai attendu ni alternative. | Le composant affiche uniquement « Aucun lit disponible dans cet espace ». | Le praticien reste sans prochaine action lorsque l’hospitalisation est médicalement décidée mais matériellement impossible. |
| GAP-PAT-07 | P1 | Présence et jalons de transport/arrivée non modélisés avant l’installation. | Le modèle actuel crée directement l’hospitalisation et l’affectation ; HOS-PATH-001/HOS-MOV-001 sont encore proposés. | Pas de visibilité sur « en attente de lit », « transporté », « arrivé », « installé » ou « handoff accepté ». |
| GAP-PAT-08 | P1 | Checklist de sécurité d’entrée absente du point de décision. | Le formulaire ne demande pas précautions d’isolement, oxygène/équipements, risque de chute, dispositifs, allergies critiques, consentements ou effets personnels. | Des prérequis importants ne sont ni confirmés ni transmis à l’unité receveuse. |
| GAP-PAT-09 | P1 | Filtrage incohérent entre admission normale et continuité urgence. | Correctif appliqué : les deux composants utilisent `isAdmissibleBed()` (`available + OPEN + READY`). | Les lits fermés ou non prêts ne sont plus proposés par le parcours normal. |
| GAP-PAT-10 | P1 | Le warning documentaire post-admission n’était vraisemblablement pas visible. | Correctif appliqué : l’événement `admitted` est différé après succès documentaire et un bouton de régénération est présenté en cas d’échec. | Une seule admission est conservée et le rattrapage reste opérable. |
| GAP-PAT-11 | P1 | Liste de lits chargée sans mécanisme de fraîcheur explicite ni récupération guidée après collision. | Les listes sont chargées au démarrage/à la sélection ; le rejet 409 est seulement affiché côté admission normale. | Deux utilisateurs voient le même lit ; après conflit, l’utilisateur doit deviner qu’il faut relancer la recherche. |
| GAP-PAT-12 | P2 | Traçabilité de l’intention avant l’admission absente. | L’audit backend trace l’action `ADMISSION`, mais pas la demande, la recommandation, l’acceptation, la réservation ou le handoff. | Les arbitrages de capacité et les responsabilités pré-admission sont difficiles à reconstituer.

## Ce qui est déjà correctement sécurisé

- lien urgence/patient contrôlé par le contexte canonique et le tenant ;
- prévention d’une seconde hospitalisation active ;
- claim atomique du lit et rejet des collisions tardives ;
- vérification backend de l’unité, de l’espace, de l’affectation unité-espace et de l’appartenance du lit ;
- séparation progressive des permissions d’admission, soins, médicaments, sortie et départ physique ;
- continuité des identités provisoires et conservation de `emergencyId`.

## Action plan

- [x] Lire les règles de gouvernance et les documents existants.
- [x] Reconstituer le parcours urgence/admission/lit dans le code et la documentation.
- [x] Identifier les manques métier, sécurité, UX et cohérence front/backend.
- [x] Classer les risques par priorité.
- [x] Documenter les preuves et les limites d’audit.
- [x] Unifier le filtre d’affichage des lits avec le claim backend (`available + OPEN + READY`).
- [x] Maintenir la continuité urgence visible après échec documentaire et permettre la régénération sans seconde admission.
- [x] Valider côté backend le tenant, l’état actif, le rôle médecin et l’affectation active du praticien responsable.
- [x] Ajouter les spécifications fonctionnelle, technique et plan de tests du durcissement borné.
- [ ] Faire valider les gaps P0 par médecin responsable, cadre infirmier, admissions et bed manager.
- [ ] Découper la suite en `HOS-ADM-001` → `HOS-MOV-001` → `HOS-PATH-001` avec critères d’acceptation et modèle de statuts.
- [ ] Écrire les scénarios E2E multi-profils, concurrence, absence de lit et handoff.

## Critères d’acceptation de la suite

1. Une décision d’hospitalisation peut exister sans occuper immédiatement un lit.
2. Une réservation de lit possède une expiration, un acteur, un motif et un état explicite.
3. L’arrivée et l’acceptation par l’unité sont confirmées avant l’occupation définitive.
4. Le moteur backend refuse un lit incompatible avec les contraintes cliniques et les habilitations du praticien.
5. L’absence de lit fournit une file/alternative/escalade et une prochaine action.
6. L’utilisateur voit distinctement l’état de la demande, du lit, du transfert, du handoff et des documents.
7. Les parcours admission normale et urgence appliquent les mêmes règles de disponibilité et de préparation.
8. Chaque transition est idempotente, auditée et couverte par tests négatifs et concurrence.

## Vérifications réalisées

- Build Angular de développement réussi pendant le démarrage de `ng serve`.
- Lecture croisée des routes, composants Angular, services Spring, DTO, documentation fonctionnelle, backlog et audit existant.
- Vérification de la présence de `proxy.conf.json` et de son référencement dans `angular.json`.
- Correctifs applicatifs bornés ajoutés : filtre de lits partagé, récupération documentaire et contrôle serveur du praticien responsable.
- Audit visuel non finalisable : l’accès du navigateur à `http://127.0.0.1:4200` a été refusé par la permission navigateur ; aucune capture n’est donc utilisée comme preuve.

## Sécurité / régression

Le risque principal n’est pas une fuite de données mais une mauvaise décision de placement et une responsabilité clinique insuffisamment contextualisée. Toute implémentation doit conserver le backend comme source de vérité, vérifier les habilitations datées, préserver l’isolation tenant et tester les collisions ainsi que les doubles soumissions.

## Impact planning

Ce diagnostic confirme que le sujet dépasse un correctif isolé. Il doit rester rattaché à `EPIC-0027` et être découpé au minimum en :

- `HOS-ADM-001` : demande, décision, préadmission, réservation et arrivée ;
- `HOS-MOV-001` : présence, transport, handoff et transferts ;
- `HOS-PATH-001` : timeline, propriétaire, échéance et prochaine action ;
- correctifs bornés P1 : disponibilité uniforme des lits et visibilité documentaire.

L’estimation et la capacité restent à établir par le Product Manager après validation métier ; aucun engagement de date n’est pris dans ce diagnostic.

## Impact version / SemVer

Le durcissement est candidat `MINOR` car une validation métier supplémentaire est maintenant appliquée à l’admission ; aucun bump ni release n’est préparé. La future introduction de statuts de réservation, nouveaux endpoints ou changements de contrat devra être évaluée au minimum en `MINOR`, et possiblement `MAJOR` si les contrats existants sont cassés.

## Reste à faire

Valider les gaps P0 avec les métiers, choisir le workflow cible et préparer la documentation fonctionnelle/technique/API/data/test avant le développement de `HOS-ADM-001`, `HOS-MOV-001` et `HOS-PATH-001`.
