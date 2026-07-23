# USER-GUIDE — Runbook démonstration client du 25 juillet 2026

## 1. Objectif de la démonstration

Présenter un parcours clinique cohérent de bout en bout, puis montrer la capacité de Joprelys à gérer une urgence sans identité fiable et à rétablir la continuité vers le DPU et l'hospitalisation.

Le discours doit rester centré sur la valeur métier et non sur la technique interne.

## 2. Acteurs à préparer

Préparer des comptes distincts utilisant les mécanismes normaux de l'application :

1. **Accueil** — création/recherche patient et ouverture de visite ;
2. **Infirmier** — constantes et triage ;
3. **Médecin** — consultation et décision clinique ;
4. **Responsable identité / ADMIN_CLINIQUE habilité** — rapprochement URG-TEMP ;
5. **Responsable hospitalisation** — admission/service/chambre/lit ;
6. **Caisse** — uniquement si la finance différée est montrée.

Ne documenter aucun mot de passe ou secret dans le dépôt.

## 3. Données à préparer

### Patient A — parcours normal

Dossier fictif de démonstration, avec identité cohérente et informations suffisantes pour l'accueil.

Objectif : montrer le parcours `accueil → visite → constantes → consultation → hospitalisation`.

### Patient B — candidat DPU existant

Dossier fictif existant permettant de démontrer le rapprochement d'un futur URG-TEMP.

Objectif : disposer d'un candidat crédible mais sans utiliser de donnée réelle.

### Structure hospitalière

Préparer :

- un service de type `HOSPITALIZATION` ;
- une chambre ;
- au moins un lit `FREE` ;
- un médecin responsable disponible dans les listes normales de l'application.

Ne pas créer de chambre sous une caisse, pharmacie, laboratoire ou service administratif.

## 4. Scénario 1 — parcours normal

### Étape 1 — Accueil

- se connecter avec le profil Accueil ;
- rechercher le Patient A ;
- s'il n'existe pas dans l'environnement de répétition, le créer avec le workflow normal ;
- ouvrir une visite.

**Message client** : le dossier patient devient le point de continuité de toutes les étapes suivantes.

### Étape 2 — Constantes

- se connecter avec le profil Infirmier ;
- ouvrir la visite/patient ;
- saisir les constantes prévues pour la démonstration ;
- vérifier leur affichage dans le dossier.

**Message client** : les données prises à l'accueil clinique sont disponibles pour le médecin sans ressaisie.

### Étape 3 — Consultation

- se connecter avec le profil Médecin ;
- ouvrir la consultation ;
- saisir motif, observations et éléments cliniques nécessaires ;
- valider la consultation selon le workflow courant.

**Message client** : le médecin travaille dans le même dossier et enrichit la timeline clinique.

### Étape 4 — Hospitalisation

- ouvrir l'action d'hospitalisation avec un profil habilité ;
- choisir le service `HOSPITALIZATION` ;
- choisir la chambre ;
- choisir un lit libre ;
- confirmer l'admission ;
- vérifier le séjour.

**Message client** : la structure hospitalière n'est pas un simple libellé ; les règles de service, chambre, lit et disponibilité sont contrôlées.

## 5. Scénario 2 — urgence sans identité fiable

### Étape 1 — Création URG-TEMP

- ouvrir l'espace Urgences ;
- choisir l'action de patient non identifié ;
- créer le dossier minimal sans inventer d'identité complète ;
- montrer le code URG-TEMP.

**Message client** : le soin n'attend ni pièce d'identité ni règlement préalable.

### Étape 2 — Triage

- saisir le triage et les constantes disponibles ;
- montrer la chronologie et les réévaluations si utiles au scénario.

### Étape 3 — Rapprochement

- ouvrir le rapprochement depuis l'urgence ;
- présenter le Patient B comme candidat ;
- expliquer que le système propose mais ne fusionne jamais automatiquement ;
- confirmer le rapprochement avec un profil habilité ;
- ouvrir le DPU canonique obtenu.

**Message client** : l'identité est régularisée sans effacer l'historique créé pendant l'urgence.

### Étape 4 — Continuité hospitalisation

- poursuivre vers l'hospitalisation depuis le parcours prévu ;
- conserver le lien avec l'urgence source ;
- sélectionner le service/chambre/lit ;
- confirmer le séjour.

**Message client** : l'urgence, la régularisation et l'hospitalisation font partie d'une même continuité de prise en charge.

## 6. Finance différée — optionnel pendant la démo

La montrer uniquement si la répétition générale est stable :

- l'urgence ne nécessite pas de paiement initial ;
- la facture peut rester en régularisation ;
- le rapprochement ne doit pas renuméroter ni dupliquer les éléments financiers.

Ne pas transformer ce point en démonstration complète de la comptabilité si ce n'est pas l'objectif du rendez-vous.

## 7. Plan de repli fonctionnel

Un plan de repli ne doit jamais masquer un défaut.

- Si une section secondaire lente ou non critique ne répond pas : revenir au dossier patient et poursuivre le parcours principal déjà validé.
- Si la finance différée est instable : ne pas la montrer et consigner le défaut ; le parcours clinique principal reste la démonstration centrale.
- Si le rapprochement échoue : **NO-GO pour le scénario URG-TEMP** ; ne pas simuler manuellement une fusion.
- Si l'hospitalisation échoue : **NO-GO sur le parcours principal**, car elle fait partie du périmètre annoncé.
- Si un bypass de permission serait nécessaire : **NO-GO**, ne pas élargir le rôle pour sauver la démonstration.

## 8. Checklist 30 minutes avant la démonstration

- [ ] URL de démonstration accessible ;
- [ ] comptes acteurs fonctionnels ;
- [ ] service d'hospitalisation présent ;
- [ ] chambre présente ;
- [ ] lit libre ;
- [ ] Patient A disponible/créable ;
- [ ] Patient B disponible comme candidat ;
- [ ] aucun déploiement en cours ;
- [ ] navigateur propre et onglets préparés ;
- [ ] langue FR sélectionnée ;
- [ ] thème choisi et stable ;
- [ ] répétition du parcours principal réussie ;
- [ ] répétition URG-TEMP réussie ou explicitement retirée du scope avant le rendez-vous.

## 9. Après la démonstration

Consigner les retours client séparément :

- besoin nouveau ;
- anomalie ;
- incompréhension UX ;
- demande de paramétrage ;
- question métier ;
- décision de priorité.

Ne pas convertir automatiquement chaque remarque en changement de code sans refinement.
