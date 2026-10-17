import json, sys, time, re
sys.path.insert(0, '.')
from harness import _req as R, login, check, LOG, latest_mail, text_of
S = json.load(open('state.json'))
A = login(S['admin'], S['pw'])
def unit(code, typ, cat=None, parent=None, name=None):
    st, u = R('POST', '/api/hospital-organization/units', A, {"code": code, "unitType": typ, "parentId": parent, "name": name, "serviceCatalogCode": cat})
    if st >= 300: print('unit err', code, st, u)
    return st, u
st, pole = unit('POLE-MED', 'POLE', name='Pôle Médecine')
check('CFG-10', 'Créer un pôle', st in (200,201), st)
st, mg = unit('SRV-MG', 'SERVICE', 'GENERAL_MEDICINE', pole['id'])
check('CFG-11', 'Créer service Médecine générale (catalogue)', st in (200,201), st)
st, mi = unit('SRV-MI', 'SERVICE', 'INTERNAL_MEDICINE', pole['id'])
st, urg = unit('SRV-URG', 'SERVICE', 'EMERGENCY')
check('CFG-12', 'Créer service Urgences sans pôle (racine)', st in (200,201), (st, urg if st>=300 else ''))
st, cu = unit('UH-MI-A', 'CARE_UNIT', parent=mi['id'], name='Hospitalisation MI - Aile A')
check('CFG-13', 'Créer unité de soins rattachée au service', st in (200,201), st)
st, lab = unit('SRV-LAB', 'SERVICE', 'LABORATORY')
st, kine = unit('SRV-KINE', 'SERVICE', 'PHYSIOTHERAPY')
check('CFG-14', 'Service hors catalogue (Kinésithérapie) refusé', st >= 400, (st, kine))
# locations
st, lt = R('GET', '/api/spatial/configuration/location-types', A); print('location types', lt)
st, stypes = R('GET', '/api/spatial/configuration/space-types', A); print('space types', [s.get('code') for s in stypes] if isinstance(stypes, list) else stypes)
st, site = R('POST', '/api/spatial/configuration/locations', A, {"code": "SITE-1", "name": "Site principal", "nodeType": "SITE"})
st, bat = R('POST', '/api/spatial/configuration/locations', A, {"parentId": site['id'], "code": "BAT-A", "name": "Bâtiment A", "nodeType": "BUILDING"})
st, et1 = R('POST', '/api/spatial/configuration/locations', A, {"parentId": bat['id'], "code": "ET-1", "name": "Étage 1", "nodeType": "FLOOR"})
check('CFG-20', 'Créer site/bâtiment/étage', st in (200,201), st)
codes = [s.get('code') for s in stypes]
room_type = next((c for c in codes if 'ROOM' in c or 'CHAMBRE' in c), codes[0])
cons_type = next((c for c in codes if 'CONSULT' in c), codes[0])
st, ch101 = R('POST', '/api/spatial/configuration/spaces', A, {"locationNodeId": et1['id'], "code": "CH-101", "name": "Chambre 101", "spaceTypeCode": room_type, "enableInpatientProfile": True})
check('CFG-21', f'Créer chambre ({room_type}) avec profil hébergement', st in (200,201), (st, ch101 if st>=300 else ''))
st, ch102 = R('POST', '/api/spatial/configuration/spaces', A, {"locationNodeId": et1['id'], "code": "CH-102", "name": "Chambre 102", "spaceTypeCode": room_type, "enableInpatientProfile": True})
st, box = R('POST', '/api/spatial/configuration/spaces', A, {"locationNodeId": et1['id'], "code": "BOX-C1", "name": "Box consultation 1", "spaceTypeCode": cons_type, "enableInpatientProfile": False})
check('CFG-22', f'Créer box de consultation ({cons_type})', st in (200,201), (st, box if st>=300 else ''))
beds = {}
for sp, nums in ((ch101, ['101-A', '101-B']), (ch102, ['102-A'])):
    for n in nums:
        st, b = R('POST', '/api/spatial/configuration/beds', A, {"spaceId": sp['id'], "bedNumber": n})
        beds[n] = b
        if st >= 300: print('bed err', st, b)
check('CFG-23', 'Créer 3 lits', len([b for b in beds.values() if isinstance(b, dict) and b.get('id')]) == 3, {k: (v.get('status'), v.get('capacityStatus'), v.get('readinessStatus')) for k, v in beds.items() if isinstance(v, dict)})
st, bx = R('POST', '/api/spatial/configuration/beds', A, {"spaceId": box['id'], "bedNumber": "BOX-1"})
check('CFG-24', 'Lit dans un espace sans profil hébergement : refusé', st >= 400, (st, bx))
S.update({"units": {"pole": pole['id'], "mg": mg['id'], "mi": mi['id'], "urg": urg['id'], "cu": cu['id'], "lab": lab['id']},
          "spaces": {"ch101": ch101['id'], "ch102": ch102['id'], "box": box['id']}, "beds": {k: v['id'] for k, v in beds.items()},
          "space_types": codes})
json.dump(S, open('state.json', 'w'))
json.dump(LOG, open('log_s2.json', 'w'), ensure_ascii=False, indent=1)
