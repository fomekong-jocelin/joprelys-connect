# Spécification fonctionnelle — Intégrité séjour et période des affectations de lit

**Feature ID** : `bed-assignment-stay-integrity`  
**Ticket** : `BUG-20260721-BED-ASSIGNMENT-STAY-INTEGRITY`  
**Story** : `EPIC-0027 / HOS-BED-001-B`

## Besoin

Chaque mouvement de lit doit être rattaché à un séjour hospitalier réel et conservé comme preuve historique. Un séjour ne peut pas être simultanément placé dans deux lits et une période ne peut pas se terminer avant son début.

## Préflights avant déploiement

### Affectations orphelines

```sql
SELECT ba.id, ba.organization_id, ba.hospitalization_id
FROM bed_assignments ba
LEFT JOIN hospitalizations h ON h.id = ba.hospitalization_id
WHERE h.id IS NULL;
```

### Plusieurs affectations actives par séjour

```sql
SELECT organization_id, hospitalization_id, COUNT(*) AS active_assignment_count
FROM bed_assignments
WHERE released_at IS NULL
GROUP BY organization_id, hospitalization_id
HAVING COUNT(*) > 1;
```

### Périodes inversées

```sql
SELECT id, organization_id, hospitalization_id, assigned_at, released_at
FROM bed_assignments
WHERE released_at < assigned_at;
```

Résultat attendu pour chaque requête : aucune ligne. Toute anomalie exige une correction supervisée et auditée ; la migration ne la corrige jamais automatiquement.

## Règles

1. Un mouvement sans séjour est interdit.
2. Un séjour référencé ne peut pas être supprimé physiquement.
3. Un séjour possède au plus une affectation active.
4. La clôture accepte une date égale ou postérieure au début.
5. Les mouvements clôturés restent consultables.

## Validation requise

- DBA : préflight, sauvegarde et restauration.
- Cadre infirmier/bed manager : unicité de la présence active.
- DPO/administration : interdiction de suppression physique des séjours référencés.
