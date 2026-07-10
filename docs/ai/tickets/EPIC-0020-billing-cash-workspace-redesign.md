# EPIC-0020 — Refonte du workspace Facturation & Caisse

## Mode

Project Manager + Product Design Audit + Engineering planification.

## Objectif métier

Permettre à un agent de facturation, un caissier ou un agent de recouvrement de savoir immédiatement :

1. ce qui doit être créé ;
2. ce qui doit être encaissé auprès du patient ;
3. ce qui doit être recouvré auprès de l'assurance ;
4. ce qui est réellement soldé ;
5. quelle action est disponible maintenant.

## Audit fondé sur les captures fournies

### Étape 1 — Sélection patient / visite / émission

**Constats visibles :**

- Le formulaire `Émettre une facture` reste dominant alors qu'une facture existe déjà.
- Le bouton `Facture déjà émise` reste visuellement proche d'une action active, ce qui laisse penser qu'il peut être utilisé.
- `Voir la facture existante` modifie l'état plus bas dans la page mais ne donne pas de retour visible ni de défilement vers la cible.
- Les lignes pré-calculées restent affichées et éditables alors que l'émission est interdite.

**Risque :** l'utilisateur ne sait pas s'il est dans un mode création, consultation ou règlement.

### Étape 2 — Historique et règlement patient

**Constats visibles :**

- Le bouton `Régler la part patient` se replie sur trois lignes et prend trop de poids visuel.
- Les boutons `Gérer`, `PDF` et `Régler` sont présentés sans hiérarchie claire.
- La facture `PAID` de la capture précédente pouvait encore avoir des créances ouvertes, ce qui détruit la confiance dans le statut.

**Risque :** double encaissement, mauvaise interprétation du statut et forte charge cognitive.

### Étape 3 — Détail facture / devis / avoirs / créances

**Constats visibles :**

- Devis, avoirs, facture sélectionnée et créances sont empilés sous l'historique.
- L'utilisateur doit effectuer un grand défilement pour atteindre le détail.
- Le détail sélectionné affiche un statut technique (`PAID`) au lieu de l'état financier ventilé.
- Le bouton `Créer un avoir` est visible dans le même espace que les opérations de règlement, sans séparation entre action courante et action exceptionnelle.

**Risque :** perte du contexte, difficulté à retrouver une action et erreurs de correction financière.

### Limites de l'audit

- Les captures ne permettent pas de vérifier les droits par rôle, les messages après validation ni le comportement mobile complet.
- Le test d'encaissement réel et le règlement de bordereau doivent être validés avec une session de caisse et des données de test contrôlées.

## Cible UX proposée

### Navigation principale

- `Factures` : créer ou consulter les factures patient.
- `Caisse` : session, encaissements et reçus.
- `Créances` : reste patient et assurance, relances.
- `Assurances` : bordereaux et règlements assurance.
- `Paramètres financiers` : conventions et tarifs, réservé aux rôles autorisés.

### Règle de contexte

- Aucune facture existante pour la visite : mode `Nouvelle facture`.
- Facture existante : mode `Facture existante`, formulaire de création masqué, actions disponibles affichées selon l'état financier.
- `PATIENT_DUE` ou `PATIENT_PARTIALLY_PAID` : action `Encaisser patient`.
- `INSURANCE_DUE` : action `Ouvrir le suivi assurance`.
- `SETTLED` : état final, aucune action d'encaissement.

### Détail facture

Le détail s'ouvre dans un panneau latéral ou une vue dédiée contenant :

- résumé financier patient / assurance ;
- lignes de facture ;
- paiements et reçus ;
- créances ;
- documents et avoirs dans des sections secondaires repliables.

## Découpage Epic → Stories → Tasks

### STORY-2201 — Contrat d'état financier unique

**User story :** En tant qu'utilisateur financier, je veux un état lisible par débiteur afin de savoir qui doit encore payer.

- **Objectif :** supprimer la contradiction entre `Invoice.status` et les créances.
- **Critères d'acceptation :**
  - [ ] Le statut affiché vient d'un contrat de règlement ventilé.
  - [ ] Une facture tiers-payant n'est `SETTLED` qu'après règlement patient et assurance.
  - [ ] Les transitions patient et assurance sont couvertes côté backend.
- **Estimation :** 5 SP, 1.5 j senior.
- **Profil :** senior backend/full-stack.
- **Reviewer :** Lead Developer + DAF.
- **Tests :** unitaires service, MockMvc, E2E financier.

### STORY-2202 — Workspace Factures orienté tâche

**User story :** En tant que secrétaire comptable, je veux savoir si je dois créer, consulter ou encaisser une facture.

- **Objectif :** séparer le mode création du mode facture existante.
- **Critères d'acceptation :**
  - [ ] Une visite facturée ne présente plus de formulaire d'émission actif.
  - [ ] Le clic sur une facture existante ouvre réellement son détail et le met en visibilité.
  - [ ] Les actions sont contextuelles et tiennent sur une ligne en desktop.
  - [ ] Les états vide, chargement et erreur sont explicites.
- **Estimation :** 8 SP, 2.5 j senior.
- **Profil :** senior frontend Angular + designer produit.
- **Reviewer :** Lead Developer + Product/DAF.
- **Tests :** tests composants, navigation clavier, responsive, E2E.

### STORY-2203 — Poste caissier simplifié

