# SPRINT PLANNING — Préparer un sprint

## 1. Entrées nécessaires

- Objectif du sprint.
- Capacité réelle de l'équipe.
- Backlog priorisé.
- Tickets READY.
- Disponibilités.
- Dépendances.
- Risques connus.

## 2. Étapes

1. Définir l'objectif du sprint.
2. Calculer la capacité planifiable.
3. Sélectionner uniquement des tickets READY.
4. Vérifier l'équilibre junior/intermédiaire/senior.
5. Garder une marge pour bugs/support.
6. Identifier les reviewers.
7. Valider le DoR et le DoD.
8. Documenter dans `docs/pm/sprints/SPRINT-XXXX.md`.

## 3. Règles

- Ne pas planifier de ticket flou.
- Ne pas charger à 100 %.
- Ne pas donner plusieurs gros tickets à un même développeur.
- Ne pas démarrer une tâche bloquée par une décision métier.
- Ne pas planifier une story à 13 points : découper.

## 4. Sortie attendue

Le sprint plan doit contenir :

- objectif ;
- capacité ;
- tickets sélectionnés ;
- charge totale ;
- charge par développeur ;
- risques ;
- dépendances ;
- marge ;
- définition de succès.


## Contraintes techniques par défaut

- Backend Spring Boot : Maven uniquement. Les estimations et capacités doivent intégrer les commandes Maven (`./mvnw test`, `./mvnw clean verify`).
- Frontend Angular : Tailwind CSS v4 uniquement. Ne pas planifier de tâche basée sur Angular Material sauf ADR validée.

## Standards configuration obligatoires

- Backend Spring Boot : `application.yml` obligatoire. Les tâches backend doivent prévoir la vérification de `src/main/resources/application.yml` et des profils YAML. `application.properties` doit être traité comme une dette ou une non-conformité à corriger.
- Frontend Angular : `proxy.conf.json` obligatoire. Les tâches frontend doivent vérifier que `angular.json` référence le proxy via `proxyConfig` et que les services utilisent des chemins API relatifs.
- Toute demande qui impose `application.properties`, une URL backend hardcodée côté Angular ou l’absence de proxy doit être bloquée ou documentée par ADR avant implémentation.

## Capacité documentation

Lors du sprint planning, prévoir explicitement du temps pour :

- créer la documentation fonctionnelle ;
- créer la documentation technique ;
- documenter API / DB / configuration si impact ;
- mettre à jour changelog et décision SemVer ;
- relire la documentation en review.

Une story sans documentation initiale ne doit pas entrer en sprint, sauf urgence explicitement marquée et régularisée avant `DONE`.

## Cadrage EPIC-0020

EPIC-0020 est prête au refinement mais ne doit pas être engagée en développement complet avant :

- l'arbitrage DAF sur `PAID` versus `SETTLED` ;
- la validation de la vue détail facture (panneau latéral ou route dédiée) ;
- la disponibilité d'un jeu de données tiers-payant avec paiement patient partiel et règlement assurance ;
- la réservation d'une capacité de deux sprints à 60–70 % maximum.

### Ajustement SPRINT-0014 — Bordereaux d'assurance

- `BUG-20260710-BORDEREAUX-HEADER` est engagé à hauteur de `0,1 j` senior / `1 SP` dans STORY-2204.
- Le code, les tests Angular et le build sont terminés ; le ticket reste en `QA` jusqu'au contrôle visuel light/dark et responsive.
- Ce correctif ponctuel n'étend pas le périmètre métier de STORY-2204 et ne consomme pas la capacité prévue pour l'arbitrage DAF de STORY-2201.

### Ajustement SPRINT-0014 — Cohérence du dossier médical patient

- `BUG-20260710-PATIENT-MEDICAL-ICONS-I18N` consomme `0,3 j` senior / `2 SP` pour harmoniser les icônes et compléter l'i18n Urgences.
- Le ticket reste en `QA` jusqu'au contrôle visuel light/dark.
- Deux dettes sont exclues du correctif et doivent être planifiées séparément : découpage du composant médical de plus de 500 lignes et compatibilité H2 de la migration V55.

### Ajustement SPRINT-0014 — Incident P0 RBAC inter-session

- `BUG-20260718-PATIENT-PROFESSIONAL-RBAC-CONTEXT-LEAK` consomme `0,75 j` senior / `3 SP`.
- Le code P0, les 267 tests Angular, le build, l'i18n et les 453 tests Maven sont verts.
- L'implémentation technique d'EPIC-0026 (21 SP / 9,5 j senior estimés) est terminée de façon accélérée ; la capacité restante porte sur la recette RSSI/métiers et la préparation de release.
- Aucun ticket n'est déclaré DONE avant la signature de la matrice et de la recette croisée des rôles.

