# Spécification Fonctionnelle — Assistant IA Vocal pour Consultations Médicales

**EPIC** : EPIC-0024  
**Version** : 1.0  
**Date** : 2026-07-17  
**Auteur** : Équipe Joprelys Connect  
**Statut** : Cadrage révisé — activation production bloquée par validation médecin/DPO  
**Version cible** : 0.11.0

---

## 1. Vue d'ensemble

> [!IMPORTANT]
> L'assistant est une aide à la saisie. Il produit un brouillon, ne pose pas de
> diagnostic autonome, ne prescrit pas et ne sauvegarde aucune donnée clinique.
> Le médecin relit, corrige puis valide explicitement via le formulaire existant.
> Le mode manuel reste disponible à tout moment.

### 1.1 Résumé

L'assistant IA vocal pour consultations médicales permet à un médecin de remplir le formulaire de consultation de manière conversationnelle grâce à la voix. Le médecin scanne un QR code associé à une visite patient, ce qui ouvre un panneau d'assistant vocal. Il décrit oralement ses observations cliniques et l'IA extrait automatiquement les informations structurées (symptômes, examen clinique, diagnostic, etc.) pour pré-remplir le formulaire de consultation.

### 1.2 Problème adressé

Actuellement, le médecin doit saisir manuellement l'ensemble des champs du formulaire de consultation (symptômes, examen clinique, diagnostic suspecté, diagnostic, diagnostic final, conclusion, conseils, suivi). Cette saisie :

- **Ralentit la consultation** : le médecin alterne entre l'examen du patient et la saisie clavier.
- **Réduit la qualité du contenu** : par manque de temps, les champs optionnels sont souvent laissés vides.
- **Augmente la charge cognitive** : le médecin doit structurer mentalement ses observations dans les bons champs.
- **Limite la mobilité** : le médecin doit rester devant un écran pour saisir les données.

### 1.3 Solution proposée

Un assistant conversationnel alimenté par l'IA qui :

1. Se déclenche par scan d'un QR code de visite.
2. Écoute la dictée vocale du médecin via le microphone du smartphone ou de l'ordinateur.
3. Transcrit l'audio en texte (Speech-to-Text).
4. Analyse le texte pour extraire les informations médicales structurées.
5. Pré-remplit les champs du formulaire de consultation avec les données extraites.
6. Pose des questions de clarification si des informations manquent ou sont ambiguës.

### 1.4 Valeur métier

| Indicateur | Avant | Après (cible) |
|---|---|---|
| Temps moyen de saisie d'une consultation | ~8 min | ~3 min |
| Taux de remplissage des champs optionnels | ~30% | ~80% |
| Satisfaction médecin (NPS saisie) | Faible | Élevé |
| Erreurs de saisie | Fréquentes | Réduites |

---

## 2. Personas

### 2.1 Persona principale : Médecin (Dr. Kamga)

| Attribut | Détail |
|---|---|
| **Rôle** | Médecin généraliste / Spécialiste |
| **Contexte** | Consulte 20 à 40 patients par jour dans une clinique partenaire |
| **Objectif** | Remplir rapidement et complètement le formulaire de consultation |
| **Frustrations** | Saisie clavier lente, champs optionnels ignorés par manque de temps |
| **Compétences techniques** | Utilisation quotidienne du smartphone, à l'aise avec la dictée vocale |
| **Accès** | Rôle `DOCTOR` dans Joprelys Connect |
| **Appareils** | Smartphone (scan QR), tablette ou PC de bureau (consultation) |

### 2.2 Persona secondaire : Administrateur clinique (Mme Nguemo)

| Attribut | Détail |
|---|---|
| **Rôle** | Administratrice de la clinique |
| **Contexte** | Configure et supervise les paramètres de l'application |
| **Objectif** | Activer/désactiver la fonctionnalité IA, surveiller les coûts d'utilisation |
| **Compétences techniques** | Avancées, gestion des paramètres système |
| **Accès** | Rôle `ADMIN` dans Joprelys Connect |

---

## 3. Scénarios utilisateur

### 3.1 Scénario principal — Consultation vocale complète

**Acteur** : Dr. Kamga (Médecin)  
**Pré-conditions** :
- Le médecin est authentifié dans Joprelys Connect.
- Une visite patient existe avec le statut `IN_CONSULTATION`.
- Le médecin dispose d'un appareil avec caméra et microphone.

