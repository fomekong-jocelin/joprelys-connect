import json, sys, time, uuid
sys.path.insert(0, '.')
from harness import _req as R, login, check, LOG
S = json.load(open('state.json')); ac = S['acc']; U = S['units']; SP = S['spaces']; B = S['beds']
T = {k: login(v['email'], v['pw']) for k, v in ac.items()}
A = login(S['admin'], S['pw'])
# Claire (loser of the bed race) : still waiting with an open visit; no bed queue
claire = S['loser_patient']
st, vs = R('GET', f'/api/visits/patient/{claire}', T['drB'])
cv = next((x for x in vs if x.get('status') == 'EN_COURS'), None) if isinstance(vs, list) else None
check('HOS-08', 'Patient sans lit : aucune file d attente de lit, visite EN_COURS orpheline', cv is not None, (cv or {}).get('careStage'))
# bed 101-A is CLEANING after transfer, 101-B ready
st, x = R('POST', f"/api/spatial/beds/{B['101-A']}/cleaning-status", T['ide'], {"status": "READY", "reasonCode": "CLEANING_COMPLETED"})
check('BED-07', 'Infirmier ne peut pas declarer un lit nettoye (agent hygiene requis)', st == 403, st)
st, h = R('POST', '/api/hospitalizations', T['drB'], {"patientId": claire, "serviceUnitId": U['cu'], "spaceId": SP['ch101'], "bedId": B['101-B'], "admissionReason": "Dyspnee", "visitId": cv['id'], "responsiblePractitionerId": ac['drB']['id']})
check('HOS-09', 'Admission de Claire sur 101-B', st < 300, (st, str(h)[:120]))
hid = h['id']
st, d = R('POST', f"/api/hospital-organization/units/{U['cu']}/deactivate", A)
check('CFG-34', 'Desactivation d une unite de soins avec patient hospitalise : bloquee', st >= 400, (st, d.get('detail') if isinstance(d, dict) else ''))
if st < 300:
    st2, x = R('GET', f'/api/hospitalizations/{hid}', T['drB'])
    check('CFG-35', 'Sejour actif dans une unite desactivee', True, (x.get('status'), x.get('serviceName')))
    R('POST', f"/api/hospital-organization/units/{U['cu']}/activate", A)
R('POST', f'/api/hospitalizations/{hid}/discharge', T['drB'], {"dischargeDiagnosis": "Amelioration", "dischargeInstructions": "Controle", "againstMedicalAdvice": False})
st, pd = R('POST', f'/api/hospitalizations/{hid}/physical-departure', T['lits'], {"confirmed": True})
st, vv = R('GET', f"/api/visits/{cv['id']}", T['drB'])
check('DIS-09', 'Visite cloturee apres depart physique', vv.get('status') != 'EN_COURS', vv.get('status'))
st, nv = R('POST', '/api/visits', T['accueil'], {"patientId": claire, "reason": "Controle J7", "orientation": "CONSULTATION"})
check('DIS-10', 'Retour du patient a J7 : nouvelle visite possible', st < 300, (st, nv.get('detail') if isinstance(nv, dict) else nv))
# EMERGENCY: unknown unconscious patient
req = {"requestId": str(uuid.uuid4()), "patient": {"apparentGender": "M", "estimatedAgeRange": "30-40", "physicalDescription": "Homme inconscient, cicatrice avant-bras", "foundAt": time.strftime('%Y-%m-%dT%H:%M:%SZ', time.gmtime()), "foundLocation": "Carrefour Ndokoti", "confidenceLevel": "NONE", "identityDeclarations": []},
       "emergency": {"arrivalMode": "AMBULANCE", "triageLevel": "P1", "hemodynamicStatus": "UNSTABLE", "chiefComplaint": "AVP, inconscient", "initialBpSystolic": 80, "initialBpDiastolic": 50, "initialHr": 130, "initialTemp": 36.1}}
