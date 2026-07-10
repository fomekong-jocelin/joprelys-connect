# BUG-20260710-PATIENT-MEDICAL-ICONS-I18N — Icônes et libellés du dossier médical incohérents

## Mode

Engineering frontend + diagnostic backend.

**Statut :** QA — code, tests Angular et build terminés ; QA visuelle manuelle restante.

## Problème

La fiche médicale patient mélange le design system SVG de Joprelys Connect avec des emojis natifs dans les sections Allergies, Antécédents, Vaccinations et Urgences. Les clés i18n de l'historique des urgences sont absentes, ce qui affiche les identifiants de traduction à l'écran.

Lors de l'ouverture d'une consultation encore vierge, `GET /api/visits/{id}/consultation` renvoie un `404` visible dans DevTools.

## Diagnostic du 404

- La route frontend correspond bien au contrôleur Spring Boot.
- La visite existe et l'accès est autorisé.
- `ConsultationService.getDetailedConsultationByVisitId` renvoie volontairement `404` lorsqu'aucune consultation n'a encore été persistée pour la visite.
- `ConsultationComponent` traite déjà ce cas comme un état initial normal et conserve le formulaire vierge.
- Le contrat `404` est documenté et couvert par `givenNoConsultation_whenGetConsultation_thenNotFound` ; aucun changement de statut HTTP n'est autorisé dans ce ticket de diagnostic.

## Critères d'acceptation

- [x] Les en-têtes médicaux utilisent exclusivement `app-ui-icon`, sans emoji décoratif.
- [x] Les statuts et actions de l'historique d'urgence utilisent les icônes SVG partagées.
- [x] Aucun identifiant i18n brut n'est visible en français ou en anglais.
- [x] Les textes Urgences, mode d'arrivée, constantes et actions de réanimation sont traduits en FR/EN.
- [x] Les thèmes light/dark et les rayons maximum de 8 px sont préservés.
- [x] Le contrat backend de consultation reste inchangé et son `404` est expliqué par le code et le test existant ; l'exécution MockMvc est bloquée avant le contrôleur par V55.

## Actions

- [x] Identifier les emojis, clés i18n absentes et usages du composant partagé.
- [x] Tracer le flux Angular → contrôleur → service responsable du `404`.
- [x] Documenter le comportement attendu et la décision de contrat.
- [x] Ajouter les icônes médicales nécessaires au composant partagé.
- [x] Remplacer les emojis et corriger les traductions FR/EN.
- [x] Ajouter les tests Angular de non-régression.
- [x] Exécuter les tests Angular et le build ; tenter le test backend ciblé.
- [x] Mettre à jour le changelog, le suivi et le planning.

## Definition of Ready

- [x] Captures de reproduction disponibles.
- [x] Icônes partagées, dictionnaires i18n et route backend identifiés.
- [x] Périmètre UI et absence de changement API explicités.
- [x] Critères d'acceptation, reviewer et tests attendus définis.

## Definition of Done

- [x] Icônes et traductions corrigées en FR/EN.
- [x] Tests Angular ciblés et globaux réussis.
- [x] Build Angular de production réussi.
- [x] Diagnostic du `404` documenté sans modification silencieuse du contrat.
- [ ] QA visuelle light/dark réalisée sur le profil patient.
- [x] Ticket, documentation, changelog, suivi et planning mis à jour.

## Estimation et responsabilités

| Champ | Valeur |
|---|---|
| Priorité | P1 |
| Story points | 2 |
| Estimation senior | 0,3 j |
| Estimation intermédiaire | 0,5 j |
| Estimation junior | 0,8 j |
| Profil recommandé | Frontend Angular intermédiaire + reviewer backend |
| Reviewer | Lead Frontend + Lead Backend |
| Sprint | SPRINT-0014 |

## Sécurité / Régression

- Aucun mécanisme RBAC, consentement, tenant ou donnée médicale n'est modifié.
- Le `404` de dissimulation d'existence pour les accès non autorisés reste intact.
- Le composant `patient-medical-info.component.ts` dépasse déjà la limite dure de 500 lignes (`712` lignes). Le correctif reste minimal, mais la review finale doit signaler cette dette et planifier son découpage avant une refonte fonctionnelle.

## Impact SemVer

PATCH — correction UI/i18n rétrocompatible, sans changement API.

## Reste à faire

QA visuelle manuelle light/dark sur le profil patient. Corriger V55 dans un ticket séparé avant de relancer les tests backend H2.

## Résultats de vérification

- Test Angular ciblé : `6/6` réussis.
- Suite Angular globale : `123/123` tests réussis dans `26` fichiers.
- Build Angular production : réussi.
- JSON i18n FR/EN : parsing réussi.
- Test backend ciblé `ConsultationControllerTest` : bloqué au démarrage (`11` erreurs de contexte) par la syntaxe PostgreSQL de `V55__billing_amounts_float_to_numeric.sql` incompatible H2 ; aucun test contrôleur n'a été exécuté.
- Preuve statique du diagnostic : `givenNoConsultation_whenGetConsultation_thenNotFound` attend explicitement `404` et le message « Aucune consultation trouvée pour cette visite. ».

## Checklist de review

| Contrôle | Statut | Justification |
|---|---|---|
| Changement minimal | ✅ | UI/i18n uniquement ; aucun contrat backend modifié. |
| Icônes partagées | ✅ | Quatre pictogrammes réutilisables ajoutés à `IconComponent`. |
| Tailwind v4 / Angular Material | ✅ | Aucun Material ni syntaxe Tailwind v3. |
| Thèmes / tokens | ✅ | Couleurs issues de variables CSS sémantiques light/dark. |
| Rayons | ✅ | Les sept `rounded-xl` du périmètre sont ramenés à `rounded-lg` (8 px). |
| i18n | ✅ | Onze clés ajoutées dans les deux dictionnaires ; aucun nouveau texte visible codé en dur. |
| Sécurité | ✅ | RBAC, tenant et validation d'accès inchangés ; `404` de dissimulation conservé. |
| Limites de taille | ❌ | Dette préexistante : `PatientMedicalInfoComponent` reste au-dessus de 500 lignes. Découpage obligatoire avant refonte supplémentaire. |
| Tests frontend | ✅ | 123 tests et build réussis. |
| Tests backend | ⚠️ | Bloqués par V55/H2, sans lien avec ce correctif. |
| `.gitignore` / proxy | ✅ | Présents et conformes ; aucun artefact généré ajouté. |
| SemVer / documentation | ✅ | PATCH, spécifications, ticket, changelog et suivi à jour. |
