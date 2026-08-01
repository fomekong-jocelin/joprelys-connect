# Spécification Fonctionnelle — Saisie & Consultation des Notes Cliniques Mobile (MOB-2814)

## 1. Contexte & Objectifs

Dans le flux de prise en charge clinique Joprelys Connect, le médecin ou professionnel de santé doit pouvoir consulter et consigner rapidement ses notes de consultation (anamnèse, examen clinique, diagnostic et conduite à tenir / plan de soins) directement depuis l'application mobile Flutter.

## 2. Utilisateurs Cibles

- Médecins généralistes / spécialistes
- Infirmiers d'accueil et d'orientation (IAO)
- Praticiens en mobilité au chevet du patient

## 3. Parcours Utilisateur

1. Depuis la carte du patient sur le Dashboard Mobile (`_ActiveVisitCard`), le praticien clique sur l'action "Notes de consultation" / "Notes cliniques".
2. Une modale (Bottom Sheet) s'ouvre, affichant l'en-tête patient (Nom, Prénom, DPU, Référence visite) et les mêmes 4 sections SOAP que l'interface Angular :
   - **Subjectif (S)** : histoire de la maladie, plainte et symptômes (`symptoms`).
   - **Objectif (O)** : examen clinique (`clinicalExam`) ; les constantes restent structurées et associées à la visite.
   - **Évaluation (A)** : diagnostic documenté par le praticien (`diagnosis`), obligatoire. Aucun statut artificiel « hypothèse », « principal » ou « final » n'est demandé tant que le produit ne gère pas ce cycle clinique.
   - **Plan (P)** : synthèse (`conclusion`), consignes (`advice`) et suivi (`followUp`) ; prescriptions et examens restent des ressources dédiées.
3. Une zone d'assistant / dictée rapide ou saisie libre permet au praticien de remplir/modifier les notes.
4. À la sauvegarde :
   - Validation UX puis validation métier backend via `POST /api/visits/{id}/consultation`.
   - Message de confirmation (Toast/SnackBar).
   - Rafraîchissement automatique de la file active.

## 4. Critères d'Acceptation

- [x] L'interface prend en charge le français (`fr`) et l'anglais (`en`).
- [x] Support dynamique des thèmes Sombre (`dark`) et Clair (`light`).
- [x] Structure SOAP claire et intuitive adaptée au mobile.
- [x] Parité implémentée des sections, sous-champs et validations UX avec Angular.
- [x] Un seul champ Diagnostic est exposé dans la section A sur les trois couches.
- [x] Persistance et récupération raccordées à l'API REST canonique `GET/POST /api/visits/{id}/consultation`.
- [ ] Validation runtime Flutter et recette cross-stack sur le même HEAD.
- [x] Conformité aux règles d'arrondis sobres (4-8px) et design system Joprelys.
