# DEMO-20260725 — Préparation de la démonstration client

## Métadonnées

- **Issue GitHub** : #125
- **Date cible** : samedi 25 juillet 2026
- **Baseline de départ** : `main@81b7d20c4cf44800e436c86b964a38bc62929305`
- **Priorité** : P0 jalon client
- **Type** : qualification fonctionnelle, QA, documentation et préparation opérationnelle
- **Responsable** : Jocelin / équipe Joprelys
- **Reviewer** : Tech Lead + QA + référent fonctionnel
- **Statut** : IN_PROGRESS — cadrage et qualification

## Objectif

Préparer une démonstration fiable et reproductible du parcours :

`Accueil → constantes → consultation → urgence / URG-TEMP → rapprochement DPU → hospitalisation`

Ce ticket ne recrée aucune fonctionnalité déjà livrée. Il qualifie le code réellement présent dans `main`, fixe le scénario démontré et isole les éventuels correctifs bloquants.

## Preuves déjà disponibles

| Domaine | Preuve code/PR | État automatisé | État humain/environnement |
|---|---|---|---|
| URG-TEMP vers hospitalisation, documents et finance différée | STORY-2305 / #46, PR #96 fusionnée | CI verte lors de la fusion | recette de démonstration à rejouer |
| Workspace URG-TEMP et parcours E2E | STORY-2306 / #47, PR #97 fusionnée | CI #908 verte sur le head final | validation métier/visuelle à rejouer |
| Services hospitaliers typés et restrictions chambres/lits | #73 / PR #75 | livré et clôturé | données de démonstration à préparer |
| Intégrité lits et affectations | PR #98 à #104 | validations backend/PostgreSQL documentées | vérifier la baseline de l'environnement de démo |
| RBAC hospitalier granulaire | PR #100, #102, #107 | suites backend/frontend vertes | profils de démonstration à valider |
| Suppression du droit générique | #121 / PR #122, Flyway V86 | CI #1027 verte | l'environnement de démo doit être aligné sur le SHA retenu |

## Décision de gel

Jusqu'à la démonstration :

1. aucun chantier structurel HOS-STAFF-001 ou HOS-DIS-001 n'est engagé sur la baseline de démonstration ;
2. aucune refonte UI non bloquante ;
3. aucun changement de schéma supplémentaire après V86 sans défaut P0 démontré ;
4. un correctif admissible doit être minimal, documenté, testé et isolé ;
5. vendredi 24 juillet, après validation du scénario, le périmètre fonctionnel démontré est gelé.

## Matrice de qualification

| Étape | Fonction attendue | Rôle pressenti | Données préalables | État code | État démo |
|---|---|---|---|---|---|
| 1 | Connexion professionnelle et navigation | ADMIN_CLINIQUE ou profil métier dédié | compte actif, tenant valide | LIVRÉ | À VALIDER |
| 2 | Rechercher/créer un patient identifié | accueil | établissement actif | LIVRÉ | À VALIDER |
| 3 | Enregistrer les constantes | infirmier/accueil selon matrice | visite ou urgence active | LIVRÉ | À VALIDER |
| 4 | Ouvrir une consultation | médecin | visite active | LIVRÉ | À VALIDER |
| 5 | Créer une urgence identifiée | urgence | patient + établissement | LIVRÉ | À VALIDER |
| 6 | Créer un patient URG-TEMP | urgence | aucun identifiant obligatoire | LIVRÉ | À VALIDER |
| 7 | Triage, réanimation, tiers et médico-légal | infirmier/médecin urgence | urgence active | LIVRÉ | À VALIDER |
| 8 | Générer les documents d'urgence | urgence | données minimales du dossier | LIVRÉ | À VALIDER |
| 9 | Rapprocher vers un DPU canonique | profil habilité | candidat ou nouveau DPU | LIVRÉ | À VALIDER |
| 10 | Afficher la continuité documentaire/financière | accueil/caisse selon permissions | documents/facture différée | LIVRÉ | À VALIDER |
| 11 | Hospitaliser depuis l'urgence | responsable hospitalisation | service autorisant chambres + lit libre | LIVRÉ | À VALIDER |
| 12 | Consulter le séjour et les activités | médecin/infirmier/responsable | séjour actif | LIVRÉ | À VALIDER |

## Scénario nominal recommandé

1. se connecter avec un profil de démonstration validé ;
2. ouvrir le workspace urgence ;
3. créer un dossier « patient non identifié » ;
4. montrer immédiatement le code URG-TEMP et le bandeau d'identité provisoire ;
5. enregistrer le triage et une donnée de soin ;
6. compléter un tiers/accompagnant et générer un document vérifiable ;
7. effectuer un rapprochement vers un DPU de démonstration préparé ;
8. ouvrir le DPU canonique et montrer la provenance conservée ;
9. poursuivre vers l'hospitalisation en conservant le lien d'urgence ;
10. sélectionner un service compatible, une chambre et un lit libre ;
11. afficher le séjour actif et les droits différenciés médecin/infirmier/responsable.

## Scénario de secours

Si le rapprochement ou l'hospitalisation échoue pendant la démonstration :

- conserver le dossier URG-TEMP créé ;
- montrer la file des dossiers à régulariser et la traçabilité disponible ;
- utiliser un second dossier préparé à l'avance pour reprendre au point suivant ;
- ne jamais modifier directement la base devant le client ;
- ne pas présenter une étape non validée comme terminée.

## Jeu de données requis

- un établissement de démonstration ;
- un service `EMERGENCY` ;
- un service `HOSPITALIZATION` ;
- une chambre disponible ;
- au moins deux lits, dont un libre ;
- un patient canonique existant pour le rapprochement ;
- un profil urgence ;
- un médecin ;
- un infirmier ;
- un responsable hospitalisation ;
- un profil accueil ou administrateur clinique pour l'amorce du parcours.

Aucun mot de passe ni secret ne doit être versionné.

## Critères GO / NO-GO

### GO

- le même SHA est déployé et documenté ;
- Flyway est au niveau attendu pour ce SHA ;
- backend, frontend et build sont verts ;
- le parcours nominal est exécuté deux fois ;
- les comptes et données sont prêts ;
- le scénario de secours est testé ;
- aucune étape présentée n'est marquée « À VALIDER ».

### NO-GO

- divergence entre GitHub, JAR, frontend et base ;
- migration Flyway incomplète ;
- rôle de démonstration sur-autorisant ou bloqué ;
- lit/service non préparé ;
- rapprochement ou admission non reproductible ;
- correctif non testé fusionné à la dernière minute.

## Plan de travail

- [x] créer l'issue #125 ;
- [x] identifier les PR fonctionnelles #96/#97 et le socle RBAC/lits ;
- [x] documenter la matrice initiale et le scénario nominal ;
- [ ] vérifier les routes, composants et tests encore présents dans `main` ;
- [ ] relever la CI du SHA retenu ;
- [ ] qualifier l'environnement qui servira à la démonstration ;
- [ ] préparer les comptes et données sans secret versionné ;
- [ ] exécuter deux répétitions chronométrées ;
- [ ] classer chaque étape GO/NO-GO ;
- [ ] geler le périmètre vendredi 24 juillet ;
- [ ] enregistrer le bilan de la démonstration.

## Reste hors périmètre avant samedi

- HOS-STAFF-001 complet ;
- HOS-DIS-001 complet ;
- reset greenfield PROD ;
- amélioration esthétique non bloquante ;
- extension de l'EPIC-0027 sans lien direct avec le scénario présenté.
