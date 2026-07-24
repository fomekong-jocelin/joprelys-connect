# BUG-20260724 — Polish mobile Organisation / Structure hospitalière

## Contexte

Les écrans `/clinic/hospital-organization` et `/clinic/spatial/configuration` restent fonctionnels mais présentent sur mobile plusieurs écarts visuels incompatibles avec une démonstration client premium :

- actions du `PageHeader` forcées sur une seule ligne ;
- sélecteur d'établissement compressé jusqu'à ne laisser apparaître que la flèche ;
- boutons d'action de largeur incohérente ;
- breadcrumb technique dérivé du slug (`Clinic hospital-organization`, `Clinic spatial configuration`) ;
- sous-titre spatial encore issu de l'ancien modèle `service/chambre/capacité/lit`, alors que HOS-ORG/HOS-LOC sépare désormais organisation et géographie.

## Décision UX

Le `PageHeader` devient réellement mobile-first :

- bloc titre/subtitle sur toute la largeur ;
- zone d'actions empilée sous 768 px ;
- actions alignées horizontalement uniquement à partir d'un viewport suffisant ;
- aucun contrôle ne doit être réduit sous sa largeur lisible ;
- textes longs autorisés à revenir à la ligne ;
- conservation des tokens UI existants et absence d'Angular Material.

## Libellés corrigés

- breadcrumb FR : `Organisation hospitalière` ;
- breadcrumb FR : `Structure hospitalière` ;
- breadcrumb EN : `Hospital organization` / `Hospital structure` ;
- sous-titre Structure hospitalière : géographie, espaces physiques, rattachements unité↔espace et lits d'hospitalisation ;
- retrait de la mention future `HOS-STAFF-001-A` dans le descriptif des spécialités puisque HOS-STAFF est désormais fusionné.

## Seed de démonstration

Le fichier `scripts/demo/seed-trauma-center-hospital-config.sql` prépare un jeu de configuration cohérent pour `TRAUMA CENTER` :

- 4 pôles ;
- 10 services issus du catalogue contrôlé HOS-ORG ;
- Site principal → Bâtiment principal → RDC / Étage 1 → zones ;
- espaces Urgences, consultation, imagerie, laboratoire, pharmacie, bloc, SSPI, hospitalisation et réanimation ;
- profils d'hébergement uniquement sur `HOSPITAL_ROOM` et `ICU_ROOM` ;
- 5 lits initiaux ;
- rattachements datés `OrganizationalUnit ↔ Space`, dont plusieurs espaces volontairement partagés.

Le seed n'est pas une migration Flyway et ne crée aucun patient, compte ou donnée clinique. Il refuse de s'exécuter si `TRAUMA CENTER` n'existe pas ou si plusieurs organisations portent ce nom.

## Critères de validation

- [ ] build Angular production vert ;
- [ ] tests Angular verts ;
- [ ] sélecteur établissement lisible à 320/375/520 px ;
- [ ] aucune action de header compressée horizontalement ;
- [ ] breadcrumbs métier FR/EN ;
- [ ] vocabulaire spatial cohérent avec HOS-LOC ;
- [ ] seed SQL exécutable sur une base recette fraîchement migrée ;
- [ ] contrôle visuel avant déploiement RECETTE.

## Hors périmètre

- refonte du design system global ;
- modification du modèle HOS-ORG/HOS-LOC ;
- création automatique de comptes de démonstration ;
- données patients / visites / hospitalisations de démonstration.
