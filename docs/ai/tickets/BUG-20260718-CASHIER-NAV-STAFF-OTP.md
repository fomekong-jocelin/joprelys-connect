# BUG-20260718 — Doublon caissier et OTP professionnel incomplet

## Statut

`QA_TECHNIQUE_EN_COURS`

## Constats

### Navigation financière

Le rôle système `CAISSIER` voyait simultanément :

- `Facturation & Caisse` (`/clinic/billing`) ;
- `Poste caissier` (`/clinic/cashier`).

Les deux routes réutilisaient le même composant de caisse. La politique d'accès à l'espace de facturation incluait les permissions de session de caisse et `BILLING_INVOICE_READ`, toutes présentes sur le rôle caissier. Le doublon existait donc dans le menu et restait accessible par URL directe.

### Authentification

L'OTP du personnel reposait sur une liste de cinq rôles codée en dur. Les profils suivants obtenaient directement une session après le mot de passe :

- `SUPER_ADMIN` ;
- `DAF` ;
- `SECRETAIRE_COMPTABLE` ;
- `CAISSIER` ;
- `AGENT_ACCUEIL` ;
- `INFIRMIER` ;
- `GESTIONNAIRE_STOCK` ;
- `RESPONSABLE_HOSPITALISATION` ;
- `AUDITEUR` ;
- tout rôle personnalisé.

## Décisions

### Séparation des espaces

- **Poste caissier** : file d'encaissement, paiements, mouvements et sessions de caisse ;
- **Facturation & gestion financière** : factures, créances, assurances, tarifs et pilotage financier ;
- un caissier pur ne voit et n'utilise que le poste caissier ;
- un profil cumulant des permissions de caisse et de gestion financière peut voir les deux espaces ;
- la route `/clinic/billing` applique la même règle que le menu afin d'empêcher un contournement par URL directe.

L'accès à la gestion financière exige au moins un groupe cohérent de permissions, par exemple :

- `BILLING_INVOICE_READ` + `PATIENT_READ` ;
- `BILLING_INVOICE_WRITE` ;
- une permission de créances ;
- une permission de bordereaux d'assurance ;
- une permission de pilotage ou d'export comptable.

### OTP professionnel obligatoire

- toute identité professionnelle, système ou personnalisée, reçoit un défi OTP après validation du mot de passe ;
- aucune session ni aucun jeton n'est créé avant validation du code ;
- les comptes `PATIENT`, y compris les valeurs mixtes contenant `PATIENT`, sont refusés sur le formulaire personnel et doivent utiliser le portail patient ;
- un nouveau code remplace l'ancien : l'ancien OTP est supprimé avant tentative d'envoi et n'est recréé qu'après un envoi réussi ;
- une panne d'e-mail ne laisse donc aucun ancien code utilisable.

### Sessions existantes

La migration `V73__revoke_professional_sessions_for_mandatory_otp.sql` révoque les sessions professionnelles actives avec :

- raison `MFA_POLICY_CHANGE` ;
- source `SYSTEM` ;
- conservation des sessions patient uniquement.

Les anciens jetons d'accès à courte durée de vie expirent normalement, mais aucun ancien jeton de rafraîchissement professionnel ne peut être renouvelé après la migration.

## Messages utilisateur

- destinataire OTP rejeté : vérifier avec un administrateur l'adresse e-mail associée au compte ;
- service d'envoi indisponible : réessayer ultérieurement ;
- identifiants incorrects ou compte patient utilisé sur le mauvais canal : message générique afin de ne pas divulguer l'existence du compte.

## Tests

### Backend

- chaque rôle système professionnel exige un OTP ;
- chaque rôle personnalisé exige un OTP ;
- le caissier ne reçoit une session qu'après le code ;
- un code incorrect est refusé ;
- les comptes patient et mixtes sont refusés sur le canal du personnel ;
- une panne lors du remplacement du code invalide l'ancien OTP ;
- Flyway applique la révocation des sessions professionnelles existantes.

### Frontend

- un caissier pur voit `/clinic/cashier` mais pas `/clinic/billing` ;
- l'accès direct à `/clinic/billing` est refusé au caissier pur ;
- un profil finance autorisé voit la gestion financière ;
- un profil multi-rôles peut voir les deux espaces ;
- aucun jeton n'est stocké après la première étape de connexion ;
- le retour vers une consultation scannée est conservé après validation OTP ;
- les erreurs d'envoi OTP sont distinguées et traduites en français et en anglais.

## Critères d'acceptation

- [x] doublon supprimé pour le caissier pur ;
- [x] contrôle identique dans le menu et la route ;
- [x] libellé renommé en « Facturation & gestion financière » ;
- [x] OTP obligatoire pour tous les comptes professionnels ;
- [x] canal patient isolé ;
- [x] anciennes sessions professionnelles révoquées au déploiement ;
- [x] erreurs OTP actionnables en FR/EN ;
- [ ] CI backend et frontend verte ;
- [ ] recette authentifiée sur l'environnement déployé.
