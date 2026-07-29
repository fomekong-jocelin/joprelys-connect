# FUNCTIONAL SPEC — Liste Patients mobile-first

## Problème métier

Un professionnel doit pouvoir identifier le bon patient en quelques secondes. La liste actuelle consomme trop d’espace vertical avant les premiers résultats et répète des actions de navigation sur chaque carte.

## Parcours cible

### Arrivée sur Patients

L’utilisateur voit immédiatement :

1. le titre `Patients` ;
2. une recherche `Nom, téléphone ou DPU` ;
3. l’action `Nouvelle admission` ;
4. la liste des dossiers disponibles.

Le breadcrumb reste la navigation vers le niveau précédent ; aucun lien `Retour` supplémentaire n’est affiché dans le PageHeader.

### Recherche

- Enter déclenche la recherche existante ;
- aucun bouton `Rechercher` séparé ne prend de largeur ;
- lorsqu’une requête est présente, une action `X` permet de l’effacer puis de recharger la liste sans filtre ;
- pendant une recherche, le nombre de résultats est affiché ;
- hors recherche, la rubrique est simplement nommée `Liste des patients`.

### Carte patient mobile

La carte est entièrement interactive et affiche uniquement les informations nécessaires pour sélectionner le bon dossier :

- nom ;
- sexe ;
- DPU ;
- téléphone ;
- ville ;
- chevron d’ouverture.

Le bouton `Voir le dossier` n’est plus affiché sur mobile : cliquer/toucher la carte ouvre le dossier.

### Desktop

Le tableau existant reste le mode de lecture desktop. Les numéros DPU et local restent non coupés et l’action explicite `Voir le dossier` peut rester dans la colonne Actions.

## Règles de sécurité et métier

- aucune donnée patient supplémentaire n’est chargée ;
- aucune permission n’est ajoutée ou retirée ;
- l’admission continue d’utiliser `UnifiedAdmissionComponent` et ses contrats existants ;
- l’ouverture d’un dossier conserve le même routage et le même mécanisme de consentement / Break-Glass ;
- aucun ordre « récent » n’est affiché car l’API sans filtre ne garantit pas un tri par récence.

## Internationalisation

Libellés cibles :

| Clé | FR | EN |
|---|---|---|
| `patients.title` | Patients | Patients |
| `patients.subtitle` | Rechercher et gérer les dossiers patients. | Search and manage patient records. |
| `patients.searchPlaceholder` | Nom, téléphone ou DPU | Name, phone or DPU |
| `patients.listHeading` | Liste des patients | Patient list |
| `patients.resultOne` | patient trouvé | patient found |
| `patients.resultsMany` | patients trouvés | patients found |
| `patients.clearSearch` | Effacer la recherche | Clear search |

## Critères d’acceptation

- [ ] Les premiers résultats apparaissent nettement plus haut sur un téléphone 360–430 px.
- [ ] Le titre ne revient pas sur deux lignes dans les viewports courants.
- [ ] La recherche et l’admission ne se concurrencent pas visuellement.
- [ ] Une carte patient mobile tient dans une hauteur compacte et s’ouvre au toucher.
- [ ] Aucun bouton interne redondant n’est nécessaire pour ouvrir une carte mobile.
- [ ] Le tableau desktop est inchangé fonctionnellement.
