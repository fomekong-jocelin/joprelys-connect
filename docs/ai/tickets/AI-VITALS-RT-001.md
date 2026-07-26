# AI-VITALS-RT-001 — Smart Vitals vocal Realtime

## Statut

- **Epic** : IA clinique / Parcours de soins
- **Type** : Feature UX + sécurité de saisie
- **Priorité** : P0
- **Statut** : IMPLEMENTED — validation finale PR en cours
- **PR** : #153
- **Branche** : `agent/realtime-clinical-copilot-v3`

## Objectif

Permettre au professionnel de saisir plusieurs constantes naturellement à la voix, en temps réel et sur mobile, tout en conservant les contrôles Joprelys, la confirmation des ambiguïtés et un geste explicite de sauvegarde.

## Périmètre inclus

- Realtime WebRTC dédié aux constantes avec permission `VISIT_VITALS_WRITE`.
- Activation explicite de l'écoute Realtime avant toute ouverture du microphone.
- Arrêt de l'écoute lors de la sortie du mode ou de la réduction de l'assistant.
- Saisie vocale de : température, poids, taille, pouls, systolique, diastolique, SpO₂, glycémie, fréquence respiratoire et douleur.
- Dictée de plusieurs paramètres dans une même phrase.
- Clarification conversationnelle sur une valeur ambiguë.
- Conservation du contexte de clarification local pour comprendre une réponse courte telle que « oui » ou une correction telle que « non, 130 sur 80 ».
- Préremplissage des champs existants sans sauvegarde automatique.
- Affichage en cartes des valeurs détectées et de leurs unités.
- Mode texte toujours disponible.
- Dictée classique MediaRecorder disponible si Realtime est désactivé ou indisponible.
- FR/EN complet.
- Design mobile-first avec grandes zones tactiles, états audio et waveform.

## Hors périmètre

- Enregistrement automatique des constantes sans action humaine.
- Connexion Bluetooth à un tensiomètre, oxymètre, thermomètre, glucomètre ou balance.
- Provenance certifiée d'un dispositif médical.
- Interprétation diagnostique autonome d'une constante.

## Critères d'acceptation

- [x] L'ouverture du modal de constantes n'ouvre pas automatiquement le microphone Realtime.
- [x] Un clic explicite est nécessaire pour activer l'écoute Realtime.
- [x] Réduire l'assistant coupe le mode Realtime continu.
- [x] Quitter le mode Realtime revient à la dictée classique.
- [x] Le Realtime vitals requiert `VISIT_VITALS_WRITE` et une visite active.
- [x] Une phrase contenant plusieurs constantes préremplit uniquement les champs reconnus.
- [x] Une valeur ambiguë n'est pas convertie silencieusement.
- [x] Une clarification est lue à voix haute et la réponse suivante est analysée avec son contexte.
- [x] Une proposition vide nécessitant clarification ne modifie aucun champ.
- [x] Le chemin Realtime ne déclenche pas le TTS classique une seconde fois.
- [x] Le professionnel conserve le bouton métier existant pour enregistrer les constantes.
- [x] Le texte reste disponible pendant l'utilisation du module.
- [x] FR/EN couvrent les états Realtime, consentement, erreurs, unités et libellés.

## Scénarios de recette

### VT-01 — Activation explicite
1. Ouvrir le modal des constantes.
2. Vérifier qu'aucun microphone Realtime n'est actif.
3. Appuyer sur « Activer l'écoute Realtime ».
4. Vérifier le passage à l'état micro en direct.

### VT-02 — Dictée multi-constantes
Dire : « Température 38,4, tension 132 sur 84, saturation 96, pouls 104, poids 73 kilos. »

Attendus :
- température = 38,4 °C ;
- systolique = 132 mmHg ;
- diastolique = 84 mmHg ;
- SpO₂ = 96 % ;
- pouls = 104 bpm ;
- poids = 73 kg ;
- aucune sauvegarde automatique.

### VT-03 — Tension abrégée ambiguë
1. Dire : « Tension douze huit. »
2. Vérifier qu'aucune conversion silencieuse n'est appliquée.
3. Répondre : « Oui, 120 sur 80. »
4. Vérifier le préremplissage 120/80 après clarification.

### VT-04 — Correction naturelle
1. Dire : « Température 38,2. »
2. Dire ensuite : « Non, 37,2. »
3. Vérifier que la correction proposée devient 37,2 °C.

### VT-05 — Réduction du panneau
1. Activer Realtime.
2. Réduire l'assistant.
3. Vérifier la coupure du mode d'écoute continue.

### VT-06 — Fallback
1. Simuler WebRTC indisponible/refusé.
2. Vérifier que dictée classique et texte restent disponibles.

## Risques et garde-fous

| Risque | Garde-fou |
|---|---|
| Micro activé sans intention claire | Activation explicite + arrêt en quittant le mode |
| Valeur fausse due à l'ASR | Parseur strict + plages + clarification |
| « 12/8 » interprété arbitrairement | Confirmation obligatoire |
| Sauvegarde involontaire | Realtime ne fait que préremplir |
| Double synthèse vocale | Realtime désactive le TTS classique pour sa réponse |
| Réponse courte sans contexte | Contexte local de clarification |

## Definition of Done

- [x] Endpoint Realtime vitals protégé.
- [x] Contrôleur Realtime vitals frontend.
- [x] Activation explicite / consentement UX.
- [x] Arrêt du mode en réduction/sortie.
- [x] Clarification conversationnelle.
- [x] Fallback classique + texte.
- [x] FR/EN.
- [x] Tests composants ajoutés.
- [ ] CI globale verte sur le HEAD final de la PR #153.
- [ ] Recette réelle sur mobile avec accent camerounais/francophone et bruit de salle.

## Preuves attendues

- Run CI final frontend tests + build production.
- Run CI backend pour le contrôleur permission/visite active.
- Captures/vidéo mobile VT-01 à VT-06.
