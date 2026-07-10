# Plan de test - Intégrité financière et postes métier

| Niveau | Scénario |
|---|---|
| Backend | Paiement partiel patient: facture et créance sont partiellement réglées. |
| Backend | Paiement total patient: créance patient réglée, créance assurance non réglée. |
| Backend | Paiement bordereau: créances assurance réglées sans modifier les créances patient. |
| Backend | Une facture non assurée n'a qu'une créance patient. |
| Angular | L'historique distingue « part patient réglée » de « facture soldée ». |
| Angular | Le suivi de créances ne relance pas une créance réglée. |
| E2E | Facture -> paiement patient -> bordereau -> règlement assurance -> caisse clôturée. |
| Sécurité | Un caissier ne voit ni actions DAF ni créances d'un autre tenant. |

## Vérifications STORY-2111

- `InvoiceControllerTest` est étendu avec une facture assurée validée, un paiement patient partiel puis complet, et les assertions des deux créances.
- `InvoiceControllerTest` exécuté avec le cache Maven temporaire autorisé : 2 tests réussis.

## Tests des futurs modules caissier, recouvrement et DAF

Les scénarios de tests unitaires, d'intégration et E2E spécifiques pour le rapprochement de caisse, le recouvrement de créances et les exports comptables OHADA sont référencés dans [DAF-VALIDATION.md](file:///C:/MES-APPLICATIONS/joprelys-connect/docs/features/financial-operations-integrity/DAF-VALIDATION.md).

