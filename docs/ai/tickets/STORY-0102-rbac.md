# STORY-0102 — Contrôle d'Accès Basé sur les Rôles (RBAC)

## 1. Objectif

Cette story a pour but de mettre en place le contrôle d'accès basé sur les rôles (RBAC) de bout en bout sur l'ensemble de la plateforme Joprelys Connect :
1. **Sécurisation Backend** : Restreindre l'accès aux endpoints de l'API selon le rôle extrait du token JWT de l'utilisateur connecté en utilisant Spring Security (contrôle de requêtes et annotations de sécurité de méthode `@PreAuthorize`).
2. **Protection Frontend** : Masquer ou désactiver les éléments d'interface non autorisés sur l'écran d'accueil, et bloquer l'accès aux routes réservées via des guards Angular (`RoleGuard`).

## 2. Rôles et Autorisations

Le système gère les rôles suivants (définis dans la charte de marque et le modèle utilisateur) :
* **`ADMIN_JOPRELYS`** : Accès global au système, audit, configuration.
* **`MEDECIN`** : Consultation et modification des dossiers patients, prescriptions.
* **`PHARMACIEN`** : Consultation des prescriptions, validation.

## 3. Critères d'acceptation

- [x] Un guard Angular (`role.guard.ts`) protège les routes de l'application selon les rôles attendus.
- [x] L'écran d'accueil affiche un tableau de bord minimaliste et dynamique où les menus d'actions varient selon le rôle de l'utilisateur connecté (Admin, Médecin, Pharmacien).
- [x] Le backend Spring Security active le contrôle d'accès sur les méthodes (`@EnableMethodSecurity`).
- [x] Création d'endpoints de test spécifiques :
  - `/api/clinic/admin` (réservé à `ADMIN_JOPRELYS`)
  - `/api/clinic/medecin` (réservé à `MEDECIN` ou `ADMIN_JOPRELYS`)
  - `/api/clinic/pharmacien` (réservé à `PHARMACIEN` ou `ADMIN_JOPRELYS`)
- [x] Les endpoints backend retournent un code HTTP `403 Forbidden` si un utilisateur authentifié avec un rôle non autorisé tente de les appeler.
- [x] Écriture de tests d'intégration backend (Spring Security) validant les permissions et les rejets 403.
- [x] Écriture de tests unitaires frontend validant le comportement du guard de rôles.

## 4. Pilotage projet

| Champ | Valeur |
|---|---|
| Epic parent | EPIC-0001 (Authentification & Rôles) |
| User story parent | STORY-0102 |
| Sprint cible | SPRINT-0002 |
| Priorité business | P0 |
| Complexité | M |
| Story points | 3 |
| Profil recommandé | Senior |
| Effort estimé senior | 0.4j |
| Effort estimé intermédiaire | 0.65j |
| Effort estimé junior | 1.1j |
| Responsable | Gemini |
| Reviewer obligatoire | Lead Developer |
| Risque fonctionnel | Moyen |
| Risque technique | Moyen |
| Dépendances | TICKET-0104, TICKET-0105 |
| Bloquants connus | Aucun |

## 5. Contexte analysé

- [x] `AGENTS.md` lu
- [x] `SKILL.md` lu
- [x] `PROJECT-MANAGER-SKILL.md` lu si nécessaire
- [x] `README-IA.md` lu
- [x] `WORKFLOW-IA.md` lu
- [x] `PROJECT-TRACKING.md` lu
- [x] `CHANGELOG.md` lu
- [x] `review-checklist.md` lu

## 6. Action plan

- [x] Initialiser le ticket (`STORY-0102-rbac.md`)
- [x] Créer la documentation fonctionnelle initiale `docs/features/rbac/FUNCTIONAL-SPEC.md`
- [x] Créer la documentation technique initiale `docs/features/rbac/TECHNICAL-DESIGN.md`
- [x] Implémenter la configuration et les endpoints d'accès contrôlé côté backend
- [x] Ajouter les tests d'intégration backend (Spring Security / `@PreAuthorize`)
- [x] Implémenter le Dashboard dynamique et le guard de rôle côté frontend
- [x] Ajouter les tests unitaires du guard Angular
- [x] Valider l'exécution locale des tests des deux projets (Maven et Vitest)
- [x] Mettre à jour `CHANGELOG.md` et `PROJECT-TRACKING.md`

## 7. Suivi d'exécution

| Date | Développeur | Temps passé | Avancement | Reste à faire | Blocage | Commentaire |
|---|---|---:|---:|---:|---|---|
| 2026-07-02 | Gemini | 0.40j | 100% | Aucun | Aucun | STORY-0102 entièrement implémentée, validée et documentée |

## 8. Statut final

Statut : DONE
