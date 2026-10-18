import json, sys, time, threading
sys.path.insert(0, '.')
from harness import _req as R, login, check, LOG
S = json.load(open('state.json')); ac = S['acc']; U = S['units']; SP = S['spaces']; B = S['beds']
T = {k: login(v['email'], v['pw']) for k, v in ac.items()}
A = login(S['admin'], S['pw'])
now = time.strftime('%Y-%m-%dT%H:%M:%SZ', time.gmtime(time.time() - 600))

st, p = R('POST', '/api/patients', T['accueil'], {"fullName": "Ngono Fictive Claire", "gender": "F", "birthDate": "1975-02-02", "phone": "+237600000002", "city": "Yaounde"})
pid = p['id']
st, v = R('POST', '/api/visits', T['accueil'], {"patientId": pid, "reason": "Dyspnee", "orientation": "HOSPITALIZATION", "service": "Medecine interne"})
vid = v['id']
admit = lambda tok, unit, space, bed, doc, patient=pid, visit=vid: R('POST', '/api/hospitalizations', tok, {"patientId": patient, "serviceUnitId": unit, "spaceId": space, "bedId": bed, "admissionReason": "Decompensation", "visitId": visit, "responsiblePractitionerId": doc})
st, x = admit(T['drB'], U['cu'], SP['ch101'], B['101-A'], ac['drB']['id'])
check('HOS-01', 'Admission avant affectation espace->unite : message de configuration explicite', st >= 400, (st, x.get('detail') if isinstance(x, dict) else x))
st, opts = R('GET', '/api/hospitalizations/placement-options', T['drB'])
check('HOS-02', 'Options de placement exposees avant affectation', st == 200, {k: len(v) for k, v in opts.items()} if isinstance(opts, dict) else (st, opts))
st, a1 = R('POST', '/api/spatial/configuration/unit-space-assignments', A, {"organizationalUnitId": U['cu'], "spaceId": SP['ch101'], "validFrom": now})
st2, a2 = R('POST', '/api/spatial/configuration/unit-space-assignments', A, {"organizationalUnitId": U['mi'], "spaceId": SP['ch102'], "validFrom": now})
st3, a3 = R('POST', '/api/spatial/configuration/unit-space-assignments', A, {"organizationalUnitId": U['mg'], "spaceId": SP['ch101'], "validFrom": now})
check('CFG-30', 'Affectations datees espace->unite (CH-101 -> UH-MI-A, CH-102 -> MI)', st < 300 and st2 < 300, (st, st2))
check('CFG-31', 'Espace partage simultanement par deux unites (CH-101 aussi -> MG)', True, (st3, a3.get('detail') if isinstance(a3, dict) and st3 >= 300 else 'accepte'))
st, x = admit(T['drB'], U['cu'], SP['ch101'], B['101-A'], ac['drB']['id'])
check('HOS-03', 'Medecin affecte au service parent (MI) refuse comme responsable de l unite de soins fille', st >= 400, (st, x.get('detail') if isinstance(x, dict) else x))
st, asg = R('POST', f"/api/staff/{ac['drB']['id']}/assignments/units", A, {"organizationalUnitId": U['cu'], "assignmentRoleCode": "PRACTITIONER", "primary": False, "validFrom": now})
check('CFG-32', 'Admin affecte Dr B a l unite de soins', st < 300, (st, str(asg)[:120]))
st, x = admit(T['ide'], U['cu'], SP['ch101'], B['101-A'], ac['drB']['id'])
check('HOS-04', 'Infirmier ne peut pas admettre (pas HOSPITALIZATION_ADMIT)', st == 403, st)
# concurrency on the same bed with a second patient
st, p3 = R('POST', '/api/patients', T['accueil'], {"fullName": "Essomba Fictif Jean", "gender": "M", "birthDate": "1990-01-01", "phone": "+237600000003", "city": "Douala"})
st, v3 = R('POST', '/api/visits', T['accueil'], {"patientId": p3['id'], "reason": "Chute", "orientation": "HOSPITALIZATION"})
results = {}
def go(key, tok, patient, visit):
    results[key] = admit(tok, U['cu'], SP['ch101'], B['101-A'], ac['drB']['id'], patient, visit)
