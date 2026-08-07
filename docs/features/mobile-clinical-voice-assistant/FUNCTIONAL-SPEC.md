# Spécification Fonctionnelle — Assistant Vocal Clinique & Extraction Intelligente (MOB-2816)

## 1. Contexte & Objectifs

Pour accélérer la prise en charge clinique et libérer du temps médical au chevet du patient, l'application mobile Joprelys Connect intègre un assistant vocal et textuel intelligent (`ClinicalVoiceAssistant`).
Le praticien peut dicter ou enregistrer une phrase médicale naturelle (ex: *"Température 38.5, tension 120 sur 80, pouls 75, patient fiévreux avec douleurs thoraciques"*), et l'assistant extrait automatiquement :
- Les 10 constantes vitales numériques (Température, Tension systolique/diastolique, Pouls, SpO2, Glycémie, Poids, Taille, Fréquence respiratoire, Douleur).
- La note clinique structurée SOAP (Subjectif, Objectif, Évaluation, Plan).
- Les prescriptions explicitement dictées et les examens explicitement demandés, conservés comme données structurées distinctes du texte SOAP.

## 2. Parcours Utilisateur

1. Depuis la modale des Constantes (`PatientVitalsSheet`) ou des Notes (`ConsultationNotesSheet`), le praticien clique sur l'icône de l'assistant vocal `[🎙️ Assistant vocal]`.
2. Le praticien s'exprime vocalement ou saisit sa dictée brute.
3. L'assistant traite la dictée et pré-remplit les champs de constantes, de notes SOAP et les éléments structurés explicitement présents dans la conversation.
4. La reconstruction finale convertit le dialogue en note clinique déclarative : les questions et marqueurs conversationnels sont retirés lorsqu'ils servent seulement de contexte à une réponse explicite.
5. Le praticien relit et accepte le résultat IA. Cette acceptation ne persiste encore aucune prescription ni demande d'examen.
6. Le praticien clique ensuite sur l'action explicite d'enregistrement de la consultation. La note SOAP est enregistrée, puis les prescriptions et examens acceptés sont transmis aux ressources métier dédiées.

## 3. Critères d'Acceptation

- [x] Extraction automatique des constantes numériques (°C, mmHg, bpm, kg, cm, %, g/L, c/min, EVA).
- [x] Parsing des structures de texte en note SOAP (Subjectif / Objectif / Diagnostic / Plan).
- [x] Prise en charge i18n Français (`fr`) et Anglais (`en`).
- [x] Support dynamique des thèmes Clair et Sombre.
- [x] Détection d'erreurs et possibilité de correction manuelle avant enregistrement.
- [ ] Une synthèse finale ne conserve pas les questions du médecin comme phrases de la note lorsqu'elles sont seulement le contexte d'une réponse explicite.
- [ ] Une question sans réponse n'est jamais transformée en fait clinique.
- [ ] Une réponse négative associée à une question explicite peut devenir une assertion négative, sans modifier la portée de la négation.
- [ ] La reformulation retire les hésitations, répétitions et échafaudages conversationnels tout en préservant exactement les faits, nombres, unités, médicaments, doses, durées, côtés et niveaux de certitude.
- [ ] Les prescriptions et examens acceptés restent en attente jusqu'au clic explicite d'enregistrement de la consultation.
- [ ] Après enregistrement, l'ordonnance est persistée via la ressource prescription de la consultation et les examens via la ressource de demandes d'examens.
- [ ] Une réouverture du dossier retrouve les ressources structurées enregistrées.

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
- La prévisualisation IA ne crée jamais d'ordonnance ni de demande d'examen.
- La note SOAP n'est pas utilisée comme substitut à la persistance structurée des prescriptions et examens.

## 5. Règles de reformulation clinique sûre

La finalisation distingue transcription et note clinique. Le transcript durable reste la preuve source ; la note finale est une représentation professionnelle de cette source.

- Les questions du médecin peuvent fournir le sujet grammatical d'une réponse courte immédiatement liée, par exemple `Avez-vous des frissons ? — Non` → `Pas de frissons.`
- Une question composée n'autorise que les faits réellement répondus. Une absence de réponse sur la perte de poids ne devient jamais `pas de perte de poids`.
- Les formulations telles que `Vous êtes essoufflé ? — Oui, quand je monte les escaliers` peuvent devenir une phrase déclarative conservant les mots cliniques source, sans introduire un diagnostic ou un synonyme médical absent.
- Les répétitions (`la saturation est correcte` dit deux fois), salutations, instructions d'installation et autres éléments non cliniques sont retirés de la note.
- Les champs structurés `prescription`, `labOrders` et `vitals` ne sont jamais « embellis » au risque de modifier une dose, une unité ou un examen.

## 6. Critères de recette P0

- [ ] Dicter une phrase de 45 secondes avec deux silences de plus de quatre secondes : aucun passage dupliqué.
- [ ] Provoquer trois reprises successives : le transcript reste ordonné et complet.
- [ ] Fermer/réouvrir avant analyse : le brouillon non finalisé revient une fois.
- [ ] Analyser puis fermer/réouvrir : aucun ancien transcript n'est restauré.
- [ ] Tout supprimer puis fermer/réouvrir : l'écran reste vide.
- [ ] Répéter volontairement une information clinique après un nouveau passage : la répétition volontaire reste visible.
- [ ] Sur un dialogue médecin/patient d'au moins deux minutes, la note finale est déclarative et plus concise que le transcript tout en conservant tous les faits explicitement répondus.
- [ ] Extraire au moins une prescription et plusieurs examens, accepter le résultat puis enregistrer : les ressources apparaissent ensuite dans leurs modules dédiés.
