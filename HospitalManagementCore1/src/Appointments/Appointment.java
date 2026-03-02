package Appointments;

import Persons.Doctor;
import Persons.Patient;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

public class Appointment {

    private final int id;
    private final Doctor doctor;
    private final Patient patient;

    // ✅ artık gerçek tarih
    private final LocalDate date;
    private final LocalTime time;

    private AppointmentStatus status;

    public Appointment(int id, Doctor doctor, Patient patient, LocalDate date, LocalTime time) {
        this.id = id;
        this.doctor = doctor;
        this.patient = patient;
        this.date = date;
        this.time = time;
        this.status = AppointmentStatus.BOOKED;
    }

    public int getId() { return id; }
    public Doctor getDoctor() { return doctor; }
    public Patient getPatient() { return patient; }

    public LocalDate getDate() { return date; }
    public LocalTime getTime() { return time; }

    // istersen lazım olur
    public DayOfWeek getDayOfWeek() { return date.getDayOfWeek(); }

    public AppointmentStatus getStatus() { return status; }
    public void setStatus(AppointmentStatus status) { this.status = status; }

    @Override
    public String toString() {
        return "#" + id + " | " + date + " " + time + " | " +
                doctor.getName() + " | " + patient.getName() + " " + patient.getSurname() +
                " (" + status + ")";
    }
}
