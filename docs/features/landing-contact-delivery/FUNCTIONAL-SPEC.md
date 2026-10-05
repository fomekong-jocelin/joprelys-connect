# Demande de démonstration — fiabilité des contacts

Les visiteurs du site demandent une démonstration en fournissant nom, établissement et téléphone ; email, fonction, ville et message sont complémentaires. Les contacts Joprelys sont +237691501780 et contact@joprelys.com.

Le succès signifie que le serveur a enregistré la demande avec une référence. Une notification mail est tentée vers l'équipe après commit ; elle ne constitue pas une garantie de lecture ou de livraison SMTP. Si l'API échoue, la saisie reste visible, le succès n'est pas affiché et le visiteur peut réessayer, ouvrir WhatsApp prérempli ou contacter l'équipe par mail. Les alternatives restent disponibles avant soumission et après succès. Aucun mail ni message WhatsApp n'est envoyé automatiquement depuis le navigateur.

Critères : contacts centralisés, FR/EN, champs obligatoires/email validés, blocage double soumission, timeout HTTP, succès uniquement sur réponse serveur réelle. Pas de sauvegarde persistante de coordonnées dans le navigateur. Thèmes et tokens existants conservés. Hors périmètre : automatisation WhatsApp, interface de traitement des prospects, reprise automatique des emails SMTP en échec.
