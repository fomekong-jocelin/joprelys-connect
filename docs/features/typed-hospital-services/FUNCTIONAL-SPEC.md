# Services hospitaliers typés — Spécification fonctionnelle

## Finalité

La configuration d'une clinique doit représenter sa réalité opérationnelle. Un service administratif ou médico-technique ne devient pas une unité d'hébergement simplement parce qu'il apparaît dans l'organigramme.

Le système distingue désormais la fonction du service de sa capacité spatiale.

## Utilisateurs

- administrateur de clinique ;
- administrateur de plateforme intervenant dans le périmètre d'une clinique ;
- personnel d'hospitalisation et d'urgence ;
- personnel de caisse, pharmacie, laboratoire et imagerie.

## Types de services

### Hospitalisation — `HOSPITALIZATION`

Unité d'hébergement clinique. Les chambres et lits sont autorisés.

Exemples : médecine, pédiatrie, maternité, chirurgie.

### Urgences — `EMERGENCY`

Unité de prise en charge urgente utilisant le modèle spatial actuel. Les chambres, box ou salles et leurs lits sont autorisés.

### Consultations externes — `OUTPATIENT`

Service ambulatoire. Les chambres et lits d'hospitalisation sont interdits.

### Médico-technique — `MEDICO_TECHNICAL`

Laboratoire, imagerie, bloc technique ou autre plateau sans hébergement géré par ce module. Les chambres et lits sont interdits.

### Pharmacie — `PHARMACY`

Service pharmaceutique. Les chambres et lits sont interdits.

### Administratif — `ADMINISTRATIVE`

Caisse, accueil administratif, direction ou archives. Les chambres et lits sont interdits.

## Règles métier

1. Le type du service est obligatoire.
2. Le type n'est jamais calculé à partir du nom.
3. Seuls `HOSPITALIZATION` et `EMERGENCY` autorisent des chambres.
4. Un lit appartient obligatoirement à une chambre existante.
5. Une chambre appartient obligatoirement à un service autorisant les chambres.
6. Un service contenant une chambre ne peut pas être converti en type non spatial.
7. Une admission exige un lit existant, configuré et libre.
8. Une admission ne crée jamais de service, chambre ou lit.
9. La création d'une clinique ne crée aucune structure hospitalière générique.
10. Les contrôles sont effectués par le backend, même lorsque l'interface masque l'action.

## Parcours de configuration

### Création d'un service

L'administrateur saisit :

- le nom ;
- le type métier obligatoire.

Après création :

- un service spatial propose l'ajout de chambres ;
- un service non spatial affiche sa fonction et ne propose aucune action chambre/lit.

### Modification du type

Le changement est accepté lorsque la structure existante reste cohérente. Le passage d'un service contenant des chambres vers un type non spatial est refusé avec un message explicite.

### Admission

Le personnel sélectionne une structure préalablement configurée. Un libellé libre ou un lit absent ne déclenche aucun auto-provisioning.

## Expérience utilisateur

- le type apparaît à proximité du nom du service ;
- les services non spatiaux n'affichent pas une fausse zone « aucune chambre » ;
- l'action « Ajouter une chambre » n'existe que lorsque `allowsRooms=true` ;
- les messages d'erreur expliquent la règle métier ;
- les textes sont disponibles en français et en anglais ;
- les thèmes light/dark et le responsive respectent le design system.

## Critères d'acceptation

- La caisse ne peut contenir ni chambre ni lit.
- Le laboratoire, l'imagerie, la pharmacie et les consultations externes ne peuvent contenir ni chambre ni lit.
- L'hospitalisation et les urgences acceptent les chambres.
- Un appel REST contournant l'interface est refusé.
- L'ancien contrat sans type est rejeté.
- Aucun objet spatial n'est créé lors d'une admission invalide.
- Aucune structure par défaut n'est injectée dans une nouvelle clinique.
