package HospitalManagementSystem;

import Appointments.Appointment;
import Appointments.AppointmentManager;
import Clinics.Clinic;
import Clinics.ClinicsManager;
import Clinics.Department;
import History.History;
import History.HistoryOfPatient;
import Persons.Doctor;
import Persons.Patient;
import Repository.DoctorRepository;
import SystemInitializer.FileInputTaker;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Console-based Hospital Management System (DATE-based).
 * Appointment booking is done using LocalDate + LocalTime.
 */
public class HospitalManagementSystem {

    /** Helper type: a single appointment option (date + start time). */
    private static class SlotOption {
        LocalDate date;
        LocalTime time;

        SlotOption(LocalDate date, LocalTime time) {
            this.date = date;
            this.time = time;
        }
    }

    private static final Scanner scanner = new Scanner(System.in);

    private static final List<Doctor> doctors;
    private static final List<Patient> patients = new ArrayList<>();
    private static final AppointmentManager appointmentManager = new AppointmentManager();
    private static final ClinicsManager clinicsManager;
    private static final HistoryOfPatient historyOfPatient;
    private static final DoctorRepository doctorRepo = new DoctorRepository();

    private static int nextPatientInternalId = 1;

    static {
        doctors = FileInputTaker.readDoctorsFromFile("doctors.txt");

        for (Doctor d : doctors) {
            appointmentManager.registerDoctor(d);
            doctorRepo.add(d);
        }

        historyOfPatient = new HistoryOfPatient();
        loadHistoriesFromDiagnosesFile();

        clinicsManager = new ClinicsManager(doctors);
    }

    public static void main(String[] args) {
        mainMenu();
    }

    // -------------------------------------------------------------------------
    //  PATIENT CREATION AND LOOKUP
    // -------------------------------------------------------------------------

    private static Patient createPatient(String nationalId, String name, String surname, String phone) {
        Patient p = new Patient(nextPatientInternalId++, nationalId, name, surname, phone);
        patients.add(p);
        return p;
    }

    private static Patient findOrCreatePatientByNameAndTc(String fullName, String nationalId, String phone) {
        String targetName = fullName.trim().toLowerCase(Locale.ENGLISH);

        for (Patient p : patients) {
            String pFull = (p.getName() + " " + p.getSurname()).trim().toLowerCase(Locale.ENGLISH);

            if (pFull.equals(targetName) && p.getNationalId().equals(nationalId)) {
                if ((p.getPhone() == null || p.getPhone().equalsIgnoreCase("N/A"))
                        && phone != null && !phone.isBlank()
                        && !phone.equalsIgnoreCase("N/A")) {
                    p.setPhone(phone.trim());
                }
                return p;
            }
        }

        String[] tokens = fullName.trim().split("\\s+");
        String name = tokens.length > 0 ? tokens[0] : fullName.trim();
        String surname = tokens.length > 1 ? tokens[tokens.length - 1] : "";

        String finalPhone = (phone == null || phone.isBlank()) ? "N/A" : phone.trim();
        return createPatient(nationalId, name, surname, finalPhone);
    }

    // -------------------------------------------------------------------------
    //  INITIALIZATION FROM registeredPatients.txt (diagnosis history)
    // -------------------------------------------------------------------------

    private static void loadHistoriesFromDiagnosesFile() {
        List<String[]> rows = FileInputTaker.readDiagnosesRaw("registeredPatients.txt");
        if (rows.isEmpty()) return;

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        for (String[] parts : rows) {
            try {
                String dateTimeStr = parts[0];
                String doctorName  = parts[1];
                String patientName = parts[2];
                String nationalId  = parts[3];
                String phone       = parts[4];
                String diagnosis   = parts[5];

                LocalDateTime dateTime = LocalDateTime.parse(dateTimeStr, fmt);

                Doctor doctor = doctors.stream()
                        .filter(d -> d.getName().equalsIgnoreCase(doctorName))
                        .findFirst()
                        .orElse(null);

                if (doctor == null) continue;

                Patient patient = findOrCreatePatientByNameAndTc(patientName, nationalId, phone);

                History history = new History(dateTime, doctor, patient, diagnosis);
                historyOfPatient.addHistory(history);

            } catch (Exception e) {
                System.out.println("A diagnosis line could not be parsed: " + Arrays.toString(parts));
            }
        }

        System.out.println("Initial patient histories loaded from registeredPatients.txt");
    }

