# Guide utilisateur — Joprelys Connect

> Version 1.0 — état fonctionnel de référence : branche `main`, 18 juillet 2026.

## 1. Objet du guide

Joprelys Connect centralise les fonctions administratives, cliniques, médico-techniques, financières et le portail patient. Ce guide décrit les parcours actuellement visibles dans l’application.

> Les menus et actions sont pilotés par permissions. Deux utilisateurs portant le même intitulé de poste peuvent donc voir des fonctions différentes.

## 2. Connexion

### Personnel

1. Choisir le mode **Personnel**.
2. Saisir l’e-mail professionnel et le mot de passe.
3. Cliquer sur **Se connecter**.
4. Saisir le code OTP lorsqu’il est demandé.

### Patient

1. Choisir le mode **Patient**.
2. Saisir le numéro patient global, le téléphone et la date de naissance.
3. Demander le code OTP.
4. Saisir le code reçu pour ouvrir le portail.

Ne partagez jamais un mot de passe ou un OTP. Chaque action est auditée sous le compte connecté.

## 3. Interface

- **Barre supérieure** : langue FR/EN, thème clair/sombre, profil et déconnexion.
- **Menu latéral** : modules autorisés pour le compte connecté.
- **Zone centrale** : listes, filtres, formulaires, panneaux de détail et messages.
- **Mobile** : le menu s’ouvre depuis l’icône dédiée.

## 4. Profils principaux

| Profil | Fonctions principales |
|---|---|
| Administrateur Joprelys / Super administrateur | Établissements, RBAC et fonctions plateforme selon périmètre |
| Administrateur clinique | Administration large de l’établissement hors fonctions plateforme et portail patient |
| DAF | Pilotage financier, créances, assurances, historique caisse et audit |
| Secrétaire comptable | Facturation, assurance et suivi comptable autorisé |
| Caissier | Session de caisse, encaissements, mouvements et clôture |
| Agent d’accueil | Patients, admissions, visites, registre et rendez-vous professionnels |
| Médecin | Dossier patient, urgences, consultation, prescriptions, examens, disponibilités et agenda |
| Infirmier | Patients, constantes, soins, urgences et hospitalisation selon permissions |
| Biologiste | File laboratoire, traitement et validation des résultats |
| Pharmacien | Vérification/dispensation des prescriptions et stocks pharmacie |
| Gestionnaire de stock | Consultation et gestion des stocks |
| Responsable hospitalisation | Hospitalisations, services, chambres et lits |
| Auditeur | Journaux et informations de contrôle autorisées |
| Patient | Portail personnel, rendez-vous et notifications |

## 5. Tableau de bord et file des visites

**Pour qui :** utilisateurs disposant de `VISIT_READ`.

1. Ouvrir **Tableau de bord**.
2. Repérer la visite dans la file active.
3. Ouvrir le détail de la visite.
4. Selon les permissions, saisir les constantes, démarrer la consultation ou clôturer la visite.

Constantes disponibles : température, poids, taille, pouls, tension, SpO2, glycémie, fréquence respiratoire et douleur.

## 6. Patients et admission unifiée

### Rechercher un patient

1. Ouvrir **Patients**.
2. Rechercher par nom ou numéro.
3. Ouvrir le dossier correspondant.
4. Utiliser les onglets Profil, Consultations, Hospitalisations, Analyses et Audit selon les droits.

### Admission normale

1. Cliquer sur **Nouvelle admission**.
2. Choisir **Parcours normal**.
3. Sélectionner un patient existant ou créer un nouveau patient.
4. Renseigner le motif, l’orientation et le service.
5. Contrôler le récapitulatif puis confirmer.

### Admission d’urgence

1. Choisir **Parcours urgence**.
2. Sélectionner un patient existant, nouveau ou provisoire.
3. Renseigner le mode d’arrivée et le tiers accompagnant le cas échéant.
4. Renseigner le triage, l’état hémodynamique, le motif principal et les constantes disponibles.
5. Confirmer pour ouvrir l’urgence.

Pour un patient non identifié, ne jamais inventer une identité : utiliser le mode provisoire.

