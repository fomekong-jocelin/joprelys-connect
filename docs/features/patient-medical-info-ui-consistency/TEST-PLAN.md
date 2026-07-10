# TEST-PLAN — Cohérence visuelle des informations médicales patient

| Scénario | Résultat attendu | Niveau |
|---|---|---|
| Rendu des sections médicales | Quatre icônes SVG partagées, aucun emoji de titre | Angular |
| Historique d'urgence vide | Titre et état vide traduits, aucune clé technique visible | Angular |
| Statut d'urgence et réanimation | Icônes partagées et libellés FR/EN | Angular |
| Consultation inexistante pour une visite valide | Réponse `404` documentée ; formulaire Angular vierge | MockMvc / diagnostic |
| Thèmes et formes | Couleurs sémantiques, rayon maximal 8 px | Build / QA visuelle |
