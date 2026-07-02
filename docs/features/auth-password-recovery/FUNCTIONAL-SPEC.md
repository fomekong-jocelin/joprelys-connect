# Spécification Fonctionnelle — Récupération de Mot de Passe Simplifiée (STORY-0103)

## 1. Contexte & Objectif

Dans le cadre du MVP de **Joprelys Connect**, un professionnel de santé (médecin, infirmier, agent d'accueil, administrateur clinique) peut perdre ou oublier son mot de passe. 
L'objectif de cette fonctionnalité est de lui permettre de réinitialiser son mot de passe de manière autonome et sécurisée, sans intervention d'un administrateur système, grâce à un processus de validation par code de sécurité temporaire (OTP) envoyé de manière simulée (affiché dans la console du serveur backend en phase MVP).

---

## 2. Acteurs & Rôles concernés

* **Professionnel de santé** (Médecin, Agent d'accueil, Infirmier, Administrateur Clinique) : souhaite réinitialiser son mot de passe pour retrouver l'accès à son compte.
* **Administrateur système** : n'a plus besoin d'intervenir manuellement pour générer de nouveaux mots de passe temporaires.

---

## 3. Parcours Utilisateur (User Flow)

1. **Accès au formulaire** : Sur l'écran de connexion unifiée (`/`), le professionnel de santé clique sur le lien "Mot de passe oublié ?".
2. **Demande de réinitialisation** : 
   * Il saisit son adresse e-mail professionnelle.
   * Il clique sur "Recevoir un code de réinitialisation".
   * Le système valide la présence et l'activation du compte.
   * Si le compte existe, un code OTP à 6 chiffres est généré (valide pendant 5 minutes) et imprimé dans la console du serveur backend.
   * L'interface affiche l'écran de saisie du code et du nouveau mot de passe, avec un message de succès générique (pour éviter la divulgation d'e-mails).
3. **Réinitialisation** :
   * L'utilisateur saisit le code de sécurité reçu.
   * Il saisit son nouveau mot de passe (qui doit respecter des critères de longueur minimale de 8 caractères).
   * Il clique sur "Modifier le mot de passe".
   * Le système valide le code OTP. Si le code est correct et valide, le mot de passe est mis à jour de manière sécurisée (hashé avec BCrypt).
   * L'utilisateur est redirigé vers l'écran de connexion pour s'authentifier avec ses nouvelles coordonnées.

---

## 4. Règles Métier (Business Rules)

* **RGPD & Divulgation d'information** : Le message renvoyé à l'utilisateur lors de la demande de réinitialisation doit être identique, que l'adresse e-mail existe en base de données ou non (ex: *"Si cette adresse e-mail existe dans notre système, un code de réinitialisation a été généré"*).
* **Durée de validité de l'OTP** : Le code généré expire automatiquement après 5 minutes (300 secondes).
* **Limitation des tentatives** : Au bout de 3 tentatives de saisie de code incorrectes, le code OTP est détruit, obligeant l'utilisateur à refaire une demande de réinitialisation.
* **Critères du mot de passe** : Le nouveau mot de passe doit faire au moins 8 caractères de longueur.
* **Acteur désactivé** : Un utilisateur dont le compte est désactivé (`enabled = false`) ou appartenant à une organisation désactivée ne peut pas réinitialiser son mot de passe.

---

## 5. Critères d'Acceptation

* [ ] Un lien "Mot de passe oublié ?" est présent sur l'écran de connexion unifiée.
* [ ] La demande de réinitialisation avec un e-mail valide et actif génère un code OTP à 6 chiffres affiché dans les logs du serveur.
* [ ] La demande de réinitialisation avec un e-mail inconnu ou inactif renvoie la même réponse utilisateur, mais aucun OTP n'est généré ni loggé.
* [ ] Le code OTP expire après 5 minutes.
* [ ] Saisir un code OTP incorrect augmente le nombre de tentatives (max 3 tentatives avant invalidation).
* [ ] La soumission d'un code OTP valide avec un nouveau mot de passe met à jour le compte en base de données (hashé avec BCrypt) et détruit le code OTP.
* [ ] L'utilisateur peut se connecter immédiatement après avec son nouveau mot de passe.
