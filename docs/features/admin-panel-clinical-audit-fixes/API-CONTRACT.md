# Contrats modifiés

## Profil et organisation
GET /api/profile/assignments : compte professionnel authentifié sans établissement → 200 avec specialties/unitAssignments vides. Authentification et STAFF_PROFILE_ACCESS restent exigés. Une panne du contexte professionnel reste visible comme avertissement Angular indépendant.
POST/PUT /api/hospital-organization/units : SERVICE conserve serviceCatalogCode obligatoire et actif ; name devient facultatif, 2–120 caractères après normalisation. null/vide → libellé catalogue. Aucun code ni type historique supprimé. Pôles/départements/unités conservent le nom obligatoire et l'isolation tenant.
Les catalogues de services, spécialités, espaces et rôles d'affectation sont enrichis. MEDICAL_HEAD exige un médecin ; NURSE_MANAGER exige un infirmier ou une sage-femme. Les périodes et contrôles tenant existants sont conservés.

## Actes cliniques — rupture d'autorisation
POST /api/consultations/{id}/prescription et POST /api/prescriptions/{id}/transmit : PRESCRIPTION_WRITE.
POST /api/prescriptions/{id}/finalize et PATCH /api/prescriptions/{id}/cancel : PRESCRIPTION_SIGN, avec médecin actif vérifié serveur.
POST /api/visits/{id}/close : VISIT_MANAGE + CLINICAL_SIGN + CONSULTATION_LOCK. Une prescription DRAFT associée exige aussi PRESCRIPTION_SIGN ; la clôture ne contourne plus la complétude de la prescription.
Les réponses consultation/prescription ajoutent signedBy, signedAt et signedContentHash, null pour les actes non signés/historiques. La clôture place le compte rendu en VALIDEE ; nouvelle écriture d'une consultation signée sur une visite active → 409. Le contrôle existant d'une visite clôturée conserve son 400. Les prescriptions actives restent non éditables.

## Pharmacie — rupture du parcours anonyme
POST /api/public/pharmacy/prescriptions/verify reste une lecture avec numéro/PIN ; la réponse ajoute pharmaceuticalValidated.
POST /api/public/pharmacy/prescriptions/validate : PHARMACY_VALIDATE ; JSON {prescriptionNumber, pinCode, reviewNotes}. Notes non vides ≤2000 caractères. Ordonnance ACTIVE/PARTIALLY_DISPENSED ; revue déjà enregistrée ou statut inadmissible → 409 ; succès → 204.
POST /api/public/pharmacy/prescriptions/dispense : PHARMACY_DISPENSE, numéro/PIN existants, validation préalable obligatoire (sinon 409). Les restrictions de statut, expiration et quantité restent contrôlées ; lignes dupliquées rejetées. Succès → 204. Absence d'autorité → 403, même pour une URL sous /public.
La consultation par PIN n'est pas un contrôle automatique d'interactions ; la revue est un acte humain tracé. Aucune note pharmaceutique détaillée ajoutée à la réponse publique.

## Téléversement de signature PNG

POST /api/files/upload conserve le multipart {file,type} et la réponse 201 {filePath,viewUrl}. FILE_UPLOAD reste requis pour type=signature, y compris la préparation administrative des assets d'un médecin. L'association de signature à son propre profil reste réservée au médecin actif ; autre profil → 403. Image décodable PNG/JPEG → stockage PNG canonique ; invalide, non pris en charge ou dimensions excessives → 400. Chemin uploads/signature/{uuid}.png, réponse de consultation image/png ; PUT /api/profile garde le contrat signaturePath existant et le contrôle médecin. Aucun certificat numérique inclus dans l'image ou la réponse.

## Navigation

/audit-trail redirige vers /clinic/rbac?tab=audit sous le guard RBAC_MANAGE existant ; l'API du journal conserve RBAC_READ/RBAC_MANAGE. Ce journal concerne les autorisations. /interop expose l'état API des établissements sous ORGANIZATION_MANAGE et renvoie vers leur fiche de gestion des clés.
