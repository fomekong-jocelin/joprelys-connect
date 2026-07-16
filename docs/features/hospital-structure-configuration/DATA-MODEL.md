# Modèle de données — Structure hospitalière

Les tables existantes sont conservées : `wards`, `rooms`, `beds`, `bed_assignments`.

## Relations

- `wards 1—N rooms`
- `rooms 1—N beds`
- `beds 1—N bed_assignments`

Toutes les tables portent `organization_id` et sont filtrées par le tenant Hibernate courant.

## Contraintes ajoutées

- index unique du nom de service dans une organisation ;
- index unique du numéro de chambre dans un service ;
- index unique du numéro de lit dans une chambre.

L'insensibilité à la casse est contrôlée par le service applicatif afin de conserver une migration compatible PostgreSQL/H2.

Les relations existantes conservent leurs contraintes de clés étrangères.