### Ajustement SPRINT-0014 — Alignement permissions dynamiques finance/laboratoire

- `BUG-20260718-DYNAMIC-PERMISSION-UI-API-MISMATCH` consomme `1,5 j` senior / `5 SP`.
- Le rôle système médecin ne porte plus les permissions de facturation ; menus, routes, actions et appels API finance/laboratoire utilisent désormais les permissions effectives exactes, y compris pour les rôles personnalisés.
- La QA technique est verte avec 267 tests Angular et 453 tests Maven ; la recette humaine multi-rôles, le redémarrage backend et le déploiement du build Angular restent à exécuter.

## Cadrage EPIC-0027 — Organisation hospitalière et capacité

EPIC-0027 est proposé à la suite de l'audit AUDIT-20260721. Il représente 143–145 SP après réestimation de HOS-BED-001 et 103–143 jours-personnes, documentation et QA incluses.

- L'epic complet n'est pas engagé dans SPRINT-0014. Quatre incréments P0 bornés, HOS-BED-001-A/B/C et HOS-BED-002-A, soit 11 SP / 7 jours senior, ont été réalisés jusqu'à la QA H2/technique.
- Le solde de la phase 0 de 12–16 jours-personnes traite les invariants critiques des lits, la disponibilité, la sortie et les permissions ; il doit faire l'objet d'un arbitrage séparé.
- L'ADR-0002, les workflows médecin/cadre/admissions et la stratégie de migration PostgreSQL sont des préconditions.
- À 16–20 jours-personnes planifiables par sprint, l'ordre de grandeur global est 6–9 sprints ; aucune date n'est engagée sans capacité nominative.

### Incrément autorisé — HOS-BED-002-A

- Valeur : empêcher l'affichage de lits en nettoyage ou maintenance comme disponibles.
- Charge : 2 SP / 1 jour senior ajouté à SPRINT-0014.
- État : QA technique verte — 480 tests Maven, 315 tests Angular et build de production réussis ; 1 test PostgreSQL ignoré faute de Docker.
- Reste : revue lead, validation cadre infirmier/bed manager et déploiement backend avant frontend.
- Limite : cet incrément ne vaut ni acceptation d'ADR-0002 ni engagement du reste d'EPIC-0027.

### Incrément autorisé — HOS-BED-001-A

- Valeur : empêcher structurellement deux affectations actives sur un même lit, même si le statut est désynchronisé ou qu'une écriture contourne le claim nominal.
- Charge : 3 SP / 2 jours senior ajoutés à SPRINT-0014 ; total des incréments lits engagés : 5 SP / 3 jours senior.
- État : QA H2 verte — 486 tests Maven réussis, migration V76 appliquée ; test PostgreSQL 16 préparé mais ignoré faute de Docker.
- Reste : préflight des doublons actifs, Testcontainers PostgreSQL 16, revue DBA et validation bed manager.
- Limite : l'incrément ne couvre ni chevauchements clôturés, ni sortie physique et n'engage pas le reste de HOS-BED-001 ; la FK séjour est traitée séparément par HOS-BED-001-B.

### Incrément autorisé — HOS-BED-001-B

- Valeur : garantir qu'une affectation référence un séjour existant, qu'un séjour ne possède qu'une affectation active et que la fin ne précède jamais le début.
- Charge : 3 SP / 2 jours senior ajoutés à SPRINT-0014 ; total des incréments lits engagés : 8 SP / 5 jours senior.
- État : QA H2 verte — 490 tests Maven réussis, migration V77 appliquée ; test PostgreSQL 16 préparé mais ignoré faute de Docker.
- Reste : exécuter `PRE-MIGRATION-CHECKS.sql`, Testcontainers PostgreSQL 16, puis obtenir les validations DBA, bed manager et DPO sur la conservation restrictive.
- Limite : l'incrément ne couvre ni chevauchements entre périodes clôturées, ni sortie physique ; la cohérence tenant est traitée séparément par HOS-BED-001-C.

### Incrément autorisé — HOS-BED-001-C

- Valeur : empêcher qu'une affectation relie un séjour, un lit et un établissement appartenant à des tenants différents.
- Charge : 3 SP / 2 jours senior ajoutés à SPRINT-0014 ; total des incréments lits engagés : 11 SP / 7 jours senior.
- État : QA H2 verte — 493 tests Maven réussis, migration V78 appliquée ; test PostgreSQL 16 préparé mais ignoré faute de Docker.
- Reste : exécuter `PRE-MIGRATION-CHECKS.sql`, Testcontainers PostgreSQL 16, puis obtenir les validations DBA, RSSI/DPO et bed manager.
- Limite : l'incrément ne couvre ni chevauchements entre périodes clôturées, ni cohérence tenant du référentiel spatial complet, ni sortie physique.
