# TEST-PLAN — Démonstration client du 25 juillet 2026

## 1. Objectif

Valider que les parcours présentés au client sont reproductibles sur la baseline courante sans régression fonctionnelle, autorisation élargie ou dépendance à un workaround.

## 2. Niveaux de validation

### Niveau A — preuves automatisées existantes

Inventorier et réutiliser en priorité les tests déjà fusionnés avec :

- PR #96 — continuité URG-TEMP/hospitalisation/documents/finance ;
- PR #97 — workspace urgence, rapprochement et continuité E2E ;
- PR #122 — suppression `HOSPITALIZATION_MANAGE`, Flyway V86 et non-régression RBAC.

### Niveau B — tests ciblés manquants

N'ajouter un test que si l'audit montre qu'une étape du runbook n'est pas protégée par une preuve automatisée significative.

### Niveau C — répétition humaine

La répétition générale vérifie l'enchaînement des écrans, comptes, données et transitions dans les conditions de démonstration.

## 3. Matrice du parcours principal

| Étape | Acteur | Résultat attendu | Preuve à identifier |
|---|---|---|---|
| Connexion | chaque profil | accès au bon workspace | Auth/RBAC tests |
| Patient | Accueil | patient créé ou retrouvé | patient API/UI tests |
| Visite | Accueil | visite ouverte | visit tests |
| Constantes | Infirmier | constantes visibles dans le dossier | vitals tests |
| Consultation | Médecin | consultation persistée | clinical tests |
| Hospitalisation | profil habilité | séjour créé sans doublon | hospitalization tests |
| Structure | profil habilité | uniquement service/chambre/lit compatibles | HOS-02 tests |

## 4. Matrice URG-TEMP

| Étape | Résultat attendu |
|---|---|
| Création URG-TEMP | identité provisoire créée sans identité fictive complète |
| Urgence | urgence liée au patient provisoire |
| Triage | ABCDE/constantes enregistrables immédiatement |
| Rapprochement | décision explicite, jamais automatique |
| DPU canonique | accès au patient canonique après décision |
| Hospitalisation | continuité conservant le lien avec `emergencyId` |
| Finance différée | aucune exigence de paiement préalable sur le soin urgent |
| Documents | provenance et identité source conservées |

## 5. Scénarios négatifs indispensables

- profil sans permission d'admission → 403 ;
- rôle ne possédant que l'ancienne permission supprimée → aucun accès par fallback ;
- service non spatial → création de chambre refusée ;
- lit non libre → admission refusée proprement ;
- rapprochement cross-tenant → refus ;
- double soumission/rejeu URG-TEMP → comportement idempotent ;
- hospitalisation active en doublon → refus métier structuré ;
- erreur d'un document après admission → ne doit pas déclencher une seconde admission.

## 6. Commandes de validation

### Backend

```bash
cd backend
./mvnw clean verify -B --no-transfer-progress -Dspring.profiles.active=test
```

Aucun `-DskipTests` ou `-Dmaven.test.skip=true`.

### Frontend

```bash
cd web
npm ci --prefer-offline --no-audit --fund=false
npm test -- --watch=false
npm run build
```

## 7. PostgreSQL / Flyway

- vérifier la présence des tests PostgreSQL 16 couvrant les migrations critiques ;
- vérifier le passage jusqu'à V86 ;
- ne pas modifier V1–V86 dans cette tâche ;
- toute lacune réelle doit produire un test ciblé, pas un contournement de migration.

## 8. Recette visuelle

Résolutions minimales :

- 320 px ;
- 375 px ;
- 768 px ;
- 1366 px ;
- 1920 px.

Contrôles :

- light/dark ;
- FR/EN sur les écrans de démonstration ;
- navigation clavier sur les actions principales ;
- pas de texte tronqué sur les boutons ;
- aucun UUID demandé à l'utilisateur.

## 9. Critères de sortie

### GO

- suites automatiques vertes ;
- aucun P0 sur les étapes principales ;
- données de démonstration préparées ;
- répétition complète réussie au moins une fois ;
- limites restantes non bloquantes consignées.

### NO-GO

- erreur 5xx reproductible ;
- migration impossible ;
- perte de continuité patient/urgence/hospitalisation ;
- contournement RBAC nécessaire ;
- structure hospitalière incohérente ;
- donnée de démonstration impossible à préparer avec les fonctions normales du produit.
