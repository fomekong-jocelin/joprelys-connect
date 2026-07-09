# EPIC-0017 — Complétion Hospitalisation, Facturation et Caisse

| Champ | Valeur |
|---|---|
| **ID** | EPIC-0017 |
| **Type** | Epic / Cadrage Engineering + Project Manager |
| **Titre** | Compléter les modules Hospitalisation, Facturation et Caisse selon le CDC V2.1 |
| **Statut** | IN_PROGRESS |
| **Priorité** | P0 |
| **Stack** | Full-stack |
| **Profil recommandé** | Senior full-stack + reviewer DAF / Médecin chef |
| **Estimation Senior** | 12.0j |
| **Estimation Intermédiaire** | 17.5j |
| **Estimation Junior** | 28.0j |
| **Sprint cible** | À planifier |
| **Assigné** | À assigner |
| **Reviewer** | Lead Developer + DAF + Médecin Chef |
| **Dernière MAJ** | 2026-07-09 |

---

## 1. Contexte

Les modules actuels couvrent un socle utile mais trop léger par rapport au CDC V2.1 et au document réel TC2CDK :

- hospitalisation : admission, lit, notes libres et fiche de sortie PDF existent, mais le séjour complet n'est pas encore structuré ;
- facturation : facture, lignes, tarifs simples, assurance et paiement existent, mais il manque les devis, créances, reçus, bordereaux et immutabilité financière ;
- caisse : les paiements existent comme lignes attachées à une facture, mais il n'y a pas de vraie caisse avec sessions, clôture journalière, écart de caisse et mouvements caisse-banque ;
- comptabilité : aucune imputation OHADA automatique n'est encore modélisée.

La demande est macro et dépasse 2-3 jours. Elle doit donc être traitée par découpage EPIC -> stories -> tasks avant développement.

---

## 2. Mode d'intervention

| Mode | Décision |
|---|---|
| Diagnostic | Oui, écart CDC vs existant |
| Engineering | Oui, backend + Angular + DB + tests |
| Project Manager | Oui, découpage et estimation requis |
| Architecture | Oui, flux financier et comptable structurant |
| Release | À prévoir lors de l'implémentation |

---

## 3. État actuel observé

### Diagnostic complémentaire du 2026-07-09 — Facturation / Caisse / UX

Conclusion : le module facturation n'est pas complet au sens métier "clinique + caisse". Le socle facture existe et les briques devis/avoirs/créances ont été amorcées, mais il ne faut pas le présenter comme un vrai module caisse exploitable.

Constats vérifiés dans le code :

- [x] Factures, lignes, conventions, tarifs, paiements et PDF facture existent dans `backend/src/main/java/com/joprelys/backend/billing`.
- [x] Devis/proformas, validation de facture, remises, avoirs et créances existent côté backend via `EstimateService` / `EstimateController` et la migration `V49__billing_estimates_receivables.sql`.
- [ ] Le composant Angular `BillingEstimatesComponent` n'est pas intégré dans la page principale de facturation : les devis/avoirs/créances ne sont donc pas encore un parcours utilisateur complet.
- [ ] Aucune vraie caisse n'existe dans le code : pas de `cash_registers`, pas de session ouverte/fermée, pas de reçu numéroté séparé, pas de clôture journalière, pas d'écart de caisse, pas de mouvements caisse-banque.
- [ ] Les paiements restent attachés directement à une facture sans session de caisse, ce qui ne satisfait pas la règle métier BR-HFC-005.
- [ ] Les écritures OHADA ne sont pas implémentées.
- [ ] Les tests existants couvrent surtout le flux facture/paiement/PDF ; les endpoints devis/validation/avoirs/créances et la future caisse ne sont pas suffisamment couverts.
- [ ] Risque technique identifié : `EstimateController.validateInvoice()` utilise `UUID.fromString(authentication.getName())`, alors que les autres flux utilisent généralement l'email comme principal JWT. Ce point peut provoquer une erreur 500/400 à la validation si le principal n'est pas un UUID.
- [ ] Risque de production identifié : la numérotation des devis/avoirs utilise `System.currentTimeMillis()` et un compteur mémoire, non conforme à une numérotation métier robuste, tenant-aware et redémarrage-safe.

### Hospitalisation

- [x] Admission liée à une visite et un médecin responsable.
- [x] Association chambre/lit/service et intégration partielle au module spatial.
- [x] Notes journalières libres.
- [x] Sortie avec PDF et document médical vérifiable.
- [ ] Billet d'entrée officiel.
- [ ] Ordonnance d'admission structurée.
- [ ] Consentement anesthésie / opération.
- [ ] Feuille de soins journalière typée.
- [ ] Administration horodatée des médicaments.
- [ ] Consommation de consommables par patient.
- [ ] Avis consultatifs.
- [ ] Suivi post-opératoire J1 à Jn.
- [ ] CRO / bloc opératoire / fiche anesthésique.
- [ ] Décès, permis d'inhumer et verrouillage financier.
- [ ] Sortie contre avis médical.