**Flux principal** :

| Étape | Action | Résultat attendu |
|---|---|---|
| 1 | Le médecin ouvre le scanner QR depuis l'application web. | La caméra s'active et affiche un cadre de scan. |
| 2 | Le médecin scanne le QR code imprimé sur la fiche d'admission du patient. | L'application décode l'identifiant de visite contenu dans le QR code. |
| 3 | L'application valide la visite et ouvre le panneau d'assistant vocal. | Le panneau affiche le nom du patient, le numéro de visite et un message de bienvenue. |
| 4 | Une session IA est automatiquement créée côté serveur. | L'API `/start` retourne un `sessionId` et les métadonnées de la visite. |
| 5 | Le médecin appuie sur le bouton microphone et dicte : *« Le patient présente une toux sèche depuis 3 jours avec fièvre modérée à 38.2°C. À l'examen, les poumons sont clairs, pas de râles. Je suspecte une rhinopharyngite virale. »* | L'enregistrement audio est capturé via `MediaRecorder`. |
| 6 | Le médecin relâche le bouton microphone. | L'audio est envoyé à l'API `/message` en `multipart/form-data`. |
| 7 | L'IA transcrit l'audio, analyse le contenu et extrait les champs. | La réponse contient : `symptoms` = « Toux sèche depuis 3 jours, fièvre modérée à 38.2°C », `clinicalExam` = « Poumons clairs, absence de râles », `suspectedDiagnosis` = « Rhinopharyngite virale ». |
| 8 | Le panneau affiche les champs extraits. En Realtime uniquement, il peut restituer une question clinique par le canal TTS unique. | L'assistant peut demander : *« Souhaitez-vous préciser le diagnostic définitif ? »*. En Dictée, le message reste visuel. |
| 9 | Le médecin dicte à nouveau : *« Le diagnostic est une rhinopharyngite aiguë. Conseils : repos, hydratation abondante, paracétamol si fièvre supérieure à 38.5°C. Suivi dans 5 jours si pas d'amélioration. »* | Nouvel envoi audio à l'API `/message`. |
| 10 | L'IA complète les champs manquants. | `diagnosis` = « Rhinopharyngite aiguë », `advice` = « Repos, hydratation abondante, paracétamol si fièvre > 38.5°C », `followUp` = « Contrôle dans 5 jours si pas d'amélioration ». |
| 11 | Le médecin valide les champs pré-remplis dans le formulaire de consultation. | Le formulaire est pré-rempli avec toutes les données extraites. Le médecin peut modifier avant de soumettre. |
| 12 | Le médecin clique sur « Enregistrer la consultation ». | L'API `POST /api/visits/{id}/consultation` est appelée avec les données validées. La session IA est automatiquement terminée. |

**Post-conditions** :
- La consultation est enregistrée avec tous les champs remplis.
- La session IA est supprimée du serveur.
- Aucun audio n'est persisté.

### 3.2 Scénario alternatif — Saisie textuelle

**Acteur** : Dr. Kamga  
**Contexte** : Le médecin se trouve dans un environnement bruyant et préfère taper son message.

| Étape | Action | Résultat attendu |
|---|---|---|
| 1–3 | Identiques au scénario 3.1. | Panneau d'assistant vocal ouvert. |
| 4 | Le médecin tape dans le champ texte : *« Symptômes : céphalées frontales depuis 2 jours, nausées matinales. Diagnostic : migraine sans aura. »* | Le texte est envoyé à l'API `/message` en `application/json`. |
| 5 | L'IA extrait les champs. | `symptoms` = « Céphalées frontales depuis 2 jours, nausées matinales », `diagnosis` = « Migraine sans aura ». |
| 6 | L'IA pose une question de clarification. | *« Avez-vous réalisé un examen clinique ? Souhaitez-vous ajouter des conseils ou un suivi ? »* |

### 3.3 Scénario alternatif — Clarification requise

**Acteur** : Dr. Kamga  
**Contexte** : Le médecin donne une description vague.

| Étape | Action | Résultat attendu |
|---|---|---|
| 1–3 | Identiques au scénario 3.1. | Panneau d'assistant vocal ouvert. |
| 4 | Le médecin dicte : *« Le patient ne se sent pas bien, il a mal partout. »* | Audio envoyé à l'API. |
| 5 | L'IA détecte que les informations sont insuffisantes. | `needsClarification` = `true`, `clarificationQuestion` = *« Pouvez-vous préciser la localisation de la douleur, sa durée et son intensité ? Le patient présente-t-il de la fièvre ou d'autres symptômes associés ? »* |
| 6 | Le médecin répond avec plus de détails. | L'IA complète les champs avec les nouvelles informations. |

