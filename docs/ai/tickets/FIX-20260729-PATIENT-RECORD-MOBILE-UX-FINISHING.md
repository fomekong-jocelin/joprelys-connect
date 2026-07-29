# FIX-20260729 — Finition UX mobile du dossier patient

GitHub issue : #227

## Contexte

La PR #226 a introduit une navigation progressive et mobile-first du dossier patient. La recette mobile réelle montre encore trois écarts de finition :

1. `Informations administratives` est un accordéon supplémentaire alors que ces données appartiennent déjà à la rubrique `Fiche d’identité` ;
2. certains titres d’accordéons sont trop longs et reviennent sur plusieurs lignes ;
3. les actions d’en-tête `Retour`, `Synthèse PDF` et `Ouvrir une visite` sont visuellement au même niveau et se placent de manière désordonnée sur petit écran.

## Objectif

Réduire encore la charge cognitive et la hauteur de page mobile sans modifier le métier, les permissions, les API ou les données.

## Périmètre inclus

- intégrer le résumé administratif directement dans la vue `Fiche d’identité` ;
- supprimer l’accordéon `Informations administratives` ;
- raccourcir les titres d’accordéons restants et garantir un rendu sur une ligne sur 360–430 px ;
- utiliser un libellé court pour le contexte d’urgence ;
- utiliser un libellé court et non ambigu pour allergies / antécédents / vaccinations ;
- hiérarchiser les actions du bandeau patient : action métier principale d’abord, actions secondaires ensuite ;
- préserver FR/EN, light/dark et accessibilité tactile.

## Hors périmètre

- backend ;
- contrats API ;
- modèle de données ;
- permissions/RBAC ;
- déploiement recette ou production.

## Critères d’acceptation

- [ ] Aucun accordéon `Informations administratives` n’est rendu.
- [ ] Le contenu administratif est visible directement dans `Fiche d’identité`.
- [ ] Les titres d’accordéons restent sur une ligne sur un viewport mobile de 360 px ou plus.
- [ ] Le contexte d’urgence conserve le badge de sécurité et son auto-ouverture en cas d’urgence active.
- [ ] Le regroupement allergies / antécédents / vaccinations reste replié par défaut.
- [ ] Sur mobile, `Ouvrir une visite` ou `Démarrer la consultation` occupe la ligne principale en pleine largeur.
- [ ] `Synthèse PDF` et `Retour` sont regroupés sur une ligne secondaire lorsque les droits permettent le PDF.
- [ ] Sur desktop, les trois actions restent compactes, alignées et ordonnées.
- [ ] Aucun texte utilisateur nouveau n’est codé en dur ; FR/EN sont présents.
- [ ] Aucun Angular Material / Tailwind v3 / URL backend hardcodée n’est introduit.
- [ ] Tests Angular et build production verts.

## Action plan

- [x] Lire la gouvernance, `DESIGN.md` et les standards UI.
- [x] Inspecter le shell patient, le profil, le contexte urgence et les tests existants.
- [x] Créer l’issue #227 et la documentation initiale.
- [ ] Modifier le profil patient.
- [ ] Modifier la hiérarchie d’actions du shell patient.
- [ ] Adapter les libellés FR/EN.
- [ ] Ajouter / adapter les tests.
- [ ] Exécuter le gate Angular complet.
- [ ] Mettre à jour `PROJECT-TRACKING.md` et `CHANGELOG.md`.
- [ ] Passer la PR Ready uniquement après le dernier commit.

## Risques

- Régression de hauteur ou débordement à 360 px si un libellé reste trop long.
- Régression de tests qui supposaient l’accordéon administratif.
- Duplication potentielle de DPU / identifiant établissement entre bandeau et fiche : accepter la présence dans le bandeau de contexte mais ne pas les répéter inutilement dans la grille administrative.

## Tests attendus

- test du profil : résumé administratif rendu sans clic ;
- test du profil : aucun bouton `Informations administratives` ;
- test du profil : informations médicales repliées par défaut ;
- test shell : ordre et classes de layout des actions mobile ;
- `npm run test` ;
- `npm run build` ;
- validation CI de la stack frontend.

## Estimation / planning

- 1 SP ;
- 0,5 jour senior frontend + QA visuelle ;
- pas d’impact backend ni DB.

## Reviewer

Tech Lead + QA UX mobile.

## SemVer

PATCH recommandé : finition UI rétrocompatible, aucun contrat cassé.