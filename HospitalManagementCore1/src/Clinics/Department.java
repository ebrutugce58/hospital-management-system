package Clinics;

public enum Department {
    ORTHOPEDICS("Orthopedics"),
    NEUROLOGY("Neurology"),
    DERMATOLOGY("Dermatology"),
    UROLOGY("Urology"),
    PEDIATRICS("Pediatrics"),
    OPHTHALMOLOGY("Ophthalmology"),
    CARDIOLOGY("Cardiology"),
    OBSTETRICS_GYNECOLOGY("Obstetrics & Gynecology"),
    RADIOLOGY("Radiology"),
    GASTROENTEROLOGY("Gastroenterology"),
    PSYCHIATRY("Psychiatry");

    private final String displayName;

    Department(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