    // -------------------------------------------------------------------------
    //  MAIN MENU
    // -------------------------------------------------------------------------

    private static void mainMenu() {
        while (true) {
            System.out.println();
            System.out.println("=== Hospital Management System ===");
            System.out.println("1) Book appointment (by clinic)");
            System.out.println("2) Cancel appointment");
            System.out.println("3) View doctor appointment queue");
            System.out.println("4) View patient history");
            System.out.println("5) Register new patient");
            System.out.println("0) Exit");

            int choice = readInt("Selection: ");

            switch (choice) {
                case 1 -> clinicAndAppointmentMenu();
                case 2 -> cancelAppointmentMenu();
                case 3 -> doctorAppointmentsMenu();
                case 4 -> patientHistoryMenu();
                case 5 -> addNewPatientMenu();
                case 0 -> {
                    System.out.println("System is shutting down.");
                    return;
                }
                default -> System.out.println("Invalid selection. Please try again.");
            }
        }
    }

    // -------------------------------------------------------------------------
    //  1) CLINIC → DOCTOR → PATIENT → APPOINTMENT
    // -------------------------------------------------------------------------

    private static void clinicAndAppointmentMenu() {
        System.out.println();
        System.out.println("--- Clinic Selection ---");

        Department[] departments = Department.values();
        for (int i = 0; i < departments.length; i++) {
            System.out.println((i + 1) + ") " + departments[i].getDisplayName());
        }

        int depChoice = readInt("Clinic (number): ") - 1;
        if (depChoice < 0 || depChoice >= departments.length) {
            System.out.println("Invalid clinic selection.");
            return;
        }
        Department selectedDep = departments[depChoice];

        Clinic clinic = clinicsManager.getClinic(selectedDep);
        List<Doctor> clinicDoctors = clinic.getDoctors();
        if (clinicDoctors.isEmpty()) {
            System.out.println("There are no doctors in this clinic.");
            return;
        }

        System.out.println();
        System.out.println("Doctors in " + selectedDep.getDisplayName() + " clinic:");
        for (Doctor d : clinicDoctors) {
            System.out.println(d.getId() + ") " + d.getInfo());
        }

        int docId = readInt("Doctor ID: ");
        Doctor selectedDoctor = doctorRepo.findById(docId);
        if (selectedDoctor == null) {
            System.out.println("Invalid doctor selection.");
            return;
        }

        boolean inThisClinic = clinicDoctors.stream().anyMatch(d -> d.getId() == docId);
        if (!inThisClinic) {
            System.out.println("The selected doctor does not belong to this clinic.");
            return;
        }

        Patient selectedPatient = choosePatientForAppointment();
        if (selectedPatient == null) {
            System.out.println("No patient selected.");
            return;
        }

        selectDateAndBook(selectedDoctor, selectedPatient);
    }

    private static Patient choosePatientForAppointment() {
        System.out.println();
        System.out.println("--- Patient Selection ---");

        if (patients.isEmpty()) {
            System.out.println("No registered patients. Please register a patient.");
            return addNewPatientMenu();
        }

        for (int i = 0; i < patients.size(); i++) {
            Patient p = patients.get(i);
            System.out.printf("%d) %s%n", i + 1, p.getInfo());
        }
        System.out.println("0) Register new patient");

        int choice = readInt("Selection: ");
        if (choice == 0) return addNewPatientMenu();
        if (choice < 1 || choice > patients.size()) {
            System.out.println("Invalid selection.");
            return null;
        }
        return patients.get(choice - 1);
    }

    // -------------------------------------------------------------------------
    //  DATE-BASED BOOKING
    // -------------------------------------------------------------------------

    private static void selectDateAndBook(Doctor doctor, Patient patient) {
        System.out.println();
        System.out.println("--- Date Selection ---");
        System.out.println("Enter appointment date (dd.MM.yyyy) (weekdays only). Example: 18.12.2025");
        System.out.println("0) Cancel");

        LocalDate date = readDateOrCancel("Date: ");
        if (date == null) {
            System.out.println("Operation cancelled.");
            return;
        }

        DayOfWeek dow = date.getDayOfWeek();
        if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
            System.out.println("Weekend is not allowed. Please choose a weekday.");
            return;
        }

