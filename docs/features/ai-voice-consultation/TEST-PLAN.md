# TEST-PLAN — Assistant vocal IA de consultation

## 1. Objectif

Prouver que l’assistant accélère la saisie sans contourner la sécurité, le
tenant, la validation médicale ni la confidentialité.

## 2. Scénarios fonctionnels

| ID | Scénario | Résultat attendu |
|---|---|---|
| TC-001 | QR d’une visite active du tenant | bon patient confirmé |
| TC-002 | QR d’un autre tenant | 404, aucune donnée révélée |
| TC-003 | Visite clôturée/annulée | 409 |
| TC-004 | Dictée symptômes + examen | brouillon, aucune persistance |
| TC-005 | « Non, corrige ici… » | champ ciblé modifié |
| TC-006 | Champ provider inconnu | sortie rejetée |
| TC-007 | Prompt injection dictée | aucune action/secret |
| TC-008 | Provider indisponible | 503, mode manuel disponible |
| TC-009 | Session expirée | redémarrage explicite |
| TC-010 | Édition manuelle après application | valeur manuelle conservée |
| TC-011 | TTS désactivé | aucune lecture vocale |
| TC-012 | Permission refusée | alternative texte/manuelle |

## 3. Tests sécurité

- 401 sans JWT ; 403 sans permission ; 404 inter-tenant.
- Limites 413/415/429.
- Clé provider absente avec feature active.
- Aucun contenu clinique dans les logs capturés.
- Sorties provider malformées, trop longues, nulles ou contradictoires.
- Sessions concurrentes et expiration.

## 4. Évaluation clinique

Corpus anonymisé : français et accents locaux, bruit, négations, correction,
antécédent versus symptôme, hypothèse versus diagnostic, contradictions.

Mesures : exactitude par champ, taux de correction, omissions critiques,
hallucinations, latence p95 et abandon.

## 5. Commandes

| Niveau | Commande |
|---|---|
| Backend ciblé | `./mvnw test -Dtest=VisitControllerTest,*Ai*Test` |
| Backend complet | `./mvnw clean verify` |
| Angular | `npm run test` |
| Angular build | `npm run build` |
| i18n | `npm run i18n:check` |

## 6. Matrice UI

Android Chrome, iOS Safari, desktop Chrome/Firefox/Edge, light/dark, FR/EN,
clavier/lecteur d’écran, permissions accordées/refusées/révoquées.

## 7. Résultats

| Date | Commande | Résultat | Commentaire |
|---|---|---|---|
| 2026-07-17 | compilation Maven offline avec cache local | SUCCESS | état amorcé uniquement |
| 2026-07-17 | Maven avec dépôt sandbox par défaut | BLOCKED | réseau Maven refusé |
| 2026-07-17 | `mvn -o -Dmaven.repo.local=… -Dtest=VisitControllerTest test` | SUCCESS | 16 tests, 0 échec ; QR décodé et cas 401/403/404/409 couverts |

La suite complète, les tests des adaptateurs IA, Angular et E2E restent à
exécuter lorsque les stories correspondantes seront implémentées.
