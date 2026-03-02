package ui.services;

import Appointments.Appointment;

import Appointments.AppointmentManager;
import Appointments.AppointmentStatus;
import Clinics.Clinic;
import Clinics.ClinicsManager;
import Clinics.Department;
import History.History;
import History.HistoryOfPatient;
import Persons.Doctor;
import Persons.Patient;
import Repository.DoctorRepository;
import SystemInitializer.FileInputTaker;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import Repository.PatientRepository;
import Repository.AppointmentRepository;


public class HospitalService {
	
    // ✅ UI'da slot göstermek için helper (DatePicker uyumlu)
	public static class Slot {
	    public final LocalDate date;
	    public final LocalTime time;

	    public Slot(LocalDate date, LocalTime time) {
	        this.date = date;
	        this.time = time;
	    }

	    @Override
	    public String toString() {
	        return time.toString(); // ListView'da sadece saat görünsün
	    }
	}


    private final List<Doctor> doctors;
    private final List<Patient> patients = new ArrayList<>();

    private final AppointmentManager appointmentManager = new AppointmentManager();
    private  ClinicsManager clinicsManager;
    private final HistoryOfPatient historyOfPatient = new HistoryOfPatient();
    private final DoctorRepository doctorRepository = new DoctorRepository();
    private final PatientRepository patientRepo = new PatientRepository();
    private final AppointmentRepository appointmentRepo = new AppointmentRepository();



    private int nextPatientInternalId = 1;

    public HospitalService() {

        System.out.println("✅ HospitalService init: patients loaded = " + patients.size());
    	patients.addAll(patientRepo.loadAll());

        // 1) Load doctors
    	ui.AppPaths.ensureDataFiles();
    	doctors = FileInputTaker.readDoctorsFromFile(
    	        ui.AppPaths.DOCTORS_FILE.toAbsolutePath().toString()
    	);
    	System.out.println("📖 Doctors loaded from: " + ui.AppPaths.DOCTORS_FILE.toAbsolutePath());
    	System.out.println("📖 Doctors loaded = " + doctors.size());




        // 2) Register doctors for appointment manager + repository (BST)
        for (Doctor d : doctors) {
            appointmentManager.registerDoctor(d);
            doctorRepository.add(d);
        }

        // 3) Init clinics
        clinicsManager = new ClinicsManager(doctors);

        // 4) Load diagnosis histories from registeredPatients.txt
     // 4) Load diagnosis histories from registeredPatients.txt
        loadHistoriesFromDiagnosesFile();
        appointmentManager.loadFromFile(getAllPatients(), getAllDoctors(), appointmentRepo);

    }

    // -------------------------
    //  DOCTOR / CLINIC
    // -------------------------

    public List<Doctor> getAllDoctors() {
        return List.copyOf(doctors);
    }

    public List<Department> getDepartments() {
        return Arrays.asList(Department.values());
    }

    public List<Doctor> getDoctorsByDepartment(Department dep) {
        if (dep == null) return List.of();
        Clinic clinic = clinicsManager.getClinic(dep);
        if (clinic == null) return List.of();
        return clinic.getDoctors();
    }


