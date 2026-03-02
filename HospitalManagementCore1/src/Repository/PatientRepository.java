package Repository;

import Persons.Patient;
import ui.AppPaths;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class PatientRepository {

    private static final Path FILE = AppPaths.PATIENTS_FILE;

    // ===============================
    // HASTALARI DOSYADAN OKU
    // ===============================
    public List<Patient> loadAll() {
        List<Patient> list = new ArrayList<>();

        try {
            if (!Files.exists(FILE)) return list;

            List<String> lines = Files.readAllLines(FILE, StandardCharsets.UTF_8);
            for (String line : lines) {
                if (line == null) continue;
                line = line.replace("\uFEFF", "").trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                // Format: tc;name;surname;phone
                String[] p = line.split(";", -1);
                if (p.length < 4) continue;

                String tc = p[0].trim();
                String name = p[1].trim();
                String surname = p[2].trim();
                String phone = p[3].trim();

                // Patient constructor senin projende bu şekildeydi
                Patient patient = new Patient(0, tc, name, surname, phone);
                list.add(patient);
            }

            System.out.println("📖 Patients loaded from: " + FILE.toAbsolutePath());
            System.out.println("📖 Patients loaded = " + list.size());

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    // ===============================
    // HASTAYI DOSYAYA KAYDET
    // ===============================
    public void save(Patient p) {
        try {
            if (!Files.exists(AppPaths.DATA_DIR))
                Files.createDirectories(AppPaths.DATA_DIR);

            String line =
                    p.getNationalId() + ";" +
                    p.getName() + ";" +
                    p.getSurname() + ";" +
                    p.getPhone();

            Files.write(
                    FILE,
                    List.of(line),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );

            System.out.println("🧍 Patient saved to: " + FILE.toAbsolutePath());
            System.out.println("🧍 Line = " + line);

        } catch (Exception e) {
            throw new RuntimeException("Could not save patient: " + e.getMessage(), e);
        }
    }
}
