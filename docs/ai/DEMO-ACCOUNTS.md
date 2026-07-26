# Accès de démonstration et rôles — Joprelys Connect

Ce document décrit la procédure sûre pour provisionner des accès de test. **Aucun compte, mot de passe ou OTP statique ne doit être versionné dans le dépôt.**

---

## 1. Bootstrap exceptionnel de l'administrateur plateforme

Le backend **ne crée aucun `ADMIN_JOPRELYS` par défaut**.

Pour un environnement de développement ou de recette explicitement autorisé, le bootstrap doit être activé avec des secrets injectés par l'environnement :

```text
JOPRELYS_SEED_ADMIN_ENABLED=true
JOPRELYS_ADMIN_EMAIL=<secret/environment>
JOPRELYS_ADMIN_NAME=<secret/environment>
JOPRELYS_ADMIN_PASSWORD=<secret/environment>
```

Les quatre conditions suivantes sont obligatoires :

- activation explicite du bootstrap ;
- email fourni ;
- nom fourni ;
- mot de passe fourni.

Une configuration activée mais incomplète provoque un échec explicite au démarrage. Il n'existe aucun email, nom ou mot de passe de repli dans le code.

Après provisionnement de l'environnement, désactiver le bootstrap sauf besoin opérationnel documenté.

---

## 2. Comptes professionnels de test

Les comptes `MEDECIN`, `BIOLOGISTE`, `PHARMACIEN`, `INFIRMIER`, `AGENT_ACCUEIL`, etc. doivent être créés ou invités via les parcours d'administration prévus pour l'établissement de test.

Ne documentez jamais ici :

- un mot de passe réel ;
- un mot de passe partagé ;
- un token ;
- une clé API ;
- un code OTP ;
- un secret temporaire encore utilisable.

Les secrets de recette ou de démonstration doivent être transmis par le gestionnaire de secrets / canal opérationnel autorisé de l'environnement concerné.

---

## 3. ADMIN_CLINIQUE

Un `ADMIN_CLINIQUE` est rattaché à un établissement et administre les utilisateurs et rôles cliniques autorisés de cet établissement.

Les rôles plateforme `ADMIN_JOPRELYS` et `SUPER_ADMIN` ne sont pas attribuables depuis l'administration d'une clinique.

---

## 4. Portail patient

Le portail patient suit son propre contexte d'identité et son mécanisme OTP. Il ne doit pas réutiliser les credentials professionnels.

Pour une recette :

1. créer un patient de test via le parcours autorisé ;
2. utiliser ses informations de test ;
3. demander un OTP via le flux prévu pour l'environnement ;
4. récupérer le code uniquement via le canal de recette prévu ;
5. ne jamais ajouter ce code ou un exemple encore valide dans Git.

---

## 5. Règle de sécurité

Toute ancienne documentation contenant des identifiants versionnés doit être considérée comme **historique compromis**. Un secret qui a été commité doit être remplacé côté environnement ; sa suppression du fichier courant ne constitue pas à elle seule une rotation du secret.