    public Persons.Doctor findDoctorByDoctorId(String doctorId) {
    	// ✅ DEMO MODE: SADECE RAM'DEN BUL
        if (ui.AppConfig.DEMO_MODE) {
            int targetId = extractDoctorIntId(doctorId);
            for (Doctor d : doctors) {
                if (d != null && d.getId() == targetId) {
                    return d;
                }
            }
            return null;
        }
        if (doctorId == null) return null;

        // normalize -> int id
        int targetId = extractDoctorIntId(doctorId);
        if (targetId < 0) return null;

        // 1) Önce zaten yüklenmiş listeden bul (62 doktor burada)
        try {
            for (Persons.Doctor d : getAllDoctors()) {
                if (d == null) continue;
                int pid = getDoctorPersonId(d);
                if (pid == targetId) return d;
            }
        } catch (Exception ignore) {}

        // 2) Listede yoksa: doctors.txt içinden bul ve runtime Doctor üret
        try (java.io.BufferedReader br = openDoctorsReader()) {
            String line;
            int n = 0;

            while ((line = br.readLine()) != null) {
                n++;

                String t = (line == null) ? "" : line.replace("\uFEFF", "").trim();
                if (t.isEmpty() || t.startsWith("#")) continue;

                // senin dosya formatın: D4321;eren;acikgöz;Orthopedics
                String[] p = t.split(";", -1);
                if (p.length < 1) continue;

                String fileId = (p[0] == null) ? "" : p[0].trim();
                int fileIntId = extractDoctorIntId(fileId);

                if (fileIntId != targetId) continue;

                String name = (p.length >= 2) ? p[1].trim() : "";
                String surname = (p.length >= 3) ? p[2].trim() : "";
                String dept = (p.length >= 4) ? p[3].trim() : "";

                System.out.println("⚠️ Doctor exists in doctors.txt but not loaded list. Creating runtime doctor. line=" + n);

                Persons.Doctor created = buildDoctorFromDoctorsTxtLine(fileId, name, surname, dept);
                System.out.println("✅ buildDoctorFromDoctorsTxtLine created? " + (created != null));

                if (created != null) {
                    try { getAllDoctors().add(created); } catch (Exception ignored) {}
                }

                return created;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private String safeUpper(String s) {
        return (s == null) ? "" : s.replace("\uFEFF", "").trim().toUpperCase().replaceAll("\\s+", "");
    }
    public Doctor registerDoctorRuntime(int id, String fullName, Department dep) {

        Doctor d = new Doctor(id, fullName, dep);

        // 1) RAM listesine ekle
        doctors.add(d);

        // 2) Appointment sistemi tanısın
        appointmentManager.registerDoctor(d);

        // 3) BST repository’ye ekle
        doctorRepository.add(d);

        // 4) ClinicsManager'ı güncelle
        clinicsManager = new ClinicsManager(doctors);

        return d;
    }

    /* =======================
       Runtime Doctor creator
       ======================= */

    private Persons.Doctor buildDoctorBySetters(String id, String name, String surname, String dept) {
        try {
            Persons.Doctor d = null;

            // 1) no-arg constructor
            try {
                var c0 = Persons.Doctor.class.getDeclaredConstructor();
                c0.setAccessible(true);
                d = (Persons.Doctor) c0.newInstance();
            } catch (Exception ignored) {
                // 2) single String constructor (id)
                try {
                    var c1 = Persons.Doctor.class.getConstructor(String.class);
                    d = (Persons.Doctor) c1.newInstance(id);
                } catch (Exception ignored2) {
                    d = null;
                }
            }

            if (d == null) return null;

            // id setters
            callSetterIfExists(d, "setDoctorId", id);
            callSetterIfExists(d, "setId", id);
            callSetterIfExists(d, "setDoctorNo", id);

            // name/surname setters
            callSetterIfExists(d, "setName", name);
            callSetterIfExists(d, "setSurname", surname);
            callSetterIfExists(d, "setLastName", surname);

            // department setter (String veya enum/class olabilir)
            if (!callSetterIfExists(d, "setDepartment", dept)) {
                // Clinics.Department enum olabilir
                try {
                    Class<?> depCls = Class.forName("Clinics.Department");
                    if (depCls.isEnum()) {
                        String key = dept.trim().toUpperCase().replace(" ", "_");
                        Object depObj = java.lang.Enum.valueOf((Class<? extends Enum>) depCls, key);

                        var m = d.getClass().getMethod("setDepartment", depCls);
                        m.invoke(d, depObj);
                    }
                } catch (Exception ignored) {}
            }

            return d;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private boolean callSetterIfExists(Object obj, String method, String value) {
        try {
            var m = obj.getClass().getMethod(method, String.class);
            m.invoke(obj, value);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }


    /**
     * Doctor sınıfında id getter adı farklı olabilir diye 2-3 ihtimal deniyoruz.
     * Bu metodlar sadece "varsa" çalışır; yoksa boş döner.
     */
    private String getDoctorIdAny(Persons.Doctor d) {
        try { return (String) d.getClass().getMethod("getDoctorId").invoke(d); } catch (Exception ignored) {}
        try { return (String) d.getClass().getMethod("getId").invoke(d); } catch (Exception ignored) {}
        try { return String.valueOf(d.getClass().getMethod("getDoctorNo").invoke(d)); } catch (Exception ignored) {}
        return "";
    }

    private String getDoctorIdAltAny(Persons.Doctor d) {
        // id int ise String'e çevirme için alternatif
        try { return String.valueOf(d.getClass().getMethod("getDoctorId").invoke(d)); } catch (Exception ignored) {}
        try { return String.valueOf(d.getClass().getMethod("getId").invoke(d)); } catch (Exception ignored) {}
        return "";
    }




   

    // -------------------------
    //  PATIENT
    // -------------------------

    public List<Patient> getAllPatients() {
    	
        return List.copyOf(patients);
    }

    public Patient findPatientByNationalId(String nationalId) {
        if (nationalId == null) return null;
        for (Patient p : patients) {
            if (nationalId.equals(p.getNationalId())) return p;
        }
        return null;
    }

    public Patient registerPatient(String nationalId, String name, String surname, String phone) {

        if (nationalId == null || !nationalId.matches("\\d{11}")) {
            throw new IllegalArgumentException("National ID must be 11 digits.");
        }
        if (name == null || name.isBlank() || surname == null || surname.isBlank()) {
            throw new IllegalArgumentException("Name and surname cannot be empty.");
        }

        if (phone == null || phone.isBlank()) phone = "N/A";

        // aynı TC ile tekrar kayıt olmasın
        for (Patient p : patients) {
            if (p.getNationalId().equals(nationalId)) {
                if (p.getPhone() == null || p.getPhone().equalsIgnoreCase("N/A")) {
                    p.setPhone(phone);
                }
                return p;
            }
        }

        Patient p = new Patient(nextPatientInternalId++, nationalId, name.trim(), surname.trim(), phone.trim());
        patients.add(p);
    
        patientRepo.save(p);   // ✅ KALICI KAYIT (EN KRİTİK SATIR)

        return p;
    }

    private Patient findOrCreatePatientByNameAndTc(String fullName, String nationalId, String phone) {
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
        return registerPatient(nationalId, name, surname, finalPhone);
    }

    // -------------------------
    //  APPOINTMENT (✅ LocalDate)
    // -------------------------

    // ✅ DatePicker ile seçilen güne göre slot ver
    public List<Slot> getAvailableSlotsForDoctor(Doctor doctor, LocalDate date) {
        if (doctor == null || date == null) return List.of();

        DayOfWeek day = date.getDayOfWeek();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) return List.of();

        LocalTime start = LocalTime.of(9, 0);
        LocalTime end = LocalTime.of(17, 0);
        LocalTime lunchStart = LocalTime.of(12, 0);
        LocalTime lunchEnd = LocalTime.of(13, 0);

        List<Slot> slots = new ArrayList<>();
        for (LocalTime t = start; t.isBefore(end); t = t.plusMinutes(30)) {

            boolean lunch = (!t.isBefore(lunchStart) && t.isBefore(lunchEnd));
            if (lunch) continue;

            // AppointmentManager: (Doctor, LocalDate, LocalTime)
            Appointment existing = appointmentManager.findAppointment(doctor, date, t);
            if (existing == null) slots.add(new Slot(date, t));
        }
       
        return slots;
    }


    // ✅ Booking: (Doctor, Patient, LocalDate, LocalTime)
    public Appointment bookAppointment(Doctor doctor, Patient patient, LocalDate date, LocalTime time) {
        if (doctor == null || patient == null || date == null || time == null) {
            throw new IllegalArgumentException("Doctor / Patient / Date / Time cannot be null.");
        }
        Appointment app = appointmentManager.bookAppointment(doctor, patient, date, time);
        patient.addToHistory(app);
        appointmentRepo.save(app);
        return app;
        
    }
    

    public boolean cancelAppointment(int appointmentId) {
        return appointmentManager.cancelAppointment(appointmentId);
    }

    public List<Appointment> getAppointmentsForDoctor(Doctor doctor) {
        if (doctor == null) return List.of();
        return appointmentManager.getAppointmentsForDoctor(doctor);
    }

    public List<Appointment> getActiveAppointmentsForPatient(Patient patient) {
        if (patient == null) return List.of();
        return appointmentManager.getActiveAppointmentsForPatient(patient);
    }

    public List<Appointment> getPatientAppointmentHistoryAll(Patient patient) {
        if (patient == null) return List.of();

        String tc = patient.getNationalId();

        List<Appointment> result = new ArrayList<>();
        for (Appointment a : appointmentManager.getAllAppointments()) { // <- sende hangi liste varsa
            if (a == null) continue;

            // 1) Appointment içinden Patient nesnesi tutuluyorsa:
            if (a.getPatient() != null && tc.equals(a.getPatient().getNationalId())) {
                result.add(a);
            }

            // 2) Eğer Appointment sadece patientTc / patientNationalId string tutuyorsa:
            // if (tc.equals(a.getPatientNationalId())) result.add(a);
        }
        return result;
    }
    public List<Appointment> getPatientAppointmentHistoryBooked(Patient patient) {
        if (patient == null) return List.of();
        List<Appointment> res = new ArrayList<>();
        for (Appointment a : patient.getHistory()) {
            if (a.getStatus() == AppointmentStatus.BOOKED) res.add(a);
        }
        return res;
    }

    // -------------------------
    //  HISTORY (DIAGNOSIS)
    // -------------------------

    public List<History> getDiagnosisHistory(Patient patient) {
        if (patient == null) return List.of();
        return historyOfPatient.getHistory(patient);
    }

    public List<Patient> getPatientsForDoctor(Doctor doctor) {
        if (doctor == null) return List.of();

        int docId = doctor.getId();
        Set<Patient> set = new LinkedHashSet<>();

        for (History h : historyOfPatient.getAll()) {
            if (h == null || h.getDoctor() == null || h.getPatient() == null) continue;
            if (h.getDoctor().getId() == docId) set.add(h.getPatient());
        }

        System.out.println("✅ getPatientsForDoctor(" + doctor.getName() + ", id=" + docId + ") = " + set.size());
        return new ArrayList<>(set);
    }
    public List<Patient> getPatientsForDoctorAll(Doctor doctor) {
        if (doctor == null) return List.of();

        Set<Patient> set = new LinkedHashSet<>();

        for (History h : historyOfPatient.getAll()) {
            if (h == null || h.getDoctor() == null || h.getPatient() == null) continue;
            if (h.getDoctor().getId() == doctor.getId()) {
                set.add(h.getPatient());
            }
        }

        for (Appointment a : appointmentManager.getAppointmentsForDoctor(doctor)) {
            if (a != null && a.getPatient() != null) {
                set.add(a.getPatient());
            }
        }

        return new ArrayList<>(set);
    }
    private void loadHistoriesFromDiagnosesFile() {
        List<String[]> rows = FileInputTaker.readDiagnosesRaw("registeredPatients.txt");
        System.out.println("✅ readDiagnosesRaw rows = " + rows.size());
        if (rows.isEmpty()) return;

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        int added = 0;
        int skipped = 0;

        for (String[] parts : rows) {
            try {
                String dateTimeStr = parts[0].trim();
                String doctorName  = parts[1].trim().replaceAll("\\s+", " ");
                String patientName = parts[2].trim().replaceAll("\\s+", " ");
                String nationalId  = parts[3].trim().replaceAll("\\s+", "");
                String phone       = parts[4].trim().replaceAll("\\s+", "");
                String diagnosis   = parts[5].trim();

                if (!dateTimeStr.matches("\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}")) {
                    skipped++;
                    System.out.println("⛔ Skipped (bad datetime): " + String.join(" | ", parts));
                    continue;
                }

                LocalDateTime dateTime = LocalDateTime.parse(dateTimeStr, fmt);

                Doctor doctor = null;
                String key = doctorName.toLowerCase(Locale.ROOT);

                for (Doctor d : doctors) {
                    if (d == null) continue;
                    String dn = d.getName().trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
                    if (dn.equals(key)) { doctor = d; break; }
                }

                if (doctor == null) {
                    skipped++;
                    System.out.println("⛔ Skipped (doctor not found): " + doctorName);
                    continue;
                }

                Patient patient = findOrCreatePatientByNameAndTc(patientName, nationalId, phone);

                History history = new History(dateTime, doctor, patient, diagnosis);
                historyOfPatient.addHistory(history);
                added++;

            } catch (Exception e) {
                skipped++;
                System.out.println("⛔ Skipped (exception): " + Arrays.toString(parts));
                e.printStackTrace();
            }
        }

        System.out.println("✅ Histories added = " + added + " | skipped = " + skipped);
        System.out.println("✅ historyOfPatient total = " + historyOfPatient.getAll().size());
    }
  

    public void appendDoctorToDataFile(String doctorId, String name, String surname, String department) {
    	  if (ui.AppConfig.DEMO_MODE) {
    	        System.out.println("🧪 DEMO MODE: Doctor file write skipped.");
    	        return;
    	    }

    	try {
            java.nio.file.Path dir = java.nio.file.Paths.get("data");
            java.nio.file.Path file = dir.resolve("doctors.txt");
            if (!java.nio.file.Files.exists(dir)) java.nio.file.Files.createDirectories(dir);

            String line = doctorId + ";" + name + ";" + surname + ";" + department;

            java.nio.file.Files.writeString(
                    file,
                    line + System.lineSeparator(),
                    java.nio.charset.StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND
            );
        } catch (Exception e) {
            throw new RuntimeException("Could not save doctor: " + e.getMessage(), e);
        }
    }
    public void saveDoctorToDataFile(String doctorId, String name, String surname, String department) {
    	 if (ui.AppConfig.DEMO_MODE) {
    	        System.out.println("🧪 DEMO MODE: Doctor file write skipped.");
    	        return; // ✅ demo modda dosyaya yazma
    	    }
    	 try {
    	        Path dir = ui.AppPaths.DATA_DIR;
    	        Path file = ui.AppPaths.DOCTORS_FILE;


            if (!java.nio.file.Files.exists(dir)) java.nio.file.Files.createDirectories(dir);

            String line = doctorId + ";" + name + ";" + surname + ";" + department;

            System.out.println("🩺 Saving doctor to: " + file.toAbsolutePath());
            System.out.println("🩺 Line = " + line);

            java.nio.file.Files.writeString(
                    file,
                    line + System.lineSeparator(),
                    java.nio.charset.StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND
            );

            System.out.println("✅ Doctor saved to doctors.txt");

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Could not save doctor: " + e.getMessage(), e);
        }
    }

    private java.io.BufferedReader openDoctorsReader() throws Exception {
        java.nio.file.Path p = ui.AppPaths.DOCTORS_FILE;

        System.out.println("🧭 DOCTORS FILE PATH = " + p.toAbsolutePath());
        System.out.println("🧭 EXISTS? " + java.nio.file.Files.exists(p));

        if (java.nio.file.Files.exists(p)) {
            return java.nio.file.Files.newBufferedReader(
                    p, java.nio.charset.StandardCharsets.UTF_8
            );
        }

        // fallback: resources/doctors.txt
        var in = getClass().getResourceAsStream("/doctors.txt");
        if (in == null) throw new IllegalStateException("doctors.txt not found in resources!");
        return new java.io.BufferedReader(
                new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)
        );
    }


    private Persons.Doctor buildDoctorByReflection(String id, String name, String surname, String dept) {
        try {
            var cons = Persons.Doctor.class.getConstructors();

            // 4 String (id,name,surname,dept)
            for (var c : cons) {
                Class<?>[] t = c.getParameterTypes();
                if (t.length == 4 && allString(t)) {
                    return (Persons.Doctor) c.newInstance(id, name, surname, dept);
                }
            }

            // 3 String (id,name,surname) veya (id,name,dept)
            for (var c : cons) {
                Class<?>[] t = c.getParameterTypes();
                if (t.length == 3 && allString(t)) {
                    String third = (dept != null && !dept.isBlank()) ? dept : surname;
                    return (Persons.Doctor) c.newInstance(id, name, third);
                }
            }

            // 2 String (id,name)
            for (var c : cons) {
                Class<?>[] t = c.getParameterTypes();
                if (t.length == 2 && allString(t)) {
                    return (Persons.Doctor) c.newInstance(id, name);
                }
            }

            // 1 String (id)
            for (var c : cons) {
                Class<?>[] t = c.getParameterTypes();
                if (t.length == 1 && allString(t)) {
                    return (Persons.Doctor) c.newInstance(id);
                }
            }

            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private boolean allString(Class<?>[] types) {
        for (Class<?> c : types) {
            if (!String.class.equals(c)) return false;
        }
        return true;
    }


 // ============================
 // Doctor file -> Doctor object
 // ============================

 private int extractDoctorIntId(String anyId) {
     if (anyId == null) return -1;
     String s = anyId.replace("\uFEFF", "").trim().toUpperCase();
     s = s.replaceAll("\\s+", "");
     if (s.startsWith("D")) s = s.substring(1);

     try {
         return Integer.parseInt(s);
     } catch (Exception e) {
         return -1;
     }
 }

 private Clinics.Department parseDepartmentSafe(String text) {
     // Department enum ise en güvenlisi values() ile eşleştirmek
     Clinics.Department[] all = Clinics.Department.values();
     if (all.length == 0) return null;

     if (text == null || text.isBlank()) return all[0];

     String raw = text.trim();
     String norm = raw.toUpperCase().replace(" ", "_");

     for (Clinics.Department d : all) {
         // enum adı ile eşleştir (CARDIOLOGY gibi)
         if (d.name().equalsIgnoreCase(norm)) return d;

         // displayName ile eşleştir (Cardiology / General Surgery gibi)
         try {
             String disp = d.getDisplayName();
             if (disp != null && disp.equalsIgnoreCase(raw)) return d;
         } catch (Exception ignored) {}
     }

     // eşleşmezse default
     return all[0];
 }

 private int getDoctorPersonId(Persons.Doctor d) {
     // Doctor extends Person => Person'da genelde getId() vardır
     try {
         Object v = d.getClass().getMethod("getId").invoke(d);
         if (v instanceof Integer) return (Integer) v;
         return Integer.parseInt(String.valueOf(v));
     } catch (Exception ignored) {
         return -1;
     }
 }

 private Persons.Doctor buildDoctorFromDoctorsTxtLine(String fileId, String name, String surname, String dept) {
     int id = extractDoctorIntId(fileId);
     if (id < 0) return null;

     String fullName = (surname == null || surname.isBlank())
             ? (name == null ? "" : name.trim())
             : (name == null ? surname.trim() : (name.trim() + " " + surname.trim()));

     Clinics.Department dep = parseDepartmentSafe(dept);
     if (dep == null) return null;

     return new Persons.Doctor(id, fullName, dep);
 }

 

 public void addDoctorRuntime(String doctorId, String name, String surname, Clinics.Department dep) {
	    int idInt = extractDoctorIntId(doctorId);
	    if (idInt < 0) throw new IllegalArgumentException("Invalid doctor id");

	    String fullName = name.trim() + " " + surname.trim();
	    Doctor d = new Doctor(idInt, fullName, dep);

	    doctors.add(d);
	    clinicsManager.getClinic(dep).addDoctor(d);

	    appointmentManager.registerDoctor(d);
	    doctorRepository.add(d);

	    // ✅ EN ÖNEMLİ SATIR: doctor'u departmanın clinic listesine de ekle
	    

	    System.out.println("✅ Runtime doctor added: " + doctorId + " | dep=" + dep + " | doctorsSize=" + doctors.size());
	}
 private void savePatientToFile(Patient p) {
	    try {
	        Path file = ui.AppPaths.PATIENTS_FILE;

	        String line =
	                p.getNationalId() + ";" +
	                p.getName() + ";" +
	                p.getSurname() + ";" +
	                (p.getPhone() == null ? "N/A" : p.getPhone());

	        Files.writeString(
	                file,
	                line + System.lineSeparator(),
	                StandardCharsets.UTF_8,
	                java.nio.file.StandardOpenOption.CREATE,
	                java.nio.file.StandardOpenOption.APPEND
	        );

	    } catch (Exception e) {
	        throw new RuntimeException("Could not save patient: " + e.getMessage(), e);
	    }
	}
//HospitalService içinde:
 public List<Patient> getPatients() {
	    return patientRepo.loadAll(); // veya patientRepo.getAll() senin repodaki metoda göre
	}


 
 }



