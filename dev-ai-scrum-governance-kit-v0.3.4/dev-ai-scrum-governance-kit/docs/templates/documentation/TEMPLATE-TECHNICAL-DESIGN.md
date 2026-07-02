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
