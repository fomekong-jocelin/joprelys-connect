# TASK-20260801 — Consolider la livraison mobile avant toute nouvelle feature

## Statut

IN_PROGRESS P0 — alignement SOAP et réconciliation IDs/PR terminés ; reviews,
dettes de taille, gates et recette restent requis avant passage à DONE.

## Mode d'intervention

Project Manager + diagnostic de readiness + Engineering de consolidation.

## Objectif

Rendre l'incrément Flutter initialement accumulé sur
`fix/mobile-dashboard-layout-overflow`, désormais fusionné dans `main`,
traçable, conforme aux contrats backend, reviewable et testable sur Android
avant de poursuivre le backlog produit.

## Actions de cadrage réalisées

- [x] Lire la gouvernance engineering, PM, QA, documentation, design et
  configuration du dépôt.
- [x] Comparer la branche courante à `origin/main` et mesurer le diff cumulé.
- [x] Recouper les tickets récents avec le découpage canonique d'EPIC-0028.
- [x] Comparer les routes et DTO Consultation Flutter aux controllers/DTO Spring
  Boot existants.
- [x] Mesurer les principaux widgets Flutter par rapport aux seuils 300/500
  lignes.
- [x] Vérifier `.gitignore`, Maven/YAML, le proxy Angular et l'absence de nouveau
  marqueur Angular Material/Tailwind v3 dans le périmètre inspecté.
- [x] Tenter les gates d'analyse Flutter et consigner leur timeout sans les
  déclarer verts.
- [x] Mettre à jour le suivi central, le backlog EPIC-0028, le dashboard delivery
  et le registre des risques.

## Réconciliation et fusion du 2026-08-01

- commit de consolidation : `124b7c12` ;
- PR #258 mise à jour pour refléter le périmètre complet, puis fusionnée dans
  `main` au merge commit `e6f7a348` ;
- branche `fix/mobile-dashboard-layout-overflow` supprimée localement et sur
  `origin` après vérification de la fusion ;
- mapping appliqué :
  - annuaire `MOB-2817` historique → `MOB-2809` canonique ;
  - historique `MOB-2816` historique → `MOB-2810` canonique ;
  - constantes `MOB-2807` historique → `MOB-2811` canonique ;
  - assistant UI `MOB-2815` historique → `MOB-2816` canonique ;
  - notes SOAP `MOB-2814` historique → nouvelle story `MOB-2821` ;
- les tickets, les spécifications, le changelog, le tracking, l'epic, le
  dashboard delivery et le registre des risques utilisent désormais ces IDs.

## Constat initial au 2026-08-01

- la branche est propre mais se trouve 16 commits devant `origin/main` ;
- le diff cumulé porte sur 61 fichiers, avec environ 6 011 insertions et 424
  suppressions ;
- plusieurs fonctionnalités distinctes ont été empilées sur une branche nommée
  comme un correctif de layout ;
- les identifiants utilisés ne correspondent plus au découpage canonique de
  `EPIC-0028` :
  - `MOB-2807` désigne l'agenda dans l'epic, mais les constantes dans le ticket
    livré ;
  - `MOB-2814` désigne le contrat backend audio dans l'epic, mais les notes SOAP
    dans le ticket livré ;
  - `MOB-2815` désigne la file locale audio chiffrée dans l'epic, mais l'assistant
    vocal UI dans le ticket livré ;
  - `MOB-2816` désigne la consultation IA mobile dans l'epic, mais l'historique
    patient dans le ticket livré ;
  - `MOB-2817` désigne la validation médecin dans l'epic, alors que le changelog
    l'utilise pour l'annuaire patient sans ticket dédié ;
- le mobile appelle `GET/POST /api/visits/{id}/consultation-notes` avec les champs
  `subjective/objective/assessment/plan`, alors que le backend expose
  `GET/POST /api/visits/{id}/consultation` avec le contrat
  `symptoms/clinicalExam/suspectedDiagnosis/diagnosis/finalDiagnosis/conclusion/advice/followUp` ;
- quatre widgets dépassent la limite dure de 500 lignes :
  - `active_queue_section.dart` : 1 205 lignes ;
  - `clinical_voice_assistant_sheet.dart` : 819 lignes ;
  - `consultation_notes_sheet.dart` : 573 lignes ;
  - `patient_history_sheet.dart` : 591 lignes ;
- `patient_vitals_sheet.dart` atteint 477 lignes et dépasse le seuil d'alerte de
  300 lignes ;
- quatre artefacts `mobile/test/failures/auth_login_dark_*.png` sont versionnés ;
- la version applicative courante est `0.10.1`, tandis que plusieurs tickets
  qualifient `v1.1.0` de cible MINOR : ce saut serait MAJOR selon SemVer et doit
  être remplacé par une cible cohérente ou justifié comme préparation 1.0 ;
