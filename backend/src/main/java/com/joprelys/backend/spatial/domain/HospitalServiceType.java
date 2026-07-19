package com.joprelys.backend.spatial.domain;

public enum HospitalServiceType {
    HOSPITALIZATION(true),
    EMERGENCY(true),
    OUTPATIENT(false),
    MEDICO_TECHNICAL(false),
    PHARMACY(false),
    ADMINISTRATIVE(false);

    private final boolean roomsAllowed;

    HospitalServiceType(boolean roomsAllowed) {
        this.roomsAllowed = roomsAllowed;
    }

    public boolean allowsRooms() {
        return roomsAllowed;
    }
}