### 3.4 Scénario alternatif — Navigation directe sans QR code

**Acteur** : Dr. Kamga  
**Contexte** : Le médecin est déjà sur la page de détail de la visite.

| Étape | Action | Résultat attendu |
|---|---|---|
| 1 | Le médecin clique sur le bouton « Assistant IA » dans la page de consultation. | Le panneau d'assistant vocal s'ouvre, pré-chargé avec le contexte de la visite en cours. |
| 2–12 | Identiques aux étapes 4–12 du scénario 3.1. | Même flux conversationnel. |

### 3.5 Scénario d'erreur — Provider IA indisponible

**Acteur** : Dr. Kamga  
**Contexte** : Le service IA tiers est temporairement indisponible.

| Étape | Action | Résultat attendu |
|---|---|---|
| 1–3 | Identiques au scénario 3.1. | Panneau d'assistant vocal ouvert. |
| 4 | L'appel à l'API `/start` échoue (503). | Le panneau affiche un message : *« Le service d'assistant vocal est temporairement indisponible. Veuillez remplir le formulaire manuellement ou réessayer dans quelques instants. »* |
| 5 | Le médecin peut remplir le formulaire manuellement. | Le formulaire de consultation reste accessible et fonctionnel. |

### 3.6 Scénario d'administration — Configuration du provider

**Acteur** : Mme Nguemo (Admin)  
**Contexte** : L'administratrice souhaite changer le provider IA.

> [!NOTE]
> La configuration du provider IA se fait via les variables d'environnement ou le fichier `application.yml`. Il n'y a pas d'interface graphique de configuration dans cette version. L'administratrice doit modifier la configuration et redémarrer le service.

---

## 4. Exigences fonctionnelles

### 4.1 Scan QR Code

| ID | Exigence | Priorité |
|---|---|---|
| **FR-01** | Le système doit permettre au médecin de scanner un QR code contenant l'identifiant de visite depuis le navigateur web (smartphone ou PC avec webcam). | P0 |
| **FR-02** | Le système doit valider que la visite référencée par le QR code existe, appartient au tenant du médecin et est au statut applicatif `EN_COURS`. | P0 |
| **FR-03** | Le système doit afficher un message d'erreur explicite si le QR code est invalide, expiré ou si la visite n'est pas au bon statut. | P0 |
| **FR-04** | Le scanner QR doit fonctionner sur les navigateurs modernes (Chrome, Safari, Firefox) sur mobile et desktop. | P1 |

### 4.2 Session IA

| ID | Exigence | Priorité |
|---|---|---|
| **FR-05** | Le système doit créer une session IA associée à une visite lors du démarrage de l'assistant. | P0 |
| **FR-06** | La session IA doit avoir un TTL (Time-To-Live) de 30 minutes. Passé ce délai, la session expire automatiquement. | P0 |
| **FR-07** | Le système ne doit permettre qu'une seule session active par visite. Si une session existe déjà, elle doit être retournée. | P1 |
| **FR-08** | Le système doit permettre la fermeture explicite d'une session via `DELETE /api/ai/consultation/{visitId}/session`. | P1 |

### 4.3 Enregistrement et envoi audio

| ID | Exigence | Priorité |
|---|---|---|
| **FR-09** | Le système doit capturer l'audio du microphone via l'API `MediaRecorder` du navigateur. | P0 |
| **FR-10** | L'audio doit être encodé au format `audio/webm;codecs=opus` (navigateurs Chromium) ou `audio/mp4` (Safari) avant envoi. | P0 |
| **FR-11** | La durée maximale d'un enregistrement unique est de 120 secondes. Au-delà, l'enregistrement s'arrête automatiquement. | P1 |
| **FR-12** | Le système doit afficher un indicateur visuel d'enregistrement en cours (icône pulsante, durée écoulée). | P0 |

### 4.4 Traitement IA et extraction de champs

