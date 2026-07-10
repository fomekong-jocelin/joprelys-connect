# Conception technique — Postes métier hospitalisation et caisse

## État initial

Le composant `patient-hospitalization` dépasse les limites projet : 640 lignes TypeScript et 797 lignes HTML. Il contient des signaux `any`, plusieurs formulaires et six domaines d'interaction. Il doit être décomposé avant toute évolution fonctionnelle supplémentaire.

Le poste de caisse issu d'EPIC-0018 reste à consolider par revue Git et validation DAF, mais son modèle backend de sessions, mouvements et rapprochement constitue le socle métier ; il ne doit pas être réimplémenté dans Angular.

## Découpage Angular cible

```text
features/hospitalization/
  pages/stay-workspace/
  components/stay-context-header/
  components/stay-timeline/
  components/admission-form/
  components/transfer-panel/
  components/nursing-worklist/
  components/daily-care-form/
  components/medication-administration-form/
  components/operating-report-form/
  components/discharge-panel/
  data-access/hospitalization-api.service.ts
  state/stay-workspace.facade.ts

features/billing/
  pages/cashier-workspace/
  pages/collections-workspace/
  pages/finance-supervision/
  components/invoice-settlement-summary/
  components/cash-session-summary/
  components/financial-exception-dialog/
```

Les pages orchestrent ; les composants présentent ; les façades conservent seulement l'état UI et appellent les services API. Les contrats TypeScript remplacent tous les `any`. Les états métier, validations financières, autorisations et calculs restent dans les cas d'usage Spring Boot.

## API et données

Avant STORY-2121 à 2124, auditer les contrats existants pour privilégier des read models dédiés plutôt que d'agréger dans les templates :

- `StayWorkspaceSummary` : identité autorisée, statut, lit/service, responsables, événements et actions permises ;
- `NursingWorklistItem` : séjour, priorité, dernière transmission et actions autorisées ;
- `CashierInvoiceSummary` : débiteur, montant exigible, état de règlement ventilé, session active et actions ;
- `CashSessionSummary` : espèces, chèques, virements, dépenses, dépôts, théorique, déclaré, écart et droits.

Toute évolution d'API possède DTO immutable, validation de frontière, contrôle RBAC côté service/controller, test MockMvc et compatibilité documentée dans l'API contract.

## Sécurité et audit

- Appliquer le moindre privilège aux données patient et financières, avec test explicite autorisé/interdit pour chaque endpoint.
- Ne jamais journaliser le contenu clinique, les documents signés, le montant complet ou un identifiant patient en clair si la politique de logs ne le permet pas.
- Les écritures validées sont append-only au sens fonctionnel : correction et annulation doivent produire une trace distincte.
- Les fichiers médicaux restent validés par type, taille, contenu et tenant, conformément aux contrôles upload du backend.

## Contraintes UI

Angular conserve Tailwind CSS v4 CSS-first, composants maison, proxy relatif, light/dark et i18n FR/EN. Les composants respectent 300 lignes cible / 500 maximum ; les rayons, ombres, couleurs et états utilisent les tokens documentés dans `DESIGN.md`.

## Première implémentation — 2026-07-10

`HospitalizationStayHeaderComponent` extrait le contexte de séjour (statut, numéro, localisation, médecin, date, motif) et les actions de document, transfert et sortie. Le composant est standalone, `OnPush`, testé, intégré aux thèmes existants et ne possède aucune règle clinique. La navigation existante des activités utilise désormais les rôles ARIA et des clés FR/EN.

Les panneaux `HospitalizationNotesPanelComponent`, `HospitalizationDailyCarePanelComponent`, `HospitalizationMedicationPanelComponent` et `HospitalizationConsumptionPanelComponent` portent désormais leurs propres chargements, états d'erreur, formulaires et tests. Le parent conserve uniquement l'orchestration du séjour et les flux restant à extraire (consentements et bloc/CRO).
