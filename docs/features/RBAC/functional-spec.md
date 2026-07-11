# Spécification fonctionnelle — Administration RBAC clinique

## Finalité

L’administrateur d’un établissement doit pouvoir décider quels postes un collaborateur occupe dans Joprelys Connect. Un collaborateur peut cumuler plusieurs rôles lorsque l’organisation réelle l’exige.

## Rôles administrables

| Catégorie | Rôle | Responsabilité principale |
|---|---|---|
| Gouvernance | `ADMIN_CLINIQUE` | Administration de l’établissement, des collaborateurs et des rôles |
| Gouvernance | `AUDITEUR` | Consultation des traces et contrôles sans modification métier |
| Finance | `DAF` | Supervision financière, assurance, écarts et exports |
| Finance | `SECRETAIRE_COMPTABLE` | Facturation, bordereaux et suivi des créances |
| Finance | `CAISSIER` | Encaissements patient et sessions de caisse |
| Clinique | `MEDECIN` | Consultations, décisions médicales et prescriptions |
| Clinique | `INFIRMIER` | Constantes, soins et hospitalisation autorisée |
| Clinique | `PHARMACIEN` | Prescriptions et stocks de médicaments |
| Clinique | `BIOLOGISTE` | Analyses et résultats de laboratoire |
| Opérations | `AGENT_ACCUEIL` | Accueil, patients, visites et facturation initiale |

## Parcours administrateur

1. Ouvrir **Équipe clinique**.
2. Inviter ou modifier un collaborateur.
3. Sélectionner un ou plusieurs rôles dans la matrice.
4. Consulter la description de chaque rôle et le marquage des rôles sensibles.
5. Enregistrer l’affectation.
6. Le collaborateur obtient les nouveaux droits dès sa requête suivante ; une désactivation bloque immédiatement ses jetons existants.

## Règles métier

- au moins un rôle doit être sélectionné ;
- les doublons et variations de casse sont normalisés ;
- une clinique ne peut attribuer ni `ADMIN_JOPRELYS` ni `PATIENT` ;
- un administrateur ne peut pas modifier ses propres rôles ou son propre statut ;
- le dernier administrateur clinique actif ne peut être ni désactivé ni rétrogradé ;
- un administrateur ne peut gérer que les utilisateurs de son établissement ;
- les menus et routes sont calculés à partir de l’union des rôles affectés.

## Limite volontaire

Cette version administre l’affectation de rôles système audités. Elle ne permet pas encore de composer un rôle personnalisé permission par permission. Ce choix évite qu’une configuration locale contourne les invariants médicaux, financiers ou de traçabilité.