| ID | Exigence | Priorité |
|---|---|---|
| **FR-13** | Le système doit transcrire l'audio en texte (Speech-to-Text) puis analyser le texte pour en extraire les champs de consultation structurés. | P0 |
| **FR-14** | Les champs extraits doivent correspondre au modèle `SaveConsultationRequest` : `symptoms`, `clinicalExam`, `suspectedDiagnosis`, `diagnosis`, `finalDiagnosis`, `conclusion`, `advice`, `followUp`. | P0 |
| **FR-15** | L'IA doit accumuler les informations au fil de la conversation. Chaque nouveau message enrichit ou affine les champs déjà extraits sans les écraser sauf correction explicite du médecin. | P0 |
| **FR-16** | Si les champs obligatoires (`symptoms`, `diagnosis`) ne sont pas encore remplis, l'IA doit poser une question de clarification ciblée. | P1 |
| **FR-17** | L'IA doit supporter le français comme langue principale de transcription et d'analyse. | P0 |
| **FR-18** | L'IA doit reformuler les propos du médecin en terminologie médicale professionnelle et structurée. | P1 |

### 4.5 Réponse vocale (TTS)

| ID | Exigence | Priorité |
|---|---|---|
| **FR-19** | En Realtime, le système restitue le seul `assistantMessage` validé par le backend via l'API TTS Joprelys. `SpeechSynthesis` et la voix OpenAI Realtime ne sont pas utilisés en parallèle. | P0 |
| **FR-20** | Le TTS doit être désactivable par le médecin via un bouton mute dans le panneau. | P2 |

### 4.6 Pré-remplissage du formulaire

| ID | Exigence | Priorité |
|---|---|---|
| **FR-21** | Les champs extraits par l'IA doivent pré-remplir le formulaire de consultation existant. | P0 |
| **FR-22** | Le médecin doit pouvoir modifier manuellement tout champ pré-rempli avant validation. | P0 |
| **FR-23** | Les champs modifiés manuellement par le médecin doivent être visuellement distingués des champs auto-remplis. | P2 |

### 4.7 Mode texte

| ID | Exigence | Priorité |
|---|---|---|
| **FR-24** | Le système doit accepter l'envoi de messages textuels en alternative à l'audio. | P1 |
| **FR-25** | Le traitement des messages textuels doit suivre la même logique d'extraction de champs que l'audio. | P1 |

### 4.8 Validation médicale et confidentialité

| ID | Exigence | Priorité |
|---|---|---|
| **FR-26** | L'IA ne doit jamais persister directement une consultation, un diagnostic ou une prescription. | P0 |
| **FR-27** | Le médecin doit relire et appliquer explicitement le brouillon avant toute sauvegarde clinique. | P0 |
| **FR-28** | Le système doit afficher clairement que le contenu est une proposition IA à vérifier. | P0 |
| **FR-29** | Le mode manuel doit rester utilisable lorsque l'IA, le micro ou la caméra sont indisponibles. | P0 |
| **FR-30** | La Dictée reste passive. En Realtime, une question ou réponse clinique courte peut être vocalisée par un canal TTS unique, sans instruction interne et sans couper la capture. | P0 |

---

## 5. Exigences non fonctionnelles

### 5.1 Performance

| ID | Exigence | Cible |
|---|---|---|
| **NFR-01** | Temps de réponse de l'API `/message` (audio ≤ 30s) | ≤ 5 secondes (p95) |
| **NFR-02** | Temps de réponse de l'API `/message` (texte) | ≤ 3 secondes (p95) |
| **NFR-03** | Temps d'activation du scanner QR | ≤ 2 secondes |
| **NFR-04** | Taille maximale d'un fichier audio par requête | 10 Mo |

### 5.2 Sécurité

| ID | Exigence | Description |
|---|---|---|
| **NFR-05** | Authentification | Toutes les API IA nécessitent un JWT valide avec le rôle `DOCTOR`. |
| **NFR-06** | Isolation multi-tenant | Le médecin ne peut accéder qu'aux visites de son organisation. |
| **NFR-07** | Non-persistance audio | Aucun fichier audio ne doit être persisté sur le serveur ou en base de données. L'audio est traité en mémoire et immédiatement supprimé après transcription. |
| **NFR-08** | Transport sécurisé | Toutes les communications doivent transiter via HTTPS/TLS 1.3. |
| **NFR-09** | Clés API | Les clés API des providers IA doivent être stockées dans des variables d'environnement, jamais dans le code source ou les fichiers de configuration versionnés. |
| **NFR-10** | Journalisation | Les appels aux providers IA doivent être journalisés (sans contenu médical) pour le suivi des coûts et le débogage. |
| **NFR-10a** | Activation production | Le fournisseur, la résidence, la rétention, les clauses de sous-traitance et les contrôles de données doivent être validés par le DPO avant toute donnée réelle. |
| **NFR-10b** | Minimisation | Aucun nom, DPU, e-mail, téléphone ou date de naissance ne doit être ajouté au contexte envoyé au fournisseur. |

