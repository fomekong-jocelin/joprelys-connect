# RELEASE NOTE — v0.5.0

## 1. Résumé

La version `0.5.0` de Joprelys Connect apporte deux évolutions majeures et rétrocompatibles :
1. **La récupération de mot de passe simplifiée** : Sécurisation de l'accès utilisateur avec flux d'OTP (One-Time Password) par e-mail, protection contre l'énumération de comptes, et interface utilisateur réactive étape par étape.
2. **La refonte visuelle et fonctionnelle de la gestion des cliniques pilotes** : Amélioration substantielle du tableau d'administration Joprelys avec l'affichage direct de l'administrateur clinique affecté, la mise en place d'un tiroir coulissant d'informations détaillées, la modification en place des métadonnées, et un agencement esthétique conforme au guide stylistique `DESIGN.md`.

## 2. Version

| Champ | Valeur |
|---|---|
| Version précédente | 0.4.0 |
| Version publiée | 0.5.0 |
| Type de bump | MINOR |
| Date | 2026-07-03 |
| Responsable release | Antigravity |

## 3. Justification SemVer

Le bump est de type **MINOR** :
- Ajout de nouvelles fonctionnalités rétrocompatibles (endpoints publics d'OTP, endpoint de mise à jour des cliniques).
- Intégration de nouveaux écrans et composants graphiques (panneau latéral, formulaire de réinitialisation).
- Pas de breaking changes ou de suppressions d'API.
- Pas de migration de base de données destructive.

## 4. Tickets inclus

| Ticket | Titre | Type | Statut |
|---|---|---|---|
| [STORY-0103](file:///C:/MES-APPLICATIONS/joprelys-connect/docs/ai/tickets/STORY-0103-recuperation-mot-de-passe.md) | Récupération de Mot de Passe Simplifiée | User Story | DONE |
| [TASK-0901](file:///C:/MES-APPLICATIONS/joprelys-connect/docs/ai/tickets/TASK-0901-amelioration-ihm-organizations.md) | Amélioration IHM Gestion des Cliniques Pilotes | Amélioration UI | DONE |

## 5. Changements

### Added

- **Gestion des OTP** : Système de code de sécurité temporaire à 6 chiffres, valable 5 minutes, stocké en mémoire avec limite stricte à 3 essais erronés.
- **REST APIs Publiques** : `/api/public/auth/password-recovery/request` et `/api/public/auth/password-recovery/reset` pour la gestion autonome de la réinitialisation de mot de passe.
- **REST APIs Administrateur** : `PUT /api/organizations/{id}` pour la sauvegarde en base des modifications d'établissement.
- **Composant ForgotPassword** : Écran standalone avec workflow i18n FR/EN complet et micro-animations d'étapes.
- **Tiroir de détails (Drawer)** : Volet latéral à glissement CSS fluide contenant l'identité complète de la clinique, ses contacts et son admin.
- **Édition en place** : Formulaire d'édition à la volée intégré directement dans le tiroir d'établissement.

### Changed

- **Tableau d'établissements** :
  - Affichage direct du nom et de l'e-mail de l'administrateur clinique.
  - Alignement vertical centré des cellules pour une meilleure harmonie visuelle.
  - Déplacement du bouton d'activation/désactivation de la table principale vers le tiroir de détails pour une interface épurée.

### Fixed

- **Sauts de ligne inesthétiques** : Restructuration de la table avec `whitespace-nowrap` sur les colonnes clés (Ville, Actions, Statut) et tronquage d'adresses (`truncate` à `max-w-[220px]`) pour figer la mise en page.

## 6. Breaking changes

- Aucun.

## 7. Migrations

Aucune migration DB n'est nécessaire pour cette release.

## 8. Tests et QA

| Vérification | Résultat | Preuve |
|---|---|---|
| Backend tests | ✅ Succès | 104 tests unitaires et d'intégration au vert (`./mvnw test`) |
| Angular tests/build | ✅ Succès | 37 tests unitaires passés et build de prod d'Angular réussi |
| Flutter analyze/test | Non impacté | - |
| Security scan | ✅ Validé | Protection contre l'énumération de comptes (retour de statut 200 systématique) et protection RBAC Joprelys sur les cliniques. |
| QA review | ✅ Validé | Interface testée en thème light/dark et traductions FR/EN complètes. |

## 9. Déploiement

Le déploiement se fait via le pipeline standard ou manuellement :
```bash
# Compilation du backend & packaging jar
./mvnw clean package -DskipTests

# Build de production frontend
cd web && npm run build
```

## 10. Tag Git

```bash
git tag -a v0.5.0 -m "Release v0.5.0"
git push origin v0.5.0
```

## 11. Rollback

En cas d'anomalie critique en production :
1. Revenir sur le tag précédent `v0.4.0`.
2. Déployer à nouveau les artéfacts compilés correspondants.
3. Aucun rollback DB n'est requis.

## 12. Risques restants

- Aucun.

---

## Vérification Maven / Tailwind / Angular Material

- [x] Backend Spring Boot vérifié avec Maven uniquement.
- [x] Angular vérifié avec Tailwind CSS v4.
- [x] Aucun Angular Material introduit.
- [x] Spring Boot utilise `application.yml` et profils YAML ; aucun `application.properties`.
- [x] Angular possède `proxy.conf.json`, `proxyConfig` dans `angular.json`, et aucune URL backend hardcodée.
