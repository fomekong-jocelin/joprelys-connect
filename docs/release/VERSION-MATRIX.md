# VERSION MATRIX — Versions par module

> À utiliser surtout en monorepo ou si backend, web et mobile ne sortent pas toujours ensemble.

## Version globale projet

| Champ | Valeur |
|---|---|
| Version courante | 0.8.0 |
| Dernière release | 2026-07-04 |
| Stratégie | SemVer |
| Source de vérité | `VERSION` + `docs/ai/CHANGELOG.md` |

## Modules

| Module | Version | Type de version | Dernière release | Compatibilité | Notes |
|---|---:|---|---|---|---|
| Backend Spring Boot | 0.8.0 | SemVer | 2026-07-04 | API v0 | Allergies, antécédents, hospitalisations (optimiste), fiche de sortie PDF |
| Angular Web | 0.8.0 | SemVer | 2026-07-04 | API v0 | Visualisation/saisie des allergies, antécédents, hospitalisations et notes |
| Flutter Mobile | 0.3.0 | SemVer | 2026-07-01 | API v0 | À adapter au projet |
| API Contract | 0.8.0 | SemVer | 2026-07-04 | v0 | Endpoints allergies, antécédents et hospitalisations |
| Database Schema | 0.6.0 | Migration versionnée | 2026-07-04 | v0 | Flyway v18 allergies et v19 hospitalisations |

## Compatibilité API / clients

| API version | Backend min | Angular min | Flutter min | Statut | Fin support |
|---|---:|---:|---:|---|---|
| v0 | 0.3.0 | 0.3.0 | 0.3.0 | Active | À définir |

## Règles

- Si backend et clients sortent ensemble, garder une version globale unique.
- Si les clients mobile/web peuvent rester sur d'anciennes versions, maintenir cette matrice.
- Toute rupture API doit créer une ligne de compatibilité ou un bump MAJOR.
- Toute migration destructive doit être documentée dans une release note et/ou un ADR.


## Contraintes de build/UI

| Module | Standard obligatoire | Exceptions |
|---|---|---|
| Backend Spring Boot | Maven (`pom.xml`, `mvnw`) | ADR validée uniquement |
| Angular Web | Tailwind CSS ; Angular Material interdit | ADR validée uniquement |
| Spring Boot Config | `application.yml` / `application-<profile>.yml` | `application.properties` uniquement via ADR + migration plan |
| Angular Dev Proxy | `proxy.conf.json` + `proxyConfig` dans `angular.json` | Exception uniquement via ADR |

Dernière mise à jour gouvernance : 0.3.1.
| 0.3.3 | PATCH | Standards Frontend/Mobile : thème centralisé, dark/light, i18n FR/EN, composants réutilisables, configuration app/branding | Gouvernance UI/mobile renforcée |