- `flutter analyze`, puis `flutter analyze --no-pub`, n'ont produit aucun résultat
  avant leurs timeouts locaux respectifs de 180 s et 90 s. Aucun succès de gate
  exact-HEAD ne peut donc être revendiqué dans cette intervention ; même
  `flutter --version` a expiré après 15 s, ce qui qualifie d'abord un problème
  d'exécution de la toolchain locale plutôt qu'un résultat qualité du code.

## Avancement SOAP au 2026-08-01

- Flutter utilise désormais la route canonique ; le contrat passe de huit à six champs utiles ;
- Angular et Flutter rendent les mêmes sections S/O/A/P ;
- la décision produit supprime `suspectedDiagnosis` et `finalDiagnosis` au profit d'un unique `diagnosis` ;
- `consultation_notes_sheet.dart` est ramené de 573 à 236 lignes ;
- les tests ciblés Angular sont verts et l'analyse Dart ciblée est sans diagnostic ;
- Maven reste bloqué par le parent Spring Boot absent du cache et Flutter runtime
  par le verrou SDK non inscriptible dans le sandbox.
- l'incrément complet est fusionné par la PR #258 ; les limites de validation
  restent explicites et les tickets ne sont pas déclarés DONE.

## Découpage

### EPIC

Restaurer une baseline mobile reviewable, contractuelle et testée avant reprise
du développement fonctionnel d'EPIC-0028.

### User Story

En tant que Tech Lead mobile, je veux un incrément Flutter aligné sur le backlog
et les contrats Spring Boot, afin de pouvoir le reviewer, le tester sur Android
et le fusionner sans masquer de régression clinique.

### Tasks et subtasks

#### TASK A — Réconcilier le backlog et les tickets

- [x] Appliquer la correspondance canonique entre les capacités livrées et les
  IDs d'EPIC-0028 sur autorisation explicite de réconciliation.
- [x] Renommer ou remplacer les tickets en conflit sans perdre l'historique.
- [x] Créer le ticket manquant MOB-2809 pour l'annuaire/recherche patient.
- [x] Documenter l'exception de la PR cumulative #258, mettre son titre et son
  corps à jour, la fusionner puis supprimer la branche intégrée.
- [x] Aligner `PROJECT-TRACKING.md`, le backlog EPIC-0028 et le changelog.
- [x] Réconcilier les cibles de version avec `VERSION=0.10.1` : candidat MINOR
  `0.11.0`, sans préparation de release ni bump.

Estimation : 2 SP / 0,5 à 1 jour senior. Profil : Tech Lead + Product Owner.

#### TASK B — Corriger le contrat Consultation mobile/backend

- [x] Choisir l'orientation : contrat SOAP sémantique unique, backend maître et
  conservation du modèle détaillé existant ; ADR-0005 créée au statut proposé.
- [ ] Faire accepter ADR-0005 par le référent clinique, le Product Owner et le
  Tech Lead.
- [x] Expliciter les sections `S — Subjectif`, `O — Objectif`,
  `A — Évaluation`, `P — Plan` dans Angular et Flutter.
- [x] Aligner Flutter sur `/api/visits/{id}/consultation` et sur les mêmes
  sous-champs que le backend/Angular ; ne pas ajouter d'alias mobile.
- [x] Supprimer `suspectedDiagnosis` et `finalDiagnosis` des contrats, UI et traitements IA.
- [x] Archiver les valeurs existantes, migrer vers `diagnosis`, puis supprimer les colonnes actives.
- [x] Mettre à jour `API-CONTRACT.md`, les modèles Angular/Flutter et les mappers.
- [x] Couvrir dans les tests les statuts 200, 204, 400, 403 et 404.
- [x] Ajouter les tests ciblés backend, Angular et Flutter.
- [ ] Exécuter les gates backend/Flutter exact-HEAD et réaliser un E2E de parité
  sur la même consultation.

Estimation : 5 SP / 2 à 3 jours senior. Profil : senior Spring + Angular/Flutter
+ reviewer clinique.

#### TASK C — Découper les widgets hors limites

- [ ] Extraire la file active, l'annuaire, les actions cliniques et leurs états
  de `active_queue_section.dart`.
- [x] Extraire orchestration, surface d'écoute et rendu des résultats de
  `clinical_voice_assistant_sheet.dart`.
- [ ] Découper les feuilles notes, historique et constantes en composants
  présentationnels réutilisables.
- [ ] Maintenir chaque fichier sous 500 lignes, cible 300 lignes, et conserver le
  backend comme maître de la vérité métier.
- [ ] Ajouter ou adapter les tests widgets après extraction.
- [x] Ramener `ConsultationComponent` Angular sous la limite dure de 500 lignes
  via `ConsultationPrescriptionFacade`, `ConsultationFeedbackStore` et
  `ConsultationUiLabelsService`.
