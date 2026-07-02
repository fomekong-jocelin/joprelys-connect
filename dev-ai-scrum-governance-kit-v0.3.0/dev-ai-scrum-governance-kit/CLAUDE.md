# CLAUDE.md

Lire et respecter obligatoirement :

- `AGENTS.md`
- `SKILL.md`
- `PROJECT-MANAGER-SKILL.md` si la demande concerne projet, sprint, capacité, estimation, retard, priorité ou découpage
- `docs/ai/README-IA.md`
- `docs/ai/WORKFLOW-IA.md`
- `docs/ai/PROJECT-TRACKING.md`
- `docs/ai/CHANGELOG.md`
- `docs/ai/review-checklist.md`
- `docs/ai/REFERENCES.md`
- `docs/release/SEMANTIC-VERSIONING.md` pour toute demande de version/release
- `docs/release/RELEASE-WORKFLOW.md` pour toute livraison

Ne jamais traiter une demande comme un ticket isolé.

Pour chaque intervention :

1. déterminer le mode : Engineering, Project Manager, QA, Diagnostic ou Architecture ;
2. créer ou mettre à jour un ticket dans `docs/ai/tickets/` ;
3. découper en epic/story/task si la demande est macro ;
4. estimer selon `docs/pm/ESTIMATION-GUIDE.md` ;
5. vérifier la capacité selon `docs/pm/CAPACITY-PLANNING.md` ;
6. cocher ce qui est terminé ;
7. documenter ce qui reste ;
8. mettre à jour le suivi projet ;
9. mettre à jour le changelog si nécessaire ;
10. appliquer SemVer et proposer le bon bump si une livraison est concernée.
