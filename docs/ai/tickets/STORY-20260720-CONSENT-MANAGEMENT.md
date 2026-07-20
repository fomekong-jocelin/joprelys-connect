# STORY-20260720 — Gestion des préférences cookies et stockages optionnels

## Statut

IN_PROGRESS

## Références

- Issue GitHub : #82
- Branche : `feat/82-consent-management`
- Type : privacy / frontend / sécurité
- Priorité : P1
- Estimation : 1,5 j senior frontend / privacy
- Reviewer : Lead Frontend + sécurité + référent protection des données

## Objectif

Préparer Joprelys Connect à une gestion moderne et extensible du consentement sans afficher de bannière intrusive tant qu’aucun traceur non essentiel n’est actif.

## Décisions

- `necessary` : toujours actif et non désactivable ;
- `preferences` : optionnel, désactivé par défaut, révocable ;
- `analytics` : désactivé et non disponible tant qu’aucun outil n’est intégré ;
- `marketing` : désactivé et non disponible ;
- aucune publicité, aucun profilage commercial et aucun replay tiers dans les espaces médicaux authentifiés ;
- le refresh token `HttpOnly` reste hors du mécanisme de préférence et demeure strictement nécessaire ;
- le choix est versionné avec `CONSENT_POLICY_VERSION` afin de pouvoir redemander une décision lors d’une évolution substantielle.

## Livrables

- `ConsentManagementService` ;
- centre public `/legal/privacy-preferences` ;
- textes FR/EN ;
- politique cookies reliée au centre de préférences ;
- thème et langue persistés uniquement lorsque les préférences sont autorisées ;
- suppression des clés connues lors de la révocation ;
- liens juridiques partagés corrigés vers les routes publiques ;
- tests unitaires service, composant et routes.

## Critères d’acceptation

- [x] Nécessaires toujours autorisés.
- [x] Préférences désactivées par défaut.
- [x] Analytics et marketing forcés à `false`.
- [x] Décision stockée et versionnée.
- [x] Révocation supprimant `joprelys.theme`, `joprelys.locale` et `joprelys.sidebar.collapsed`.
- [x] Thème utilisable en mémoire sans persistance lorsque les préférences sont refusées.
- [x] Langue utilisable en mémoire sans persistance lorsque les préférences sont refusées.
- [x] Centre de préférences public, responsive et accessible.
- [x] Politique cookies reliée au centre de préférences.
- [x] Aucun bandeau de consentement affiché dans cette version.
- [ ] CI Angular et Maven verte.
- [ ] Recette visuelle FR/EN light/dark.

## Points d’attention

Le logout conserve la stratégie de frontière de session complète existante. Les mécanismes serveur de sécurité ne doivent pas être affaiblis pour préserver un choix local de préférence.

Toute future intégration analytics/marketing devra être ajoutée derrière `ConsentManagementService` et ne devra pas charger son script avant autorisation lorsque le consentement est requis.
