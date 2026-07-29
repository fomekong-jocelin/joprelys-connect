# FIX-20260729 — Suppression du sous-menu patient dupliqué

GitHub issue : #239

## Contexte

Le shell global injectait dynamiquement, sous l’entrée `Patients`, un second menu patient complet lorsque `ActivePatientService` contenait un patient actif. Ce menu dupliquait exactement la navigation locale déjà fournie par `PatientRecordNavigationComponent` dans le dossier patient.

## Décision UX

- La sidebar globale reste une navigation de modules.
- Le dossier patient conserve sa propre navigation locale.
- Aucun sous-menu patient dynamique n’est généré dans `AppShellNavComponent`.
- La correction s’applique simultanément au desktop et au mobile car les deux rendus utilisent le même composant `app-shell-nav`.

## Périmètre technique

- suppression de la dépendance `ActivePatientService` dans `AppShellNavComponent` ;
- suppression de `appendActivePatientItems(...)` et de son appel ;
- conservation de l’entrée globale `/patients` soumise à `PATIENT_READ` ;
- aucun changement de `PatientRecordNavigationComponent`, des routes ou du RBAC ;
- ajout d’un test empêchant la réintroduction de routes `/patients/:id/...` dans la navigation globale.

## Critères d’acceptation

- [x] La sidebar globale ne contient plus le bloc `Fiche d’identité : <patient>`.
- [x] Aucun lien Fiche d’identité / Dossier médical / Analyses & Labo / Hospitalisations / Journal d’Audit n’est injecté dans la sidebar globale.
- [x] L’entrée globale `Patients` reste disponible selon `PATIENT_READ`.
- [x] La navigation locale du dossier patient reste intacte.
- [x] Le même comportement vaut pour desktop et mobile.
- [ ] CI Angular et build production verts.

## SemVer

PATCH — correction UX sans changement de contrat API ni de données.
