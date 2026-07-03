# STORY-1103 — Gestion réelle des stocks de médicaments
 
> Ticket d'évolution fonctionnelle de gestion des stocks de pharmacie.
 
## 1. Objectif
 
Remplacer l'état déclaratif par un tableau de suivi des stocks physiques de médicaments dans la clinique, avec décrémentation automatique des quantités disponibles lors d'une délivrance validée.
 
## 2. Critères d'acceptation
 
- [ ] Création de la table `drug_stocks` liée à l'organisation (tenant).
- [ ] Le pharmacien reçoit une alerte si la quantité demandée dépasse le stock physique de la clinique.
- [ ] La validation d'une dispensation décrémente le stock en transaction sécurisée.
- [ ] API de mise à jour / approvisionnement des stocks documentée.
 
## 3. Pilotage projet
 
| Champ | Valeur |
|---|---|
| Epic parent | EPIC-0010 — Dispensation en Pharmacie & Gestion des Prescriptions |
| User story parent | STORY-1103 |
| Sprint cible | SPRINT-0005 |
| Priorité business | P1 |
| Complexité | XL |
| Story points | 8 |
| Profil recommandé | Intermédiaire |
| Effort estimé senior | 1.8j |
| Effort estimé intermédiaire | 2.3j |
| Effort estimé junior | 4.0j |
| Responsable | Intermédiaire |
| Reviewer obligatoire | Tech Lead |
| Risque fonctionnel | Fort |
| Risque technique | Moyen |
| Dépendances | STORY-1005 |
| Bloquants connus | Aucun |
 
## 4. Action plan
 
- [ ] Rédiger la migration Flyway pour `drug_stocks`.
- [ ] Implémenter les entités et repositories.
- [ ] Ajouter les contrôles de stock dans `PharmacyService.java`.
- [ ] Ajouter les tests unitaires et d'intégration.
- [ ] Créer l'IHM d'inventaire stock dans le portail pharmacie.
- [ ] Mettre à jour `CHANGELOG.md` et `PROJECT-TRACKING.md`.
 
## 13. Statut final
 
Statut : TODO
 
## 14. Impact version / SemVer
 
| Champ | Valeur |
|---|---|
| Changement livrable | Oui |
| Type de bump | MINOR |
| Justification | Nouveau module d'inventaire de stock pharmacie rétrocompatible |
