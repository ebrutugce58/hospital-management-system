package ui;

import java.io.InputStream;
import java.nio.file.*;

public final class AppPaths {
    private AppPaths() {}

    public static final Path BASE_DIR =Paths.get(System.getProperty("user.home"), "HospitalManagementSystem");
    public static final Path DATA_DIR = BASE_DIR.resolve("data");
    public static final Path USERS_FILE = DATA_DIR.resolve("users.txt");
    public static final Path DOCTORS_FILE = DATA_DIR.resolve("doctors.txt");
    public static final Path PATIENTS_FILE = DATA_DIR.resolve("patients.txt");
    public static final Path APPOINTMENTS_FILE = DATA_DIR.resolve("appointments.txt");

    
    public static void ensureDataFiles() {
        try {
            if (!Files.exists(DATA_DIR)) Files.createDirectories(DATA_DIR);

            // 1) users.txt (data) yoksa veya boşsa resources/users.txt -> data/users.txt
            boolean usersMissing = !Files.exists(USERS_FILE);
            boolean usersEmpty = (!usersMissing && Files.size(USERS_FILE) == 0);
            if (usersMissing || usersEmpty) {
                try (InputStream in = AppPaths.class.getResourceAsStream("/users.txt")) {
                    if (in == null) {
                        // resources/users.txt yoksa en azından boş dosya oluştur
                        if (!Files.exists(USERS_FILE)) Files.createFile(USERS_FILE);
                    } else {
                        Files.copy(in, USERS_FILE, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }

         // 2) doctors.txt (data) yoksa veya boşsa resources/doctors.txt -> data/doctors.txt
            boolean doctorsMissing = !Files.exists(DOCTORS_FILE);
            boolean doctorsEmpty = (!doctorsMissing && Files.size(DOCTORS_FILE) == 0);

            if (doctorsMissing || doctorsEmpty) {
                try (InputStream in = AppPaths.class.getResourceAsStream("/doctors.txt")) {

                    // ✅ resources'ta yoksa CRASH ETME -> boş dosya oluştur
                    if (in == null) {
                        if (!Files.exists(DOCTORS_FILE)) Files.createFile(DOCTORS_FILE);
                    } else {
                        Files.copy(in, DOCTORS_FILE, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }

            // 3) patients.txt (data) yoksa veya boşsa resources/patients.txt -> data/patients.txt
            boolean patientsMissing = !Files.exists(PATIENTS_FILE);
            boolean patientsEmpty = (!patientsMissing && Files.size(PATIENTS_FILE) == 0);
            if (patientsMissing || patientsEmpty) {
                try (InputStream in = AppPaths.class.getResourceAsStream("/patients.txt")) {
                    if (in == null) {
                        if (!Files.exists(PATIENTS_FILE)) Files.createFile(PATIENTS_FILE);
                    } else {
                        Files.copy(in, PATIENTS_FILE, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
            // 4) appointments.txt yoksa oluştur
            boolean appMissing = !Files.exists(APPOINTMENTS_FILE);
            if (appMissing) Files.createFile(APPOINTMENTS_FILE);


        } catch (Exception e) {
            throw new RuntimeException("Could not prepare data files: " + e.getMessage(), e);
        }
        System.out.println("✅ PATIENTS_FILE = " + PATIENTS_FILE.toAbsolutePath());
        System.out.println("✅ exists? " + Files.exists(PATIENTS_FILE));

      }

    }


