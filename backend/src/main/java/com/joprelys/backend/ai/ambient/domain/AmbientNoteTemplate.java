package com.joprelys.backend.ai.ambient.domain;

import java.util.List;
import java.util.Locale;
import java.util.Set;

public enum AmbientNoteTemplate {
    SOAP(
            List.of("SUBJECTIVE", "OBJECTIVE", "ASSESSMENT", "PLAN"),
            Set.of("OBJECTIVE", "ASSESSMENT", "PLAN")),
    APSO(
            List.of("ASSESSMENT", "PLAN", "SUBJECTIVE", "OBJECTIVE"),
            Set.of("OBJECTIVE", "ASSESSMENT", "PLAN")),
    MULTI_SECTION(
            List.of(
                    "CHIEF_COMPLAINT",
                    "HISTORY",
                    "EXAM",
                    "VITALS",
                    "ASSESSMENT",
                    "PLAN",
                    "MEDICATIONS",
                    "ORDERS",
                    "FOLLOW_UP"),
            Set.of("EXAM", "VITALS", "ASSESSMENT", "PLAN", "MEDICATIONS", "ORDERS"));

    private final List<String> sections;
    private final Set<String> criticalSections;

    AmbientNoteTemplate(List<String> sections, Set<String> criticalSections) {
        this.sections = sections;
        this.criticalSections = criticalSections;
    }

    public List<String> sections() {
        return sections;
    }

    public boolean isAllowedSection(String section) {
        return section != null && sections.contains(section.trim().toUpperCase(Locale.ROOT));
    }

    public boolean isCriticalSection(String section) {
        return section != null && criticalSections.contains(section.trim().toUpperCase(Locale.ROOT));
    }

    public static AmbientNoteTemplate parse(String value) {
        if (value == null || value.isBlank()) return SOAP;
        return valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
