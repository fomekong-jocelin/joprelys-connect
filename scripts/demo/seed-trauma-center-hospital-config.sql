-- DEMO ONLY — TRAUMA CENTER
-- HOS-ORG / HOS-LOC configuration for the Saturday demo.
--
-- IMPORTANT:
-- - This is NOT a Flyway migration.
-- - It does not create or alter patients, users, staff assignments or clinical data.
-- - It expects the organization "TRAUMA CENTER" to already exist.
-- - It is intentionally idempotent on organizational/location/space codes.
-- - Existing bed state is never reset on re-run.

BEGIN;

DO $$
DECLARE
    v_org_id UUID;
    v_match_count INTEGER;
    v_now TIMESTAMPTZ := NOW();

    -- Organizational units
    v_pol_crit UUID;
    v_pol_chir UUID;
    v_pol_med UUID;
    v_pol_mtech UUID;
    v_srv_urg UUID;
    v_srv_rea UUID;
    v_srv_chir UUID;
    v_srv_anes UUID;
    v_srv_bloc UUID;
    v_srv_mg UUID;
    v_srv_hosp UUID;
    v_srv_img UUID;
    v_srv_lab UUID;
    v_srv_phar UUID;

    -- Geography
    v_site UUID;
    v_building UUID;
    v_floor_rdc UUID;
    v_floor_1 UUID;
    v_zone_urg UUID;
    v_zone_cons UUID;
    v_zone_mtech UUID;
    v_zone_bloc UUID;
    v_zone_hosp UUID;
    v_zone_rea UUID;

    -- Spaces
    v_box_01 UUID;
    v_box_02 UUID;
    v_urg_soin UUID;
    v_urg_att UUID;
    v_cons_01 UUID;
    v_cons_02 UUID;
    v_img_01 UUID;
    v_lab_01 UUID;
    v_phar_01 UUID;
    v_bloc_01 UUID;
    v_sspi_01 UUID;
    v_ch_101 UUID;
    v_ch_102 UUID;
    v_rea_01 UUID;
