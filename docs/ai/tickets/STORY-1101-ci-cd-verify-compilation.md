# STORY-1101 — CI/CD Automatisation & Compilation strictes
 
> Ticket d'automatisation et de validation de non-régression du build et des tests.
 
## 1. Objectif
 
Mettre en place un pipeline GitHub Actions / GitLab CI pour exécuter automatiquement `./mvnw clean verify` (Maven uniquement) et `npm run build && npm test` (Angular) à chaque Pull Request.
 
## 2. Critères d'acceptation
 
- [x] Pipeline CI activé sur les branches et les Pull Requests.
- [x] Le build Maven backend doit compiler et exécuter tous les 128 tests sans erreur.
- [x] Le build frontend Angular doit compiler en mode strict et valider les 58 tests.
- [x] Aucune variable d'environnement ou clé secrète ne doit être versionnée.
 
## 3. Pilotage projet
 
| Champ | Valeur |
|---|---|
| Epic parent | EPIC-0011 — DevOps & Intégration Continue |
| User story parent | STORY-1101 |
| Sprint cible | SPRINT-0005 |
| Priorité business | P0 |
| Complexité | M |
| Story points | 3 |
| Profil recommandé | Senior |
| Effort estimé senior | 0.7j |
| Effort estimé intermédiaire | 1.0j |
| Effort estimé junior | 1.8j |
| Responsable | Senior (Full-stack) |
| Reviewer obligatoire | Tech Lead |
| Risque fonctionnel | Faible |
| Risque technique | Moyen |
| Dépendances | Aucune |
| Bloquants connus | Aucun |
 
## 4. Action plan
 
- [x] Créer le fichier de workflow CI (ex: `.github/workflows/ci.yml`).
- [x] Configurer les étapes Java/Maven (Java 21+, mise en cache du repo `.m2`).
- [x] Configurer les étapes Node.js (Node 22+, `npm ci`, build Angular, tests).
- [x] Mettre à jour `CHANGELOG.md` et `PROJECT-TRACKING.md`.
 
## 13. Statut final
 
Statut : DONE
 
## 14. Impact version / SemVer
 
| Champ | Valeur |
|---|---|
| Changement livrable | Non |
| Type de bump | Aucun |
| Justification | Configuration DevOps interne uniquement |
