# Réinitialisation des données de production — Spécification fonctionnelle

Après exécution, la plateforme ne contient plus aucun patient, établissement, donnée clinique, document, facturation, session ni audit utilisateur. Un seul compte actif reste disponible : `jocelin.fomekong@joprelys.com`, avec le rôle `SUPER_ADMIN` et sans rattachement à une organisation.

Le catalogue de rôles et permissions système est régénéré depuis le code au redémarrage. L'historique Flyway est conservé. Une sauvegarde complète et restaurable est obligatoire avant toute suppression.
