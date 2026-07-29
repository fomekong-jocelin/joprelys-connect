# TEST PLAN — Liste Patients mobile-first

## Tests Angular ciblés

### En-tête

- [ ] titre court `Patients` ;
- [ ] aucun `backLink` / lien Retour du PageHeader ;
- [ ] CTA admission hors du PageHeader.

### Recherche

- [ ] placeholder court ;
- [ ] absence du bouton texte `Rechercher` ;
- [ ] Enter appelle la recherche ;
- [ ] l’action d’effacement apparaît uniquement quand la requête n’est pas vide ;
- [ ] `clearSearch()` vide la requête et recharge la liste.

### Contexte résultats

- [ ] sans filtre : `Liste des patients` ;
- [ ] avec filtre : compteur singulier/pluriel.

### Cartes mobile

- [ ] chaque patient est une carte/bouton indépendante ;
- [ ] nom, sexe, DPU, téléphone et ville sont présents ;
- [ ] aucun bouton `Voir le dossier` n’est imbriqué dans la carte mobile ;
- [ ] clic sur la carte appelle le routage patient existant ;
- [ ] aucune grande carte parent n’enferme la liste mobile.

### Desktop

- [ ] tableau toujours rendu à partir de `md` ;
- [ ] identifiants DPU et local restent `whitespace-nowrap` ;
- [ ] action `Voir le dossier` toujours présente.

## Viewports de recette

- 360 × 800 ;
- 390 × 844 ;
- 412 × 915 ;
- 430 × 932 ;
- 768 × 1024 ;
- ≥ 1280 px desktop.

## Thèmes / langues

- FR light/dark ;
- EN light/dark.

## Gate

```bash
cd web
npm run test
npm run build
```

## Non-régression métier

- admission normale ;
- admission urgence ;
- ouverture du dossier patient ;
- recherche API ;
- consentement / Break-Glass ;
- RBAC.

## Critère de sortie

CI frontend verte sur le HEAD final, puis recette visuelle réelle sur mobile avant tout déploiement production.
