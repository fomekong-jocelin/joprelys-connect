# BUG-20260807-MOBILE-VOICE-PERSISTENCE-REFORMULATION

## 1. Objectif

Corriger la finalisation de l'assistant vocal mobile après la stabilisation du streaming : conserver et enregistrer les prescriptions/examens explicitement validés par le praticien, et transformer le dialogue brut en une vraie note clinique professionnelle sans enrichissement médical.

Issue GitHub : #278.

## 2. Symptômes reproduits

- La transcription longue est nettement plus stable mais peut encore perdre environ 10 % des mots selon la dictée Android réelle.
- Les champs `prescription` et `labOrders` sont extraits et affichés dans la synthèse IA.
- Lors du retour vers la note de consultation, ces deux champs sont abandonnés par les contrôleurs de formulaire et ne sont jamais persistés.
- La reconstruction finale conserve les questions du médecin et les réponses du patient presque mot pour mot ; elle ne produit donc pas une synthèse clinique réellement reformulée.

## 3. Diagnostic

- `ConsultationNoteFormControllers.applyAcceptedDraft()` et `toConsultationNote()` ne transportent que les six champs SOAP.
- `ConsultationNotesSheet` avertit explicitement que l'ordonnance et les examens sont « à saisir séparément » au lieu d'orchestrer leur sauvegarde après l'action explicite Enregistrer.
- Le contrat SOAP impose à juste titre que prescription et examens restent dans leurs ressources structurées dédiées.
- `AiClinicalCapturePrompt` limite la reformulation à des corrections grammaticales minimales et interdit presque tout nouveau token ; le modèle privilégie donc la copie du dialogue.

## 4. Règles métier / sécurité

- Aucune prescription ni demande d'examen n'est persistée automatiquement pendant l'écoute ou la prévisualisation.
- Le praticien doit d'abord accepter le résultat IA puis déclencher explicitement l'enregistrement de la consultation.
- Le backend reste maître de la validation, des permissions et de la persistance des ressources structurées.
- Les questions sans réponse ne deviennent jamais des faits cliniques.
- Une réponse négative peut être reformulée en assertion négative uniquement lorsque son objet est explicitement présent dans la question immédiatement associée.
- La reformulation peut supprimer les marqueurs conversationnels, répétitions et questions, mais ne peut ajouter aucun diagnostic, médicament, dose, unité, négation, temporalité, latéralité ou degré de certitude absent de la source.

## 5. Plan d'action

- [x] Analyser le pipeline capture → rebuild → formulaire → sauvegarde.
- [x] Vérifier les contrats SOAP, prescription et examens existants.
- [x] Documenter la cible fonctionnelle et technique.
- [ ] Conserver les éléments structurés acceptés dans l'état du formulaire jusqu'à l'enregistrement.
- [ ] Ajouter une orchestration mobile dédiée de persistance SOAP + ordonnance + examens, sans logique métier clinique côté Flutter.
- [ ] Utiliser l'endpoint prescription upsert existant après obtention de l'identifiant de consultation.
- [ ] Créer les demandes d'examens via le contrat dédié, sans inférer une spécialité non dictée ; utiliser le type neutre `AUTRE` lorsque l'extraction ne fournit pas de type sûr.
- [ ] Renforcer le prompt de reconstruction pour convertir questions/réponses explicites en assertions déclaratives sûres et supprimer le bavardage conversationnel.
- [ ] Ajuster le garde de factualité seulement si nécessaire, avec tests anti-enrichissement.
- [ ] Ajouter les tests Flutter et Spring ciblés.
- [ ] Exécuter `flutter analyze`, `flutter test` et `./mvnw clean verify` via CI.
- [ ] Mettre à jour changelog et suivi projet.
- [ ] Recette Android réelle avec dictée longue, reformulation, ordonnance et examens.

## 6. Critères d'acceptation

- [ ] Une ordonnance extraite puis acceptée est enregistrée avec la consultation après clic explicite sur Enregistrer.
- [ ] Les examens extraits puis acceptés sont créés comme demandes d'examens structurées après le même clic explicite.
- [ ] Une réouverture du dossier retrouve ces ressources via leurs APIs dédiées.
- [ ] Un retry de sauvegarde d'ordonnance met à jour l'ordonnance de consultation au lieu de créer un doublon.
- [ ] Les questions du médecin disparaissent de la note finale lorsqu'elles servent seulement de contexte à une réponse explicite.
- [ ] Les réponses restent factuellement identiques : négation, durée, nombre, unité, côté et certitude sont préservés.
- [ ] Aucune question non répondue n'est convertie en fait.
- [ ] La CI mobile/backend est verte sur le HEAD final.

## 7. Impact

- Stack : Flutter + Spring Boot IA + APIs consultation/prescription/examens.
- DB : aucune migration attendue ; réutilisation des ressources existantes.
- API : aucun changement cassant attendu ; réutilisation des endpoints existants.
- Sécurité : permissions existantes `CLINICAL_WRITE` et `LAB_ORDER_CREATE` conservées.
- SemVer : PATCH rétrocompatible.

## 8. Estimation / responsabilité

- Priorité : P0 clinique.
- Estimation senior : 1 à 1,5 jour avec tests et recette.
- Profil : Senior Flutter + Senior Spring/IA, revue clinique.
- Risque : moyen jusqu'à validation de la non-duplication des ressources structurées.

## 9. Reste à faire

Voir la checklist du plan d'action ; le ticket ne sera DONE qu'après CI verte et recette Android réelle.