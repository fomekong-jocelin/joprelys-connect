# Consent Management — Technical Design

## Scope

Cette fonctionnalité prépare Joprelys Connect à la gestion des cookies et stockages optionnels selon un modèle `deny by default`, sans introduire de CMP tierce ni de bandeau tant qu’aucun traceur non essentiel n’est actif.

## Catégories

| Catégorie | État | Activation utilisateur | Usage actuel |
|---|---|---|---|
| `necessary` | toujours `true` | non | authentification, sécurité, refresh token |
| `preferences` | `false` par défaut | oui | langue, thème, préférences d’affichage |
| `analytics` | toujours `false` actuellement | non disponible | aucun outil intégré |
| `marketing` | toujours `false` actuellement | non disponible | aucun outil intégré |

## Stockage

Le choix est enregistré dans `localStorage` sous la clé :

`joprelys.privacy.consent`

Structure :

```json
{
  "version": "1.0",
  "necessary": true,
  "preferences": false,
  "analytics": false,
  "marketing": false,
  "updatedAt": "2026-07-20T00:00:00.000Z"
}
```

Une version inconnue ou obsolète est ignorée et rétablit les valeurs protectrices par défaut.

## Intégration UI

Route publique : `/legal/privacy-preferences`.

Le centre permet :

- de voir les quatre catégories ;
- d’activer ou refuser la persistance des préférences ;
- de constater qu’analytics et marketing ne sont pas utilisés ;
- de revenir à la politique cookies ;
- de changer temporairement langue et thème même sans persistance.

## Intégration thème et langue

`ThemeService` et `I18nService` consultent `ConsentManagementService.preferencesAllowed()` avant d’écrire dans `localStorage`.

Lorsque les préférences sont refusées :

- le thème continue de fonctionner pour la page courante mais n’est pas mémorisé ;
- la langue continue de fonctionner pour la session courante mais n’est pas mémorisée ;
- les clés `joprelys.theme`, `joprelys.locale` et `joprelys.sidebar.collapsed` sont supprimées lors de la révocation.

## Sécurité

Le cookie de refresh token reste géré séparément :

- `HttpOnly` ;
- `Secure` en production ;
- `SameSite=Lax` ;
- `Path=/api/auth` ;
- host-only.

Il appartient à la catégorie strictement nécessaire et n’est jamais piloté par le centre de préférences.

Le mécanisme existant `Clear-Site-Data` et la purge de frontière de session restent inchangés.

## Future intégration de traceurs

Avant d’intégrer un outil analytics ou marketing :

1. déclarer explicitement la catégorie et son fournisseur ;
2. mettre à jour la politique cookies ;
3. rendre la catégorie activable uniquement si son usage est légalement justifié ;
4. charger le script uniquement après `isAllowed(category) === true` ;
5. prévoir la suppression des cookies du fournisseur lors de la révocation ;
6. exclure les données patients, dossiers, consultations, résultats et prescriptions de tout outil publicitaire ou replay tiers.

## Hors scope

- CMP SaaS tierce ;
- Google Consent Mode ;
- IAB TCF ;
- Google Analytics, Meta Pixel ou outil marketing ;
- session replay ;
- consentement médical du patient, qui reste un sujet distinct du consentement cookies.
