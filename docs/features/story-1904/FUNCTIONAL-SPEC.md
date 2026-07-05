# Functional Specification - STORY-1904: Prescriptions et ordonnances conformes CDC

> Feature ID : `prescriptions-ordonnances-cdc`  
> User Story : `STORY-1904`  
> Epic : `EPIC-0014`  

## 1. Objectifs

Mettre en conformité le module de prescription et d'ordonnance avec les exigences du module 7 du Cahier des Charges.

## 2. Périmètre

### Fonctionnalités incluses :
- **Modèle de données complet** : médicaments avec forme, voie, fréquence, substitution autorisée ou non.
- **Cycle de vie de l'ordonnance** :
  - `DRAFT` : Brouillon modifiable par le médecin.
  - `ACTIVE` : Validée et transmissible à la pharmacie.
  - `CANCELLED` : Annulée par le médecin.
  - `EXPIRED` : Expirée automatiquement après sa date de fin.
  - `PARTIALLY_DISPENSED` / `FULLY_DISPENSED` : Délivrée en pharmacie.
- **Document vérifiable** :
  - Génération d'un document PDF d'ordonnance avec filigrane ou type spécifique, QR code de vérification publique et hash de sécurité.
- **Vérification publique** :
  - Permettre de vérifier l'authenticité d'une ordonnance via son numéro unique et un code PIN ou QR code, sans exposer d'informations médicales sensibles.

## 3. Cas d'utilisation

1. **Médecin** :
   - Rédige une ordonnance en mode brouillon (`DRAFT`).
   - Valide l'ordonnance (`ACTIVE`), ce qui fige les données, génère le document PDF, le QR code de vérification, et l'envoie à AllôPharma.
   - Annule une ordonnance active (`CANCELLED`).
2. **Pharmacien** :
   - Vérifie le statut de l'ordonnance.
   - Enregistre la délivrance (partielle ou totale).
3. **Public/Patient** :
   - Scanne le QR code pour valider l'authenticité de l'ordonnance.

## 4. Critères d'acceptation

- Une ordonnance au statut `DRAFT` n'est pas visible par la pharmacie.
- Une ordonnance au statut `ACTIVE` ne peut plus être modifiée.
- L'annulation d'une ordonnance la passe au statut `CANCELLED` et met à jour AllôPharma.
- Une tâche planifiée (scheduler) passe quotidiennement les ordonnances dont la date `expiresAt` est dépassée au statut `EXPIRED`.
