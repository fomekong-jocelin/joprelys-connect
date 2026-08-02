# TEST PLAN — Dossier patient mobile premium

## Tests automatisés

### Domaine RBAC

- normalisation des rôles et permissions ;
- vérification insensible à la casse ;
- absence sûre des collections de permissions.

### Domaine patient

- repli sur les informations de l’annuaire lorsque la réponse patient est partielle ;
- compatibilité des alias backend pour les allergies ;
- normalisation des niveaux `LOW`, `MODERATE`, `HIGH` et `CRITICAL` ;
- conservation de la visite active dans la cible du dossier.

### Gates Flutter

- `dart format --output=none --set-exit-if-changed .`
- `flutter analyze`
- `flutter test`

## Matrice de permissions

| Profil de test | Résultat attendu |
|---|---|
| Sans `PATIENT_READ` | aucune destination Patients |
| `PATIENT_READ` seul | identité et aperçu uniquement |
| + `CLINICAL_READ` | informations médicales et consultations |
| + `LAB_ORDER_READ` | section laboratoire |
| + `HOSPITALIZATION_READ` | section hospitalisations |
| + `AUDIT_READ` | section traçabilité |
| Sans `VISIT_READ` | file active non chargée |

## Recette réseau

- identité patient disponible ;
- collections vides ;
- collection optionnelle en 404 ;
- session expirée en 401 ;
- consentement requis en 403 ;
- activation Break-Glass réussie ;
- activation Break-Glass refusée ;
- perte réseau et reprise.

## Recette visuelle

Tester en français et en anglais, en light et dark, sur :

- 360 × 800 ;
- 412 × 915 ;
- tablette 800 px ou plus.

Vérifier :

- aucun débordement ;
- menu bas lisible et stable ;
- rail affiché sur tablette ;
- en-tête patient compact ;
- allergies critiques visibles dans l’aperçu ;
- cartes et contrastes cohérents ;
- bouton retour Android ;
- état conservé lors du changement d’onglet.

## Recette sur appareil Android

1. Se connecter avec un compte clinique autorisé.
2. Ouvrir Patients depuis le menu bas.
3. Rechercher un patient puis ouvrir son dossier.
4. Parcourir toutes les sections autorisées.
5. Ouvrir le même dossier depuis la file active.
6. Tester un patient exigeant un consentement.
7. Activer le Break-Glass avec une justification.
8. Passer du thème clair au thème sombre.
9. Passer du français à l’anglais.
10. Fermer et rouvrir le dossier pour vérifier la stabilité.
