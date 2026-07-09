# EPIC-0017 — Complétion Hospitalisation, Facturation et Caisse

## 1. Objectif métier

Amener les modules hospitalisation, facturation et caisse au niveau attendu par le CDC V2.1 et les documents réels TC2CDK, afin que la clinique puisse gérer un séjour patient complet, facturer tous les actes, encaisser, clôturer la caisse et générer les premières écritures OHADA.

## 2. Périmètre

### Inclus

- Séjour hospitalier complet : admission, billets, soins, bloc, sortie.
- Facturation complète : devis, facture validée, avoir, créances.
- Caisse : ouverture, encaissements, reçus, dépenses, clôture, écarts.
- Assurance : tiers payant avancé et bordereaux.
- Comptabilité minimale : écritures OHADA sur factures et encaissements.
- Refactor Angular pour respecter les limites de taille.

### Exclus

- Paie complète.
- Achats et fournisseurs complets.
- États financiers réglementaires complets.
- Intégration bancaire automatisée.

## 3. Utilisateurs concernés

- Médecin
- Infirmier / Major
- Chirurgien / Anesthésiste
- Secrétaire comptable
- Caissier
- DAF
- Médecin Chef
- Patient

## 4. User stories

| ID | Titre | Priorité | SP | Statut | Sprint cible |
|---|---|---|---:|---|---|
| STORY-2101 | Refactor UI et services volumineux hospitalisation/facturation | P0 | 3 | DONE | SPRINT-0012 |
| STORY-2102 | Séjour hospitalier complet et documents d'entrée/sortie | P0 | 5 | DONE | SPRINT-0012 |
| STORY-2103 | Soins journaliers, administration médicaments et consommables | P0 | 8 | DONE | SPRINT-0012 |
| STORY-2104 | Bloc opératoire, CRO, anesthésie et implants | P0 | 8 | DONE | SPRINT-0012 |
| STORY-2105 | Devis, factures validées, remises, avoirs et créances | P0 | 8 | DONE | SPRINT-0012 |
| STORY-2106 | Caisse recettes/dépenses et clôture journalière | P0 | 8 | READY | À planifier |
| STORY-2107 | Bordereaux assurance et tiers payant avancé | P1 | 5 | READY | À planifier |
| STORY-2108 | Imputations comptables OHADA minimales | P1 | 8 | READY | À planifier |
| STORY-2109 | Reporting clinique et financier de contrôle | P1 | 5 | READY | À planifier |

## 4.1 Diagnostic PM du 2026-07-09

Le module facturation ne doit pas être considéré comme complet. L'état réel est :

- facturation simple opérationnelle : factures, lignes, conventions, tarifs, paiements, PDF ;
- facturation avancée partielle : devis, validation, remises, avoirs et créances existent côté backend, mais le parcours UX principal ne les expose pas encore complètement ;
- caisse non livrée : pas de session, reçu numéroté séparé, clôture, écart, mouvements caisse-banque ou dépenses de caisse ;
- comptabilité OHADA non livrée.

Priorité planning recommandée :

1. Terminer STORY-2105 avant de la déclarer DONE : intégration UX, tests, numérotation métier robuste, correction du principal JWT sur validation facture.
2. Démarrer STORY-2106 juste après : vrai module caisse avec ouverture, encaissement, reçu, clôture journalière et audit.
3. Reporter STORY-2107/2108 après validation DAF du flux caisse et des règles d'imputation.

## 5. Tasks par story

