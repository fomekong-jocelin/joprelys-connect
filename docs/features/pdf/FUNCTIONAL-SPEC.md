# Spécification fonctionnelle — Génération de PDF & Vérification par QR Code (EPIC-0006)

## 1. Problème métier à résoudre
Dans un cabinet médical ou clinique, les médecins remettent aux patients des comptes-rendus de consultation et des prescriptions d'ordonnances papier ou numériques. Cependant, ces documents sont facilement falsifiables (modification du traitement, falsification de signatures). 
Il est nécessaire d'offrir :
- Un format PDF officiel et infalsifiable généré automatiquement.
- Un moyen simple pour des tiers (pharmaciens, assureurs) de vérifier instantanément et de façon sécurisée que le document est authentique et correspond à celui enregistré en clinique, sans compromettre le secret médical (RGPD / OWASP).

## 2. Utilisateurs concernés
* **Médecin / Praticien** : Clôture la visite et génère/remet le document (imprimé ou PDF).
* **Patient** : Reçoit son document et le présente aux tiers.
* **Vérificateur externe (Pharmacien, Assureur)** : Scanne le QR code sur le document pour vérifier son authenticité en ligne.

## 3. Périmètre fonctionnel

### Inclus
1. **Génération automatique du PDF** lors de la clôture de la visite par le médecin (lorsque son statut passe de `EN_COURS` à `TERMINEE`).
2. **Génération d'un QR code unique** embarqué dans le PDF (en haut à droite ou en bas de page). Le QR code contient une URL publique et unique de vérification.
3. **Page publique de vérification d'authenticité** (accessible sans authentification) qui affiche :
   - Statut du document (Valide, Remplacé, Annulé).
   - Identifiant unique du document (ex: `DOC-CONS-YYYYMMDD-XXXXXX`).
   - Nom de la clinique émettrice.
   - Date et heure d'émission.
   - Nom du médecin émetteur.
   - Nom et prénom du patient.
4. **Protection stricte du secret médical** : La page publique de vérification ne doit afficher **aucune** information sur le diagnostic, les symptômes ou la liste des médicaments prescrits. Elle valide uniquement l'existence et l'intégrité administrative du document.

### Exclus
- Stockage décentralisé IPFS ou Blockchain.
- Signature électronique qualifiée eIDAS (signature PKI avancée).
- Envoi automatique par mail/SMS (prévu dans une epic ultérieure).

## 4. Parcours utilisateur

```
Médecin clôture la visite
  └─► Le système génère le PDF combiné (Consultation + Ordonnance)
  └─► Le système génère un QR Code pointant vers /verify/{documentUuid}
  └─► Le PDF est stocké sur le serveur

Vérificateur (ex: Pharmacien) scanne le QR code
  └─► Ouvre un navigateur sur l'URL publique de Joprelys Connect
  └─► Si le document existe : Affiche "DOCUMENT AUTHENTIQUE", la date, la clinique, le médecin et le patient.
  └─► Si le document n'existe pas ou est falsifié : Affiche "DOCUMENT INVALIDE ou INEXISTANT".
```

## 5. Critères d'acceptation
- [ ] Le PDF contient l'en-tête de la clinique (Nom, Ville, Téléphone, Adresse), le numéro de visite, le DPU du patient, le motif, le diagnostic (uniquement pour le patient), et la liste des médicaments prescrits (Nom + Dosage + Posologie).
- [ ] Le QR code est visible sur le document PDF et est facilement scannable avec un smartphone.
- [ ] Le scan redirige vers `http://<domain>/verify/<document-uuid>`.
- [ ] La page de vérification n'affiche aucun contenu médical (pas de diagnostic, pas de médicaments).
- [ ] Si la visite n'est pas clôturée, aucun document n'est généré et aucune vérification n'est possible.
