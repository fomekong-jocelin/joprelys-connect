# BUG-20260726-RXNORM-SPRING-CONSTRUCTOR-BOOT-FAILURE

## Qualification

- **Mode** : Diagnostic + Engineering + QA Review
- **Epic** : AI-MED-SAFE-002
- **Priorité** : P0 — indisponibilité applicative
- **Statut** : IMPLEMENTED — validation Maven et redéploiement requis
- **Stack** : Spring Boot / RxNorm / configuration conditionnelle
- **Estimation** : 0,25 jour senior
- **Reviewer** : Lead Backend + QA
- **Impact SemVer prévu** : PATCH

## Incident

Le déploiement de Recette au commit `03f02071` échoue au démarrage lorsque le
référentiel RxNorm est activé. Le service systemd redémarre en boucle.

## Cause racine

`RxNormMedicationReferencePort` est un composant Spring conditionnel avec deux
constructeurs. Aucun constructeur n'est désigné pour l'injection. Spring tente alors
une instanciation sans argument et échoue avec :

```text
No default constructor found
RxNormMedicationReferencePort.<init>()
```

Les tests unitaires existants instancient directement le constructeur `RestClient` et
ne démarrent aucun contexte Spring avec les propriétés RxNorm actives.

## Contournement Recette connu

```text
JOPRELYS_MEDICATION_REFERENCE_ENABLED=false
JOPRELYS_RXNORM_ENABLED=false
```

Ce contournement rétablit le service, mais désactive le référentiel et ne constitue
pas le correctif source.

## Actions

- [x] Confirmer les deux constructeurs et l'absence de constructeur injectable.
- [x] Créer la documentation fonctionnelle, technique et de test.
- [x] Désigner explicitement le constructeur `MedicationReferenceProperties`.
- [x] Ajouter un test de contexte Spring avec RxNorm activé.
- [x] Ajouter un test avec RxNorm désactivé.
- [ ] Exécuter le test ciblé puis le `clean verify`.
- [x] Mettre à jour changelog et suivi.

## Critères d'acceptation

- [x] Le test de contexte couvre le démarrage avec les deux propriétés RxNorm à `true`.
- [x] Le test exige un seul bean `MedicationReferencePort` RxNorm.
- [x] Le test exige l'absence du bean quand le référentiel est désactivé.
- [x] Le constructeur `RestClient` reste utilisable par les tests unitaires.
- [x] La construction du bean ne réalise aucun appel réseau RxNav.
- [ ] Recette peut réactiver les deux variables après déploiement du correctif.

## Reste à faire

La validation Maven locale est bloquée car le parent
`spring-boot-starter-parent:4.1.0` n'est pas présent dans le cache et le réseau est
indisponible. Relancer le test de contexte et `clean verify` en CI, déployer en
Recette avec les flags désactivés, puis réactiver les deux flags et vérifier le
démarrage stable.
