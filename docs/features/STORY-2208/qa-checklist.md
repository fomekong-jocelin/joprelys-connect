# STORY-2208 — Checklist de QA RBAC

## Préconditions
- Utiliser une base contenant la migration Flyway `V57__admin_managed_rbac.sql`.
- Tester avec deux établissements distincts.
- Disposer d'un `ADMIN_CLINIQUE`, d'un utilisateur métier et d'un compte plateforme.

## Migration et compatibilité
- [ ] Les utilisateurs existants conservent leurs rôles historiques après migration.
- [ ] `DAF`, `CAISSIER`, `SECRETAIRE_COMPTABLE`, `GESTIONNAIRE_STOCK` et `RESPONSABLE_HOSPITALISATION` sont présents.
- [ ] Un utilisateur multi-rôles conserve l'ensemble de ses permissions effectives.
- [ ] La colonne historique `users.role` reste synchronisée pendant la phase de compatibilité.

## Administration des rôles
- [ ] L'admin clinique consulte le catalogue des rôles et permissions de son établissement.
- [ ] Les rôles plateforme et `ORGANIZATION_MANAGE` ne sont pas visibles pour l'admin clinique.
- [ ] Un rôle personnalisé peut être créé, renommé, activé ou désactivé.
- [ ] Les rôles système sont consultables mais non modifiables.
- [ ] Une permission inconnue ou réservée à la plateforme est refusée côté backend.

## Affectation des utilisateurs
- [ ] Un admin peut affecter plusieurs rôles à un utilisateur de son établissement.
- [ ] Les permissions effectives sont visibles après l'enregistrement.
- [ ] La modification prend effet sans attendre l'expiration du JWT.
- [ ] Un admin ne peut pas modifier ses propres privilèges.
- [ ] Un admin ne peut pas retirer le dernier administrateur actif.
- [ ] Un utilisateur d'un autre établissement ne peut pas être modifié.
- [ ] Un admin clinique ne peut pas attribuer `SUPER_ADMIN` ou `ADMIN_JOPRELYS`.

## Permissions métier
- [ ] Un rôle personnalisé de caisse peut ouvrir la file et encaisser uniquement avec les permissions correspondantes.
- [ ] Un rôle personnalisé assurance peut lire, faire progresser ou régler un bordereau selon sa matrice.
- [ ] `ACCOUNTING_EXPORT` contrôle réellement l'export comptable.
- [ ] Les permissions patient séparent lecture, écriture, fusion et accès d'urgence.
- [ ] Le laboratoire sépare la lecture d'un patient, la file globale, la prescription et le traitement.
- [ ] Les urgences séparent lecture, prise en charge et stabilisation.
- [ ] Les hospitalisations séparent lecture et gestion.

## Interface Angular
- [ ] Le menu est construit à partir des permissions effectives.
- [ ] Un rôle personnalisé interne peut accéder au tableau de bord.
- [ ] Le rôle `PATIENT` reste isolé dans le portail patient.
- [ ] Une permission révoquée bloque la route même si le JWT contient encore un ancien rôle.
- [ ] Le workspace est utilisable en 360 px, 768 px et 1440 px.
- [ ] Les thèmes clair et sombre restent lisibles.
- [ ] La navigation clavier et les états de focus sont visibles.

## Audit et sécurité
- [ ] La création ou modification d'un rôle est journalisée.
- [ ] Le remplacement des rôles d'un utilisateur est journalisé.
- [ ] Les tentatives d'escalade sont refusées par le backend, même via appel API manuel.
- [ ] Les données RBAC sont strictement isolées par `organization_id`.
- [ ] Les administrateurs plateforme n'obtiennent pas automatiquement l'accès aux dossiers patients d'une clinique.

## Non-régression
- [ ] Maven `clean verify` est vert sous Java 21.
- [ ] Les migrations passent sur H2 et PostgreSQL 16.
- [ ] Les tests Angular sont verts sous Node.js 22.
- [ ] Le build Angular de production est vert.
- [ ] Le transfert concurrent vers un même lit donne exactement un succès et un conflit HTTP 409.

## Déploiement
- Vérifier `flyway_schema_history` avant application sur une base partagée.
- Sauvegarder la table `users` avant la première migration RBAC.
- Conserver `users.role` pendant la période de compatibilité.
- Prévoir une reconnexion contrôlée après déploiement, même si les permissions sont résolues côté serveur à chaque requête.
