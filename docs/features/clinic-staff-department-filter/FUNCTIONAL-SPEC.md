# Spécification Fonctionnelle — Menu déroulant des services et filtrage des praticiens lors de l'admission

## 1. Description du besoin
Afin d'améliorer la saisie des services/départements cliniques et d'éviter les erreurs dues à la saisie de texte libre (ex: orthographes différentes pour "Pédiatrie"), le champ "Département / Service" dans les fiches collaborateurs et "Service clinique" dans l'admission patient doivent être guidés par un menu déroulant (dropdown).

De plus, lors de l'admission d'une visite patient, le choix du service clinique doit filtrer dynamiquement la liste des praticiens disponibles sous "Praticien responsable" pour ne lister que les professionnels affectés à ce service/département spécifique.

## 2. Parcours utilisateur
1. **Gestion du personnel / Mon Profil** :
   - L'administrateur ou l'utilisateur voit un menu déroulant pour sélectionner son service parmi une liste prédéfinie (Médecine générale, Pédiatrie, Gynécologie, Urgences, Pharmacie, Laboratoire, Cardiologie).
   - Une option "Autre" est disponible. Si sélectionnée, elle affiche un champ de texte libre pour saisir un service sur mesure.

2. **Admission patient (Ouverture de visite)** :
   - Le champ "Service clinique" affiche un menu déroulant regroupant la liste des services prédéfinis combinée à tous les services saisis pour les collaborateurs actifs de l'établissement (élimination des doublons).
   - Une option "Autre" permet de saisir une orientation sur mesure.
   - Dès qu'un service est sélectionné, le champ "Praticien responsable" se met à jour en n'affichant que les praticiens dont le département correspond à ce service.
   - Si aucun praticien n'est configuré pour ce service, la liste repasse au filtrage par défaut (basé sur l'orientation) afin de ne pas bloquer l'admission.

## 3. Rôles et habilitations
- **ADMIN_CLINIQUE** : Peut configurer les services des collaborateurs.
- **Tout collaborateur connecté** : Peut sélectionner son service dans sa propre fiche profil.
- **Agent d'accueil / Infirmier / Médecin** : Peut initier une visite patient et sélectionner le service clinique.