| Story | Task | Objectif | Critères d'acceptation | Profil | Estimation senior | Reviewer | Tests attendus |
|---|---|---|---|---|---:|---|---|
| STORY-2101 | TASK-2101-01 | Extraire composants facturation | `billing-management-page` < 500 lignes | Senior Frontend | 0.4j | Lead Frontend | DONE — Angular build + tests OK |
| STORY-2101 | TASK-2101-02 | Extraire composants hospitalisation | `patient-hospitalization` < 500 lignes | Senior Frontend | 0.4j | Lead Frontend | DONE — Angular build + tests OK |
| STORY-2102 | TASK-2102-01 | Ajouter documents d'entrée/sortie | Billet entrée/sortie PDF | Full-stack | 0.5j | Médecin Chef | Tests PDF/API |
| STORY-2102 | TASK-2102-02 | Ajouter sortie contre avis médical | Flux + document + audit | Full-stack | 0.5j | Médecin Chef | Tests API |
| STORY-2102 | TASK-2102-03 | Ajouter consentements opératoires | Signature/upload lié au séjour | Full-stack | 0.5j | Lead | Tests RBAC/API |
| STORY-2103 | TASK-2103-01 | Créer feuille de soins structurée | CRUD soins journaliers | Backend | 0.6j | Lead | Tests service/controller |
| STORY-2103 | TASK-2103-02 | Administrer médicaments | Horodatage, auteur, source prescription | Backend | 0.6j | Médecin Chef | Tests métier |
| STORY-2103 | TASK-2103-03 | Lier consommables au patient | Lignes facturables et stock future-proof | Backend | 0.4j | DAF | Tests métier |
| STORY-2103 | TASK-2103-04 | UI séjour onglets soins | Écran utilisable, i18n | Frontend | 0.4j | Lead Frontend | Angular tests |
| STORY-2104 | TASK-2104-01 | Modèle CRO/anesthésie | Tables + entités | Backend | 0.5j | Médecin Chef | Migration/tests |
| STORY-2104 | TASK-2104-02 | Actes K depuis bloc | Génération lignes facturables | Backend | 0.5j | DAF | Tests calcul |
| STORY-2104 | TASK-2104-03 | Traçabilité implants | Lots, quantités, document | Backend | 0.4j | Médecin Chef | Tests API |
| STORY-2104 | TASK-2104-04 | UI bloc/CRO | Saisie et validation | Frontend | 0.6j | Lead Frontend | Angular build/tests |
| STORY-2105 | TASK-2105-01 | Devis/proforma | Création, PDF, conversion facture | Full-stack | 0.5j | DAF | DONE — Tests API/PDF |
| STORY-2105 | TASK-2105-02 | Validation facture immuable | Statut VALIDATED + historique | Backend | 0.5j | DAF | DONE — Tests métier |
| STORY-2105 | TASK-2105-03 | Avoirs/remises/annulations | Correction contrôlée | Backend | 0.5j | DAF | DONE — Tests métier |
| STORY-2105 | TASK-2105-04 | Créances patient/assurance | Liste et filtres | Full-stack | 0.5j | DAF | DONE — Tests API/UI |
| STORY-2105 | TASK-2105-05 | Intégrer l'UX devis/avoirs/créances | Composant visible dans le parcours principal | Frontend | 0.3j | Lead Frontend | DONE — Angular build/tests |
| STORY-2105 | TASK-2105-06 | Sécuriser numérotation et principal JWT | Numéros robustes + validation facture sans erreur principal | Backend | 0.4j | Lead Backend | DONE — Tests API |
| STORY-2106 | TASK-2106-01 | Modèle caisse/session | Tables et service | Backend | 0.5j | DAF | Tests migration/service |
| STORY-2106 | TASK-2106-02 | Paiement rattaché session | Reçu numéroté | Backend | 0.5j | DAF | Tests API |
| STORY-2106 | TASK-2106-03 | Clôture de caisse | Solde, déclaré, écart | Backend | 0.5j | DAF | Tests métier |
| STORY-2106 | TASK-2106-04 | UI caisse | Ouverture, encaissement, clôture | Frontend | 0.5j | Lead Frontend | Angular tests |
| STORY-2107 | TASK-2107-01 | Bordereaux assurance | Agrégation mensuelle | Backend | 0.6j | DAF | Tests API |
| STORY-2107 | TASK-2107-02 | UI bordereaux | Export/consultation | Frontend | 0.6j | DAF | Angular build |
| STORY-2108 | TASK-2108-01 | Plan minimal journaux/comptes | Tables OHADA | Backend | 0.5j | DAF | Tests migration |
| STORY-2108 | TASK-2108-02 | Posting facture | 411/706 | Backend | 0.5j | DAF | Tests unitaires |
| STORY-2108 | TASK-2108-03 | Posting encaissement | 571 ou 521 / 411 | Backend | 0.5j | DAF | Tests unitaires |
| STORY-2108 | TASK-2108-04 | Consultation écritures | API + UI lecture | Full-stack | 0.5j | DAF | Tests API/UI |
| STORY-2109 | TASK-2109-01 | Indicateurs hospitalisation | DMS, occupation, sorties | Full-stack | 0.5j | Lead | Tests API |
| STORY-2109 | TASK-2109-02 | Indicateurs financiers | CA, encaissements, créances | Full-stack | 0.5j | DAF | Tests API |

