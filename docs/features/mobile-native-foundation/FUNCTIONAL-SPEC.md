# FUNCTIONAL SPEC — Fondation mobile native Joprelys Connect

## Problème métier

Joprelys Connect dispose aujourd’hui d’un frontend web mobile-first, mais certaines fonctions cliniques exigent une maîtrise du cycle de vie du terminal que le navigateur ne peut pas garantir. Le cas le plus critique est la consultation vocale : si Android suspend le navigateur ou si l’écran se verrouille, le professionnel peut continuer à parler alors que la capture audio n’est plus réellement active.

L’application mobile native doit devenir le poste de travail clinique mobile de référence, sans dupliquer les règles métier du backend ni créer un second produit divergent du web.

## Utilisateurs concernés

- médecins ;
- infirmiers et personnels de triage ;
- agents d’accueil/admission ;
- administrateurs cliniques ;
- professionnels disposant des permissions Joprelys correspondantes.

Le portail patient n’est pas inclus dans la première fondation native professionnelle.

## Objectifs

1. Fournir une application mobile Android fiable, rapide et cohérente avec Joprelys Connect web.
2. Préserver les mêmes permissions, contrats métier et règles cliniques que le backend.
3. Offrir une expérience mobile pensée pour la charge cognitive clinique : information progressive, actions principales explicites, gros points tactiles et états visibles.
4. Garantir dès la fondation : FR/EN, light/dark/system, accessibilité, sécurité et testabilité.
5. Préparer une capture audio native capable de rester contrôlable lorsque l’application passe en arrière-plan ou que l’écran s’éteint, dans les limites du système Android.

## Principes produit

### Backend maître

L’application mobile :

- affiche ;
- collecte ;
- orchestre l’expérience ;
- conserve éventuellement un brouillon local explicitement autorisé.

Le backend :

- autorise ;
- valide ;
- calcule ;
- décide ;
- persiste le dossier médical ;
- détermine les statuts métier.

Aucune règle critique ne doit exister uniquement dans l’application mobile.

### Cohérence Web / Mobile

Le mobile réutilise :

- les mêmes concepts patients, visites, constantes, prescriptions, examens, planning et permissions ;
- les mêmes principes du `DESIGN.md` ;
- les mêmes libellés métier FR/EN lorsque le sens est identique ;
- les mêmes endpoints backend, sauf évolution contractuelle explicitement documentée.

Le mobile peut adapter la composition et l’ordre visuel pour l’ergonomie tactile.

## Parcours fonctionnels cibles

### 1. Connexion

- connexion professionnelle ;
- récupération de session ;
- accès biométrique optionnel après une authentification serveur réussie ;
- choix langue et thème accessibles ;
- déconnexion et expiration de session explicites.

### 2. Tableau de bord

- accueil chaleureux ;
- file d’attente active existante mise en avant ;
- raccourcis selon permissions ;
- accès planning, patients, consultation, laboratoire et urgences selon rôle.

### 3. File d’attente active

- visites actives issues du backend ;
- ordre d’arrivée fiable ;
- accès aux actions réellement permises ;
- aucun statut inventé côté mobile.

### 4. Planning

- agenda jour/semaine ;
- rendez-vous ;
- disponibilités et indisponibilités ;
- modification selon permissions.

### 5. Patients

- recherche progressive nom/téléphone/DPU ;
- liste compacte ;
- ouverture du dossier ;
- admission selon permission.

### 6. Dossier patient

Navigation locale :

- Fiche d’identité ;
- Dossier médical ;
- Analyses & Labo ;
- Hospitalisations ;
- Audit.

Les informations critiques de sécurité ne doivent jamais être masquées uniquement pour gagner de la place.

### 7. Saisie des constantes

- saisie manuelle ;
- validation UX immédiate ;
- validation métier finale backend ;
- enregistrement explicite ;
- état erreur/retry sans perte silencieuse de saisie.

