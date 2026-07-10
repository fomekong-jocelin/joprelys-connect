# FUNCTIONAL-SPEC — Workspace Facturation & Caisse orienté tâche

## Problème

La page actuelle mélange émission, règlement patient, recouvrement assurance, créances, devis et avoirs. L'utilisateur ne sait pas quelle action est attendue et le statut `PAID` peut contredire les créances affichées.

## Utilisateurs

- Secrétaire comptable : facturer et encaisser la part patient.
- Caissier : gérer une session et produire les reçus.
- Recouvrement / DAF : suivre et solder les créances assurance.

## Parcours cible

1. Rechercher un patient et sélectionner une visite.
2. Si aucune facture existe, afficher le mode `Nouvelle facture`.
3. Si une facture existe, afficher son résumé et ses actions financières, sans formulaire de création.
4. Encaisser uniquement la part patient exigible depuis une session de caisse ouverte.
5. Traiter la part assurance dans le parcours bordereau.
6. Afficher le détail dans une vue secondaire, sans empiler toutes les sections dans la page principale.

## États affichés

| État | Signification | Action principale |
|---|---|---|
| `PATIENT_DUE` | Part patient non réglée | Encaisser patient |
| `PATIENT_PARTIALLY_PAID` | Part patient partiellement réglée | Encaisser le solde patient |
| `INSURANCE_DUE` | Patient réglé, assurance restante | Ouvrir suivi assurance |
| `SETTLED` | Patient et assurance réglés | Aucune action financière |
| `NOT_YET_DUE` | Facture à valider | Valider / consulter |

## Critères d'acceptation

- Une seule action principale est présentée par état.
- Une facture déjà créée ne peut pas être recréée depuis la visite.
- Un clic sur une facture ouvre et rend visible son détail.
- Les devis et avoirs ne sont pas affichés comme des actions de caisse courantes.
- Dans le détail d’une facture, l’historique des devis est visuellement séparé de l’action « Créer un devis » ; l’avoir est présenté comme une action financière exceptionnelle nécessitant une saisie explicite.
- La saisie d’une ligne de devis reste intégralement visible dans le panneau de détail, y compris lorsque ce panneau est plus étroit que la fenêtre ; l’action de suppression est lisible et atteignable.
- L’annulation d’une facture utilise une modale interne accessible ; aucun `confirm()` navigateur n’est autorisé.
- Un devis peut être lié à une visite du patient et transmet ce `visitId` à l’API.
- Les mutations devis/facture/avoir affichent un retour succès ou erreur visible.
- Le chargement d’une facture par deep-link expose un état visuel explicite.
- Un paiement sans session ouverte est bloqué dans l'interface et renvoie vers l'ouverture de caisse.
- Les textes sont disponibles en français et en anglais.
- Le parcours respecte les thèmes light/dark et les rayons sobres du design system.