## 6. Dépendances

| Dépendance | Type | Impact |
|---|---|---|
| STORY-1912 Gestion spatiale | Technique | Admission et transferts de lits |
| STORY-1913 Facturation médicale | Technique | Socle factures/paiements |
| STORY-1907 Hospitalisations CDC | Technique | Socle séjour |
| Décision DAF comptes OHADA | Métier | Imputations comptables |
| Décision rôles financiers | Sécurité | RBAC caisse et comptabilité |

## 7. Risques

| Risque | Impact | Mitigation |
|---|---|---|
| Trop gros pour un sprint | Dérive | Planifier sur 2 à 3 sprints |
| Comptabilité incorrecte | Risque conformité | Validation DAF obligatoire |
| UI monolithique actuelle | Régression | STORY-2101 en premier |
| Données financières sensibles | Risque sécurité | RBAC + audit + tests cross-tenant |

## 8. Définition de succès

- [ ] Flux complet admission -> soins -> facture -> paiement -> clôture démontrable.
- [ ] Caisse clôturée avec état imprimable.
- [ ] Facture validée immuable et corrigible seulement par avoir.
- [ ] Écritures OHADA minimales générées.
- [ ] Tests backend et Angular au vert.

## 9. Estimation globale

| Élément | Valeur |
|---|---|
| Total story points | 58 |
| Effort senior | 12.0j |
| Effort intermédiaire | 17.5j |
| Effort junior | 28.0j |
| Nombre de sprints estimé | 2 à 3 sprints selon capacité |

## Impact version / SemVer

| Champ | Valeur |
|---|---|
| Impact version | MINOR |
| Justification | Nouvelles fonctionnalités rétrocompatibles majeures |
| Breaking change | Non prévu |
| Release cible | À planifier |

## Impact UI / thème / i18n / configuration

| Point | Valeur |
|---|---|
| Impact Angular | Oui |
| Impact Flutter | Non |
| Thème centralisé impacté | Non, réutilisation des tokens |
| Dark/light à vérifier | Oui |
| Traductions FR/EN nécessaires | Oui |
| Composants/widgets réutilisables à créer | Oui |
| Configuration app/branding impactée | Non |
| Logo / nom app / slogan impactés | Non |

## Documentation First

- [x] Documentation fonctionnelle initiale créée : `docs/features/hospitalisation-facturation-caisse-complete/FUNCTIONAL-SPEC.md`
- [x] Documentation technique initiale créée : `docs/features/hospitalisation-facturation-caisse-complete/TECHNICAL-DESIGN.md`
- [x] `API-CONTRACT.md` créé
- [x] `DATA-MODEL.md` créé
- [x] `TEST-PLAN.md` créé
- [ ] Documentation relue en review
- [ ] Documentation à jour avant passage à DONE

## Design System / UI

- [x] `DESIGN.md` lu
- [x] Tokens couleurs/typo/spacing/radius inchangés
- [x] Composants réutilisables identifiés
- [x] Tailwind CSS v4 vérifié côté Angular
- [x] Tailwind v3 / Angular Material non requis
- [x] Light/dark à vérifier pendant implémentation
- [x] i18n FR/EN prévue
- [x] Contraste/focus/accessibilité à vérifier pendant implémentation

## Architecture / SOLID

- [x] Backend maître de la vérité métier identifié.
- [x] Front limité à l'affichage, l'expérience utilisateur et l'état de présentation.
- [x] Controllers sans logique métier à conserver.
- [x] Services/use cases, interfaces et implémentations prévus.
- [x] Aucune classe/composant/widget ne doit dépasser 500 lignes.