### Facturation

- [x] Factures, lignes, tarifs, conventions simples.
- [x] Paiements partiels et PDF facture.
- [x] Répartition patient / assurance simple.
- [ ] Devis / proforma avant hospitalisation.
- [ ] Facture validée immuable.
- [ ] Remises, avoirs et annulations contrôlées.
- [ ] Créances patient et tiers payant.
- [ ] Bordereaux mensuels assurance.
- [ ] Reçu / ticket de caisse séparé de la facture.
- [ ] Numérotation configurable au format clinique.
- [ ] Facturation des soins, examens, bloc, kiné, consommables et restauration à partir d'événements source.

### Caisse et comptabilité

- [ ] Caisse recettes / caisse dépenses.
- [ ] Ouverture et clôture de session de caisse.
- [ ] Fond de caisse, solde théorique, solde déclaré, écart.
- [ ] Mouvements caisse-banque.
- [ ] Plafond caisse dépenses 100 000 FCFA avec double visa DAF + Médecin Chef.
- [ ] Imputations automatiques OHADA : 411/706, 571/521/411, 58/57/52.
- [ ] Journal des recettes, journal de caisse, journal de banque.

---

## 4. Découpage EPIC -> User Stories -> Tasks

| ID | Titre | Priorité | SP | Profil | Est. Senior | Statut |
|---|---|---|---:|---|---:|---|
| STORY-2101 | Refactor UI et services volumineux hospitalisation/facturation | P0 | 3 | Senior Frontend | 0.8j | DONE |
| STORY-2102 | Séjour hospitalier complet et documents d'entrée/sortie | P0 | 5 | Senior Full-stack | 1.5j | DONE |
| STORY-2103 | Soins journaliers, administration médicaments et consommables | P0 | 8 | Senior Full-stack | 2.0j | DONE |
| STORY-2104 | Bloc opératoire, CRO, anesthésie et implants | P0 | 8 | Senior Backend + Frontend intermédiaire | 2.0j | DONE |
| STORY-2105 | Devis, factures validées, remises, avoirs et créances | P0 | 8 | Senior Full-stack | 2.0j | DONE |
| STORY-2106 | Caisse recettes/dépenses et clôture journalière | P0 | 8 | Senior Full-stack | 2.0j | DONE |
| STORY-2107 | Bordereaux assurance et tiers payant avancé | P1 | 5 | Senior Backend | 1.2j | DONE |
| STORY-2108 | Imputations comptables OHADA minimales | P1 | 8 | Senior Backend + DAF | 2.0j | READY |
| STORY-2109 | Reporting clinique et financier de contrôle | P1 | 5 | Intermédiaire Full-stack | 1.0j | READY |

---

## 5. Critères d'acceptation globaux

- [ ] Le patient hospitalisé dispose d'un dossier de séjour structuré : entrée, soins, bloc, prescriptions, consommables, avis, sortie.
- [ ] Chaque acte financier critique est traçable, numéroté et non modifiable après validation.
- [ ] Une facture peut être précédée d'un devis/proforma puis convertie en facture.
- [ ] Les paiements produisent un reçu ou ticket de caisse séparé.
- [ ] La caisse journalière peut être ouverte, alimentée, clôturée et contrôlée.
- [ ] Les créances patient et assurance sont consultables et exportables.
- [ ] Les écritures OHADA minimales sont générées automatiquement sur validation facture et encaissement.
- [ ] Les composants Angular impactés sont découpés sous 500 lignes.
- [ ] Les textes visibles sont internationalisés FR/EN.
- [ ] Les tests backend et Angular couvrent les règles critiques.

---

## 6. Action plan

- [x] Lire les consignes obligatoires et standards.
- [x] Analyser le CDC V2.1 et les documents réels TC2CDK.
- [x] Comparer le périmètre attendu aux modules existants.
- [x] Identifier les gaps majeurs.
- [x] Créer le ticket macro.
- [x] Créer la documentation fonctionnelle initiale.
- [x] Créer la documentation technique initiale.
- [x] Créer le contrat API cible.
- [x] Créer le modèle de données cible.
- [x] Créer le plan de test cible.
- [x] Implémenter STORY-2101 — refactor UI hospitalisation/facturation.
- [x] Implémenter STORY-2102 à STORY-2105.
- [x] Implémenter STORY-2106.
- [x] Implémenter STORY-2107 — bordereaux assurance et tiers-payant.
- [ ] Implémenter STORY-2108 à STORY-2109 (OHADA et reporting).
- [x] Exécuter `backend/mvnw test` quand une story backend est modifiée.
- [x] Exécuter `web/npm run build` et tests Angular pour STORY-2101.
- [x] Auditer l'état réel facturation/caisse après ajout de `V49` et du composant devis.
- [x] Intégrer le parcours devis/avoirs/créances dans la page facturation principale.
- [x] Corriger la validation facture si le principal JWT est un email.
- [x] Remplacer la numérotation mémoire des devis/avoirs by une séquence robuste tenant-aware.
- [x] Créer le vrai module caisse : sessions, reçus, clôture, mouvements.
- [ ] Préparer la release MINOR quand le périmètre sera livré.

