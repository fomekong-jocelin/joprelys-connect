# TECHNICAL-DESIGN — <Feature name>

## 1. Objectif technique

Décrire l’approche technique retenue.

## 2. Stack concernée

- [ ] Spring Boot
- [ ] Angular
- [ ] Flutter
- [ ] Base de données
- [ ] CI/CD
- [ ] Documentation

## 3. Contraintes projet obligatoires

- Backend Spring Boot : Maven uniquement (`pom.xml`, `mvnw`).
- Backend Spring Boot : `application.yml` obligatoire, pas de `application.properties`.
- Angular : Tailwind CSS obligatoire, Angular Material interdit sauf ADR.
- Angular : `proxy.conf.json` obligatoire et URLs API relatives.
- Angular / Flutter : thème centralisé, light/dark, i18n FR/EN, configuration app/branding.
- Documentation fonctionnelle et technique maintenue dès le démarrage.

## 4. Architecture cible

Décrire les modules, couches et dépendances.

```text
Controller / Page / Screen
  → Service / Facade / UseCase
  → Domain
  ← Infrastructure / Repository / API Client
```

## 5. Fichiers ou modules impactés

| Module | Fichier | Type d’impact |
|---|---|---|
| | | |

## 6. Contrats API

| Méthode | Endpoint | Request | Response | Erreurs |
|---|---|---|---|---|
| | | | | |

## 7. Modèle de données / migrations

| Élément | Description | Migration requise |
|---|---|---|
| | | Oui / Non |

## 8. Configuration

| Paramètre | Fichier | Valeur / source | Environnement |
|---|---|---|---|
| | `application.yml` / `app.config.ts` / `app_config.dart` | | |

## 9. Sécurité

- [ ] Authentification requise
- [ ] Autorisation / rôle requis
- [ ] Inputs validés
- [ ] Requêtes paramétrées
- [ ] Pas de secret dans le code
- [ ] PII masquée dans logs

## 10. Observabilité

- Logs attendus :
- Métriques :
- Traces :
- Corrélation / request id :

## 11. Tests prévus

| Niveau | Tests attendus | Commande |
|---|---|---|
| Unit | | |
| Integration | | |
| UI / Widget | | |
| E2E | | |

## 12. Impact version / SemVer

| Champ | Valeur |
|---|---|
| Type de bump | Aucun / PATCH / MINOR / MAJOR |
| Justification | |
| Breaking change | Oui / Non |
| Migration requise | Oui / Non |

## 13. Risques techniques

| Risque | Impact | Mitigation |
|---|---|---|
| | | |

## 14. Historique des mises à jour

| Date | Auteur | Changement |
|---|---|---|
| YYYY-MM-DD | | Création |

## Vérification DESIGN.md / Tailwind v4

Pour tout changement UI Angular/Flutter :

- `DESIGN.md` lu ou mis à jour ;
- `docs/standards/DESIGN-SYSTEM-STANDARDS.md` appliqué ;
- Angular en Tailwind CSS v4 CSS-first ;
- absence de Tailwind v3 et Angular Material sauf ADR ;
- Flutter aligné sur les tokens du design system ;
- light/dark, i18n FR/EN et accessibilité vérifiés.


## Découpage SOLID et responsabilités

### Couche backend

| Élément | Responsabilité | Interface | Implémentation | Tests |
|---|---|---|---|---|
| Controller | Endpoint HTTP + délégation | N/A | | |
| Use case / Service | Orchestration métier | | | |
| Domain | Règles métier / invariants | | | |
| Infrastructure | Persistence / clients externes | | | |

### Couche front/mobile

| Élément | Responsabilité | Ce qui est interdit |
|---|---|---|
| Page / Screen | Orchestration UI | Règle métier critique |
| Component / Widget | Affichage réutilisable | Logique complexe |
| Service / Facade / Provider | Appels API + état présentation | Décision métier source de vérité |

### Vérifications

- [ ] Le backend est maître de la règle métier.
- [ ] Le frontend/mobile ne contient pas de logique métier critique.
- [ ] Les controllers ne contiennent aucune logique métier.
- [ ] Les services/use cases sont clairement séparés.
- [ ] Les abstractions sont justifiées.
- [ ] Les limites de taille sont respectées.
