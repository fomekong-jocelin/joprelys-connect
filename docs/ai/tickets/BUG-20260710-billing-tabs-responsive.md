# BUG-20260710 — Onglets facturation/caisse non responsifs sur mobile

## 1. Objectif

Rendre la navigation par onglets de la page Facturation & Caisse lisible et utilisable sur les petits écrans, sans modifier les routes ni le comportement métier.

## 2. Critères d'acceptation

- [x] Les libellés des onglets ne sont plus compressés ni coupés sur mobile.
- [x] La barre d'onglets reste navigable tactilement par défilement horizontal.
- [x] Chaque onglet conserve une cible tactile minimale et un état actif visible.
- [x] Les rôles ARIA `tab` et `aria-selected` restent cohérents avec l'onglet actif.
- [x] Valider le rendu structurel via build et tests Angular ; une validation manuelle sur appareil réel reste recommandée.

## 3. Pilotage projet

| Champ | Valeur |
|---|---|
| Epic parent | EPIC-0018 — Intégrité financière et poste facturation/caisse |
| Type | Bug UI/UX |
| Priorité | P1 |
| Sprint cible | SPRINT-0013 |
| Profil recommandé | Frontend intermédiaire |
| Estimation senior | 0.15j |
| Reviewer | Lead Developer |
| Risque | Faible |

## 4. Diagnostic

Le conteneur des onglets utilisait `display: flex` sans `overflow-x` ni `flex-shrink: 0` sur les boutons. Les sept libellés se partageaient donc la largeur disponible et devenaient illisibles sur mobile.

## 5. Action plan

- [x] Analyser la capture et le composant de facturation.
- [x] Documenter la correction dans la spécification et la conception du module.
- [x] Ajouter une barre d'onglets horizontalement défilable.
- [x] Empêcher la compression des libellés et renforcer les cibles tactiles.
- [x] Ajouter les attributs ARIA de tabulation.
- [x] Exécuter les tests et le build Angular.
- [ ] Effectuer une validation visuelle mobile sur appareil réel ou navigateur avec émulation tactile.
- [x] Mettre à jour le changelog et le suivi projet.

## 6. Sécurité / régression

Aucun contrat API, état métier, droit d'accès ou donnée sensible n'est modifié. Le correctif porte uniquement sur la présentation et l'accessibilité de la navigation existante.

## 7. Statut

Statut : **DONE**

## 8. Impact version / SemVer

Correctif rétrocompatible : **PATCH** si livré.
