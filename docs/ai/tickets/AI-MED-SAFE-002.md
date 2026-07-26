# AI-MED-SAFE-002 — Garde ordonnance et référentiel RxNorm

## Statut

- **Epic** : IA clinique / Sécurité médicamenteuse
- **Type** : Feature sécurité
- **Priorité** : P0
- **Statut** : IN PROGRESS — validation CI en attente
- **Branche** : `agent/medication-safety-reference-v2`

## Objectif

Finaliser le branchement du référentiel médicament dans le garde ordonnance afin que Joprelys puisse retenir une proposition avant validation lorsqu'une preuve autoritative montre que deux noms représentent le même concept ou le même principe actif.

Le référentiel ne prescrit pas, ne remplace aucun médicament et ne conclut jamais qu'une ordonnance est sûre. Il apporte uniquement des preuves positives de rapprochement.

## Pipeline

```text
Médicament explicitement dicté
  ↓
AiClinicalGroundingGuard
  ↓
AiMedicationSafetyGuard
  ├─ allergie exacte locale
  ├─ traitement actif exact local
  ├─ équivalence référentielle allergie
  └─ équivalence référentielle traitement actif
        ↓
MedicationReferenceDuplicateDetector
        ↓
MedicationReferenceService
        ↓
RxNormMedicationReferencePort
        ├─ RxCUI produit/concept
        └─ IN / PIN / MIN (principe actif)
  ↓
Clarification médecin si preuve positive
  ↓
Validation humaine avant révision ordonnance
```

## Règles de sécurité

- Les contrôles exacts locaux sont exécutés avant tout appel au référentiel.
- `SAME_CONCEPT` exige un identifiant de concept partagé dans une même source.
- `SAME_INGREDIENT` exige un identifiant de principe actif partagé fourni par une source autoritative.
- Toute autre situation reste `UNKNOWN`.
- `UNKNOWN` ne signifie jamais « sûr » ou « différent ».
- Une panne ou un timeout RxNorm ne bloque pas la consultation : les garde-fous locaux restent actifs.
- Aucun RxCUI n'est affiché au professionnel ; les identifiants restent dans les preuves/logs techniques.
- Une réponse explicite du médecin à une clarification prescription déjà ouverte continue le flux sans boucle de confirmation.

## Activation

Configuration générale, désactivée par défaut pour les environnements de développement/CI :

```yaml
JOPRELYS_MEDICATION_REFERENCE_ENABLED=false
JOPRELYS_RXNORM_ENABLED=false
```

Le profil `prod` active le référentiel et RxNorm par défaut, avec possibilité de coupure immédiate par variable d'environnement :

```yaml
JOPRELYS_MEDICATION_REFERENCE_ENABLED=true
JOPRELYS_RXNORM_ENABLED=true
```

Paramètres réseau :

```yaml
JOPRELYS_RXNORM_BASE_URL=https://rxnav.nlm.nih.gov
JOPRELYS_RXNORM_CONNECT_TIMEOUT_MS=1500
JOPRELYS_RXNORM_READ_TIMEOUT_MS=2500
JOPRELYS_MEDICATION_REFERENCE_CACHE_TTL_MINUTES=1440
JOPRELYS_MEDICATION_REFERENCE_MAX_CANDIDATES=5
```

## Critères d'acceptation

- [x] Le garde ordonnance conserve les contrôles exacts existants.
- [x] Le référentiel est branché après ces contrôles exacts.
- [x] Une preuve `SAME_CONCEPT` déclenche une clarification avant proposition d'ordonnance.
- [x] Une preuve `SAME_INGREDIENT` déclenche une clarification même lorsque les RxCUI produit diffèrent.
- [x] RxNorm remonte les concepts ingrédients `IN`, `PIN` et `MIN` liés à un produit.
- [x] Les appels RxNorm ont des timeouts bornés.
- [x] Les réponses référentielles et ingrédients sont mises en cache.
- [x] Une panne RxNorm n'empêche pas la consultation.
- [x] Les identifiants techniques ne sont pas exposés dans le message médecin.
- [x] RxNorm est activé par défaut dans le profil `prod` mais reste désactivable sans redéploiement de code.
- [ ] Maven strict vert sur le HEAD final.
- [ ] Recette environnementale avec accès réel à RxNav.

## Scénarios de recette

### MED-01 — Doublon exact
Traitement actif `Amlodipine`, nouvelle prescription `Amlodipine`.

Attendu : clarification locale immédiate, sans dépendre de RxNorm.

### MED-02 — Même principe actif, noms différents
Référentiel : le produit proposé et le traitement actif remontent vers le même concept ingrédient.

Attendu : ordonnance retenue et question de confirmation au médecin.

### MED-03 — Allergie référentielle
Une entrée d'allergie et le médicament proposé sont prouvés équivalents par concept ou ingrédient.

Attendu : demande de confirmation avant proposition d'ordonnance.

### MED-04 — Concepts sans preuve commune
Deux médicaments résolus mais sans concept ni ingrédient partagé.

Attendu : aucune alerte référentielle ; le résultat reste `UNKNOWN`, jamais `SAFE`.

### MED-05 — RxNorm indisponible
Simuler timeout ou panne RxNav.

Attendu : consultation non bloquée ; contrôles exacts locaux maintenus.

## Hors périmètre volontaire

- interactions médicament-médicament ;
- contre-indications liées aux pathologies, grossesse, fonction rénale/hépatique ;
- allergies par classe thérapeutique déduites automatiquement ;
- substitution thérapeutique ;
- recommandation de dose ou posologie ;
- utilisation d'une similarité floue comme preuve clinique.

Ces fonctions nécessitent une source pharmacologique autoritative dédiée. RxNorm est ici utilisé pour l'identité et les relations de concepts, pas comme moteur complet de décision thérapeutique.

## Source de données

L'adaptateur utilise les données publiques RxNorm/RxNav de la U.S. National Library of Medicine (NLM). Joprelys doit présenter l'origine NLM de ces données dans sa documentation produit ; la NLM/NIH/HHS n'endosse pas Joprelys ni ses décisions cliniques.

## Definition of Done

- [x] Comparaison concept + principe actif.
- [x] Branchement au garde ordonnance.
- [x] Activation `prod` configurable.
- [x] Timeouts + cache.
- [x] Tests unitaires référentiel, ingrédients et garde ordonnance.
- [ ] CI Maven strict verte.
- [ ] Test réel RxNav sur environnement de recette.
- [ ] Revue clinique des libellés de confirmation.