- [ ] Poursuivre le découpage de `ConsultationComponent` (481 lignes) et
  `ClinicalNoteEditorComponent` (358 lignes) vers la cible d'alerte de 300.
- [ ] Poursuivre le découpage de l'orchestration vocale Flutter (379 lignes) et
  de sa surface d'écoute (326 lignes) vers la cible de 300.

Estimation : 5 SP / 2 à 3 jours senior. Profil : senior Flutter + reviewer
architecture.

#### TASK D — Nettoyer les artefacts et fermer les gates

- [ ] Retirer du périmètre livrable les images de failure générées et compléter
  `.gitignore` si nécessaire, sans supprimer les goldens de référence valides.
- [ ] Exécuter le format Dart, `flutter analyze` et `flutter test` sur le HEAD
  final.
- [ ] Exécuter le build APK debug exact-HEAD en CI.
- [ ] Réaliser la recette Android FR/EN, light/dark, texte agrandi, clavier,
  réseau réel, permissions micro et parcours backend.

Estimation : 3 SP / 1 à 1,5 jour senior + QA appareil. Profil : senior Flutter,
QA mobile et backend/auth.

## Estimation consolidée

| Élément | Valeur |
|---|---|
| Story points | 15 SP, découpés en quatre tasks livrables |
| Effort senior | 6 à 8,5 jours |
| Effort intermédiaire | 8 à 11 jours avec accompagnement |
| Profil recommandé | Tech Lead + senior Flutter + senior Spring + QA mobile |
| Risque | Élevé tant que le contrat et les gates ne sont pas fermés |
| Capacité | À engager sur un sprint dédié à 60–70 %, sans nouvelle feature en parallèle |

## Critères d'acceptation

- [x] Aucun identifiant MOB actif n'a deux significations dans le backlog, les
  tickets et le changelog ; les anciens usages sont limités à la table de
  transition historique.
- [x] La PR cumulative #258 constitue l'exception explicitement autorisée : son
  périmètre et ses limites ont été documentés avant fusion, et sa branche a été
  supprimée après intégration.
- [ ] Le contrat Consultation exécuté par Flutter correspond exactement au
  controller et aux DTO Spring Boot documentés.
- [ ] Aucun fichier source Flutter modifié ne dépasse 500 lignes ; toute cible
  supérieure à 300 lignes est justifiée ou découpée.
- [ ] Aucun artefact de failure de test n'est livré comme source produit.
- [x] La cible SemVer est cohérente avec `0.10.1` ; aucun saut vers `1.1.0` n'est
  présenté comme un MINOR.
- [ ] `flutter analyze`, `flutter test` et le build APK debug sont verts sur le
  même HEAD.
- [ ] La recette Android réelle couvre FR/EN, light/dark et les parcours cliniques
  connectés au backend.
- [ ] Les tickets ne repassent à DONE qu'après preuves de contrat, review et QA.

## Definition of Ready

- [x] Écart de backlog et défaut de contrat reproduits par inspection du code.
- [x] Fichiers hors limites identifiés et mesurés.
- [x] Profil, estimation, reviewer attendu et tests requis documentés.
- [x] Mapping canonique des IDs appliqué sur autorisation explicite ; review
  Product Owner/Tech Lead conservée comme contrôle de gouvernance post-fusion.
- [ ] Capacité nominative du sprint confirmée.

## Definition of Done

- [ ] Toutes les tâches A à D sont terminées.
- [ ] Review Tech Lead, backend et QA mobile approuvée.
- [ ] Documentation, suivi, changelog et décision SemVer alignés.
- [ ] Aucun risque critique ouvert pour la fusion.

## Reviewer

Tech Lead Flutter + Lead Backend + QA mobile + référent clinique ; Product Owner
pour la réconciliation des IDs.

## Sécurité / régression

- ne pas modifier silencieusement les permissions `CLINICAL_READ`,
  `CLINICAL_WRITE`, `VISIT_READ` et `VISIT_VITALS_WRITE` ;
- ne pas déplacer de règle clinique dans Flutter pour adapter le contrat ;
- ne pas journaliser token, OTP, audio, transcript ou donnée patient ;
- valider le microphone et les données de santé sur appareil avec la stratégie
  DPO prévue par EPIC-0028.

## Impact version / SemVer

Aucun bump pour ce cadrage documentaire. L'incrément mobile fonctionnel reste un
candidat MINOR uniquement après consolidation, QA et préparation de release.

## Reste à faire

TASK A est terminée et TASK B est implémentée mais encore en review. Poursuivre
par la review V109/ADR-0005, TASK C et TASK D. Les capacités fusionnées restent
IN_REVIEW ou REVIEW_BLOCKED jusqu'aux gates exact-HEAD et à la recette clinique ;
aucune branche de livraison cumulative ne subsiste.
