# ADR — Durcissement sécurité et sécurité clinique issu de l'audit du 2026-11-04

Date : 2026-11-04. Statut : **proposé** : à valider par un médecin référent, le pharmacien responsable et le DPO avant fusion.

Référence : [AUDIT-20261104](../../features/audit-20261104-full/AUDIT-REPORT.md), ticket [AUDIT-20261104](../../ai/tickets/AUDIT-20261104-FULL-SECURITY-CLINICAL-UX.md). Prolonge [ADR-20261017](ADR-20261017-journey-coherence-decisions.md).

## Contexte

L'audit complet du 2026-11-04 relève des chemins où une permission technique (RBAC) suffit pour poser un acte médical, des données non validées visibles par le patient, des actes possibles après le décès et un PIN public d'ordonnance exposé à la force brute.

## Décisions

| ID | Décision | Constat |
|---|---|---|
| D-01 | Les actes médicaux exigent un **médecin habilité** (rôle `MEDECIN`, compte actif), **en plus** de la permission RBAC : rédaction de la consultation, décision de sortie, constat de décès (y compris via l'orientation « Décès » des urgences). Contrôle côté serveur dans `MedicalSigningPolicy`. L'administrateur clinique conserve ses permissions d'administration mais ne pose aucun acte médical. | CLIN-07, CLIN-08, FA-37 |
| D-02 | Une ordonnance est signée ou annulée par le **praticien qui a le patient en consultation** (même règle que D-01 d'ADR-20261017 pour la clôture de visite). L'annulation n'est possible que depuis `DRAFT` ou `ACTIVE`. Aucune ordonnance n'est rédigée sur une visite qui n'est plus `EN_COURS`. | CLIN-06 |
| D-03 | Le résumé médical (portail et personnel) ne montre que les consultations **signées** et les résultats **validés, dernière version**. Un patient n'exporte que des résultats validés. | CLIN-04 |
| D-04 | Aucun soin après le décès : délivrance, validation pharmaceutique et demande d'examen refusées ; une administration datée **après l'heure du décès** est refusée (une administration antérieure peut encore être tracée a posteriori). Une administration ne peut pas être datée de plus de 5 minutes dans le futur. | CLIN-13, CLIN-09 |
| D-05 | Une demande d'examen et sa visite concernent **le même patient** (identité canonique en cas de fusion), sinon 400. | CLIN-11 |
| D-06 | PIN d'ordonnance : 6 chiffres tirés de `SecureRandom` pour les nouvelles ordonnances (les PIN à 4 chiffres existants restent valides jusqu'à expiration). Tentatives **persistées et réservées atomiquement** avant comparaison (3 essais, puis verrouillage de 15 minutes, conservé au redémarrage) ; comparaison en temps constant ; réponse 401 identique pour un numéro inconnu et un PIN faux. Migration `V121`. | SEC-01, FA-39 |
| D-07 | Le secret de signature JWT n'a plus de valeur par défaut hors profil `dev` ; l'application refuse de démarrer avec le secret publié dans l'historique Git. | Secret par défaut versionné |

## Conséquences

- **Contrat API** : nouveaux refus 403/409/400 sur des chemins auparavant permissifs. La vérification publique d'ordonnance renvoie 401 (au lieu de 404) pour un numéro inconnu. Aucun champ ajouté ni retiré.
- **Exploitation** : tout environnement lancé sans `JOPRELYS_JWT_SECRET` refuse désormais de démarrer (hors `mvnw spring-boot:run`, qui active le profil `dev`). Si un serveur a tourné avec l'ancien secret, **changer le secret** invalide les jetons forgés éventuels.
- **Métier** : un établissement où un non-médecin rédigeait les consultations ou décidait des sorties doit réaffecter ces actes. Les infirmiers tracent leurs observations dans les soins infirmiers.

## Non retenu (backlog)

Posologie structurée et obligatoire, plusieurs ordonnances par séjour, contrôle allergie/doublon à la prescription manuelle, décrément de stock (question d'unité de dispensation), corbeille de résultats du prescripteur : voir [EPIC-0030](../../pm/backlog/EPIC-0030-audit-20261104-remediation.md).