st, e = R('POST', '/api/emergencies/provisional', T['ide'], req)
check('URG-01', 'Infirmier ouvre une urgence pour patient inconnu (identite provisoire)', st < 300, (st, str(e)[:200]))
st, e2 = R('POST', '/api/emergencies/provisional', T['ide'], req)
check('URG-02', 'Double clic (meme requestId) idempotent', st < 300 and isinstance(e2, dict) and e2.get('id') == e.get('id'), (st, e2.get('id') == e.get('id') if isinstance(e2, dict) else e2))
eid = e['id']; epid = e.get('patientId')
st, inv = R('POST', '/api/invoices', T['accueil'], {"patientId": epid})
check('URG-03', 'Facturation possible sur identite provisoire (regularisation en attente)', True, (st, inv.get('regularizationStatus') if isinstance(inv, dict) else inv))
st, rl = R('POST', f'/api/emergencies/{eid}/resuscitation', T['ide'], {"actionType": "MEDICATION", "description": "Remplissage NaCl 0.9%", "quantity": 500, "unit": "mL", "administeredAt": time.strftime('%Y-%m-%dT%H:%M:%SZ', time.gmtime())})
check('URG-04', 'Infirmier trace un geste de reanimation', st < 300, (st, str(rl)[:120]))
st, s = R('POST', f'/api/emergencies/{eid}/stabilize?orientation=ADMISSION', T['ide'])
check('URG-05', 'Infirmier ne peut pas cloturer l urgence (EMERGENCY_STABILIZE medecin)', st == 403, st)
st, s = R('POST', f'/api/emergencies/{eid}/stabilize?orientation=ADMISSION', T['drA'])
check('URG-06', 'Medecin "stabilise" avec orientation hospitalisation', st < 300, (st, {k: s.get(k) for k in ('orientation', 'stabilizedAt', 'stabilizedBy', 'visitId')} if isinstance(s, dict) else s))
st, act = R('GET', '/api/emergencies/active', T['ide'])
check('URG-07', 'Patient oriente hospitalisation mais sans lit : reste visible dans une file', isinstance(act, list) and any(x.get('id') == eid for x in act), len(act) if isinstance(act, list) else act)
st, rl = R('POST', f'/api/emergencies/{eid}/resuscitation', T['ide'], {"actionType": "MEDICATION", "description": "2e remplissage en attente de lit", "quantity": 500, "unit": "mL", "administeredAt": time.strftime('%Y-%m-%dT%H:%M:%SZ', time.gmtime())})
check('URG-08', 'Soins traces apres "stabilisation" en attente de lit', st < 300, (st, rl.get('detail') if isinstance(rl, dict) else rl))
st, s2 = R('POST', '/api/emergencies/provisional', T['ide'], dict(req, requestId=str(uuid.uuid4()), emergency=dict(req['emergency'], chiefComplaint='Arret cardio-respiratoire')))
eid2 = s2['id']; ep2 = s2['patientId']
st, s = R('POST', f'/api/emergencies/{eid2}/stabilize?orientation=DEATH', T['drA'])
check('URG-09', 'Deces enregistre via "stabiliser" orientation DEATH', st < 300, st)
st, pp = R('GET', f'/api/patients/{ep2}', T['drA'])
check('URG-10', 'Dossier patient marque decede (statut, date)', isinstance(pp, dict) and ('DECE' in str(pp.get('status')).upper() or pp.get('deceasedAt')), {k: pp.get(k) for k in ('status', 'identityStatus', 'deceasedAt')} if isinstance(pp, dict) else pp)
st, nv = R('POST', '/api/visits', T['accueil'], {"patientId": ep2, "reason": "test post-deces", "orientation": "CONSULTATION"})
check('URG-11', 'Ouverture d une visite pour un patient decede refusee', st >= 400, (st, nv.get('visitNumber') if isinstance(nv, dict) else nv))
# old emergency vitals copied to a new unrelated visit
st, pk = R('POST', '/api/patients', T['accueil'], {"fullName": "Tchoupo Fictif Marc", "gender": "M", "birthDate": "1985-03-03", "phone": "+237600000004", "city": "Douala"})
st, e3 = R('POST', '/api/emergencies', T['ide'], {"patientId": pk['id'], "arrivalMode": "WALK_IN", "triageLevel": "P3", "hemodynamicStatus": "STABLE", "chiefComplaint": "Entorse", "initialBpSystolic": 190, "initialBpDiastolic": 110, "initialHr": 99, "initialTemp": 37})
st, s = R('POST', f"/api/emergencies/{e3['id']}/stabilize?orientation=DISCHARGE", T['drA'])
st, v = R('POST', '/api/visits', T['accueil'], {"patientId": pk['id'], "reason": "Consultation programmee diabete", "orientation": "CONSULTATION"})
check('URG-12', 'Nouvelle visite : constantes de l urgence precedente recopiees sans horodatage', True, (v.get('vitals') if isinstance(v, dict) else v, v.get('careStage') if isinstance(v, dict) else ''))
S.update({"eid": eid, "epid": epid})
json.dump(S, open('state.json', 'w'))
json.dump(LOG, open('log_s7.json', 'w'), ensure_ascii=False, indent=1)