---

## 7. Risques

| Risque | Impact | Mitigation |
|---|---|---|
| Périmètre trop large | Dérive sprint | Livraison par stories indépendantes |
| Flux financier sensible | Erreurs de caisse / créances | Tests métier + validation DAF |
| Composants Angular trop gros | Dette et régressions UI | Refactor en premier |
| Comptabilité OHADA complexe | Décision métier incomplète | Implémenter un socle minimal, documenter les limites |
| Données patient + financières | Risque sécurité / RGPD | RBAC strict, audit, masquage logs |

---

## 8. Impact version / SemVer

| Champ | Valeur |
|---|---|
| Impact version | MINOR |
| Justification | Ajout rétrocompatible de fonctionnalités majeures hospitalisation, facturation, caisse et comptabilité |
| Breaking change | Non prévu |
| Release cible | À planifier |

---

## 9. Reste à faire

- Prioriser les stories en sprint planning (STORY-2107 à STORY-2109).
- Valider avec le DAF les règles de caisse, clôture, écarts et écritures OHADA.
- Valider avec le Médecin Chef les documents hospitaliers, CRO, consentements et sorties.
- Démarrer STORY-2107/2108 après stabilisation de la caisse et validation DAF.

---

## 10. Suivi STORY-2101

| Élément | Résultat |
|---|---|
| Statut | DONE |
| Date | 2026-07-09 |
| Changements | Extraction des templates inline et création de composants Angular dédiés pour l'historique de factures, la configuration conventions/tarifs et la modale de paiement |
| Fichiers concernés | `billing-management-page.component.ts/html`, `patient-hospitalization.component.ts/html`, `billing-admin-tabs.component.ts`, `billing-invoice-history.component.ts`, `billing-payment-modal.component.ts` |
| Taille après refactor | Facturation TS 315 lignes, HTML 288 lignes ; Hospitalisation TS 335 lignes, HTML 280 lignes |
| Tests | `npm run build` OK ; `npm run test -- --watch=false` OK, 101 tests passés |

---

## 11. Suivi STORY-2105

| Élément | Résultat |
|---|---|
| Statut | DONE |
| Date | 2026-07-09 |
| Changements | Résolution de la numérotation des devis (DEV-...) et avoirs (AV-...) par séquences PostgreSQL (`estimate_number_seq` et `credit_note_number_seq`). Résolution de l'identification de l'utilisateur par e-mail JWT dans la validation de facture. Intégration du composant devis/avoirs/créances `BillingEstimatesComponent` dans le parcours principal de facturation et sélection des factures depuis l'historique. |
| Fichiers concernés | `V50__add_estimate_and_credit_note_sequences.sql`, `EstimateRepository.java`, `CreditNoteRepository.java`, `EstimateService.java`, `EstimateController.java`, `billing-invoice-history.component.ts`, `billing-management-page.component.ts/html`, `EstimateControllerTest.java` |
| Tests | `mvn test` OK (263 tests au vert) ; `npm run build` OK ; `npm run test` OK |

---

## 12. Suivi STORY-2106

| Élément | Résultat |
|---|---|
| Statut | DONE |
| Date | 2026-07-09 |
| Changements | Création des tables de caisses physiques, sessions, mouvements et reçus numérotés (`REC-yyyyMMdd-XXXXXX`) via Flyway V51. Enregistrement des règlements soumis obligatoirement à une session active de caisse. Plafond de dépenses à 100 000 FCFA soumis au double visa. Contrôle des écarts de caisse avec justification obligatoire. Création du contrôleur REST `CashRegisterController` et de ses tests. Création du service d'API Angular et des composants UI découpés (`BillingCashRegisterComponent` pour les sessions et mouvements, et `BillingReceivablesComponent` pour l'affichage des créances). Réorganisation de la page principale de facturation en onglets pour ne pas encombrer l'existant. |
| Fichiers concernés | `V51__create_cash_register_tables.sql`, `CashRegisterController.java`, `CashRegisterControllerTest.java`, `InvoiceController.java`, `billing-api.service.ts`, `patient.models.ts`, `billing-cash-register.component.ts`, `billing-receivables.component.ts`, `billing-management-page.component.ts/html` |
| Tests | `mvn test` OK (264 tests passés) ; `npm run build` OK |

