# STORY-0302 — Recherche de Patients Multicritères

## 1. User story

En tant qu'**utilisateur clinique**, je veux **rechercher un patient par son nom, son DPU ou son numéro de téléphone**, afin de **pouvoir rapidement accéder à son dossier médical ou ouvrir une visite**.

## 2. Critères d'acceptation

- [ ] L'interface propose un champ de recherche unique.
- [ ] La recherche est multicritère et s'effectue sur le DPU, le nom complet (nom/prénom) ou le numéro de téléphone du patient.
- [ ] La recherche prend en compte les correspondances partielles et insensibles à la casse (ex. chercher "Chan" doit retourner "Chantal").
- [ ] Les résultats sont paginés (10 par page par défaut) avec indication du nombre total de correspondances. L'API REST utilise Spring Data `Pageable` et retourne une structure enveloppée `Page<PatientDTO>` (voir [ADR-0002](docs/ai/adr/ADR-0002-pagination-lazy-loading.md)).
- [ ] L'affichage des résultats comprend : DPU, Nom complet, Date de naissance, Sexe, Téléphone, et Ville.
- [ ] Un clic sur un résultat redirige l'utilisateur vers la fiche profil du patient.
- [ ] Toutes les recherches effectuées sont journalisées dans les logs d'audit.

## 3. Périmètre

### Inclus

- API REST d'interrogation multicritère filtrée par clinique (`organization_id`).
- Composant Frontend de recherche avec table de résultats et pagination.
- Indexation SQL appropriée pour garantir des performances de recherche rapides.

### Exclus

- Recherche phonétique avancée (ex. Soundex).

## 4. Tâches

| ID | Titre | Stack | Profil recommandé | SP | Estimation | Statut |
|---|---|---|---|---:|---:|---|
| TASK-0302-01 | Indexation SQL de la table `patients` (index sur name, phone, global_patient_number) | DB / SQL | Intermédiaire | 1 | 0.25j | TODO |
| TASK-0302-02 | Service de recherche JPA/SQL multicritère paginé (Spring Data Pageable) | Backend | Intermédiaire | 1 | 0.5j | TODO |
| TASK-0302-03 | API REST GET `/api/v1/patients/search?q=&page=&size=&sort=` (Page<PatientDTO>) | Backend | Junior | 1 | 0.25j | TODO |
| TASK-0302-04 | Composant de recherche et pagination Frontend (chargement paginé) | Frontend | Junior | 1 | 0.75j | TODO |
| TASK-0302-05 | Intégration et redirection vers la fiche profil | Frontend | Junior | 1 | 0.25j | TODO |
| TASK-0302-06 | Tests unitaires et d'intégration de recherche | Test | Intermédiaire | 1 | 0.25j | TODO |

## 5. Estimation

| Champ | Valeur |
|---|---|
| Story points | 3 |
| Complexité | S |
| Profil recommandé | Intermédiaire |
| Effort senior | 0.5j |
| Effort intermédiaire | 0.65j |
| Effort junior | 1.1j |
| Risque | Faible |

## 6. Definition of Ready

- [x] Critères d'acceptation clairs
- [x] Dépendances connues (Patient DPU)
- [x] Profil recommandé identifié
- [x] Estimation faite
- [x] Reviewer identifié (Lead Developer)

## 7. Definition of Done

- [ ] API REST et contrôles de pagination opérationnels
- [ ] Interface fluide et responsive
- [ ] Performance de recherche validée sous charge simulée

## Impact version / SemVer

| Champ | Valeur |
|---|---|
| Impact version | MINOR |
| Justification | Ajout du moteur de recherche de patients paginé |
| Breaking change | Non |
| Release cible | v0.4.0 |
