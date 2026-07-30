# Spécification Fonctionnelle — Saisie & Consultation des Notes Cliniques Mobile (MOB-2814)

## 1. Contexte & Objectifs

Dans le flux de prise en charge clinique Joprelys Connect, le médecin ou professionnel de santé doit pouvoir consulter et consigner rapidement ses notes de consultation (anamnèse, examen clinique, diagnostic et conduite à tenir / plan de soins) directement depuis l'application mobile Flutter.

## 2. Utilisateurs Cibles

- Médecins généralistes / spécialistes
- Infirmiers d'accueil et d'orientation (IAO)
- Praticiens en mobilité au chevet du patient

## 3. Parcours Utilisateur

1. Depuis la carte du patient sur le Dashboard Mobile (`_ActiveVisitCard`), le praticien clique sur l'action "Notes de consultation" / "Notes cliniques".
2. Une modale (Bottom Sheet) s'ouvre, affichant l'en-tête patient (Nom, Prénom, DPU, Référence visite) et les 4 sections SOAP cliniques :
   - **Subjectif (S)** : Anamnèse / Plaintes exprimées par le patient.
   - **Objectif (O)** : Examen physique / Observations cliniques.
   - **Évaluation (A)** : Hypothèses diagnostiques ou diagnostic retenu.
   - **Plan (P)** : Traitement, examens complémentaires demandés et conduite à tenir.
3. Une zone d'assistant / dictée rapide ou saisie libre permet au praticien de remplir/modifier les notes.
4. À la sauvegarde :
   - Validation locale et envoi au backend via `POST /api/visits/{id}/consultation-notes`.
   - Message de confirmation (Toast/SnackBar).
   - Rafraîchissement automatique de la file active.

## 4. Critères d'Acceptation

- [x] L'interface prend en charge le français (`fr`) et l'anglais (`en`).
- [x] Support dynamique des thèmes Sombre (`dark`) et Clair (`light`).
- [x] Structure SOAP claire et intuitive adaptée au mobile.
- [x] Persistance et récupération via l'API REST `GET/POST /api/visits/{id}/consultation-notes`.
- [x] Conformité aux règles d'arrondis sobres (4-8px) et design system Joprelys.