### 8. Consultation clinique / IA

- démarrage explicite de la capture ;
- état réel du microphone visible ;
- pause/reprise/arrêt ;
- transcription et corrections ;
- propositions IA distinctes de la décision médicale ;
- validation médecin obligatoire ;
- aucun transcript perdu silencieusement ;
- interruption audio explicitement signalée.

### 9. Prescription et examens

- médicaments et examens existants ;
- brouillon modifiable ;
- validation explicite ;
- backend maître des validations et permissions.

## Thème

Modes obligatoires :

- Light ;
- Dark ;
- System.

Le choix peut être persisté localement car il ne s’agit pas d’une donnée clinique sensible.

## Internationalisation

Minimum :

- `fr` ;
- `en`.

Règles :

- aucun texte visible codé en dur ;
- dates/heures/nombres formatés selon locale ;
- langue envoyée au backend quand nécessaire (`Accept-Language` et champs locale existants) ;
- français comme locale produit par défaut tant qu’aucune préférence n’est définie.

## Sécurité fonctionnelle

- biométrie = déverrouillage local additionnel, jamais remplacement de l’authentification backend ;
- aucune donnée clinique dans les logs ;
- aucun secret dans les préférences ordinaires ;
- aucun cache DPU complet par défaut ;
- tout brouillon clinique local doit être temporaire, chiffré et traçable ;
- fermeture de session = purge des états sensibles non nécessaires.

## Capture audio — exigences fonctionnelles P0

Le médecin ne doit jamais voir « Enregistrement en cours » si le moteur natif n’a pas confirmé qu’il capture réellement.

États minimum :

- prêt ;
- demande de permission ;
- démarrage ;
- enregistrement ;
- pause ;
- interruption ;
- reprise ;
- arrêt ;
- finalisation ;
- erreur.

Toute interruption doit produire :

- un état visible ;
- une heure de début/fin ;
- une indication qu’une portion peut ne pas avoir été capturée ;
- une possibilité de redicter/reprendre.

## Offline / réseau faible

La v1 reste online-first.

Autorisé localement uniquement si explicitement implémenté :

- préférence langue/thème ;
- tokens dans stockage sécurisé ;
- brouillon de formulaire limité ;
- segments audio temporaires chiffrés en attente d’envoi.

Non autorisé par défaut :

- réplication locale complète du DPU ;
- base offline générale des patients ;
- conservation longue durée des audios.

## Critères d’acceptation de la fondation

- [ ] l’application démarre sur Android avec branding Joprelys ;
- [ ] le thème `light`, `dark` et `system` est centralisé ;
- [ ] FR/EN sont disponibles via ARB ;
- [ ] navigation centralisée et testable ;
- [ ] configuration environnement centralisée ;
- [ ] authentification/session sécurisée ;
- [ ] client API central et erreurs normalisées ;
- [ ] composants partagés pour boutons, champs, cartes, états et navigation ;
- [ ] aucun texte utilisateur hardcodé ;
- [ ] aucun secret dans le code ;
- [ ] le squelette `Flutter Demo` a disparu ;
- [ ] les tests de fondation passent ;
- [ ] CI mobile exécute analyse + tests ;
- [ ] architecture audio native prête avant le développement de la consultation vocale.

## Cas limites à prévoir

- permissions micro refusées puis accordées ;
- notification désactivée ;
- téléphone verrouillé ;
- appel téléphonique / interruption audio ;
- changement de réseau ;
- absence de réseau ;
- expiration JWT pendant un formulaire ;
- refresh token invalide ;
- changement FR/EN pendant une session ;
- thème système changé pendant l’application ;
- application tuée puis relancée avec brouillon autorisé ;
- utilisateur change d’organisation/session.

## Hors périmètre de la fondation

- portail patient natif ;
- mode offline complet ;
- synchronisation locale de l’ensemble du DPU ;
- publication stores ;
- iOS audio background complet ;
- modification métier du backend.
