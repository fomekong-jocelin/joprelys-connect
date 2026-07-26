# Conception technique — Finitions UI i18n, thème et mobile IA

## Décisions

### Logo contextuel

`AppLogoComponent` expose une apparence `default` ou `on-dark`. Les chemins d'assets et le nom produit proviennent de `APP_BRAND_CONFIG`. L'apparence sur fond sombre neutralise explicitement tout filtre dark.

### Panneau de connexion

Les textes marketing deviennent des nœuds HTML liés à des clés `login.showcase.*`. Les pseudo-éléments `content:` et la détection de langue via `:has` sont supprimés.

### Assistant de constantes

Le template est extrait dans un fichier HTML. L'état initial est déterminé par le viewport : replié sous 640 px, ouvert au-dessus. La surface mobile est bornée par `dvh`, avec actions pleine largeur et rayons de 8 px maximum.

### Composition dashboard

Le dashboard déclare `app-smart-vitals-assistant` dans son template et relaie l'événement `proposed`. Les appels `createComponent`, `querySelector`, `setTimeout` et la mutation de classes sont retirés.

### Internationalisation IA

Les libellés transverses des champs IA sont regroupés sous `consultation.ai.field.*`. Les aides de clarification, de brouillon et le texte alternatif du QR sont ajoutés aux catalogues FR/EN.

### Bootstrap des tests navigateur

Node 25 expose dans ce poste un objet `localStorage` sans implémentation fonctionnelle. Le fichier `src/test-setup.ts`, chargé uniquement par le builder de tests Angular, fournit un stockage mémoire strictement lorsque `localStorage.clear` est absent. Il n'est pas référencé par l'application et n'altère ni le stockage ni le consentement en production. `tsconfig.spec.json` l'inclut afin qu'il reste vérifié par TypeScript.

## Sécurité et régression

- aucune décision métier n'est déplacée vers le frontend ;
- aucune sauvegarde automatique n'est introduite ;
- le repli de l'assistant coupe toujours Realtime ;
- aucune URL backend absolue n'est ajoutée ;
- aucune dépendance ou librairie UI n'est ajoutée.
- le correctif de stockage est limité au programme TypeScript de test.

## Dette explicitement hors périmètre

Le template historique du dashboard dépasse le seuil documentaire recommandé. L'extraction complète du formulaire de constantes sera traitée séparément afin d'éviter un refactoring large dans ce correctif.
