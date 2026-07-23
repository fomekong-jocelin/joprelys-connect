# FUNCTIONAL-SPEC — HOS-ORG-001-A Référentiels organisationnels hospitaliers

## 1. Besoin métier

Joprelys doit permettre de représenter l'organisation médicale d'un établissement sans confondre cette organisation avec sa géographie et sans recourir à des champs texte libres pour les types de service ou les spécialités.

## 2. Modèle fonctionnel

Hiérarchie facultative :

```text
Établissement
└── Pôle (optionnel)
    └── Département (optionnel)
        └── Service
            └── Unité de soins (optionnelle)
```

Une petite clinique peut omettre Pôle et Département :

```text
Établissement
├── Médecine générale
├── Maternité
├── Pédiatrie
└── Urgences
```

## 3. Règles métier

- RF-01 : chaque unité appartient à un seul tenant.
- RF-02 : les niveaux sont `POLE`, `DEPARTMENT`, `SERVICE`, `CARE_UNIT`.
- RF-03 : un `SERVICE` utilise obligatoirement un code issu du catalogue des services.
- RF-04 : un `SERVICE` ne stocke aucun libellé localisé propre ; son nom visible est résolu depuis le catalogue FR/EN selon la langue active. L'UI ne demande donc jamais de nom de service libre.
- RF-05 : un `POLE`, `DEPARTMENT` ou `CARE_UNIT` peut avoir un libellé propre à l'établissement.
- RF-06 : les parents autorisés sont : établissement→POLE/DEPARTMENT/SERVICE ; POLE→DEPARTMENT/SERVICE ; DEPARTMENT→SERVICE ; SERVICE→CARE_UNIT. Une CARE_UNIT n'a pas d'enfant dans ce lot.
- RF-07 : un code d'unité est unique dans un tenant.
- RF-08 : une unité ne change jamais de tenant.
- RF-09 : une unité utilisée n'est pas supprimée physiquement ; elle est désactivée.
- RF-10 : une unité désactivée reste visible dans l'historique mais n'est plus proposée pour de nouvelles affectations.
- RF-11 : les spécialités médicales sont sélectionnées depuis un catalogue codifié.
- RF-12 : aucune saisie libre de spécialité n'est ajoutée dans cette fonctionnalité.
- RF-13 : dans HOS-ORG-001-A, lecture et gestion de cette configuration nécessitent `ORGANIZATION_STRUCTURE_MANAGE`. Les futurs parcours métier disposeront de contrats de lecture adaptés plutôt que d'élargir implicitement l'API d'administration.
- RF-14 : l'API est l'autorité finale ; Angular peut filtrer les options pour l'ergonomie mais ne remplace aucune validation backend.

## 4. Catalogue initial de services

Le catalogue initial doit au minimum couvrir les besoins de la démo et les services hospitaliers courants :

- Médecine générale ;
- Médecine interne ;
- Maternité / gynécologie-obstétrique ;
- Pédiatrie ;
- Urgences ;
- Chirurgie générale ;
- Cardiologie ;
- Réanimation / soins intensifs ;
- Anesthésie ;
- Bloc opératoire ;
- Laboratoire ;
- Imagerie ;
- Pharmacie ;
- Hospitalisation polyvalente.

Les codes sont stables et indépendants des libellés FR/EN.

## 5. Catalogue initial de spécialités

Catalogue minimal : médecine générale, médecine interne, pédiatrie, gynécologie-obstétrique, cardiologie, chirurgie générale, anesthésie-réanimation, radiologie/imagerie, biologie médicale, pharmacie hospitalière.

Ce catalogue est extensible par migration/application maîtrisée ; il n'est pas modifiable librement par un utilisateur clinique dans ce lot.

## 6. Parcours administrateur

1. ouvrir Configuration de l'établissement ;
2. ouvrir Organisation hospitalière ;
3. visualiser l'arbre existant ;
4. créer un pôle/département facultatif ou créer directement un service ;
5. pour un service, sélectionner un type dans le catalogue ;
6. ajouter éventuellement une unité de soins ;
7. désactiver/réactiver une unité selon les droits et contraintes ;
8. voir les unités actives/inactives sans ambiguïté.

## 7. UX/UI

- mobile-first ;
- liste/arbre transformé en cartes empilées sur 320/375 px ;
- indentation progressive sur tablette/desktop ;
- formulaires dans panneaux compacts ;
- actions au niveau du parent ;
- rayons 4–6 px, 8 px maximum ;
- ombres légères ;
- thèmes light/dark ;
- i18n FR/EN ;
- service affiché dans la langue active sans dupliquer le libellé dans l'unité ;
- focus visible ;
- aucune chaîne visible hardcodée.

## 8. Hors périmètre

- géographie `Site/Bâtiment/Étage/Zone/Espace` : HOS-LOC-001-A ;
- affectations du personnel : HOS-STAFF-001-A ;
- suppression physique des anciens `wards` ;
- ABAC final par unité ;
- gestion libre des catalogues par les établissements.
