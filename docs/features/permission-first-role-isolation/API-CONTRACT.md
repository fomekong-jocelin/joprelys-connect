# API-CONTRACT — Autorisation permission-first

## Contrat transversal

Les payloads métier ne changent pas. La politique d'accès est renforcée :

- un endpoint professionnel sensible exige une ou plusieurs authorities métier ;
- un rôle système ou personnalisé n'accorde aucun accès par son seul nom ;
- l'absence de permission produit `403 Forbidden` ;
- l'absence ou l'invalidité de session produit `401 Unauthorized` ;
- les contrôles tenant, propriétaire, consentement et urgence restent appliqués après le contrôle fonctionnel.

Pour les disponibilités, `AVAILABILITY_MANAGE` reste limité au praticien courant.
La gestion des créneaux d'un autre praticien exige explicitement
`AVAILABILITY_MANAGE_ALL`, en plus du périmètre d'établissement.

## Accès effectif

`GET /api/rbac/me` retourne les rôles et permissions effectifs de la session courante. Angular lie cette réponse au jeton qui l'a demandée et rejette toute réponse appartenant à une session remplacée.

## Portail patient

Une session contenant `PATIENT` reçoit exclusivement :

- `ROLE_PATIENT` ;
- `PATIENT_PORTAL_ACCESS` ;
- `PATIENT_APPOINTMENT_MANAGE` ;
- `PATIENT_NOTIFICATION_MANAGE`.

Elle ne reçoit aucune authority professionnelle, même si la donnée source contient un claim de rôles mixte.

## Frontières de session

Les endpoints de déconnexion et de révocation expirent le cookie refresh HttpOnly et renvoient :

```http
Clear-Site-Data: "cache", "cookies", "storage"
```

Le login patient applique également cette neutralisation afin qu'un ancien cookie professionnel ne survive pas au changement de compte.

## Intégration laboratoire

`POST /api/public/lab-integration/**` exige `X-API-KEY`. Si `JOPRELYS_LAB_INTEGRATION_API_KEY` est absente ou vide, toute requête est refusée. Aucune clé par défaut n'est fournie.
