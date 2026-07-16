# Réinitialisation des données de production — Plan de test

- Vérifier le refus sans `--execute` et sans confirmation exacte.
- Vérifier la présence des variables obligatoires.
- Vérifier la sauvegarde non vide avant purge.
- Vérifier après redémarrage : service actif, port 8084, 1 utilisateur, e-mail attendu, rôle `SUPER_ADMIN`, 0 organisation, 0 patient.
- Vérifier 15 rôles système, 44 permissions et une affectation `user_roles` au super-administrateur.
- Tester le rollback sur une base de staging avec le dump produit.
