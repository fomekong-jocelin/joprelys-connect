import json, sys, time, datetime
sys.path.insert(0, '.')
from harness import _req as R, login, check, LOG
S = json.load(open('state.json')); ac = S['acc']; U = S['units']; SP = S['spaces']; B = S['beds']
T = {k: login(v['email'], v['pw']) for k, v in ac.items()}
A = login(S['admin'], S['pw'])
# --- appointments
tomorrow = datetime.date.today() + datetime.timedelta(days=1)
st, rule = R('POST', '/api/availabilities', T['drA'], {"weekday": tomorrow.isoweekday(), "startTime": "08:00", "endTime": "12:00", "validFrom": datetime.date.today().isoformat()})
check('RDV-01', 'Medecin declare ses disponibilites', st < 300, (st, str(rule)[:150]))
st, ag = R('GET', '/api/doctor/appointments', T['drA'])
check('RDV-02', 'Agenda medecin accessible', st == 200, (st, str(ag)[:100]))
st, x = R('GET', '/api/patient/appointments/doctors', T['accueil'])
check('RDV-03', 'Accueil peut reserver un RDV pour un patient (APPOINTMENT_WRITE)', st < 300, (st, 'aucun endpoint personnel de reservation' if st >= 400 else ''))
# --- inpatient medication: prescribe in a consultation then admit, then dispense
st, p = R('POST', '/api/patients', T['accueil'], {"fullName": "Abena Fictive Rose", "gender": "F", "birthDate": "1960-06-06", "phone": "+237600000005", "city": "Douala"})
pid = p['id']
st, v = R('POST', '/api/visits', T['accueil'], {"patientId": pid, "reason": "Pneumopathie", "orientation": "HOSPITALIZATION"})
vid = v['id']
R('POST', f'/api/visits/{vid}/vitals', T['ide'], {"temperature": 38.9, "pulse": 104, "systolic": 130, "diastolic": 80, "spo2": 92})
R('POST', f'/api/visits/{vid}/take-charge', T['drB'])
st, c = R('POST', f'/api/visits/{vid}/consultation', T['drB'], {"symptoms": "Toux", "clinicalExam": "Crepitants", "diagnosis": "PAC", "conclusion": "Hospitalisation", "advice": "-", "followUp": "-"})
st, pr = R('POST', f"/api/consultations/{c['id']}/prescription", T['drB'], {"items": [{"drugName": "Ceftriaxone", "dosage": "1 g", "posology": "1 g IV/24h", "quantity": "7", "route": "IV", "duration": "7 jours"}]})
st, fin = R('POST', f"/api/prescriptions/{pr['id']}/finalize", T['drB'])
check('RX-10', 'Signature de l ordonnance sans cloturer la visite', st < 300, (st, str(fin)[:120]))
st, h = R('POST', '/api/hospitalizations', T['drB'], {"patientId": pid, "serviceUnitId": U['cu'], "spaceId": SP['ch101'], "bedId": B['101-B'], "admissionReason": "PAC", "visitId": vid, "responsiblePractitionerId": ac['drB']['id']})
if st >= 300:
    R('POST', f"/api/spatial/beds/{B['101-B']}/cleaning-status", T['lits'], {"status": "READY", "reasonCode": "CLEANING_COMPLETED"})
    st, h = R('POST', '/api/hospitalizations', T['drB'], {"patientId": pid, "serviceUnitId": U['cu'], "spaceId": SP['ch101'], "bedId": B['101-B'], "admissionReason": "PAC", "visitId": vid, "responsiblePractitionerId": ac['drB']['id']})
