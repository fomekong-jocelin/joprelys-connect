# Spécification fonctionnelle — Annuaire patient mobile (MOB-2809)

## Objectif

Permettre à un professionnel authentifié de rechercher un patient dans son
établissement depuis l'application Flutter, puis d'ouvrir son dossier mobile.

## Parcours

1. Le professionnel ouvre l'onglet Annuaire depuis le dashboard.
2. Il saisit un nom, un numéro DPU, un téléphone ou un numéro temporaire.
3. Le mobile interroge le backend et affiche les résultats autorisés.
4. Il sélectionne un patient pour ouvrir son dossier/historique MOB-2810.

## Règles métier et UX

- Le backend reste maître de la recherche, du tenant et des autorisations.
- La file active et l'annuaire sont deux états distincts de l'interface.
- Les états chargement, vide et erreur sont explicites.
- Les textes passent par l'i18n FR/EN et l'UI utilise le thème central.
- Aucun résultat clinique n'est inventé, mis en cache durablement ou journalisé.

## Critères d'acceptation

- [x] Recherche branchée sur l'API patients existante.
- [x] Résultats ouvrant le dossier patient.
- [x] Affichage responsive sur écran Android compact.
- [x] Light/dark et FR/EN conservés.
- [ ] Recette Android et backend sur le même HEAD.

## Hors périmètre

- création ou modification de patient ;
- cache hors ligne du dossier patient ;
- modification des règles RBAC backend.
