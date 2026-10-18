# FUNCTIONAL SPEC — Système unifié de dialogues et notifications (Dialog & Notification System)

## 1. Contexte & Problématique métier

Dans une application hospitalière et clinique comme Joprelys Connect, l'utilisateur (médecin, infirmier, caissier, agent d'accueil) réalise des actions sensibles :
- Reprendre la prise en charge d'un patient déjà suivi par un confrère ;
- Annuler une ordonnance ou supprimer un antécédent médical ;
- Rejeter une demande d'admission ou un consentement ;
- Être informé en cas d'erreur de téléchargement d'un document médical.

Auparavant, certaines de ces actions déclenchaient les boîtes de dialogue natives du navigateur (`window.confirm()`, `window.alert()`). Ces fenêtres présentent plusieurs défauts majeurs en milieu clinique :
1. **Rupture visuelle et ergonomique** : boîte modale grise système (« joprelys.com indique... ») déconnectée de l'interface et du thème (sombre/clair).
2. **Bloquage du thread JavaScript** : le navigateur fige l'exécution et l'interface utilisateur, empêchant toute interaction en arrière-plan.
3. **Risque de désactivation par le navigateur** : les navigateurs modernes proposent souvent une case « Empêcher cette page d'ouvrir d'autres boîtes de dialogue », ce qui casse les flux applicatifs si l'utilisateur la coche par inadvertance.
4. **Accessibilité limitée** : pas d'intégration fine avec les lecteurs d'écran (ARIA) ni avec la palette de contraste WCAG AA.

## 2. Objectifs fonctionnels

1. **Zéro boîte de dialogue native** : interdiction totale de `window.alert()`, `window.confirm()` et `window.prompt()`.
2. **Dialogue de confirmation asynchrone (`DialogService.confirm`)** :
   - Présentation claire de la décision à prendre : titre, description détaillée, bouton d'action contextuel (ex: « Reprendre le patient ») et bouton d'annulation (ex: « Annuler »).
   - Tonalité visuelle explicite adaptée à la sévérité :
     - `warning` : action sensible nécessitant une confirmation (reprise de patient, écrasement de saisie).
     - `danger` : action destructive ou irréversible (annulation d'ordonnance, rejet d'admission, suppression d'allergie/antécédent).
     - `primary` / `info` : validation ou changement d'état standard.
3. **Dialogue d'alerte / information bloquante (`DialogService.alert`)** :
   - Présentation d'un message important nécessitant un accusé de lecture obligatoire par l'utilisateur (bouton unique « D'accord » ou « Fermer »).
4. **Notifications éphémères (`ToastService`)** :
   - Pour les rétroactions de succès (« Document téléchargé », « Consentement mis à jour ») ou les erreurs non-bloquantes (« Erreur lors de l'export »), affichage d'un toast non-intrusif en bas à droite de l'écran, disparaissant automatiquement après quelques secondes ou sur clic sur la croix.
5. **Internationalisation (i18n)** : tous les libellés de dialogue et de toast sont localisés en français et en anglais.

## 3. Parcours utilisateur clés

### 3.1. Reprise de patient en consultation (File active)
1. Le Dr A consulte la file active et clique sur le patient P pris en charge par le Dr B.
2. Le tiroir latéral s'ouvre, montrant le statut « En consultation chez Dr B ».
3. Le Dr A clique sur le bouton « Reprendre le patient ».
4. Une boîte de dialogue Joprelys Connect au design soigné s'affiche au centre de l'écran avec un badge ambre d'avertissement :
   - Titre : « Reprendre la consultation »
   - Message : « Ce patient est déjà en consultation. Le reprendre à la place de : NOUPOUE TRAUMA »
   - Boutons : [Annuler] (secondaire) et [Reprendre le patient] (avertissement/primaire).
5. Si le Dr A clique sur [Reprendre le patient], la prise en charge est effectuée et la consultation s'ouvre.
6. Si le Dr A clique sur [Annuler] ou appuie sur la touche Échap, le dialogue se ferme sans aucun impact.

### 3.2. Annulation d'ordonnance ou suppression d'antécédent
1. Le praticien demande l'annulation d'une prescription.
2. Un dialogue rouge `danger` demande confirmation.
3. Après confirmation, l'annulation est envoyée au backend et un toast de succès confirme l'opération.

### 3.3. Notification d'erreur de téléchargement dans le portail patient
1. Le patient clique sur « Télécharger la synthèse médicale ».
2. En cas d'erreur réseau, un toast rouge d'erreur apparaît en bas à droite avec le message « Impossible de télécharger le document. Veuillez réessayer. », sans bloquer la navigation.

### 3.4. Confirmation de déconnexion (AppShell)
1. L'utilisateur connecté (personnel soignant, administratif ou patient) clique sur le bouton de déconnexion dans l'en-tête (ou le menu mobile).
2. Une boîte de dialogue de confirmation Joprelys Connect s'affiche au centre de l'écran avec une tonalité d'avertissement (`warning`) :
   - Titre : « Confirmation de déconnexion » / « Confirm Sign Out »
   - Message : « Êtes-vous sûr de vouloir vous déconnecter de votre session ? » / « Are you sure you want to sign out of your session? »
   - Bouton de confirmation : [Se déconnecter]
   - Bouton d'annulation : [Annuler]
3. Si l'utilisateur confirme, la session est invalidée via `authApi.logout()` et l'utilisateur est redirigé vers l'accueil (`/`).
4. Si l'utilisateur annule ou presse Échap, le dialogue se referme et l'utilisateur conserve sa session active sans interruption.
