# Diagnostic — formulaire annoncé mais absent

Preuve utilisateur : capture supplémentaire montrant le bandeau « Compte rendu validé » sans champs.

Preuve code : `voice-assistant-panel.component.html` infère FORM_READY depuis les valeurs de `currentDraft` et le composant émet `formReadyChange`; `consultation.component.html` ne consomme pas cet événement et se fonde sur `consultation() || form.dirty`. Une ouverture manuelle émet un brouillon vide, donc `applyAiDraft` ne marque pas dirty. Une valeur préremplie sans dirty peut également déclencher le bandeau sans les champs.

Correction appliquée : signal parent explicite, output relié, input de visibilité transmis à l'assistant. Le chargement d'une consultation ouvre ce signal. Le titre « compte rendu validé » n'est plus utilisé pour la saisie manuelle ou simplement chargée ; seul un rapport explicitement accepté utilise ce titre. Aucun changement de validation médicale ou sauvegarde implicite. Une initialisation realtime en erreur laisse le formulaire ouvert. Un retour manuel existe après capture arrêtée ; les valeurs sont conservées. Les doubles démarrages de dictée sont bloqués pendant l'autorisation du microphone.

Régression couverte : DOM réel pour saisie manuelle vierge, consultation chargée pristine, rapport accepté, reprise sans perte du texte, erreur realtime, erreur/autorisation microphone en attente. 15 nouveaux tests, suite complète 619/619 réussie. Recette du rendu et microphone réel reste à effectuer.
