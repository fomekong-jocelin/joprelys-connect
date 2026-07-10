# Spécification fonctionnelle — Clôture et historique de caisse

## Parcours nominal

1. Le caissier ouvre une session et réalise ses encaissements/mouvements.
2. Il clôture la session en déclarant le montant physique et, si nécessaire, une justification d’écart.
3. La session clôturée apparaît immédiatement en tête de l’historique personnel.
4. Le récapitulatif affiche les montants par moyen de règlement, les sorties, le solde attendu, le déclaré et l’écart.
5. Le caissier peut développer le détail des mouvements et télécharger le bordereau PDF.

## États d’interface

- chargement ;
- erreur avec nouvelle tentative ;
- historique vide ;
- session clôturée sans écart ;
- session clôturée avec écart justifié ;
- téléchargement PDF en cours/échoué ;
- session tout juste clôturée mise en évidence.

## Visibilité

- CAISSIER, AGENT_ACCUEIL : historique personnel uniquement ;
- DAF, ADMIN_CLINIQUE : historique personnel dans ce parcours et accès autorisé aux bordereaux de toute session tenantée ;
- rôles cliniques : accès refusé.

## Bordereau

Le PDF comprend : établissement, numéro de clôture, caisse, caissier, ouverture/clôture, fond initial, encaissements espèces/chèques/virements, dépenses espèces, versements banque, solde théorique, montant déclaré, écart, justification, liste des mouvements et zones de signature.