        showSlotsForDateAndBook(doctor, patient, date);
    }

    private static void showSlotsForDateAndBook(Doctor doctor, Patient patient, LocalDate date) {
        final LocalTime startTime = LocalTime.of(9, 0);
        final LocalTime endTime   = LocalTime.of(17, 0);
        final int stepMinutes     = 30;

        LocalTime lunchStart = LocalTime.of(12, 0);
        LocalTime lunchEnd   = LocalTime.of(13, 0);

        System.out.println();
        System.out.println(date + " (" + date.getDayOfWeek() + ") – Available time slots:");

        List<SlotOption> options = new ArrayList<>();
        int index = 1;

        LocalTime t = startTime;
        while (t.isBefore(endTime)) {

            if (!t.isBefore(lunchStart) && t.isBefore(lunchEnd)) {
                System.out.printf("    %s - %s  (LUNCH BREAK)%n", t, t.plusMinutes(stepMinutes));
                t = t.plusMinutes(stepMinutes);
                continue;
            }

            Appointment existing = appointmentManager.findAppointment(doctor, date, t);
            if (existing != null) {
                System.out.printf("    %s - %s  (BOOKED – %s)%n",
                        t, t.plusMinutes(stepMinutes), existing.getPatient().getName());
            } else {
                System.out.printf(" %2d) %s - %s  (AVAILABLE)%n",
                        index, t, t.plusMinutes(stepMinutes));
                options.add(new SlotOption(date, t));
                index++;
            }

            t = t.plusMinutes(stepMinutes);
        }

        if (options.isEmpty()) {
            System.out.println("No available slots for this date.");
            return;
        }

        int choice = readInt("Select time slot (number, 0 = cancel): ");
        if (choice == 0) {
            System.out.println("Operation cancelled.");
            return;
        }
        if (choice < 1 || choice > options.size()) {
            System.out.println("Invalid selection.");
            return;
        }

        SlotOption selected = options.get(choice - 1);

        try {
            Appointment app = appointmentManager.bookAppointment(
                    doctor,
                    patient,
                    selected.date,
                    selected.time
            );

            patient.addToHistory(app);
            System.out.println("✅ Appointment created: " + app);

        } catch (Exception ex) {
            System.out.println("❌ Appointment could not be created: " + ex.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    //  2) CANCEL APPOINTMENT (PATIENT-BASED)
    // -------------------------------------------------------------------------

    private static void cancelAppointmentMenu() {
        System.out.println();
        System.out.println("--- Cancel Appointment ---");

        if (patients.isEmpty()) {
            System.out.println("There are no registered patients.");
            return;
        }

        for (int i = 0; i < patients.size(); i++) {
            Patient p = patients.get(i);
            System.out.printf("%d) %s%n", i + 1, p.getInfo());
        }

        int choice = readInt("Select patient: ");
        if (choice < 1 || choice > patients.size()) {
            System.out.println("Invalid selection.");
            return;
        }

        Patient selectedPatient = patients.get(choice - 1);

        List<Appointment> patientAppointments =
                appointmentManager.getActiveAppointmentsForPatient(selectedPatient);

        if (patientAppointments.isEmpty()) {
            System.out.println("This patient has no active appointments.");
            return;
        }

        System.out.println();
        System.out.println("Active appointments for this patient:");
        for (int i = 0; i < patientAppointments.size(); i++) {
            Appointment a = patientAppointments.get(i);
            System.out.printf("%d) %s%n", i + 1, a);
        }

        int appChoice = readInt("Appointment to cancel (0 = cancel): ");
        if (appChoice == 0) {
            System.out.println("Operation cancelled.");
            return;
        }
        if (appChoice < 1 || appChoice > patientAppointments.size()) {
            System.out.println("Invalid selection.");
            return;
        }

        Appointment toCancel = patientAppointments.get(appChoice - 1);

        if (appointmentManager.cancelAppointment(toCancel.getId())) {
            System.out.println("✅ Appointment cancelled: " + toCancel);
        } else {
            System.out.println("❌ The appointment could not be cancelled.");
        }
    }

    // -------------------------------------------------------------------------
    //  3) DOCTOR APPOINTMENT QUEUE
    // -------------------------------------------------------------------------

    private static void doctorAppointmentsMenu() {
        System.out.println();
        System.out.println("--- Doctor Appointment Queue ---");

        for (Doctor d : doctors) {
            System.out.println(d.getId() + ") " + d.getInfo());
        }

        int docId = readInt("Doctor ID: ");
        Doctor doctor = doctorRepo.findById(docId);

        if (doctor == null) {
            System.out.println("Invalid doctor selection.");
            return;
        }

        List<Appointment> list = appointmentManager.getAppointmentsForDoctor(doctor);
        if (list.isEmpty()) {
            System.out.println("This doctor has no active appointments.");
            return;
        }

        System.out.println("Appointments (FIFO order):");
        list.forEach(System.out::println);
    }

    // -------------------------------------------------------------------------
    //  4) PATIENT HISTORY (DIAGNOSES + APPOINTMENTS)
    // -------------------------------------------------------------------------

    private static void patientHistoryMenu() {
        System.out.println();
        System.out.println("--- Patient History ---");

        if (patients.isEmpty()) {
            System.out.println("There are no registered patients.");
            return;
        }

        for (int i = 0; i < patients.size(); i++) {
            Patient p = patients.get(i);
            System.out.printf("%d) %s%n", i + 1, p.getInfo());
        }

        int choice = readInt("Select patient: ");
        if (choice < 1 || choice > patients.size()) {
            System.out.println("Invalid selection.");
            return;
        }

        Patient patient = patients.get(choice - 1);

        List<History> diagHistory = historyOfPatient.getHistory(patient);
        if (!diagHistory.isEmpty()) {
            System.out.println();
            System.out.println("Diagnosis history:");
            for (History h : diagHistory) {
                System.out.println(h);
            }
        } else {
            System.out.println();
            System.out.println("No diagnosis history.");
        }

        List<Appointment> appHistory = patient.getHistory();
        if (!appHistory.isEmpty()) {
            System.out.println();
            System.out.println("Appointment history:");
            for (Appointment a : appHistory) {
                System.out.println(a);
            }
        } else {
            System.out.println("No appointment history.");
        }
    }

    // -------------------------------------------------------------------------
    //  5) NEW PATIENT REGISTRATION
    // -------------------------------------------------------------------------

    private static Patient addNewPatientMenu() {
        System.out.println();
        System.out.println("--- New Patient Registration ---");

        System.out.print("National ID (11 digits): ");
        String nationalId = readNationalId();

        System.out.print("First name: ");
        String name = scanner.nextLine().trim();

        System.out.print("Last name: ");
        String surname = scanner.nextLine().trim();

        System.out.print("Phone: ");
        String phone = scanner.nextLine().trim();

        if (name.isEmpty() || surname.isEmpty()) {
            System.out.println("First name and last name must not be empty.");
            return null;
        }

        Patient p = createPatient(nationalId, name, surname, phone);
        System.out.println("Patient registered: " + p.getInfo());
        return p;
    }

    // -------------------------------------------------------------------------
    //  INPUT HELPERS
    // -------------------------------------------------------------------------

    private static int readInt(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                String line = scanner.nextLine();
                return Integer.parseInt(line.trim());
            } catch (NumberFormatException ex) {
                System.out.println("Please enter a valid integer value.");
            }
        }
    }

    private static String readNationalId() {
        while (true) {
            String id = scanner.nextLine().trim();
            if (id.matches("\\d{11}")) return id;
            System.out.print("Invalid ID. National ID must contain 11 digits: ");
        }
    }

    private static LocalDate readDateOrCancel(String prompt) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        while (true) {
            System.out.print(prompt);
            String s = scanner.nextLine().trim();
            if (s.equals("0")) return null;

            try {
                return LocalDate.parse(s, fmt);
            } catch (DateTimeParseException ex) {
                System.out.println("Invalid date format. Use dd.MM.yyyy (example 18.12.2025) or 0 to cancel.");
            }
        }
    }
}
