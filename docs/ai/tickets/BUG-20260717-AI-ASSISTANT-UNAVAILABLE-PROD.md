# BUG-20260717-AI-ASSISTANT-UNAVAILABLE-PROD — Assistant IA indisponible en production — erreur 500 en cascade

**Mode** : Diagnostic + correctif

**Date** : 2026-07-17

**Statut** : DONE

**Priorité** : P0

**Epic** : AI_VOICE_CONSULTATION (EPIC-0024)

**Estimation** : 0,25 j senior

**Profil recommandé** : Senior full-stack Spring Boot / Angular / Production

**Reviewer** : Lead Backend + QA

**Stack** : Spring Boot / Angular / Production / systemd

**Impact version** : PATCH

## Diagnostic initial

- [x] L'assistant vocal IA en production affiche « Assistant IA indisponible » avec des erreurs HTTP 500.
- [x] Cause racine n°1 : la variable `JOPRELYS_AI_ENABLED` est positionnée à `false` dans le fichier `/opt/joprelys-connect/api/.env` du serveur de production, ce qui désactive entièrement le module IA.
- [x] Cause racine n°2 : le `GlobalExceptionHandler` ne gère pas `NoResourceFoundException` ; les routes inexistantes retournent un HTTP 500 au lieu de 404.
- [x] Cause racine n°3 : le `GlobalExceptionHandler` ne gère pas `HttpRequestMethodNotSupportedException` ; les méthodes HTTP non supportées retournent un HTTP 500 au lieu de 405.

## Actions

- [x] Corriger `JOPRELYS_AI_ENABLED=false` en `JOPRELYS_AI_ENABLED=true` dans `/opt/joprelys-connect/api/.env`.
- [x] Redémarrer le service `joprelys-connect-api.service` via systemd.
- [x] Ajouter le handler `NoResourceFoundException` dans `GlobalExceptionHandler.java` → retourne HTTP 404 avec message normalisé.
- [x] Ajouter le handler `HttpRequestMethodNotSupportedException` dans `GlobalExceptionHandler.java` → retourne HTTP 405 avec message normalisé.
- [x] Vérifier que l'assistant IA est de nouveau accessible en production.
- [x] Mettre à jour le suivi projet et le changelog.

## Critères d'acceptation

- [x] L'assistant vocal IA répond correctement en production (plus de message « Assistant IA indisponible »).
- [x] Les routes inexistantes retournent HTTP 404 au lieu de 500.
- [x] Les méthodes HTTP non supportées retournent HTTP 405 au lieu de 500.
- [x] Le service `joprelys-connect-api.service` est actif et stable après redémarrage.
- [x] Aucune régression sur les autres endpoints de l'API.

## Reste à faire

- Vérifier la validité des clés API OpenAI après activation.
