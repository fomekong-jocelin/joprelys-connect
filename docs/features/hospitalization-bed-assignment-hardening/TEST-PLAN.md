# TEST-PLAN — Durcissement de l’attribution du lit

| Scénario | Résultat attendu | Niveau |
|---|---|---|
| Lit libre, ouvert et prêt | Proposé | Angular + service |
| Lit libre mais fermé | Non proposé | Angular |
| Lit libre mais non prêt | Non proposé | Angular |
| Admission créée, documents en échec | Warning visible, bouton de reprise, aucun second POST séjour | Angular |
| Reprise documentaire réussie | Événement de fin émis, une seule admission | Angular |
| Praticien d’un autre tenant | HTTP 409, aucune création | Spring unit |
| Praticien désactivé | HTTP 409, aucune création | Spring unit |
| Praticien sans affectation active à l’unité | HTTP 409, aucune création | Spring unit |
| Praticien affecté à l’unité | Admission nominale | Spring unit |
| Médecin affecté à un autre service | Non proposé | Angular |
| Compte médecin désactivé | Non proposé | Angular |
| Profil `ADMIN_CLINIQUE` | Non proposé comme médecin responsable | Angular |
| Aucun service sélectionné | Sélecteur médecin désactivé | Angular |
| Service sélectionné | Médecins du service affichés ensuite | Angular |
