# FUNCTIONAL-SPEC — Correctifs de l'audit du 2026-11-04

Référence : [AUDIT-REPORT](AUDIT-REPORT.md) · [ADR-20261104](../../architecture/adr/ADR-20261104-audit-security-clinical-hardening.md) · [EPIC-0030](../../pm/backlog/EPIC-0030-audit-20261104-remediation.md) (reste à faire).

## 1. Objectif

Chaque acte est posé par la bonne personne, au bon moment et sur le bon patient. Le patient ne voit que ce qui a été validé. L'interface ne laisse jamais croire qu'une donnée absente est une donnée vérifiée.

## 2. Règles métier livrées

### 2.1 Qui pose les actes médicaux

| Acte | Qui peut le faire | Sinon |
|---|---|---|
| Rédiger la consultation médicale | un médecin habilité (compte actif) | 403 « La consultation médicale est rédigée par un médecin. Utilisez les transmissions infirmières pour vos observations. » |
| Décider la sortie d'hospitalisation | un médecin | 403 « La décision médicale de sortie est réservée à un médecin. » |
| Déclarer un décès, directement ou par l'orientation « Décès » des urgences | un médecin | 403 « Le constat de décès est réservé à un médecin. » |
| Signer ou annuler une ordonnance | le praticien qui a le patient en consultation | 409 « Seul le praticien en charge peut signer ou annuler cette ordonnance » |

L'administrateur clinique garde ses fonctions d'administration (personnel, structure, facturation…) mais ne pose aucun acte médical.

### 2.2 Ordonnance

- Une ordonnance n'est rédigée que sur une visite **en cours**. Sur une visite close : 409.
- Annulation possible depuis **Brouillon** ou **Active** uniquement. Une ordonnance expirée ou déjà annulée ne s'annule pas : 409.
- Le document PDF valide associé est annulé en même temps. Un document déjà annulé ne provoque plus d'erreur.

### 2.3 Pharmacie

- Le pharmacien voit les valeurs **prescrites** : forme, voie, fréquence, posologie, durée et autorisation de substitution. Le système n'invente plus de valeur par défaut.
- Quantité délivrée strictement positive. Sinon : 400.
- Substitution interdite par le prescripteur : 409 « Le prescripteur n'autorise pas la substitution de : <médicament> ».
- Patient décédé, y compris par le dossier fusionné : délivrance et validation pharmaceutique refusées (409).
- **Vérification publique** (numéro d'ordonnance + PIN) :
  - le PIN des nouvelles ordonnances a 6 chiffres ; ceux à 4 chiffres déjà émis restent valides ;
  - après 3 erreurs, l'ordonnance est bloquée 15 minutes, même si le serveur redémarre (429) ;
  - un numéro inconnu et un PIN faux reçoivent la même réponse : 401 « Numéro d'ordonnance ou code PIN incorrect. ».

### 2.4 Soins et décès

- Une administration ne peut pas être datée de plus de 5 minutes dans le futur (400).
- Après un décès :
  - aucune administration datée **après l'heure du décès** (409) ; une administration antérieure peut encore être tracée a posteriori ;
  - aucune demande d'examen (409).

### 2.5 Laboratoire

- Une demande d'examen et sa visite concernent le même patient, identité fusionnée comprise. Sinon : 400 « La visite indiquée n'appartient pas à ce patient. ».
- Une saisie de résultats **sans statut explicite** reste un brouillon.
- Un examen se valide en enregistrant ses résultats validés, jamais par un simple changement de statut (409).

### 2.6 Ce que voit le patient

- **Résumé médical** :
  - seules les consultations **signées** ;
  - seule la **dernière version validée** de chaque résultat critique.
- **Export de ses résultats** : uniquement les résultats validés.
- **Notification de résultats** :

| Interprétation validée | Message |
|---|---|
| Normale | « Vos résultats d'analyses pour la demande … sont disponibles. » |
| Anormale | … « Certaines valeurs sont en dehors des valeurs habituelles : parlez-en avec votre médecin. » (information) |
| Critique | … « Une valeur doit être revue rapidement avec un médecin : contactez votre médecin ou l'établissement. En cas de malaise ou de symptômes inquiétants, rendez-vous sans attendre aux urgences. » (urgent) |

> Le message n'affirme pas que l'équipe soignante est prévenue : l'alerte structurée au prescripteur (corbeille de résultats) n'existe pas encore (story **A4**, prioritaire). Libellés à faire valider par un médecin référent avant la mise en production.

### 2.7 Urgences

- Niveau de triage obligatoire, parmi : RED, ORANGE, YELLOW, GREEN.
- Constantes initiales bornées par la plausibilité **physique** : PAS 0–300, PAD 0–200, FC 0–300, T° 20–45 °C. Une FC à 0 (arrêt cardiaque) est acceptée. Ce ne sont pas des seuils cliniques.

## 3. Parcours web

| Situation | Nouveau comportement |
|---|---|
| Les allergies ne se chargent pas | Message « Allergies non vérifiées » + « Réessayer ». La clôture de la consultation et l'ordonnance restent bloquées, avec une explication. |
| Ouverture d'une admission aux urgences | Tri, état hémodynamique et mode d'arrivée sont vides et obligatoires. Rien n'est pré-sélectionné au niveau le plus grave. |
| Clôture de consultation, délivrance, validation de résultats, fermeture d'un lit | Confirmation avec récapitulatif ; « Annuler » a le focus pour une action dangereuse. |
| Session expirée ou accès sans connexion | Retour à la page de connexion (personnel ou patient), puis à la page demandée. |
| Serveur injoignable | Page « Serveur indisponible » avec « Réessayer », au lieu d'un écran vide. |
| URL inconnue | Page 404 traduite avec un lien vers le tableau de bord. |
| Montants et dates | Format local : `125 000 FCFA`, `04/11/2026 14:30`. |
| Erreur de chargement (devis, acomptes, accueil, menu) | Message explicite ; les erreurs de facturation restent affichées jusqu'à leur fermeture. |

## 4. Parcours mobile

- **Session** :
  - une coupure réseau ne déconnecte plus ;
  - une vraie expiration ramène à l'écran de connexion avec un message ;
  - le médecin suivant ne voit jamais la file du précédent.
- **Dictée** : elle fonctionne au-delà de la durée du jeton d'accès.
- **Verrou** : il s'affiche par-dessus l'écran en cours sans perdre la saisie.
- **Protection des données sur l'appareil** :
  - captures d'écran et aperçu multitâche bloqués sur Android ;
  - aucune sauvegarde cloud des données ;
  - PDF et brouillons effacés à la déconnexion ;
  - le clavier n'apprend pas les noms ni les diagnostics.

## 5. Hors périmètre (EPIC-0030)

Les points suivants sont planifiés, pas livrés :
- contrôle d'allergie et de doublon ;
- posologie structurée ;
- prescriptions de séjour ;
- corbeille du prescripteur ;
- permission de validation biologique ;
- bris de glace par utilisateur ;
- QR patient local ;
- navigation par parcours ;
- garde « modifications non enregistrées » ;
- i18n complète ;
- signature release mobile ;
- décrément de stock ;
- constantes saisies par l'accueil (décision métier attendue).
