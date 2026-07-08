# BUG-20260707 — Patient introuvable lors de la connexion patient

## Statut : RÉSOLU ✅

**Date** : 2026-07-07
**Priorité** : CRITIQUE (bloque l'accès au portail patient)
**Mode d'intervention** : Diagnostic + Engineering
**Composant** : `web/src/app/patient/portal/patient-login.component.ts`
**SemVer** : PATCH

---

## Symptôme

Le patient saisit son numéro DPU, téléphone et date de naissance, puis reçoit "Patient introuvable."
alors que le patient existe bien en base.

---

## Cause racine

Dans `PatientLoginComponent`, les champs de formulaire étaient déclarés comme Angular Signals :

```typescript
// AVANT (bugué)
globalPatientNumber = signal('');
phone = signal('');
birthDate = signal('');
otpCode = signal('');
```

Le template utilise [(ngModel)]="globalPatientNumber".

Incompatibilité critique : ngModel ne peut pas appeler .set() sur un WritableSignal.
Il réassigne la référence, détruisant le signal → valeurs vides envoyées → 404.

---

## Correction appliquée

Remplacement des signal('') par des propriétés string simples :

```typescript
// APRÈS (corrigé)
globalPatientNumber = '';
phone = '';
birthDate = '';
otpCode = '';
```

---

## Fichiers modifiés

- web/src/app/patient/portal/patient-login.component.ts

---

## Reste à faire

- [ ] Valider manuellement la connexion patient dans le navigateur
- [ ] Ajouter un test unitaire PatientLoginComponent