**User story :** En tant que caissier, je veux ouvrir ma session puis encaisser une part patient avec un reçu sans chercher dans une longue page.

- **Objectif :** créer une file d'encaissement et un panneau de paiement focalisé.
- **Critères d'acceptation :**
  - [ ] L'état de session est visible avant toute action de paiement.
  - [ ] Le montant restant est fourni par le backend et non recalculé par l'écran.
  - [ ] Le paiement produit mouvement, reçu et mise à jour de créance.
  - [ ] Les erreurs 409 sont actionnables.
- **Estimation :** 8 SP, 2.5 j senior.
- **Profil :** senior full-stack.
- **Reviewer :** Lead Developer + DAF.
- **Tests :** cash register integration, composant paiement, E2E.

### STORY-2204 — Poste assurance et bordereaux

**User story :** En tant que chargé de recouvrement, je veux générer, envoyer et solder un bordereau avec une progression lisible.

- **Objectif :** rendre le parcours `DRAFT → SENT → PAID` explicite.
- **Critères d'acceptation :**
  - [ ] Les factures éligibles et le montant total sont visibles avant génération.
  - [ ] Chaque statut propose une seule prochaine action.
  - [ ] Le règlement exige référence et montant contrôlé.
  - [ ] Les créances assurance associées passent à jour après paiement.
- **Estimation :** 5 SP, 1.5 j senior.
- **Profil :** intermédiaire frontend + senior backend reviewer.
- **Reviewer :** DAF + Lead Developer.
- **Tests :** `InsuranceBordereauControllerTest`, tests UI et E2E.

### STORY-2205 — Détail facture, documents et actions exceptionnelles

**User story :** En tant qu'agent financier, je veux consulter les détails sans parcourir toute la page.

- **Objectif :** déplacer devis, avoirs, créances et paiements dans une vue secondaire contextualisée.
- **Critères d'acceptation :**
  - [ ] Le détail s'ouvre par sélection d'une facture et le focus est déplacé vers lui.
  - [ ] Les avoirs sont séparés des encaissements et protégés par confirmation.
  - [ ] La fermeture retourne à l'historique sans perte de contexte.
- **Estimation :** 5 SP, 1.5 j senior.
- **Profil :** senior frontend.
- **Reviewer :** Lead Developer + DAF.
- **Tests :** composants, accessibilité clavier, régression responsive.

### STORY-2206 — QA UX, accessibilité et régression financière

- **Objectif :** valider les parcours par rôle et les états critiques.
- **Estimation :** 5 SP, 1.5 j senior QA/full-stack.
- **Tests :** E2E, RBAC, axe/WCAG, desktop/tablette/mobile, parcours assurance.

## Estimation globale et capacité

| Élément | Valeur |
|---|---:|
| Story points | 36 SP |
| Effort senior estimé | 11.0 j |
| Effort intermédiaire estimé | 14.3 j |
| Profil recommandé | Senior full-stack + Product/DAF + QA |
| Capacité planifiable recommandée | 60–70 % d'un sprint avec support |
| Découpage recommandé | 2 sprints |

## Definition of Ready

- [ ] Arbitrage DAF sur la signification définitive de `PAID` et `SETTLED`.
- [ ] Maquette ou validation du panneau de détail.
- [ ] Données de test patient 100 %, tiers-payant, paiement partiel et assurance.
- [ ] Rôles et permissions confirmés.
- [ ] Contrats API et documentation initiale validés.

## Definition of Done

- [ ] Tous les critères des stories sont validés.
- [ ] Tests Angular, Maven et E2E au vert.
- [ ] Parcours clavier et responsive vérifiés.
- [ ] Documentation, changelog, suivi et release note mis à jour.
- [ ] Validation métier DAF obtenue.

## Décision SemVer proposée

`MINOR` : refonte rétrocompatible du parcours et enrichissement de l'expérience financière, sous réserve de ne pas modifier les contrats API publics de manière cassante.

## Avancement d'implémentation

### Lot 1 — STORY-2202 partiel

- [x] Le formulaire de création est masqué lorsqu'une visite possède déjà une facture.
- [x] La carte facture existante présente total, part patient, part assurance et prochaine action.
- [x] Le détail facture s'ouvre dans un panneau latéral visible immédiatement.
- [x] L'historique utilise des libellés courts (`Détail`, `Encaisser`) adaptés aux petits écrans.
- [x] Le détail financier est affiché avant les sections secondaires devis/avoirs.
- [x] Le paiement informe l'utilisateur lorsqu'aucune session de caisse n'est ouverte et propose d'ouvrir la caisse.
- [x] L’annulation de facture est protégée par une modale interne accessible et internationalisée.
- [x] Le devis peut être lié à une visite et transmet le `visitId` sélectionné.
- [x] Les retours succès/erreur et le chargement deep-link sont visibles.
- [x] Les tests du panneau devis couvrent les mutations financières critiques.
- [x] Build Angular et 111 tests frontend réussis.
- [ ] Validation visuelle navigateur locale : bloquée par la politique d'accès à `localhost` dans l'environnement Codex.
- [ ] Lot 2 : simplifier l'écran caisse, la file de créances et les bordereaux assurance.
- [x] Correctif ponctuel du lot 2 : l'en-tête de la liste des bordereaux conserve son titre sur une ligne et un filtre de statut de largeur bornée sur tablette/desktop, avec repli vertical sur mobile.
