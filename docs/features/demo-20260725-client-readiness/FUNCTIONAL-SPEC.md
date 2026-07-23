# FUNCTIONAL-SPEC — Démonstration client du 25 juillet 2026

## 1. Objectif

Démontrer de façon cohérente le parcours clinique déjà livré dans Joprelys Connect, sans présenter comme finalisé un élément qui ne possède pas encore de validation humaine.

La démonstration doit montrer la continuité du soin plutôt qu'une succession d'écrans isolés.

## 2. Parcours principal — patient identifié

```text
Accueil
→ création/sélection patient
→ ouverture de visite
→ prise de constantes
→ consultation médecin
→ décision d'hospitalisation
→ service d'hospitalisation
→ chambre
→ lit
→ séjour visible
```

### Acteurs

- agent d'accueil ;
- infirmier ;
- médecin ;
- responsable hospitalisation ou profil habilité.

### Résultat attendu

Le client voit qu'un patient peut progresser de l'arrivée jusqu'au séjour hospitalier avec des responsabilités séparées et des données conservées dans son dossier.

## 3. Parcours secondaire — patient URG-TEMP

```text
Arrivée urgente sans identité fiable
→ création URG-TEMP
→ triage / constantes / réévaluation
→ données médico-légales si utiles à la démonstration
→ rapprochement d'identité
→ DPU canonique
→ continuité vers l'hospitalisation
→ conservation du lien avec l'urgence source
```

### Acteurs

- accueil urgence / infirmier ;
- médecin urgentiste ;
- responsable identité ou `ADMIN_CLINIQUE` habilité au rapprochement ;
- responsable hospitalisation.

### Résultat attendu

Le client voit que l'absence d'identité définitive ne bloque pas le soin et que la régularisation ultérieure ne casse pas la continuité clinique.

## 4. Données de démonstration

Préparer sans hardcoding applicatif :

- un patient identifié de démonstration ;
- un DPU existant pouvant servir de candidat de rapprochement ;
- un dossier URG-TEMP créé pendant la démonstration ou juste avant la répétition ;
- au moins un service `HOSPITALIZATION` ;
- une chambre disponible ;
- un lit `FREE` ;
- un médecin habilité ;
- les comptes métiers nécessaires, configurés par les mécanismes normaux de l'application.

Aucun identifiant UUID ne doit être demandé à l'opérateur pendant le parcours.

## 5. Ce qui ne doit pas être improvisé avant samedi

- nouveau modèle ABAC complet unité/relation de soin ;
- nouvelle architecture de délégation ;
- changement destructif supplémentaire du schéma ;
- réintroduction d'une permission globale hospitalisation ;
- écrans ou données factices uniquement destinés à masquer un manque fonctionnel.

Un défaut P0 démontré sur le parcours reste corrigeable, mais avec ticket, documentation, tests et PR dédiés.

## 6. Critères GO démo

- authentification des acteurs de démonstration fonctionnelle ;
- patient normal : parcours accueil → consultation → hospitalisation reproductible ;
- URG-TEMP : création → triage → rapprochement → hospitalisation reproductible ;
- service/chambre/lit cohérents avec le typage métier ;
- aucune erreur 5xx sur les deux parcours ;
- aucune fuite cross-tenant ;
- aucune permission legacy requise ;
- textes essentiels FR lisibles ;
- répétition générale réalisée sur l'environnement prévu pour la démonstration.

## 7. Critères NO-GO

- migration de la baseline non applicable ;
- impossibilité de se connecter avec les comptes de démonstration ;
- création patient/visite/urgence/hospitalisation bloquée par une régression ;
- perte de continuité après rapprochement ;
- lit disponible présenté comme occupable alors que le backend le refuse pour incohérence de structure ;
- erreur 500 reproductible sur une étape obligatoire ;
- autorisation contournable ou rôle de démonstration nécessitant un bypass.

## 8. Limites à annoncer honnêtement

La démonstration n'est pas une signature UAT globale du CDC V3.1 ni de l'EPIC-0027. Les validations avancées RSSI/DPO/médicales et les futurs contrôles contextuels ABAC restent des travaux distincts.
