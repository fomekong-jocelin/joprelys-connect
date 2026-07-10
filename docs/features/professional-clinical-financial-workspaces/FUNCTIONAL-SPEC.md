# Spécification fonctionnelle — Postes métier hospitalisation et caisse

## But

Cette spécification transforme les besoins CDC déjà recensés dans EPIC-0017 en parcours utilisables dans une clinique. En cas de silence du CDC, elle définit un minimum opérationnel prudent ; elle ne remplace ni protocole médical local ni décision de la DAF.

## Espaces de travail cibles

| Poste | Décision / travail principal | Informations nécessaires | Hors périmètre de l'écran |
|---|---|---|---|
| Admission | Créer et affecter le séjour | Identité, visite, prescripteur, motif, service, chambre/lit | Prescription et encaissement |
| Soignant | Transmissions et soins exécutés | Patient, séjour actif, plan de soin, dernières transmissions | Décision médicale ou facturation manuelle |
| Médecin | Décider, prescrire, valider la sortie | Synthèse séjour, examens, évolution, documents | Manipulation de caisse |
| Bloc | Documenter intervention, anesthésie et implants | Consentement, équipe, procédure, traçabilité implant | Admission administrative |
| Caissier | Encaisser et gérer sa session | Facture, débiteur, montant dû, moyen de paiement, session | Remise/avoir et validation d'écart |
| Recouvrement | Traiter les créances ouvertes | Débiteur, échéance, balance âgée, relances | Encaissement physique |
| DAF | Superviser, traiter exception et exporter | Sessions, écarts, bordereaux, écritures | Saisie de soin |

## Parcours hospitalisation

1. L'admission affiche un résumé patient, les prérequis du séjour et la disponibilité réelle du lit.
2. Une fois admis, le séjour est un dossier chronologique : événements signés, documents, soins, administrations, consommations et changements d'état.
3. Le soignant travaille depuis une liste de patients et ne saisit que les actions de son rôle. Les erreurs, annulations et corrections sont traçables.
4. Le médecin ou le bloc disposent de formulaires structurés, préparés par le contexte du séjour, sans mélanger ces actions avec les notes quotidiennes.
5. La sortie vérifie les prérequis décidés par l'établissement, produit les documents nécessaires et rend le séjour en lecture seule pour les données validées ; toute correction suit un processus explicite.

## Parcours caisse

1. Le caissier ouvre sa session avec un fonds de caisse et voit uniquement les factures ou parts patient exigibles.
2. L'encaissement confirme le débiteur, le montant restant, le moyen de paiement et la session, puis produit un reçu numéroté.
3. Les chèques et virements sont visibles séparément des espèces. Les dépenses, versements banque et écarts portent justificatif et autorisation selon les règles backend existantes.
4. La clôture présente le théorique, le déclaré, la ventilation des moyens et l'écart ; une exception ne peut être validée que par le rôle autorisé.
5. Recouvrement et DAF disposent de leurs propres listes d'action ; une facture tiers payant ne peut jamais être affichée simplement comme « payée » tant qu'une part assurance reste due.

## Exigences UX transverses

- Contexte patient ou session toujours lisible avant une action sensible.
- Statuts sous forme de texte, icône et couleur ; jamais la couleur seule.
- État vide actionnable, chargement, succès et erreur non bloquants ; pas d'`alert()` navigateur.
- Validation avant action irréversible et retour d'erreur utilisable au clavier et lecteur d'écran.
- Deux thèmes, FR/EN et design tokens centralisés respectant `DESIGN.md`.

## Points à valider avant développement

- règles locales de verrouillage/correction des données cliniques validées par le Médecin Chef ;
- liste des alertes patient affichables et leurs droits d'accès ;
- prérequis exacts de sortie et rôles pouvant les lever ;
- procédures DAF pour dépenses, dépôts, écarts, reçus et export comptable.