### 5.3 Accessibilité

| ID | Exigence | Description |
|---|---|---|
| **NFR-11** | Navigation clavier | Le panneau d'assistant vocal doit être entièrement navigable au clavier. |
| **NFR-12** | Lecteur d'écran | Les boutons et contrôles doivent avoir des attributs `aria-label` appropriés. |
| **NFR-13** | Contraste | Les textes et boutons doivent respecter le ratio de contraste WCAG 2.1 AA (4.5:1). |

### 5.4 Fiabilité

| ID | Exigence | Description |
|---|---|---|
| **NFR-14** | Dégradation gracieuse | Si le service IA est indisponible, le formulaire de consultation reste entièrement fonctionnel en mode manuel. |
| **NFR-15** | Reprise de session | Si la connexion réseau est interrompue temporairement, les champs déjà extraits doivent être conservés côté client. |

### 5.5 Compatibilité

| ID | Exigence | Description |
|---|---|---|
| **NFR-16** | Navigateurs | Chrome 90+, Safari 15+, Firefox 100+, Edge 90+ |
| **NFR-17** | Appareils | Desktop (Windows, macOS), Tablette et Smartphone (iOS, Android) |

---

## 6. Exigences UI/UX

### 6.1 Panneau d'assistant vocal

Le panneau d'assistant vocal est un composant latéral (slide-over) qui s'ouvre à droite de l'écran sur desktop, ou en plein écran sur mobile.

#### Structure du panneau

```
┌──────────────────────────────────────┐
│  ✕  Assistant IA — Consultation      │
│─────────────────────────────────────-│
│  👤 Patient : Jean Mbouda            │
│  📋 Visite : V-2026-00142           │
│──────────────────────────────────────│
│                                      │
│  ┌──────────────────────────────┐    │
│  │ 🤖 Bonjour Dr. Kamga,       │    │
│  │ je suis prêt à vous aider   │    │
│  │ pour la consultation de      │    │
│  │ Jean Mbouda. Décrivez les   │    │
│  │ symptômes du patient.       │    │
│  └──────────────────────────────┘    │
│                                      │
│  ┌──────────────────────────────┐    │
│  │ 🩺 Le patient présente une  │    │
│  │ toux sèche depuis 3 jours...│    │
│  └──────────────────────────────┘    │
│                                      │
│  ┌──────────────────────────────┐    │
│  │ 🤖 J'ai extrait :           │    │
│  │ ✅ Symptômes                 │    │
│  │ ✅ Examen clinique           │    │
│  │ ✅ Diagnostic suspecté       │    │
│  │ ⬜ Diagnostic (requis)       │    │
│  │ ⬜ Conseils                  │    │
│  └──────────────────────────────┘    │
│                                      │
│──────────────────────────────────────│
│  [📝 Texte]  [ 🎙 Maintenir ]  🔇  │
│──────────────────────────────────────│
│  [ Appliquer au formulaire ]         │
└──────────────────────────────────────┘
```

#### Éléments clés

| Élément | Comportement |
|---|---|
| **En-tête** | Affiche le nom du patient et le numéro de visite. Bouton de fermeture (✕). |
| **Zone de conversation** | Défilement vertical. Messages IA (bulle gauche, fond bleu clair) et messages médecin (bulle droite, fond gris). |
| **Indicateur de champs** | Checklist temps réel des champs remplis (✅) et manquants (⬜). Les champs obligatoires manquants sont marqués en orange. |
| **Bouton microphone** | Mode « Push-to-Talk » : l'enregistrement démarre au press et s'arrête au release. Animation pulsante pendant l'enregistrement. |
| **Champ texte** | Alternative à la voix. Champ de saisie avec bouton d'envoi. |
| **Bouton mute (TTS)** | Évolution P2 : désactive la restitution conversationnelle Realtime sans couper le microphone. |
| **Bouton « Appliquer au formulaire »** | Transfère les champs extraits dans le formulaire de consultation. Disponible uniquement quand au moins un champ est extrait. |