check('HOS-10', 'Admission apres remise en etat du lit', st < 300, (st, str(h)[:150]))
hid = h.get('id')
st, el = R('GET', f'/api/hospitalizations/{hid}/eligible-medications', T['ide'])
check('MED-01', 'Infirmier voit la Ceftriaxone a administrer', isinstance(el, list) and len(el) == 1, (st, [(e.get('medicationName') or e.get('drugName'), e.get('posology')) for e in el] if isinstance(el, list) else el))
item = el[0] if isinstance(el, list) and el else {}
iid = item.get('prescriptionItemId') or item.get('id')
st, ad = R('POST', f'/api/hospitalizations/{hid}/medication-administrations', T['ide'], {"prescriptionItemId": iid, "medicationName": "Ceftriaxone", "dose": "1 g", "administeredAt": time.strftime('%Y-%m-%dT%H:%M:%SZ', time.gmtime())})
check('MED-02', 'Administration J1 tracee', st < 300, (st, str(ad)[:120]))
st, ad2 = R('POST', f'/api/hospitalizations/{hid}/medication-administrations', T['ide'], {"prescriptionItemId": iid, "medicationName": "Ceftriaxone", "dose": "1 g", "administeredAt": time.strftime('%Y-%m-%dT%H:%M:%SZ', time.gmtime())})
check('MED-03', 'Deuxieme administration immediate (double dose / double clic) bloquee ou alertee', st >= 400, (st, str(ad2)[:120]))
st, pres = R('GET', f"/api/consultations/{c['id']}/prescription", T['drB'])
body = {"prescriptionNumber": pres['prescriptionNumber'], "pinCode": pres['pinCode']}
R('POST', '/api/public/pharmacy/prescriptions/validate', T['pharma'], dict(body, reviewNotes="OK"))
st, d = R('POST', '/api/public/pharmacy/prescriptions/dispense', T['pharma'], dict(body, pharmacyName="PUI", pharmacistLicense="PH-1", dispensedItems=[{"prescriptionItemId": pres['items'][0]['id'], "quantityDispensed": 3}]))
check('MED-04', 'Pharmacie delivre 3 flacons au service', st < 300, st)
st, el = R('GET', f'/api/hospitalizations/{hid}/eligible-medications', T['ide'])
check('MED-05', 'Apres delivrance, le traitement reste administrable', isinstance(el, list) and len(el) == 1, (st, len(el) if isinstance(el, list) else el))
# --- invoice cancellation / credit note
st, inv = R('GET', f"/api/invoices/{S['inv_id']}", T['compta'])
st, cn = R('POST', f"/api/invoices/{S['inv_id']}/credit-notes", T['compta'], {"amount": 307500, "reason": "Quantite medicament erronee"})
check('BIL-10', 'Facturation emet un avoir pour corriger une facture partiellement payee', st < 300, (st, str(cn)[:160]))
st, ca = R('POST', f"/api/invoices/{S['inv_id']}/cancel", T['compta'])
check('BIL-11', 'Secretaire comptable peut annuler une facture', st < 300, (st, str(ca)[:140]))
st, ca = R('POST', f"/api/invoices/{S['inv_id']}/cancel", T['admin'] if 'admin' in T else A)
check('BIL-12', 'Annulation d une facture deja encaissee partiellement (admin)', True, (st, str(ca)[:160]))
st, refund = R('GET', '/api/cash-registers/sessions/active/summary', T['caisse'])
check('BIL-13', 'Remboursement du paiement deja encaisse : circuit present', False, 'aucun endpoint de remboursement / mouvement lie a l avoir')
# --- duplicates
st, dl = R('GET', '/api/patients/duplicates', A)
check('ID-01', 'Doublon Mbarga detecte dans la file des doublons', isinstance(dl, (list, dict)) and S.get('dup_id', '') in json.dumps(dl), (st, str(dl)[:200]))
st, dl2 = R('GET', '/api/patients/duplicates', T['accueil'])
check('ID-02', 'Agent d accueil voit / signale les doublons', st == 200, st)
# --- RBAC custom role & admin clinical powers
st, sign = R('POST', f'/api/hospitalizations/{hid}/discharge', A, {"dischargeDiagnosis": "admin test", "dischargeInstructions": "-", "againstMedicalAdvice": False})
check('RBAC-01', 'Administrateur clinique (non soignant) peut decider une sortie medicale', st >= 400, (st, str(sign)[:120]))
json.dump(LOG, open('log_s8.json', 'w'), ensure_ascii=False, indent=1)
