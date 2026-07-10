# Billing Financial Precision — Functional Specification

## Problème métier

Les montants de facturation doivent être stockés avec une précision décimale déterministe. La migration de `Double` vers `BigDecimal` ne peut être considérée comme fiable si la migration de base de données ne s’exécute pas dans les environnements de test ou si elle modifie les valeurs financières existantes.

## Utilisateurs concernés

- Agent d’accueil ;
- Caissier ;
- DAF ;
- Administrateur clinique ;
- équipes support, QA et exploitation.

## Objectif

Garantir que tous les montants historiques de facturation sont convertis vers des colonnes décimales exactes, avec la même précision sur les environnements supportés par le projet.

## Périmètre inclus

- montants des factures ;
- parts patient et assurance ;
- remises ;
- lignes de facture ;
- paiements ;
- créances ;
- tarifs ;
- taux de couverture assurance.

## Périmètre exclu

- modification des formules de calcul ;
- changement du cycle de vie d’une facture ;
- modification des autorisations ;
- changement des écrans ou contrats API.

## Règles métier préservées

1. Les montants utilisent une précision maximale de 19 chiffres avec 4 décimales.
2. Le taux de couverture assurance est stocké comme fraction décimale, par exemple `0.8000` pour 80 %, avec une précision `NUMERIC(5,4)`.
3. Une remise historique absente est normalisée à `0.0000`, conformément au comportement déjà prévu par V55.
4. Un coefficient historique absent est normalisé à `1.0000`, conformément au comportement déjà prévu par V55.
5. Les autres valeurs nulles ne sont pas transformées silencieusement.
6. Aucun montant n’est recalculé par cette migration.

## Parcours attendu

```text
Schéma historique FLOAT8
→ normalisation ciblée des valeurs nulles déjà prévue par V55
→ conversion des colonnes en NUMERIC
→ démarrage Flyway réussi
→ démarrage du contexte Spring réussi
→ tests financiers exécutables
```

## Critères d’acceptation

- La migration s’exécute dans les tests H2 et reste compatible PostgreSQL.
- Les précisions définies par les entités JPA correspondent au schéma final.
- Les valeurs existantes sont converties sans changement de règle métier.
- Aucun endpoint ou écran n’est modifié.
- La suite backend peut être exécutée jusqu’à son terme.

## Cas limites

- valeurs avec plus de quatre décimales : arrondi selon le moteur lors de la conversion vers l’échelle 4 ;
- valeur dépassant la précision cible : la migration doit échouer plutôt que tronquer silencieusement ;
- `NULL` dans `discount_amount` : conversion explicite vers zéro ;
- `NULL` dans `coefficient` : conversion explicite vers un ;
- taux de couverture hors format fractionnaire : non corrigé automatiquement par cette migration.

## Hypothèses

- PostgreSQL reste le moteur de production.
- H2 reste temporairement le moteur des tests automatisés existants.
- La migration V55 n’a pas encore été appliquée sur un environnement de production nécessitant une migration corrective séparée.

## Zones à clarifier

- Planifier séparément une migration des tests critiques vers PostgreSQL/Testcontainers afin de réduire les différences de dialecte.