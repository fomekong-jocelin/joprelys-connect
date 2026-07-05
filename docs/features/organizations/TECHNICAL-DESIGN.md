# TECHNICAL-DESIGN — Complétion de la gestion des établissements (Module 1)

## 1. Objectif technique

Implémenter l'enrichissement des données d'établissement et le cycle de vie sécurisé des clés d'API (avec hachage en base de données) et un filtre d'accès pour les requêtes machine-to-machine.

## 2. Stack concernée

- [x] Spring Boot
- [x] Angular
- [ ] Flutter
- [x] Base de données
- [ ] CI/CD
- [x] Documentation

## 3. Contraintes projet obligatoires

- Backend Spring Boot : Maven uniquement (`pom.xml`, `mvnw`).
- Backend Spring Boot : `application.yml` obligatoire, pas de `application.properties`.
- Angular : Tailwind CSS v4 obligatoire, Angular Material interdit.
- Angular : `proxy.conf.json` obligatoire et URLs API relatives.
- Angular / Flutter : thème centralisé, light/dark, i18n FR/EN, configuration app/branding.
- Documentation fonctionnelle et technique maintenue dès le démarrage.

## 4. Architecture cible

```text
HTTP Request (Header: X-API-KEY)
  → ApiKeyAuthenticationFilter (Filtre Spring Security)
  → OrganizationApiKeyRepository / Service (Hachage SHA-256 et validation)
  → SecurityContext (Authentication injectée avec le rôle API_CLIENT)
```

## 5. Fichiers ou modules impactés

| Module | Fichier | Type d’impact |
|---|---|---|
| Database | `V28__create_organization_extensions_and_api_keys.sql` | Ajout colonnes organizations et création table clés API. |
| Backend | `OrganizationEntity.java` | Ajout des champs type, country, responsibleName, apiEnabled. |
| Backend | `OrganizationApiKeyEntity.java` | Nouvelle entité JPA pour les clés API. |
| Backend | `OrganizationApiKeyRepository.java` | Nouveau Repository JPA. |
| Backend | `OrganizationController.java` | Mise à jour des endpoints CRUD et ajout de `/api-keys`. |
| Backend | `ApiKeyAuthenticationFilter.java` | Filtre Spring Security pour l'authentification par clé API. |
| Frontend | `organizations.models.ts` | Enrichissement du modèle TS. |
| Frontend | `organization-api.service.ts` | Ajout des appels pour les clés API. |
| Frontend | `organization-form.component.ts` | Ajout des inputs de type, pays, et responsable. |
| Frontend | `organization-table.component.ts` & HTML | Ajout des actions de génération/révocation et des colonnes type/pays. |

## 6. Contrats API

### 1. CRUD Etablissements
`POST /api/organizations` / `PUT /api/organizations/{id}`
- **Request Body** :
```json
{
  "name": "Clinique de l'Espoir",
  "email": "espoir@clinique.org",
  "phone": "+237600000000",
  "address": "Rue du Centre",
  "city": "Yaoundé",
  "country": "Cameroun",
  "type": "CLINIC",
  "responsibleName": "Dr. Jean Dupont",
  "apiEnabled": true
}
```

### 2. Clés API
`POST /api/organizations/{id}/api-keys`
- **Request Body** :
```json
{
  "name": "Clé Intégration Labo"
}
```
- **Response (250 Created - Affichée une seule fois)** :
```json
{
  "id": "uuid-key",
  "name": "Clé Intégration Labo",
  "rawKey": "jop_live_ab12cd34ef56...",
  "createdAt": "2026-07-05T14:30:00Z"
}
```

`GET /api/organizations/{id}/api-keys`
- **Response** :
```json
[
  {
    "id": "uuid-key",
    "name": "Clé Intégration Labo",
    "prefix": "jop_live_ab12...",
    "status": "ACTIVE",
    "createdAt": "2026-07-05T14:30:00Z",
    "revokedAt": null
  }
]
```

`DELETE /api/organizations/{orgId}/api-keys/{keyId}`
- **Response** : `204 No Content`

## 7. Modèle de données / migrations

### Migration SQL (`V28__create_organization_extensions_and_api_keys.sql`) :
```sql
ALTER TABLE organizations ADD COLUMN type VARCHAR(50) DEFAULT 'CLINIC';
ALTER TABLE organizations ADD COLUMN country VARCHAR(100) DEFAULT 'Cameroun';
ALTER TABLE organizations ADD COLUMN responsible_name VARCHAR(150) DEFAULT 'Responsable';
ALTER TABLE organizations ADD COLUMN api_enabled BOOLEAN DEFAULT TRUE;

CREATE TABLE organization_api_keys (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    hashed_key VARCHAR(64) NOT NULL UNIQUE,
    prefix VARCHAR(20) NOT NULL,
    name VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_org_api_keys_org FOREIGN KEY (organization_id) REFERENCES organizations(id)
);
```

## 8. Sécurité

- [x] Authentification requise par clé API pour les clients M2M.
- [x] Rôle `ADMIN_JOPRELYS` requis pour créer/modifier les établissements et gérer leurs clés.
- [x] Clé API stockée sous forme hachée SHA-256 en base de données.
- [x] Aucun secret en clair dans le code.

## 9. Tests prévus

| Niveau | Tests attendus | Commande |
|---|---|---|
| Unit | Tests de hachage de clé API et de détection de préfixes. | `./mvnw test` |
| Integration | Validation du filtre MockMvc Security avec requêtes X-API-KEY. | `./mvnw test` |
| UI | Tests de validation de formulaire et d'affichage des listes Angular. | `npm run test` |

## 10. Historique des mises à jour

| Date | Auteur | Changement |
|---|---|---|
| 2026-07-05 | Antigravity | Création initiale du document technique |
