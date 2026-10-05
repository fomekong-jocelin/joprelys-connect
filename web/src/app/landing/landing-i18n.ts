export interface LandingContent {
  nav: {
    skip: string;
    dpu: string;
    aiAssistant: string;
    clinicManagement: string;
    ecosystem: string;
    security: string;
    faq: string;
    allopharma: string;
    patientPortal: string;
    doctorLogin: string;
    dashboard: string;
    themeLight: string;
    themeDark: string;
    switchLang: string;
    openMenu: string;
  };
  hero: {
    title: string;
    subtitle: string;
    ctaDemo: string;
    ctaDoctor: string;
    allopharmaLabel: string;
    allopharmaLink: string;
    mockup: {
      patientName: string;
      patientId: string;
      patientInfo: string;
      badgeDpu: string;
      vitalsBp: string;
      vitalsHr: string;
      vitalsTemp: string;
      vitalsSpo2: string;
      aiTitle: string;
      aiSecurity: string;
      aiTranscript: string;
      tagReason: string;
      tagVitals: string;
      tagStock: string;
    };
  };
  metrics: {
    m1Value: string;
    m1Label: string;
    m1Sub: string;
    m2Value: string;
    m2Label: string;
    m2Sub: string;
    m3Value: string;
    m3Label: string;
    m3Sub: string;
    m4Value: string;
    m4Label: string;
    m4Sub: string;
  };
  comparison: {
    tag: string;
    title: string;
    subtitle: string;
    oldTitle: string;
    oldPoints: string[];
    newTitle: string;
    newPoints: string[];
  };
  features: {
    tag: string;
    title: string;
    subtitle: string;
    f1Title: string;
    f1Desc: string;
    f1Points: string[];
    f2Badge: string;
    f2Title: string;
    f2Desc: string;
    f2Points: string[];
    f3Title: string;
    f3Desc: string;
    f3Points: string[];
    f4Title: string;
    f4Desc: string;
    f4Points: string[];
  };
  ecosystem: {
    tag: string;
    title: string;
    subtitle: string;
    c1Badge: string;
    c1Title: string;
    c1Desc: string;
    c1Target: string;
    c2Badge: string;
    c2Title: string;
    c2Desc: string;
    c2Link: string;
    c3Badge: string;
    c3Title: string;
    c3Desc: string;
    c3Target: string;
    c4Badge: string;
    c4Title: string;
    c4Desc: string;
    c4Target: string;
  };
  security: {
    tag: string;
    title: string;
    desc: string;
    b1Title: string;
    b1Desc: string;
    b2Title: string;
    b2Desc: string;
    b3Title: string;
    b3Desc: string;
    shieldTitle: string;
    shieldDesc: string;
  };
  faq: {
    tag: string;
    title: string;
    subtitle: string;
    items: { question: string; answer: string }[];
  };
  demo: {
    tag: string;
    title: string;
    subtitle: string;
    nameLabel: string;
    namePlaceholder: string;
    orgLabel: string;
    orgPlaceholder: string;
    roleLabel: string;
    roleOptions: { value: string; label: string }[];
    phoneLabel: string;
    phonePlaceholder: string;
    cityLabel: string;
    cityPlaceholder: string;
    emailLabel: string;
    emailPlaceholder: string;
    messageLabel: string;
    messagePlaceholder: string;
    submitBtn: string;
    submittingBtn: string;
    privacyNote: string;
    successTitle: string;
    successDesc: string;
    whatsappBtn: string;
    errorDesc: string;
    referenceLabel: string;
  };
  footer: {
    slogan: string;
    desc: string;
    youtubeText: string;
    col1Title: string;
    col2Title: string;
    col3Title: string;
    cgu: string;
    privacy: string;
    consent: string;
    support: string;
    location: string;
    copyright: string;
    subCopyright: string;
  };
}