### 6.2 Scanner QR Code

| Élément | Comportement |
|---|---|
| **Activation** | Bouton « Scanner QR » dans la barre d'outils du médecin ou dans le dashboard. |
| **Cadre de scan** | Superposition semi-transparente avec cadre de visée centré. |
| **Feedback** | Vibration/son à la détection du QR code. Affichage du nom du patient pour confirmation avant ouverture du panneau. |
| **Erreur** | Message inline si le QR code est invalide ou la visite non trouvée. |

### 6.3 Responsive Design

| Breakpoint | Comportement du panneau |
|---|---|
| Desktop (≥ 1024px) | Panneau latéral droit (largeur : 420px), formulaire de consultation visible à gauche. |
| Tablette (768–1023px) | Panneau latéral droit (largeur : 360px), formulaire partiellement visible. |
| Mobile (< 768px) | Panneau en plein écran avec navigation par onglets : Conversation / Formulaire. |

---

## 7. Règles métier pour l'extraction de champs

### 7.1 Mapping des champs

| Champ du formulaire | Indicateurs linguistiques | Obligatoire |
|---|---|---|
| `symptoms` | « symptômes », « se plaint de », « présente », « depuis X jours », « douleur », « fièvre », « toux » | ✅ Oui |
| `clinicalExam` | « à l'examen », « examen clinique », « auscultation », « palpation », « tension », « pouls » | Non |
| `suspectedDiagnosis` | « je suspecte », « hypothèse diagnostique », « probablement », « compatible avec » | Non |
| `diagnosis` | « diagnostic », « le diagnostic est », « il s'agit de », « diagnostic retenu » | ✅ Oui |
| `finalDiagnosis` | « diagnostic final », « diagnostic définitif », « confirmé par » | Non |
| `conclusion` | « en conclusion », « pour conclure », « synthèse » | Non |
| `advice` | « conseils », « recommandations », « je conseille », « il est recommandé de » | Non |
| `followUp` | « suivi », « contrôle dans », « revoir dans », « rendez-vous de suivi » | Non |

### 7.2 Règles d'accumulation

1. **Enrichissement progressif** : Chaque message enrichit les champs existants. Un champ déjà rempli n'est remplacé que si le médecin donne explicitement une correction (ex : *« Non, en fait le diagnostic est... »*).
2. **Fusion intelligente** : Si le médecin mentionne des symptômes dans plusieurs messages, ils sont fusionnés dans le champ `symptoms` avec une virgule comme séparateur.
3. **Reformulation médicale** : L'IA reformule les propos en langage médical structuré tout en préservant le sens clinique original.
4. **Respect du contexte francophone** : L'IA doit utiliser la terminologie médicale francophone (ex : « rhinopharyngite » et non « nasopharyngitis »).

### 7.3 Règles de clarification

L'IA doit demander des clarifications dans les cas suivants :

| Condition | Question de clarification |
|---|---|
| `symptoms` vide après le premier message | « Pouvez-vous décrire les symptômes présentés par le patient ? » |
| Description vague sans localisation | « Pouvez-vous préciser la localisation et la durée de la douleur ? » |
| `diagnosis` vide après 3 échanges | « Quel diagnostic retenez-vous pour ce patient ? » |
| Contradiction détectée | « Vous avez mentionné [X] puis [Y], pouvez-vous confirmer ? » |

---

## 8. Critères d'acceptation

### 8.1 Scénario 3.1 — Consultation vocale complète

- [ ] **AC-01** : Le scan QR ouvre le panneau d'assistant avec le bon patient et numéro de visite.
- [ ] **AC-02** : L'enregistrement vocal fonctionne en mode Push-to-Talk avec indicateur visuel.
- [ ] **AC-03** : L'audio est transcrit et les champs `symptoms`, `clinicalExam`, `suspectedDiagnosis` sont correctement extraits d'une description combinée.
- [ ] **AC-04** : L'IA pose une question de clarification pour le champ `diagnosis` manquant.
- [ ] **AC-05** : Un second message vocal complète les champs `diagnosis`, `advice`, `followUp`.
- [ ] **AC-06** : Le bouton « Appliquer au formulaire » pré-remplit correctement tous les champs extraits.
- [ ] **AC-07** : Le médecin peut modifier les champs pré-remplis avant de soumettre la consultation.
- [ ] **AC-08** : La soumission de la consultation via `POST /api/visits/{id}/consultation` réussit.
- [ ] **AC-09** : La session IA est terminée après soumission.
- [ ] **AC-10** : Aucun fichier audio n'est persisté sur le serveur.

