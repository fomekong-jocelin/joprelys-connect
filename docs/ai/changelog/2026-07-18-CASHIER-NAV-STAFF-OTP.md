# 2026-07-18 — OTP professionnel et séparation du poste caissier

## Corrigé

- un caissier pur voit uniquement le poste caissier ;
- l’espace global devient « Facturation & gestion financière » ;
- la route globale applique la même politique que le menu ;
- les profils multi-rôles conservent les deux espaces lorsque leurs permissions le justifient.

## Sécurité

- OTP obligatoire pour tous les comptes professionnels et personnalisés ;
- aucune session avant validation du code ;
- canal patient isolé ;
- ancien OTP invalidé avant un nouvel envoi ;
- anciennes sessions professionnelles révoquées par la migration V73 ;
- erreurs d’envoi OTP explicites en français et en anglais.
