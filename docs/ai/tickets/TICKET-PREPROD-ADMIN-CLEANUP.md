# TICKET-PREPROD-ADMIN-CLEANUP — Nettoyage de l'Administrateur par défaut et des données de test pour la préproduction

> Fichier obligatoire pour chaque ticket ou intervention IA.
>
> **Réouverture Go-Live — 2026-07-22** : le comportement historique de préproduction ci-dessous reste tracé, mais il n'est plus acceptable pour une production greenfield. Pour le Go-Live, ce ticket est prolongé avec les critères P0 de la section 10 : seed désactivé par défaut, activation explicite, email/nom/password obligatoires, aucun credential de repli versionné et fail-fast si la configuration est incomplète.

## 1. Objectif historique

Préparer l'application pour l'environnement de préproduction :
1. supprimer les identifiants sensibles faibles présents dans la configuration ;
2. désactiver la création automatique de données cliniques de démonstration ;
3. amorcer un unique administrateur plateforme pour permettre la création manuelle des établissements et comptes métiers.

> Le credential de bootstrap historique a été volontairement retiré de cette documentation. Il ne doit jamais être réutilisé.

## 2. Critères historiques

- [x] Les valeurs d'initialisation ont été retirées de `application.yml` au profit de variables d'environnement.
- [x] `AdminUserSeeder` n'initialise plus l'établissement de démonstration, le médecin de test et le pharmacien de test.
- [x] Les tests unitaires historiques ont été alignés sur la création exclusive d'un administrateur.
- [x] Validation historique : `./mvnw test` avec 240 tests verts au 2026-07-06.

## 3. Pilotage historique

| Champ | Valeur |
|---|---|
| Epic parent | EPIC-0001 |
| User story parent | STORY-0104 |
| Sprint cible | SPRINT-0011 |
| Priorité business | P1 |
| Complexité | S |
| Story points | 1 |
| Profil recommandé | Backend Engineer |
| Responsable | Antigravity |
| Reviewer obligatoire | Lead Developer |

## 4. Contexte analysé

- [x] `AGENTS.md` lu
- [x] `SKILL.md` lu
- [x] Configuration `application.yml` analysée
- [x] `AdminUserSeeder.java` et ses tests analysés

## 5. Implémentation historique

- données cliniques de démonstration retirées du seeder ;
- bootstrap limité au compte administrateur ;
- ancien comportement de repli conservé à l'époque pour faciliter la préproduction.

Ce dernier point est précisément celui qui est remplacé par le durcissement Go-Live ci-dessous.

## 6. Suivi d'exécution

| Date | Développeur | Avancement | Reste à faire | Commentaire |
|---|---|---:|---|---|
| 2026-07-06 | Antigravity | 100% historique | Aucun | Préproduction validée avec la politique de l'époque. |
| 2026-07-22 | GPT-5.6 Thinking | En cours | Durcissement Go-Live + tests complets | Réouverture documentaire avant code ; aucune action serveur. |

## 7. Impact version historique

| Champ | Valeur |
|---|---|
| Changement livrable | Oui |
| Type de bump | PATCH |
| Migration DB | Non |
| Changement API | Non |
| Impact Angular | Non |
| Impact Flutter | Non |

## 8. Extension P0 — Go-Live production greenfield

### Constat sur `main@078c3dc5f913f615910fad9f061085bc7acdcfec`

- le seed admin est actif par défaut dans `application.yml` ;
- le composant seeder est activable lorsque la propriété est absente ;
- `SeedAdminProperties.isComplete()` ne valide pas encore tous les champs obligatoires ;
- `AdminUserSeeder` contient encore des valeurs de repli versionnées.

### Critères d'acceptation Go-Live

- [ ] seed admin désactivé par défaut ;
- [ ] activation explicite obligatoire ;
- [ ] email obligatoire ;
- [ ] nom obligatoire ;
- [ ] mot de passe obligatoire ;
- [ ] aucun fallback email/password dans le code ;
- [ ] configuration activée mais incomplète => fail-fast ;
- [ ] mot de passe jamais journalisé ;
- [ ] mot de passe hashé via le `PasswordEncoder` existant ;
- [ ] création idempotente ;
- [ ] aucun établissement ou utilisateur de démonstration créé ;
- [ ] tests ciblés et suite Maven globale verts.

### Commande de validation finale

```bash
SPRING_DATASOURCE_URL='jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE' \
JOPRELYS_JWT_SECRET='<secret de TEST uniquement>' \
./mvnw clean verify -B --no-transfer-progress -Dspring.profiles.active=test
```

Aucun secret de production ne doit être utilisé ou versionné.
