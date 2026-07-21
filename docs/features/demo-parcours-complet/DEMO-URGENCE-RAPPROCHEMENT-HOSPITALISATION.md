# Guide de démo — Urgence, rapprochement et hospitalisation

- Date de préparation : 2026-07-21
- Environnement cible : `https://recette.joprelys.com`
- Référence d'audit : #92
- Statut : scénario démontrable avec navigation manuelle contrôlée

## 1. Message à présenter

Joprelys Connect permet de :

1. prendre en charge un patient inconscient ou non identifié sans inventer son identité ;
2. générer un code URG-TEMP ;
3. effectuer le triage et documenter les soins immédiats ;
4. régulariser et rapprocher l'identité avec un DPU existant ;
5. poursuivre la prise en charge sur le DPU canonique ;
6. ouvrir une visite et hospitaliser le patient sur un lit réellement configuré.

La démo ne doit pas présenter cette séquence comme un assistant automatique unique. Le passage entre rapprochement, visite et hospitalisation nécessite encore une navigation métier explicite.

## 2. Préparation des données

### Comptes et permissions

Préparer les sessions suivantes :

| Profil | Permissions minimales |
|---|---|
| Accueil/urgence | `PATIENT_WRITE`, `EMERGENCY_READ`, `EMERGENCY_WRITE` |
| Régularisation | `PATIENT_MERGE` |
| Hospitalisation | `PATIENT_READ`, `VISIT_WRITE`, `HOSPITALIZATION_READ`, `HOSPITALIZATION_MANAGE` |

Un compte `ADMIN_CLINIQUE` correctement habilité peut servir de compte de secours, mais la démonstration gagne en crédibilité lorsque les responsabilités sont séparées.

### Structure hospitalière

Configurer avant la présentation :

- un service de type `HOSPITALIZATION` ;
- une chambre ;
- un lit libre ;
- idéalement un second lit libre de secours ;
- un médecin responsable actif.

Ne pas créer la caisse, le laboratoire, la pharmacie ou la consultation comme services d'hébergement. Ces types n'autorisent plus les chambres.

### Patients

Préparer :

- un DPU vérifié qui servira de candidat au rapprochement ;
- des données d'identité suffisamment proches de celles qui seront déclarées sur le dossier URG-TEMP ;
- une preuve et une justification fictives clairement identifiées comme données de recette.

## 3. Déroulé

### Étape 1 — Créer l'urgence provisoire

1. Ouvrir **Nouvelle admission**.
2. Choisir **Parcours urgence**.
3. Choisir **Patient provisoire**.
4. Renseigner uniquement les éléments réellement disponibles : sexe apparent, tranche d'âge, description, lieu, mode d'arrivée et motif observable.
5. Renseigner le triage initial.
6. Valider.
7. Montrer le code `URG-TEMP-...` généré.

**Point de valeur :** aucun faux nom, téléphone ou date de naissance n'est imposé pour commencer les soins.

### Étape 2 — Montrer le triage et la réévaluation

1. Ouvrir **Urgences**.
2. Sélectionner le dossier créé.
3. Montrer l'évaluation initiale ABCDE.
4. Ajouter une réévaluation ou un geste de soins fictif.
5. Montrer l'horodatage et l'historique append-only.

**Point de valeur :** les soins et réévaluations commencent avant la régularisation administrative.

### Étape 3 — Régulariser et rapprocher l'identité

1. Compléter les informations d'identité lorsque le patient reprend conscience ou lorsqu'une preuve fiable est disponible.
2. Ouvrir **Rapprochement patient**.
3. Sélectionner le dossier URG-TEMP.
4. Afficher les candidats et expliquer le score.
5. Sélectionner le DPU préparé.
6. Renseigner la source, la référence de preuve et la justification de recette.
7. Confirmer le rapprochement.

**Point de valeur :** aucune fusion automatique ; la décision est humaine, justifiée, idempotente et auditée. Le code URG-TEMP reste un alias.

### Étape 4 — Ouvrir le DPU canonique

1. Aller dans **Patients**.
2. Rechercher le DPU canonique ou l'alias URG-TEMP.
3. Ouvrir le dossier patient retourné.
4. Vérifier que l'identité affichée est l'identité canonique.

Le succès du rapprochement ne redirige pas encore automatiquement vers le DPU final ; cette navigation est volontairement réalisée par l'opérateur pendant la démo.

### Étape 5 — Ouvrir une visite

1. Depuis le DPU canonique, cliquer sur **Ouvrir une visite**.
2. Renseigner le motif et le service.
3. Choisir le médecin responsable.
4. Valider la visite.

Cette étape est nécessaire dans l'état actuel : le dossier d'urgence n'est pas encore directement utilisé comme `visitId` par l'admission hospitalière.

### Étape 6 — Hospitaliser

1. Ouvrir l'onglet **Hospitalisations** du patient.
2. Cliquer sur l'action d'admission.
3. Sélectionner la visite créée à l'étape précédente.
4. Sélectionner le service `HOSPITALIZATION`.
5. Choisir la chambre et le lit libre.
6. Renseigner le motif d'admission et le médecin responsable.
7. Valider.
8. Montrer le séjour actif, le lit occupé et le billet d'entrée PDF.

**Point de valeur :** le lit doit exister réellement, appartenir au bon établissement et être libre. La réservation est atomique afin d'éviter le double-booking.

## 4. Ce qui peut être affirmé

- La prise en charge d'un patient non identifié est fonctionnelle.
- Le triage et les réévaluations sont historisés.
- Le rapprochement est manuel, audité et corrigeable.
- Un patient rapproché peut poursuivre un nouveau parcours sur son DPU canonique.
- L'hospitalisation utilise une structure typée et un lit configuré.

## 5. Ce qui ne doit pas être affirmé comme terminé

- redirection automatique depuis l'urgence vers l'hospitalisation ;
- lien `emergencyId` natif dans le séjour hospitalier ;
- génération de tous les documents médico-légaux d'urgence ;
- agrégation canonique complète des hospitalisations créées avant rapprochement ;
- finance différée URG-TEMP et ventilation assurance/patient après régularisation ;
- validation E2E automatisée des dix scénarios de #47 ;
- UAT finale signée par tous les métiers.

## 6. Checklist de répétition

- [ ] Les comptes se connectent et les OTP sont disponibles.
- [ ] Les menus Urgences, Rapprochement, Patients et Hospitalisations sont visibles.
- [ ] Le DPU candidat apparaît dans la recherche de rapprochement.
- [ ] Le service d'hospitalisation, la chambre et les lits libres sont visibles.
- [ ] Une visite peut être créée sur le DPU canonique.
- [ ] L'admission occupe le lit et génère le billet d'entrée.
- [ ] Un scénario de secours déjà préchargé est disponible.
- [ ] Aucune donnée réelle de patient n'est utilisée.
- [ ] La démo complète a été répétée sans modification de code après validation.
