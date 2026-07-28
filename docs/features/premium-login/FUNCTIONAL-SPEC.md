# Page de connexion premium — Spécification fonctionnelle

## Objectif

Renforcer la perception de confiance, de qualité clinique et de sécurité dès le premier écran de Joprelys Connect, sans modifier les mécanismes d'authentification existants.

## Utilisateurs

- personnel de santé ;
- patients ;
- utilisateur déjà authentifié arrivant sur la page racine.

## Parcours conservés

### Personnel de santé

1. Saisir l'adresse e-mail et le mot de passe (avec possibilité d'afficher/masquer le mot de passe via un bouton icône d'œil).
2. Déclencher la connexion.
3. Lorsque l'API l'exige, saisir le code OTP professionnel.
4. Revenir à l'étape précédente si nécessaire.
5. Être redirigé vers le `returnUrl` sécurisé ou vers le tableau de bord.

### Patient

1. Saisir le numéro patient global, le téléphone et la date de naissance.
2. Demander un code OTP.
3. Saisir le code reçu.
4. Accéder au portail patient.

### Session déjà active

L'écran continue d'afficher la session détectée et l'action de déconnexion existante.

## Expérience visuelle

- vraie identité visuelle via le composant partagé `app-logo` ;
- présentation mobile-first ;
- carte de connexion compacte, lisible et légèrement surélevée ;
- décor clinique abstrait uniquement visuel, sans donnée simulée ;
- panneau institutionnel complémentaire sur grand écran, sans bloc « Flux clinique synchronisé » ;
- contrôles langue et thème toujours disponibles avant connexion ;
- drapeaux 🇫🇷 et 🇬🇧 pour les langues ;
- prise en charge automatique des thèmes light et dark.

## Contraintes UX

- aucune nouvelle méthode d'authentification ne doit être suggérée si elle n'existe pas réellement ;
- les boutons conservent des libellés sur une seule ligne ;
- les coins restent sobres, entre 4 et 8 px ;
- la priorité mobile est donnée au logo, au choix de profil et au formulaire ;
- les éléments purement décoratifs ne doivent pas perturber les lecteurs d'écran ;
- les erreurs doivent rester proches du formulaire concerné.

## Accessibilité

- navigation clavier complète ;
- focus visible ;
- `aria-pressed` sur les choix de langue et de mode ;
- libellés accessibles pour le changement de thème ;
- ordre de lecture cohérent ;
- contraste WCAG AA dans les deux thèmes.

## Hors périmètre

- modification des API d'authentification ;
- ajout d'une connexion sociale ;
- ajout d'un mode OTP alternatif non pris en charge ;
- modification des règles de session ou de sécurité ;
- refonte de la récupération de mot de passe.
