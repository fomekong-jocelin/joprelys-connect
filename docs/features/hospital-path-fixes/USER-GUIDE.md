# Parcours hospitalier corrigé

Pour admettre un patient, sélectionner sa visite, le service, le médecin affecté et un lit disponible. Un échec de chargement affiche une erreur et une reprise ; il ne signifie pas absence de séjour ou de lit. Pendant l'enregistrement, attendre la réponse. Après conflit de disponibilité, choisir à nouveau un lit dans la liste actualisée.

Pour administrer un médicament, le profil habilité sélectionne une prescription validée et active du patient, puis renseigne la dose réellement administrée. Sans prescription admissible, faire établir/valider la prescription dans le parcours existant. Une prescription annulée ou expirée depuis son chargement est refusée par le serveur ; actualiser la liste.

Les consentements et comptes-rendus opératoires sont dans leurs onglets dédiés. La validation du CRO nécessite confirmation. Une panne de précalcul financier doit être résolue avant d'utiliser le calcul ; aucun total partiel n'est retourné comme réussi.

La décision médicale de sortie et le départ physique restent deux actions distinctes. Les règles de disponibilité, de nettoyage et les permissions existantes sont conservées.
