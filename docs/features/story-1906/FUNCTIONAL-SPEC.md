# FUNCTIONAL-SPEC.md — Alignement Module 9 : Résultats d'examens

## 1. Contexte & Objectifs

Le Cahier des Charges (CDC) exige une traçabilité rigoureuse et une immuabilité des résultats d'examens biologiques.
Le système actuel permet le téléversement direct sans gestion de statut, de versioning ni de structure interopérable complète (FHIR).
Cette spécification vise à :
1. Implémenter un cycle de vie strict pour les résultats d'examens (`DRAFT`, `VALIDATED`, `CANCELLED`).
2. Imposer l'immutabilité d'un résultat une fois validé. Toute modification ultérieure doit générer une nouvelle version du résultat.
3. Identifier le biologiste ou médecin validateur par son identifiant unique utilisateur (`validator_user_id`), plutôt qu'un simple champ texte.
4. Intégrer une conclusion clinique textuelle globale pour l'examen.
5. Lier le résultat d'examen à un document médical persistant et vérifiable (`document_id`).
6. Permettre l'export structuré (CSV/JSON) pour le patient ou les praticiens.
7. Rendre les résultats interopérables via des endpoints FHIR standardisés (`DiagnosticReport` et `Observation`).

---

## 2. Parcours Utilisateur

### 2.1 Biologiste (Portail Laboratoire)
- Le biologiste sélectionne une demande d'examen reçue.
- Il peut saisir les résultats (analytes, valeurs, interprétations, conclusion).
- Il peut enregistrer en brouillon (`DRAFT`) ou valider définitivement (`VALIDATED`).
- Lors de la validation, le système génère un document PDF associé (stocké dans les documents médicaux) et l'examen passe à `VALIDATED`.
- Si le biologiste doit modifier un résultat déjà `VALIDATED`, le système ne modifie pas l'ancien enregistrement : il crée un nouvel enregistrement lié (version incrémentée) et marque le précédent comme archivé/parent.

### 2.2 Patient (Portail Patient)
- Le patient se connecte sur son espace.
- Il accède à la page "Mes résultats" (`/patient/results`) listant ses résultats validés avec les conclusions et la possibilité de télécharger le document d'examen officiel.

---

## 3. Critères d'Acceptation Fonctionnels

- **FR-RESULT-001 (Immutabilité & Versioning)** : Tout résultat passé à l'état `VALIDATED` ne peut plus être modifié ou supprimé. Toute mise à jour ultérieure crée une nouvelle version avec un lien vers la version parente (`parent_result_id`), incrémentant le numéro de version.
- **FR-RESULT-002 (Validateur strict)** : Le validateur est un utilisateur existant dans le système, identifié par `validator_user_id` (UUID).
- **FR-RESULT-003 (Conclusion)** : Chaque résultat d'examen contient une conclusion clinique rédigée par le validateur.
- **FR-RESULT-004 (Export structuré)** : Possibilité d'exporter l'historique des résultats d'un patient au format CSV ou JSON.
- **FR-RESULT-005 (FHIR)** : Les résultats d'un patient sont exposés via `/fhir/DiagnosticReport` et `/fhir/Observation` conformément au profil FHIR R4.