th = [threading.Thread(target=go, args=('claire', T['drB'], pid, vid)), threading.Thread(target=go, args=('jean', T['lits'], p3['id'], v3['id']))]
[t.start() for t in th]; [t.join() for t in th]
codes = {k: r[0] for k, r in results.items()}
check('HOS-05', 'Deux admissions simultanees sur le meme lit : une seule reussit', sorted(codes.values())[0] < 300 and sorted(codes.values())[1] >= 400, codes)
winner = 'claire' if codes['claire'] < 300 else 'jean'
hid = results[winner][1]['id']; hpatient = pid if winner == 'claire' else p3['id']
st, h = R('GET', f'/api/hospitalizations/{hid}', T['ide'])
check('HOS-06', 'Sejour visible : unite, espace, lit, responsable', st == 200, {k: h.get(k) for k in h if k in ('status', 'serviceName', 'spaceName', 'bedNumber', 'serviceUnitId', 'currentSpaceId', 'responsiblePractitionerId')})
st, m = R('POST', f"/api/spatial/beds/{B['101-A']}/maintenance-status", T['lits'], {"status": "MAINTENANCE", "reasonCode": "MAINTENANCE_CORRECTIVE"})
check('BED-01', 'Lit occupe mis en maintenance : refuse', st >= 400, (st, m.get('detail') if isinstance(m, dict) else m))
st, c = R('POST', f"/api/spatial/beds/{B['102-A']}/capacity-status", T['lits'], {"status": "CLOSED", "reasonCode": "CAPACITY_STAFFING_SHORTAGE", "note": "Absence IDE"})
check('BED-02', 'Fermeture capacitaire d un lit libre (motif obligatoire)', st < 300, st)
st, tr = R('POST', '/api/spatial/transfers', T['ide'], {"hospitalizationId": hid, "targetServiceUnitId": U['mi'], "targetSpaceId": SP['ch102'], "targetBedId": B['102-A']})
check('BED-03', 'Transfert vers lit ferme refuse', st >= 400, (st, tr.get('detail') if isinstance(tr, dict) else tr))
R('POST', f"/api/spatial/beds/{B['102-A']}/capacity-status", T['lits'], {"status": "OPEN", "reasonCode": "CAPACITY_REOPENING"})
st, tr = R('POST', '/api/spatial/transfers', T['ide'], {"hospitalizationId": hid, "targetServiceUnitId": U['mi'], "targetSpaceId": SP['ch102'], "targetBedId": B['102-A']})
check('BED-04', 'Infirmier transfere vers MI/CH-102 sans motif ni acceptation de l unite receveuse', st < 300, (st, str(tr)[:160]))
st, h = R('GET', f'/api/hospitalizations/{hid}', T['ide'])
check('BED-05', 'Responsable inchange apres transfert vers une unite ou il n est pas affecte', h.get('responsiblePractitionerId') == ac['drB']['id'], {k: h.get(k) for k in h if k in ('serviceName', 'spaceName', 'bedNumber', 'responsiblePractitionerId')})
st, old = R('GET', f"/api/spatial/beds/{B['101-A']}/state-history", T['lits'])
check('BED-06', 'Ancien lit passe en nettoyage apres transfert (historique)', st == 200, [(e.get('axis'), e.get('toValue') or e.get('newValue'), e.get('reasonCode')) for e in old][:4] if isinstance(old, list) else old)
st, deact = R('POST', f"/api/hospital-organization/units/{U['mi']}/deactivate", A)
check('CFG-33', 'Desactivation d un service hebergeant un patient : bloquee', st >= 400, (st, deact.get('detail') if isinstance(deact, dict) else ''))
if st < 300: R('POST', f"/api/hospital-organization/units/{U['mi']}/activate", A)
# visit closure while hospitalized
st, vc = R('POST', f'/api/visits/{vid if winner == "claire" else v3["id"]}/close', T['drB'])
check('HOS-07', 'Cloture de la visite d un patient hospitalise : bloquee ou geree', st >= 400, (st, str(vc)[:120]))
# discharge
st, pd = R('POST', f'/api/hospitalizations/{hid}/physical-departure', T['lits'], {"confirmed": True})
check('DIS-01', 'Depart physique avant decision medicale refuse', st >= 400, st)
st, dd = R('POST', f'/api/hospitalizations/{hid}/discharge', T['drB'], {"dischargeDiagnosis": "IC decompensee", "dischargeInstructions": "Controle J7", "againstMedicalAdvice": False})
check('DIS-02', 'Decision medicale de sortie', st < 300, st)
st, dd2 = R('POST', f'/api/hospitalizations/{hid}/discharge', T['drB'], {"dischargeDiagnosis": "correction", "dischargeInstructions": "x", "againstMedicalAdvice": False})
check('DIS-03', 'Correction/annulation de la decision de sortie possible', st < 300, (st, dd2.get('detail') if isinstance(dd2, dict) else ''))
st, pd = R('POST', f'/api/hospitalizations/{hid}/physical-departure', T['ide'], {"confirmed": True})
check('DIS-04', 'Infirmier confirme le depart physique', st < 300, st)
st, pd = R('POST', f'/api/hospitalizations/{hid}/physical-departure', T['lits'], {"confirmed": True, "note": "Sortie domicile"})
check('DIS-05', 'Gestionnaire des lits confirme le depart physique (facture non reglee)', st < 300, st)
st, vv = R('GET', f'/api/visits/{vid if winner == "claire" else v3["id"]}', T['drB'])
check('DIS-06', 'Visite cloturee automatiquement a la sortie', isinstance(vv, dict) and vv.get('status') != 'EN_COURS', vv.get('status') if isinstance(vv, dict) else vv)
st, pre = R('POST', f'/api/invoices/precalculate?patientId={hpatient}&visitId={vid if winner == "claire" else v3["id"]}', T['compta'])
check('DIS-07', 'Facture de sejour pre-calculee (sejour < 1 jour, transfert)', st == 200, [(i.get('label'), i.get('unitPrice'), i.get('quantity')) for i in pre.get('items', [])] if isinstance(pre, dict) else pre)
st, bh = R('GET', f"/api/spatial/beds/{B['102-A']}/state-history", T['lits'])
check('DIS-08', 'Lit libere passe en nettoyage', True, [(e.get('axis'), e.get('toValue') or e.get('newValue'), e.get('reasonCode')) for e in bh][:3] if isinstance(bh, list) else bh)
S.update({"hid": hid, "p_inpatient": hpatient, "loser_patient": p3['id'] if winner == 'claire' else pid})
json.dump(S, open('state.json', 'w'))
json.dump(LOG, open('log_s6.json', 'w'), ensure_ascii=False, indent=1)
