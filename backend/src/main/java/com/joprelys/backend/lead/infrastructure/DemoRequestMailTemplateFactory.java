package com.joprelys.backend.lead.infrastructure;

import com.joprelys.backend.lead.dto.DemoRequestDto;
import com.joprelys.backend.lead.service.DemoRequestRegistered;
import com.joprelys.backend.notification.infrastructure.mail.BrandedMailContent;
import com.joprelys.backend.notification.infrastructure.mail.BrandedMailTemplateFactory;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

@Component
public class DemoRequestMailTemplateFactory {
    private record Field(String label, String value) {}
    private final BrandedMailTemplateFactory brandedTemplate;

    public DemoRequestMailTemplateFactory(BrandedMailTemplateFactory brandedTemplate) {
        this.brandedTemplate = brandedTemplate;
    }

    public BrandedMailContent create(DemoRequestRegistered request) {
        boolean english = "en".equalsIgnoreCase(request.details().locale());
        String subject = english ? "Joprelys Connect — New demo request" : "Joprelys Connect — Nouvelle demande de démonstration";
        String title = english ? "New demo request" : "Nouvelle demande de démonstration";
        String introduction = english ? "A healthcare facility would like to discover Joprelys Connect."
                : "Un établissement souhaite découvrir Joprelys Connect.";
        List<Field> fields = fields(request, english);
        String plainText = (english ? "Hello Joprelys team," : "Bonjour l’équipe Joprelys,") + "\n\n"
                + introduction + "\n\n" + fields.stream().map(f -> f.label() + " : " + optional(f.value()))
                .collect(Collectors.joining("\n")) + "\n\nJoprelys Connect";
        String body = "<p style=\"margin:0 0 24px;color:#40556f;font-size:16px;line-height:25px;\">"
                + escape(introduction) + "</p><table width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" style=\"border-collapse:collapse;\">"
                + fields.stream().map(this::row).collect(Collectors.joining()) + "</table>";
        String html = brandedTemplate.render(new BrandedMailTemplateFactory.Header(english ? "en" : "fr",
                subject, english ? "Demonstration" : "Démonstration", title, introduction), body);
        return new BrandedMailContent(subject, plainText, html);
    }

    private List<Field> fields(DemoRequestRegistered request, boolean english) {
        DemoRequestDto lead = request.details();
        return List.of(new Field(english ? "Reference" : "Référence", request.id().toString()),
                new Field(english ? "Name" : "Nom", lead.fullName()),
                new Field(english ? "Facility" : "Établissement", lead.organizationName()),
                new Field(english ? "Role" : "Fonction", role(lead.role(), english)),
                new Field(english ? "Phone" : "Téléphone", lead.phone()), new Field("Email", lead.email()),
                new Field(english ? "City" : "Ville", lead.city()),
                new Field(english ? "Language" : "Langue", english ? "English" : "Français"),
                new Field("Message", lead.message()));
    }

    private String row(Field field) {
        return "<tr><td style=\"padding:12px 0;border-bottom:1px solid #d8e5e8;\">"
                + "<p style=\"margin:0 0 4px;color:#687b92;font-size:12px;line-height:18px;font-weight:700;\">"
                + escape(field.label()) + "</p><p style=\"margin:0;color:#0a1d3d;font-size:15px;line-height:24px;word-break:break-word;overflow-wrap:anywhere;\">"
                + escape(optional(field.value())).replace("\r\n", "\n").replace("\r", "\n").replace("\n", "<br>")
                + "</p></td></tr>";
    }

    private String role(String value, boolean english) {
        if (value == null || value.isBlank()) return "—";
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "directeur" -> english ? "Director / CFO" : "Directeur / DAF";
            case "medecin" -> english ? "Practicing physician" : "Médecin praticien";
            case "major" -> english ? "Head nurse / Supervisor" : "Major / Cadre infirmier";
            case "pharmacien" -> english ? "Hospital pharmacist" : "Pharmacien hospitalier";
            case "autre" -> english ? "Other healthcare professional" : "Autre professionnel";
            default -> value;
        };
    }

    private String optional(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    private String escape(String value) {
        return HtmlUtils.htmlEscape(value);
    }
}
