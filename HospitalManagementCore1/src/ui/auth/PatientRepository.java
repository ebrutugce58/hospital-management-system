package ui.auth;

import Persons.Patient;
import ui.AppPaths;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class PatientRepository {

    private static final Path FILE = AppPaths.PATIENTS_FILE;

    public List<Patient> loadAll() {
        List<Patient> list = new ArrayList<>();
        if (!Files.exists(FILE)) return list;
        System.out.println("📖 PatientRepository.loadAll size = " + list.size());

        try {
            List<String> lines = Files.readAllLines(FILE, StandardCharsets.UTF_8);
            for (String line : lines) {
                String t = (line == null) ? "" : line.replace("\uFEFF", "").trim();
                if (t.isEmpty() || t.startsWith("#")) continue;

                String[] p = t.split(";", -1);
                if (p.length < 5) continue;

                String nationalId = p[0].trim();
                String name = p[1].trim();
                String surname = p[2].trim();
                String phone = p[3].trim();
                int internalId;

                try { internalId = Integer.parseInt(p[4].trim()); }
                catch (Exception e) { continue; }

                list.add(new Patient(internalId, nationalId, name, surname, phone));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
        
    }

    // aynı TC varsa tekrar yazma (basit kontrol)
    public boolean existsNationalId(String nationalId) {
        if (nationalId == null) return false;
        nationalId = nationalId.trim();
        if (nationalId.isEmpty()) return false;

        if (!Files.exists(FILE)) return false;

        try {
            for (String line : Files.readAllLines(FILE, StandardCharsets.UTF_8)) {
                String t = (line == null) ? "" : line.replace("\uFEFF","").trim();
                if (t.isEmpty() || t.startsWith("#")) continue;
                String[] p = t.split(";", -1);
                if (p.length < 1) continue;
                if (p[0].trim().equals(nationalId)) return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    public void append(Patient p) {
        try {
            if (!Files.exists(AppPaths.DATA_DIR)) Files.createDirectories(AppPaths.DATA_DIR);
            if (!Files.exists(FILE)) Files.createFile(FILE);

            String line = p.getNationalId() + ";" +
                    p.getName() + ";" +
                    p.getSurname() + ";" +
                    (p.getPhone() == null ? "N/A" : p.getPhone()) + ";" +
                    p.getId(); // Patient class'ında internalId getter yoksa söyle, alternatif yazarım

            Files.writeString(FILE, line + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);

        } catch (Exception e) {
            throw new RuntimeException("Could not save patient: " + e.getMessage(), e);
        }
    }
}