### Accès d’urgence au dossier

1. Sélectionner le patient.
2. Choisir l’accès d’urgence.
3. Saisir une justification précise et factuelle.
4. Valider puis consulter uniquement les informations nécessaires.

L’accès d’urgence est exceptionnel et audité.

## 7. Registre d’accueil

1. Ouvrir **Registre d’accueil**.
2. Filtrer les entrées si nécessaire.
3. Cliquer sur **Nouvelle entrée**.
4. Choisir le type, saisir l’identité, la pièce, la personne/patient visé et le motif.
5. Enregistrer l’entrée.
6. Au départ, cliquer sur **Enregistrer le départ**.

Le registre d’accueil ne remplace pas une admission médicale.

## 8. Urgences

1. Ouvrir **Urgences** et sélectionner le dossier actif.
2. Consulter les onglets Vue d’ensemble, Identité, Soins et Contexte médico-légal selon les droits.
3. Ajouter les gestes et soins réalisés avec description, quantité et unité si nécessaire.
4. Mettre à jour le triage lorsque requis.
5. Stabiliser le patient et choisir l’orientation adaptée.

Documenter les actes immédiatement et sans altérer les faits.

## 9. Disponibilités et rendez-vous

### Définir les disponibilités du médecin

1. Ouvrir **Mes disponibilités**.
2. Ajouter une plage hebdomadaire : jour, début, fin et période de validité.
3. Enregistrer.
4. Ajouter les indisponibilités ponctuelles.
5. Contrôler l’aperçu des créneaux libres.

### Consulter Mon agenda

1. Ouvrir **Mon agenda**.
2. Naviguer entre les semaines.
3. Lire le patient, le numéro local, l’horaire, le statut et le motif.
4. Utiliser **Actualiser** si nécessaire ; la page se rafraîchit aussi automatiquement.

### Réserver côté patient

1. Ouvrir **Rendez-vous** dans le portail patient.
2. Filtrer les médecins par spécialité ou service.
3. Sélectionner un médecin.
4. Choisir un créneau disponible dans les 14 prochains jours.
5. Ajouter éventuellement un motif et confirmer.
6. Vérifier le rendez-vous dans **Mes rendez-vous**.

## 10. Consultation médicale

1. Depuis le tableau de bord, ouvrir la visite et cliquer sur **Démarrer la consultation**.
2. Contrôler les constantes et le numéro de visite.
3. Renseigner symptômes, examen clinique, diagnostics, conclusion, conseils et suivi.
4. Ajouter les lignes d’ordonnance.
5. Ajouter les demandes d’examens et leur priorité.
6. Sauvegarder, relire les documents puis clôturer selon le processus de l’établissement.

L’assistant vocal IA génère uniquement un brouillon. Le médecin doit relire, corriger et valider chaque information.

## 11. Laboratoire

1. Ouvrir **Laboratoire**.
2. Rechercher ou filtrer par priorité et statut.
3. Sélectionner la demande et contrôler patient, prescripteur, examens et motif.
4. Mettre à jour le statut.
5. Saisir les analytes, valeurs, unités, références et interprétations.
6. Ajouter la conclusion, le validateur et les dates.
7. Enregistrer en brouillon ou valider selon la responsabilité.

Un résultat `CRITICAL` doit suivre le protocole d’alerte de l’établissement.

## 12. Pharmacie et stocks

### Vérifier et dispenser une prescription

1. Ouvrir **Prescriptions pharmacie**.
2. Saisir le numéro d’ordonnance et le code PIN.
3. Vérifier le patient, le prescripteur, la validité et les quantités déjà dispensées.
4. Enregistrer la délivrance et les substitutions autorisées.

### Gérer les stocks

1. Ouvrir **Stocks**.
2. Utiliser le filtre d’alertes pour les produits sous seuil.
3. Contrôler quantité, seuil, lot et péremption.
4. Ajouter ou modifier une fiche de stock selon les permissions.

## 13. Hospitalisation et espaces

