# Spécification technique - Balance Âgée et Relances de Créances (STORY-2114)

## 1. Modèle de données (Flyway)

Création de la table `receivable_reminders` pour enregistrer l'historique des actions de relance.

### Script de migration `V53__create_receivable_reminders_table.sql` :
```sql
CREATE TABLE receivable_reminders (
    id UUID PRIMARY KEY,
    receivable_id UUID NOT NULL REFERENCES receivables(id) ON DELETE CASCADE,
    action_type VARCHAR(50) NOT NULL, -- PHONE_CALL, EMAIL, LETTER, VISIT
    status VARCHAR(50) NOT NULL, -- PENDING, PROMISED_PAYMENT, DISPUTE, UNREACHABLE
    notes TEXT,
    actor_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,
    organization_id UUID NOT NULL,
    version BIGINT NOT NULL
);

CREATE INDEX idx_receivable_reminders_receivable ON receivable_reminders(receivable_id);
```

## 2. API Rest

### `POST /api/receivables/{receivableId}/reminders`
Crée une nouvelle action de relance pour la créance spécifiée.
- **Payload** :
```json
{
  "actionType": "PHONE_CALL",
  "status": "PROMISED_PAYMENT",
  "notes": "Promesse de paiement par chèque le 15/07"
}
```
- **Réponse 201** :
```json
{
  "id": "uuid",
  "receivableId": "uuid",
  "actionType": "PHONE_CALL",
  "status": "PROMISED_PAYMENT",
  "notes": "Promesse de paiement par chèque le 15/07",
  "createdAt": "2026-07-09T20:00:00Z",
  "actorName": "Pierre Recouvrement"
}
```

### `GET /api/receivables/{receivableId}/reminders`
Retourne l'historique chronologique des relances pour une créance.
- **Réponse 200** : Liste de rappels de relances.

### `GET /api/receivables/aging-balance`
Retourne la liste des créances non soldées avec leur tranche d'ancienneté calculée par le serveur.

## 3. Frontend Angular

- **`BillingReceivablesComponent`** :
  - Ajout d'une colonne "Tranche d'ancienneté" (0-30j, 31-60j, etc.) avec styles de badges colorés.
  - Formulaire de filtre par tranche d'ancienneté.
  - Modale `BillingReminderModalComponent` de saisie d'action (Type d'action, Statut, Notes) avec affichage de l'historique des relances précédentes en timeline.

## 4. Tests

- **Backend** :
  - Tests unitaires de `ReceivableReminderService`.
  - Tests MockMvc d'autorisation et de création de relance (seul le rôle `DAF`, `SECRETAIRE_COMPTABLE` ou `ADMIN_CLINIQUE` peut créer).
- **Frontend** :
  - Tests unitaires Vitest de la modale de relance.
