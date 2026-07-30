# Pré-enregistrement public — refonte UX

## Ticket

- GitHub : #247
- Route : `/public/register`
- Utilisateurs : patient, accompagnant ou proche utilisant le QR Code d'un établissement
- Impact SemVer prévu : PATCH

## Problème

Le pré-enregistrement public est actuellement présenté sous la forme d'un formulaire monolithique de cinq sections. Sur mobile, il impose un scroll important et expose beaucoup d'informations simultanément. Son en-tête et son sélecteur de langue ne suivent pas les conventions du shell Joprelys Connect. Enfin, l'utilisateur ne dispose pas d'une sortie claire vers le site ou la connexion patient, ni avant la saisie ni après une soumission réussie.

## Objectifs

1. Réduire la charge cognitive et le scroll mobile en n'affichant qu'un groupe logique à la fois.
2. Aligner la topbar publique sur les conventions visuelles du shell Joprelys Connect.
3. Rendre les sorties explicites et permanentes : site Joprelys et connexion patient.
4. Préserver intégralement le comportement métier actuel de pré-enregistrement, le `orgId`, le captcha et le payload backend.

## Parcours cible

### Avant saisie

L'utilisateur voit :

- le logo Joprelys Connect ;
- un choix explicite `FR | EN` ;
- le changement de thème ;
- un accès « Retour au site » ;
- un accès « Se connecter » vers `/patient/login`.

### Saisie progressive

Le formulaire est découpé en quatre étapes visibles une par une :

1. **Admission & identité** : type d'admission, prénom, nom, genre, date de naissance.
2. **Coordonnées** : groupe sanguin, téléphone, e-mail, adresse.
3. **Contact d'urgence** : nom, téléphone, lien de parenté.
4. **Validation** : captcha et soumission finale.

Une progression compacte indique l'étape courante. Les valeurs sont conservées lors des retours arrière.

### Après succès

L'utilisateur peut :

- retourner au site Joprelys ;
- ouvrir la connexion patient ;
- démarrer un autre pré-enregistrement.

## Règles fonctionnelles

- Les champs obligatoires restent : prénom, nom, genre et date de naissance.
- L'utilisateur ne peut pas quitter l'étape 1 vers l'étape 2 si ces champs sont incomplets.
- Les étapes 2 et 3 restent optionnelles comme aujourd'hui.
- Le captcha reste obligatoire à l'étape finale.
- Un échec de soumission recharge le captcha comme aujourd'hui.
- Le type d'admission reste un état d'interface sans modification du contrat backend dans ce ticket.
- Le reset après succès remet le parcours à l'étape 1 et recharge un captcha.

## Hors périmètre

- modification du backend ou de `PatientPreRegistrationRequest` ;
- ajout de colonnes ou migration DB ;
- changement du mécanisme de captcha ;
- changement des règles de réconciliation à l'accueil ;
- authentification ou création automatique d'un compte patient.

## Critères d'acceptation

- [ ] topbar alignée avec le shell Joprelys Connect ;
- [ ] FR et EN sélectionnables explicitement ;
- [ ] thème clair/sombre conservé ;
- [ ] retour au site disponible avant saisie et après succès ;
- [ ] connexion patient disponible avant saisie et après succès ;
- [ ] quatre étapes progressives, une seule visible à la fois ;
- [ ] validation de l'identité avant passage à l'étape 2 ;
- [ ] précédent / continuer utilisables au clavier et sur mobile ;
- [ ] reset après succès revient à l'étape 1 ;
- [ ] textes FR/EN complets ;
- [ ] aucune régression du payload ou du captcha ;
- [ ] tests Angular et build production verts.

## Recette manuelle

Tester au minimum en 375 px, 768 px et 1366 px :

1. ouvrir un lien `/public/register?orgId=<uuid>` ;
2. vérifier FR puis EN ;
3. vérifier light puis dark ;
4. vérifier les sorties vers le site et `/patient/login` ;
5. avancer et revenir entre les quatre étapes en contrôlant la conservation des valeurs ;
6. tenter de continuer avec identité incomplète ;
7. soumettre avec captcha valide ;
8. vérifier les trois actions de l'écran de succès ;
9. tester le reset et confirmer le retour à l'étape 1.