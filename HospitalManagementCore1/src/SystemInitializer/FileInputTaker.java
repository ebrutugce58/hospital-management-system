package SystemInitializer;

import Clinics.Department;
import Persons.Doctor;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FileInputTaker {

    // ---------- Helper: resources'tan veya diskten aç ----------
    	private static BufferedReader openReader(String fileName) throws IOException {

    	    // 1) ÖNCE disk / data (yazılabilir yer)
    	    java.nio.file.Path p1 = java.nio.file.Paths.get(fileName);
    	    if (java.nio.file.Files.exists(p1)) {
    	        return new BufferedReader(new InputStreamReader(new FileInputStream(p1.toFile()), StandardCharsets.UTF_8));
    	    }

    	    // Eğer "doctors.txt" gibi isim geldiyse ve data/doctors.txt varsa onu da kontrol et
    	    java.nio.file.Path p2 = ui.AppPaths.DATA_DIR.resolve(fileName);
    	    if (java.nio.file.Files.exists(p2)) {
    	        return new BufferedReader(new InputStreamReader(new FileInputStream(p2.toFile()), StandardCharsets.UTF_8));
    	    }

    	    // 2) EN SON resources (read-only fallback)
    	    InputStream is = FileInputTaker.class.getResourceAsStream("/" + fileName);
    	    if (is != null) {
    	        return new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
    	    }

    	    // 3) Hiçbiri yoksa hata ver
    	    throw new FileNotFoundException("File not found: " + fileName);
    	}

    	// doctors.txt -> desteklenen formatlar:
    	// 1) "Full Name,DEPARTMENT"
    	// 2) "D1001;Name;Surname;DEPARTMENT"  (senin yeni kaydettiğin)
    	// 3) "1001;Full Name;DEPARTMENT" (olursa)
    	public static List<Doctor> readDoctorsFromFile(String fileName) {
    	    List<Doctor> doctors = new ArrayList<>();
    	    int nextId = 1001;

    	    try (BufferedReader br = openReader(fileName)) {
    	        String line;

    	        while ((line = br.readLine()) != null) {
    	            line = line.trim();
    	            if (line.isEmpty() || line.startsWith("#")) continue;

    	            String fullName;
    	            String depText;
    	            int id;

    	            // ✅ FORMAT A: Noktalı virgül
    	            if (line.contains(";")) {
    	                String[] p = line.split(";", -1);
    	                if (p.length < 4 && p.length < 3) continue;

    	                // D1001;Name;Surname;DEPT
    	                if (p.length >= 4) {
    	                    String rawId = p[0].trim().toUpperCase();
    	                    if (rawId.startsWith("D")) rawId = rawId.substring(1);
    	                    try { id = Integer.parseInt(rawId); }
    	                    catch (Exception e) { id = nextId++; }

    	                    String name = p[1].trim();
    	                    String surname = p[2].trim();
    	                    fullName = (name + " " + surname).trim();
    	                    depText = p[3].trim();
    	                }
    	                // 1001;Full Name;DEPT
    	                else {
    	                    try { id = Integer.parseInt(p[0].trim()); }
    	                    catch (Exception e) { id = nextId++; }

    	                    fullName = p[1].trim();
    	                    depText = p[2].trim();
    	                }
    	            }

    	            // ✅ FORMAT B: Virgül
    	            else if (line.contains(",")) {
    	                String[] parts = line.split(",");
    	                if (parts.length != 2) continue;

    	                id = nextId++;
    	                fullName = parts[0].trim();
    	                depText = parts[1].trim();
    	            }

    	            else {
    	                continue;
    	            }

    	            depText = depText.trim()
    	                    .toUpperCase(Locale.ENGLISH)
    	                    .replace('İ', 'I');

    	            Department dep;
    	            try {
    	                dep = Department.valueOf(depText);
    	            } catch (Exception e) {
    	                System.out.println("Invalid department in doctors file: " + depText + " line=" + line);
    	                continue;
    	            }

    	            doctors.add(new Doctor(id, fullName, dep));
    	        }

    	    } catch (IOException e) {
    	        System.out.println("Doctor file could not be read: " + e.getMessage());
    	    }

    	    System.out.println("✅ Doctors loaded = " + doctors.size());
    	    return doctors;
    	}

    public static List<String[]> readDiagnosesRaw(String fileName) {
        List<String[]> result = new ArrayList<>();

        try (BufferedReader br = openReader(fileName)) {
            String line;

            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                // AYIRAÇ: |
                String[] parts = line.split("\\|");

                // Trim all parts
                for (int i = 0; i < parts.length; i++) {
                    parts[i] = parts[i].trim();
                }

                // New format: 6 columns
                if (parts.length == 6) {
                    result.add(parts);
                }
                // Old format: 5 columns (no phone)
                else if (parts.length == 5) {
                    String[] upgraded = new String[6];
                    upgraded[0] = parts[0];
                    upgraded[1] = parts[1];
                    upgraded[2] = parts[2];
                    upgraded[3] = parts[3];
                    upgraded[4] = "N/A";
                    upgraded[5] = parts[4];
                    result.add(upgraded);
                }
                // otherwise skip
            }

        } catch (IOException e) {
            System.out.println("Diagnoses file could not be read: " + e.getMessage());
        }

        System.out.println("✅ Diagnoses rows loaded = " + result.size());
        return result;
    }
}
