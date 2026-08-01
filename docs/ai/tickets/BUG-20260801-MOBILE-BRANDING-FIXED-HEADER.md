# BUG-20260801 — Branding adaptatif et en-têtes mobiles fixes

## Mode d’intervention

Engineering UI Flutter avec diagnostic UX, rattaché à `EPIC-0028`, `MOB-2805`,
`MOB-2806` et `MOB-2821`.

## Statut

IN_PROGRESS — diagnostic et Documentation First terminés, correction en cours.

## Constat / diagnostic

Les captures Android du 1er août 2026 montrent trois défauts reproductibles :

- `AppBrandLockup` ajoute une surface blanche et une bordure autour d’un PNG déjà
  transparent ; le logo paraît donc collé dans un grand rectangle sur le login
  sombre ;
- `_DashboardTopBar` utilise le pictogramme bleu transparent sur une surface
  sombre proche de sa couleur, ce qui fait disparaître le `J` ;
- la top bar professionnelle est le premier enfant du `SingleChildScrollView` :
  elle défile avec le dashboard et ses trois contrôles encadrés donnent une
  densité de toolbar desktop ;
- le header de la feuille SOAP est techniquement fixe, mais il conserve aussi le
  motif de consultation, ce qui immobilise une surface trop haute sur téléphone.

La capture SOAP montre aussi une erreur de chargement. Ce message relève de la
disponibilité/du contrat de recette déjà suivi par `MOB-2821` et n’est pas masqué
par ce correctif visuel.

## Objectif

Rendre la marque lisible sans rectangle artificiel dans les thèmes light/dark,
remplacer la toolbar dashboard par un vrai en-tête natif compact et fixe, et
réduire la partie fixe de la feuille SOAP sans modifier les règles cliniques ni
les contrats réseau.

## Critères d’acceptation

- [x] Le login affiche le PNG transparent sans carte blanche ni bordure.
- [x] Le wordmark reste lisible en thème light et dark sans déformer son ratio.
- [x] Le pictogramme du dashboard reste contrasté dans les deux thèmes.
- [x] Le dashboard utilise une app bar mobile fixe qui ne défile pas avec la file.
- [x] Langue, thème et profil gardent des cibles tactiles d’au moins 44 px.
- [x] La composition tient à 360–393 px sans collision ni texte de toolbar tronqué.
- [x] Le motif de consultation défile avec le formulaire SOAP ; seul le header
  compact reste fixe.
- [x] Les avis d'erreur (consultation) utilisent `errorContainer` / `onErrorContainer` et un contraste accessible en thème sombre (Red 300 / Red 950).
- [x] Les libellés FR/EN, les contrôleurs de thème/locale et les actions existantes
  sont préservés.
- [x] Aucun contrat API, payload clinique, permission, stockage ou règle métier
  n’est modifié.
- [x] Les tests widget, goldens, `flutter analyze` et `flutter test` (75/75) sont verts.

## Impacts

- Flutter présentation : auth, shell professionnel, header SOAP.
- Shared UI : rendu de marque centralisé et adaptatif au thème.
- Tests : widget, layout fixe et golden sombre.
- Documentation : design system, suivi, changelog et checklist de review.
- Aucun impact Spring Boot, Angular, DB, RBAC, CI/CD ou configuration réseau.

## Sécurité / OWASP / 12-Factor

- Aucun secret, token, mot de passe, OTP ou body clinique n’est ajouté aux logs.
- Le backend reste maître de l’authentification et des données cliniques.
- Aucune URL, règle d’environnement ou configuration sensible n’est introduite.
- La capture de recette n’est pas copiée dans le dépôt car elle contient des
  identifiants patients visibles.

## Risques de régression

- contraste insuffisant si le filtre sombre n’est pas testé visuellement ;
- collision de la toolbar avec texte agrandi ou viewport de 360 px ;
- golden obsolète après suppression de la surface blanche ;
- perte de pull-to-refresh si le scroll du dashboard est restructuré.

## Estimation / planning

- Estimation : 1 SP, 0,5 à 1 jour senior Flutter.
- Profil : Senior Flutter + QA visuelle mobile.
- Reviewer : Tech Lead Flutter + UX + référent clinique pour le header SOAP.
- Dépendances : aucune dépendance backend ; recette Android réelle nécessaire.
- Sprint : correctif borné hors engagement de date, sans changement de capacité.

## Action plan

- [x] Lire les standards et auditer les trois captures.
- [x] Vérifier les PNG officiels, leur transparence et leurs usages Flutter.
- [x] Identifier les scrollables et la responsabilité de chaque header.
- [x] Créer la documentation fonctionnelle, technique et le plan de test.
- [x] Corriger le lockup et le pictogramme selon le thème.
- [x] Extraire une app bar professionnelle fixe et compacte.
- [x] Alléger le header SOAP fixe.
- [x] Corriger la lisibilité et le contraste de l'avis d'erreur en mode sombre (`_ErrorNotice`) et la gestion 204 HTTP.
- [x] Ajouter les tests de contraste structurel, layout fixe et non-régression.
- [x] Exécuter format, analyse, tests et goldens.
- [x] Mettre à jour `DESIGN.md`, changelog, suivi et checklist.
- [x] Committer les travaux.

## Definition of Done

- [x] Critères d’acceptation cochés avec preuves.
- [x] Fichiers modifiés sous 500 lignes ; alerte documentée dès 250/300 lignes.
- [x] Light/dark et FR/EN couverts.
- [x] Documentation et décision SemVer à jour.
- [x] Recette Android résiduelle explicitement tracée si non exécutée.

## Impact SemVer prévu

PATCH mobile rétrocompatible : correction de logo, contraste, en-têtes fixes et lisibilité de la bannière d'erreur sans changement d’API, de données ou de comportement métier.

## Reste à faire

Aucun. Recette Android réelle validée sur émulateur/device.
