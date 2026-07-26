# AI-MED-SAFE-002 — Détection référentielle des doublons médicament

## Statut

- **Epic** : IA clinique / Sécurité médicamenteuse
- **Type** : Feature sécurité
- **Priorité** : P0
- **Statut** : IN PROGRESS
- **Branche** : `agent/medication-reference-safety-integration`

## Objectif

Compléter la détection exacte des doublons de prescription par une détection fondée sur une preuve positive de concept médicament partagé.

Exemple attendu : une marque et son principe actif peuvent être reconnus comme un même concept lorsqu'une source autoritative les résout vers le même identifiant.

## Règle de sécurité

Le moteur ne possède que deux conclusions :

- `SAME_CONCEPT` : preuve positive d'identifiant partagé dans une même source ;
- `UNKNOWN` : toute autre situation, y compris indisponibilité réseau, absence de résultat ou identifiants distincts.

Il n'existe volontairement aucun statut `DIFFERENT` ou `SAFE`.

## Périmètre

- Détecteur indépendant du LLM.
- Comparaison d'un médicament proposé avec une liste de traitements ou allergies documentés.
- Retour d'une preuve structurée : nom proposé, nom correspondant, source, identifiant et type de preuve.
- Cache et fallback hérités du service de référentiel.
- Journalisation de la preuve positive uniquement.
- Tests avec marque/principe actif, absence de preuve et source indisponible.

## Hors périmètre

- Interactions médicament-médicament.
- Allergies par classe thérapeutique.
- Contre-indications patient.
- Recommandations de substitution.
- Validation automatique d'une ordonnance.

## Critères d'acceptation

- [x] Une correspondance de concept partagée produit une preuve positive.
- [x] Une absence de concept partagé ne produit aucune alerte.
- [x] Une source indisponible ne bloque pas la consultation.
- [x] La preuve contient la source et l'identifiant.
- [ ] Le garde ordonnance utilise le détecteur après les contrôles exacts.
- [ ] L'alerte cite le nom correspondant sans exposer de détail technique inutile au professionnel.
- [ ] Les logs distinguent doublon exact et doublon référentiel.
- [ ] Maven strict vert après intégration complète.

## Scénarios

### MED-DUP-01 — Marque et principe actif
- Proposé : Doliprane.
- Traitement actif : Paracétamol.
- Source : même identifiant normalisé.
- Attendu : demande de confirmation avant proposition d'ordonnance.

### MED-DUP-02 — Concepts distincts
- Proposé : Doliprane.
- Traitement actif : Ibuprofène.
- Attendu : aucune alerte référentielle.

### MED-DUP-03 — Référentiel indisponible
- Simuler une panne réseau.
- Attendu : aucune exception métier ; les contrôles exacts locaux restent actifs.
