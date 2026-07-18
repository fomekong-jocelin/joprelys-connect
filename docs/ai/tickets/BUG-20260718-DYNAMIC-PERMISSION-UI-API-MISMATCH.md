# BUG-20260718 — Désalignement permissions dynamiques, menus, routes et API

| Champ | Valeur |
|---|---|
| Type | Diagnostic + Engineering sécurité |
| Statut | QA TECHNIQUE VERTE |
| Priorité | P0 — contrôle d'accès et cohérence UX |
| Epic lié | EPIC-0026 |
| Branche | `fix/dynamic-permission-menu-alignment` |
| Stack | Angular 22 / Spring Security / RBAC dynamique |
| Estimation | 5 SP / 1,5 j Senior |
| Profil recommandé | Senior full-stack sécurité |
| Reviewer | Tech Lead + QA sécurité + RSSI |
| Date | 2026-07-18 |

## Problème

Un médecin voit la Facturation parce que le rôle système `MEDECIN` reçoit
`BILLING_INVOICE_READ` et `BILLING_INVOICE_WRITE`. Il voit aussi le portail laboratoire
avec `LAB_ORDER_READ`, alors que la file globale exige `LAB_QUEUE_READ`. Des écrans
multi-domaines déclenchent en outre des appels API sans vérifier la permission exacte,
ce qui génère des réponses `403 Forbidden` après une navigation pourtant proposée.

## Critères d'acceptation

- [x] Le rôle système `MEDECIN` ne reçoit plus de permission de facturation ou de caisse par défaut.
- [x] Une permission ajoutée par un rôle personnalisé est prise en compte par la même politique que les rôles système.
- [x] Le portail laboratoire global exige exclusivement `LAB_QUEUE_READ` dans le menu, le dashboard et la route.
- [x] `LAB_ORDER_READ` conserve uniquement les parcours patient autorisés.
- [x] Le frontend ne fabrique ou n'étend aucune permission absente de `/api/rbac/me`.
- [x] Chaque entrée de menu utilise la même politique que sa route.
- [x] Chaque sous-onglet et appel initial d'un écran composite vérifie sa permission exacte.
- [x] Aucun appel HTTP connu comme interdit n'est lancé uniquement pour découvrir un `403`.
- [x] Les tests couvrent les rôles système et les rôles personnalisés à permission minimale.
- [x] Les suites Angular et Maven sont vertes.

## Actions

- [x] Reproduire et rapprocher les captures des politiques frontend/backend.
- [x] Identifier les permissions par défaut et les incohérences menu/route/API.
- [x] Créer le ticket et le rapport diagnostic avant développement.
- [x] Corriger le catalogue du rôle `MEDECIN`.
- [x] Supprimer toute permission implicite côté Angular.
- [x] Aligner les politiques des menus, routes, dashboard et sous-écrans.
- [x] Empêcher les appels caisse/laboratoire non autorisés.
- [x] Cloisonner les actions d'encaissement, relance, assurance, export et résolution d'écarts.
- [x] Ajouter les tests de non-régression dynamiques.
- [x] Exécuter les validations complètes.
- [x] Mettre à jour le suivi, le changelog et la revue sécurité.

## Impacts

- API : aucun payload modifié ; lecture des conventions admise pour `INSURANCE_BORDEREAU_READ`
  et consultation de sa session active admise pour `CASH_PAYMENT_COLLECT`.
- DB : aucune migration ; le catalogue des rôles système est resynchronisé au démarrage.
- UI : retrait des entrées et actions sans permission effective.
- Sécurité : correction OWASP A01 Broken Access Control et moindre privilège.
- SemVer : PATCH pour la correction ; aucun nouveau contrat public.

## Reste à faire

Recette humaine en environnement déployé avec au minimum médecin, biologiste, caissier,
DAF, secrétaire comptable, administrateur clinique et rôle personnalisé minimal.
