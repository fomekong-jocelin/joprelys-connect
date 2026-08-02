# FUNCTIONAL SPEC — Dossier patient mobile premium

## Objectif

Donner aux professionnels autorisés un accès mobile au dossier patient avec le même niveau fonctionnel que le frontend web, sans contourner le consentement, le RBAC ni le Break-Glass.

## Navigation principale

L’espace professionnel comporte une navigation persistante :

- **Accueil** : file de prise en charge lorsque `VISIT_READ` est accordé ;
- **Patients** : annuaire et accès aux dossiers lorsque `PATIENT_READ` est accordé ;
- **Profil** : compte, sécurité et synthèse des permissions effectives.

Sur téléphone, les destinations sont présentées dans un menu bas. Sur tablette, elles sont présentées dans un rail latéral. Une destination non autorisée n’est jamais rendue.

## Dossier patient

Le dossier s’ouvre en plein écran depuis l’annuaire ou depuis une visite active. Il contient un en-tête compact et des sections horizontales adaptées au mobile.

### Aperçu

Toujours accessible après autorisation `PATIENT_READ` :

- identité et références patient ;
- date de naissance, sexe, téléphone et groupe sanguin ;
- statut et indicateur d’accès d’urgence ;
- allergies critiques visibles sans ouvrir une autre section ;
- compteurs des données disponibles.

### Informations médicales

Visible uniquement avec `CLINICAL_READ` :

- antécédents ;
- allergies et niveau de gravité ;
- vaccinations.

### Consultations

Visible uniquement avec `CLINICAL_READ` :

- date et numéro de visite ;
- motif ou diagnostic disponible ;
- praticien ;
- constantes principales.

### Laboratoire

Visible uniquement avec `LAB_ORDER_READ` :

- demandes d’examens ;
- résultats biologiques ;
- interprétation et valeurs de référence lorsqu’elles existent.

### Hospitalisations

Visible uniquement avec `HOSPITALIZATION_READ` :

- service ;
- statut du séjour ;
- motif ;
- dates disponibles.

### Traçabilité

Visible uniquement avec `AUDIT_READ` :

- action ;
- acteur ;
- statut ;
- date ;
- justification disponible.

## Accès protégé et Break-Glass

Lorsqu’un dossier répond en `403`, aucune donnée clinique n’est affichée. L’utilisateur voit une surface d’accès protégé et peut saisir une justification d’urgence. La demande est envoyée à l’endpoint Break-Glass existant, puis le dossier est rechargé.

## Internationalisation et thèmes

- français et anglais ;
- dates formatées selon la locale ;
- aucune couleur métier dépendante d’un thème fixe ;
- utilisation de `ColorScheme` et des design tokens ;
- compatibilité light/dark ;
- libellés courts et sans débordement sur écran de 360 px.

## Critères d’acceptation

- [x] menu bas persistant sur téléphone ;
- [x] rail de navigation sur tablette ;
- [x] destinations filtrées par permissions effectives ;
- [x] dossier plein écran depuis la file et l’annuaire ;
- [x] sections filtrées par permission ;
- [x] gestion du consentement et du Break-Glass ;
- [x] états chargement, vide, erreur et accès refusé ;
- [x] support light/dark et FR/EN ;
- [ ] validation automatisée Flutter verte ;
- [ ] recette Android physique.
