# Continuité URG-TEMP vers hospitalisation, documents et finance

## Objectif

Permettre à un patient pris en charge sous une identité provisoire `URG-TEMP` de poursuivre ses soins sans rupture administrative vers une hospitalisation, tout en conservant :

- le lien avec l'urgence d'origine ;
- la provenance de chaque donnée ;
- les numéros, versions et empreintes des documents ;
- les lignes financières créées avant régularisation ;
- la lecture longitudinale depuis le DPU canonique après rapprochement.

## Parcours fonctionnel

1. Un professionnel crée ou ouvre une urgence pour un patient identifié ou provisoire.
2. Le triage, les réévaluations, les actes de réanimation et les informations médico-légales sont saisis.
3. Le professionnel choisit « Poursuivre vers l'hospitalisation ».
4. Joprelys demande un service autorisant des chambres, un lit libre, un médecin responsable et un motif.
5. L'admission :
   - réutilise la visite active appropriée ;
   - ou crée une visite de continuité lorsque l'urgence n'en possède pas ;
   - relie explicitement l'hospitalisation à l'urgence ;
   - réserve le lit de manière atomique.
6. Le lot documentaire d'urgence est généré et conservé avec numéro, hash et version.
7. Le billet d'entrée est persisté et versionné lors de son premier téléchargement.
8. Les actes peuvent être facturés sans paiement initial. Une facture liée à une identité provisoire porte le statut `REGULARIZATION_PENDING`.
9. Après rapprochement, le DPU canonique agrège les hospitalisations, documents et factures des identités sources sans déplacer ni renuméroter les éléments historiques.

## Règles métier

### Identité et rapprochement

- Un patient `PROVISIONAL_URGENCY` ou `DECLARED` peut être hospitalisé.
- Un patient source déjà `MERGED` ne reçoit pas de nouveau séjour ; la continuité est créée sur le DPU canonique.
- Une urgence ne peut être reliée qu'à un patient appartenant au même contexte canonique et au même établissement.
- Une même urgence ne peut produire qu'une hospitalisation.
- Une hospitalisation active sur une identité source bloque une seconde hospitalisation sur le DPU canonique, et inversement.

### Hospitalisation

- Une visite ou une urgence est obligatoire comme contexte de soins.
- Une urgence sans visite déclenche une visite de continuité contrôlée.
- Le service doit avoir `allowsRooms=true`.
- Le lit doit exister, appartenir à l'établissement et être libre.
- La réservation du lit reste atomique et anti double-booking.

### Documents

Le lot minimal comprend :

- fiche d'urgence ;
- feuille de réanimation ;
- constat d'incapacité et base d'urgence ;
- fiche du déclarant/accompagnant ;
- inventaire et reçu des effets personnels ;
- billet d'entrée d'hospitalisation.

Chaque document conserve :

- son numéro ;
- son hash SHA-256 ;
- sa version ;
- son patient d'origine ;
- son URL de vérification ;
- son QR code ;
- son auteur et sa date.

La génération du lot est idempotente : une nouvelle demande retourne les documents existants au lieu de les dupliquer.

### Finance différée

- La création d'une facture ne nécessite pas de paiement immédiat.
- Une facture d'un patient provisoire ou déclaré est créée avec `REGULARIZATION_PENDING`.
- Une facture d'un patient vérifié est `RESOLVED`.
- Le rapprochement ne modifie ni le `patient_id` historique, ni le numéro, ni les lignes, ni les paiements.
- La consultation des factures sur le DPU canonique agrège celles de toutes les identités contributrices.

## Interface

Le panneau de continuité :

- affiche l'identifiant URG-TEMP lorsqu'il existe ;
- avertit que l'identité reste provisoire sans bloquer les soins ;
- n'affiche que les services autorisant l'hébergement ;
- n'affiche que les lits libres ;
- permet de sélectionner le praticien responsable ;
- conserve une erreur documentaire séparée d'une admission déjà réussie afin d'éviter une double admission lors d'un rejeu.

## Sécurité

- permissions d'hospitalisation, documents et finance inchangées ;
- isolation tenant côté backend ;
- résolution canonique partagée ;
- aucun transfert silencieux de données entre patients ;
- audit des admissions, factures et documents.

## Limites assumées

- La modélisation détaillée des box d'urgence et postes de réanimation reste hors périmètre.
- Les fichiers sont stockés par le mécanisme documentaire actuel ; la migration vers un stockage objet relève d'un chantier d'infrastructure séparé.
- La signature UAT humaine des métiers reste nécessaire avant une déclaration de conformité opérationnelle définitive.
