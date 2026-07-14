# 2026-07-14 — STORY-2402 Révocation persistante des sessions

## Changements

- Ajout de la liste des sessions du compte courant.
- Ajout de la consultation administrative same-tenant avec `AUTH_SESSION_MANAGE`.
- Ajout de la révocation persistante, du logout courant et du logout-all.
- Validation en base des accès modernes liés à une session.
- Compatibilité persistante pour les anciens accès sans identifiant de session.
- Détection du rejeu après rotation et révocation de la famille concernée.
- Ajout d’un audit append-only des événements de session.
- Ajout de la migration Flyway V67.

## Sécurité

- Révocation effective sur toutes les instances partageant la base.
- Refus cross-tenant sans révéler l’existence de la session.
- Aucun secret ni adresse IP complète dans les réponses.
- Opérations idempotentes.
- Codes d’erreur stables à traduire en français et en anglais dans #34.

## Validation

- GitHub Actions #643 réussie sur `41875195`.
- Maven strict, H2, PostgreSQL 16, concurrence, Angular et build production réussis.

## SemVer

- MINOR : endpoints et migration additifs.
