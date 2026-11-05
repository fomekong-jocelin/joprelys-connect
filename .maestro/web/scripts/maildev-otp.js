// Lit le dernier code OTP envoyé à EMAIL dans MailDev local (http://localhost:1080). Recette locale uniquement.
const response = http.get('http://localhost:1080/api/email');
const mails = json(response.body).filter((mail) => (mail.to || []).some((to) => to.address === EMAIL));
const last = mails[mails.length - 1];
const match = last ? String(last.text || last.html || '').match(/\b(\d{6})\b/) : null;
output.otp = match ? match[1] : '';