### 8.2 Scénario 3.2 — Saisie textuelle

- [ ] **AC-11** : Un message texte est traité avec la même logique d'extraction que l'audio.
- [ ] **AC-12** : La réponse de l'IA inclut les champs extraits et une question de clarification le cas échéant.

### 8.3 Scénario 3.5 — Provider indisponible

- [ ] **AC-13** : En cas d'erreur 503, un message explicite est affiché et le formulaire manuel reste accessible.
- [ ] **AC-14** : Aucune exception non gérée n'est visible par l'utilisateur.

---

## 9. Hors périmètre (Out of Scope)

Les éléments suivants sont **explicitement exclus** de cette version :

| Élément | Raison |
|---|---|
| Reconnaissance vocale temps réel (streaming) | Complexité technique trop élevée pour la v1. L'audio est envoyé segment par segment. |
| Support multilingue (anglais, etc.) | La v1 supporte uniquement le français. Le support multilingue sera ajouté dans une version ultérieure. |
| Génération automatique de prescription | Hors périmètre de l'assistant de consultation. Sera traité dans un EPIC dédié. |
| Interface d'administration pour la configuration IA | La configuration se fait via `application.yml` / variables d'environnement. |
| Historique des sessions IA | Les sessions sont éphémères et non persistées en base de données. |
| Intégration avec des systèmes de dictée médicale tiers | L'IA est intégrée directement via les APIs des providers (OpenAI, Gemini, Claude). |
| Mode hors ligne | L'assistant IA nécessite une connexion internet active. |
| Formation du modèle IA sur des données spécifiques à la clinique | Les modèles pré-entraînés sont utilisés tels quels avec un system prompt adapté. |

---

## 10. Dépendances

| Dépendance | Type | Statut |
|---|---|---|
| API de consultation existante (`POST /api/visits/{id}/consultation`) | Interne | ✅ Disponible |
| QR code sur la fiche d'admission patient | Interne | ✅ Disponible (EPIC-0003) |
| Provider IA (OpenAI / Gemini / Claude) | Externe | 🔑 Clé API requise |
| Navigateur avec support `MediaRecorder` | Client | ✅ Chrome 49+, Safari 14.1+, Firefox 25+ |
| Navigateur avec lecture audio HTML5 | Client | ✅ requis pour la restitution Realtime |
| HTTPS en production | Infrastructure | ✅ Requis (microphone) |

---

## 11. Métriques de succès

| Métrique | Cible | Méthode de mesure |
|---|---|---|
| Taux d'adoption par les médecins | 50% à 3 mois | Ratio sessions IA / consultations totales |
| Temps moyen de saisie | Réduction de 50% | Mesure du temps entre ouverture et soumission |
| Taux de remplissage des champs optionnels | ≥ 70% | Analyse des consultations avec/sans IA |
| Satisfaction médecin | NPS ≥ 40 | Enquête trimestrielle |
| Taux d'erreur IA (extraction incorrecte) | < 10% | Audit manuel échantillonné |

---

## 12. Glossaire

| Terme | Définition |
|---|---|
| **STT** | Speech-to-Text — Transcription de la voix en texte |
| **TTS** | Text-to-Speech — Synthèse vocale, lecture de texte à haute voix |
| **LLM** | Large Language Model — Modèle de langage de grande taille (GPT-4o, Gemini, Claude) |
| **Provider IA** | Service tiers fournissant les capacités d'IA (OpenAI, Google, Anthropic) |
| **Session IA** | Instance temporaire de conversation entre le médecin et l'IA pour une visite donnée |
| **Push-to-Talk** | Mode d'enregistrement où le microphone est actif tant que le bouton est maintenu |
| **QR Code** | Quick Response Code — Code-barres bidimensionnel contenant l'identifiant de visite |
| **Tenant** | Organisation (clinique/hôpital) dans le système multi-tenant Joprelys Connect |

---

## 13. Uniformisation de la surface d’écoute vocale

### 13.1 Besoin

Le professionnel ne doit pas apprendre quatre représentations différentes du
microphone selon qu’il utilise la Dictée ou le Realtime dans la Consultation ou
la saisie des Constantes. À état équivalent, la surface d’écoute est strictement
identique dans les quatre parcours.

