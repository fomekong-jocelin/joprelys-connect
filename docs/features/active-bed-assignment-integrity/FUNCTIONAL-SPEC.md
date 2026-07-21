# Spécification fonctionnelle — Intégrité des affectations actives de lit

**Feature ID** : `active-bed-assignment-integrity`  
**Ticket** : `BUG-20260721-ACTIVE-BED-ASSIGNMENT-INTEGRITY`  
**Story** : `EPIC-0027 / HOS-BED-001-A`

## Besoin

Le plan de lits ne doit jamais représenter deux patients simultanément affectés au même lit. Le claim atomique existant reste conservé pour le chemin nominal, mais une contrainte de base doit protéger tous les chemins d'écriture.

## Définitions

- Affectation active : affectation dont `released_at` est nul.
- Affectation clôturée : affectation dont `released_at` est renseigné.
- Marqueur technique actif : copie de `bed_id` tant que l'affectation est active, puis `NULL` après clôture.

## Règles

1. Un lit accepte zéro ou une affectation active.
2. Plusieurs affectations clôturées du même lit sont conservées.
3. Une affectation clôturée libère le marqueur avant toute nouvelle affectation.
4. Une incohérence du marqueur est rejetée par la base.
5. Une collision tardive d'admission ou de transfert est présentée comme conflit métier HTTP 409.
6. Une migration ne corrige jamais automatiquement des doublons actifs historiques.

## Préflight avant déploiement

```sql
SELECT organization_id, bed_id, COUNT(*) AS active_assignment_count
FROM bed_assignments
WHERE released_at IS NULL
GROUP BY organization_id, bed_id
HAVING COUNT(*) > 1;
```

Résultat attendu : aucune ligne. Toute ligne impose une analyse par le DBA et le bed manager avant la migration.

## Hors périmètre

Réservations futures, périodes fermées chevauchantes, sortie physique, rattachement FK au séjour et modèle multi-axes de disponibilité.

## Validation métier requise

Le cadre infirmier ou bed manager valide la définition « active = non libérée ». Le DBA valide le préflight et la stratégie de restauration.
