# BUG-20260809 — Médecin responsable filtré par service

## Statut

CORRIGÉ — le frontend ne propose désormais que les médecins actifs affectés au service sélectionné ; le backend conserve la validation finale.

## Cause

La modale d’hospitalisation normale affichait tous les comptes portant un rôle `MEDECIN` ou `ADMIN_CLINIQUE`, sans tenir compte des affectations actives aux unités. Le backend rejetait ensuite correctement un médecin non affecté.

## Correction

- règle frontend partagée entre admission normale et continuité urgence : rôle `MEDECIN`, compte actif et affectation active à l’unité sélectionnée ;
- réinitialisation du praticien choisi lors d’un changement de service s’il n’est plus éligible ;
- message explicite lorsqu’aucun médecin n’est affecté au service ;
- tests de non-régression sur le service sélectionné, le rôle et l’état actif.

## Vérifications

- tests Angular ciblés : 17 tests réussis ;
- suite Angular complète : 109 fichiers, 573 tests réussis ;
- build Angular réussi ;
- contrôle i18n réussi : 47 clés FR/EN ;
- backend inchangé dans ce correctif : il reste la source de vérité et refuse toute valeur incohérente.

## Reste à faire

Conserver ce ticket rattaché au durcissement d’attribution du lit et faire valider les règles d’affectation par les métiers.
