# Spécification fonctionnelle — Structure hospitalière

## Objectif

Donner aux administrateurs de clinique un espace central pour paramétrer la structure utilisée par les hospitalisations et le suivi d'occupation.

## Périmètre

La hiérarchie administrable est :

```text
Service hospitalier
└── Chambre (numéro, capacité, niveau de confort)
    └── Lit (numéro, statut opérationnel)
```

## Utilisateurs

- `ADMIN_CLINIQUE` : administration de sa clinique.
- `ADMIN_JOPRELYS` et `SUPER_ADMIN` : sélection obligatoire de la clinique à administrer.
- Les soignants et agents d'accueil conservent uniquement la vue d'occupation existante.

## Règles métier

1. Le nom d'un service est unique dans une clinique.
2. Le numéro d'une chambre est unique dans un service.
3. Le numéro d'un lit est unique dans une chambre.
4. La capacité d'une chambre est strictement positive.
5. Une chambre ne peut contenir plus de lits que sa capacité.
6. La capacité ne peut être réduite sous le nombre de lits existants.
7. Un service contenant des chambres ne peut pas être supprimé.
8. Une chambre contenant des lits ne peut pas être supprimée.
9. Un lit occupé ou ayant déjà été affecté ne peut pas être supprimé.
10. Toutes les opérations sont limitées à la clinique courante et auditées.

## États UI

- Chargement.
- Structure vide avec invitation à créer un service.
- Formulaires de création et modification.
- Confirmation avant suppression.
- Erreur métier explicite renvoyée par le backend.
- Succès après chaque opération.

## Hors périmètre

- Bâtiments, étages et ailes physiques.
- Plan graphique de l'établissement.
- Tarification avancée par chambre.
- Réservation future de lits.
