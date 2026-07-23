# HOS-STAFF-001-A — Spécialités structurées et affectations datées du personnel

## Métadonnées

- Issue GitHub : #132
- Epic : EPIC-0027 / HOS-STAFF-001
- Dépendances : HOS-ORG-001-A / #130 **DONE**, HOS-LOC-001-A / #131 **DONE**
- Baseline de départ : `main@e495477ea02beb05596b20b656bc092bd8fbbd83`
- Branche : `feat/132-hos-staff-001-a`
- Statut : IN_PROGRESS — cadrage + implémentation
- Priorité : P0 avant répétition finale #127
- Estimation : 9 SP
- Profil : senior full-stack sécurité / données RH clinique
- Reviewers : Tech Lead + RH + cadre hospitalier + RSSI/DPO + Product

## Objectif

Remplacer les champs libres `department` et `specialty` du personnel par des références structurées et historisées, afin que les droits, l'organisation clinique, les rendez-vous, les admissions et les futurs plannings puissent raisonner sur des identités stables plutôt que sur des chaînes de caractères.

## Principes non négociables

1. `StaffMember` / utilisateur reste une identité de personne ; une unité organisationnelle n'est jamais encodée dans son libellé.
2. Un membre du personnel peut être affecté à plusieurs unités, avec dates de validité et rôle d'affectation.
3. Une spécialité médicale vient exclusivement de `medical_specialty_catalog` créé par HOS-ORG.
4. Les affectations organisationnelles ciblent `organizational_units` créé par HOS-ORG.
5. Aucun mapping automatique par ressemblance de `users.department` ou `users.specialty`.
6. Toute donnée legacy ambiguë doit provoquer un diagnostic explicite ; aucun fallback silencieux.
7. Les API/UI de création et modification du personnel ne doivent plus avoir besoin de texte libre `department/specialty`.
8. Tenant isolation obligatoire côté application et DB.
9. Historisation : pas de réécriture destructive des affectations passées.
10. Aucun déploiement PROD/RECETTE dans ce lot.

## Modèle cible

```text
User / Staff Member
├── StaffSpecialtyAssignment
│   ├── specialtyCode → medical_specialty_catalog.code
│   ├── isPrimary
│   ├── validFrom
│   └── validTo?
│
└── StaffOrganizationalUnitAssignment
    ├── organizationalUnitId → organizational_units.id
    ├── assignmentRoleCode
    ├── isPrimary
    ├── validFrom
    └── validTo?
```

### Spécialités

- plusieurs spécialités possibles si le métier l'exige ;
- au plus une spécialité principale active par membre ;
- code contrôlé, libellé FR/EN résolu depuis le catalogue ;
- aucune spécialité libre persistée dans le nouveau flux.

### Affectations organisationnelles

- plusieurs unités possibles ;
- au plus une affectation principale active par membre ;
- une unité désactivée ne peut pas recevoir une nouvelle affectation active ;
- les périodes historiques restent consultables ;
- les affectations cross-tenant sont interdites par l'application et la base.

## Découpage

### Task A1 — data / migration / invariants — 3 SP

- [ ] analyser les champs legacy `users.department` / `users.specialty` et leurs consommateurs ;
- [ ] créer `staff_specialty_assignments` ;
- [ ] créer `staff_organizational_unit_assignments` ;
- [ ] ajouter contraintes tenant et FK vers les référentiels HOS-ORG ;
- [ ] empêcher plusieurs affectations principales actives du même type ;
- [ ] définir le preflight des données legacy ambiguës ;
- [ ] aucun mapping automatique par nom.

### Task A2 — backend / contrats / sécurité — 3 SP

- [ ] repositories + services métier ;
- [ ] lecture des spécialités et affectations actives/historiques ;
- [ ] création/modification/clôture des affectations ;
- [ ] intégrer les références structurées dans le flux staff ;
- [ ] supprimer le besoin des champs libres `department/specialty` dans les contrats d'écriture ;
- [ ] protéger les mutations par permission appropriée ;
- [ ] tenant isolation et contrôles cross-tenant ;
- [ ] tests unitaires + intégration + PostgreSQL.

### Task A3 — Angular / migration consommateurs / QA — 3 SP

- [ ] modèles et services Angular structurés ;
- [ ] écran staff : sélecteurs de spécialités et unités depuis référentiels actifs ;
- [ ] affichage des affectations datées et de l'affectation principale ;
- [ ] supprimer les champs texte libres des formulaires actifs ;
- [ ] migrer les consommateurs staff/appointments/hospitalization qui lisent encore `department/specialty` ;
- [ ] FR/EN, light/dark, responsive ;
- [ ] tests Angular + build production.

## Critères d'acceptation

- [ ] aucun nouveau staff ne peut enregistrer un `department` libre ;
- [ ] aucune spécialité libre n'est enregistrée dans le nouveau flux ;
- [ ] seules les spécialités actives du catalogue sont sélectionnables ;
- [ ] seules les unités organisationnelles actives du tenant sont assignables ;
- [ ] un staff peut avoir plusieurs affectations datées ;
- [ ] une affectation principale active est identifiable sans ambiguïté ;
- [ ] une spécialité principale active est identifiable sans ambiguïté ;
- [ ] les historiques ne sont pas détruits lors d'un changement d'unité/spécialité ;
- [ ] les affectations cross-tenant sont refusées ;
- [ ] aucun mapping automatique des anciennes chaînes par similarité ;
- [ ] les consommateurs actifs n'ont plus besoin de `users.department/users.specialty` comme identité métier ;
- [ ] Maven strict + PostgreSQL/Testcontainers + tests Angular + build production verts ;
- [ ] documentation, tracking et changelog alignés ;
- [ ] aucune action PROD/RECETTE.

## Questions à trancher pendant l'implémentation

- le rôle d'affectation organisationnelle doit-il réutiliser le rôle RBAC global ou posséder un catalogue clinique distinct (`MEDECIN_REFERENT`, `INFIRMIER_UNITE`, etc.) ? Par défaut, ne pas coupler automatiquement RBAC global et rôle contextuel ;
- les spécialités doivent-elles être limitées aux médecins ou rester disponibles pour d'autres professionnels spécialisés ? Le modèle doit permettre l'extension sans casser le schéma ;
- les plannings/rendez-vous devront-ils exiger une affectation active à la date du créneau ? Identifier les consommateurs actuels avant de modifier leur règle métier.

## Definition of Done

- [ ] modèle + migrations cohérents ;
- [ ] contrats backend structurés ;
- [ ] Angular sans saisie libre department/specialty ;
- [ ] consommateurs legacy migrés ;
- [ ] tenant isolation application + DB ;
- [ ] tests backend/PostgreSQL/Angular verts ;
- [ ] branche synchronisée avec le `main` courant avant revue finale ;
- [ ] documentation/tracking/changelog alignés ;
- [ ] PR squash-mergée ;
- [ ] recette humaine #127 rejouée.
