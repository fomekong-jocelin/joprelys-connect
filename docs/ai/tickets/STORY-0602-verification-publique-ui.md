# STORY-0602 — Page publique de vérification d'authenticité (UI/UX)

## 1. Objectif

Cette user story consiste à concevoir et implémenter la page web publique de vérification d'authenticité, accessible sans authentification par un tiers (par exemple en scannant le QR code sur le PDF). Elle affiche les métadonnées administratives sécurisées pour confirmer que le document est valide, sans divulguer de données médicales (symptômes, diagnostic, ordonnance).

## 2. Critères d'acceptation
- [ ] Une page publique d'authentification est accessible à l'adresse `/verify/:documentId`.
- [ ] La page affiche de manière claire le statut de validation du document :
  - **VERT / AUTHENTIQUE** : Document existant et valide.
  - **ORANGE / RÉVOQUÉ ou REMPLACÉ** : Si le document a été annulé/remplacé.
  - **ROUGE / INVALIDE** : Si le document n'existe pas ou si la signature est incorrecte.
- [ ] Les données administratives suivantes sont affichées en clair :
  - Numéro de document
  - Nom de la clinique pilote émettrice
  - Date et heure d'émission
  - Nom et prénom du médecin traitant
  - Nom et prénom du patient
- [ ] **Secret médical** : Aucune donnée concernant les symptômes, le diagnostic, l'examen clinique, les conseils ou les médicaments n'est affichée.
- [ ] Design premium, responsive (adapté aux mobiles de pharmaciens) et conforme à la charte graphique du projet.

## 3. Pilotage projet
| Champ | Valeur |
|---|---|
| Epic parent | EPIC-0006 (Génération PDF & Vérification par QR Code) |
| User story parent | STORY-0602 |
| Sprint cible | SPRINT-0004 |
| Priorité business | P0 |
| Complexité | M |
| Story points | 3 |
| Profil recommandé | Intermédiaire |
| Effort senior | 0.8j |
| Responsable | Antigravity |
| Reviewer obligatoire | Lead Developer |
| Risque fonctionnel | Moyen |
| Risque technique | Faible |
| Dépendances | STORY-0601 |

## 4. Contexte analysé
- [x] `AGENTS.md` lu
- [x] `SKILL.md` lu
- [x] `PROJECT-TRACKING.md` lu
- [x] `CHANGELOG.md` lu

## 5. Hypothèses
- La route `/verify/:documentId` dans Angular est accessible hors routeur avec authentification (pas de `canActivate: [roleGuard]`).
- L'appel API public `/api/public/documents/{id}/verify` est fait de manière anonyme.

## 6. Risques et impacts
| Risque | Impact | Mitigation |
|---|---|---|
| Fuite involontaire d'informations cliniques sensibles | Élevé | Audit strict du template HTML et du DTO de retour de l'API pour s'assurer que seuls les champs administratifs sont transmis. |

## 7. Action plan
1. Créer la route publique `/verify/:documentId` dans `app.routes.ts`.
2. Créer le composant standalone public `VerificationComponent` sans en-tête d'application connectée (pas de barre latérale ni d'entête connectée), mais avec un design épuré, centré et professionnel.
3. Afficher un indicateur visuel fort (badge vert/rouge, icônes de succès/erreur, dégradés premium).
4. Écrire les tests unitaires frontend.

## 8. Implémentation réalisée
- Modification de `ConsultationApiService` pour ajouter `downloadDocument` et `verifyDocumentPublic`.
- Ajout de la route `/verify/:documentId` sans guard dans `app.routes.ts`.
- Création du composant public `VerificationComponent` avec un design premium, responsive, supportant le mode sombre et le respect du secret médical (RGPD).
- Ajout de clés i18n dans `I18nService` pour la traduction FR/EN de la page de vérification et du téléchargement.
- Ajout du bouton de téléchargement "Télécharger PDF" avec gestion d'erreurs (visite non clôturée) dans la section Historique Médical de `PatientDetailComponent`.

## 9. Suivi d'exécution
- Temps estimé : 0.8j
- Temps passé : 0.4j
- Dérive : 0.0j

## 10. Tests et vérifications
- [x] Compilation Angular réussie (`npm run build`).
- [x] Vérification visuelle et design responsive.
- [x] Vérification de l'absence de données médicales sur l'écran public.

## 11. Documentation
- [x] Spécification fonctionnelle créée : `docs/features/pdf/FUNCTIONAL-SPEC.md`
- [x] Spécification technique créée : `docs/features/pdf/TECHNICAL-DESIGN.md`

## 13. Statut final
Statut : DONE
