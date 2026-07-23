# FUNCTIONAL-SPEC — HOS-LOC-001-A Géographie et espaces hospitaliers

## 1. Besoin métier

Joprelys doit représenter la géographie physique réelle d'un établissement sans l'assimiler à son organisation médicale.

Un service peut exercer dans plusieurs espaces, plusieurs bâtiments ou plusieurs sites. Un espace peut également être partagé entre plusieurs services. Une salle de consultation, un box d'urgence ou un laboratoire ne doivent jamais être modélisés comme une fausse chambre d'hospitalisation.

## 2. Concepts

### 2.1 Géographie

Hiérarchie facultative :

```text
Établissement
└── Site ?
    └── Bâtiment ?
        └── Étage ?
            └── Zone ?
```

Tous les niveaux sont omissibles.

Exemples valides :

```text
Cabinet
└── Espace Consultation 1
```

```text
Clinique
└── Bâtiment principal
    └── RDC
        └── Espace Box urgence 1
```

```text
CHU
└── Site principal
    └── Bâtiment A
        └── Étage 2
            └── Zone Hospitalisation
                └── Espace Chambre 201
```

### 2.2 Espace

Un `SPACE` est une instance physique utilisable.

Exemples :

- Consultation 01 ;
- Salle d'attente principale ;
- Box urgence 02 ;
- Chambre 101 ;
- Bloc 3 ;
- Laboratoire central ;
- Pharmacie centrale.

Son **type** provient d'un catalogue contrôlé. Son **nom** et son **code interne** appartiennent à l'établissement.

### 2.3 Profil d'hébergement

Un espace pouvant contenir des lits possède un profil d'hébergement explicite. Le simple fait d'être un `SPACE` ne permet pas de créer des lits.

La capacité installée d'un espace est dérivée des lits qui lui sont rattachés ; aucune seconde valeur `capacity` redondante n'est conservée dans ce lot.

### 2.4 Rattachement organisationnel

Les services et unités viennent de HOS-ORG / V87.

Une relation datée relie une unité organisationnelle à un espace :

```text
Service Médecine générale
   ├── Consultation 01
   ├── Consultation 02
   └── Chambre 201

Urgences
   ├── Box urgence 01
   ├── Box urgence 02
   └── Imagerie 01 (partagée)
```

Le même espace peut être rattaché à plusieurs unités simultanément lorsque le partage est volontaire.

## 3. Types d'espace initiaux

Catalogue contrôlé minimal :

- `CONSULTATION_ROOM` — salle de consultation ;
- `TREATMENT_ROOM` — salle de soins/examen ;
- `EMERGENCY_BOX` — box d'urgence ;
- `WAITING_ROOM` — salle d'attente ;
- `HOSPITAL_ROOM` — chambre d'hospitalisation ;
- `OPERATING_ROOM` — salle opératoire ;
- `RECOVERY_ROOM` — SSPI / salle de réveil ;
- `ICU_ROOM` — réanimation / soins intensifs ;
- `DELIVERY_ROOM` — salle d'accouchement ;
- `NEONATAL_ROOM` — espace néonatal ;
- `LABORATORY_ROOM` — laboratoire ;
- `IMAGING_ROOM` — imagerie ;
- `PHARMACY` — pharmacie ;
- `STORAGE` — stockage ;
- `OFFICE` — bureau ;
- `MORGUE` — morgue ;
- `OTHER_CONTROLLED` — autre espace contrôlé.

Dans ce lot, seuls `HOSPITAL_ROOM` et `ICU_ROOM` sont compatibles avec les lits d'hospitalisation standards. Les autres types pourront recevoir d'autres ressources dans HOS-RES sans détourner l'entité `Bed`.

## 4. Règles de hiérarchie géographique

- RF-LOC-01 : un nœud appartient à un seul tenant.
- RF-LOC-02 : types : `SITE`, `BUILDING`, `FLOOR`, `ZONE`.
- RF-LOC-03 : tous les niveaux sont facultatifs.
- RF-LOC-04 : un parent doit être d'un niveau supérieur : SITE > BUILDING > FLOOR > ZONE.
- RF-LOC-05 : les niveaux intermédiaires peuvent être sautés : SITE peut contenir directement FLOOR ou ZONE ; BUILDING peut contenir directement ZONE.
- RF-LOC-06 : un nœud ne peut pas être son propre ancêtre.
- RF-LOC-07 : le code d'un nœud est unique par tenant.
- RF-LOC-08 : un parent cross-tenant est interdit.
- RF-LOC-09 : une structure utilisée est désactivée plutôt que supprimée physiquement.

## 5. Règles des espaces

