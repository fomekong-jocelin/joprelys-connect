# HOS-ORG-001-A — Référentiels et unités organisationnelles hospitalières

## Métadonnées

- Issue GitHub : #130
- Epic : EPIC-0027 / HOS-ORG-001
- Priorité : P0 avant démonstration client du 25 juillet 2026
- Baseline : `main@81b7d20c4cf44800e436c86b964a38bc62929305`
- Branche : `feat/130-hos-org-001-a`
- Statut : READY — documentation initiale créée, code non démarré au moment de ce commit
- Profil : senior full-stack / architecte
- Reviewers : Tech Lead, DBA, Product/Direction médicale

## Objectif

Remplacer la notion ambiguë de « service » portée par `Ward` et les champs texte libres par un référentiel organisationnel explicite, hiérarchique et tenant-scoped permettant de représenter :

`Pôle → Département → Service → Unité de soins`

Tous les niveaux sont facultatifs. Une petite clinique peut créer directement des services sous l'établissement ; un CHU peut activer toute la hiérarchie.

## Décisions

- Organisation et géographie sont deux axes distincts.
- `Ward` n'est pas étendu pour représenter pôle/département/unité.
- Le type métier d'un service vient d'un catalogue codifié ; il n'est pas saisi librement.
- Les spécialités médicales viennent d'un catalogue codifié.
- Une unité organisationnelle n'est pas supprimée physiquement dès qu'elle a pu être utilisée ; elle est désactivée pour préserver l'historique.
- L'autorisation de gestion est dédiée : `ORGANIZATION_STRUCTURE_MANAGE`.
- Aucune nouvelle UI ne crée de fallback vers `department`, `specialty` ou un nom de service libre.

## Action plan

- [x] Relire audit, ADR-0002, backlog et règles UI/architecture.
- [x] Créer issue #130 et découpage 3 × 3 SP.
- [x] Documenter fonctionnel, technique, data, API et tests.
- [ ] Ajouter migration V87 et modèle backend.
- [ ] Ajouter permission `ORGANIZATION_STRUCTURE_MANAGE` et matrice RBAC minimale.
- [ ] Ajouter API catalogues + unités.
- [ ] Ajouter tests backend/migration/sécurité.
- [ ] Ajouter UI Angular mobile-first FR/EN light/dark.
- [ ] Ajouter tests Angular et build.
- [ ] Mettre à jour tracking, changelog, backlog et ADR.
- [ ] Ouvrir PR Ready uniquement après tests.

## Critères d'acceptation

- [ ] Petite clinique : création directe de services sans pôle/département obligatoire.
- [ ] Hôpital complexe : hiérarchie Pôle → Département → Service → Unité.
- [ ] Les services utilisent un `serviceCatalogCode` contrôlé.
- [ ] Les spécialités utilisent un catalogue codifié.
- [ ] Un service ne peut pas être placé sous un parent invalide.
- [ ] Un enfant ne peut pas référencer un parent d'un autre tenant.
- [ ] Les codes d'unités sont uniques par tenant.
- [ ] Les unités désactivées restent consultables dans l'historique mais ne sont plus proposées pour de nouvelles affectations.
- [ ] API : validation 200/400/403/404/409.
- [ ] UI : 320/375/768/1366 px, clavier/focus, light/dark, FR/EN.
- [ ] Aucun texte utilisateur codé en dur dans les composants.
- [ ] Aucun Angular Material, aucun Tailwind v3, aucune URL backend hardcodée.
- [ ] Maven strict + tests Angular + build production verts.

## Estimation et capacité

9 SP total, découpés en tâches de 3 SP :

1. data/migration/catalogues — 3 SP ;
2. services/API/RBAC — 3 SP ;
3. UI mobile-first/i18n/QA — 3 SP.

Effort senior : 3 à 4 jours. La démonstration impose un ordre de livraison serré, mais aucun critère qualité n'est supprimé.

## Risques

- migration et coexistence temporaire avec `Ward` tant que HOS-LOC/HOS-STAFF ne sont pas fusionnés ;
- nomenclature médicale non exhaustive : les catalogues initiaux sont un référentiel technique extensible, pas une norme médicale fermée ;
- ne pas dupliquer la logique métier des services dans Angular ;
- ne pas utiliser une permission spatiale générique pour gérer l'organisation médicale.

## SemVer

Ajout parallèle rétrocompatible : **MINOR** pour ce lot. Le retrait ultérieur des anciens contrats/champs libres sera traité comme breaking change dans HOS-STAFF/HOS-LOC lorsque leur remplacement sera complet.
