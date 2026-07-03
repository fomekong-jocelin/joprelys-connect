# STORY-1103 — Spécification Fonctionnelle

## 1. Contexte métier

La plateforme Joprelys Connect gérait jusqu'ici les stocks de façon déclarative (sans persistance réelle). Cette évolution introduit un inventaire physique des médicaments par clinique (tenant), avec décrémentation automatique lors des délivrances et alertes de stock bas.

## 2. Acteurs et rôles concernés

| Acteur | Rôle système | Actions autorisées |
|---|---|---|
| Pharmacien | `PHARMACIEN` | Consulter, créer/mettre à jour le stock, recevoir les alertes |
| Administrateur clinique | `ADMIN_CLINIQUE` | Idem pharmacien (supervision) |

## 3. Parcours utilisateur

### 3.1 Consultation des stocks
1. Le pharmacien se connecte au portail pharmacie.
2. Il navigue vers la section « Inventaire des médicaments ».
3. Il voit la liste des médicaments triée par nom avec les colonnes :
   - Nom du médicament / nom générique
   - Unité (comprimé, flacon, ampoule…)
   - Quantité disponible
   - Seuil minimum
   - Lot / Date d'expiration
   - Fournisseur
   - Indicateur **⚠ Stock bas** si `quantityAvailable ≤ minimumThreshold`

### 3.2 Approvisionnement (création / mise à jour)
1. Le pharmacien clique sur « Ajouter / Approvisionner ».
2. Il saisit le nom du médicament (obligatoire), la quantité, le seuil minimum et les informations de lot.
3. Si le médicament existe déjà (même nom, insensible à la casse), la fiche est mise à jour (*upsert*).
4. Si c'est un nouveau médicament, une nouvelle fiche est créée.

### 3.3 Alertes de stock bas
- Endpoint dédié `GET /api/pharmacy/stocks/alerts` retournant uniquement les médicaments en dessous du seuil.
- L'interface affiche un badge rouge sur l'icône pharmacie si des alertes existent.

### 3.4 Décrémentation automatique lors d'une délivrance
- Lors d'une dispensation via `PharmacyService.dispensePrescription()`, le service `DrugStockService.checkAndDecrementStock()` est appelé pour chaque médicament dispensé.
- Si le stock est insuffisant → réponse `409 Conflict` avec message explicite.
- Si le médicament n'est pas référencé dans les stocks → la délivrance est autorisée (stock optionnel).

## 4. Règles métier

| ID | Règle |
|---|---|
| RG-01 | `quantityAvailable` ne peut jamais descendre en dessous de 0 (contrainte SQL + logique métier) |
| RG-02 | La recherche par nom de médicament est insensible à la casse (LOWER) |
| RG-03 | Lors d'un upsert, les champs non fournis (null) ne sont pas écrasés |
| RG-04 | Le verrouillage optimiste via `@Version` prévient les conflits d'écriture concurrente |
| RG-05 | Un médicament non référencé dans le stock ne bloque pas la délivrance |

## 5. Critères d'acceptation

- [x] `GET /api/pharmacy/stocks` → liste triée par nom, filtrée par tenant
- [x] `POST /api/pharmacy/stocks` → création ou mise à jour (upsert) d'un stock
- [x] `GET /api/pharmacy/stocks/alerts` → médicaments en dessous du seuil
- [x] Décrémentation transactionnelle avec `409 Conflict` si stock insuffisant
- [x] Isolation multi-tenant via `@TenantId` Hibernate
- [x] Verrouillage optimiste `@Version` pour la concurrence
