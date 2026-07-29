# 2026-07-29 — Journal d’audit mobile compact

Issue #231 / PR #232.

- Les entrées d’audit utilisent désormais le libellé i18n court associé à leur action lorsqu’il existe ; `READ_AUDIT` affiche `Lecture audit` en français.
- Le motif original reste disponible comme fallback si l’action n’a pas de libellé localisé.
- Dans un événement ouvert, `Action : valeur` et `IP : valeur` sont affichés chacun sur une seule ligne.
- Ressource, user-agent, RBAC, données d’audit et comportement des accordéons restent inchangés.
- Correctif frontend rétrocompatible : SemVer PATCH.
