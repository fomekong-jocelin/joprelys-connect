# Demande de démonstration — fiabilité des contacts

Les visiteurs du site demandent une démonstration en fournissant nom, établissement et téléphone ; email, fonction, ville et message sont complémentaires. Les contacts Joprelys sont +237691501780 et contact@joprelys.com.

Le succès signifie que le serveur a enregistré la demande avec une référence. Une notification mail est tentée vers l'équipe après commit ; elle ne constitue pas une garantie de lecture ou de livraison SMTP. Si l'API échoue, la saisie reste visible, le succès n'est pas affiché et le visiteur peut réessayer ou ouvrir WhatsApp prérempli. Le lien WhatsApp reste disponible avant soumission et après succès. Le contact mail reste dans le footer ; le bouton « Nous contacter par e-mail » est retiré du formulaire à la demande de l'utilisateur. Aucun mail ni message WhatsApp n'est envoyé automatiquement depuis le navigateur.

Le CTA principal utilise un libellé court FR/EN (« Demander une démo »), un padding intérieur et un espace autour de l'action. Son texte reste intégralement visible sur une ligne dès 320px, sans troncature, avec une cible tactile minimale de 44px. Les états prêt/chargement/erreur et les thèmes light/dark sont conservés.

Le mail de notification adopte la structure des autres mails Joprelys : logo, carte blanche avec bandeau cyan, titre, référence, coordonnées et footer. La fonction est un libellé lisible ; le message libre conserve ses sauts de ligne et apparaît comme texte, jamais comme HTML exécutable. Une version texte reste disponible pour les clients sans HTML. La capture utilisateur du 2026-10-05 confirme la réception SMTP de la version précédente ; le nouveau rendu doit être revérifié dans le client mail après déploiement.

Critères : contacts centralisés, FR/EN, champs obligatoires/email validés, blocage double soumission, timeout HTTP, succès uniquement sur réponse serveur réelle. Pas de sauvegarde persistante de coordonnées dans le navigateur. Thèmes et tokens existants conservés. Hors périmètre : automatisation WhatsApp, interface de traitement des prospects, reprise automatique des emails SMTP en échec.