export const LANDING_I18N: Record<'fr' | 'en', LandingContent> = {
  fr: {
    nav: {
      skip: 'Aller au contenu principal',
      dpu: 'Dossier Patient',
      aiAssistant: 'Assistant IA',
      clinicManagement: 'Gestion Clinique',
      ecosystem: 'Écosystème',
      security: 'Sécurité & HDS',
      faq: 'FAQ',
      allopharma: 'AllôPharma ↗',
      patientPortal: 'Espace Patient',
      doctorLogin: 'Connexion Praticien',
      dashboard: 'Tableau de bord',
      themeLight: 'Activer le mode clair',
      themeDark: 'Activer le mode sombre',
      switchLang: 'Passer en anglais',
      openMenu: 'Ouvrir le menu de navigation',
    },
    hero: {
      title: 'Parce qu’elle est précieuse, nous innovons pour la protéger.',
      subtitle:
        "Joprelys Connect est la suite hospitalière unifiée conçue pour le Cameroun et l'Afrique : Dossier Patient Partagé (DPU), consultation médicale augmentée par IA vocale et coordination clinique en temps réel.",
      ctaDemo: 'Demander une démonstration',
      ctaDoctor: 'Espace Praticien',
      allopharmaLabel: 'Solution officinale associée : ',
      allopharmaLink: 'AllôPharma · Observatoire & Pharmacies de garde',
      mockup: {
        patientName: 'FOMEKONG Jocelin',
        patientId: 'ID: JOP-CM-2026-084',
        patientInfo: '38 ans · Groupe O+ · Allergie : Pénicilline',
        badgeDpu: 'Dossier DPU Actif',
        vitalsBp: 'Tension Artérielle',
        vitalsHr: 'Fréquence Cardiaque',
        vitalsTemp: 'Température',
        vitalsSpo2: 'SpO2',
        aiTitle: 'Dictée & Assistant IA',
        aiSecurity: 'AES-256',
        aiTranscript:
          '« Le patient rapporte des céphalées matinales persistantes depuis 3 jours avec asthénie fébrile. Examen oropharyngé sans écoulement... »',
        tagReason: 'Motif : Syndrome céphalalgique',
        tagVitals: 'Constantes validées',
        tagStock: 'AllôPharma : Molécules en stock à Douala',
      },
    },
    metrics: {
      m1Value: '100%',
      m1Label: 'Dossier Patient Unique (DPU)',
      m1Sub: 'Antécédents consolidés sans perte',
      m2Value: '-45%',
      m2Label: 'Temps de Saisie Médicale',
      m2Sub: "Grâce à l'Assistant Vocal IA",
      m3Value: 'FHIR & HL7',
      m3Label: 'Interopérabilité Ouverte',
      m3Sub: 'Cliniques, hôpitaux, labos & pharmacies',
      m4Value: '24/7',
      m4Label: 'Continuité des Soins',
      m4Sub: 'Résilience réseau & souveraineté des données',
    },
    comparison: {
      tag: 'Transformation Digitale',
      title: 'Pourquoi les cliniques et hôpitaux choisissent Joprelys Connect',
      subtitle:
        'Le passage du papier au dossier numérique doit apporter un gain de temps immédiat aux soignants et une sécurité renforcée pour chaque patient.',
      oldTitle: 'Pratiques traditionnelles morcelées',
      oldPoints: [
        "Dossiers cartonnés égarés, dégradés ou illisibles lors d'une réadmission.",
        'Perte de 20 minutes par consultation pour réécrire les constantes et antécédents.',
        "Absence de traçabilité sur les stocks de pharmacie hospitalière et les lits d'hospitalisation.",
        'Patient orienté vers des pharmacies sans garantie de trouver les molécules prescrites.',
      ],
      newTitle: "L'Expérience Joprelys Connect",
      newPoints: [
        'Dossier Patient Unique (DPU) accessible en 1 clic par les soignants autorisés.',
        'Assistant IA de dictée vocale qui structure le compte rendu médical en temps réel.',
        "Supervision des lits et admissions en direct pour fluidifier les flux d'urgences.",
        'Passerelle avec AllôPharma pour localiser instantanément les pharmacies de garde.',
      ],
    },
    features: {
      tag: 'Fonctionnalités Clés',
      title: 'Une suite médicale complète pour chaque acteur du soin',
      subtitle:
        "De l'accueil du patient à la pharmacie, en passant par la salle de consultation et le service d'hospitalisation.",
      f1Title: 'Dossier Patient Unique (DPU)',
      f1Desc:
        'Vue consolidée à 360° : historique des consultations, biométrie, antécédents familiaux, allergies critiques, hospitalisations passées et ordonnances.',
      f1Points: [
        "Numéro d'identification patient permanent",
        "Alertes d'allergies et interactions médicamenteuses",
        'Partage sécurisé entre services hospitaliers',
      ],
      f2Badge: 'Technologie Brevetée',
      f2Title: 'Assistant Consultation & Dictée IA',
      f2Desc:
        'Écoute ambiante ou dictée directe qui retranscrit fidèlement le dialogue médecin-patient et propose une ébauche de compte rendu structuré.',
      f2Points: [
        'Gain de 40% sur le temps de saisie administrative',
        'Vocabulaire médical spécialisé adapté au contexte local',
        'Le médecin garde la validation finale souveraine',
      ],
      f3Title: 'Admissions & Gestion des Lits',
      f3Desc:
        "Visualisation en temps réel du taux d'occupation, régulation des urgences, planification des sorties et gestion fine des unités de soins.",
      f3Points: [
        'Plan de salle interactif et statut des lits',
        'Parcours urgences avec traçabilité médico-légale',
        'Facturation intégrée et encaissements DAF',
      ],
      f4Title: 'Portail Patient & Interopérabilité',
      f4Desc:
        "Espace sécurisé pour les patients afin de consulter leurs résultats d'analyses, ordonnances électroniques et rendez-vous sans déplacement inutile.",
      f4Points: [
        'Connexion sécurisée par OTP mobile',
        "Connectivité laboratoires d'analyses & imagerie",
        'Rappels de suivi par SMS et WhatsApp',
      ],
    },
    ecosystem: {
      tag: 'Synergie de Marque',
      title: "L'Écosystème Joprelys HealthTech",
      subtitle:
        "Une vision intégrée qui relie l'hôpital, la clinique, la pharmacie d'officine et le domicile du patient.",
      c1Badge: 'Plateforme Cœur',
      c1Title: 'Joprelys Connect',
      c1Desc:
        "Système d'Information Hospitalier (SIH) et Dossier Patient Partagé pour cliniques, centres médicaux et spécialistes de santé.",
      c1Target: 'Public : Médecins, Infirmiers, DAF, Gestionnaires de santé',
      c2Badge: 'Réseau Officinal',
      c2Title: 'AllôPharma',
      c2Desc:
        'Observatoire citoyen des médicaments, géolocalisation des pharmacies de garde à Douala et Yaoundé, et vérification de disponibilité.',
      c2Link: 'Visiter allopharma.online ↗',
      c3Badge: 'Portail Citoyen',
      c3Title: 'Joprelys Patient',
      c3Desc:
        'Carnet de santé numérique individuel et familial, accès direct aux ordonnances, rappels de prises et carnet de vaccination.',
      c3Target: 'Public : Patients, Familles, Aidants',
      c4Badge: 'Interopérabilité',
      c4Title: 'Joprelys Interop',
      c4Desc:
        'Passerelles de communication conformes aux normes HL7 FHIR pour connecter laboratoires, mutuelles et ministères de la santé.',
      c4Target: 'Public : Laboratoires, Assurances, Partenaires techniques',
    },
    security: {
      tag: 'Protection Médicale',
      title: 'Des standards de sécurité intransigeants pour vos données de santé',
      desc:
        "La confidentialité médicale est le fondement de notre engagement. Joprelys Connect applique les meilleures pratiques internationales d'Hébergement de Données de Santé (HDS) et garantit la souveraineté complète de vos archives.",
      b1Title: 'Chiffrement AES-256 de bout en bout',
      b1Desc: 'Données chiffrées au repos et en transit via protocoles TLS 1.3 stricts.',
      b2Title: "Piste d'Audit Inviolable (Audit Trail)",
      b2Desc:
        'Chaque consultation, modification ou prescription est horodatée et signée cryptographiquement.',
      b3Title: 'Cloisonnement Multi-Clinique Étanche',
      b3Desc:
        "Chaque établissement conserve l'étanchéité absolue de ses dossiers médicaux.",
      shieldTitle: 'HDS & RGPD Conforme',
      shieldDesc:
        'Garantie du secret professionnel médical et souveraineté locale des données',
    },
    faq: {
      tag: 'Questions Pratiques',
      title: 'Foire aux questions',
      subtitle:
        'Tout ce que vous devez savoir avant de déployer Joprelys Connect au sein de votre établissement de santé.',
      items: [
        {
          question: "Comment Joprelys Connect s'adapte-t-il aux coupures d'électricité ou d'Internet ?",
          answer:
            "Joprelys Connect est conçu selon une architecture résiliente optimisée pour les contextes africains. L'application intègre un mode de saisie réactif et une mise en cache locale qui permet de continuer les consultations même en cas d'instabilité réseau, avec synchronisation sécurisée dès le rétablissement de la connexion.",
        },
        {
          question: "L'assistant IA remplace-t-il le praticien lors de la consultation ?",
          answer:
            "Absolument pas. L'assistant IA agit en copilote sécurisé pour libérer le praticien de la charge administrative. Il transcrit les échanges et propose une structuration du compte rendu, mais chaque donnée clinique doit être formellement relue et validée par le médecin avant d'intégrer le dossier patient.",
        },
        {
          question: 'Comment Joprelys Connect est-il connecté avec AllôPharma ?',
          answer:
            "Joprelys Connect et AllôPharma font partie du même écosystème Joprelys HealthTech. Une fois la consultation terminée, la prescription peut être sécurisée et le patient peut identifier en un clic les pharmacies de garde disposant réellement des molécules prescrites via AllôPharma.",
        },
        {
          question: 'Nos données de santé sont-elles hébergées en toute sécurité ?',
          answer:
            "Oui. Joprelys Connect applique un chiffrement de niveau militaire AES-256 en transit et au repos, une architecture étanche multi-établissements et une piste d'audit inviolable garantissant une traçabilité totale des accès médicaux, dans le respect du secret professionnel.",
        },
        {
          question: 'Quelle est la durée de déploiement et de formation pour une clinique ?',
          answer:
            "Une clinique pilote peut être opérationnelle en moins de 48 heures. L'interface a été conçue pour éliminer toute friction cognitive : les médecins et infirmiers prennent en main le Dossier Patient Partagé en moins de 30 minutes de formation.",
        },
        {
          question: 'Comment planifier une démonstration ou rejoindre le programme pilote ?',
          answer:
            'Remplissez simplement le formulaire de demande de démonstration ci-dessous ou contactez directement notre équipe à Douala. Nous configurons un espace de démonstration personnalisé avec les spécialités de votre établissement.',
        },
      ],
    },
    demo: {
      tag: 'Programme Pilote 2026',
      title: 'Demandez une démonstration personnalisée',
      subtitle:
        'Découvrez comment équiper votre clinique ou cabinet médical en 48 heures à Douala, Yaoundé ou en région.',
      nameLabel: 'Nom et Prénom *',
      namePlaceholder: 'Dr Sophie Martin',
      orgLabel: 'Établissement de santé *',
      orgPlaceholder: 'Clinique Saint-Jean',
      roleLabel: 'Votre fonction',
      roleOptions: [
        { value: 'directeur', label: 'Directeur / DAF' },
        { value: 'medecin', label: 'Médecin Praticien' },
        { value: 'major', label: 'Major / Cadre infirmier' },
        { value: 'pharmacien', label: 'Pharmacien hospitalier' },
        { value: 'autre', label: 'Autre professionnel' },
      ],
      phoneLabel: 'Téléphone / WhatsApp *',
      phonePlaceholder: '+237 6XX XX XX XX',
      cityLabel: 'Ville',
      cityPlaceholder: 'Douala, Yaoundé...',
      emailLabel: 'Adresse e-mail professionnelle',
      emailPlaceholder: 'contact@clinique.cm',
      messageLabel: 'Vos priorités ou questions',
      messagePlaceholder:
        'Ex : Digitalisation des admissions urgences et dossier médical informatisé...',
      submitBtn: 'Demander une démo',
      submittingBtn: 'Envoi en cours…',
      privacyNote:
        'Vos coordonnées sont confidentielles et ne seront jamais partagées à des tiers.',
      successTitle: 'Demande enregistrée avec succès !',
      successDesc:
        'Notre équipe Joprelys HealthTech prendra contact avec vous sous 24h ouvrées pour planifier votre session de démonstration personnalisée.',
      whatsappBtn: 'Discuter directement sur WhatsApp ↗',
      errorDesc: 'L’enregistrement de votre demande n’a pas pu être confirmé. Réessayez ou contactez-nous sur WhatsApp.',
      referenceLabel: 'Référence de la demande :',
    },
    footer: {
      slogan: '« Parce qu’elle est précieuse, nous innovons pour la protéger. »',
      desc:
        "Joprelys Connect unifie la gestion hospitalière et le Dossier Patient Partagé pour offrir des soins d'excellence au Cameroun et en Afrique.",
      youtubeText: 'Chaîne YouTube @Joprelys',
      col1Title: 'Solutions',
      col2Title: 'Écosystème HealthTech',
      col3Title: 'Informations & Légal',
      cgu: "Conditions d'Utilisation (CGU)",
      privacy: 'Politique de Confidentialité',
      consent: 'Gestion des Consentements',
      support: 'Contact :',
      location: 'Douala & Yaoundé, Cameroun',
      copyright: '© 2026 JOPRELYS SARL · Fondé par Jocelin Fomekong. Tous droits réservés.',
      subCopyright: 'Plateforme médicale certifiée pour cliniques pilotes.',
    },
  },
  en: {
    nav: {
      skip: 'Skip to main content',
      dpu: 'Patient Record',
      aiAssistant: 'AI Assistant',
      clinicManagement: 'Clinical Management',
      ecosystem: 'Ecosystem',
      security: 'Security & HDS',
      faq: 'FAQ',
      allopharma: 'AllôPharma ↗',
      patientPortal: 'Patient Portal',
      doctorLogin: 'Doctor Login',
      dashboard: 'Dashboard',
      themeLight: 'Switch to light mode',
      themeDark: 'Switch to dark mode',
      switchLang: 'Passer en français',
      openMenu: 'Open navigation menu',
    },
    hero: {
      title: 'Because it is precious, we innovate to protect it.',
      subtitle:
        'Joprelys Connect is the unified hospital suite built for Cameroon and Africa: Shared Electronic Patient Record (DPU), Voice AI-assisted consultation, and real-time clinical care coordination.',
      ctaDemo: 'Request a Live Demo',
      ctaDoctor: 'Doctor Portal',
      allopharmaLabel: 'Associated pharmacy platform: ',
      allopharmaLink: 'AllôPharma · Medicine Observatory & Duty Pharmacies',
      mockup: {
        patientName: 'FOMEKONG Jocelin',
        patientId: 'ID: JOP-CM-2026-084',
        patientInfo: '38 yo · Blood type O+ · Allergy: Penicillin',
        badgeDpu: 'Active DPU Record',
        vitalsBp: 'Blood Pressure',
        vitalsHr: 'Heart Rate',
        vitalsTemp: 'Temperature',
        vitalsSpo2: 'SpO2',
        aiTitle: 'Voice Dictation & AI',
        aiSecurity: 'AES-256',
        aiTranscript:
          '« Patient reports persistent morning headaches for 3 days accompanied by febrile asthenia. Oropharyngeal examination shows no discharge... »',
        tagReason: 'Chief Complaint: Cephalea syndrome',
        tagVitals: 'Vitals validated',
        tagStock: 'AllôPharma: In stock at Douala pharmacies',
      },
    },
    metrics: {
      m1Value: '100%',
      m1Label: 'Unified Patient Record (DPU)',
      m1Sub: 'Consolidated history with zero data loss',
      m2Value: '-45%',
      m2Label: 'Clinical Dictation Time',
      m2Sub: 'Powered by Ambient Voice AI',
      m3Value: 'FHIR & HL7',
      m3Label: 'Open Interoperability',
      m3Sub: 'Clinics, hospitals, labs & pharmacies',
      m4Value: '24/7',
      m4Label: 'Continuity of Care',
      m4Sub: 'Network resilience & data sovereignty',
    },
    comparison: {
      tag: 'Digital Healthcare Transformation',
      title: 'Why Hospitals and Clinics Choose Joprelys Connect',
      subtitle:
        'Moving from paper to unified electronic health records saves immediate clinical hours and ensures total safety for every patient.',
      oldTitle: 'Fragmented Paper-Based Practices',
      oldPoints: [
        'Paper files lost, damaged, or unreadable upon patient readmission.',
        '20 minutes lost per consultation re-entering medical background and vitals.',
        'Zero real-time visibility over pharmacy stock levels and hospital bed turnover.',
        'Patients sent across the city with no guarantee of finding prescribed drugs.',
      ],
      newTitle: 'The Joprelys Connect Experience',
      newPoints: [
        'Unified Patient Record (DPU) accessible in 1 click by authorized caregivers.',
        'Ambient Voice AI that structures clinical notes in real time during consultation.',
        'Live bed turnover and emergency triage monitoring for seamless care pathways.',
        'Direct AllôPharma integration to instantly pinpoint on-duty pharmacies with stock.',
      ],
    },
    features: {
      tag: 'Key Features',
      title: 'A Complete Clinical Suite for Healthcare Professionals',
      subtitle:
        'From patient check-in to pharmacy dispensation, consultation rooms, and inpatient wards.',
      f1Title: 'Unified Patient Record (DPU)',
      f1Desc:
        '360° consolidated medical history: past consultations, biometrics, family history, drug allergies, inpatient admissions, and digital prescriptions.',
      f1Points: [
        'Permanent nationwide patient ID system',
        'Real-time drug allergy & interaction warnings',
        'Secure multi-specialty hospital data sharing',
      ],
      f2Badge: 'Patented Technology',
      f2Title: 'Consultation Assistant & Voice AI',
      f2Desc:
        'Ambient listening or direct voice dictation that accurately captures doctor-patient dialogues and generates structured consultation summaries.',
      f2Points: [
        '40% reduction in administrative typing time',
        'Specialized medical vocabulary adapted to local African healthcare context',
        'The doctor retains sovereign validation over all clinical records',
      ],
      f3Title: 'Inpatient Admissions & Bed Management',
      f3Desc:
        'Live ward occupancy tracking, emergency flow triage, planned discharges, and precision nursing care management.',
      f3Points: [
        'Interactive floor plans and real-time bed status',
        'Emergency pathways with tamper-proof medico-legal logs',
        'Integrated billing and cash-register finance management',
      ],
      f4Title: 'Patient Portal & Interoperability',
      f4Desc:
        'Secure patient portal to check lab results, e-prescriptions, and appointments without unnecessary round trips.',
      f4Points: [
        'Safe login with mobile OTP verification',
        'Direct lab & medical imaging integrations',
        'Automated follow-up reminders via SMS & WhatsApp',
      ],
    },
    ecosystem: {
      tag: 'HealthTech Ecosystem',
      title: 'The Joprelys HealthTech Ecosystem',
      subtitle:
        'An integrated vision connecting hospital wards, outpatient clinics, retail pharmacies, and patient homes.',
      c1Badge: 'Core Platform',
      c1Title: 'Joprelys Connect',
      c1Desc:
        'Hospital Information System (HIS) and Shared Patient Record for clinics, health centers, and medical specialists.',
      c1Target: 'Audience: Doctors, Nurses, CFOs, Hospital Administrators',
      c2Badge: 'Pharmacy Network',
      c2Title: 'AllôPharma',
      c2Desc:
        'Citizen medication observatory, geolocation of on-duty pharmacies in Douala and Yaoundé, and live stock verification.',
      c2Link: 'Visit allopharma.online ↗',
      c3Badge: 'Citizen Portal',
      c3Title: 'Joprelys Patient',
      c3Desc:
        'Personal and family digital health record, instant prescription access, intake reminders, and immunization tracking.',
      c3Target: 'Audience: Patients, Families, Caregivers',
      c4Badge: 'Interoperability',
      c4Title: 'Joprelys Interop',
      c4Desc:
        'HL7 FHIR compliant gateways connecting medical laboratories, health insurance providers, and ministries of health.',
      c4Target: 'Audience: Laboratories, Insurers, Technical Partners',
    },
    security: {
      tag: 'Medical Data Protection',
      title: 'Uncompromising Security Standards for Healthcare Data',
      desc:
        'Medical confidentiality is the cornerstone of our commitment. Joprelys Connect applies international Health Data Hosting (HDS) best practices and guarantees complete local sovereignty over your archives.',
      b1Title: 'End-to-End AES-256 Encryption',
      b1Desc: 'Data encrypted at rest and in transit via strict TLS 1.3 cryptographic protocols.',
      b2Title: 'Tamper-Proof Audit Trail',
      b2Desc:
        'Every consultation, edit, or prescription is cryptographically signed and timestamped.',
      b3Title: 'Strict Multi-Tenant Clinic Isolation',
      b3Desc:
        'Each health facility retains absolute isolation and full ownership over its medical records.',
      shieldTitle: 'HDS & GDPR Compliant',
      shieldDesc:
        'Guaranteed medical professional secrecy and sovereign healthcare data protection',
    },
    faq: {
      tag: 'Practical Answers',
      title: 'Frequently Asked Questions',
      subtitle:
        'Everything you need to know before deploying Joprelys Connect in your healthcare facility.',
      items: [
        {
          question: 'How does Joprelys Connect handle power or Internet outages?',
          answer:
            'Joprelys Connect is engineered specifically for resilient African infrastructure. The application features an offline-first caching layer that allows doctors and nurses to continue consultations uninterrupted even during network blackouts, syncing securely once connection is restored.',
        },
        {
          question: 'Does the AI assistant replace the physician during consultation?',
          answer:
            'Absolutely not. The AI assistant acts strictly as an administrative co-pilot to relieve clinicians of repetitive paperwork. It transcribes conversations and formats clinical notes, but every single entry must be formally reviewed and validated by the doctor.',
        },
        {
          question: 'How does Joprelys Connect connect with AllôPharma?',
          answer:
            'Joprelys Connect and AllôPharma are sister platforms in the Joprelys HealthTech ecosystem. Once a consultation finishes, the e-prescription can be verified and the patient can immediately locate the nearest duty pharmacies with confirmed stock via AllôPharma.',
        },
        {
          question: 'Is healthcare data hosted with certified security?',
          answer:
            'Yes. Joprelys Connect utilizes military-grade AES-256 encryption at rest and in transit, complete multi-clinic data partitioning, and an immutable cryptographic audit trail ensuring full traceability in strict compliance with medical confidentiality.',
        },
        {
          question: 'How long does deployment and staff training take for a clinic?',
          answer:
            'A pilot clinic can be fully operational in less than 48 hours. The interface was crafted to remove cognitive friction: doctors, majors, and administrative staff master the Unified Patient Record in under 30 minutes of guided onboarding.',
        },
        {
          question: 'How can our clinic schedule a demo or join the pilot program?',
          answer:
            'Simply fill out the demo request form below or contact our team in Douala. We configure a personalized trial environment tailored to your medical specialties.',
        },
      ],
    },
    demo: {
      tag: 'Pilot Program 2026',
      title: 'Request a Personalized Demonstration',
      subtitle:
        'Discover how to digitize your clinic or medical practice in 48 hours in Douala, Yaoundé, or regional centers.',
      nameLabel: 'Full Name *',
      namePlaceholder: 'Dr Sophie Martin',
      orgLabel: 'Healthcare Facility *',
      orgPlaceholder: 'Saint-Jean Hospital',
      roleLabel: 'Your Role',
      roleOptions: [
        { value: 'directeur', label: 'Director / CFO' },
        { value: 'medecin', label: 'Practicing Physician' },
        { value: 'major', label: 'Head Nurse / Supervisor' },
        { value: 'pharmacien', label: 'Hospital Pharmacist' },
        { value: 'autre', label: 'Other Healthcare Professional' },
      ],
      phoneLabel: 'Phone / WhatsApp *',
      phonePlaceholder: '+237 6XX XX XX XX',
      cityLabel: 'City',
      cityPlaceholder: 'Douala, Yaoundé...',
      emailLabel: 'Professional Email Address',
      emailPlaceholder: 'contact@clinic.cm',
      messageLabel: 'Your Priorities or Questions',
      messagePlaceholder:
        'E.g., Digitizing emergency triage and unifying inpatient medical records...',
      submitBtn: 'Request a free demo',
      submittingBtn: 'Sending…',
      privacyNote:
        'Your contact details are strictly confidential and will never be shared with third parties.',
      successTitle: 'Demo Request Successfully Received!',
      successDesc:
        'Our Joprelys HealthTech team will contact you within 24 business hours to arrange your customized walkthrough.',
      whatsappBtn: 'Chat Directly on WhatsApp ↗',
      errorDesc: 'Your request could not be confirmed. Please try again or contact us via WhatsApp.',
      referenceLabel: 'Request reference:',
    },
    footer: {
      slogan: '« Because it is precious, we innovate to protect it. »',
      desc:
        'Joprelys Connect unifies hospital workflows and the Shared Patient Record to deliver healthcare excellence in Cameroon and across Africa.',
      youtubeText: 'YouTube Channel @Joprelys',
      col1Title: 'Solutions',
      col2Title: 'HealthTech Ecosystem',
      col3Title: 'Information & Legal',
      cgu: 'Terms of Service',
      privacy: 'Privacy Policy',
      consent: 'Consent Management',
      support: 'Contact:',
      location: 'Douala & Yaoundé, Cameroon',
      copyright: '© 2026 JOPRELYS SARL · Founded by Jocelin Fomekong. All rights reserved.',
      subCopyright: 'Certified medical software for pilot healthcare facilities.',
    },
  },
};
