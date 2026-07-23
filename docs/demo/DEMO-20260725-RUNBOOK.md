# RUNBOOK — Démonstration client Joprelys du 25 juillet 2026

## 1. Baseline

- Repository : `fomekong-jocelin/joprelys-connect`
- Baseline de cadrage : `81b7d20c4cf44800e436c86b964a38bc62929305`
- Version Flyway du code : V86
- Environnement réellement utilisé : **à renseigner avant répétition**
- SHA déployé : **à renseigner**
- Version Flyway installée : **à renseigner**
- Date/heure du dernier contrôle : **à renseigner**

La démonstration est NO-GO tant que le SHA déployé et la version Flyway ne sont pas renseignés et cohérents.

## 2. Parcours présenté

`Accueil → URG-TEMP → triage → médico-légal → documents → rapprochement DPU → hospitalisation`

La consultation classique et les constantes d'un patient identifié peuvent être montrées en introduction, mais le fil narratif principal reste la continuité d'un patient non identifié jusqu'au séjour hospitalier.

## 3. Message métier

> Joprelys permet de commencer les soins sans attendre l'identification ou le paiement, tout en conservant la traçabilité, la provenance des données, les documents, les actes et la continuité vers le dossier patient définitif et l'hospitalisation.

Ne pas annoncer :

- une automatisation non vérifiée sur l'environnement ;
- une validation réglementaire/DPO non signée ;
- un contrôle ABAC unité/relation de soin encore porté par HOS-STAFF-001 ;
- une clearance complète encore portée par HOS-DIS-001.

## 4. Comptes nécessaires

| Profil | Usage pendant la démo | Compte prêt | Connexion testée |
|---|---|---:|---:|
| Accueil / ADMIN_CLINIQUE | amorce, recherche patient, navigation | ☐ | ☐ |
| Urgence / infirmier | URG-TEMP, triage, soins | ☐ | ☐ |
| Médecin | consultation, décision clinique | ☐ | ☐ |
| Responsable hospitalisation | admission et mouvement | ☐ | ☐ |
| Caisse, uniquement si montrée | régularisation financière | ☐ | ☐ |

Aucun identifiant ou mot de passe ne doit apparaître dans ce document.

## 5. Données nécessaires

| Donnée | Préparée | Vérifiée |
|---|---:|---:|
| établissement de démo actif | ☐ | ☐ |
| service urgence | ☐ | ☐ |
| service hospitalisation | ☐ | ☐ |
| chambre active | ☐ | ☐ |
| deux lits actifs dont un libre | ☐ | ☐ |
| patient canonique de rapprochement | ☐ | ☐ |
| dossier de secours déjà rapproché | ☐ | ☐ |
| dossier de secours déjà hospitalisable | ☐ | ☐ |

## 6. Vérifications techniques avant répétition

- [ ] URL accessible ;
- [ ] certificat HTTPS valide ;
- [ ] backend répond ;
- [ ] frontend charge sans erreur bloquante ;
- [ ] SHA déployé identifié ;
- [ ] Flyway au niveau attendu ;
- [ ] aucune migration failed ;
- [ ] aucun service en boucle de redémarrage ;
- [ ] les comptes de démonstration se connectent ;
- [ ] le navigateur ne conserve pas une ancienne session ou d'anciennes permissions ;
- [ ] les permissions sont rechargées après changement de rôle ;
- [ ] le lit de démonstration est libre ;
- [ ] le patient canonique de rapprochement est disponible ;
- [ ] aucun secret visible dans l'écran, les logs ou les documents préparés.

## 7. Script de démonstration

### Étape A — Situation clinique

1. Présenter un patient inconscient arrivé sans pièce d'identité.
2. Ouvrir le workspace urgence.
3. Sélectionner « patient non identifié ».
4. Créer le dossier minimal.
5. Montrer le code URG-TEMP et le bandeau d'identité provisoire.

### Étape B — Soins sans blocage administratif

1. Enregistrer le triage et les constantes.
2. Ajouter une donnée de réanimation ou de soin.
3. Renseigner un tiers/accompagnant si le scénario le prévoit.
4. Montrer qu'aucun paiement initial ne bloque les soins.

### Étape C — Documents et traçabilité

1. Ouvrir l'espace Documents.
2. Générer une fiche disponible dans le scénario.
3. Montrer numéro, version et preuve d'intégrité.
4. Réexécuter l'action prévue comme idempotente et vérifier l'absence de doublon imprévu.

### Étape D — Rapprochement

1. Ouvrir l'assistant de rapprochement.
2. Sélectionner le patient canonique préparé.
3. Confirmer la décision.
4. Ouvrir le DPU canonique.
5. Montrer que le code URG-TEMP et la provenance restent visibles.

### Étape E — Hospitalisation

1. Poursuivre depuis l'urgence vers l'hospitalisation.
2. Choisir le service hospitalisation.
3. Choisir une chambre et le lit libre.
4. Valider l'admission.
5. Ouvrir le séjour actif.
6. Montrer la séparation des droits médecin, infirmier et responsable hospitalisation si les comptes ont été validés.

## 8. Scénario de secours

| Incident | Réponse visible | Reprise |
|---|---|---|
| création URG-TEMP impossible | expliquer qu'un dossier préparé sert de secours | ouvrir le dossier URG-TEMP de secours |
| rapprochement impossible | montrer la file à régulariser et la traçabilité | ouvrir le dossier déjà rapproché |
| lit devenu indisponible | expliquer le contrôle anti-double-affectation | choisir le second lit préparé |
| session/permissions incohérentes | ne pas bricoler les rôles devant le client | se déconnecter, purger la session et utiliser le compte de secours |
| erreur réseau | conserver l'écran et expliquer le rejeu contrôlé | basculer vers le dossier préparé, sans modifier la base |

## 9. Chronométrage

| Passage | Durée cible | Durée réelle | Résultat |
|---|---:|---:|---|
| répétition 1 | 15–20 min | — | — |
| répétition 2 | 15–20 min | — | — |
| démonstration client | 20–30 min avec échanges | — | — |

## 10. Décision finale

### GO

- [ ] deux répétitions successives terminées ;
- [ ] aucune manipulation SQL/serveur nécessaire pendant le parcours ;
- [ ] tous les comptes et données de secours sont disponibles ;
- [ ] aucune étape non vérifiée n'est incluse ;
- [ ] scénario et discours métier maîtrisés.

### NO-GO / Réduction de périmètre

Toute étape non reproductible deux fois est retirée du parcours principal et remplacée par une présentation limitée à la preuve réellement vérifiée.

## 11. Bilan après démonstration

À compléter après le rendez-vous :

- réactions du client ;
- points compris immédiatement ;
- questions récurrentes ;
- défauts observés ;
- demandes nouvelles ;
- décisions produit ;
- tickets à créer ou à réordonner.
