# Spécification Fonctionnelle — Assistant Vocal Clinique & Extraction Intelligente (MOB-2815)

## 1. Contexte & Objectifs

Pour accélérer la prise en charge clinique et libérer du temps médical au chevet du patient, l'application mobile Joprelys Connect intègre un assistant vocal et textuel intelligent (`ClinicalVoiceAssistant`).
Le praticien peut dicter ou enregistrer une phrase médicale naturelle (ex: *"Température 38.5, tension 120 sur 80, pouls 75, patient fiévreux avec douleurs thoraciques"*), et l'assistant extrait automatiquement :
- Les 10 constantes vitales numériques (Température, Tension systolique/diastolique, Pouls, SpO2, Glycémie, Poids, Taille, Fréquence respiratoire, Douleur).
- La note clinique structurée SOAP (Subjectif, Objectif, Évaluation, Plan).

## 2. Parcours Utilisateur

1. Depuis la modale des Constantes (`PatientVitalsSheet`) ou des Notes (`ConsultationNotesSheet`), le praticien clique sur l'icône de l'assistant vocal `[🎙️ Assistant vocal]`.
2. Le praticien s'exprime vocalement ou saisit sa dictée brute.
3. L'assistant traite la dictée et pré-remplit les champs de constantes et de notes SOAP en temps réel avec un taux d'erreur nul sur les unités médicales.
4. Le praticien vérifie le pré-remplissage et clique sur "Valider & Enregistrer".

## 3. Critères d'Acceptation

- [x] Extraction automatique des constantes numériques (°C, mmHg, bpm, kg, cm, %, g/L, c/min, EVA).
- [x] Parsing des structures de texte en note SOAP (Subjectif / Objectif / Diagnostic / Plan).
- [x] Prise en charge i18n Français (`fr`) et Anglais (`en`).
- [x] Support dynamique des thèmes Clair et Sombre.
- [x] Détection d'erreurs et possibilité de correction manuelle avant enregistrement.
