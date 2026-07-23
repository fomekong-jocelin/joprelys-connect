# TECHNICAL-DESIGN — Stabilisation pré-démo du 25 juillet 2026

## 1. Principe

Cette intervention n'introduit pas une nouvelle architecture. Elle vérifie et stabilise les capacités déjà présentes dans `main`.

La règle est : **audit du code actuel → preuve par tests → correction minimale uniquement si un défaut est démontré**.

## 2. Baseline

- Repository : `fomekong-jocelin/joprelys-connect`
- Base initiale : `main@81b7d20c4cf44800e436c86b964a38bc62929305`
- Flyway applicatif : V86
- HOS-RBAC-001-D : fusionné via PR #122
- Gouvernance HOS-RBAC : alignée via PR #124
- Continuité URG-TEMP : PR #96 fusionnée
- Workspace/E2E urgence : PR #97 fusionnée

## 3. Architecture à préserver

### Backend

Conserver les frontières existantes :

```text
Controller
→ use case / service applicatif
→ domaine / policies
→ repositories / infrastructure
```

Aucune règle métier de démonstration ne doit être introduite dans un controller ou le frontend.

### Frontend

Angular reste un orchestrateur d'interface :

- API par services ;
- guards/permissions existants ;
- Tailwind CSS v4 ;
- design system et thèmes existants ;
- i18n FR/EN ;
- aucune URL backend hardcodée.

## 4. Domaines à auditer

### Parcours normal

- patient ;
- visite ;
- constantes ;
- consultation ;
- hospitalisation ;
- structure service/chambre/lit.

### Parcours urgence

- patient provisoire URG-TEMP ;
- urgence/triage/réévaluation ;
- rapprochement canonique ;
- hospitalisation avec `emergencyId` ;
- documents et finance différée uniquement comme preuve de continuité, sans transformer la démo en recette exhaustive finance.

### Auth/RBAC

- permissions spécialisées hospitalières après V86 ;
- absence de fallback `HOSPITALIZATION_MANAGE` ;
- profils métiers strictement nécessaires à la démonstration ;
- tenant isolation.

## 5. Politique de modification avant démo

Une modification de code n'est autorisée dans cette branche que si :

1. le défaut est reproductible sur `main` ;
2. il bloque ou fragilise directement un scénario de démonstration ;
3. le correctif reste borné ;
4. aucun contournement ou dette n'est introduit ;
5. des tests de non-régression sont ajoutés ;
6. la documentation impactée est mise à jour en parallèle.

Les chantiers structurants (ABAC complet, délégations avancées, clearance complète) sont explicitement différés après la démo sauf blocage critique démontré.

## 6. Données et migrations

Aucune nouvelle migration n'est prévue pour la readiness de démonstration.

La baseline doit rester compatible avec V86. Une nouvelle migration ne serait acceptable que pour corriger un défaut de données P0 impossible à résoudre autrement, avec ticket séparé, test PostgreSQL 16 et analyse SemVer.

## 7. Stratégie de tests

Priorité aux preuves existantes. Ajouter seulement les scénarios manquants :

- tests backend intégration du parcours principal ;
- tests backend URG-TEMP/rapprochement/hospitalisation ;
- tests Angular navigation/permissions si lacune réelle ;
- migration/greenfield PostgreSQL 16 déjà couverts à réutiliser ;
- suite Maven et Angular globales selon le diff final.

## 8. Observabilité de la répétition

Pendant la répétition sur environnement déployé :

- relever uniquement les erreurs fonctionnelles et HTTP utiles ;
- aucun secret ou PII réelle dans les logs de recette ;
- utiliser des données de démonstration ;
- consigner l'étape, l'acteur, le résultat attendu et le résultat observé dans le runbook.

## 9. Sécurité

- deny-by-default inchangé ;
- aucun rôle élargi pour faciliter la démo ;
- aucune permission legacy ;
- aucun compte partagé si les comptes métiers peuvent être préparés séparément ;
- aucune donnée patient réelle nécessaire.

## 10. SemVer

Aucun bump applicatif pour la phase d'audit/QA/documentation. Toute correction de code éventuelle sera qualifiée séparément avant merge.
