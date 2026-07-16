package com.joprelys.backend.notification.infrastructure.mail;

import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

@Component
class AccountMailTemplateFactory {

    MailContent temporaryPassword(String displayName, String temporaryPassword) {
        return create(
                "Votre compte Joprelys Connect",
                "Votre compte est prêt",
                "Un compte Joprelys Connect vient d’être créé pour vous.",
                "Mot de passe temporaire",
                temporaryPassword,
                "Utilisez ce mot de passe pour votre première connexion, puis remplacez-le immédiatement.",
                "Si vous n’attendiez pas cette création de compte, contactez votre administrateur.",
                displayName);
    }

    MailContent loginCode(String displayName, String code) {
        return securityCode(
                "Code de connexion Joprelys",
                "Confirmez votre connexion",
                "Une tentative de connexion sécurisée nécessite votre confirmation.",
                code,
                displayName);
    }

    MailContent passwordRecoveryCode(String displayName, String code) {
        return securityCode(
                "Réinitialisation de votre mot de passe Joprelys",
                "Réinitialisez votre mot de passe",
                "Utilisez ce code pour poursuivre la réinitialisation de votre mot de passe.",
                code,
                displayName);
    }

    MailContent patientLoginCode(String displayName, String code) {
        return securityCode(
                "Code d’accès à votre espace patient Joprelys",
                "Accédez à votre espace patient",
                "Utilisez ce code pour confirmer l’accès sécurisé à votre espace santé.",
                code,
                displayName);
    }

    private MailContent securityCode(
            String subject,
            String title,
            String introduction,
            String code,
            String displayName) {
        return create(
                subject,
                title,
                introduction,
                "Code de sécurité",
                code,
                "Ce code expire dans 5 minutes et ne peut être utilisé qu’une seule fois.",
                "Si vous n’êtes pas à l’origine de cette demande, ignorez cet e-mail et ne communiquez ce code à personne.",
                displayName);
    }

    private MailContent create(
            String subject,
            String title,
            String introduction,
            String credentialLabel,
            String credential,
            String guidance,
            String securityNotice,
            String displayName) {
        String greeting = displayName == null || displayName.isBlank()
                ? "Bonjour,"
                : "Bonjour " + displayName.trim() + ",";
        String plainText = greeting + "\n\n"
                + introduction + "\n\n"
                + credentialLabel + " : " + credential + "\n\n"
                + guidance + "\n\n"
                + securityNotice + "\n\n"
                + "L’équipe Joprelys Connect";

        String html = HTML_TEMPLATE
                .replace("{{SUBJECT}}", escape(subject))
                .replace("{{TITLE}}", escape(title))
                .replace("{{GREETING}}", escape(greeting))
                .replace("{{INTRODUCTION}}", escape(introduction))
                .replace("{{CREDENTIAL_LABEL}}", escape(credentialLabel))
                .replace("{{CREDENTIAL}}", escape(credential))
                .replace("{{GUIDANCE}}", escape(guidance))
                .replace("{{SECURITY_NOTICE}}", escape(securityNotice));
        return new MailContent(subject, plainText, html);
    }

    private static String escape(String value) {
        return HtmlUtils.htmlEscape(value == null ? "" : value);
    }

    record MailContent(String subject, String plainText, String html) {
    }

    private static final String HTML_TEMPLATE = """
            <!doctype html>
            <html lang="fr">
              <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>{{SUBJECT}}</title>
              </head>
              <body style="margin:0;padding:0;background:#f7fafc;color:#0a1d3d;font-family:Arial,'Helvetica Neue',sans-serif;">
                <div style="display:none;max-height:0;overflow:hidden;opacity:0;color:transparent;">{{INTRODUCTION}}</div>
                <table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0" style="background:#f7fafc;">
                  <tr>
                    <td align="center" style="padding:32px 16px;">
                      <table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0" style="max-width:600px;">
                        <tr>
                          <td style="padding:0 0 20px 0;">
                            <img src="cid:joprelys-logo" width="178" alt="Joprelys Connect" style="display:block;width:178px;max-width:100%;height:auto;border:0;">
                          </td>
                        </tr>
                        <tr>
                          <td style="background:#ffffff;border:1px solid #d8e5e8;border-radius:8px;box-shadow:0 8px 20px rgba(10,29,61,.08);overflow:hidden;">
                            <div style="height:5px;background:#0b91b2;font-size:0;line-height:0;">&nbsp;</div>
                            <div style="padding:32px;">
                              <p style="margin:0 0 12px;color:#0b91b2;font-size:12px;line-height:18px;font-weight:700;letter-spacing:.08em;text-transform:uppercase;">Sécurité du compte</p>
                              <h1 style="margin:0 0 20px;color:#0a1d3d;font-size:26px;line-height:34px;font-weight:700;">{{TITLE}}</h1>
                              <p style="margin:0 0 12px;color:#40556f;font-size:16px;line-height:25px;">{{GREETING}}</p>
                              <p style="margin:0 0 24px;color:#40556f;font-size:16px;line-height:25px;">{{INTRODUCTION}}</p>
                              <table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0" style="margin:0 0 24px;background:#eef8fa;border:1px solid #b9dce4;border-radius:6px;">
                                <tr>
                                  <td align="center" style="padding:22px 16px;">
                                    <p style="margin:0 0 8px;color:#40556f;font-size:12px;line-height:18px;font-weight:700;letter-spacing:.06em;text-transform:uppercase;">{{CREDENTIAL_LABEL}}</p>
                                    <p style="margin:0;color:#0a1d3d;font-family:'Courier New',monospace;font-size:30px;line-height:38px;font-weight:700;letter-spacing:.16em;word-break:break-all;">{{CREDENTIAL}}</p>
                                  </td>
                                </tr>
                              </table>
                              <p style="margin:0 0 20px;color:#40556f;font-size:15px;line-height:24px;">{{GUIDANCE}}</p>
                              <table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0" style="background:#fff8e8;border-left:4px solid #d97706;">
                                <tr>
                                  <td style="padding:14px 16px;color:#704214;font-size:14px;line-height:22px;">{{SECURITY_NOTICE}}</td>
                                </tr>
                              </table>
                            </div>
                          </td>
                        </tr>
                        <tr>
                          <td align="center" style="padding:22px 16px 0;color:#687b92;font-size:12px;line-height:19px;">
                            <p style="margin:0 0 4px;font-weight:700;color:#40556f;">Joprelys Connect</p>
                            <p style="margin:0;">La santé connectée, en toute confiance.</p>
                            <p style="margin:12px 0 0;">Message automatique — merci de ne pas répondre.</p>
                          </td>
                        </tr>
                      </table>
                    </td>
                  </tr>
                </table>
              </body>
            </html>
            """;
}
