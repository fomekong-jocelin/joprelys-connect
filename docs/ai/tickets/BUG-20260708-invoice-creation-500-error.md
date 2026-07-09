# BUG-20260708-invoice-creation-500-error — Erreur 500 sur la création de facture (PostgreSQL sequence syntax)

> Fichier obligatoire pour chaque ticket ou intervention IA.

## 1. Objectif

Résoudre l'erreur 500 (Internal Server Error) survenant lors du POST sur `/api/invoices` (création d'une facture patient). L'anomalie est due à une incompatibilité de syntaxe de séquence SQL native entre H2 et PostgreSQL dans `InvoiceRepository.java`.

## 2. Critères d'acceptation

- [x] L'envoi du formulaire de facturation de patient via l'interface Angular (POST `/api/invoices`) réussit sans erreur 500.
- [x] La méthode `getNextInvoiceNumberSequenceValue()` dans `InvoiceRepository` utilise une requête compatible PostgreSQL (`nextval('invoice_number_seq')`).
- [x] Les tests de facturation dans le backend Maven (`InvoiceControllerTest`) s'exécutent avec succès.

## 3. Pilotage projet

| Champ | Valeur |
|---|---|
| Epic parent | EPIC-0014 — Alignement modules 4 à 12 du CDC |
| User story parent | STORY-1913-medical-billing |
| Sprint cible | SPRINT-0011 |
| Priorité business | P0 |
| Complexité | XS |
| Story points | 1 |
| Profil recommandé | Senior |
| Effort estimé senior | 0.05j |
| Effort estimé intermédiaire | 0.1j |
| Effort estimé junior | 0.2j |
| Responsable | Antigravity |
| Reviewer obligatoire | Lead Developer |
| Risque fonctionnel | Faible |
| Risque technique | Faible |
| Dépendances | Aucune |
| Bloquants connus | Aucun |

## 4. Contexte analysé

- [x] `AGENTS.md` lu
- [x] `SKILL.md` lu
- [x] `README-IA.md` lu
- [x] `WORKFLOW-IA.md` lu
- [x] `PROJECT-TRACKING.md` lu
- [x] `CHANGELOG.md` lu
- [x] `review-checklist.md` lu
- [x] Code existant analysé
- [x] Tests existants analysés
- [x] Contrats API analysés
- [x] Impacts sécurité analysés
- [x] Impacts données analysés
- [x] Impacts backend analysés
- [x] Backend Maven uniquement vérifié
- [x] Backend `application.yml` / profils YAML vérifiés ; aucun nouveau `application.properties`
- [x] Capacité sprint analysée

## 5. Hypothèses

- L'environnement de production/dev local utilise PostgreSQL comme base de données.
- PostgreSQL n'accepte pas la syntaxe `SELECT NEXT VALUE FOR invoice_number_seq` (syntaxe H2 native), ce qui produit une erreur de syntaxe SQL et une erreur 500 sur le serveur.
- H2 en mode PostgreSQL (`MODE=PostgreSQL`) accepte la fonction `nextval('invoice_number_seq')`, ce qui garantit la compatibilité à la fois pour les tests unitaires/intégration et l'exécution réelle.

## 6. Risques et impacts

| Risque | Impact | Mitigation |
|---|---|---|
| Régression sur la génération des numéros de facture sous H2 | Échec des tests unitaires backend | Utiliser la fonction standard de séquence `nextval` déjà validée sur d'autres repositories. |

## 7. Action plan

- [x] Comprendre le comportement actuel
- [x] Identifier les fichiers impactés
- [x] Modifier l'implémentation dans [InvoiceRepository.java](file:///C:/MES-APPLICATIONS/joprelys-connect/backend/src/main/java/com/joprelys/backend/billing/infrastructure/persistence/InvoiceRepository.java)
- [x] Lancer les tests locaux `./mvnw test` pour s'assurer que le changement est compatible avec H2
- [x] Mettre à jour `CHANGELOG.md`
- [x] Mettre à jour `PROJECT-TRACKING.md`

## 8. Implémentation réalisée

- Modification de la signature SQL native dans [InvoiceRepository.java](file:///C:/MES-APPLICATIONS/joprelys-connect/backend/src/main/java/com/joprelys/backend/billing/infrastructure/persistence/InvoiceRepository.java) à la ligne 17 :
  ```java
  @Query(value = "SELECT nextval('invoice_number_seq')", nativeQuery = true)
  Long getNextInvoiceNumberSequenceValue();
  ```
  Cela permet d'appeler la fonction native compatible PostgreSQL `nextval` qui fonctionne également sous H2 en mode PostgreSQL de test, résolvant ainsi l'erreur 500 rencontrée sur PostgreSQL.

## 9. Suivi d'exécution

| Date | Développeur | Temps passé | Avancement | Reste à faire | Blocage | Commentaire |
|---|---|---:|---:|---:|---|---|
| 2026-07-08 | Antigravity | 0.05j | 100% | Aucun | Aucun | Diagnostic posé, correction effectuée et tests validés. |

## 10. Tests et vérifications

### Commandes exécutées ou à exécuter

```bash
# Backend
./mvnw test
```

### Résultats

- [x] Tests unitaires OK (256/256 tests au vert, incluant `InvoiceControllerTest`)
- [x] Tests intégration OK
- [x] Build OK

## 11. Documentation

- [x] Changelog mis à jour
- [x] Suivi projet mis à jour

## 12. Reste à faire

Aucun.

## 13. Statut final

Statut : DONE

## 14. Notes finales

La correction applique l'utilisation de `nextval('invoice_number_seq')` à la place de `NEXT VALUE FOR invoice_number_seq`. Ce changement résout le crash 500 sur PostgreSQL en production/preprod tout en conservant la parfaite compatibilité des tests unitaires exécutés sur H2 avec le profil `test`.

## 15. Impact version / SemVer

| Champ | Valeur |
|---|---|
| Changement livrable | Oui |
| Type de bump | PATCH |
| Justification | Résolution d'un bug de requête native PostgreSQL bloquant la création de factures. |
| Breaking change | Non |
| Migration DB | Non |
| Changement API | Non |
| Impact Angular | Non |
| Impact Flutter | Non |
| Changelog requis | Oui |
| Release note requise | Non |

## 16. Impact thème / i18n / branding

- [x] Impact Angular UI analysé (aucun changement requis sur le front-end)

## Documentation First

- [x] Non requis pour un correctif mineur de syntaxe de base de données.

## Design System / UI

- [x] Non applicable (correction de requête SQL backend pure).

## Vérification `.gitignore`

- [x] `.gitignore` présent à la racine
- [x] `.gitignore` adapté à la stack réelle du projet
- [x] `docs/standards/GITIGNORE-STANDARDS.md` respecté
