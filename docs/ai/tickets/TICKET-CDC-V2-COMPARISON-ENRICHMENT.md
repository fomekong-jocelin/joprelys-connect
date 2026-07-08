# TICKET-CDC-V2-COMPARISON-ENRICHMENT

## Informations

| Champ              | Valeur                                               |
|--------------------|------------------------------------------------------|
| ID                 | TICKET-CDC-V2-COMPARISON-ENRICHMENT                  |
| Type               | Gouvernance / Documentation                          |
| Priorité           | P0                                                   |
| Sprint             | SPRINT-0011                                          |
| Assigné            | Antigravity                                          |
| Reviewer           | Lead Developer                                       |
| Statut             | **DONE**                                             |
| Estimation         | 0.5j (Senior)                                        |
| Temps passé        | 0.4j                                                 |
| Dernière MAJ       | 2026-07-08                                           |
| Mode d'intervention| Engineering / Documentation                          |

## Contexte

L'utilisateur demande une comparaison entre `SPECIFICATION_LOGICIEL_GESTION_CLINIQUE_TC2CDK.md` et le cahier des charges V2 récemment généré, afin d'identifier d'éventuels manquements ou d'améliorer sa structure, puis d'enrichir le CDC V2 en conséquence.

## Actions à réaliser

- [x] Lire et analyser le document `SPECIFICATION_LOGICIEL_GESTION_CLINIQUE_TC2CDK.md`
- [x] Comparer les deux documents pour en extraire les écarts et les axes d'amélioration
- [x] Rédiger et structurer la V2 enrichie du cahier des charges (`Cahier_des_charges_Joprelys_Connect_V2.md`)
- [x] Mettre à jour `docs/ai/PROJECT-TRACKING.md`
- [x] Mettre à jour `docs/ai/CHANGELOG.md`


## Écarts identifiés (Manques dans CDC V2 à rajouter)

1. **Urgences & Réanimation** : la réanimation initiale en cas de choc hémodynamique septique ou traumatique avec fiche de réanimation (remplissage, bolus, monitorage hémodynamique).
2. **Gestion budgétaire & Contrôle budgétaire** : fiches d'engagement, crédits disponibles par ligne budgétaire.
3. **Achats & Fournisseurs** : demandes internes, consultation des 3 fournisseurs, PV de réception.
4. **Immobilisations** : codification, étiquetage, fiche d'immobilisation, historique maintenance/panne.
5. **Ressources Humaines & Paie** : recrutement, notation du personnel, conseil de discipline, bulletin de paie détaillé.
6. **Comptabilité générale OHADA** : principes (prudence, non-compensation, coût historique...), imputation comptable par compte (classe 66, 421/422, 57, 52, 58), journaux auxiliaires (recettes, dépenses, banque, caisse, OD).
7. **Restauration** : menus, régimes alimentaires, commandes repas patients hospitalisés.
8. **Statistiques médico-économiques** : indicateurs cliniques et financiers (taux d'occupation des lits, durée moyenne de séjour, etc.).

## Critères d'acceptation

- Le fichier `Cahier_des_charges_Joprelys_Connect_V2.md` est enrichi avec les sections manquantes de façon structurée.
- La structure générale est claire et cohérente avec la philosophie Joprelys Connect (interopérabilité et gestion clinique intégrée).