BEGIN
    SELECT COUNT(*)
      INTO v_match_count
      FROM organizations
     WHERE UPPER(TRIM(name)) = 'TRAUMA CENTER';

    IF v_match_count = 0 THEN
        RAISE EXCEPTION 'TRAUMA CENTER introuvable dans organizations. Créez d''abord l''établissement depuis l''application.';
    ELSIF v_match_count > 1 THEN
        RAISE EXCEPTION 'Plusieurs établissements TRAUMA CENTER existent. Le seed refuse de choisir automatiquement un tenant.';
    END IF;

    SELECT id
      INTO v_org_id
      FROM organizations
     WHERE UPPER(TRIM(name)) = 'TRAUMA CENTER';

    ---------------------------------------------------------------------------
    -- 1. ORGANISATION HOSPITALIÈRE
    ---------------------------------------------------------------------------

    INSERT INTO organizational_units
        (id, organization_id, parent_id, code, name, unit_type, service_catalog_code, active, created_at, updated_at)
    VALUES
        ('10000000-0000-4000-8000-000000000001', v_org_id, NULL, 'POL-CRIT', 'Urgences & soins critiques', 'POLE', NULL, TRUE, v_now, v_now),
        ('10000000-0000-4000-8000-000000000002', v_org_id, NULL, 'POL-CHIR', 'Chirurgie', 'POLE', NULL, TRUE, v_now, v_now),
        ('10000000-0000-4000-8000-000000000003', v_org_id, NULL, 'POL-MED', 'Médecine & hospitalisation', 'POLE', NULL, TRUE, v_now, v_now),
        ('10000000-0000-4000-8000-000000000004', v_org_id, NULL, 'POL-MTECH', 'Médico-technique', 'POLE', NULL, TRUE, v_now, v_now)
    ON CONFLICT (organization_id, code) DO UPDATE SET
        name = EXCLUDED.name,
        unit_type = EXCLUDED.unit_type,
        service_catalog_code = NULL,
        active = TRUE,
        updated_at = EXCLUDED.updated_at;

    SELECT id INTO v_pol_crit  FROM organizational_units WHERE organization_id = v_org_id AND code = 'POL-CRIT';
    SELECT id INTO v_pol_chir  FROM organizational_units WHERE organization_id = v_org_id AND code = 'POL-CHIR';
    SELECT id INTO v_pol_med   FROM organizational_units WHERE organization_id = v_org_id AND code = 'POL-MED';
    SELECT id INTO v_pol_mtech FROM organizational_units WHERE organization_id = v_org_id AND code = 'POL-MTECH';

    INSERT INTO organizational_units
        (id, organization_id, parent_id, code, name, unit_type, service_catalog_code, active, created_at, updated_at)
    VALUES
        ('11000000-0000-4000-8000-000000000001', v_org_id, v_pol_crit,  'SRV-URG',  NULL, 'SERVICE', 'EMERGENCY',          TRUE, v_now, v_now),
        ('11000000-0000-4000-8000-000000000002', v_org_id, v_pol_crit,  'SRV-REA',  NULL, 'SERVICE', 'INTENSIVE_CARE',     TRUE, v_now, v_now),
        ('11000000-0000-4000-8000-000000000003', v_org_id, v_pol_chir,  'SRV-CHIR', NULL, 'SERVICE', 'GENERAL_SURGERY',    TRUE, v_now, v_now),
        ('11000000-0000-4000-8000-000000000004', v_org_id, v_pol_chir,  'SRV-ANES', NULL, 'SERVICE', 'ANESTHESIA',         TRUE, v_now, v_now),
        ('11000000-0000-4000-8000-000000000005', v_org_id, v_pol_chir,  'SRV-BLOC', NULL, 'SERVICE', 'OPERATING_THEATRE',  TRUE, v_now, v_now),
        ('11000000-0000-4000-8000-000000000006', v_org_id, v_pol_med,   'SRV-MG',   NULL, 'SERVICE', 'GENERAL_MEDICINE',   TRUE, v_now, v_now),
        ('11000000-0000-4000-8000-000000000007', v_org_id, v_pol_med,   'SRV-HOSP', NULL, 'SERVICE', 'INPATIENT_GENERAL',  TRUE, v_now, v_now),
        ('11000000-0000-4000-8000-000000000008', v_org_id, v_pol_mtech, 'SRV-IMG',  NULL, 'SERVICE', 'IMAGING',            TRUE, v_now, v_now),
        ('11000000-0000-4000-8000-000000000009', v_org_id, v_pol_mtech, 'SRV-LAB',  NULL, 'SERVICE', 'LABORATORY',         TRUE, v_now, v_now),
        ('11000000-0000-4000-8000-000000000010', v_org_id, v_pol_mtech, 'SRV-PHAR', NULL, 'SERVICE', 'PHARMACY',           TRUE, v_now, v_now)
    ON CONFLICT (organization_id, code) DO UPDATE SET
        parent_id = EXCLUDED.parent_id,
        name = NULL,
        unit_type = 'SERVICE',
        service_catalog_code = EXCLUDED.service_catalog_code,
        active = TRUE,
        updated_at = EXCLUDED.updated_at;

    SELECT id INTO v_srv_urg  FROM organizational_units WHERE organization_id = v_org_id AND code = 'SRV-URG';
    SELECT id INTO v_srv_rea  FROM organizational_units WHERE organization_id = v_org_id AND code = 'SRV-REA';
    SELECT id INTO v_srv_chir FROM organizational_units WHERE organization_id = v_org_id AND code = 'SRV-CHIR';
    SELECT id INTO v_srv_anes FROM organizational_units WHERE organization_id = v_org_id AND code = 'SRV-ANES';
    SELECT id INTO v_srv_bloc FROM organizational_units WHERE organization_id = v_org_id AND code = 'SRV-BLOC';
    SELECT id INTO v_srv_mg   FROM organizational_units WHERE organization_id = v_org_id AND code = 'SRV-MG';
    SELECT id INTO v_srv_hosp FROM organizational_units WHERE organization_id = v_org_id AND code = 'SRV-HOSP';
    SELECT id INTO v_srv_img  FROM organizational_units WHERE organization_id = v_org_id AND code = 'SRV-IMG';
    SELECT id INTO v_srv_lab  FROM organizational_units WHERE organization_id = v_org_id AND code = 'SRV-LAB';
    SELECT id INTO v_srv_phar FROM organizational_units WHERE organization_id = v_org_id AND code = 'SRV-PHAR';

    ---------------------------------------------------------------------------
    -- 2. GÉOGRAPHIE
    ---------------------------------------------------------------------------

    INSERT INTO facility_location_nodes
        (id, organization_id, parent_id, code, name, node_type, active, created_at, updated_at)
    VALUES
        ('20000000-0000-4000-8000-000000000001', v_org_id, NULL, 'SITE-01', 'Site principal', 'SITE', TRUE, v_now, v_now)
    ON CONFLICT (organization_id, code) DO UPDATE SET
        parent_id = NULL, name = EXCLUDED.name, node_type = 'SITE', active = TRUE, updated_at = EXCLUDED.updated_at;
    SELECT id INTO v_site FROM facility_location_nodes WHERE organization_id = v_org_id AND code = 'SITE-01';

    INSERT INTO facility_location_nodes
        (id, organization_id, parent_id, code, name, node_type, active, created_at, updated_at)
    VALUES
        ('20000000-0000-4000-8000-000000000002', v_org_id, v_site, 'BAT-01', 'Bâtiment principal', 'BUILDING', TRUE, v_now, v_now)
    ON CONFLICT (organization_id, code) DO UPDATE SET
        parent_id = EXCLUDED.parent_id, name = EXCLUDED.name, node_type = 'BUILDING', active = TRUE, updated_at = EXCLUDED.updated_at;
    SELECT id INTO v_building FROM facility_location_nodes WHERE organization_id = v_org_id AND code = 'BAT-01';

    INSERT INTO facility_location_nodes
        (id, organization_id, parent_id, code, name, node_type, active, created_at, updated_at)
    VALUES
        ('20000000-0000-4000-8000-000000000003', v_org_id, v_building, 'FL-RDC', 'Rez-de-chaussée', 'FLOOR', TRUE, v_now, v_now),
        ('20000000-0000-4000-8000-000000000004', v_org_id, v_building, 'FL-01', 'Étage 1', 'FLOOR', TRUE, v_now, v_now)
    ON CONFLICT (organization_id, code) DO UPDATE SET
        parent_id = EXCLUDED.parent_id, name = EXCLUDED.name, node_type = 'FLOOR', active = TRUE, updated_at = EXCLUDED.updated_at;
    SELECT id INTO v_floor_rdc FROM facility_location_nodes WHERE organization_id = v_org_id AND code = 'FL-RDC';
    SELECT id INTO v_floor_1   FROM facility_location_nodes WHERE organization_id = v_org_id AND code = 'FL-01';

    INSERT INTO facility_location_nodes
        (id, organization_id, parent_id, code, name, node_type, active, created_at, updated_at)
    VALUES
        ('21000000-0000-4000-8000-000000000001', v_org_id, v_floor_rdc, 'ZN-URG',   'Zone Urgences',       'ZONE', TRUE, v_now, v_now),
        ('21000000-0000-4000-8000-000000000002', v_org_id, v_floor_rdc, 'ZN-CONS',  'Zone Consultations',   'ZONE', TRUE, v_now, v_now),
        ('21000000-0000-4000-8000-000000000003', v_org_id, v_floor_rdc, 'ZN-MTECH', 'Zone Médico-technique','ZONE', TRUE, v_now, v_now),
        ('21000000-0000-4000-8000-000000000004', v_org_id, v_floor_rdc, 'ZN-BLOC',  'Zone Bloc opératoire', 'ZONE', TRUE, v_now, v_now),
        ('21000000-0000-4000-8000-000000000005', v_org_id, v_floor_1,   'ZN-HOSP',  'Zone Hospitalisation', 'ZONE', TRUE, v_now, v_now),
        ('21000000-0000-4000-8000-000000000006', v_org_id, v_floor_1,   'ZN-REA',   'Zone Réanimation',     'ZONE', TRUE, v_now, v_now)
    ON CONFLICT (organization_id, code) DO UPDATE SET
        parent_id = EXCLUDED.parent_id, name = EXCLUDED.name, node_type = 'ZONE', active = TRUE, updated_at = EXCLUDED.updated_at;

    SELECT id INTO v_zone_urg   FROM facility_location_nodes WHERE organization_id = v_org_id AND code = 'ZN-URG';
    SELECT id INTO v_zone_cons  FROM facility_location_nodes WHERE organization_id = v_org_id AND code = 'ZN-CONS';
    SELECT id INTO v_zone_mtech FROM facility_location_nodes WHERE organization_id = v_org_id AND code = 'ZN-MTECH';
    SELECT id INTO v_zone_bloc  FROM facility_location_nodes WHERE organization_id = v_org_id AND code = 'ZN-BLOC';
    SELECT id INTO v_zone_hosp  FROM facility_location_nodes WHERE organization_id = v_org_id AND code = 'ZN-HOSP';
    SELECT id INTO v_zone_rea   FROM facility_location_nodes WHERE organization_id = v_org_id AND code = 'ZN-REA';

    ---------------------------------------------------------------------------
    -- 3. ESPACES PHYSIQUES
    ---------------------------------------------------------------------------

    INSERT INTO facility_spaces
        (id, organization_id, location_node_id, code, name, space_type_code, active, created_at, updated_at)
    VALUES
        ('30000000-0000-4000-8000-000000000001', v_org_id, v_zone_urg,   'URG-BOX-01',  'Box urgence 01',           'EMERGENCY_BOX',      TRUE, v_now, v_now),
        ('30000000-0000-4000-8000-000000000002', v_org_id, v_zone_urg,   'URG-BOX-02',  'Box urgence 02',           'EMERGENCY_BOX',      TRUE, v_now, v_now),
        ('30000000-0000-4000-8000-000000000003', v_org_id, v_zone_urg,   'URG-SOIN-01', 'Salle de soins urgences',  'TREATMENT_ROOM',     TRUE, v_now, v_now),
        ('30000000-0000-4000-8000-000000000004', v_org_id, v_zone_urg,   'URG-ATT-01',  'Salle d’attente urgences', 'WAITING_ROOM',       TRUE, v_now, v_now),
        ('30000000-0000-4000-8000-000000000005', v_org_id, v_zone_cons,  'CONS-01',     'Consultation 01',          'CONSULTATION_ROOM',  TRUE, v_now, v_now),
        ('30000000-0000-4000-8000-000000000006', v_org_id, v_zone_cons,  'CONS-02',     'Consultation 02',          'CONSULTATION_ROOM',  TRUE, v_now, v_now),
        ('30000000-0000-4000-8000-000000000007', v_org_id, v_zone_mtech, 'IMG-01',      'Imagerie 01',              'IMAGING_ROOM',       TRUE, v_now, v_now),
        ('30000000-0000-4000-8000-000000000008', v_org_id, v_zone_mtech, 'LAB-01',      'Laboratoire central',      'LABORATORY_ROOM',    TRUE, v_now, v_now),
        ('30000000-0000-4000-8000-000000000009', v_org_id, v_zone_mtech, 'PHAR-01',     'Pharmacie centrale',       'PHARMACY',           TRUE, v_now, v_now),
        ('30000000-0000-4000-8000-000000000010', v_org_id, v_zone_bloc,  'BLOC-01',     'Bloc opératoire 01',       'OPERATING_ROOM',     TRUE, v_now, v_now),
        ('30000000-0000-4000-8000-000000000011', v_org_id, v_zone_bloc,  'SSPI-01',     'Salle de réveil 01',       'RECOVERY_ROOM',      TRUE, v_now, v_now),
        ('30000000-0000-4000-8000-000000000012', v_org_id, v_zone_hosp,  'CH-101',      'Chambre 101',              'HOSPITAL_ROOM',      TRUE, v_now, v_now),
        ('30000000-0000-4000-8000-000000000013', v_org_id, v_zone_hosp,  'CH-102',      'Chambre 102',              'HOSPITAL_ROOM',      TRUE, v_now, v_now),
        ('30000000-0000-4000-8000-000000000014', v_org_id, v_zone_rea,   'REA-01',      'Réanimation 01',           'ICU_ROOM',           TRUE, v_now, v_now)
    ON CONFLICT (organization_id, code) DO UPDATE SET
        location_node_id = EXCLUDED.location_node_id,
        name = EXCLUDED.name,
        space_type_code = EXCLUDED.space_type_code,
        active = TRUE,
        updated_at = EXCLUDED.updated_at;

    SELECT id INTO v_box_01   FROM facility_spaces WHERE organization_id = v_org_id AND code = 'URG-BOX-01';
    SELECT id INTO v_box_02   FROM facility_spaces WHERE organization_id = v_org_id AND code = 'URG-BOX-02';
    SELECT id INTO v_urg_soin FROM facility_spaces WHERE organization_id = v_org_id AND code = 'URG-SOIN-01';
    SELECT id INTO v_urg_att  FROM facility_spaces WHERE organization_id = v_org_id AND code = 'URG-ATT-01';
    SELECT id INTO v_cons_01  FROM facility_spaces WHERE organization_id = v_org_id AND code = 'CONS-01';
    SELECT id INTO v_cons_02  FROM facility_spaces WHERE organization_id = v_org_id AND code = 'CONS-02';
    SELECT id INTO v_img_01   FROM facility_spaces WHERE organization_id = v_org_id AND code = 'IMG-01';
    SELECT id INTO v_lab_01   FROM facility_spaces WHERE organization_id = v_org_id AND code = 'LAB-01';
    SELECT id INTO v_phar_01  FROM facility_spaces WHERE organization_id = v_org_id AND code = 'PHAR-01';
    SELECT id INTO v_bloc_01  FROM facility_spaces WHERE organization_id = v_org_id AND code = 'BLOC-01';
    SELECT id INTO v_sspi_01  FROM facility_spaces WHERE organization_id = v_org_id AND code = 'SSPI-01';
    SELECT id INTO v_ch_101   FROM facility_spaces WHERE organization_id = v_org_id AND code = 'CH-101';
    SELECT id INTO v_ch_102   FROM facility_spaces WHERE organization_id = v_org_id AND code = 'CH-102';
    SELECT id INTO v_rea_01   FROM facility_spaces WHERE organization_id = v_org_id AND code = 'REA-01';

    ---------------------------------------------------------------------------
    -- 4. PROFILS D'HÉBERGEMENT + LITS
    ---------------------------------------------------------------------------

    INSERT INTO inpatient_space_profiles
        (space_id, organization_id, space_type_code, comfort_level, created_at, updated_at)
    VALUES
        (v_ch_101, v_org_id, 'HOSPITAL_ROOM', 'STANDARD',       v_now, v_now),
        (v_ch_102, v_org_id, 'HOSPITAL_ROOM', 'VIP',            v_now, v_now),
        (v_rea_01, v_org_id, 'ICU_ROOM',      'INTENSIVE_CARE', v_now, v_now)
    ON CONFLICT (space_id) DO UPDATE SET
        comfort_level = EXCLUDED.comfort_level,
        updated_at = EXCLUDED.updated_at;

    INSERT INTO beds
        (id, space_id, bed_number, status, capacity_status, readiness_status, organization_id, version, created_at, updated_at)
    VALUES
        ('40000000-0000-4000-8000-000000000001', v_ch_101, '101-A', 'FREE', 'OPEN', 'READY', v_org_id, 0, v_now, v_now),
        ('40000000-0000-4000-8000-000000000002', v_ch_101, '101-B', 'FREE', 'OPEN', 'READY', v_org_id, 0, v_now, v_now),
        ('40000000-0000-4000-8000-000000000003', v_ch_102, '102-A', 'FREE', 'OPEN', 'READY', v_org_id, 0, v_now, v_now),
        ('40000000-0000-4000-8000-000000000004', v_rea_01, 'REA-01-A', 'FREE', 'OPEN', 'READY', v_org_id, 0, v_now, v_now),
        ('40000000-0000-4000-8000-000000000005', v_rea_01, 'REA-01-B', 'FREE', 'OPEN', 'READY', v_org_id, 0, v_now, v_now)
    ON CONFLICT (organization_id, space_id, bed_number) DO NOTHING;

    ---------------------------------------------------------------------------
    -- 5. RATTACHEMENTS UNITÉ ↔ ESPACE
    ---------------------------------------------------------------------------

    -- Helper pattern repeated deliberately to keep the script plain PostgreSQL.
    INSERT INTO organizational_unit_space_assignments (id, organization_id, organizational_unit_id, space_id, valid_from, valid_to, created_at, updated_at)
    SELECT '50000000-0000-4000-8000-000000000001', v_org_id, v_srv_urg, v_box_01, v_now, NULL, v_now, v_now
    WHERE NOT EXISTS (SELECT 1 FROM organizational_unit_space_assignments WHERE organization_id=v_org_id AND organizational_unit_id=v_srv_urg AND space_id=v_box_01 AND valid_to IS NULL);

    INSERT INTO organizational_unit_space_assignments SELECT '50000000-0000-4000-8000-000000000002', v_org_id, v_srv_urg, v_box_02, v_now, NULL, v_now, v_now
    WHERE NOT EXISTS (SELECT 1 FROM organizational_unit_space_assignments WHERE organization_id=v_org_id AND organizational_unit_id=v_srv_urg AND space_id=v_box_02 AND valid_to IS NULL);

    INSERT INTO organizational_unit_space_assignments SELECT '50000000-0000-4000-8000-000000000003', v_org_id, v_srv_urg, v_urg_soin, v_now, NULL, v_now, v_now
    WHERE NOT EXISTS (SELECT 1 FROM organizational_unit_space_assignments WHERE organization_id=v_org_id AND organizational_unit_id=v_srv_urg AND space_id=v_urg_soin AND valid_to IS NULL);

    INSERT INTO organizational_unit_space_assignments SELECT '50000000-0000-4000-8000-000000000004', v_org_id, v_srv_urg, v_urg_att, v_now, NULL, v_now, v_now
    WHERE NOT EXISTS (SELECT 1 FROM organizational_unit_space_assignments WHERE organization_id=v_org_id AND organizational_unit_id=v_srv_urg AND space_id=v_urg_att AND valid_to IS NULL);

    -- Shared imaging space: Emergency + Imaging.
    INSERT INTO organizational_unit_space_assignments SELECT '50000000-0000-4000-8000-000000000005', v_org_id, v_srv_urg, v_img_01, v_now, NULL, v_now, v_now
    WHERE NOT EXISTS (SELECT 1 FROM organizational_unit_space_assignments WHERE organization_id=v_org_id AND organizational_unit_id=v_srv_urg AND space_id=v_img_01 AND valid_to IS NULL);

    INSERT INTO organizational_unit_space_assignments SELECT '50000000-0000-4000-8000-000000000006', v_org_id, v_srv_img, v_img_01, v_now, NULL, v_now, v_now
    WHERE NOT EXISTS (SELECT 1 FROM organizational_unit_space_assignments WHERE organization_id=v_org_id AND organizational_unit_id=v_srv_img AND space_id=v_img_01 AND valid_to IS NULL);

    INSERT INTO organizational_unit_space_assignments SELECT '50000000-0000-4000-8000-000000000007', v_org_id, v_srv_mg, v_cons_01, v_now, NULL, v_now, v_now
    WHERE NOT EXISTS (SELECT 1 FROM organizational_unit_space_assignments WHERE organization_id=v_org_id AND organizational_unit_id=v_srv_mg AND space_id=v_cons_01 AND valid_to IS NULL);

    -- Consultation 02 shared by general medicine and general surgery.
    INSERT INTO organizational_unit_space_assignments SELECT '50000000-0000-4000-8000-000000000008', v_org_id, v_srv_mg, v_cons_02, v_now, NULL, v_now, v_now
    WHERE NOT EXISTS (SELECT 1 FROM organizational_unit_space_assignments WHERE organization_id=v_org_id AND organizational_unit_id=v_srv_mg AND space_id=v_cons_02 AND valid_to IS NULL);

    INSERT INTO organizational_unit_space_assignments SELECT '50000000-0000-4000-8000-000000000009', v_org_id, v_srv_chir, v_cons_02, v_now, NULL, v_now, v_now
    WHERE NOT EXISTS (SELECT 1 FROM organizational_unit_space_assignments WHERE organization_id=v_org_id AND organizational_unit_id=v_srv_chir AND space_id=v_cons_02 AND valid_to IS NULL);

    -- Operating theatre intentionally shared by theatre, surgery and anesthesia.
    INSERT INTO organizational_unit_space_assignments SELECT '50000000-0000-4000-8000-000000000010', v_org_id, v_srv_bloc, v_bloc_01, v_now, NULL, v_now, v_now
    WHERE NOT EXISTS (SELECT 1 FROM organizational_unit_space_assignments WHERE organization_id=v_org_id AND organizational_unit_id=v_srv_bloc AND space_id=v_bloc_01 AND valid_to IS NULL);

    INSERT INTO organizational_unit_space_assignments SELECT '50000000-0000-4000-8000-000000000011', v_org_id, v_srv_chir, v_bloc_01, v_now, NULL, v_now, v_now
    WHERE NOT EXISTS (SELECT 1 FROM organizational_unit_space_assignments WHERE organization_id=v_org_id AND organizational_unit_id=v_srv_chir AND space_id=v_bloc_01 AND valid_to IS NULL);

    INSERT INTO organizational_unit_space_assignments SELECT '50000000-0000-4000-8000-000000000012', v_org_id, v_srv_anes, v_bloc_01, v_now, NULL, v_now, v_now
    WHERE NOT EXISTS (SELECT 1 FROM organizational_unit_space_assignments WHERE organization_id=v_org_id AND organizational_unit_id=v_srv_anes AND space_id=v_bloc_01 AND valid_to IS NULL);

    INSERT INTO organizational_unit_space_assignments SELECT '50000000-0000-4000-8000-000000000013', v_org_id, v_srv_anes, v_sspi_01, v_now, NULL, v_now, v_now
    WHERE NOT EXISTS (SELECT 1 FROM organizational_unit_space_assignments WHERE organization_id=v_org_id AND organizational_unit_id=v_srv_anes AND space_id=v_sspi_01 AND valid_to IS NULL);

    INSERT INTO organizational_unit_space_assignments SELECT '50000000-0000-4000-8000-000000000014', v_org_id, v_srv_lab, v_lab_01, v_now, NULL, v_now, v_now
    WHERE NOT EXISTS (SELECT 1 FROM organizational_unit_space_assignments WHERE organization_id=v_org_id AND organizational_unit_id=v_srv_lab AND space_id=v_lab_01 AND valid_to IS NULL);

    INSERT INTO organizational_unit_space_assignments SELECT '50000000-0000-4000-8000-000000000015', v_org_id, v_srv_phar, v_phar_01, v_now, NULL, v_now, v_now
    WHERE NOT EXISTS (SELECT 1 FROM organizational_unit_space_assignments WHERE organization_id=v_org_id AND organizational_unit_id=v_srv_phar AND space_id=v_phar_01 AND valid_to IS NULL);

    INSERT INTO organizational_unit_space_assignments SELECT '50000000-0000-4000-8000-000000000016', v_org_id, v_srv_hosp, v_ch_101, v_now, NULL, v_now, v_now
    WHERE NOT EXISTS (SELECT 1 FROM organizational_unit_space_assignments WHERE organization_id=v_org_id AND organizational_unit_id=v_srv_hosp AND space_id=v_ch_101 AND valid_to IS NULL);

    INSERT INTO organizational_unit_space_assignments SELECT '50000000-0000-4000-8000-000000000017', v_org_id, v_srv_hosp, v_ch_102, v_now, NULL, v_now, v_now
    WHERE NOT EXISTS (SELECT 1 FROM organizational_unit_space_assignments WHERE organization_id=v_org_id AND organizational_unit_id=v_srv_hosp AND space_id=v_ch_102 AND valid_to IS NULL);

    INSERT INTO organizational_unit_space_assignments SELECT '50000000-0000-4000-8000-000000000018', v_org_id, v_srv_rea, v_rea_01, v_now, NULL, v_now, v_now
    WHERE NOT EXISTS (SELECT 1 FROM organizational_unit_space_assignments WHERE organization_id=v_org_id AND organizational_unit_id=v_srv_rea AND space_id=v_rea_01 AND valid_to IS NULL);

    RAISE NOTICE 'TRAUMA CENTER configuré: organisation, géographie, espaces, profils, lits et rattachements.';
END
$$;

COMMIT;

-- Verification summary
SELECT
    o.name AS organization,
    (SELECT COUNT(*) FROM organizational_units u WHERE u.organization_id = o.id AND u.active) AS active_units,
    (SELECT COUNT(*) FROM facility_location_nodes l WHERE l.organization_id = o.id AND l.active) AS active_locations,
    (SELECT COUNT(*) FROM facility_spaces s WHERE s.organization_id = o.id AND s.active) AS active_spaces,
    (SELECT COUNT(*) FROM beds b WHERE b.organization_id = o.id) AS beds,
    (SELECT COUNT(*) FROM organizational_unit_space_assignments a WHERE a.organization_id = o.id AND a.valid_to IS NULL) AS active_unit_space_assignments
FROM organizations o
WHERE UPPER(TRIM(o.name)) = 'TRAUMA CENTER';
