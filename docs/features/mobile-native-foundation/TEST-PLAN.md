# TEST PLAN — Fondation mobile native Joprelys Connect

## Objectif

Définir les preuves minimales exigées avant qu’une brique mobile Flutter soit considérée comme READY/DONE, avec un niveau renforcé sur l’audio clinique et la sécurité.

## 1. Gates automatiques de base

Pour toute PR qui modifie `mobile/**` :

```bash
flutter pub get
dart format --output=none --set-exit-if-changed .
flutter analyze
flutter test
```

Selon la portée :

```bash
flutter build apk --debug
```

Aucun test ignoré sans issue liée.

## 2. Fondation / bootstrap

### Cas

- démarrage nominal ;
- configuration `dev`, `recette`, `prod` ;
- configuration invalide fail-fast ;
- aucune valeur sensible dans les logs ;
- app name/logo issus de `AppConfig` ;
- aucun `Flutter Demo` restant.

### Preuves

- unit tests config ;
- widget smoke test de `JoprelysApp` ;
- build Android debug.

## 3. Thème

### Couverture

- light ;
- dark ;
- system ;
- changement à chaud ;
- persistance du choix ;
- contraste des composants principaux.

### Golden tests minimum

- bouton primaire ;
- input ;
- card ;
- badge ;
- page header ;
- loading ;
- empty ;
- error ;
- patient context header.

Chaque golden important doit être vérifié en light et dark.

## 4. Internationalisation

### Couverture

- locale `fr` ;
- locale `en` ;
- fallback ;
- changement de langue à chaud ;
- dates/heures ;
- pluriels ;
- placeholders ;
- aucune clé brute visible.

### Gate

Un contrôle doit échouer si une clé du template ARB n’a pas son équivalent requis.

## 5. Navigation / session

### Cas

- non authentifié -> login ;
- login -> destination autorisée ;
- session expirée -> re-authentification ;
- logout -> purge + login ;
- route interdite côté UI -> redirection sûre ;
- backend 403 reste bloquant même si la route est visible ;
- deep link patient/visite avec session valide ;
- deep link avec session absente.

## 6. Réseau

### Cas

- 200/201/204 ;
- 400/401/403/404/409/422/429/500/503 ;
- timeout ;
- DNS/réseau indisponible ;
- annulation d’une requête lors de sortie d’écran ;
- refresh token concurrent : une seule opération de refresh ;
- refresh invalide : déconnexion ;
- aucune répétition automatique d’une écriture non idempotente.

### Confidentialité

Vérifier que corps clinique, JWT et PII n’apparaissent pas dans les logs de test.

## 7. Secure storage / biométrie

### Cas

- token écrit/lu/supprimé dans secure storage ;
- aucune utilisation de préférences ordinaires pour les secrets ;
- biométrie indisponible ;
- biométrie refusée ;
- biométrie réussie ;
- session serveur expirée malgré biométrie locale -> login serveur ;
- logout purge les éléments de session.

## 8. Features standard

Pour chaque feature :

- use cases unitaires ;
- repository avec data source mockée ;
- controller Riverpod via `ProviderContainer` ;
- screen/widget interactions ;
- loading/error/empty ;
- permission autorisée/refusée ;
- FR/EN ;
- light/dark.

## 9. Audio clinique Android — P0

### 9.1 Démarrage

- `RECORD_AUDIO` accordé ;
- permission refusée ;
- permission retirée ;
- service foreground démarre ;
- notification visible ;
- UI ne passe à `recording` qu’après ACK natif.

### 9.2 Cycle de vie

Sur appareil physique Android au minimum :

- écran restant allumé ;
- écran éteint ;
- verrouillage manuel ;
- app en background ;
- retour foreground ;
- rotation ;
- changement d’application ;
- batterie faible / économie d’énergie documentée ;
- appel téléphonique ou perte audio focus ;
- casque/Bluetooth connecté puis déconnecté ;
- micro indisponible.

### 9.3 Résilience segments

- segments numérotés sans trou silencieux ;
- checksum stable ;
- ACK serveur ;
- retry réseau uniquement avec idempotence ;
- segment envoyé deux fois -> déduplication serveur attendue après évolution contractuelle ;
- perte réseau prolongée -> file locale chiffrée ;
- retour réseau -> reprise ordonnée ;
- suppression locale uniquement après ACK/finalisation ;
- crash/restart -> détection des orphelins.

### 9.4 Interruptions

- interruption produit un événement ;
- durée manquante traçable ;
- écran affiche l’état interrompu ;
- reprise explicite ;
- aucune UI mensongère `recording`.

### 9.5 Arrêt

- arrêt manuel ;
- logout ;
- session expirée ;
- erreur fatale ;
- app kill ;
- fichiers temporaires correctement traités selon politique.

## 10. Sécurité mobile

### Contrôles

- aucun secret versionné ;
- aucun secret dans `--dart-define` ;
- aucun token/PII dans logs ;
- stockage sensible uniquement sécurisé ;
- aucun audio dans stockage partagé ;
- API uniquement TLS ;
- build release sans logs debug sensibles ;
- signature release hors dépôt ;
- R8/obfuscation avant pilote externe ;
- revue de pinning avant activation ;
- politique DPO pour brouillons/audio avant pilote.

## 11. Accessibilité

- cibles tactiles >= 44 px ;
- labels sémantiques ;
- TalkBack sur parcours principaux ;
- text scale ;
- contrastes light/dark ;
- états ne reposant pas uniquement sur couleur ;
- focus clavier/lecteur si pertinent.

## 12. E2E prioritaires

### E2E-01 Auth

Connexion -> dashboard -> logout.

### E2E-02 Patient

Recherche -> dossier -> navigation identité/médical -> retour.

### E2E-03 File active

Dashboard -> file active -> visite -> constantes.

### E2E-04 Constantes

Saisie -> validation -> erreur réseau -> retry -> succès sans duplication.

### E2E-05 Consultation audio

Visite -> démarrage audio -> écran éteint/background -> reprise -> transcript -> correction -> proposition -> validation médecin.

### E2E-06 Session expirée

Formulaire en cours -> expiration -> re-auth -> restauration uniquement du brouillon autorisé.

## 13. Matrice appareils pilote Android

Au minimum :

- Samsung milieu de gamme récent — priorité car appareil réel utilisé dans les recettes actuelles ;
- Android stock récent ;
- un appareil Android plus ancien encore dans le minSdk supporté.

Les versions exactes sont fixées dans la story QA en fonction du `minSdk/targetSdk` réellement retenu.

## 14. Definition of Done mobile

Une story mobile n’est DONE que si :

- [ ] critères fonctionnels validés ;
- [ ] `flutter analyze` vert ;
- [ ] `flutter test` vert ;
- [ ] build requis vert ;
- [ ] FR/EN vérifiés ;
- [ ] light/dark vérifiés ;
- [ ] permission/RBAC testés ;
- [ ] loading/error/empty testés ;
- [ ] sécurité revue ;
- [ ] documentation à jour ;
- [ ] aucune donnée clinique dans logs ;
- [ ] recette appareil physique effectuée pour toute feature audio/platform.
