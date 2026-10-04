# Entrée en consultation — proposition visuelle

Source : capture utilisateur du 2026-10-04 et parcours existant. Cible : médecin sur desktop. But : rendre compréhensible le choix du mode et l'action de démarrage avant la consultation.

La proposition montre les modes manuel, dictée et conversation enregistrée avec descriptions concises. La sélection est visible ; le bouton principal annonce l'action réelle. Le contexte patient et les alertes sont conservés. Pour le mode assisté, capture, relecture et formulaire clinique sont des étapes explicites. La saisie manuelle reste immédiatement repérable.

La maquette a été acceptée le 2026-10-04. L'implémentation conserve les trois modes dans un groupe radio accessible ; choisir un mode ne démarre rien. Une action principale ouvre le formulaire, prépare la dictée ou démarre le canal de conversation. La dictée reste une capture contrôlée par le praticien, avec arrêt puis relecture. Une capacité indisponible est expliquée et l'action désactivée.

Le formulaire apparaît après ouverture manuelle, chargement d'une consultation sauvegardée ou acceptation explicite du rapport. Son affichage ne dépend pas de l'état dirty. Le bandeau distingue rapport accepté et formulaire simplement ouvert ; aucune validation clinique ou sauvegarde n'est déduite de valeurs préremplies. La reprise de capture conserve les valeurs déjà saisies.

Thèmes light/dark, traductions FR/EN, empilement mobile et focus clavier utilisent les mécanismes centraux existants. Aucun changement de politique clinique, de contrat API, de stockage ni d'autorisation.
