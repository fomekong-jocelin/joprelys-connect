# FIX-20260729 — Compacter les lignes du journal d’audit mobile

GitHub issue : #231

## Contexte

Après la PR #230, la recette mobile réelle confirme deux derniers écarts de densité :

- le motif `Consultation de l'historique d'audit du patient` reste trop long dans l’en-tête de chaque entrée ;
- dans le détail ouvert, `Action` et `IP` sont affichés au-dessus de leurs valeurs alors que chaque couple tient sur une seule ligne.

## Décision UX

- Utiliser pour le titre de la ligne le libellé i18n court `patient.audit.action.<ACTION>` lorsqu’il existe ; conserver `reason` puis `action` comme fallback.
- Afficher `Action : valeur` et `IP : valeur` en ligne, avec label fixe et valeur non tronquée.
- Ne pas modifier la ressource, le user-agent, l’ouverture indépendante des accordéons ni le backend.

## Critères d’acceptation

- [ ] `READ_AUDIT` affiche le libellé court localisé (`Lecture audit` en FR).
- [ ] `Action : READ_AUDIT` reste sur une seule ligne.
- [ ] `IP : 143.105.152.106` reste sur une seule ligne.
- [ ] Les valeurs restent intégrales.
- [ ] FR/EN et RBAC inchangés.
- [ ] Tests Angular + build production verts.

## Tests

- vérifier le titre court pour `READ_AUDIT` ;
- vérifier les lignes `Action` et `IP` dans le détail ouvert ;
- conserver les tests d’accordéon indépendant et de locale.

## Estimation

0,5 SP — correctif frontend chirurgical.
