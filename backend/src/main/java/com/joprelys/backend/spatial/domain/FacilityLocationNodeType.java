package com.joprelys.backend.spatial.domain;

public enum FacilityLocationNodeType {
    SITE(10),
    BUILDING(20),
    FLOOR(30),
    ZONE(40);

    private final int rank;

    FacilityLocationNodeType(int rank) {
        this.rank = rank;
    }

    public int rank() {
        return rank;
    }

    public boolean canContain(FacilityLocationNodeType child) {
        return child != null && this.rank < child.rank;
    }
}
