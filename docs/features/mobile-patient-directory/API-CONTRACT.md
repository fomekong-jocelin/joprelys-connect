# Contrat API — Annuaire patient mobile (MOB-2809)

## Endpoint consommé

```http
GET /api/patients?q={query}
```

L'URL est relative et passe par le client API central. Le mobile n'introduit
aucun endpoint alternatif.

## Authentification et autorisation

- session professionnelle existante ;
- filtrage établissement et RBAC appliqués par le backend ;
- aucun token ni détail patient dans les logs.

## Réponse

Le mapper `PatientDirectoryItem` ne consomme que les champs réellement exposés
par le backend et nécessaires à l'identification et à l'ouverture du dossier.
Une réponse non autorisée ou une erreur réseau suit le mapping central MOB-2804.

## Compatibilité

Ce document ne modifie pas le contrat backend. Il décrit la consommation mobile
de l'endpoint existant.
