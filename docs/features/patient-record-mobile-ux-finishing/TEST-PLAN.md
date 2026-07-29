# TEST PLAN — Finition UX du dossier patient mobile

## Objectif

Prouver que la finition UX réduit les niveaux de navigation sans perdre de donnée, de permission ou de comportement de sécurité.

## Tests unitaires Angular

### PatientProfileTabComponent

- [ ] affiche `PatientAdministrativeSummaryComponent` dès le chargement de la fiche ;
- [ ] ne rend plus de bouton/accordéon administratif ;
- [ ] garde les informations médicales repliées par défaut ;
- [ ] ouvre et referme les informations médicales sur action utilisateur ;
- [ ] conserve la régularisation d’identité provisoire.

### PatientEmergencyContextComponent

- [ ] utilise le libellé i18n court du contexte d’urgence ;
- [ ] reste replié sans urgence active ;
- [ ] s’ouvre automatiquement avec une urgence active ;
- [ ] conserve l’état d’erreur existant.

### PatientDetailComponent

- [ ] l’action principale disponible est rendue avant les actions secondaires dans le DOM ;
- [ ] l’action principale possède le layout pleine largeur mobile ;
- [ ] `Synthèse PDF` reste conditionnée à `CLINICAL_READ` ;
- [ ] `Retour` reste toujours disponible ;
- [ ] aucun changement des computed RBAC.

## Vérifications responsive

Viewports cibles :

- 360 × 800 ;
- 390 × 844 ;
- 412 × 915 ;
- 430 × 932 ;
- 768 × 1024 ;
- desktop ≥ 1280 px.

À vérifier :

- [ ] `Contexte d’urgence` sur une ligne ;
- [ ] `Informations médicales` sur une ligne ;
- [ ] aucun scroll horizontal de page ;
- [ ] chevrons visibles ;
- [ ] boutons sans retour à la ligne interne ;
- [ ] action principale évidente ;
- [ ] footer non chevauché.

## Thèmes / langues

- [ ] FR light ;
- [ ] FR dark ;
- [ ] EN light ;
- [ ] EN dark.

## Gate automatisé

```bash
cd web
npm run test
npm run build
```

Le lint est exécuté si le script existe dans `package.json` / le pipeline.

## Régression métier

Aucune modification attendue sur :

- API patient ;
- création de visite ;
- démarrage consultation ;
- génération PDF ;
- allergies critiques ;
- Break-Glass ;
- contexte urgence ;
- RBAC.

## Critère de sortie

CI frontend verte sur le HEAD final de la PR, puis recette visuelle mobile réelle avant déploiement production.