### 13.2 Parcours couverts

| Contexte | Moteur | Surface attendue |
|---|---|---|
| Consultation | Realtime | carte Soft UI partagée |
| Consultation | Dictée | carte Soft UI partagée |
| Constantes | Realtime | carte Soft UI partagée |
| Constantes | Dictée | carte Soft UI partagée |

### 13.3 Règles fonctionnelles

- La carte contient toujours le badge d’activité, l’action d’arrêt, le micro
  central, les ondes, le message d’état et un conseil.
- Le conseil peut mentionner les notes de consultation ou les constantes, sans
  changer la structure, les dimensions ni la hiérarchie visuelle.
- Le composant partagé ne décide pas comment arrêter le moteur : il émet une
  intention et chaque contrôleur conserve son comportement existant.
- Les messages de connexion, pause ou indisponibilité utilisent la même zone
  d’état sans créer une autre carte concurrente.
- L’historique de transcription n’appartient pas à la surface d’écoute. Il reste
  disponible juste après la carte dans une zone bornée et scrollable.
- Les propositions IA, clarifications, validations et sauvegardes restent
  inchangées et toujours explicites.

### 13.4 Critères d’acceptation

- Les quatre parcours réutilisent un seul composant de présentation.
- L’interface correspond à la maquette
  `docs/ai/mockups/exact_soft_voice_card_1785238037119.jpg`.
- Les textes sont traduits en français et en anglais.
- Les thèmes light/dark et les viewports mobile/desktop conservent le même ordre
  visuel et des actions tactiles d’au moins 44 px.
- Aucun moteur vocal, contrat API ou comportement clinique n’est modifié.

## 14. Dictée Realtime continue, correction et finalisation sans perte

### 14.1 Contrat conversationnel

- OpenAI Realtime est un transport de transcription, pas un interlocuteur
  vocal autonome.
- La Dictée est passive. Le Realtime reste conversationnel : une clarification
  ou réponse clinique courte validée par le backend peut être vocalisée.
- Une seule voix est autorisée. Une instruction interne, un prompt ou une
  explication métatechnique n'est jamais lue.
- La restitution Realtime ne coupe pas le micro et s'interrompt si le médecin
  reprend la parole.
- Les messages et clarifications sont affichés visuellement, brièvement et sans
  discours métatechnique sur le fonctionnement du modèle.
- Chaque tour non vide apparaît immédiatement dans l'historique avant son
  analyse clinique.

### 14.2 Correction humaine

- Une transcription dont la confiance est absente ou inférieure au seuil reste
  visible et durable ; l'absence de confiance ne signifie jamais « absence de
  texte ».
- Même lorsque la confiance est annoncée comme suffisante, la dernière phrase
  Realtime expose toujours l'action « Corriger ». Le médecin peut modifier
  noms, nombres, doses, négations, unités et latéralité ; le texte visible est
  remplacé par sa correction et celle-ci devient un nouveau tour
  conversationnel explicite.
- Lorsque la confiance est absente ou faible, le médecin dispose en plus dans
  le mode Realtime du même éditeur bloquant que dans la Dictée.
- L'analyse reprend uniquement après l'action explicite « Corriger et
  analyser ». L'action « Écarter » est elle aussi explicite et n'efface pas le
  journal durable d'origine.
- La capture des tours suivants continue pendant la relecture et les conserve
  dans l'ordre.

### 14.3 Finalisation

L'action « Terminer » suit l'ordre obligatoire suivant :

1. arrêter l'envoi de nouveaux tours audio ;
2. conserver le contrôleur et les files en mémoire ;
3. attendre les accusés de réception durables et les analyses en cours ;
4. présenter toute revue ou décision humaine encore nécessaire ;
5. appliquer le brouillon par la fusion sûre existante ;
6. seulement ensuite quitter le mode Realtime.

Une finalisation ne vide jamais une file, un transcript visible ou un brouillon.
La pause explicite, la finalisation explicite et l'échec de persistance durable
fail-closed sont les seuls états autorisés à couper le sender WebRTC.

### 14.4 Constantes

- La dictée ponctuelle des constantes reste passive. En Realtime, une
  clarification clinique peut être vocalisée par le canal TTS unique.
- Le transcript entendu est éditable et peut être réanalysé avant
  l'application.
- Les valeurs détectées restent des propositions ; la persistance nécessite
  toujours la validation explicite du professionnel.
