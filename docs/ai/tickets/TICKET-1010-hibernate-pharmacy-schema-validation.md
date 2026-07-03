# TICKET-1010 — Correction validation Hibernate du schema pharmacie

## Mode d'intervention

Diagnostic + Engineering.

## Probleme

Le backend echoue au demarrage pendant la creation de `entityManagerFactory` avec une `SchemaManagementException` Hibernate. Le symptome correspond a une base locale dont le schema pharmacie n'est pas aligne avec les entites JPA courantes, notamment si `V14__add_pharmacy_fields_and_tables.sql` a deja ete applique avant l'ajout de certaines colonnes.

## Criteres d'acceptation

- [x] Identifier l'ecart entite/migration le plus probable.
- [x] Ajouter une migration Flyway additive, forward-only et compatible avec les bases deja migrees.
- [x] Ne pas modifier silencieusement de contrat API.
- [x] Verifier la compilation backend et la migration SQL.
- [x] Mettre a jour la documentation et le suivi.

## Action plan

- [x] Lire les consignes IA et standards Spring/configuration.
- [x] Reproduire ou approcher l'erreur via les tests de contexte.
- [x] Inspecter `PrescriptionEntity` et les migrations `V7`/`V14`.
- [x] Ajouter `V15__repair_pharmacy_schema_validation.sql`.
- [x] Documenter le modele de donnees pharmacie.
- [x] Executer les verifications backend disponibles.
- [ ] Relancer le backend sur la base PostgreSQL locale de l'IDE.

## Analyse

Les tests H2 sur base fraiche passent, ce qui confirme que les entites et les migrations actuelles sont coherentes pour une base neuve. L'erreur de la capture survient sur une base PostgreSQL locale deja migree : Flyway ne rejoue pas une migration versionnee deja appliquee, donc une ancienne variante de `V14` peut laisser Hibernate face a une colonne ou table attendue mais absente.

## Correction

Ajout de `V15__repair_pharmacy_schema_validation.sql` avec des instructions `IF NOT EXISTS` pour synchroniser :

- les colonnes pharmacie de `prescriptions` ;
- l'index unique du numero d'ordonnance ;
- les tables `prescription_dispensations` et `dispensation_items` ;
- les index associes.

## Tests et verifications

- [x] `mvn test -DskipTests` : compilation main/test OK.
- [x] Verification SQL H2 en mode PostgreSQL : `V15__repair_pharmacy_schema_validation.sql` s'execute sur un schema `prescriptions` ancien.
- [ ] `mvn test` complet : non valide apres `V15`; Surefire/Maven a ete instable dans l'environnement local (`Unable to create test class`, puis resolution reseau du parent Spring Boot bloquee).
- [ ] `mvn spring-boot:run` : non verifie via Maven, resolution reseau bloquee dans le sandbox apres compilation.
- [ ] Wrapper `mvnw.cmd` : anomalie locale constatee (`Cannot start maven from wrapper`), a traiter separement si recurrent.

## Securite / Regression

- Pas d'affaiblissement AuthN/AuthZ.
- Pas de secret ajoute.
- Migration additive uniquement, sans suppression de donnees.
- Risque principal : doublon d'index unique si une base avait deja un index equivalent sous un autre nom ; impact faible et non bloquant fonctionnellement.

## Impact planning

- Estimation : 0.1j.
- Profil recommande : Intermediaire backend.
- Sprint : SPRINT-0004.

## Impact version / SemVer

- Version actuelle : 0.5.0.
- Bump recommande : PATCH.
- Breaking change : Non.

## Statut final

Statut : DONE cote correction de schema et compilation. Reste a relancer l'application contre la base PostgreSQL locale.
