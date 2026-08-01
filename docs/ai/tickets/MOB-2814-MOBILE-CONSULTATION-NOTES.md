# TICKET: MOB-2814 — Saisie & consultation mobile des notes cliniques

## Infos
- Mode: Engineering
- Date: 2026-07-30
- Priorité: P1 - Clinical Flow
- Statut: IN_REVIEW — contrat et UI SOAP alignés ; gates Flutter, Maven et recette
  cross-stack encore requis avant DONE
- Target Release: candidat MINOR `0.11.0` ; aucune release préparée

## Description
Implémentation de l'interface mobile de consultation et saisie des notes cliniques SOAP (Subjectif, Objectif, Évaluation, Plan). La surface visuelle est conservée, mais doit être alignée sur le contrat canonique Angular/backend `GET/POST /api/visits/{id}/consultation` conformément à ADR-0005.

## Check-list d'intervention
- [x] Documentation First (`FUNCTIONAL-SPEC.md`, `TECHNICAL-DESIGN.md`, `API-CONTRACT.md`)
- [x] Création du modèle domain `ConsultationNote`
- [x] Création de la gateway API `ConsultationApi`
- [x] Création du composant modal UI `ConsultationNotesSheet`
- [x] Raccordement i18n FR/EN et thèmes Light/Dark
- [x] Raccordement de l'action `_ActionChip` sur la carte patient du Dashboard
- [x] Écriture des tests unitaires `consultation_api_test.dart`
- [x] Preuve historique `flutter test` 63/63 conservée ; non valable pour le HEAD actuel
- [x] Mise à jour du suivi `PROJECT-TRACKING.md` et `CHANGELOG.md`
- [x] Parité des quatre sections et sous-champs avec Angular
- [x] Supprimer `suspectedDiagnosis` et `finalDiagnosis` de toutes les couches
- [x] Migrer les valeurs existantes sans perte et supprimer les colonnes actives
- [x] Adapter les contrats IA, résumés patient et exports PDF
- [x] Contrat canonique `/api/visits/{id}/consultation` intégré
- [x] Tests de contrat backend/Angular/Flutter et test widget FR/EN ajoutés
- [x] `consultation_notes_sheet.dart` découpé de 573 à 236 lignes
- [x] Analyse Dart ciblée : aucun diagnostic remonté
- [x] Migration V109 vérifiée sur H2 2.4.240 : archive alimentée, valeur finale retenue et zéro colonne legacy active
- [ ] Exécution runtime des tests Flutter et Maven dans un environnement inscriptible/connecté
- [ ] Recette Angular ↔ Flutter sur la même consultation

## Checklist de review

- [x] Backend maître : validations et persistance restent côté Spring Boot.
- [x] Contrat API relatif et unique ; aucun alias mobile ajouté.
- [x] Un seul champ `diagnosis` subsiste dans l'API, les UI, l'IA et la base active.
- [x] Les valeurs diagnostiques antérieures sont archivées avant suppression des colonnes.
- [x] Flutter FR/EN et light/dark préservés via les services centraux.
- [x] Aucun widget ajouté ou modifié dans cette surface ne dépasse 300 lignes.
- [x] Aucun secret, token, audio ou contenu clinique ajouté aux logs.
- [x] Angular reste Tailwind CSS v4 sans Angular Material.
- [x] `ConsultationComponent` ramené sous 500 lignes par extraction de facades/services.
- [ ] `ConsultationComponent` (463) et `ClinicalNoteEditorComponent` (334) restent
  à découper vers la cible de 300 lignes avant extension importante.
- [x] Maven/YAML et `proxy.conf.json` préservés.
- [ ] Gates backend et Flutter exact-HEAD verts.
- [ ] Validation clinique et review Tech Lead signées.

## Reste à faire
- Faire reviewer la migration ADR-0005 par le Tech Lead backend.
- Réconcilier l'ID, car MOB-2814 est réservé au contrat backend audio dans
  EPIC-0028.
- Exécuter les gates Maven/Flutter dans un environnement autorisé, puis valider
  le flux complet en CI exact-HEAD et recette cross-stack.
- Planifier le découpage Angular restant sous le seuil d'alerte de 300 lignes.
- Découper la dette préexistante `PdfGeneratorService` (1 180 lignes) sous la
  limite de 500 avant extension de cette surface PDF.
