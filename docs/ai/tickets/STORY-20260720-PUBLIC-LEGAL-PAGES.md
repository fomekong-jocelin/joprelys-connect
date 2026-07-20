# STORY-20260720 — Pages juridiques publiques JOPRELYS SARL

## Statut

QA TECHNIQUE VERTE — VALIDATION JURIDIQUE REQUISE

## Références

- Issue GitHub : #80
- Pull request : #81
- Branche : `feat/80-public-legal-pages`
- Type : conformité / frontend / contenu juridique
- Priorité : P0 avant publication commerciale
- Estimation : 2,5 j senior frontend / conformité
- Reviewer : Lead Frontend + sécurité + conseil juridique camerounais

## Contexte validé

- Éditeur : **JOPRELYS SARL**.
- Service utilisable au Cameroun uniquement.
- Données de production destinées à être hébergées au Cameroun.
- Droit applicable : droit camerounais.
- Référence principale : loi n° 2024/017 du 23 décembre 2024 relative à la protection des données à caractère personnel au Cameroun.

## Pages livrées

- politique de confidentialité ;
- conditions générales d’utilisation ;
- mentions légales ;
- politique cookies et traceurs ;
- politique spécifique aux données de santé ;
- politique de conservation et suppression ;
- page d’exercice des droits.

## Architecture

- composant public unique `LegalPageComponent` ;
- contenu structuré dans `legal-documents.ts` ;
- textes FR/EN dans `assets/i18n/features/legal/` ;
- routes publiques regroupées dans `legal.routes.ts` ;
- logo partagé, thème light/dark et sélection FR/EN ;
- navigation mobile-first ;
- aucun appel backend ni donnée utilisateur sur ces pages.

## Critères d’acceptation

- [x] Sept pages publiques sans garde d’authentification.
- [x] Contenu français et anglais.
- [x] Logo partagé et thèmes existants.
- [x] Contacts `@joprelys.com` affichés.
- [x] Distinction clinique / JOPRELYS SARL explicitée.
- [x] Aucune autorisation réglementaire non vérifiée n’est présentée comme acquise.
- [x] Informations légales manquantes clairement signalées.
- [x] Tests de rendu, langues, thème, routes et liens ajoutés.
- [x] CI Angular et Maven verte.
- [ ] Relecture juridique camerounaise signée.
- [ ] Informations société et hébergeur complétées.

## Validation technique

Pipeline GitHub Actions **#867** :

- tests Angular : succès ;
- build Angular production : succès ;
- build et tests Maven stricts : succès.

Le premier pipeline a détecté une taille de logo non supportée dans le pied de page. Le template a été corrigé pour utiliser la taille partagée `md`, puis l’ensemble de la CI est passé au vert.

## Informations à compléter avant publication définitive

- siège social, capital, RCCM et NIU ;
- gérant, directeur de publication et téléphone officiel ;
- identité et adresse précise de l’hébergeur camerounais ;
- formalités applicables aux données sensibles et interconnexions ;
- durées finales de conservation des dossiers médicaux et journaux d’audit.

## Validation restante

1. Vérifier visuellement FR/EN, light/dark et responsive.
2. Vérifier tous les liens et adresses e-mail.
3. Comparer les traitements réellement activés avec les politiques.
4. Obtenir la validation juridique écrite.
5. Compléter les informations société et hébergeur.
6. Retirer le bandeau « document préparatoire » après validation et complétion.
