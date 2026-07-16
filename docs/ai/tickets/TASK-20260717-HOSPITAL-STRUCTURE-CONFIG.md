# TASK-20260717 — Configuration de la structure hospitalière

## Mode d'intervention

Engineering — Backend Spring Boot, Angular, base de données et RBAC.

## Statut

QA — développement et tests automatisés terminés, validation visuelle après déploiement restante.

## Objectif

Permettre à un administrateur de clinique de configurer les services hospitaliers, les chambres et les lits depuis une page dédiée.

## Critères d'acceptation

- [x] Une entrée « Structure hospitalière » est visible pour les administrateurs autorisés.
- [x] Un administrateur peut créer, renommer et supprimer un service vide.
- [x] Un administrateur peut créer, modifier et supprimer une chambre vide.
- [x] Un administrateur peut créer, renommer et supprimer un lit jamais affecté.
- [x] La capacité d'une chambre ne peut pas être inférieure au nombre de lits configurés.
- [x] Le nombre de lits ne peut pas dépasser la capacité de la chambre.
- [x] Un lit occupé ou possédant un historique d'affectation ne peut pas être supprimé.
- [x] Les données restent strictement isolées par clinique.
- [x] Tous les libellés visibles existent en français et en anglais.
- [x] Les opérations sensibles sont auditées.
- [x] Les tests backend et frontend ciblés réussissent.

## Actions

- [x] Diagnostiquer l'existant et confirmer l'absence de CRUD de configuration.
- [x] Rédiger la spécification, la conception, le contrat API, le modèle de données et le plan de test.
- [x] Ajouter les contraintes d'unicité en base.
- [x] Implémenter le use case et les endpoints d'administration.
- [x] Implémenter la page Angular et son entrée de navigation.
- [x] Ajouter les traductions FR/EN.
- [x] Ajouter les tests backend/frontend.
- [x] Mettre à jour le changelog et le suivi.

## Risques

- Suppression accidentelle d'une structure utilisée par une hospitalisation.
- Dépassement de capacité d'une chambre en cas de concurrence.
- Fuite inter-clinique si un accès n'est pas filtré par le tenant courant.

## Estimation

1,5 à 2 jours — profil full-stack Spring Boot / Angular.

## Reviewer

Développeur senior full-stack + validation fonctionnelle métier clinique.

## Reste à faire

Déployer puis effectuer la validation visuelle authentifiée en light/dark et sur mobile.
