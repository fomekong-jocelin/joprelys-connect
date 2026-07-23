# RUNBOOK CLIENT — Démonstration du 25 juillet 2026

> **Document canonique de référence** : `DEMO-PARCOURS-COMPLET.md`.
>
> Ce runbook ne remplace pas le guide complet. Il borne uniquement le scénario client du 25/07/2026 sur la baseline courante et ajoute le parcours URG-TEMP/hospitalisation livré après la version 1.0 du guide.

## 1. Périmètre retenu

### Parcours principal

```text
Accueil
→ patient
→ visite
→ constantes
→ consultation
→ hospitalisation
```

Pour les données de patient, constantes et consultation, réutiliser **Amélie Nkomo** et les valeurs déjà documentées dans `DEMO-PARCOURS-COMPLET.md`. Aucun second jeu de données concurrent n'est créé ici.

### Parcours secondaire

```text
Urgence — patient non identifié
→ URG-TEMP
→ triage / réévaluation
→ rapprochement vers un DPU existant
→ DPU canonique
→ hospitalisation avec continuité de l'urgence source
```

Ce parcours s'appuie sur STORY-2305 / PR #96 et STORY-2306 / PR #97 déjà fusionnées.

## 2. Acteurs nécessaires

Réutiliser les comptes documentés dans le guide complet lorsqu'ils existent :

- `AGENT_ACCUEIL` ;
- `INFIRMIER` ;
- `MEDECIN` ;
- `ADMIN_CLINIQUE` ou autre profil explicitement habilité au rapprochement ;
- profil possédant les permissions hospitalières dédiées nécessaires à l'admission.

La caisse n'est utilisée que si la finance différée est montrée après une répétition générale réussie.

Aucun mot de passe, OTP ou secret n'est ajouté au dépôt.

## 3. Préparation des données

### Données déjà documentées

Réutiliser dans `DEMO-PARCOURS-COMPLET.md` :

- **Amélie Nkomo** pour le parcours normal ;
- les constantes ;
- les données de consultation.

### Données supplémentaires pour samedi

Préparer par les fonctionnalités normales du produit :

- un DPU fictif existant pouvant servir de candidat de rapprochement ;
- un service de type `HOSPITALIZATION` ;
- une chambre de ce service ;
- au moins un lit `FREE` ;
- un médecin responsable valide.

Ne jamais créer de chambre sous un service `ADMINISTRATIVE`, `PHARMACY`, `OUTPATIENT` ou `MEDICO_TECHNICAL`.

## 4. Scénario principal

### A. Accueil

1. Connexion `AGENT_ACCUEIL`.
2. Créer ou retrouver Amélie Nkomo selon l'état de l'environnement.
3. Ouvrir la visite avec les données du guide complet.
4. Vérifier la présence dans la file de travail.

### B. Constantes

1. Connexion `INFIRMIER`.
2. Ouvrir la visite.
3. Saisir les constantes documentées dans le guide complet.
4. Vérifier leur persistance et leur disponibilité pour le médecin.

### C. Consultation

1. Connexion `MEDECIN`.
2. Démarrer la consultation depuis la visite.
3. Utiliser les données cliniques du guide complet.
4. Montrer la continuité patient/visite/constantes/consultation.

La prescription, pharmacie et laboratoire restent disponibles dans le guide complet mais sont hors du chemin critique de samedi sauf décision explicite pendant la répétition.

### D. Hospitalisation

1. Utiliser un profil possédant la permission d'admission dédiée.
2. Ouvrir l'hospitalisation depuis le parcours prévu.
3. Sélectionner le service `HOSPITALIZATION`.
4. Sélectionner la chambre.
5. Sélectionner un lit réellement `FREE`.
6. Confirmer l'admission.
7. Vérifier le séjour créé.

**Point client** : la structure service/chambre/lit est contrôlée métier et la disponibilité du lit est portée par le backend.

## 5. Scénario URG-TEMP

### A. Création et triage

1. Aller dans le workspace Urgences.
2. Déclencher « patient non identifié ».
3. Créer le dossier minimal URG-TEMP sans identité fictive complète.
4. Enregistrer triage/constantes utiles au scénario.
5. Montrer le code URG-TEMP et le bandeau d'identité provisoire.

**Point client** : le soin démarre avant la régularisation administrative et sans paiement préalable.

### B. Rapprochement

1. Depuis l'urgence, ouvrir le workspace de rapprochement.
2. Vérifier que le patient et l'urgence source sont présélectionnés.
3. Présenter le DPU fictif candidat.
4. Expliquer que le système propose des candidats mais ne fusionne jamais automatiquement.
5. Confirmer le rapprochement avec un profil habilité.
6. Ouvrir le DPU canonique obtenu.

### C. Continuité vers l'hospitalisation

1. Poursuivre vers l'hospitalisation depuis le parcours prévu.
2. Vérifier que l'urgence source reste liée.
3. Choisir service/chambre/lit valides.
4. Confirmer l'admission.
5. Vérifier le séjour sur le DPU canonique.

**Point client** : le changement d'identité de référence ne supprime pas l'historique créé pendant l'urgence.

## 6. Finance différée — démonstration optionnelle

Montrer uniquement après validation pendant la répétition :

- absence de blocage de soin faute de paiement ;
- état `REGULARIZATION_PENDING` lorsqu'applicable ;
- conservation des références historiques après rapprochement.

Ce point n'est pas nécessaire pour valider le cœur de la démonstration clinique de samedi.

## 7. GO / NO-GO

### GO

- comptes de démonstration disponibles ;
- visite et constantes reproductibles ;
- consultation reproductible ;
- service/chambre/lit compatibles préparés ;
- parcours hospitalisation normal réussi ;
- parcours URG-TEMP → rapprochement → hospitalisation réussi si celui-ci reste annoncé au client ;
- aucune erreur 5xx sur les étapes obligatoires ;
- aucun bypass RBAC ou permission legacy nécessaire.

### NO-GO ciblé

- rapprochement en erreur → retirer le scénario URG-TEMP plutôt que simuler une fusion ;
- hospitalisation en erreur → bloquant pour la démonstration annoncée ;
- structure hospitalière incohérente → corriger les données par les écrans normaux, jamais par SQL ad hoc ;
- rôle insuffisant → corriger le rôle par le mécanisme RBAC normal, jamais réintroduire un droit global.

## 8. Checklist 30 minutes avant rendez-vous

- [ ] aucun déploiement en cours ;
- [ ] URL de démonstration accessible ;
- [ ] comptes métier testés ;
- [ ] Amélie Nkomo disponible ou créable ;
- [ ] DPU candidat URG-TEMP disponible ;
- [ ] service `HOSPITALIZATION` disponible ;
- [ ] chambre disponible ;
- [ ] lit `FREE` disponible ;
- [ ] langue FR ;
- [ ] thème prévu ;
- [ ] parcours principal répété intégralement ;
- [ ] parcours URG-TEMP répété intégralement s'il reste dans le scope client ;
- [ ] aucun P0 non expliqué découvert pendant la répétition.

## 9. Règle après démonstration

Chaque retour client est qualifié séparément en besoin, bug, UX, paramétrage ou question métier avant création/modification de code.
