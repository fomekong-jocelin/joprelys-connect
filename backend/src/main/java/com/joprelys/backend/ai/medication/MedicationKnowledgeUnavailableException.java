package com.joprelys.backend.ai.medication;

public class MedicationKnowledgeUnavailableException extends RuntimeException {

    public MedicationKnowledgeUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
