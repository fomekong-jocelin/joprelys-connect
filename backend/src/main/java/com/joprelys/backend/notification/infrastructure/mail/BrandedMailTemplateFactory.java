package com.joprelys.backend.notification.infrastructure.mail;

import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

@Component
public class BrandedMailTemplateFactory {
    public record Header(String locale, String subject, String category, String title, String introduction) {}

    public String render(Header header, String bodyHtml) {
        boolean english = "en".equalsIgnoreCase(header.locale());
        return HTML_TEMPLATE
                .replace("{{LANGUAGE}}", english ? "en" : "fr")
                .replace("{{SUBJECT}}", HtmlUtils.htmlEscape(header.subject()))
                .replace("{{CATEGORY}}", HtmlUtils.htmlEscape(header.category()))
                .replace("{{TITLE}}", HtmlUtils.htmlEscape(header.title()))
                .replace("{{INTRODUCTION}}", HtmlUtils.htmlEscape(header.introduction()))
                .replace("{{TAGLINE}}", english ? "Connected healthcare you can trust." : "La santé connectée, en toute confiance.")
                .replace("{{AUTOMATIC_NOTICE}}", english ? "Automatic message — please do not reply." : "Message automatique — merci de ne pas répondre.")
                .replace("{{BODY}}", bodyHtml);
    }

    private static final String HTML_TEMPLATE = """
            <!doctype html>
            <html lang="{{LANGUAGE}}">
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
                              <p style="margin:0 0 12px;color:#0b91b2;font-size:12px;line-height:18px;font-weight:700;letter-spacing:.08em;text-transform:uppercase;">{{CATEGORY}}</p>
                              <h1 style="margin:0 0 20px;color:#0a1d3d;font-size:26px;line-height:34px;font-weight:700;">{{TITLE}}</h1>
                              {{BODY}}
                            </div>
                          </td>
                        </tr>
                        <tr>
                          <td align="center" style="padding:22px 16px 0;color:#687b92;font-size:12px;line-height:19px;">
                            <p style="margin:0 0 4px;font-weight:700;color:#40556f;">Joprelys Connect</p>
                            <p style="margin:0;">{{TAGLINE}}</p>
                            <p style="margin:12px 0 0;">{{AUTOMATIC_NOTICE}}</p>
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
