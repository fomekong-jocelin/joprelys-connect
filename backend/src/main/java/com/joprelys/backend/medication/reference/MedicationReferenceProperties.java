package com.joprelys.backend.medication.reference;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "joprelys.medication-reference")
public class MedicationReferenceProperties {

    private boolean enabled;
    private int cacheTtlMinutes = 1440;
    private int maxCandidates = 5;
    private final RxNorm rxnorm = new RxNorm();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getCacheTtlMinutes() {
        return cacheTtlMinutes;
    }

    public void setCacheTtlMinutes(int cacheTtlMinutes) {
        this.cacheTtlMinutes = cacheTtlMinutes;
    }

    public int getMaxCandidates() {
        return maxCandidates;
    }

    public void setMaxCandidates(int maxCandidates) {
        this.maxCandidates = maxCandidates;
    }

    public RxNorm getRxnorm() {
        return rxnorm;
    }

    public static class RxNorm {

        private boolean enabled;
        private String baseUrl = "https://rxnav.nlm.nih.gov";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }
    }
}
