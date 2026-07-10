# Plan de test - Rapprochement de caisse

| Niveau | Scénario |
|---|---|
| Backend | Une entrée espèces augmente le solde à clôturer. |
| Backend | Une entrée chèque ou virement n'augmente pas le solde espèces. |
| Backend | Un versement banque sans référence est rejeté. |
| Backend | Un versement banque réduit le solde espèces. |
| Backend | Une autre organisation ne peut pas lire les mouvements d'une session. |
| Angular | Le récapitulatif par moyen de règlement et les contrôles du formulaire sont affichés. |

## Commandes

```powershell
cd backend; .\mvnw test
cd ..\web; npm run test -- --watch=false
cd ..\web; npm run build
```
