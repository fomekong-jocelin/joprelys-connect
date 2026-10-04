# Décision — Autorisations des actes cliniques et scellement serveur

Date : 2026-10-04. Retenue pour les corrections demandées de QA-20261004-ADMIN-PANEL-CLINICAL-AUDIT ; revue Tech Lead, médecin et pharmacien requise avant livraison.

## Contexte
CLINICAL_WRITE permettait aux infirmiers de prescrire. La clôture d'une visite activait une ordonnance brouillon sans contrôle de signature propre. La délivrance publique par PIN ne garantissait ni identité du pharmacien ni revue pharmaceutique.

## Décision
Séparer PRESCRIPTION_WRITE, PRESCRIPTION_SIGN, CLINICAL_SIGN, CONSULTATION_LOCK, PHARMACY_VALIDATE et PHARMACY_DISPENSE. Les médecins reçoivent les droits médicaux ; les pharmaciens les droits de revue/délivrance ; les infirmiers conservent la documentation clinique. Les administrateurs gèrent l'attribution sans recevoir automatiquement ces nouveaux actes réservés. La signature exige également un compte médecin actif côté application, même en présence d'une permission accordée à un autre profil.
La clôture existante signe et verrouille le compte rendu, puis finalise le brouillon de prescription avec sa permission explicite et ses contrôles de complétude. Les chemins d'édition et de signature partagent le verrou transactionnel de visite. La délivrance et l'annulation sont sérialisées sur l'ordonnance.
Auteur, heure UTC serveur et empreinte SHA-256 des champs sont stockés. L'API interdit l'édition du contenu signé ; l'annulation reste explicite et auditée. Les ordonnances historiques ne reçoivent pas de signature inventée.
Le PIN reste un moyen de présentation d'une ordonnance à une pharmacie externe. Sa lecture ne vaut pas validation pharmaceutique. Revue et délivrance exigent un compte authentifié avec droit dédié, ainsi que le PIN ; la revue manuelle est enregistrée avant délivrance. Aucune détection automatique d'interactions n'est prétendue.
La gouvernance d'unité réutilise les affectations datées avec MEDICAL_HEAD et NURSE_MANAGER ; elle ne crée pas une responsabilité dupliquée dans une colonne non historisée.

## Complément utilisateur — gratuit / open source / PNG

Le 2026-10-04, l'utilisateur demande une solution gratuite/open source et la possibilité de téléverser la signature du médecin, enregistrée en PNG. Décision : conserver OpenPDF et ImageIO existants pour la signature visuelle, renforcer la normalisation serveur, préserver alpha et l'insertion PDF. Aucun service payant ni nouvelle bibliothèque nécessaire.
Pour la signature cryptographique future, DSS est retenu : bibliothèque Java de création/validation PAdES sous LGPL 2.1. Choix documenté, pas d'intégration fictive ni dépendance ajoutée sans certificats/configuration et tests correspondants. Le logiciel open source ne fournit pas à lui seul les certificats qualifiés, la protection des clés ou un service de temps certifié.
Sources officielles consultées : [OpenPDF et licence](https://github.com/LibrePDF/OpenPDF), [ImageIO Java 21 — PNG/JPEG](https://docs.oracle.com/en/java/javase/21/docs/api/java.desktop/javax/imageio/package-summary.html), [DSS — licence et signatures qualifiées](https://ec.europa.eu/digital-building-blocks/sites/spaces/DIGITAL/pages/467109107/Digital%2BSignature%2BService%2B-%2BDSS).

## Conséquences

Rupture d'autorisation et du parcours pharmacie externe : MAJOR candidat, clients et rôles personnalisés à coordonner avant déploiement. Les partenaires anonymes ne peuvent plus dispenser. Deux migrations additives V113/V114, sans modification des migrations existantes.
Le scellement applicatif n'est pas une signature électronique qualifiée ni un horodatage certifié. Fournisseur de confiance, vérification ordinale et validation réglementaire restent des décisions externes ouvertes. Une empreinte en base ne protège pas contre un administrateur de base disposant du pouvoir de réécrire contenu et empreinte ; cette capacité n'est pas revendiquée.
Rollback : privilégier un correctif en avant. Les anciens binaires rétabliraient les failles d'autorisation et effaceraient les noms locaux à l'édition ; toute restauration exige arrêt des écritures, sauvegarde et revue sécurité, pas un rollback automatique.
