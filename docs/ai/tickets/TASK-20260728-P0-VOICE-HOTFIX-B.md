# TASK-20260728 — P0 Voice Hotfix B

## Objectif
Éliminer la perte de mots observée après déploiement de #216.

## Périmètre
- Realtime Consultation : ne plus couper le sender pendant `assistantSpeaking` ni pendant le backlog.
- Realtime Constantes : même règle.
- Désactiver la restitution vocale automatique non critique pendant la capture clinique continue.
- Maintenir la pause explicite médecin et le fail-closed de persistance durable.
- Ajouter les tests adversariaux correspondants.
- Supprimer ensuite les branches historiques destructives `.clear()` / `saveVitals()` du parent Consultation.

## Définition de terminé
- [ ] Parole pendant réponse assistant : transcript conservé.
- [ ] Parole pendant backlog : transcript conservé.
- [ ] 20+ tours dans l'ordre, sans duplication.
- [ ] Prescription/examens existants jamais supprimés.
- [ ] Constantes jamais persistées avant validation explicite.
- [ ] Tests Angular et build production verts.
- [ ] Test mobile réel documenté avant clôture de #215.
