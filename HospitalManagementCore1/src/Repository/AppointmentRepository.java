package Repository;

import Appointments.Appointment;
import Appointments.AppointmentStatus;
import Persons.Doctor;
import Persons.Patient;
import ui.AppPaths;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

public class AppointmentRepository {

    private static final Path FILE = AppPaths.APPOINTMENTS_FILE;

    public void save(Appointment a) {
        try {
            if (!Files.exists(AppPaths.DATA_DIR))
                Files.createDirectories(AppPaths.DATA_DIR);

            String line =
                    a.getId() + ";" +
                    a.getPatient().getNationalId() + ";" +
                    a.getDoctor().getId() + ";" +
                    a.getDate() + ";" +
                    a.getTime() + ";" +
                    a.getStatus().name();

            Files.write(FILE, List.of(line), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);

        } catch (Exception e) {
            throw new RuntimeException("Could not save appointment: " + e.getMessage(), e);
        }
    }

    // ✅ BU METOT BİREBİR BU ŞEKİLDE OLMALI
    public List<Appointment> loadAll(Map<String, Patient> patientsByTc,
                                    Map<String, Doctor> doctorsById) {

        List<Appointment> list = new ArrayList<>();
        if (!Files.exists(FILE)) return list;

        try {
            List<String> lines = Files.readAllLines(FILE, StandardCharsets.UTF_8);

            for (String line : lines) {
                if (line == null) continue;
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                String[] p = line.split(";", -1);
                if (p.length < 6) continue;

                int id = Integer.parseInt(p[0].trim());
                String tc = p[1].trim();
                String docId = p[2].trim();

                LocalDate date = LocalDate.parse(p[3].trim());
                LocalTime time = LocalTime.parse(p[4].trim());
                AppointmentStatus status = AppointmentStatus.valueOf(p[5].trim());

                Patient patient = patientsByTc.get(tc);
                Doctor doctor = doctorsById.get(docId);

                if (patient == null || doctor == null) continue;

                Appointment a = new Appointment(id, doctor, patient, date, time);
                a.setStatus(status);

                list.add(a);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }
}