package com.joprelys.backend.notification.infrastructure.mail;

import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

@Component
class AccountMailTemplateFactory {
    private final BrandedMailTemplateFactory brandedTemplate;

    AccountMailTemplateFactory(BrandedMailTemplateFactory brandedTemplate) {
        this.brandedTemplate = brandedTemplate;
    }

    BrandedMailContent temporaryPassword(String displayName, String temporaryPassword) {
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

    BrandedMailContent loginCode(String displayName, String code) {
        return securityCode(
                "Code de connexion Joprelys",
                "Confirmez votre connexion",
                "Une tentative de connexion sécurisée nécessite votre confirmation.",
                code,
                displayName);
    }

    BrandedMailContent passwordRecoveryCode(String displayName, String code) {
        return securityCode(
                "Réinitialisation de votre mot de passe Joprelys",
                "Réinitialisez votre mot de passe",
                "Utilisez ce code pour poursuivre la réinitialisation de votre mot de passe.",
                code,
                displayName);
    }

    BrandedMailContent patientLoginCode(String displayName, String code) {
        return securityCode(
                "Code d’accès à votre espace patient Joprelys",
                "Accédez à votre espace patient",
                "Utilisez ce code pour confirmer l’accès sécurisé à votre espace santé.",
                code,
                displayName);
    }

    private BrandedMailContent securityCode(
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

    private BrandedMailContent create(
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

        String body = BODY_TEMPLATE
                .replace("{{GREETING}}", escape(greeting))
                .replace("{{INTRODUCTION}}", escape(introduction))
                .replace("{{CREDENTIAL_LABEL}}", escape(credentialLabel))
                .replace("{{CREDENTIAL}}", escape(credential))
                .replace("{{GUIDANCE}}", escape(guidance))
                .replace("{{SECURITY_NOTICE}}", escape(securityNotice));
        String html = brandedTemplate.render(new BrandedMailTemplateFactory.Header(
                "fr", subject, "Sécurité du compte", title, introduction), body);
        return new BrandedMailContent(subject, plainText, html);
    }

    private static String escape(String value) {
        return HtmlUtils.htmlEscape(value == null ? "" : value);
    }

    private static final String BODY_TEMPLATE = """
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
            """;
}
