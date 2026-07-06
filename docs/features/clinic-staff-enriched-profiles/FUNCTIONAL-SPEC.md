# FUNCTIONAL-SPEC — Profils enrichis et ressources graphiques (Logos, Signatures, Cachets, Photos)

## 1. Résumé métier

Le cahier des charges de Joprelys Connect exige que les documents médicaux officiels (comptes-rendus de consultation, fiches de sortie, ordonnances, synthèses médicales) comportent des ressources visuelles d'authentification et de branding :
1. **Logo de la clinique** : pour l'identité visuelle de l'établissement.
2. **Signature du médecin** : pour engager la responsabilité légale du praticien.
3. **Cachet (stamp) du médecin** : pour matérialiser la conformité et l'agrément légal.

De plus, l'interface de gestion du personnel doit être enrichie avec des photos d'identité (avatars) et des champs professionnels additionnels (spécialité médicale, numéro d'inscription à l'Ordre des Médecins, département, téléphone direct, bio) pour professionnaliser le back-office clinique.
Toutes ces images doivent être téléversées via une zone interactive **Drag-and-Drop** intégrant une prévisualisation (preview) instantanée avant enregistrement.

## 2. Objectifs

- [ ] Permettre à l'administrateur d'un établissement d'ajouter/modifier le logo de la clinique.
- [ ] Permettre aux médecins de téléverser et mettre à jour leur signature numérique et leur cachet professionnel.
- [ ] Permettre aux collaborateurs d'ajouter leur photo de profil et d'enrichir leurs coordonnées et attributs professionnels.
- [ ] Offrir une interface utilisateur moderne avec un composant de téléversement en glisser-déposer (Drag & Drop) doté d'une prévisualisation.
- [ ] Intégrer dynamiquement ces éléments visuels (logo, signature, cachet) dans les documents PDF générés par la plateforme (ordonnances, fiches de consultation, etc.).

## 3. Utilisateurs / acteurs concernés

| Acteur | Besoin | Droits / limites |
|---|---|---|
| **ADMIN_JOPRELYS** | Configurer ou valider les établissements et réinitialiser les configurations globales | Tous droits de modification sur les organisations |
| **ADMIN_CLINIQUE** | Gérer les informations de son établissement (logo) et inviter/éditer les profils des collaborateurs de sa clinique | Peut éditer sa propre organisation et ses collaborateurs |
| **MEDECIN** | Enrichir son propre profil, téléverser sa signature et son cachet | Ne peut éditer que son propre profil (signature/cachet/champs pro) |
| **INFIRMIER / BIOLOGISTE / PHARMACIEN** | Enrichir leur propre profil, téléverser leur photo | Pas d'accès aux champs de signature/cachet médicaux (réservés MEDECIN) |

## 4. Périmètre

### Inclus

- **Modifications de la base de données** : Ajout de colonnes pour les chemins de stockage (ou base64), numéro d'ordre professionnel, téléphone pro, spécialité, département, bio.
- **APIs Backend** :
  - Services d'upload d'images sécurisés (validation stricte de la taille, du format MIME image/png, image/jpeg).
  - Endpoint de mise à jour des informations de profil enrichi pour les collaborateurs et l'organisation.
- **Interfaces Frontend Angular** :
  - Composant réutilisable de Drag-and-Drop pour le téléversement avec preview d'image (respectant les arrondis sobres et Tailwind CSS v4).
  - Formulaires enrichis pour la clinique (logo) et le personnel (photo, signature, cachet, spécialité, etc.).
- **Génération PDF** :
  - Intégration du logo de la clinique dans l'en-tête du PDF.
  - Intégration de la signature et du cachet du médecin en bas du PDF pour les documents patients.

### Exclus

- Signature cryptographique qualifiée (eIDAS) : seule l'image numérisée de la signature et du cachet est gérée dans ce périmètre.
- Outils de recadrage d'images (Cropping) avancés dans le navigateur (restreint aux validations de taille côté client/serveur).

## 5. Parcours utilisateur

### Parcours 1 : Configuration de la clinique par l'ADMIN_CLINIQUE
1. L'administrateur de clinique accède aux paramètres de l'organisation.
2. Il voit le formulaire d'édition de la clinique.
3. Il clique sur la zone de Drag-and-Drop pour sélectionner le logo de la clinique ou y glisse directement le fichier image.
4. Une prévisualisation du logo s'affiche immédiatement.
5. Il clique sur "Enregistrer", le fichier est téléversé et les données de l'organisation sont mises à jour.

### Parcours 2 : Profil Médecin & Ajout des ressources légales (Signature, Cachet, Spécialité)
1. Le médecin se connecte et accède à la page "Mon Profil".
2. Il complète ses informations : spécialité (ex: Cardiologie), numéro d'inscription à l'Ordre, téléphone.
3. Il glisse-dépose sa signature numérisée dans la zone de drag-and-drop de la signature (preview immédiate).
4. Il glisse-dépose l'image de son cachet physique dans la zone de drag-and-drop du cachet (preview immédiate).
5. Il clique sur "Mettre à jour mon profil".

