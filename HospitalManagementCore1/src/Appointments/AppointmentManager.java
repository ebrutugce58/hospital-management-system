package Appointments;

import Persons.Doctor;
import Persons.Patient;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import Repository.AppointmentRepository;

public class AppointmentManager {

    private final Map<Integer, List<Appointment>> doctorApps = new HashMap<>();
    private int nextId = 1;

    public void registerDoctor(Doctor d) {
        if (d == null) return;
        doctorApps.putIfAbsent(d.getId(), new ArrayList<>());
    }

    public Appointment findAppointment(Doctor doctor, LocalDate date, LocalTime time) {
        if (doctor == null || date == null || time == null) return null;
        List<Appointment> list = doctorApps.getOrDefault(doctor.getId(), List.of());
        for (Appointment a : list) {
            if (a.getStatus() == AppointmentStatus.BOOKED
                    && a.getDate().equals(date)
                    && a.getTime().equals(time)) {
                return a;
            }
        }
        return null;
    }

    public Appointment bookAppointment(Doctor doctor, Patient patient, LocalDate date, LocalTime time) {
        if (doctor == null || patient == null || date == null || time == null) {
            throw new IllegalArgumentException("Doctor/Patient/Date/Time cannot be null.");
        }

        doctorApps.putIfAbsent(doctor.getId(), new ArrayList<>());

        // dolu mu kontrol
        Appointment existing = findAppointment(doctor, date, time);
        if (existing != null) {
            throw new IllegalStateException("This slot is already booked.");
        }

        Appointment app = new Appointment(nextId++, doctor, patient, date, time);
        doctorApps.get(doctor.getId()).add(app);

        // ✅ KRİTİK: Patient History Stack'i de doldur
        patient.addToHistory(app);

        return app;
    }

    public boolean cancelAppointment(int appointmentId) {
        for (List<Appointment> list : doctorApps.values()) {
            for (Appointment a : list) {
                if (a.getId() == appointmentId && a.getStatus() == AppointmentStatus.BOOKED) {
                    a.setStatus(AppointmentStatus.CANCELLED);
                    return true;
                }
            }
        }
        return false;
    }

    public List<Appointment> getAppointmentsForDoctor(Doctor doctor) {
        if (doctor == null) return List.of();
        return new ArrayList<>(doctorApps.getOrDefault(doctor.getId(), List.of()));
    }

    public List<Appointment> getActiveAppointmentsForPatient(Patient patient) {
        if (patient == null) return List.of();

        List<Appointment> res = new ArrayList<>();

        for (List<Appointment> list : doctorApps.values()) {
            for (Appointment a : list) {
                if (a.getPatient() != null
                        && a.getPatient().getNationalId().equals(patient.getNationalId())
                        && a.getStatus() == AppointmentStatus.BOOKED) {
                    res.add(a);
                }
            }
        }
        return res;
    }

    public void loadFromFile(List<Patient> patients, List<Doctor> doctors, AppointmentRepository repo) {

        Map<String, Patient> patientsByTc = new HashMap<>();
        for (Patient p : patients) {
            patientsByTc.put(p.getNationalId(), p);
        }

        Map<String, Doctor> doctorsById = new HashMap<>();
        for (Doctor d : doctors) {
            doctorsById.put(String.valueOf(d.getId()), d);
        }

        List<Appointment> all = repo.loadAll(patientsByTc, doctorsById);

        int maxId = 0;

        for (Appointment a : all) {
            doctorApps.putIfAbsent(a.getDoctor().getId(), new ArrayList<>());
            doctorApps.get(a.getDoctor().getId()).add(a);

            // patient history dolsun
            if (a.getPatient() != null) {
                a.getPatient().addToHistory(a);
            }

            if (a.getId() > maxId) maxId = a.getId();
        }

        nextId = maxId + 1;
        System.out.println("📖 Appointments loaded = " + all.size() + " | nextId=" + nextId);
    }

    // ✅ HATALI "appointments" YERİNE doctorApps'ten toparla
    public List<Appointment> getAllAppointments() {
        List<Appointment> res = new ArrayList<>();
        for (List<Appointment> list : doctorApps.values()) {
            res.addAll(list);
        }
        return res;
    }
}
