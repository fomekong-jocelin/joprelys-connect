# BUG-20260807-MOBILE-VOICE-PERSISTENCE-REFORMULATION

## 1. Objectif

Corriger la finalisation de l'assistant vocal mobile après la stabilisation du streaming : conserver et enregistrer les prescriptions/examens explicitement validés par le praticien, et transformer le dialogue brut en une vraie note clinique professionnelle sans enrichissement médical.

Issue GitHub : #278. PR : #279.

## 2. Symptômes reproduits

- La transcription longue est nettement plus stable mais peut encore perdre environ 10 % des mots selon la dictée Android réelle.
- Les champs `prescription` et `labOrders` sont extraits et affichés dans la synthèse IA.
- Lors du retour vers la note de consultation, ces deux champs étaient abandonnés par les contrôleurs de formulaire et n'étaient jamais persistés.
- La reconstruction finale conservait les questions du médecin et les réponses du patient presque mot pour mot ; elle ne produisait donc pas une synthèse clinique réellement reformulée.

## 3. Diagnostic

- `ConsultationNoteFormControllers.applyAcceptedDraft()` et `toConsultationNote()` ne transportaient que les six champs SOAP.
- `ConsultationNotesSheet` avertissait explicitement que l'ordonnance et les examens étaient « à saisir séparément » au lieu d'orchestrer leur sauvegarde après l'action explicite Enregistrer.
- Le contrat SOAP impose à juste titre que prescription et examens restent dans leurs ressources structurées dédiées.
- `AiClinicalCapturePrompt` limitait la reformulation à des corrections grammaticales minimales et interdisait presque tout nouveau token ; le modèle privilégiait donc la copie du dialogue.
- Le DTO prescription imposait un dosage non vide dès le brouillon, ce qui empêchait de conserver fidèlement une prescription partielle telle que « inhalateur » sans inventer de dose.

## 4. Règles métier / sécurité

- Aucune prescription ni demande d'examen n'est persistée automatiquement pendant l'écoute ou la prévisualisation.
- Le praticien doit d'abord accepter le résultat IA puis déclencher explicitement l'enregistrement de la consultation.
- Le backend reste maître de la validation, des permissions et de la persistance des ressources structurées.
- Les questions sans réponse ne deviennent jamais des faits cliniques.
- Une réponse négative peut être reformulée en assertion négative uniquement lorsque son objet est explicitement présent dans la question immédiatement associée.
- La reformulation peut supprimer les marqueurs conversationnels, répétitions et questions, mais ne peut ajouter aucun diagnostic, médicament, dose, unité, négation, temporalité, latéralité ou degré de certitude absent de la source.
- Une prescription incomplète peut être conservée comme brouillon DRAFT ; sa finalisation ACTIVE est refusée tant qu'un dosage requis manque.
- Le working set vocal n'est consommé qu'après réussite de la note SOAP et des ressources structurées acceptées, afin qu'une panne partielle reste récupérable.

## 5. Plan d'action

- [x] Analyser le pipeline capture → rebuild → formulaire → sauvegarde.
- [x] Vérifier les contrats SOAP, prescription et examens existants.
- [x] Documenter la cible fonctionnelle et technique.
- [x] Conserver les éléments structurés acceptés dans l'état du formulaire jusqu'à l'enregistrement.
- [x] Ajouter une orchestration mobile dédiée de persistance SOAP + ordonnance + examens, sans logique métier clinique côté Flutter.
- [x] Utiliser l'endpoint prescription upsert existant après obtention de l'identifiant de consultation.
- [x] Créer les demandes d'examens via le contrat dédié, sans inférer une spécialité non dictée ; utilisation du type neutre `AUTRE` lorsque l'extraction ne fournit pas de type sûr.
- [x] Renforcer le prompt de reconstruction pour convertir questions/réponses explicites en assertions déclaratives sûres et supprimer le bavardage conversationnel.
- [x] Conserver le garde de factualité existant et ajouter des tests anti-enrichissement ; aucun assouplissement médical n'a été nécessaire.
- [x] Autoriser un dosage vide dans un brouillon d'ordonnance tout en bloquant sa finalisation tant que le dosage manque.
- [x] Porter à deux les retries bornés d'une fenêtre de transcription distante pour réduire les pertes transitoires sans modifier le VAD ni le recouvrement déjà stabilisés.
- [x] Ajouter les tests Flutter et Spring ciblés.
- [x] Exécuter format, `flutter analyze`, `flutter test` et `./mvnw clean verify` via CI : run #2668 vert sur `af62319f2d6af1bdda15c435f4e86d8c5639b8b7` avant clôture documentaire.
- [ ] Mettre à jour changelog et suivi projet, puis exécuter le gate final exact-HEAD.
- [ ] Recette Android réelle avec dictée longue, reformulation, ordonnance et examens.

