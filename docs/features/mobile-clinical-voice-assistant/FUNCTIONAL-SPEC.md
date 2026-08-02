# Spécification Fonctionnelle — Assistant Vocal Clinique & Extraction Intelligente (MOB-2816)

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

## 4. Invariants de capture et de reprise

- Une parole reconnue ne doit apparaître qu'une fois, y compris après trois ou
  quatre callbacks identiques ou une reprise automatique du moteur Android.
- Une reprise peut rejouer la fin de la fenêtre précédente : le chevauchement
  récent est fusionné sans supprimer les nouveaux mots.
- Un brouillon local ou serveur n'est restauré que s'il attend réellement la
  revue du praticien (`PENDING_REVIEW`). Un transcript `ANALYZED` ne redevient
  jamais une nouvelle dictée.
- « Tout supprimer » est durable pour la visite courante et reste effectif après
  fermeture puis réouverture de la modale.
- Une erreur terminale arrête visuellement et techniquement l'écoute. L'analyse
  reste indisponible tant que le transcript n'est pas sauvegardé et relu.
- L'application aux constantes et à la note reste explicite et ne remplace pas
  les données saisies par le praticien sans confirmation.

## 5. Critères de recette P0

- [ ] Dicter une phrase de 45 secondes avec deux silences de plus de quatre secondes : aucun passage dupliqué.
- [ ] Provoquer trois reprises successives : le transcript reste ordonné et complet.
- [ ] Fermer/réouvrir avant analyse : le brouillon non finalisé revient une fois.
- [ ] Analyser puis fermer/réouvrir : aucun ancien transcript n'est restauré.
- [ ] Tout supprimer puis fermer/réouvrir : l'écran reste vide.
- [ ] Répéter volontairement une information clinique après un nouveau passage : la répétition volontaire reste visible.
