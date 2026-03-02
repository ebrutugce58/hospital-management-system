package History;

import Persons.Doctor;
import Persons.Patient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class History {

    private final LocalDateTime dateTime;
    private final Doctor doctor;
    private final Patient patient;
    private final String diagnosis;

    private static final DateTimeFormatter DISPLAY_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public History(LocalDateTime dateTime, Doctor doctor, Patient patient, String diagnosis) {
        this.dateTime = dateTime;
        this.doctor = doctor;
        this.patient = patient;
        this.diagnosis = diagnosis;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public Doctor getDoctor() {
        return doctor;
    }

    public Patient getPatient() {
        return patient;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    @Override
    public String toString() {
        return "[" + dateTime.format(DISPLAY_FMT) + "] "
                + doctor.getInfo()
                + " -> "
                + patient.getName()
                + " : "
                + diagnosis;
    }
}