## 6. Critères d'acceptation

### Validés automatiquement

- [x] Une ordonnance extraite puis acceptée est envoyée à l'upsert ordonnance après clic explicite sur Enregistrer.
- [x] Les examens extraits puis acceptés sont envoyés comme demande d'examens structurée après le même clic explicite.
- [x] Un retry après réussite connue de l'ordonnance ne renvoie pas cette ordonnance dans la même session UI si la demande d'examens a échoué ensuite.
- [x] La note consultation n'embarque jamais `prescriptions`/`labOrders` dans le contrat SOAP canonique.
- [x] Une prescription sans dosage explicite est conservée comme DRAFT avec dosage vide ; aucune dose n'est inventée et la finalisation ACTIVE est bloquée.
- [x] Le prompt exige que les questions du médecin disparaissent de la note finale lorsqu'elles servent seulement de contexte à une réponse explicite.
- [x] Les tests de factualité vérifient la préservation de la négation et le rejet d'un enrichissement médical absent, par exemple l'introduction de « bronchite » à partir d'une simple toux.
- [x] Une question non répondue est explicitement interdite comme source d'un fait par le contrat de reconstruction.
- [x] La CI mobile/backend est verte sur le HEAD fonctionnel `af62319f2d6af1bdda15c435f4e86d8c5639b8b7` (run #2668).

### À confirmer sur appareil / recette clinique

- [ ] Une réouverture réelle du dossier retrouve l'ordonnance et les examens créés dans leurs modules dédiés.
- [ ] La reformulation du dialogue d'exemple produit visiblement une note plus concise et déclarative sans perte de fait clinique.
- [ ] Le taux de mots perdus sur une dictée Android longue est mesuré après le passage à deux retries par fenêtre ; aucune promesse de suppression totale des ~10 % n'est faite avant cette mesure.

## 7. Impact

- Stack : Flutter + Spring Boot IA + APIs consultation/prescription/examens.
- DB : aucune migration ; réutilisation des ressources existantes.
- API : changement rétrocompatible de validation du brouillon prescription (`dosage` non-null mais vide autorisé) ; finalisation renforcée.
- Sécurité : permissions existantes `CLINICAL_WRITE` et `LAB_ORDER_CREATE` conservées ; validation explicite du praticien conservée.
- SemVer : PATCH rétrocompatible.

## 8. Estimation / responsabilité

- Priorité : P0 clinique.
- Estimation senior initiale : 1 à 1,5 jour avec tests et recette.
- Profil : Senior Flutter + Senior Spring/IA, revue clinique.
- Risque résiduel : faible côté compilation/tests automatisés, moyen jusqu'à la recette Android réelle de reformulation et persistance end-to-end.

## 9. Reste à faire

- Mettre à jour `CHANGELOG.md` et `PROJECT-TRACKING.md` puis obtenir un gate final exact-HEAD vert.
- Fusionner la PR #279 après ce gate.
- Effectuer la recette Android réelle décrite dans `docs/features/mobile-clinical-voice-assistant/TEST-PLAN.md`, notamment réouverture du dossier et mesure du taux de mots manquants.
