# Comptes de démonstration et rôles — Joprelys Connect

## 1. Principe de sécurité

Le dépôt **ne fournit plus de comptes de démonstration avec mot de passe prédéfini**.

- aucun mot de passe de démonstration ne doit être commité ;
- aucun compte médecin, pharmacien ou clinique de test n'est créé automatiquement par `AdminUserSeeder` ;
- le bootstrap `ADMIN_JOPRELYS` est désactivé par défaut et, lorsqu'il est exceptionnellement activé, ses informations proviennent exclusivement des variables d'environnement / du gestionnaire de secrets ;
- les accès utilisés pour une démonstration doivent être provisionnés dans l'environnement concerné par les parcours normaux d'administration.

## 2. Préparation des comptes d'une démonstration

Préparer uniquement les profils nécessaires au scénario :

| Profil | Usage typique |
|---|---|
| `ADMIN_JOPRELYS` | Administration plateforme et création/gestion des établissements selon les permissions effectives |
| `ADMIN_CLINIQUE` | Administration de l'établissement, personnel et RBAC clinique |
| `AGENT_ACCUEIL` | Accueil, patients, visites et parcours administratifs autorisés |
| `INFIRMIER` | Constantes, soins, urgences et hospitalisation selon permissions |
| `MEDECIN` | Consultation, prescriptions, examens, urgences et hospitalisation selon permissions |
| `BIOLOGISTE` | File laboratoire et traitement des résultats |
| `PHARMACIEN` | Dispensation et fonctions pharmacie autorisées |
| `RESPONSABLE_HOSPITALISATION` | Admission, transfert, départ physique et opérations de lit selon permissions |
| `CAISSIER` / profils finance | Encaissement et fonctions financières selon le scénario |
| `PATIENT` | Portail patient via authentification OTP |

Les rôles visibles et les actions disponibles sont déterminés par le RBAC effectif. Ne jamais utiliser un ancien tableau documentaire de credentials comme source de vérité.

## 3. Création des accès personnel

1. Le compte plateforme initial, lorsqu'il est nécessaire, est créé par un bootstrap contrôlé et temporaire.
2. L'administrateur plateforme crée ou sélectionne l'établissement cible.
3. Un `ADMIN_CLINIQUE` est créé via le parcours produit prévu.
4. L'administrateur clinique invite ensuite les collaborateurs et leur attribue les rôles nécessaires.
5. Les secrets temporaires ou liens de première connexion sont transmis par les mécanismes applicatifs prévus et ne sont jamais copiés dans Git.

## 4. Portail patient

Le portail patient utilise un OTP et non un mot de passe statique de démonstration.

Pour tester le parcours :

1. créer ou sélectionner un patient de démonstration dans l'établissement ;
2. utiliser les informations patient exigées par le formulaire de connexion ;
3. demander l'OTP ;
4. récupérer le code via le canal d'envoi configuré dans l'environnement ;
5. saisir le code dans sa fenêtre de validité.

Le code courant utilise le service de messagerie applicatif pour l'OTP de connexion patient. Il ne faut pas compter sur un affichage du code dans la console backend comme procédure normale de démonstration.

## 5. Runbook de démonstration

Les identités fictives, rôles requis et données métier d'un scénario doivent être documentés dans le runbook de la démonstration concernée, **sans mot de passe, OTP, token ou secret**.

Pour la démonstration du 25 juillet 2026, utiliser les documents canoniques du parcours de démo et le runbook daté associé plutôt que de recréer un second jeu de comptes ou de données concurrent.

## 6. Règle de maintenance

Ce document décrit la politique de préparation des accès. Il ne doit jamais redevenir un inventaire de credentials.

Toute valeur ressemblant à un secret, mot de passe, OTP ou token ajoutée à ce fichier doit être considérée comme une anomalie de sécurité et retirée avant fusion.
