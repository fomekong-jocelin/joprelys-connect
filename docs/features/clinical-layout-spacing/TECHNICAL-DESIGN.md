# Conception — correctif de layout

`visit-details-drawer.component.html` : footer en colonne, gap explicite, boutons partagés pleine largeur et cible 44px ; fermeture en variante link dans une zone séparée. Header/footer shrink-0 et corps min-h-0 flex-1 overflow-y-auto pour garder les actions accessibles dans la bottom sheet mobile. Ne pas changer ButtonComponent globalement : son input class est transmis au bouton interne, donc flex-1 seul ne remplit pas la largeur.

`consultation.component.html` : conteneur flex-col avec gap pour espacer réellement les hôtes de composants Angular (inline par défaut). `consultation-entry-mode.component.html` : colonne avec gap, étapes en grille responsive avec padding vertical et séparateurs. Utilitaires de spacing Tailwind v4 et tokens existants ; pas de CSS local, couleur/rayon/texte nouveau.

Sécurité/API/DB/backend/mobile/configuration/CI inchangés. PATCH candidat sans bump. Tests Angular existants, build, diff ; recette visuelle distincte et encore ouverte, sans contournement du refus navigateur précédent.
