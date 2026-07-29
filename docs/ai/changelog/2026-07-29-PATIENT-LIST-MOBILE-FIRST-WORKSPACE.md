# 2026-07-29 — Liste Patients mobile-first

Issue #233 / PR #234.

- La page `Patients` devient un poste de travail compact : titre et sous-titre courts, sans lien Retour redondant avec le breadcrumb.
- La recherche `Nom, téléphone ou DPU` devient l’action principale, se lance avec Entrée et peut être effacée via une action dédiée ; le bouton texte `Rechercher` séparé disparaît.
- `Nouvelle admission` reste immédiatement accessible mais n’occupe plus le hero de la page.
- La liste mobile n’est plus enfermée dans une grande carte parent : chaque patient devient une carte indépendante et entièrement cliquable avec nom, sexe, DPU, téléphone, ville et chevron.
- Le bouton mobile `Voir le dossier` est supprimé ; le tableau desktop et son action explicite sont conservés.
- La liste non filtrée est libellée `Liste des patients` et non `Patients récents`, l’API ne garantissant pas actuellement un tri par récence.
- Aucun backend, contrat API, modèle de données ou RBAC n’est modifié.
- FR/EN sont portés par le dictionnaire de feature `patient-list`.
- SemVer : PATCH.
- Gate frontend final requis avant fusion.
