# BUG-20260721-LOGIN-PRODUCT-SHOWCASE

- GitHub : #88
- Type : bug UI/UX et conversion produit
- Priorité : P1
- Statut : IN REVIEW
- Stack : Angular / CSS / responsive / branding / i18n

## Constat

La version déployée après la PR #87 affiche le slogan officiel comme un très grand hero title. Cette hiérarchie rend le panneau gauche lourd, allonge inutilement la composition et provoque un scroll vertical sur des écrans desktop où le formulaire et la présentation peuvent tenir dans la hauteur disponible.

Le panneau présente par ailleurs peu de bénéfices produit concrets et conserve un bouclier isolé qui n’aide pas l’utilisateur à comprendre la valeur de Joprelys Connect.

## Décision design

Conformément à `DESIGN.md` :

1. limiter le titre principal à l’échelle `headline-lg`, soit 32 px maximum ;
2. utiliser une promesse produit comme titre principal ;
3. présenter un résumé fonctionnel et trois bénéfices réels ;
4. déplacer le slogan officiel au niveau d’une signature secondaire ;
5. supprimer le bouclier, les orbites, repères et espaces verticaux sans fonction ;
6. tenir dans `100dvh` à partir de 760 px de hauteur desktop ;
7. conserver le scroll sous 760 px afin de ne jamais masquer un champ ou une action ;
8. préserver les parcours Personnel, Patient, OTP, session active, thème et langue.

## Contenu retenu

### Français

- Promesse : `Le parcours patient, coordonné de bout en bout.`
- Description : `Joprelys Connect réunit le dossier patient, les consultations, les admissions, la facturation et la coordination clinique dans un espace sécurisé.`
- Bénéfices :
  - une information patient centralisée ;
  - des équipes mieux coordonnées ;
  - des accès sécurisés et traçables.
- Signature : `Parce qu’elle est précieuse, nous innovons pour la protéger.`

### Anglais

- Promesse : `One connected workspace for the entire patient journey.`
- Description et bénéfices équivalents en anglais.
- Signature : `Because it is precious, we innovate to protect it.`

## Fichiers modifiés

- `web/src/app/auth/login.branding-refinement.css`
- `DESIGN.md`
- `docs/ai/tickets/BUG-20260721-LOGIN-PRODUCT-SHOWCASE.md`

## Critères d’acceptation

- [x] slogan réduit à une signature secondaire ;
- [x] promesse produit affichée comme titre principal ;
- [x] taille du titre plafonnée à 32 px ;
- [x] trois bénéfices fonctionnels sans métriques fictives ;
- [x] bouclier et décorations inutiles masqués ;
- [x] composition fixe en `100dvh` à partir de 760 px de hauteur ;
- [x] scroll de sécurité conservé sous 760 px ;
- [x] version anglaise prévue ;
- [ ] tests Angular verts ;
- [ ] build Angular production vert ;
- [ ] Maven strict vert ;
- [ ] recette visuelle à 1366×768, 1440×900 et 1920×1080.

## Recette manuelle

1. Ouvrir `/` en mode Personnel à 1366×768, 1440×900 et 1920×1080.
2. Vérifier l’absence de scrollbar verticale sur ces hauteurs.
3. Vérifier que le titre produit ne domine pas excessivement le panneau.
4. Vérifier que le slogan est lisible mais secondaire.
5. Basculer en mode Patient et vérifier que le formulaire complet reste accessible.
6. Passer en anglais et vérifier la promesse, les bénéfices et la signature.
7. Vérifier light/dark et les contrôles thème/langue.
8. Réduire la hauteur sous 760 px et confirmer que le scroll réapparaît.
