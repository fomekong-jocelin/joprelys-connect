# ADR-0003 — Exclusion temporelle PostgreSQL des affectations de lit

- **Statut** : ACCEPTED pour le prototype technique ; validation DBA et bed manager requise avant production
- **Date** : 2026-07-22
- **Décision liée** : EPIC-0027 / HOS-BED-001-D / GAP-005

## Contexte

V76 empêche plusieurs affectations actives pour un même lit, V77 contrôle la chronologie simple et V78 garantit le même établissement entre l'affectation, le séjour et le lit. Deux périodes clôturées peuvent toutefois encore se chevaucher après un import ou une correction rétroactive.

Une vérification uniquement applicative ne protège pas les imports, scripts DBA, traitements futurs ni écritures concurrentes. La garantie doit donc être portée par PostgreSQL.

## Décision

1. Les périodes opérationnelles utilisent la sémantique semi-ouverte `[assigned_at, released_at)`.
2. Une fin égale au début de la période suivante est autorisée.
3. Une affectation active est représentée par une borne haute infinie.
4. V79 ajoute un statut d'intégrité `VALID | QUARANTINED` et les métadonnées de quarantaine, sans modifier les données existantes.
5. Seules les affectations clôturées peuvent être mises en quarantaine.
6. V80 est une migration Java Flyway :
   - elle ne fait rien sous H2 ;
   - elle compte les chevauchements `VALID` sous PostgreSQL ;
   - elle échoue avant activation si un conflit subsiste ;
   - elle installe `btree_gist` ;
   - elle crée une contrainte d'exclusion GiST partielle, immédiatement vérifiée, sur les lignes `VALID`.
7. La quarantaine est une décision manuelle, signée par le DBA et le bed manager. Elle conserve la ligne dans `bed_assignments` et n'efface aucun historique.

## Forme de la contrainte

```sql
EXCLUDE USING gist (
    organization_id WITH =,
    bed_id WITH =,
    tsrange(
        assigned_at,
        COALESCE(released_at, 'infinity'::timestamp),
        '[)') WITH &&
)
WHERE (integrity_status = 'VALID')
```

La contrainte reste `NOT DEFERRABLE`, valeur PostgreSQL par défaut. Chaque insertion ou correction est donc contrôlée à la fin de l'instruction SQL. Les workflows applicatifs existants clôturent et flushent l'ancienne affectation avant d'en créer une nouvelle.

Le schéma historique utilise actuellement `TIMESTAMP WITHOUT TIME ZONE`; la plage est donc `tsrange`. Le passage futur à `TIMESTAMPTZ`/`Instant` relève de GAP-038 et exigera une migration dédiée vers `tstzrange`.

## Pourquoi `btree_gist`

La contrainte combine l'égalité sur des UUID (`organization_id`, `bed_id`) et le chevauchement d'une plage temporelle. L'extension fournit les classes d'opérateurs GiST nécessaires aux UUID dans cet index multicolonne.

## Alternatives rejetées

### Contrôle applicatif seul

Rejeté : contournable par import ou SQL direct et insuffisant face à la concurrence.

### Trigger H2/PostgreSQL portable

Rejeté : complexité supérieure, verrouillage plus difficile à prouver et moindre lisibilité opérationnelle.

### Suppression ou correction automatique des conflits

Rejetée : le logiciel ne peut pas décider quelle période clinique est vraie. Toute correction nécessite une preuve, un acteur et un motif.

## Conséquences

### Positives

- garantie atomique en base sur toutes les écritures PostgreSQL ;
- bornes adjacentes autorisées explicitement ;
- corrections rétroactives réévaluées dans la transaction ;
- historique litigieux conservé et identifiable ;
- migration H2 maintenue pour les suites rapides existantes.

### Négatives

- la garantie complète n'existe pas sous H2 ; les tests critiques doivent rester sous PostgreSQL 16 ;
- l'utilisateur de migration doit pouvoir installer l'extension ou le DBA doit la préinstaller ;
- un déploiement peut s'arrêter sur V80 tant que les conflits ne sont pas arbitrés ;
- les lignes `QUARANTINED` restent consultables mais ne participent plus à la contrainte d'exclusion.

## Déploiement

1. sauvegarder la base et exécuter le préflight sur une copie anonymisée ;
2. faire arbitrer chaque paire conflictuelle ;
3. appliquer V79 ;
4. marquer manuellement les seules lignes approuvées comme `QUARANTINED` ;
5. relancer Flyway pour V80 ;
6. exécuter les scénarios adjacence, chevauchement, correction rétroactive et concurrence ;
7. conserver le rapport de préflight et les décisions d'arbitrage.

## Rollback

En cas d'incident après activation :

```sql
ALTER TABLE bed_assignments
    DROP CONSTRAINT IF EXISTS ex_bed_assignments_valid_period_no_overlap;
```

Les colonnes de quarantaine et l'extension sont conservées. Aucune ligne `QUARANTINED` ne redevient `VALID` automatiquement. Une réactivation exige un nouvel arbitrage et la vérification qu'elle ne recrée aucun chevauchement.
