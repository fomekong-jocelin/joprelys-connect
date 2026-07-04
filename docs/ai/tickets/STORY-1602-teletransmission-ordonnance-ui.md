# STORY-1602 — Interface de télétransmission (IHM)

## 1. Description et contexte

**Epic** : API & Intégration Partenaire (Module 16)  
**Titre** : Interface de télétransmission (IHM)  
**Statut** : READY  
**Priorité** : P1  
**Sprint** : SPRINT-0008  
**SP** : 3  
**Profil recommandé** : Intermédiaire  
**Estimation** : 0.8j (Senior: 0.5j, Junior: 1.3j)  

## 2. Objectifs

Offrir aux patients et praticiens une interface visuelle pour déclencher la télétransmission de l'ordonnance et suivre son statut en temps réel.

## 3. Critères d'acceptation (DoD)

- [ ] Bouton "Télétransmettre à AllôPharma" visible sur la vue de l'ordonnance (portail patient et dossier de consultation praticien) si le statut de transmission n'est pas déjà `TRANSMITTED`.
- [ ] Badge indicateur coloré selon le statut (ex : vert pour `TRANSMITTED`, jaune pour `PENDING`, gris pour non transmis).
- [ ] Support multilingue FR/EN pour toutes les chaînes textuelles introduites.
- [ ] Alignement strict sur les directives de `DESIGN.md` (coins carrés/arrondis sobres max 6-8px, ombres légères, Tailwind CSS v4).
- [ ] Tests unitaires Angular Vitest vérifiant le clic et le rendu visuel.

## 4. Reste à faire

- [ ] Raccordement aux services API Angular.
- [ ] Ajout du bouton et du badge sur le composant ordonnance.
- [ ] Traduction linguistique.
- [ ] Écriture des tests unitaires frontend.