### Parcours 3 : Visualisation sur les documents patients
1. Le médecin clôture une consultation ou finalise une ordonnance.
2. Le système génère le PDF officiel.
3. Le PDF présente en en-tête le logo de la clinique à côté du titre et du QR code.
4. En bas de page, dans la section "Signatures et cachet", la signature manuscrite du médecin et son cachet officiel sont affichés côte à côte.

## 6. Règles métier

| ID | Règle | Priorité | Source |
|---|---|---|---|
| **BR-PROF-001** | La signature et le cachet (stamp) ne sont autorisés que pour les comptes ayant le rôle `MEDECIN`. | P0 | Spécification légale |
| **BR-PROF-002** | Les fichiers d'images téléversés doivent respecter une taille maximale de 2 Mo pour éviter les surcharges de stockage. | P1 | Sécurité & Performance |
| **BR-PROF-003** | Seuls les formats d'images standard `image/png` et `image/jpeg` sont acceptés. Les SVG, PDF et exécutables sont interdits. | P0 | Sécurité (XSS/RCE) |
| **BR-PROF-004** | Les documents PDF ne doivent afficher la signature et le cachet que si le médecin les a effectivement configurés. | P1 | Intégrité documentaire |
| **BR-PROF-005** | Un collaborateur ne peut modifier que ses propres informations de profil (hors rôle, qui est géré par l'ADMIN_CLINIQUE). | P0 | Sécurité / RBAC |

## 7. Critères d’acceptation

- [ ] L'IHM de Drag-and-Drop accepte le glisser-déposer ainsi que le clic classique pour parcourir les fichiers.
- [ ] Une image non conforme (format ou taille) déclenche un message d'erreur clair en français et en anglais.
- [ ] La preview s'affiche de manière fluide sans rechargement de page.
- [ ] L'ADMIN_CLINIQUE et le MEDECIN peuvent supprimer/remplacer le logo, la photo, la signature et le cachet.
- [ ] Les PDF générés intègrent correctement le logo de la clinique à gauche dans l'en-tête.
- [ ] Les PDF générés intègrent la signature et le cachet à la fin des ordonnances et des comptes-rendus de consultation.

## 8. Cas limites / erreurs attendues

| Cas | Comportement attendu |
|---|---|
| Téléversement d'un fichier script malveillant renommé `.png` | Le backend valide le type MIME réel (Magic Numbers) et rejette le fichier avec une erreur 400 Bad Request. |
| Génération de PDF sans logo ou sans signature configurée | Le PDF est généré sans image (espace vide propre) afin de ne pas bloquer le flux de soins, tout en affichant textuellement le nom de la clinique et du médecin. |
| Dimension aberrante de l'image téléversée | L'image est redimensionnée proportionnellement (scaled) lors de l'intégration dans le PDF pour ne pas casser la mise en page. |

## 9. Textes / i18n

| Clé | Français | English |
|---|---|---|
| `profile.photo.drag_drop` | Glissez-déposez votre photo ici ou cliquez pour parcourir | Drag & drop your photo here or click to browse |
| `profile.signature.drag_drop` | Glissez-déposez votre signature ici ou cliquez | Drag & drop your signature here or click |
| `profile.stamp.drag_drop` | Glissez-déposez votre cachet ici ou cliquez | Drag & drop your stamp here or click |
| `profile.specialty` | Spécialité | Specialty |
| `profile.license_number` | Numéro d'inscription à l'Ordre | License Number |
| `profile.phone` | Téléphone professionnel | Professional Phone |
| `profile.bio` | Biographie / Présentation | Biography / Description |
| `profile.error.invalid_file_type` | Type de fichier invalide. Seuls PNG et JPEG sont acceptés. | Invalid file type. Only PNG and JPEG are accepted. |
| `profile.error.file_too_large` | Fichier trop volumineux. La taille maximale est de 2 Mo. | File too large. Maximum size is 2 MB. |

## 10. Impacts UI / branding

| Point | Impact |
|---|---|
| Nom de l’app | Non |
| Logo | Oui (Logo clinique personnalisable dynamique) |
| Thème light/dark | Oui (Composant de drag-and-drop compatible light/dark) |
| Composants réutilisables | Oui (Création d'un composant Angular `app-file-drag-drop` réutilisable) |

## 11. Hypothèses et questions ouvertes

- **Stockage physique** : Les fichiers seront stockés en local dans le répertoire `storage/uploads/` structuré par organisation et par utilisateur pour simplifier les sauvegardes, plutôt que stockés en base de données sous forme de BLOB (recommandé pour la performance de la base de données).
- **URLs publiques vs protégées** : Les ressources graphiques nécessaires aux documents publics (ex: logo sur page de vérification ou signature) doivent être exposées via une API de lecture publique filtrée.

## 12. Historique des mises à jour

| Date | Auteur | Changement |
|---|---|---|
| 2026-07-06 | Antigravity | Création initiale |