1. Ouvrir **Hospitalisation / Espaces**.
2. Sélectionner le service.
3. Consulter le total de lits, les lits occupés, libres et le taux d’occupation.
4. Contrôler les chambres et le statut de chaque lit.
5. Selon les droits, passer un lit en nettoyage, maintenance ou libre.
6. Utiliser **Configuration des espaces** uniquement si autorisé.

Un lit occupé ne doit pas être remis libre manuellement sans le processus de sortie ou transfert.

## 14. Facturation, caisse et assurance

1. Ouvrir **Facturation**.
2. Rechercher et sélectionner le patient.
3. Choisir la visite et la convention d’assurance.
4. Contrôler les prestations pré-calculées, quantités, coefficients et prix.
5. Enregistrer la facture et consulter son historique.
6. Pour encaisser, vérifier qu’une session de caisse est ouverte.
7. Utiliser Créances, Bordereaux, Conventions/Tarifs ou DAF selon les permissions.

Fonctions associées :

- partage patient/assurance ;
- historique et détail de facture ;
- sessions et clôture de caisse ;
- créances et relances ;
- bordereaux assurance ;
- tableau de bord DAF et exports.

## 15. Administration du personnel et des droits

### Personnel

1. Ouvrir **Personnel**.
2. Inviter ou modifier un collaborateur.
3. Saisir le nom, l’e-mail et au moins un rôle.
4. Pour un médecin, compléter spécialité, numéro d’inscription, service, signature et cachet.
5. Activer ou désactiver le compte si nécessaire.

### Rôles et permissions

1. Ouvrir **Rôles et permissions**.
2. Affecter les rôles aux utilisateurs.
3. Créer un rôle personnalisé ou dupliquer un rôle système.
4. Ajuster les permissions.
5. Consulter l’onglet Audit.

Appliquer le principe du moindre privilège.

## 16. Portail patient

Le portail patient propose les rubriques suivantes :

- Tableau de bord ;
- Rendez-vous ;
- Profil ;
- Synthèse ;
- Prescriptions ;
- Résultats ;
- Documents ;
- QR code ;
- Consentements ;
- Confidentialité ;
- Audit ;
- Demandes ;
- Notifications.

Le patient voit uniquement ses propres données.

## 17. Bonnes pratiques de sécurité

- Utiliser uniquement son compte nominatif.
- Verrouiller le poste en cas d’absence.
- Vérifier l’identité du patient avant toute saisie ou impression.
- Ne pas copier les données médicales dans une messagerie non approuvée.
- Ne pas photographier l’écran avec un téléphone personnel.
- Limiter l’accès d’urgence au strict besoin.
- Ne jamais inscrire mot de passe, OTP ou clé API dans le dossier patient.
- Signaler tout accès ou comportement inhabituel.

## 18. Dépannage

| Situation | Action recommandée |
|---|---|
| Menu absent | Demander la vérification des permissions |
| Accès non autorisé | Se reconnecter puis faire vérifier les rôles |
| OTP invalide | Utiliser le code le plus récent et recommencer la demande |
| Patient introuvable | Vérifier l’identité et éviter de créer un doublon |
| Créneau disparu | Actualiser : il a probablement été réservé |
| Visite absente | Vérifier si elle a été clôturée |
| Erreur serveur | Noter l’heure et l’action puis contacter le support sans capturer de données sensibles |
| Données incohérentes | Utiliser le processus métier ou demander l’aide d’un responsable |

## 19. Glossaire

- **OTP** : code à usage unique.
- **RBAC** : gestion des accès par rôles et permissions.
- **Visite** : épisode de prise en charge ouvert pour un patient.
- **Patient provisoire** : patient d’urgence non identifié, géré sans identité inventée.
- **Triage** : évaluation initiale de la gravité.
- **Dispensation** : délivrance tracée par la pharmacie.
- **Convention d’assurance** : règles de couverture et de partage du montant.
- **Bordereau** : regroupement de créances transmis à l’assureur.
- **Audit** : journal des accès et modifications importantes.

## 20. Livrable éditorial

La version Word complète, illustrée et prête à diffusion est produite séparément. La présente source Markdown est la version maintenable dans le dépôt.