- RF-SPC-01 : un espace appartient à un tenant.
- RF-SPC-02 : le code d'espace est unique par tenant.
- RF-SPC-03 : son `locationNodeId` est facultatif.
- RF-SPC-04 : un espace rattaché à un nœud utilise un nœud du même tenant.
- RF-SPC-05 : son type est issu du catalogue contrôlé.
- RF-SPC-06 : un espace inactif n'est plus proposé pour de nouvelles affectations/admissions.
- RF-SPC-07 : un espace historiquement utilisé est désactivé, pas supprimé.
- RF-SPC-08 : l'activation exige un parent géographique actif le cas échéant.

## 6. Règles des lits

- RF-BED-01 : un lit appartient à un seul `SPACE` d'hébergement.
- RF-BED-02 : un lit ne peut pas être créé dans une consultation, une salle d'attente, un laboratoire, une pharmacie ou un bloc.
- RF-BED-03 : unicité `(tenant, space, bedNumber)`.
- RF-BED-04 : les états capacité/préparation/occupation déjà durcis restent inchangés.
- RF-BED-05 : un lit possède une identité UUID stable ; son numéro est un libellé métier, jamais une clé globale.

## 7. Rattachements unité ↔ espace

- RF-USE-01 : relation N:N datée.
- RF-USE-02 : unité et espace doivent appartenir au même tenant.
- RF-USE-03 : `validFrom` obligatoire ; `validTo` facultatif.
- RF-USE-04 : `validTo > validFrom` si renseigné.
- RF-USE-05 : deux périodes du même couple unité/espace ne peuvent pas se chevaucher.
- RF-USE-06 : plusieurs unités distinctes peuvent utiliser simultanément le même espace.
- RF-USE-07 : une nouvelle admission doit utiliser une relation active entre `serviceUnitId` et `spaceId`.

## 8. Admission

Le nouveau parcours ne demande plus à l'utilisateur de taper ou transmettre :

```text
serviceName
roomNumber
bedNumber
```

Le payload contient :

```text
serviceUnitId
spaceId
bedId
```

Le backend :

1. vérifie le tenant ;
2. vérifie que `serviceUnitId` est actif et de type `SERVICE` ou unité compatible définie par le contrat ;
3. vérifie que l'espace est actif et rattaché à l'unité à l'instant de l'admission ;
4. vérifie que le lit appartient à cet espace ;
5. réclame le lit atomiquement ;
6. crée le séjour avec les UUID structurés ;
7. dérive les snapshots lisibles depuis les référentiels.

Aucun texte fourni par le client n'est utilisé comme source d'identité.

## 9. Transfert

Un `bedId` seul ne suffit pas : un espace peut être partagé entre plusieurs services.

Le transfert contient donc :

```text
hospitalizationId
targetServiceUnitId
targetSpaceId
targetBedId
```

Le backend valide le rattachement actif unité/espace puis met à jour les références courantes du séjour et l'affectation de lit.

## 10. Hospitalisation : références et snapshots

Le séjour conserve comme sources de vérité :

- `currentServiceUnitId` ;
- `currentSpaceId` ;
- `currentBedId`.

Les chaînes lisibles restent des snapshots explicites :

- `serviceNameSnapshot` ;
- `spaceNameSnapshot` ;
- `bedNumberSnapshot`.

Le transfert met à jour les références courantes et les snapshots courants. L'historique temporel du lit reste porté par `bed_assignments` ; HOS-MOV complétera ultérieurement l'historique de présence/mouvement.

## 11. Cycle de vie / suppression

- géographie : activation/désactivation ;
- espaces : activation/désactivation ;
- rattachements : clôture `validTo` ;
- lits : fermeture capacité/états existants, pas de suppression si historique ;
- aucun DELETE physique public pour un objet déjà référencé.

## 12. UX mobile-first

Configuration en trois zones :

1. **Géographie** — arbre Site/Bâtiment/Étage/Zone ;
2. **Espaces** — cartes filtrables par localisation/type ;
3. **Utilisation & capacité** — unités utilisatrices et lits pour espaces compatibles.

Contraintes :

- 320/375 px : cartes empilées, formulaire plein écran/bottom-sheet, actions non compressées ;
- 768+ : master/detail ;
- 1366+ : arbre + détail sans surcharge ;
- FR/EN ;
- light/dark via tokens ;
- rayons 4–8 px ;
- aucun Angular Material ;
- labels/focus clavier ;
- aucun texte utilisateur hardcodé dans les composants.

## 13. Hors périmètre

- équipements médicaux et réservation de salles : HOS-RES ;
- géolocalisation GPS/cartographie ;
- capacité planifiée temporelle complète : HOS-BED-002 ;
- affectations personnel : HOS-STAFF ;
- mouvement patient complet : HOS-MOV ;
- ABAC unité/relation de soin : HOS-RBAC reliquat.
