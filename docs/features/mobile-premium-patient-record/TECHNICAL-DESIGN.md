# TECHNICAL DESIGN — Dossier patient mobile premium

## Architecture

La fonctionnalité s’appuie sur quatre couches Flutter :

1. **Accès effectif** : `EffectiveAccessApi` charge `/api/rbac/me` et expose un ensemble normalisé de permissions.
2. **Shell professionnel** : `FoundationPage` construit les destinations autorisées et conserve leur état avec `IndexedStack`.
3. **Données patient** : `PatientRecordApi` charge l’identité, puis uniquement les ressources autorisées.
4. **Présentation** : `PatientRecordPage` et ses sections rendent une interface plein écran basée sur `ColorScheme`.

## Autorisation

Le rôle affiché dans la session n’est pas utilisé pour déduire les capacités. Les permissions effectives du backend sont la source de vérité.

| Capacité | Permission |
|---|---|
| Annuaire et identité | `PATIENT_READ` |
| Antécédents, allergies, consultations | `CLINICAL_READ` |
| Laboratoire | `LAB_ORDER_READ` |
| Hospitalisations | `HOSPITALIZATION_READ` |
| Traçabilité | `AUDIT_READ` |
| File active | `VISIT_READ` |

Les endpoints interdits ne sont pas appelés par l’application. Une réponse `403` sur l’identité déclenche le parcours de consentement/Break-Glass.

## Endpoints utilisés

- `GET /api/rbac/me`
- `GET /api/patients`
- `GET /api/patients/{id}`
- `GET /api/patients/{id}/allergies`
- `GET /api/patients/{id}/medical-history`
- `GET /api/patients/{id}/consultations`
- `GET /api/patients/{id}/vaccinations`
- `GET /api/lab-orders/patient/{id}`
- `GET /api/lab-orders/patient/{id}/results`
- `GET /api/hospitalizations/patient/{id}`
- `GET /api/audit/patients/{id}`
- `POST /api/patients/{id}/emergency-access`

## Navigation adaptative

- largeur inférieure à 760 px : `NavigationBar` ;
- largeur supérieure ou égale à 760 px : `NavigationRail` ;
- `IndexedStack` conserve la recherche, le défilement et l’état des destinations ;
- l’ouverture du dossier se fait sur une route modale plein écran, ce qui préserve le bouton retour Android.

## Gestion des erreurs

- `404` sur une collection optionnelle : collection vide ;
- `401` : géré par la politique de session existante ;
- `403` sur le dossier : accès protégé et Break-Glass ;
- réponse mal formée : `ApiFailureKind.malformedResponse` ;
- erreur réseau : état de reprise avec action d’actualisation.

## Thème et accessibilité

- aucune couleur de surface codée pour un seul thème ;
- `ColorScheme` pour les surfaces, textes et états ;
- design tokens pour espacements, rayons et bordures ;
- cibles tactiles Material ;
- navigation sémantique avec état sélectionné ;
- textes limités ou adaptatifs pour éviter les débordements.

## Sécurité

- aucune donnée clinique n’est mise en cache en dehors de l’état mémoire des widgets ;
- les permissions sont rechargées à l’entrée de l’espace authentifié ;
- le Break-Glass exige une justification non vide ;
- l’autorisation backend reste obligatoire, même si une section est visible côté client.
