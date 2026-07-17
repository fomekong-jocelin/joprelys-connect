# DATA-MODEL — Assistant vocal IA de consultation

## 1. Vue d’ensemble

La v1 ne crée aucune table. Les sessions sont éphémères. Le modèle durable
existant n’est modifié qu’après validation du médecin par l’API habituelle.

## 2. Modèle éphémère

| Objet | Champs principaux | Rétention |
|---|---|---|
| Session | sessionId, organizationId, visitId, doctorId, timestamps, turnCount | TTL 30 min |
| Brouillon | 8 champs bornés | TTL session |
| Historique | rôle + texte minimal | TTL session |
| Audio | octets de la requête courante | durée de transcription |

## 3. Données interdites

- Aucun fichier audio ou blob en base/disque applicatif.
- Aucun prompt/transcript/brouillon dans l’audit générique.
- Aucun nom patient, DPU, e-mail, téléphone ou date de naissance en session.
- Aucun cache navigateur persistant pour le brouillon clinique.

## 4. Migrations

| Migration | Requise | Commentaire |
|---|---|---|
| v1 mono-instance | Non | store mémoire derrière un port |
| Redis multi-instance | À décider | ADR, chiffrement et rétention requis |
| Historique IA durable | Hors périmètre | nouvelle analyse DPO obligatoire |

## 5. Contraintes

- Clé logique : `(organizationId, visitId, doctorId)`.
- Une session active maximum par clé.
- TTL glissant borné par une durée absolue à définir.
- Suppression sur `DELETE`, validation finale ou expiration.

## 6. Données sensibles

- [x] Données de santé identifiées.
- [x] Masquage total des contenus dans les logs prévu.
- [x] Rétention applicative éphémère prévue.
- [ ] Résidence, rétention provider et DPA validés par DPO.
- [ ] Chiffrement défini si Redis retenu.

## 7. Historique

| Date | Auteur | Changement |
|---|---|---|
| 2026-07-17 | Codex | Modèle v1 sans migration |
