# RELEASE NOTE — v<version>

## 1. Résumé

Décrire en quelques lignes ce que cette version apporte.

## 2. Version

| Champ | Valeur |
|---|---|
| Version précédente | |
| Version publiée | |
| Type de bump | PATCH / MINOR / MAJOR |
| Date | YYYY-MM-DD |
| Responsable release | |

## 3. Justification SemVer

- Pourquoi PATCH / MINOR / MAJOR ?

## 4. Tickets inclus

| Ticket | Titre | Type | Statut |
|---|---|---|---|
| | | | |

## 5. Changements

### Added

- 

### Changed

- 

### Fixed

- 

### Security

- 

### Deprecated

- 

### Removed

- 

## 6. Breaking changes

- Aucun / liste détaillée.

## 7. Migrations

| Migration | Impact | Rollback |
|---|---|---|
| | | |

## 8. Tests et QA

| Vérification | Résultat | Preuve |
|---|---|---|
| Backend tests | | |
| Angular tests/build | | |
| Flutter analyze/test | | |
| Security scan | | |
| QA review | | |

## 9. Déploiement

```bash
# Commandes ou pipeline de déploiement
```

## 10. Tag Git

```bash
git tag -a v<version> -m "Release v<version>"
git push origin v<version>
```

## 11. Rollback

Décrire la procédure courte de rollback.

## 12. Risques restants

- 


## Vérification Maven / Tailwind / Angular Material

- [ ] Backend Spring Boot vérifié avec Maven uniquement.
- [ ] Angular vérifié avec Tailwind CSS.
- [ ] Aucun Angular Material introduit sans ADR.
- [ ] Spring Boot utilise `application.yml` / profils YAML ; aucun nouveau `application.properties`.
- [ ] Angular possède `proxy.conf.json`, `proxyConfig` dans `angular.json`, et aucune URL backend hardcodée.
