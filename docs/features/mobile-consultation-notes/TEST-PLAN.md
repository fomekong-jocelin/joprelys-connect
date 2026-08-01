# Plan de test — Note clinique SOAP unifiée

## Périmètre

Vérifier qu'une même consultation conserve les mêmes sections, champs,
validations et erreurs sur Spring Boot, Angular et Flutter.

## Cas automatisés

| ID | Niveau | Cas | Statut au 2026-08-01 |
|---|---|---|---|
| SOAP-BE-001 | Backend intégration | POST puis GET conserve l'unique `diagnosis` avec `conclusion`, `advice`, `followUp` et n'expose aucun champ diagnostique legacy | Test adapté ; Maven bloqué par le parent Spring Boot absent du cache et le réseau sandbox refusé |
| SOAP-BE-003 | Migration DB | Les anciennes valeurs sont archivées, la priorité `final` → `diagnosis` → `suspected` est appliquée et les deux colonnes actives sont supprimées | V109 exécutée avec succès sur H2 2.4.240 en mémoire ; test PostgreSQL Testcontainers ajouté mais Maven bloqué |
| SOAP-BE-002 | Backend sécurité/erreurs | 204 sans consultation, 400 validation/visite close, 403 permission/tenant, 404 visite inconnue | Couverture existante inspectée ; réexécution bloquée comme SOAP-BE-001 |
| SOAP-WEB-001 | Angular composant | Le formulaire n'expose qu'un champ Diagnostic | Vert — suite ciblée Angular, 15 tests cumulés |
| SOAP-WEB-002 | Angular API | POST transmet uniquement `diagnosis`, 204 sans valeur et 404 propagé | Vert — suite ciblée Angular, 15 tests cumulés |
| SOAP-WEB-003 | Angular architecture | Chargement, 204 et verrouillage des lignes de prescription via facade dédiée | Vert — suite ciblée Angular, 15 tests cumulés |
| SOAP-MOB-001 | Flutter modèle/API | Six champs JSON, route `/consultation`, aucun diagnostic legacy, 204 absent, 404 propagé | Tests adaptés et analyse Dart verte ; `flutter test` expire sur la toolchain locale |
| SOAP-MOB-002 | Flutter dictée locale | Aucun diagnostic ni plan clinique inventé depuis une simple mention de fièvre | Test ajouté ; même blocage runtime que SOAP-MOB-001 |
| SOAP-MOB-003 | Flutter widget | Six champs, un seul Diagnostic, validations et quatre titres SOAP FR/EN | Test adapté et analyse Dart verte ; runtime Flutter bloqué |

## Recette manuelle requise

- créer une consultation sur Angular puis l'ouvrir sur Flutter ;
- modifier les six champs sur Flutter puis les relire sur Angular ;
- vérifier FR/EN et light/dark sur desktop et Android ;
- vérifier champs obligatoires, limites 5000/3000/1000 et erreurs 400/403/404 ;
- vérifier qu'un `204` affiche une consultation vide et qu'un `404` n'est pas
  masqué comme une absence de note ;
- vérifier que prescriptions, examens et constantes restent structurés dans
  leurs parcours dédiés.

## Gate de sortie

- `./mvnw test` vert avec dépendances disponibles ;
- tests Angular ciblés et build frontend verts ;
- `flutter analyze`, `flutter test` et APK debug verts sur le même HEAD ;
- recette cross-stack signée par QA mobile et référent clinique.
